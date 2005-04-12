/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.views;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.branding.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.part.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.operations.*;
import org.osgi.framework.*;
import org.eclipse.core.runtime.Path;


/**
 * Insert the type's description here.
 * @see ViewPart
 */
public class ConfigurationView
	implements
		IInstallConfigurationChangedListener,
		IConfiguredSiteChangedListener,
		ILocalSiteChangedListener {
	private TreeViewer treeViewer;
	private DrillDownAdapter drillDownAdapter;
	private Action collapseAllAction;
	private static final String STATE_SHOW_UNCONF = "ConfigurationView.showUnconf"; //$NON-NLS-1$
	private static final String STATE_SHOW_SITES = "ConfigurationView.showSites"; //$NON-NLS-1$
	private static final String STATE_SHOW_NESTED_FEATURES =
		"ConfigurationView.showNestedFeatures"; //$NON-NLS-1$

	private Action showSitesAction;
	private Action showNestedFeaturesAction;
	private ReplaceVersionAction swapVersionAction;
	private FeatureStateAction featureStateAction;
	private UninstallFeatureAction uninstallFeatureAction;
	private InstallOptionalFeatureAction installOptFeatureAction;
	private Action showUnconfFeaturesAction;
	private RevertConfigurationAction revertAction;
	private ShowActivitiesAction showActivitiesAction;
	private Action propertiesAction;
	private SiteStateAction siteStateAction;
	private Action installationHistoryAction;
	private Action newExtensionLocationAction;
	private FindUpdatesAction findUpdatesAction;
	private SashForm splitter;
	private ConfigurationPreview preview;
	private Hashtable previewTasks;

	private IUpdateModelChangedListener modelListener;
	private boolean refreshLock = false;
	private Image eclipseImage;
	private boolean initialized;
	private ConfigurationManagerWindow configurationWindow;

	class ConfigurationSorter extends ViewerSorter {
		public int category(Object obj) {
			// sites
			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfiguredSite();
				if (csite.isProductSite())
					return 1;
				if (csite.isExtensionSite())
					return 2;
				return 3;
			}
			return super.category(obj);
		}
	}

	class LocalSiteProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public void inputChanged(
			Viewer viewer,
			Object oldInput,
			Object newInput) {
			if (newInput == null)
				return;
		}

		/**
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parent) {
			if (parent instanceof UpdateModel) {
				ILocalSite localSite = getLocalSite();
				return (localSite != null) ? new Object[] { localSite }
				: new Object[0];
			}

			if (parent instanceof ILocalSite) {
				Object[] csites = openLocalSite();
				if (showSitesAction.isChecked())
					return csites;
				ArrayList result = new ArrayList();
				boolean showUnconf = showUnconfFeaturesAction.isChecked();
				for (int i = 0; i < csites.length; i++) {
					IConfiguredSiteAdapter adapter =
						(IConfiguredSiteAdapter) csites[i];
					Object[] roots = getFeatures(adapter, !showUnconf);
					for (int j = 0; j < roots.length; j++) {
						result.add(roots[j]);
					}
				}
				return result.toArray();
			}

			if (parent instanceof IConfiguredSiteAdapter) {
				return getFeatures(
					(IConfiguredSiteAdapter) parent,
					!showUnconfFeaturesAction.isChecked());
			}
			if (parent instanceof ConfiguredFeatureAdapter
				&& showNestedFeaturesAction.isChecked()) {
				IFeatureAdapter[] nested =
					((ConfiguredFeatureAdapter) parent).getIncludedFeatures(
						null);
				if (showUnconfFeaturesAction.isChecked())
					return nested;
				ArrayList result = new ArrayList();
				for (int i = 0; i < nested.length; i++) {
					if (((ConfiguredFeatureAdapter) nested[i]).isConfigured())
						result.add(nested[i]);
				}
				return (IFeatureAdapter[]) result.toArray(
					new IFeatureAdapter[result.size()]);
			}
			return new Object[0];
		}

		public Object getParent(Object child) {
			return null;
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof ConfiguredFeatureAdapter) {
				if (!showNestedFeaturesAction.isChecked())
					return false;
				IFeatureAdapter[] features =
					((ConfiguredFeatureAdapter) parent).getIncludedFeatures(
						null);

				if (showUnconfFeaturesAction.isChecked())
					return features.length > 0;

				for (int i = 0; i < features.length; i++) {
					if (((ConfiguredFeatureAdapter) features[i])
						.isConfigured())
						return true;
				}
				return false;
			}
			if (parent instanceof ConfiguredSiteAdapter) {
				IConfiguredSite site =
					((ConfiguredSiteAdapter) parent).getConfiguredSite();
				if (site.isEnabled()) {
					if (!showUnconfFeaturesAction.isChecked())
						return site.getConfiguredFeatures().length > 0;
					return site.getFeatureReferences().length > 0;
				}
				return (showUnconfFeaturesAction.isChecked());
			}
			return true;
		}

		public Object[] getElements(Object input) {
			return getChildren(input);
		}
	}

	class LocalSiteLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof ILocalSite) {
				IProduct product = Platform.getProduct();
				if (product != null)
					return product.getName();
				return UpdateUIMessages.ConfigurationView_current; 
			}

			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfiguredSite();
				ISite site = csite.getSite();
				return new File(site.getURL().getFile()).toString();
			}
			if (obj instanceof IFeatureAdapter) {
				try {
					IFeature feature = ((IFeatureAdapter) obj).getFeature(null);
					if (feature instanceof MissingFeature) {
						return NLS.bind("ConfigurationView.missingFeature", feature.getLabel());
					}
					String version =
						feature
							.getVersionedIdentifier()
							.getVersion()
							.toString();
					String pending = ""; //$NON-NLS-1$
					if (OperationsManager.findPendingOperation(feature)
						!= null)
						pending = UpdateUIMessages.ConfigurationView_pending; 
					return feature.getLabel() + " " + version + pending; //$NON-NLS-1$
				} catch (CoreException e) {
					return UpdateUIMessages.ConfigurationView_error; 
				}
			}
			return super.getText(obj);
		}

		public Image getImage(Object obj) {
			UpdateLabelProvider provider =
				UpdateUI.getDefault().getLabelProvider();
			if (obj instanceof ILocalSite)
				return eclipseImage;

			if (obj instanceof ConfiguredFeatureAdapter)
				return getFeatureImage(
					provider,
					(ConfiguredFeatureAdapter) obj);

			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfiguredSite();
				int flags =
					csite.isUpdatable() ? 0 : UpdateLabelProvider.F_LINKED;
				if (!csite.isEnabled())
					flags |= UpdateLabelProvider.F_UNCONFIGURED;
				return provider.get(
					provider.getLocalSiteDescriptor(csite),
					flags);
			}
			return null;
		}

		private Image getFeatureImage(
			UpdateLabelProvider provider,
			ConfiguredFeatureAdapter adapter) {
			try {
				IFeature feature = adapter.getFeature(null);
				if (feature instanceof MissingFeature) {
					if (((MissingFeature) feature).isOptional())
						return provider.get(
							UpdateUIImages.DESC_NOTINST_FEATURE_OBJ);
					return provider.get(
						UpdateUIImages.DESC_FEATURE_OBJ,
						UpdateLabelProvider.F_ERROR);
				}

				boolean efix = feature.isPatch();
				ImageDescriptor baseDesc =
					efix
						? UpdateUIImages.DESC_EFIX_OBJ
						: (adapter.isConfigured()
							? UpdateUIImages.DESC_FEATURE_OBJ
							: UpdateUIImages.DESC_UNCONF_FEATURE_OBJ);

				int flags = 0;
				if (efix && !adapter.isConfigured())
					flags |= UpdateLabelProvider.F_UNCONFIGURED;
				if (OperationsManager.findPendingOperation(feature) == null) {
					ILocalSite localSite = getLocalSite();
					if (localSite != null) {
						int code =
							getStatusCode(
								feature,
								localSite.getFeatureStatus(feature));
						switch (code) {
							case IFeature.STATUS_UNHAPPY :
								flags |= UpdateLabelProvider.F_ERROR;
								break;
							case IFeature.STATUS_AMBIGUOUS :
								flags |= UpdateLabelProvider.F_WARNING;
								break;
							default :
								if (adapter.isConfigured()
									&& adapter.isUpdated())
									flags |= UpdateLabelProvider.F_UPDATED;
								break;
						}
					}
				}
				return provider.get(baseDesc, flags);
			} catch (CoreException e) {
				return provider.get(
					UpdateUIImages.DESC_FEATURE_OBJ,
					UpdateLabelProvider.F_ERROR);
			}
		}
	}

	class PreviewTask implements IPreviewTask {
		private String name;
		private String desc;
		private IAction action;
		public PreviewTask(String name, String desc, IAction action) {
			this.name = name;
			this.desc = desc;
			this.action = action;
		}
		public IAction getAction() {
			return action;
		}
		public String getName() {
			if (name != null)
				return name;
			return action.getText();
		}
		public String getDescription() {
			return desc;
		}
		public void setDescription(String desc) {
			this.desc = desc;
		}
		public void run() {
			action.run();
		}
		public boolean isEnabled() {
			return action.isEnabled();
		}
	}

	public ConfigurationView(ConfigurationManagerWindow window) {
		UpdateUI.getDefault().getLabelProvider().connect(this);
		initializeImages();
		configurationWindow=window;
	}

	private void initializeImages() {
		ImageDescriptor edesc = UpdateUIImages.DESC_APP_OBJ;
		IProduct product = Platform.getProduct();
		if (product != null) {
			String windowImageURL = product.getProperty(IProductConstants.WINDOW_IMAGE);
			if (windowImageURL == null) {
				String windowImagesUrls = product.getProperty(IProductConstants.WINDOW_IMAGES);
				if (windowImagesUrls != null ) {
					StringTokenizer st = new StringTokenizer(windowImagesUrls, ","); //$NON-NLS-1$
					if (st.hasMoreTokens())
						windowImageURL = st.nextToken();
				}
			}
			if (windowImageURL != null)
				try {
					edesc = ImageDescriptor.createFromURL(new URL(windowImageURL));
				} catch (MalformedURLException e) {
					// must be a path relative to the product bundle
					Bundle productBundle = product.getDefiningBundle();
					if (productBundle != null) { 
						URL url = Platform.find(productBundle, new Path(windowImageURL));
						if (url != null)
							edesc = ImageDescriptor.createFromURL(url);
					}
				}
		}
		eclipseImage = UpdateUI.getDefault().getLabelProvider().get(edesc);
	}

	public void initProviders() {
		treeViewer.setContentProvider(new LocalSiteProvider());
		treeViewer.setLabelProvider(new LocalSiteLabelProvider());
		treeViewer.setInput(UpdateUI.getDefault().getUpdateModel());
		treeViewer.setSorter(new ConfigurationSorter());
		ILocalSite localSite = getLocalSite();
		if (localSite != null)
			localSite.addLocalSiteChangedListener(this);

		modelListener = new IUpdateModelChangedListener() {
			public void objectsAdded(Object parent, Object[] children) {
			}
			public void objectsRemoved(Object parent, Object[] children) {
			}
			public void objectChanged(final Object obj, String property) {
				if (refreshLock)
					return;
				Control control = getControl();
				if (!control.isDisposed()) {
					control.getDisplay().asyncExec(new Runnable() {
						public void run() {
							treeViewer.refresh();
							handleSelectionChanged(
								(IStructuredSelection) treeViewer
									.getSelection());
						}
					});
				}
			}
		};
		OperationsManager.addUpdateModelChangedListener(modelListener);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(
			getControl(),
			"org.eclipse.update.ui.ConfigurationView"); //$NON-NLS-1$
	}

	private ILocalSite getLocalSite() {
		try {
			return SiteManager.getLocalSite();
		} catch (CoreException e) {
			UpdateUI.logException(e);
			return null;
		}
	}

	private Object[] openLocalSite() {
		final Object[][] bag = new Object[1][];
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				ILocalSite localSite = getLocalSite();
				if (localSite == null)
					return;
				IInstallConfiguration config =
					getLocalSite().getCurrentConfiguration();
				IConfiguredSite[] sites = config.getConfiguredSites();
				Object[] result = new Object[sites.length];
				for (int i = 0; i < sites.length; i++) {
					result[i] = new ConfiguredSiteAdapter(config, sites[i]);
				}
				if (!initialized) {
					config.addInstallConfigurationChangedListener(
						ConfigurationView.this);
					initialized = true;
				}
				bag[0] = result;
			}
		});
		return bag[0];
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		if (initialized) {
			ILocalSite localSite = getLocalSite();
			if (localSite != null) {
				localSite.removeLocalSiteChangedListener(this);
				IInstallConfiguration config =
					localSite.getCurrentConfiguration();
				config.removeInstallConfigurationChangedListener(this);
			}
			initialized = false;
		}
		OperationsManager.removeUpdateModelChangedListener(modelListener);
		if (preview != null)
			preview.dispose();
		//super.dispose();
	}

	protected void makeActions() {
		collapseAllAction = new Action() {
			public void run() {
				treeViewer.getControl().setRedraw(false);
				treeViewer.collapseToLevel(
					treeViewer.getInput(),
					TreeViewer.ALL_LEVELS);
				treeViewer.getControl().setRedraw(true);
			}
		};
		collapseAllAction.setText(UpdateUIMessages.ConfigurationView_collapseLabel); 
		collapseAllAction.setToolTipText(UpdateUIMessages.ConfigurationView_collapseTooltip); 
		collapseAllAction.setImageDescriptor(UpdateUIImages.DESC_COLLAPSE_ALL);

		drillDownAdapter = new DrillDownAdapter(treeViewer);

		featureStateAction = new FeatureStateAction(this);

		siteStateAction = new SiteStateAction(getConfigurationWindow().getShell());

		revertAction = new RevertConfigurationAction(getConfigurationWindow().getShell(),UpdateUIMessages.ConfigurationView_revertLabel); 
        PlatformUI.getWorkbench().getHelpSystem().setHelp(
			revertAction,
			"org.eclipse.update.ui.CofigurationView_revertAction"); //$NON-NLS-1$

		installationHistoryAction =
			new InstallationHistoryAction(getConfigurationWindow().getShell(),
				UpdateUIMessages.ConfigurationView_installHistory, 
				UpdateUIImages.DESC_HISTORY_OBJ);
		installationHistoryAction.setToolTipText(installationHistoryAction.getText());
		
		newExtensionLocationAction =
			new NewExtensionLocationAction(getConfigurationWindow().getShell(),
				UpdateUIMessages.ConfigurationView_extLocation, 
				UpdateUIImages.DESC_ESITE_OBJ);
		
		propertiesAction =
			new PropertyDialogAction(
				getConfigurationWindow().getShell(),
				treeViewer);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(
			propertiesAction,
			"org.eclipse.update.ui.CofigurationView_propertiesAction"); //$NON-NLS-1$

		uninstallFeatureAction = new UninstallFeatureAction(getConfigurationWindow().getShell(), UpdateUIMessages.ConfigurationView_uninstall); 

		installOptFeatureAction =
			new InstallOptionalFeatureAction(
				getControl().getShell(),
				UpdateUIMessages.ConfigurationView_install); 

		swapVersionAction = new ReplaceVersionAction(getConfigurationWindow().getShell(), UpdateUIMessages.ConfigurationView_anotherVersion); 

		findUpdatesAction =
			new FindUpdatesAction(getControl().getShell(), UpdateUIMessages.ConfigurationView_findUpdates); 

		showActivitiesAction = new ShowActivitiesAction(getControl().getShell(), UpdateUIMessages.ConfigurationView_showActivitiesLabel); 
        PlatformUI.getWorkbench().getHelpSystem().setHelp(
			showActivitiesAction,
			"org.eclipse.update.ui.ConfigurationView_showActivitiesAction"); //$NON-NLS-1$

		makeShowUnconfiguredFeaturesAction();
		makeShowSitesAction();
		makeShowNestedFeaturesAction();
		makePreviewTasks();
		configurationWindow.setPropertiesActionHandler(propertiesAction);
	}

	private void makeShowNestedFeaturesAction() {
		final Preferences pref = UpdateUI.getDefault().getPluginPreferences();
		pref.setDefault(STATE_SHOW_NESTED_FEATURES, true);
		showNestedFeaturesAction = new Action() {
			public void run() {
				treeViewer.refresh();
				pref.setValue(
					STATE_SHOW_NESTED_FEATURES,
					showNestedFeaturesAction.isChecked());
			}
		};
		showNestedFeaturesAction.setText(UpdateUIMessages.ConfigurationView_showNestedFeatures); 
		showNestedFeaturesAction.setImageDescriptor(
			UpdateUIImages.DESC_SHOW_HIERARCHY);
		showNestedFeaturesAction.setDisabledImageDescriptor(
			UpdateUIImages.DESC_SHOW_HIERARCHY_D);

		showNestedFeaturesAction.setChecked(
			pref.getBoolean(STATE_SHOW_NESTED_FEATURES));
		showNestedFeaturesAction.setToolTipText(UpdateUIMessages.ConfigurationView_showNestedTooltip); 
	}

	private void makeShowSitesAction() {
		final Preferences pref = UpdateUI.getDefault().getPluginPreferences();
		pref.setDefault(STATE_SHOW_SITES, true);
		showSitesAction = new Action() {
			public void run() {
				treeViewer.refresh();
				pref.setValue(STATE_SHOW_SITES, showSitesAction.isChecked());
				UpdateUI.getDefault().savePluginPreferences();
			}
		};
		showSitesAction.setText(UpdateUIMessages.ConfigurationView_showInstall); 
		showSitesAction.setImageDescriptor(UpdateUIImages.DESC_LSITE_OBJ);
		showSitesAction.setChecked(pref.getBoolean(STATE_SHOW_SITES));
		showSitesAction.setToolTipText(UpdateUIMessages.ConfigurationView_showInstallTooltip); 
	}

	private void makeShowUnconfiguredFeaturesAction() {
		final Preferences pref = UpdateUI.getDefault().getPluginPreferences();
		pref.setDefault(STATE_SHOW_UNCONF, false);
		showUnconfFeaturesAction = new Action() {
			public void run() {
				pref.setValue(
					STATE_SHOW_UNCONF,
					showUnconfFeaturesAction.isChecked());
				UpdateUI.getDefault().savePluginPreferences();
				treeViewer.refresh();
			}
		};
        PlatformUI.getWorkbench().getHelpSystem().setHelp(
			showUnconfFeaturesAction,
			"org.eclipse.update.ui.CofigurationView_showUnconfFeaturesAction"); //$NON-NLS-1$
		showUnconfFeaturesAction.setText(UpdateUIMessages.ConfigurationView_showDisabled); 
		showUnconfFeaturesAction.setImageDescriptor(
			UpdateUIImages.DESC_UNCONF_FEATURE_OBJ);
		showUnconfFeaturesAction.setChecked(pref.getBoolean(STATE_SHOW_UNCONF));
		showUnconfFeaturesAction.setToolTipText(UpdateUIMessages.ConfigurationView_showDisabledTooltip); 
	}

	protected void fillActionBars(ToolBarManager tbm) {
		tbm.add(showSitesAction);
		tbm.add(showNestedFeaturesAction);
		tbm.add(showUnconfFeaturesAction);
		tbm.add(new Separator());
		drillDownAdapter.addNavigationActions(tbm);
		tbm.add(new Separator());
		tbm.add(collapseAllAction);
		tbm.add(new Separator());
		tbm.add(installationHistoryAction);
	}

	protected Object getSelectedObject() {
		ISelection selection = treeViewer.getSelection();
		if (selection instanceof IStructuredSelection
			&& !selection.isEmpty()) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1)
				return ssel.getFirstElement();
		}
		return null;
	}

	protected void fillContextMenu(IMenuManager manager) {
		Object obj = getSelectedObject();

		if (obj instanceof ILocalSite) {
			manager.add(findUpdatesAction);
			manager.add(revertAction);
		} else if (obj instanceof IConfiguredSiteAdapter) {
			manager.add(siteStateAction);
		}

		if (obj instanceof ILocalSite
			|| obj instanceof IConfiguredSiteAdapter) {
			manager.add(new Separator());
			MenuManager mgr = new MenuManager(UpdateUIMessages.ConfigurationView_new); 
			mgr.add(newExtensionLocationAction);
			manager.add(mgr);
			manager.add(new Separator());
		} else if (obj instanceof ConfiguredFeatureAdapter) {
			try {
				MenuManager mgr = new MenuManager(UpdateUIMessages.ConfigurationView_replaceWith); 
				
				manager.add(findUpdatesAction);
				manager.add(new Separator());
				
				mgr.add(swapVersionAction);
				manager.add(mgr);

				manager.add(featureStateAction);

				IFeature feature =
					((ConfiguredFeatureAdapter) obj).getFeature(null);
				if (feature instanceof MissingFeature) {
					manager.add(installOptFeatureAction);
				} else {
					manager.add(uninstallFeatureAction);
				}
				manager.add(new Separator());

			} catch (CoreException e) {
			}
		}

		drillDownAdapter.addNavigationActions(manager);

		if (obj instanceof ILocalSite) {
			manager.add(new Separator());
			manager.add(installationHistoryAction);
		}

		if (obj instanceof IFeatureAdapter
			|| obj instanceof ILocalSite
			|| obj instanceof IConfiguredSiteAdapter) {
			manager.add(new Separator());
			manager.add(propertiesAction);
		}
	}

	public void installSiteAdded(IConfiguredSite csite) {
		asyncRefresh();
	}
	public void installSiteRemoved(IConfiguredSite site) {
		asyncRefresh();
	}
	public void featureInstalled(IFeature feature) {
		asyncRefresh();
	}
	public void featureRemoved(IFeature feature) {
		asyncRefresh();
	}
	public void featureConfigured(IFeature feature) {
	}

	public void featureUnconfigured(IFeature feature) {
	}

	public void currentInstallConfigurationChanged(IInstallConfiguration configuration) {
		asyncRefresh();
	}

	public void installConfigurationRemoved(IInstallConfiguration configuration) {
		asyncRefresh();
	}

	private void asyncRefresh() {
		Display display = SWTUtil.getStandardDisplay();
		if (display == null)
			return;
		if (getControl().isDisposed())
			return;
		display.asyncExec(new Runnable() {
			public void run() {
				if (!getControl().isDisposed())
					treeViewer.refresh();
			}
		});
	}

	private Object[] getFeatures(
		final IConfiguredSiteAdapter siteAdapter,
		final boolean configuredOnly) {
		final IConfiguredSite csite = siteAdapter.getConfiguredSite();
		final Object[][] bag = new Object[1][];
		refreshLock = true;

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				ArrayList result = new ArrayList();
				IFeatureReference[] refs;

				if (configuredOnly)
					refs = csite.getConfiguredFeatures();
				else {
					ISite site = csite.getSite();
					refs = site.getFeatureReferences();
				}
				monitor.beginTask(
					UpdateUIMessages.ConfigurationView_loading, 
					refs.length);

				for (int i = 0; i < refs.length; i++) {
					IFeatureReference ref = refs[i];
					IFeature feature;
					try {
						monitor.subTask(ref.getURL().toString());
						feature = ref.getFeature(null);
					} catch (CoreException e) {
						feature =
							new MissingFeature(ref.getSite(), ref.getURL());
					}
					monitor.worked(1);
					result.add(
						new ConfiguredFeatureAdapter(
							siteAdapter,
							feature,
							csite.isConfigured(feature),
							false,
							false));
				}
				monitor.done();
				bag[0] = getRootFeatures(result);
			}
		};

		try {
			if (configurationWindow.getShell().isVisible())
				configurationWindow.run(true, false, op);
			else
				op.run(new NullProgressMonitor());
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
		} finally {
			refreshLock = false;
		}
		return bag[0];
	}

	private Object[] getRootFeatures(ArrayList list) {
		ArrayList children = new ArrayList();
		ArrayList result = new ArrayList();
		try {
			for (int i = 0; i < list.size(); i++) {
				ConfiguredFeatureAdapter cf =
					(ConfiguredFeatureAdapter) list.get(i);
				IFeature feature = cf.getFeature(null);
				if (feature != null)
					addChildFeatures(
						feature,
						children,
						cf.isConfigured());
			}
			for (int i = 0; i < list.size(); i++) {
				ConfiguredFeatureAdapter cf =
					(ConfiguredFeatureAdapter) list.get(i);
				IFeature feature = cf.getFeature(null);
				if (feature != null
					&& isChildFeature(feature, children) == false)
					result.add(cf);
			}
		} catch (CoreException e) {
			return list.toArray();
		}
		return result.toArray();
	}

	private void addChildFeatures(
		IFeature feature,
		ArrayList children,
		boolean configured) {
		try {
			IIncludedFeatureReference[] included =
				feature.getIncludedFeatureReferences();
			for (int i = 0; i < included.length; i++) {
				IFeature childFeature;
				try {
					childFeature =
						included[i].getFeature(null);
				} catch (CoreException e) {
					childFeature = new MissingFeature(included[i]);
				}
				children.add(childFeature);
			}
		} catch (CoreException e) {
			UpdateUI.logException(e);
		}
	}

	private boolean isChildFeature(IFeature feature, ArrayList children) {
		for (int i = 0; i < children.size(); i++) {
			IFeature child = (IFeature) children.get(i);
			if (feature
				.getVersionedIdentifier()
				.equals(child.getVersionedIdentifier()))
				return true;
		}
		return false;
	}

	protected void handleDoubleClick(DoubleClickEvent e) {
		if (e.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) e.getSelection();
			Object obj = ssel.getFirstElement();
			if (obj!=null)
				propertiesAction.run();
		}
	}

	public void createPartControl(Composite parent) {
		splitter = new SashForm(parent, SWT.HORIZONTAL);
		splitter.setLayoutData(new GridData(GridData.FILL_BOTH));
		Composite leftContainer = createLineContainer(splitter);
		Composite rightContainer = createLineContainer(splitter);
		createTreeViewer(leftContainer);
		makeActions();
		createVerticalLine(leftContainer);
		createVerticalLine(rightContainer);
		preview = new ConfigurationPreview(this);
		preview.createControl(rightContainer);
		preview.getControl().setLayoutData(
			new GridData(GridData.FILL_BOTH));
		splitter.setWeights(new int[] { 2, 3 });
		fillActionBars(getConfigurationWindow().getToolBarManager());

		treeViewer.expandToLevel(2);
	}

	private void createTreeViewer(Composite parent) {
		treeViewer =
			new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.setUseHashlookup(true);
		initProviders();

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(new GroupMarker("additions")); //$NON-NLS-1$
				fillContextMenu(manager);
			}
		});

		treeViewer.getControl().setMenu(
			menuMgr.createContextMenu(treeViewer.getControl()));

		treeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged(event);
			}
		});

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(event);
			}
		});

	}

	public TreeViewer getTreeViewer() {
		return treeViewer;
	}

	private Composite createLineContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		container.setLayout(layout);
		return container;
	}

	private void createVerticalLine(Composite parent) {
		Label line = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_FILL);
		gd.widthHint = 1;
		line.setLayoutData(gd);
	}

	public Control getControl() {
		return splitter;
	}

	private int getStatusCode(IFeature feature, IStatus status) {
		int code = status.getCode();
		if (code == IFeature.STATUS_UNHAPPY) {
			if (status.isMultiStatus()) {
				IStatus[] children = status.getChildren();
				for (int i = 0; i < children.length; i++) {
					IStatus child = children[i];
					if (child.isMultiStatus()
						|| child.getCode() != IFeature.STATUS_DISABLED)
						return code;
				}
				// If we are here, global status is unhappy
				// because one or more included features
				// is disabled.
				if (UpdateUtils.hasObsoletePatches(feature)) {
					// The disabled included features
					// are old patches that are now
					// subsumed by better versions of
					// the features they were designed to
					// patch.
					return IFeature.STATUS_HAPPY;
				}
			}
		}
		return code;
	}

	protected void handleSelectionChanged(IStructuredSelection ssel) {
		Object obj = ssel.getFirstElement();
		if (obj instanceof IFeatureAdapter) {
			try {
				propertiesAction.setEnabled(true);
				ConfiguredFeatureAdapter adapter = (ConfiguredFeatureAdapter) obj;
				IFeature feature = adapter.getFeature(null);

				boolean missing = feature instanceof MissingFeature;
				boolean enable = !missing
						&& ((adapter.isOptional() || !adapter.isIncluded()));

				uninstallFeatureAction.setFeature(adapter);
				uninstallFeatureAction.setEnabled(enable
						&& uninstallFeatureAction.canUninstall());
				if (adapter.isConfigured())
					setDescriptionOnTask(
							uninstallFeatureAction,
							adapter,
							UpdateUIMessages.ConfigurationView_uninstallDesc2);
				else
					setDescriptionOnTask(
							uninstallFeatureAction,
							adapter,
							UpdateUIMessages.ConfigurationView_uninstallDesc);

				featureStateAction.setFeature(adapter);
				featureStateAction.setEnabled(enable);
				swapVersionAction.setEnabled(false);
				if (enable) {
					IFeature[] features = UpdateUtils.getInstalledFeatures(
							feature, false);
					if (features.length > 1) {
						if (adapter.isConfigured()) {
							// We only enable replace action if configured
							// and selected. bug 74019
							swapVersionAction.setEnabled(true);
							swapVersionAction.setCurrentFeature(feature);
							swapVersionAction.setFeatures(features);
						} else {
							// If we are not configured and another version is
							// we want to disable StateAction. bug 74019
							features = UpdateUtils.getInstalledFeatures(
									feature, true);
							if (features.length > 0) {
								featureStateAction.setEnabled(false);
							}
						}
					}
				}

				findUpdatesAction.setEnabled(false);
				if (enable && adapter.isConfigured()) {
					if (feature.getUpdateSiteEntry() != null) {
						findUpdatesAction.setFeature(feature);
						findUpdatesAction.setEnabled(true);
					}
				}
				
				if (missing) {
					MissingFeature mf = (MissingFeature) feature;
					installOptFeatureAction.setEnabled(mf.isOptional()
							&& mf.getOriginatingSiteURL() != null);
					installOptFeatureAction.setFeature(mf);
				} else {
					installOptFeatureAction.setEnabled(false);
				}
			} catch (CoreException ex) {
				UpdateUI.logException(ex);
			}
		}
		if (obj instanceof ILocalSite) {
			propertiesAction.setEnabled(true);
			findUpdatesAction.setEnabled(true);
			findUpdatesAction.setFeature(null);
			ILocalSite site = getLocalSite();
			revertAction.setEnabled(site != null
					&& site.getConfigurationHistory().length > 1);
		} else if (obj instanceof IConfiguredSiteAdapter) {
			siteStateAction.setSite(((IConfiguredSiteAdapter) obj)
					.getConfiguredSite());
			siteStateAction.setEnabled(true);
		}
		preview.setSelection(ssel);
	}

	protected void handleSelectionChanged(SelectionChangedEvent e) {
		handleSelectionChanged(((IStructuredSelection) e.getSelection()));
	}

	private void setDescriptionOnTask(IAction action, ConfiguredFeatureAdapter adapter, String desc) {
		IPreviewTask[] tasks = getPreviewTasks(adapter);
		if (tasks == null)
			return;
		for (int i=0; i<tasks.length; i++)
			if (tasks[i].getAction() == action)
				tasks[i].setDescription(desc);
	}
	
	private void makePreviewTasks() {
		previewTasks = new Hashtable();
		Class key;
		ArrayList array = new ArrayList();
		// local site tasks
		key = ILocalSite.class;
		array.add(
			new PreviewTask(
				UpdateUIMessages.ConfigurationView_updateLabel, 
				UpdateUIMessages.ConfigurationView_updateDesc, 
				findUpdatesAction));
		array.add(
			new PreviewTask(
				UpdateUIMessages.ConfigurationView_installHistLabel, 
				UpdateUIMessages.ConfigurationView_installHistDesc, 
				installationHistoryAction));
		array.add(
			new PreviewTask(
				UpdateUIMessages.ConfigurationView_activitiesLabel, 
				UpdateUIMessages.ConfigurationView_activitiesDesc, 
				showActivitiesAction));
		array.add(
				new PreviewTask(
					UpdateUIMessages.ConfigurationView_extLocLabel, 
					UpdateUIMessages.ConfigurationView_extLocDesc, 
					newExtensionLocationAction));
		array.add(
				new PreviewTask(
					UpdateUIMessages.ConfigurationView_revertPreviousLabel, 
					UpdateUIMessages.ConfigurationView_revertPreviousDesc, 
					revertAction));

		previewTasks.put(key, array.toArray(new IPreviewTask[array.size()]));

		// configured site tasks
		array.clear();
		key = IConfiguredSiteAdapter.class;
		array.add(
			new PreviewTask(
				null,
				UpdateUIMessages.ConfigurationView_enableLocDesc, 
				siteStateAction));
		array.add(
			new PreviewTask(
				UpdateUIMessages.ConfigurationView_extLocLabel, 
				UpdateUIMessages.ConfigurationView_extLocDesc, 
				newExtensionLocationAction));
		array.add(
			new PreviewTask(
				UpdateUIMessages.ConfigurationView_propertiesLabel, 
				UpdateUIMessages.ConfigurationView_installPropDesc, 
				propertiesAction));
		previewTasks.put(key, array.toArray(new IPreviewTask[array.size()]));

		// feature adapter tasks
		array.clear();
		key = IFeatureAdapter.class;
		array.add(
				new PreviewTask(
					UpdateUIMessages.ConfigurationView_scanLabel, 
					UpdateUIMessages.ConfigurationView_scanDesc, 
					findUpdatesAction));
		array.add(
			new PreviewTask(
				UpdateUIMessages.ConfigurationView_replaceVersionLabel, 
				UpdateUIMessages.ConfigurationView_replaceVersionDesc, 
				swapVersionAction));
		array.add(
			new PreviewTask(
				null,
				UpdateUIMessages.ConfigurationView_enableFeatureDesc, 
				featureStateAction));
		array.add(
			new PreviewTask(
				UpdateUIMessages.ConfigurationView_installOptionalLabel, 
				UpdateUIMessages.ConfigurationView_installOptionalDesc, 
				installOptFeatureAction));
		array.add(
			new PreviewTask(
				UpdateUIMessages.ConfigurationView_uninstallLabel, 
				UpdateUIMessages.ConfigurationView_uninstallDesc, 
				uninstallFeatureAction));
		array.add(
			new PreviewTask(
				UpdateUIMessages.ConfigurationView_featurePropLabel, 
				UpdateUIMessages.ConfigurationView_featurePropDesc, 
				propertiesAction));
		previewTasks.put(key, array.toArray(new IPreviewTask[array.size()]));
	}

	public IPreviewTask[] getPreviewTasks(Object object) {
		IPreviewTask[] tasks = null;

		if (object instanceof IFeatureAdapter)
			tasks = (IPreviewTask[]) previewTasks.get(IFeatureAdapter.class);
		if (object instanceof ILocalSite)
			tasks = (IPreviewTask[]) previewTasks.get(ILocalSite.class);
		if (object instanceof IConfiguredSiteAdapter)
			tasks =
				(IPreviewTask[]) previewTasks.get(IConfiguredSiteAdapter.class);
		return (tasks != null) ? tasks : new IPreviewTask[0];
	}

	ConfigurationManagerWindow getConfigurationWindow(){
		return configurationWindow;
	}

}
