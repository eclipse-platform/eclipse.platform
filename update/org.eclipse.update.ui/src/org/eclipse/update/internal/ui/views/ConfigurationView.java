package org.eclipse.update.internal.ui.views;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IConfiguredSiteChangedListener;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.IInstallConfigurationChangedListener;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.configuration.ILocalSiteChangedListener;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.UpdateUIPluginImages;
import org.eclipse.update.internal.ui.manager.RevertSection;
import org.eclipse.update.internal.ui.model.ConfigurationSiteAdapter;
import org.eclipse.update.internal.ui.model.ConfiguredFeatureAdapter;
import org.eclipse.update.internal.ui.model.IConfiguredFeatureAdapter;
import org.eclipse.update.internal.ui.model.IConfiguredSiteAdapter;
import org.eclipse.update.internal.ui.model.IFeatureAdapter;
import org.eclipse.update.internal.ui.model.IUpdateModelChangedListener;
import org.eclipse.update.internal.ui.model.MissingFeature;
import org.eclipse.update.internal.ui.model.PreservedConfiguration;
import org.eclipse.update.internal.ui.model.UpdateModel;
import org.eclipse.update.internal.ui.parts.DefaultContentProvider;
import org.eclipse.update.internal.ui.parts.OverlayIcon;
import org.eclipse.update.internal.ui.model.ModelObject;

/**
 * Insert the type's description here.
 * @see ViewPart
 */
