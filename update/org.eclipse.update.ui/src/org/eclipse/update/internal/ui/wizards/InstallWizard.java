package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.UpdateUIPluginImages;
import org.eclipse.update.internal.ui.model.PendingChange;
import org.eclipse.update.internal.ui.model.UpdateModel;
import org.eclipse.update.internal.ui.security.JarVerificationService;

public class InstallWizard extends Wizard {
	private static final String KEY_UNABLE = "InstallWizard.error.unable";
	private static final String KEY_OLD = "InstallWizard.error.old";
	private static final String KEY_SAVED_CONFIG = "InstallWizard.savedConfig";
	private ReviewPage reviewPage;
	private OptionalFeaturesPage optionalFeaturesPage;
	private TargetPage targetPage;
	private PendingChange job;
	private boolean successfulInstall = false;
	private IInstallConfiguration config;
	private boolean patch;

	public InstallWizard(PendingChange job) {
		setDialogSettings(UpdateUIPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(UpdateUIPluginImages.DESC_INSTALL_WIZ);
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);
		this.job = job;
		IFeature feature = job.getFeature();
		patch = UpdateUIPlugin.isPatch(feature);
	}

	public boolean isSuccessfulInstall() {
		return successfulInstall;
	}

	/**
	 * @see Wizard#performFinish()
	 */
	public boolean performFinish() {
		final IConfiguredSite targetSite =
			(targetPage == null) ? null : targetPage.getTargetSite();
		final IFeatureReference[] optionalFeatures =
			(optionalFeaturesPage == null)
				? null
				: optionalFeaturesPage.getCheckedOptionalFeatures();
		final Object[] optionalElements =
			(optionalFeaturesPage == null)
				? null
				: optionalFeaturesPage.getOptionalElements();
		if (job.getJobType() == PendingChange.INSTALL) {
			ArrayList conflicts =
				DuplicateConflictsDialog.computeDuplicateConflicts(
					job,
					config,
					targetSite,
					optionalFeatures);
			if (conflicts != null) {
				DuplicateConflictsDialog dialog =
					new DuplicateConflictsDialog(getShell(), conflicts);
				if (dialog.open() != 0)
					return false;
			}
		}
		IRunnableWithProgress operation = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException {
				try {
					successfulInstall = false;
					makeConfigurationCurrent(config, job);
					execute(
						targetSite,
						optionalElements,
						optionalFeatures,
						monitor);
					saveLocalSite();
					successfulInstall = true;
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
			Throwable target = e.getTargetException();
			if (target instanceof InstallAbortedException) {
				// should we revert to the previous configuration?
			} else {
				UpdateUIPlugin.logException(e);
			}
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	public void addPages() {
		reviewPage = new ReviewPage(job);
		addPage(reviewPage);

		config = createInstallConfiguration();

		if (job.getJobType() == PendingChange.INSTALL) {
			if (UpdateModel.hasLicense(job)) {
				addPage(new LicensePage(job));
			}
			if (hasOptionalFeatures(job.getFeature())) {
				optionalFeaturesPage = new OptionalFeaturesPage(job, config);
				addPage(optionalFeaturesPage);
			}
			targetPage = new TargetPage(job, config);
			addPage(targetPage);
		}
	}

	public static IInstallConfiguration createInstallConfiguration() {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config =
				localSite.cloneCurrentConfiguration();
			config.setLabel(Utilities.format(config.getCreationDate()));
			return config;
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
			return null;
		}
	}

	public static void makeConfigurationCurrent(IInstallConfiguration config, PendingChange job)
		throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		if (job!=null && job.getJobType()==PendingChange.INSTALL) {
			if (UpdateUIPlugin.isPatch(job.getFeature())) {
				// Installing a patch - preserve the current configuration
				IInstallConfiguration cconfig = localSite.getCurrentConfiguration();
				IInstallConfiguration savedConfig = localSite.addToPreservedConfigurations(cconfig);
				VersionedIdentifier vid = job.getFeature().getVersionedIdentifier();
				String key = "@"+vid.getIdentifier()+"_"+vid.getVersion();
				String newLabel = UpdateUIPlugin.getFormattedMessage(KEY_SAVED_CONFIG, key);
				savedConfig.setLabel(newLabel);
				UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
				model.fireObjectChanged(savedConfig, null);
			}
		}
		localSite.addConfiguration(config);
	}

	public static void saveLocalSite() throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		localSite.save();
	}

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return page.getNextPage() == null && super.canFinish();
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		return super.getPreviousPage(page);
	}
	public IWizardPage getNextPage(IWizardPage page) {
		return super.getNextPage(page);
	}
	/*
	 * When we are uninstalling, there is no targetSite
	 */
	private void execute(
		IConfiguredSite targetSite,
		Object[] optionalElements,
		IFeatureReference[] optionalFeatures,
		IProgressMonitor monitor)
		throws CoreException {
		IFeature feature = job.getFeature();
		if (job.getJobType() == PendingChange.UNINSTALL) {
			//find the  config site of this feature
			IConfiguredSite site = findConfigSite(feature, config);
			if (site != null) {
				site.remove(feature, monitor);
			} else {
				// we should do something here
				throwError(UpdateUIPlugin.getResourceString(KEY_UNABLE));
			}
		} else if (job.getJobType() == PendingChange.INSTALL) {
			if (optionalFeatures == null)
				targetSite.install(feature, getVerificationListener(), monitor);
			else
				targetSite.install(
					feature,
					optionalFeatures,
					getVerificationListener(),
					monitor);
			IFeature oldFeature = job.getOldFeature();
			if (oldFeature != null && !job.isOptionalDelta()) {
				if (optionalElements != null) {
					boolean patch = UpdateUIPlugin.isPatch(feature);
					preserveOptionalState(config, targetSite, patch, optionalElements);
				}
				boolean oldSuccess = unconfigure(config, oldFeature);
				if (!oldSuccess) {
					if (!isNestedChild(oldFeature))
						// "eat" the error if nested child
						throwError(UpdateUIPlugin.getResourceString(KEY_OLD));
				}
			}
			if (oldFeature == null) {
				ensureUnique(config, feature, targetSite);
				if (optionalFeatures != null) {
					preserveOriginatingURLs(feature, optionalFeatures);
				}
			}
		} else if (job.getJobType() == PendingChange.CONFIGURE) {
			configure(feature);
			ensureUnique(config, feature, targetSite);
		} else if (job.getJobType() == PendingChange.UNCONFIGURE) {
			unconfigure(config, job.getFeature());
		} else {
			// should not be here
			return;
		}
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.addPendingChange(job);
	}

	static void ensureUnique(
		IInstallConfiguration config,
		IFeature feature,
		IConfiguredSite targetSite)
		throws CoreException {
		boolean patch = false;
		if (targetSite==null)
			targetSite = feature.getSite().getCurrentConfiguredSite();
		IImport[] imports = feature.getImports();
		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				patch = true;
				break;
			}
		}
		// Only need to check features that patch other features.
		if (!patch)
			return;
		IFeature localFeature = findLocalFeature(targetSite, feature);
		ArrayList oldFeatures = new ArrayList();
		// First collect all older active features that
		// have the same ID as new features marked as 'unique'.
		collectOldFeatures(localFeature, targetSite, oldFeatures);
		// Now unconfigure old features to enforce uniqueness
		for (int i = 0; i < oldFeatures.size(); i++) {
			IFeature oldFeature = (IFeature) oldFeatures.get(i);
			unconfigure(config, oldFeature);
		}
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

	private static boolean unconfigure(
		IInstallConfiguration config,
		IFeature feature)
		throws CoreException {
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

	private boolean isNestedChild(IFeature feature) {
		IConfiguredSite[] csites = config.getConfiguredSites();
		try {
			for (int i = 0; csites != null && i < csites.length; i++) {
				IFeatureReference[] refs = csites[i].getConfiguredFeatures();
				for (int j = 0; refs != null && j < refs.length; j++) {
					IFeature parent = refs[j].getFeature();
					IFeatureReference[] children =
						parent.getIncludedFeatureReferences();
					for (int k = 0;
						children != null && k < children.length;
						k++) {
						IFeature child = children[k].getFeature();
						if (feature.equals(child))
							return true;
					}
				}
			}
		} catch (CoreException e) {
			// will return false
		}
		return false;
	}

	static boolean hasOptionalFeatures(IFeatureReference fref) {
		try {
			return hasOptionalFeatures(fref.getFeature());
		} catch (CoreException e) {
			return false;
		}
	}
	static boolean hasOptionalFeatures(IFeature feature) {
		try {
			IFeatureReference[] irefs = feature.getIncludedFeatureReferences();
			for (int i = 0; i < irefs.length; i++) {
				IFeatureReference iref = irefs[i];
				if (iref.isOptional())
					return true;
				// see if it has optional children
				IFeature child = iref.getFeature();
				if (hasOptionalFeatures(child))
					return true;
			}
		} catch (CoreException e) {
		}
		return false;
	}

	static void preserveOptionalState(
		IInstallConfiguration config,
		IConfiguredSite targetSite,
		boolean patch,
		Object[] optionalElements) {
		for (int i = 0; i < optionalElements.length; i++) {
			FeatureHierarchyElement fe =
				(FeatureHierarchyElement) optionalElements[i];
			Object[] children = fe.getChildren(true, patch, config);
			preserveOptionalState(config, targetSite, patch, children);
			if (!fe.isEnabled(config)) {
				IFeature newFeature = fe.getFeature();
				try {
					IFeature localFeature =
						findLocalFeature(targetSite, newFeature);
					if (localFeature != null)
						targetSite.unconfigure(localFeature);
				} catch (CoreException e) {
					// Eat this - we will leave with it
				}
			}
		}
	}

	static void collectOldFeatures(
		IFeature feature,
		IConfiguredSite targetSite,
		ArrayList result)
		throws CoreException {
		IFeatureReference[] included = feature.getIncludedFeatureReferences();
		for (int i = 0; i < included.length; i++) {
			IFeatureReference iref = included[i];
			IFeature ifeature;
			
			try {
				ifeature = iref.getFeature();
			}
			catch (CoreException e) {
				if (iref.isOptional())
					continue;
				throw e;
			}
			// find other features and unconfigure
			String id = iref.getVersionedIdentifier().getIdentifier();
			IFeature[] sameIds =
				UpdateUIPlugin.searchSite(id, targetSite, true);
			for (int j = 0; j < sameIds.length; j++) {
				IFeature sameId = sameIds[j];
				// Ignore self.
				if (sameId.equals(ifeature))
					continue;
				result.add(sameId);
			}
			collectOldFeatures(ifeature, targetSite, result);
		}
	}

	private static IFeature findLocalFeature(
		IConfiguredSite csite,
		IFeature feature)
		throws CoreException {
		IFeatureReference[] refs = csite.getConfiguredFeatures();
		for (int i = 0; i < refs.length; i++) {
			IFeatureReference ref = refs[i];
			VersionedIdentifier refVid = ref.getVersionedIdentifier();
			if (feature.getVersionedIdentifier().equals(refVid))
				return ref.getFeature();
		}
		return null;
	}

	private void preserveOriginatingURLs(
		IFeature feature,
		IFeatureReference[] optionalFeatures) {
		// walk the hieararchy and preserve the originating URL
		// for all the optional features that are not chosen to
		// be installed.
		URL url = feature.getSite().getURL();
		try {
			IFeatureReference[] irefs = feature.getIncludedFeatureReferences();
			for (int i = 0; i < irefs.length; i++) {
				IFeatureReference iref = irefs[i];
				boolean preserve = false;
				if (iref.isOptional()) {
					boolean onTheList = false;
					for (int j = 0; j < optionalFeatures.length; j++) {
						if (optionalFeatures[j].equals(iref)) {
							//was on the list
							onTheList = true;
							break;
						}
					}
					if (!onTheList)
						preserve = true;
				}
				if (preserve) {
					try {
						String id =
							iref.getVersionedIdentifier().getIdentifier();
						UpdateUIPlugin.setOriginatingURL(id, url);
					} catch (CoreException e) {
						// Silently ignore
					}
				} else {
					try {
						IFeature ifeature = iref.getFeature();
						preserveOriginatingURLs(ifeature, optionalFeatures);
					} catch (CoreException e) {
						// Silently ignore
					}
				}
			}
		} catch (CoreException e) {
			// Silently ignore
		}
	}
}