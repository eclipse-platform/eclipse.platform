package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.search.*;
import org.eclipse.update.internal.ui.security.JarVerificationService;

public class NewUpdatesWizard extends Wizard {
	private static final String KEY_INSTALLING = "NewUpdatesWizard.installing";
	private IInstallConfiguration config;
	private NewUpdatesWizardPage mainPage;
	private PendingChange[] jobs;
	private int installCount = 0;

	public NewUpdatesWizard(SearchObject searchObject) {
		setDialogSettings(UpdateUIPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(UpdateUIPluginImages.DESC_UPDATE_WIZ);
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
		createPendingChanges(searchObject);
	}

	public boolean isSuccessfulInstall() {
		return installCount>0;
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
				IFeature feature = adapter.getFeature();
				IFeature[] installed =
					UpdateUIPlugin.getInstalledFeatures(feature);
				PendingChange change = new PendingChange(installed[0], feature);
				result.add(change);
			} catch (CoreException e) {
				UpdateUIPlugin.logException(e);
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
		final PendingChange [] selectedJobs = mainPage.getSelectedJobs();
		IRunnableWithProgress operation = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException {
				try {
					installCount = 0;
					InstallWizard.makeConfigurationCurrent(config);
					execute(selectedJobs, monitor);
					InstallWizard.saveLocalSite();
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
			UpdateUIPlugin.logException(e);
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
		}
	}

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return page.getNextPage() == null && super.canFinish();
	}

	/*
	 * When we are uninstalling, there is not targetSite
	 */
	private void execute(PendingChange [] selectedJobs, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(UpdateUIPlugin.getResourceString(KEY_INSTALLING), jobs.length);
		for (int i=0; i<selectedJobs.length; i++) {
			PendingChange job = selectedJobs[i];
			SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
			executeOneJob(job, subMonitor);
			installCount++;
		}
	}
	
	private void executeOneJob(PendingChange job, SubProgressMonitor monitor) throws CoreException {
		IFeature feature = job.getFeature();
		IFeature oldFeature = job.getOldFeature();
		IConfiguredSite targetSite = TargetPage.getDefaultTargetSite(config, job);
		targetSite.install(feature, getVerificationListener(), monitor);
		unconfigure(oldFeature);
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.addPendingChange(job);
	}

	private void throwError(String message) throws CoreException {
		IStatus status =
			new Status(
				IStatus.ERROR,
				UpdateUIPlugin.getPluginId(),
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
			return site.unconfigure(feature);
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