public class ConfigurationView
	extends BaseTreeView
	implements
		IInstallConfigurationChangedListener,
		IConfiguredSiteChangedListener,
		ILocalSiteChangedListener {
	private static final String KEY_CURRENT = "ConfigurationView.current";
	private static final String KEY_SHOW_UNCONF_FEATURES =
		"ConfigurationView.showUnconfFeatures";
	private static final String KEY_SHOW_UNCONF_FEATURES_TOOLTIP =
		"ConfigurationView.showUnconfFeatures.tooltip";
	private Image eclipseImage;
	private Image featureImage;
	private Image unconfFeatureImage;
	private Image siteImage;
	private Image installSiteImage;
	private Image linkedSiteImage;
	private Image configImage;
	private Image currentConfigImage;
	private boolean initialized;
	private SavedFolder savedFolder;
	private HistoryFolder historyFolder;
	private Action showUnconfFeaturesAction;
	private Action revertAction;
	private Action preserveAction;
	private Action removePreservedAction;
	private Action propertiesAction;
	private IUpdateModelChangedListener modelListener;
	private DrillDownAdapter drillDownAdapter;
	private static final String KEY_RESTORE = "ConfigurationView.Popup.restore";
	private static final String KEY_PRESERVE = "ConfigurationView.Popup.preserve";
	private static final String KEY_REMOVE_PRESERVED =
		"ConfigurationView.Popup.removePreserved";
	private static final String KEY_HISTORY_FOLDER = "ConfigurationView.historyFolder";
	private static final String KEY_SAVED_FOLDER = "ConfigurationView.savedFolder";

	abstract class ViewFolder extends ModelObject {
		private String label;
		private Image image;

		public ViewFolder(String label) {
			this.label = label;
			String imageKey = ISharedImages.IMG_OBJ_FOLDER;
			image = PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}

		public Image getImage() {
			return image;
		}

		public String toString() {
			return label;
		}
		public abstract Object[] getChildren();
	}

	class SavedFolder extends ViewFolder {
		public SavedFolder() {
			super(UpdateUIPlugin.getResourceString(KEY_SAVED_FOLDER));
		}
		public Object[] getChildren() {
			try {
				ILocalSite localSite = SiteManager.getLocalSite();
				return makeChildren(localSite.getPreservedConfigurations());
			} catch (CoreException e) {
				return new Object[0];
			}
		}

		private Object[] makeChildren(IInstallConfiguration[] preserved) {
			Object[] children = new Object[preserved.length];
			for (int i = 0; i < preserved.length; i++) {
				children[i] = new PreservedConfiguration(preserved[i]);
			}
			return children;
		}
	}

	class HistoryFolder extends ViewFolder {
		public HistoryFolder() {
			super(UpdateUIPlugin.getResourceString(KEY_HISTORY_FOLDER));
		}
		public Object[] getChildren() {
			try {
				ILocalSite localSite = SiteManager.getLocalSite();
				return localSite.getConfigurationHistory();
			} catch (CoreException e) {
				return new Object[0];
			}
		}
	}

	class UpdateModelChangedListener implements IUpdateModelChangedListener {
		/**
		 * @see IUpdateModelChangedListener#objectAdded(Object, Object)
		 */
		public void objectsAdded(Object parent, Object[] children) {
		}

		/**
		 * @see IUpdateModelChangedListener#objectRemoved(Object, Object)
		 */
		public void objectsRemoved(Object parent, Object[] children) {
		}

		/**
		 * @see IUpdateModelChangedListener#objectChanged(Object, String)
		 */
		public void objectChanged(Object object, String property) {
		}

	}

	class LocalSiteProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		/**
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parent) {
			if (parent instanceof UpdateModel) {
				UpdateModel model = (UpdateModel) parent;
				ILocalSite localSite = getLocalSite();
				if (localSite != null)
					return new Object[] { getLocalSite(), historyFolder, savedFolder };
				else
					return new Object[0];
			}
			if (parent instanceof ILocalSite) {
				return openLocalSite();
			}
			if (parent instanceof ViewFolder) {
				return ((ViewFolder) parent).getChildren();
			}
			if (parent instanceof PreservedConfiguration) {
				// resolve the adapter
				parent = ((PreservedConfiguration) parent).getConfiguration();
			}
			if (parent instanceof IInstallConfiguration) {
				return getConfigurationSites((IInstallConfiguration) parent);
			}
			if (parent instanceof IConfiguredSiteAdapter) {
				IConfiguredSiteAdapter adapter = (IConfiguredSiteAdapter) parent;
				boolean showUnconf = showUnconfFeaturesAction.isChecked();
				if (showUnconf)
					return getAllFeatures(adapter);
				else
					return getConfiguredFeatures(adapter);
			}
			return new Object[0];
		}

		private Object[] getConfigurationSites(IInstallConfiguration config) {
			IConfiguredSite[] sites = config.getConfiguredSites();
			Object[] adapters = new Object[sites.length];
			for (int i = 0; i < sites.length; i++) {
				adapters[i] = new ConfigurationSiteAdapter(config, sites[i]);
			}
			return adapters;
		}

		private Object[] getConfiguredFeatures(IConfiguredSiteAdapter adapter) {
			try {
				IConfiguredSite csite = adapter.getConfigurationSite();
				IFeatureReference[] refs = csite.getConfiguredFeatures();
				Object[] result = new Object[refs.length];
				for (int i = 0; i < refs.length; i++) {
					IFeature feature = refs[i].getFeature();
					result[i] = new ConfiguredFeatureAdapter(adapter, feature, true);
				}
				return result;
			} catch (CoreException e) {
				UpdateUIPlugin.logException(e);
				return new Object[0];
			}
		}

		private Object[] getAllFeatures(IConfiguredSiteAdapter adapter) {
			IConfiguredSite csite = adapter.getConfigurationSite();
			IFeatureReference[] crefs = csite.getConfiguredFeatures();
			ISite site = csite.getSite();
			IFeatureReference[] allRefs = site.getFeatureReferences();
			Object[] result = new Object[allRefs.length];

			for (int i = 0; i < allRefs.length; i++) {
				IFeature feature;
				try {
					feature = allRefs[i].getFeature();
				} catch (CoreException e) {
					feature = new MissingFeature(site, allRefs[i].getURL());
				}
				boolean configured = isOnTheList(allRefs[i], crefs);
				result[i] = new ConfiguredFeatureAdapter(adapter, feature, configured);
			}
			return result;
		}

		private boolean isOnTheList(IFeatureReference ref, IFeatureReference[] refs) {
			for (int i = 0; i < refs.length; i++) {
				IFeatureReference confRef = refs[i];
				if (confRef.getURL().equals(ref.getURL()))
					return true;
			}
			return false;
		}

		/**
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object child) {
			return null;
		}

		/**
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object parent) {
			return !(parent instanceof IFeatureAdapter);
		}

		/**
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object input) {
			return getChildren(input);
		}
	}

	class LocalSiteLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof ILocalSite) {
				return UpdateUIPlugin.getResourceString(KEY_CURRENT);
			}
			if (obj instanceof IInstallConfiguration) {
				IInstallConfiguration config = (IInstallConfiguration) obj;
				return config.getLabel();
			}
			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite = ((IConfiguredSiteAdapter) obj).getConfigurationSite();
				ISite site = csite.getSite();
				return site.getURL().toString();
			}
			if (obj instanceof IFeatureAdapter) {
				try {
					IFeature feature = ((IFeatureAdapter) obj).getFeature();
					String version = feature.getVersionedIdentifier().getVersion().toString();
					return feature.getLabel() + " " + version;
				} catch (CoreException e) {
					return "Error";
				}
			}
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			if (obj instanceof ILocalSite)
				return eclipseImage;
			if (obj instanceof IFeatureAdapter) {
				return getFeatureImage((IFeatureAdapter) obj);
			}
			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite = ((IConfiguredSiteAdapter) obj).getConfigurationSite();
				if (csite.isUpdateable())
					return installSiteImage;
				else
					return linkedSiteImage;
			}
			if (obj instanceof ViewFolder) {
				return ((ViewFolder) obj).getImage();
			}
			if (obj instanceof PreservedConfiguration) {
				obj = ((PreservedConfiguration) obj).getConfiguration();
			}
			if (obj instanceof IInstallConfiguration) {
				IInstallConfiguration config = (IInstallConfiguration) obj;
				if (config.isCurrent())
					return currentConfigImage;
				return configImage;
			}
			return null;
		}

		private Image getFeatureImage(IFeatureAdapter adapter) {
			boolean configured = true;
			if (adapter instanceof IConfiguredFeatureAdapter) {
				configured = ((IConfiguredFeatureAdapter) adapter).isConfigured();
			}
			return (configured ? featureImage : unconfFeatureImage);
		}
	}

	public ConfigurationView() {
		eclipseImage = UpdateUIPluginImages.DESC_ECLIPSE_OBJ.createImage();
		featureImage = UpdateUIPluginImages.DESC_FEATURE_OBJ.createImage();
		unconfFeatureImage = UpdateUIPluginImages.DESC_UNCONF_FEATURE_OBJ.createImage();
		ImageDescriptor siteDesc = UpdateUIPluginImages.DESC_SITE_OBJ;
		siteImage = siteDesc.createImage();
		ImageDescriptor installSiteDesc =
			new OverlayIcon(
				siteDesc,
				new ImageDescriptor[][] { { UpdateUIPluginImages.DESC_INSTALLABLE_CO }
		});
		installSiteImage = installSiteDesc.createImage();
		ImageDescriptor linkedSiteDesc =
			new OverlayIcon(
				siteDesc,
				new ImageDescriptor[][] { { UpdateUIPluginImages.DESC_LINKED_CO }
		});
		linkedSiteImage = linkedSiteDesc.createImage();
		configImage = UpdateUIPluginImages.DESC_CONFIG_OBJ.createImage();
		ImageDescriptor cdesc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_CONFIG_OBJ,
				new ImageDescriptor[][] { {
			}, {
				UpdateUIPluginImages.DESC_CURRENT_CO }
		});
		currentConfigImage = cdesc.createImage();
		savedFolder = new SavedFolder();
		historyFolder = new HistoryFolder();
	}

	public void initProviders() {
		viewer.setContentProvider(new LocalSiteProvider());
		viewer.setInput(UpdateUIPlugin.getDefault().getUpdateModel());
		viewer.setLabelProvider(new LocalSiteLabelProvider());
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			localSite.addLocalSiteChangedListener(this);
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
		}
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		modelListener = new IUpdateModelChangedListener() {
			public void objectsAdded(Object parent, Object[] children) {
			}
			public void objectsRemoved(Object parent, Object[] children) {
			}
			public void objectChanged(Object obj, String property) {
				viewer.update(obj, null);
			}
		};
		model.addUpdateModelChangedListener(modelListener);
	}

	private ILocalSite getLocalSite() {
		try {
			return SiteManager.getLocalSite();
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
			return null;
		}
	}

	private Object[] openLocalSite() {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			IConfiguredSite[] sites = config.getConfiguredSites();
			Object[] result = new Object[sites.length];
			for (int i = 0; i < sites.length; i++) {
				result[i] = new ConfigurationSiteAdapter(config, sites[i]);
			}
			if (!initialized) {
				config.addInstallConfigurationChangedListener(this);
				initialized = true;
			}
			return result;
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
			return new Object[0];
		}
	}

	public void dispose() {
		eclipseImage.dispose();
		featureImage.dispose();
		unconfFeatureImage.dispose();
		installSiteImage.dispose();
		linkedSiteImage.dispose();
		configImage.dispose();
		currentConfigImage.dispose();

		if (initialized) {
			try {
				ILocalSite localSite = SiteManager.getLocalSite();
				localSite.removeLocalSiteChangedListener(this);
				IInstallConfiguration config = localSite.getCurrentConfiguration();
				config.removeInstallConfigurationChangedListener(this);
			} catch (CoreException e) {
				UpdateUIPlugin.logException(e);
			}
			initialized = false;
		}
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.removeUpdateModelChangedListener(modelListener);
		super.dispose();
	}
	private Object getSelectedObject() {
		ISelection selection = viewer.getSelection();

		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1) {
				return ssel.getFirstElement();
			}
		}
		return null;
	}

	private IInstallConfiguration getSelectedConfiguration(
		Object obj,
		boolean onlyPreserved) {
		if (!onlyPreserved && obj instanceof IInstallConfiguration)
			return (IInstallConfiguration) obj;
		if (obj instanceof PreservedConfiguration)
			return ((PreservedConfiguration) obj).getConfiguration();
		return null;
	}

	private boolean isPreserved(IInstallConfiguration config) {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration[] preservedConfigs =
				localSite.getPreservedConfigurations();
			for (int i = 0; i < preservedConfigs.length; i++) {
				if (preservedConfigs[i].equals(config))
					return true;
			}
			return false;
		} catch (CoreException e) {
			return false;
		}
	}

	protected void makeActions() {
		super.makeActions();
		showUnconfFeaturesAction = new Action() {
			public void run() {
				viewer.refresh(getLocalSite());
			}
		};
		showUnconfFeaturesAction.setText(
			UpdateUIPlugin.getResourceString(KEY_SHOW_UNCONF_FEATURES));
		showUnconfFeaturesAction.setImageDescriptor(
			UpdateUIPluginImages.DESC_UNCONF_FEATURE_OBJ);
		showUnconfFeaturesAction.setChecked(false);
		showUnconfFeaturesAction.setToolTipText(
			UpdateUIPlugin.getResourceString(KEY_SHOW_UNCONF_FEATURES_TOOLTIP));
		drillDownAdapter = new DrillDownAdapter(viewer);
		super.makeActions();
		revertAction = new Action() {
			public void run() {
				Object obj = getSelectedObject();
				IInstallConfiguration target = getSelectedConfiguration(obj, false);
				if (target != null)
					RevertSection.performRevert(target);
			}
		};
		revertAction.setText(UpdateUIPlugin.getResourceString(KEY_RESTORE));
		preserveAction = new Action() {
			public void run() {
				Object obj = getSelectedObject();
				IInstallConfiguration target = getSelectedConfiguration(obj, false);
				if (target == null)
					return;
				try {
					ILocalSite localSite = SiteManager.getLocalSite();
					localSite.addToPreservedConfigurations(target);
					localSite.save();
					viewer.refresh(savedFolder);
				} catch (CoreException e) {
					UpdateUIPlugin.logException(e);
				}
			}
		};
		preserveAction.setText(UpdateUIPlugin.getResourceString(KEY_PRESERVE));
		removePreservedAction = new Action() {
			public void run() {
				Object obj = getSelectedObject();
				IInstallConfiguration target = getSelectedConfiguration(obj, true);
				if (target == null)
					return;
				if (isPreserved(target) == false)
					return;
				try {
					ILocalSite localSite = SiteManager.getLocalSite();
					localSite.removeFromPreservedConfigurations(target);
					localSite.save();
					viewer.remove(obj);
				} catch (CoreException e) {
					UpdateUIPlugin.logException(e);
				}
			}
		};
		removePreservedAction.setText(
			UpdateUIPlugin.getResourceString(KEY_REMOVE_PRESERVED));
		propertiesAction =
			new PropertyDialogAction(UpdateUIPlugin.getActiveWorkbenchShell(), viewer);
	}

	protected void fillActionBars(IActionBars bars) {
		IToolBarManager tbm = bars.getToolBarManager();
		drillDownAdapter.addNavigationActions(tbm);
		tbm.add(new Separator());
		tbm.add(showUnconfFeaturesAction);
	}
	protected void fillContextMenu(IMenuManager manager) {
		Object obj = getSelectedObject();
		IInstallConfiguration config = getSelectedConfiguration(obj, false);
		if (config != null && !config.isCurrent()) {
			manager.add(revertAction);
			manager.add(new Separator());
		}
		if (config != null && !isPreserved(config)) {
			manager.add(preserveAction);
		}
		config = getSelectedConfiguration(obj, true);
		if (config != null) {
			manager.add(removePreservedAction);
		}
		super.fillContextMenu(manager);
		if (obj instanceof PreservedConfiguration
			|| obj instanceof IInstallConfiguration)
			manager.add(propertiesAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		super.fillContextMenu(manager);
	}

	private void registerListeners() {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			config.addInstallConfigurationChangedListener(this);
			IConfiguredSite[] isites = config.getConfiguredSites();
			for (int i = 0; i < isites.length; i++) {
				isites[i].addConfiguredSiteChangedListener(this);
			}
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
		}
	}

	private void unregisterListeners() {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			config.removeInstallConfigurationChangedListener(this);
			IConfiguredSite[] isites = config.getConfiguredSites();
			for (int i = 0; i < isites.length; i++) {
				isites[i].removeConfiguredSiteChangedListener(this);
			}
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
		}
	}

	/**
	 * @see IInstallConfigurationChangedListener#installSiteAdded(ISite)
	 */
	public void installSiteAdded(IConfiguredSite csite) {
		//viewer.add(getLocalSite(), csite);
		viewer.refresh(getLocalSite());
	}

	/**
	 * @see IInstallConfigurationChangedListener#installSiteRemoved(ISite)
	 */
	public void installSiteRemoved(IConfiguredSite site) {
		//viewer.remove(site);
		viewer.refresh(getLocalSite());
	}

	/**
	 * @see IConfiguredSiteChangedListener#featureInstalled(IFeature)
	 */
	public void featureInstalled(IFeature feature) {
		//viewer.add(feature.getSite(), feature);
		viewer.refresh();
	}

	/**
	 * @see IConfiguredSiteChangedListener#featureUninstalled(IFeature)
	 */
	public void featureUninstalled(IFeature feature) {
		//viewer.remove(feature);
		viewer.refresh();
	}
	public void currentInstallConfigurationChanged(IInstallConfiguration configuration) {
		viewer.refresh();
	}

	public void installConfigurationRemoved(IInstallConfiguration configuration) {
		viewer.refresh();
	}

}