/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIImages;
import org.eclipse.update.internal.ui.forms.ActivityConstraints;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.search.SearchObject;
import org.eclipse.update.internal.ui.search.SearchResultSite;
import org.eclipse.update.internal.ui.security.JarVerificationService;

public class NewUpdatesWizard extends Wizard {
	private static final String KEY_INSTALLING = "NewUpdatesWizard.installing";
	private IInstallConfiguration config;
	private NewUpdatesWizardPage mainPage;
	private LicensePage licensePage;
	private PendingChange[] jobs;
	private int installCount = 0;

	public NewUpdatesWizard(SearchObject searchObject) {
		setDialogSettings(UpdateUI.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(UpdateUIImages.DESC_UPDATE_WIZ);
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
		createPendingChanges(searchObject);
	}

	public boolean isSuccessfulInstall() {
		return installCount > 0;
	}

	private void createPendingChanges(SearchObject searchObject) {
		ArrayList result = new ArrayList();
		Object[] sites = searchObject.getChildren(null);
		for (int i = 0; i < sites.length; i++) {
			SearchResultSite site = (SearchResultSite) sites[i];
			createPendingChanges(site, result);
		}
		jobs =
			(PendingChange[]) result.toArray(new PendingChange[result.size()]);
	}

	private void createPendingChanges(
		SearchResultSite site,
		ArrayList result) {
		Object[] candidates = site.getChildren(null);
		for (int i = 0; i < candidates.length; i++) {
			SimpleFeatureAdapter adapter = (SimpleFeatureAdapter) candidates[i];
			try {
				IFeature feature = adapter.getFeature(null);
				IFeature[] installed =
					UpdateUI.getInstalledFeatures(feature);
				PendingChange change = new PendingChange(installed.length>0?installed[0]:null, feature);
				result.add(change);
			} catch (CoreException e) {
				UpdateUI.logException(e);
			}
		}
	}

	private IFeature[] getFeaturesWithLicenses() {
		ArrayList result = new ArrayList();
		for (int i = 0; i < jobs.length; i++) {
			PendingChange job = jobs[i];
			IFeature feature = job.getFeature();
			IURLEntry info = feature.getLicense();
			if (info == null)
				continue;
			if (info.getAnnotation() != null
				&& info.getAnnotation().length() > 0)
				result.add(feature);
		}
		return (IFeature[]) result.toArray(new IFeature[result.size()]);
	}

	/**
	 * @see Wizard#performFinish()
	 */
	public boolean performFinish() {
		final PendingChange[] selectedJobs = mainPage.getSelectedJobs();
		
		// make sure we can actually apply the entire batch of updates
		IStatus status = ActivityConstraints.validatePendingOneClickUpdate(selectedJobs);
		if (status != null) {
			ErrorDialog.openError(
				UpdateUI.getActiveWorkbenchShell(),
				null,
				null,
				status);
			return false;
		}
		
		// Check for duplication conflicts
		ArrayList conflicts = DuplicateConflictsDialog.computeDuplicateConflicts(selectedJobs, config);
		if (conflicts!=null) {
			DuplicateConflictsDialog dialog = new DuplicateConflictsDialog(getShell(), conflicts);
			if (dialog.open()!=0) return false;
		}
		
		// ok to continue		
		IRunnableWithProgress operation = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException {
				try {
					installCount = 0;
					InstallWizard.makeConfigurationCurrent(config, null);
					execute(selectedJobs, monitor);
				} catch (InstallAbortedException e) {
					throw new InvocationTargetException(e);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, true, operation);
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			if (targetException instanceof InstallAbortedException) {
				return true;
			} else {
				UpdateUI.logException(e);
			}
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	public void addPages() {
		config = InstallWizard.createInstallConfiguration();
		if (config != null) {
			mainPage = new NewUpdatesWizardPage(jobs, config);
			addPage(mainPage);
			licensePage = new LicensePage(true);
			addPage(licensePage);
		}
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (page instanceof LicensePage)
			return null;
		PendingChange[] licenseJobs = mainPage.getSelectedJobsWithLicenses();
		if (licenseJobs.length == 0)
			return null;
		licensePage.setJobs(licenseJobs);
		return licensePage;
	}
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page instanceof LicensePage)
			return mainPage;
		return null;
	}

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return page.getNextPage() == null && super.canFinish();
	}

	/*
	 * When we are uninstalling, there is not targetSite
	 */
	private void execute(
		PendingChange[] selectedJobs,
		IProgressMonitor monitor)
		throws InstallAbortedException, CoreException {
		monitor.beginTask(
			UpdateUI.getString(KEY_INSTALLING),
			jobs.length);
		for (int i = 0; i < selectedJobs.length; i++) {
			PendingChange job = selectedJobs[i];
			SubProgressMonitor subMonitor =
				new SubProgressMonitor(
					monitor,
					1,
					SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
			executeOneJob(job, subMonitor);
			monitor.worked(1);
			InstallWizard.saveLocalSite();
			installCount++;
		}
	}

	private void executeOneJob(PendingChange job, SubProgressMonitor monitor)
		throws InstallAbortedException, CoreException {
		IFeature feature = job.getFeature();
		IFeature oldFeature = job.getOldFeature();
		boolean reinstall=false;
		IFeatureReference [] optionalFeatures=null;
		if (oldFeature!=null && feature.getVersionedIdentifier().equals(oldFeature.getVersionedIdentifier())) {
			reinstall=true;
		}
		ArrayList optionalElements = new ArrayList();
		boolean hasOptionalFeatures = FeatureHierarchyElement.computeElements(
			oldFeature,
			feature,
			oldFeature != null,
			false,
			config,
			optionalElements);
		if (hasOptionalFeatures)
			optionalFeatures = computeOptionalFeatures(optionalElements, oldFeature!=null);
		IConfiguredSite targetSite =
			TargetPage.getDefaultTargetSite(config, job);
		if (optionalFeatures!=null)
			targetSite.install(feature, optionalFeatures, getVerificationListener(), monitor);
		else
			targetSite.install(feature, getVerificationListener(), monitor);
		if (!reinstall) {
			if (optionalFeatures!=null) {
				InstallWizard.preserveOptionalState(config, targetSite, false, optionalElements.toArray());
			}
			if (oldFeature!=null) unconfigure(oldFeature);
			else {
				MultiInstallWizard.ensureUnique(config, feature, targetSite);
			}
		}
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		model.addPendingChange(job);
	}

	private IFeatureReference[] computeOptionalFeatures(ArrayList elements, boolean update) {
		HashSet set = new HashSet();
		for (int i = 0; i < elements.size(); i++) {
			FeatureHierarchyElement element =
				(FeatureHierarchyElement) elements.get(i);
			element.addCheckedOptionalFeatures(
				update,
				false,
				config,
				set);
		}
		return (IFeatureReference[]) set.toArray(
			new IFeatureReference[set.size()]);
	}
	
	private void throwError(String message) throws CoreException {
		IStatus status =
			new Status(
				IStatus.ERROR,
				UpdateUI.getPluginId(),
				IStatus.OK,
				message,
				null);
		throw new CoreException(status);
	}

	static IConfiguredSite findConfigSite(
		IFeature feature,
		IInstallConfiguration config)
		throws CoreException {
		IConfiguredSite[] configSites = config.getConfiguredSites();
		for (int i = 0; i < configSites.length; i++) {
			IConfiguredSite site = configSites[i];
			if (site.getSite().equals(feature.getSite())) {
				return site;
			}
		}
		return null;
	}

	private boolean unconfigure(IFeature feature) throws CoreException {
		IConfiguredSite site = findConfigSite(feature, config);
		if (site != null) {
			PatchCleaner cleaner = new PatchCleaner(site, feature);
			boolean result = site.unconfigure(feature);
			cleaner.dispose();
			return result;
		}
		return false;
	}
	private void configure(IFeature feature) throws CoreException {
		IConfiguredSite site = findConfigSite(feature, config);
		if (site != null) {
			site.configure(feature);
		}
	}

	private IVerificationListener getVerificationListener() {
		return new JarVerificationService(this.getShell());
	}
}
