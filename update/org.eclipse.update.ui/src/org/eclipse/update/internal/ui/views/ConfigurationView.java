package org.eclipse.update.internal.ui.views;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.forms.RevertSection;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;

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
	private static final String KEY_MISSING_OPTIONAL_STATUS =
		"ConfigurationView.missingOptionalStatus";
	private static final String KEY_MISSING_STATUS =
		"ConfigurationView.missingStatus";
	private static final String STATE_SHOW_UNCONF =
		"ConfigurationView.showUnconf";
	private Image eclipseImage;
	private Image featureImage;
	private Image updatedFeatureImage;
	private Image errorFeatureImage;
	private Image optionalFeatureImage;
	private Image warningFeatureImage;
	private Image unconfFeatureImage;
	private Image errorUnconfFeatureImage;
	private Image warningUnconfFeatureImage;
	private Image efixImage;
	private Image warningEfixImage;
	private Image errorEfixImage;
	private Image siteImage;
	private Image installSiteImage;
	private Image linkedSiteImage;
	private Image configImage;
	private Image currentConfigImage;
	private Image modConfigImage;
	private Image historyImage;
	private Image savedImage;
	private boolean initialized;
	private SavedFolder savedFolder;
	private HistoryFolder historyFolder;
	private Action showUnconfFeaturesAction;
	private Action revertAction;
	private Action preserveAction;
	private Action unlinkAction;
	private Action removePreservedAction;
	private Action propertiesAction;
	private Action showStatusAction;
	private IUpdateModelChangedListener modelListener;
	private DrillDownAdapter drillDownAdapter;
	private static final String KEY_RESTORE = "ConfigurationView.Popup.restore";
	private static final String KEY_PRESERVE =
		"ConfigurationView.Popup.preserve";
	private static final String KEY_UNLINK = "ConfigurationView.Popup.unlink";
	private static final String KEY_REMOVE_PRESERVED =
		"ConfigurationView.Popup.removePreserved";
	private static final String KEY_SHOW_STATUS =
		"ConfigurationView.Popup.showStatus";
	private static final String KEY_HISTORY_FOLDER =
		"ConfigurationView.historyFolder";
	private static final String KEY_SAVED_FOLDER =
		"ConfigurationView.savedFolder";
	private static final String KEY_STATUS_TITLE =
		"ConfigurationView.statusTitle";
	private static final String KEY_STATUS_DEFAULT =
		"ConfigurationView.statusDefault";
	private static final String KEY_MISSING_FEATURE =
		"ConfigurationView.missingFeature";

	abstract class ViewFolder extends UIModelObject {
		private String label;
		private Image image;

		public ViewFolder(String label) {
			this.label = label;
			String imageKey = ISharedImages.IMG_OBJ_FOLDER;
			image =
				PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}

		public Object getAdapter(Class key) {
			return null;
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
				return invertArray(
					makeChildren(localSite.getPreservedConfigurations()));
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
				return invertArray(localSite.getConfigurationHistory());
			} catch (CoreException e) {
				return new Object[0];
			}
		}
	}

	class ConfigurationSorter extends ViewerSorter {
		public int category(Object obj) {
			// Top level
			if (obj instanceof ILocalSite)
				return 1;
			if (obj.equals(historyFolder))
				return 2;
			if (obj.equals(savedFolder))
				return 3;

			return super.category(obj);
		}
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof IInstallConfiguration
				&& e2 instanceof IInstallConfiguration) {
				return 0;
			}
			return super.compare(viewer, e1, e2);
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
					return new Object[] {
						localSite,
						historyFolder,
						savedFolder };
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
				IConfiguredSiteAdapter adapter =
					(IConfiguredSiteAdapter) parent;
				boolean showUnconf = showUnconfFeaturesAction.isChecked();
				if (showUnconf)
					return getAllFeatures(adapter);
				else
					return getConfiguredFeatures(adapter);
			}
			if (parent instanceof ConfiguredFeatureAdapter) {
				return ((ConfiguredFeatureAdapter) parent)
					.getIncludedFeatures();
			}
			return new Object[0];
		}

		private Object[] getConfigurationSites(final IInstallConfiguration config) {
			final Object[][] bag = new Object[1][];
			BusyIndicator
				.showWhile(viewer.getControl().getDisplay(), new Runnable() {
				public void run() {
					IConfiguredSite[] sites = config.getConfiguredSites();
					Object[] adapters = new Object[sites.length];
					for (int i = 0; i < sites.length; i++) {
						adapters[i] =
							new ConfigurationSiteAdapter(config, sites[i]);
					}
					bag[0] = adapters;
				}
			});
			return bag[0];
		}

		private Object[] getConfiguredFeatures(final IConfiguredSiteAdapter adapter) {
			final Object[][] bag = new Object[1][];

			BusyIndicator
				.showWhile(viewer.getControl().getDisplay(), new Runnable() {
				public void run() {
					IConfiguredSite csite = adapter.getConfigurationSite();
					IFeatureReference[] refs = csite.getConfiguredFeatures();
					ArrayList result = new ArrayList();
					for (int i = 0; i < refs.length; i++) {
						IFeatureReference ref = refs[i];
						IFeature feature;
						try {
							feature = ref.getFeature();
						} catch (CoreException e) {
							feature =
								new MissingFeature(ref.getSite(), ref.getURL());
						}
						result.add(
							new ConfiguredFeatureAdapter(
								adapter,
								feature,
								true,
								false,
								ref.isOptional()));
					}
					bag[0] = getRootFeatures(result);
				}
			});
			return bag[0];
		}

		private Object[] getAllFeatures(IConfiguredSiteAdapter adapter) {
			IConfiguredSite csite = adapter.getConfigurationSite();
			ISite site = csite.getSite();
			IFeatureReference[] allRefs = site.getFeatureReferences();
			ArrayList result = new ArrayList();

			for (int i = 0; i < allRefs.length; i++) {
				IFeature feature;
				try {
					feature = allRefs[i].getFeature();
				} catch (CoreException e) {
					feature = new MissingFeature(site, allRefs[i].getURL());
				}
				result.add(
					new ConfiguredFeatureAdapter(
						adapter,
						feature,
						csite.isConfigured(feature),
						false,
						allRefs[i].isOptional()));
			}
			return getRootFeatures(result);
		}

		private Object[] getRootFeatures(ArrayList list) {
			ArrayList children = new ArrayList();
			ArrayList result = new ArrayList();
			try {
				for (int i = 0; i < list.size(); i++) {
					ConfiguredFeatureAdapter cf =
						(ConfiguredFeatureAdapter) list.get(i);
					IFeature feature = cf.getFeature();
					if (feature != null)
						addChildFeatures(feature, children, cf.isConfigured());
				}
				for (int i = 0; i < list.size(); i++) {
					ConfiguredFeatureAdapter cf =
						(ConfiguredFeatureAdapter) list.get(i);
					IFeature feature = cf.getFeature();
					if (feature != null
						&& isChildFeature(feature, children) == false)
						result.add(cf);
				}
			} catch (CoreException e) {
				return list.toArray();
			}
			return result.toArray();
		}

		private void addChildFeatures(IFeature feature, ArrayList children, boolean configured) {
			try {
				IFeatureReference[] included =
					feature.getIncludedFeatureReferences();
				for (int i = 0; i < included.length; i++) {
					IFeature childFeature;
					try {
						childFeature = included[i].getFeature(!configured);
					} catch (CoreException e) {
						childFeature = new MissingFeature(included[i]);
					}
					children.add(childFeature);
				}
			} catch (CoreException e) {
				UpdateUIPlugin.logException(e);
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
		public Object getParent(Object child) {
			return null;
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof ConfiguredFeatureAdapter) {
				return ((ConfiguredFeatureAdapter) parent)
					.hasIncludedFeatures();
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
				AboutInfo info = UpdateUIPlugin.getDefault().getAboutInfo();
				String productName = info.getProductName();
				if (productName != null)
					return productName;
				return UpdateUIPlugin.getResourceString(KEY_CURRENT);
			}
			if (obj instanceof IInstallConfiguration) {
				IInstallConfiguration config = (IInstallConfiguration) obj;
				return config.getLabel();
			}
			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfigurationSite();
				ISite site = csite.getSite();
				return site.getURL().toString();
			}
			if (obj instanceof IFeatureAdapter) {
				try {
					IFeature feature = ((IFeatureAdapter) obj).getFeature();
					if (feature instanceof MissingFeature) {
						return UpdateUIPlugin.getFormattedMessage(
							KEY_MISSING_FEATURE,
							feature.getLabel());
					}
					String version =
						feature
							.getVersionedIdentifier()
							.getVersion()
							.toString();
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
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfigurationSite();
				if (csite.isUpdatable())
					return installSiteImage;
				else
					return linkedSiteImage;
			}
			if (obj instanceof SavedFolder) {
				return savedImage;
			}
			if (obj instanceof HistoryFolder) {
				return historyImage;
			}
			if (obj instanceof PreservedConfiguration) {
				obj = ((PreservedConfiguration) obj).getConfiguration();
			}
			if (obj instanceof IInstallConfiguration) {
				IInstallConfiguration config = (IInstallConfiguration) obj;
				if (config.isCurrent())
					return currentConfigImage;
				boolean currentTimeline = isCurrentTimeline(config);
				return currentTimeline?configImage:modConfigImage;
			}
			return null;
		}
		
		private boolean isCurrentTimeline(IInstallConfiguration config) {
			ILocalSite localSite = getLocalSite();
			if (localSite==null) return true;
			IInstallConfiguration cconfig = localSite.getCurrentConfiguration();
			return config.getTimeline()==cconfig.getTimeline();
		}

		private Image getFeatureImage(IFeatureAdapter adapter) {
			boolean configured = true;
			boolean updated = false;
			if (adapter instanceof IConfiguredFeatureAdapter) {
				configured =
					((IConfiguredFeatureAdapter) adapter).isConfigured();
				updated = ((IConfiguredFeatureAdapter) adapter).isUpdated();
			}
			ILocalSite localSite = getLocalSite();
			try {
				IFeature feature = adapter.getFeature();
				if (updated)
					System.out.println("Updated: "+feature.getVersionedIdentifier());
				if (feature instanceof MissingFeature) {
					MissingFeature mfeature = (MissingFeature) feature;
					if (mfeature.isOptional() == false)
						return errorFeatureImage;
					return optionalFeatureImage;
				}
				IStatus status = localSite.getFeatureStatus(feature);
				int code = getStatusCode(feature, status);
				if (configured) {
					boolean efix = UpdateUIPlugin.isPatch(feature);
					switch (code) {
						case IFeature.STATUS_UNHAPPY :
							return efix ? errorEfixImage : errorFeatureImage;
						case IFeature.STATUS_AMBIGUOUS :
							return efix
								? warningEfixImage
								: warningFeatureImage;
						default :
							return updated
								? updatedFeatureImage
								: (efix ? efixImage : featureImage);
					}
				} else {
					switch (code) {
						case IFeature.STATUS_UNHAPPY :
							return errorUnconfFeatureImage;
						case IFeature.STATUS_AMBIGUOUS :
							return warningUnconfFeatureImage;
						default :
							return unconfFeatureImage;
					}
				}
			} catch (CoreException e) {
				//UpdateUIPlugin.logException(e);
				return errorFeatureImage;
			}
		}
	}

	public ConfigurationView() {
		initializeImages();
		savedFolder = new SavedFolder();
		historyFolder = new HistoryFolder();
	}

	private void initializeImages() {
		ImageDescriptor edesc = UpdateUIPluginImages.DESC_APP_OBJ;
		AboutInfo info = UpdateUIPlugin.getDefault().getAboutInfo();
		if (info.getWindowImage() != null)
			edesc = info.getWindowImage();
		eclipseImage = edesc.createImage();
		featureImage = UpdateUIPluginImages.DESC_FEATURE_OBJ.createImage();
		optionalFeatureImage =
			UpdateUIPluginImages.DESC_NOTINST_FEATURE_OBJ.createImage();
		edesc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_FEATURE_OBJ,
				new ImageDescriptor[][] { {
			}, {
			}, {
				UpdateUIPluginImages.DESC_ERROR_CO }
		});
		errorFeatureImage = edesc.createImage();
		edesc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_FEATURE_OBJ,
				new ImageDescriptor[][] { {
			}, {
			}, {
				UpdateUIPluginImages.DESC_WARNING_CO }
		});
		warningFeatureImage = edesc.createImage();
		edesc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_FEATURE_OBJ,
				new ImageDescriptor[][] { {
			}, {
			}, {
			}, {
				UpdateUIPluginImages.DESC_UPDATED_CO }
		});
		updatedFeatureImage = edesc.createImage();
		unconfFeatureImage =
			UpdateUIPluginImages.DESC_UNCONF_FEATURE_OBJ.createImage();

		edesc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_UNCONF_FEATURE_OBJ,
				new ImageDescriptor[][] { {
			}, {
			}, {
				UpdateUIPluginImages.DESC_ERROR_CO }
		});
		errorUnconfFeatureImage = edesc.createImage();
		edesc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_UNCONF_FEATURE_OBJ,
				new ImageDescriptor[][] { {
			}, {
			}, {
				UpdateUIPluginImages.DESC_WARNING_CO }
		});
		warningUnconfFeatureImage = edesc.createImage();

		efixImage = UpdateUIPluginImages.DESC_EFIX_OBJ.createImage();
		edesc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_EFIX_OBJ,
				new ImageDescriptor[][] { {
			}, {
			}, {
				UpdateUIPluginImages.DESC_ERROR_CO }
		});
		errorEfixImage = edesc.createImage();
		edesc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_EFIX_OBJ,
				new ImageDescriptor[][] { {
			}, {
			}, {
				UpdateUIPluginImages.DESC_WARNING_CO }
		});
		warningEfixImage = edesc.createImage();

		ImageDescriptor siteDesc = UpdateUIPluginImages.DESC_LSITE_OBJ;
		siteImage = siteDesc.createImage();
		ImageDescriptor installSiteDesc = UpdateUIPluginImages.DESC_LSITE_OBJ;
		installSiteImage = installSiteDesc.createImage();
		ImageDescriptor linkedSiteDesc =
			new OverlayIcon(
				siteDesc,
				new ImageDescriptor[][] { {
					UpdateUIPluginImages
					.DESC_LINKED_CO }
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
		cdesc =
			new OverlayIcon(
				UpdateUIPluginImages.DESC_CONFIG_OBJ,
				new ImageDescriptor[][] { {
			}, {
				UpdateUIPluginImages.DESC_MOD_CO }
		});
		modConfigImage = cdesc.createImage();
		savedImage = UpdateUIPluginImages.DESC_SAVED_OBJ.createImage();
		historyImage = UpdateUIPluginImages.DESC_HISTORY_OBJ.createImage();
	}

	public void initProviders() {
		viewer.setContentProvider(new LocalSiteProvider());
		viewer.setInput(UpdateUIPlugin.getDefault().getUpdateModel());
		viewer.setLabelProvider(new LocalSiteLabelProvider());
		viewer.setSorter(new ConfigurationSorter());
		viewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object element) {
				if (element instanceof IConfiguredFeatureAdapter) {
					IConfiguredFeatureAdapter adapter =
						(IConfiguredFeatureAdapter) element;
					if (adapter.isConfigured())
						return true;
					boolean showUnconf = showUnconfFeaturesAction.isChecked();
					return showUnconf;
				}
				return true;
			}
		});
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
			public void objectChanged(final Object obj, String property) {
				viewer.getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
							//viewer.update(obj, null);
		//must refresh because name change may
		// require resort
	viewer.refresh();
					}
				});
			}
		};
		model.addUpdateModelChangedListener(modelListener);
		WorkbenchHelp.setHelp(
			viewer.getControl(),
			"org.eclipse.update.ui.ConfigurationView");
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
		final Object[][] bag = new Object[1][];
		BusyIndicator
			.showWhile(viewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				try {
					ILocalSite localSite = SiteManager.getLocalSite();
					IInstallConfiguration config =
						localSite.getCurrentConfiguration();
					IConfiguredSite[] sites = config.getConfiguredSites();
					Object[] result = new Object[sites.length];
					for (int i = 0; i < sites.length; i++) {
						result[i] =
							new ConfigurationSiteAdapter(config, sites[i]);
					}
					if (!initialized) {
						config.addInstallConfigurationChangedListener(
							ConfigurationView.this);
						initialized = true;
					}
					bag[0] = result;
				} catch (CoreException e) {
					UpdateUIPlugin.logException(e);
					bag[0] = new Object[0];
				}
			}
		});
		return bag[0];
	}

	public void dispose() {
		eclipseImage.dispose();
		featureImage.dispose();
		updatedFeatureImage.dispose();
		optionalFeatureImage.dispose();
		unconfFeatureImage.dispose();
		errorFeatureImage.dispose();
		warningFeatureImage.dispose();
		efixImage.dispose();
		warningEfixImage.dispose();
		errorEfixImage.dispose();
		errorUnconfFeatureImage.dispose();
		warningUnconfFeatureImage.dispose();
		siteImage.dispose();
		installSiteImage.dispose();
		linkedSiteImage.dispose();
		savedImage.dispose();
		historyImage.dispose();
		configImage.dispose();
		currentConfigImage.dispose();
		modConfigImage.dispose();
		if (initialized) {
			try {
				ILocalSite localSite = SiteManager.getLocalSite();
				localSite.removeLocalSiteChangedListener(this);
				IInstallConfiguration config =
					localSite.getCurrentConfiguration();
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
		if (selection instanceof IStructuredSelection
			&& !selection.isEmpty()) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1) {
				return ssel.getFirstElement();
			}
		}
		return null;
	}

	public void selectHistoryFolder() {
		viewer.setExpandedState(historyFolder, true);
		viewer.setSelection(new StructuredSelection(historyFolder), true);
	}
	public void selectCurrentConfiguration() {
		viewer.setSelection(new StructuredSelection(getLocalSite()), true);
	}

	private IInstallConfiguration getSelectedConfiguration(
		Object obj,
		boolean onlyPreserved) {
		if (!onlyPreserved) {
			if (obj instanceof IInstallConfiguration)
				return (IInstallConfiguration) obj;
			if (obj instanceof ILocalSite)
				return ((ILocalSite) obj).getCurrentConfiguration();
		}
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
		final IDialogSettings settings =
			UpdateUIPlugin.getDefault().getDialogSettings();
		boolean showUnconfState = settings.getBoolean(STATE_SHOW_UNCONF);
		showUnconfFeaturesAction = new Action() {
			public void run() {
				viewer.refresh(getLocalSite());
				settings.put(
					STATE_SHOW_UNCONF,
					showUnconfFeaturesAction.isChecked());
			}
		};
		WorkbenchHelp.setHelp(
			showUnconfFeaturesAction,
			"org.eclipse.update.ui.CofigurationView_showUnconfFeaturesAction");
		showUnconfFeaturesAction.setText(
			UpdateUIPlugin.getResourceString(KEY_SHOW_UNCONF_FEATURES));
		showUnconfFeaturesAction.setImageDescriptor(
			UpdateUIPluginImages.DESC_UNCONF_FEATURE_OBJ);
		showUnconfFeaturesAction.setChecked(showUnconfState);
		showUnconfFeaturesAction.setToolTipText(
			UpdateUIPlugin.getResourceString(KEY_SHOW_UNCONF_FEATURES_TOOLTIP));
		drillDownAdapter = new DrillDownAdapter(viewer);
		super.makeActions();
		revertAction = new Action() {
			public void run() {
				Object obj = getSelectedObject();
				IInstallConfiguration target =
					getSelectedConfiguration(obj, false);
				if (target != null)
					RevertSection.performRevert(target);
			}
		};
		revertAction.setText(UpdateUIPlugin.getResourceString(KEY_RESTORE));
		WorkbenchHelp.setHelp(
			revertAction,
			"org.eclipse.update.ui.CofigurationView_revertAction");

		showStatusAction = new Action() {
			public void run() {
				Object obj = getSelectedObject();
				try {
					if (obj instanceof IFeatureAdapter) {
						IFeature feature = ((IFeatureAdapter) obj).getFeature();
						showFeatureStatus(feature);
					}
				} catch (CoreException e) {
					UpdateUIPlugin.logException(e);
				}
			}
		};
		WorkbenchHelp.setHelp(
			showStatusAction,
			"org.eclipse.update.ui.CofigurationView_showStatusAction");
		showStatusAction.setText(
			UpdateUIPlugin.getResourceString(KEY_SHOW_STATUS));
		preserveAction = new Action() {
			public void run() {
				Object obj = getSelectedObject();
				IInstallConfiguration target =
					getSelectedConfiguration(obj, false);
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
		WorkbenchHelp.setHelp(
			preserveAction,
			"org.eclipse.update.ui.CofigurationView_preserveAction");
		preserveAction.setText(UpdateUIPlugin.getResourceString(KEY_PRESERVE));
		removePreservedAction = new Action() {
			public void run() {
				Object obj = getSelectedObject();
				IInstallConfiguration target =
					getSelectedConfiguration(obj, true);
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
		WorkbenchHelp.setHelp(
			removePreservedAction,
			"org.eclipse.update.ui.CofigurationView_removePreservedAction");
		removePreservedAction.setText(
			UpdateUIPlugin.getResourceString(KEY_REMOVE_PRESERVED));

		unlinkAction = new Action() {
			public void run() {
				performUnlink();
			}
		};
		WorkbenchHelp.setHelp(
			unlinkAction,
			"org.eclipse.update.ui.CofigurationView_unlinkAction");
		unlinkAction.setText(UpdateUIPlugin.getResourceString(KEY_UNLINK));
		propertiesAction =
			new PropertyDialogAction(
				UpdateUIPlugin.getActiveWorkbenchShell(),
				viewer);
		WorkbenchHelp.setHelp(
			propertiesAction,
			"org.eclipse.update.ui.CofigurationView_propertiesAction");
	}

	private void showFeatureStatus(IFeature feature) throws CoreException {
		IStatus status;
		if (feature instanceof MissingFeature) {
			MissingFeature missingFeature = (MissingFeature) feature;
			String msg;
			int severity;

			if (missingFeature.isOptional()) {
				severity = IStatus.INFO;
				msg =
					UpdateUIPlugin.getResourceString(
						KEY_MISSING_OPTIONAL_STATUS);
			} else {
				severity = IStatus.ERROR;
				msg = UpdateUIPlugin.getResourceString(KEY_MISSING_STATUS);
			}
			status =
				new Status(
					severity,
					UpdateUIPlugin.PLUGIN_ID,
					IStatus.OK,
					msg,
					null);
		} else {
			status = getLocalSite().getFeatureStatus(feature);
		}
		String title = UpdateUIPlugin.getResourceString(KEY_STATUS_TITLE);
		int severity = status.getSeverity();
		String message = status.getMessage();

		if (severity == IStatus.ERROR
			&& status.getCode() == IFeature.STATUS_UNHAPPY) {
			//see if this is a false alarm
			int code = getStatusCode(feature, status);
			if (code == IFeature.STATUS_HAPPY) {
				// This was a patch referencing a
				// subsumed patch - change to happy.
				severity = IStatus.INFO;
				message = null;
			}
		}

		switch (severity) {
			case IStatus.ERROR :
				ErrorDialog.openError(
					viewer.getControl().getShell(),
					title,
					null,
					status);
				break;
			case IStatus.WARNING :
				MessageDialog.openWarning(
					viewer.getControl().getShell(),
					title,
					status.getMessage());
				break;
			case IStatus.INFO :
			default :
				if (message == null || message.length() == 0)
					message =
						UpdateUIPlugin.getResourceString(KEY_STATUS_DEFAULT);
				MessageDialog.openInformation(
					viewer.getControl().getShell(),
					title,
					message);
		}
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
		if (config != null
			&& !isPreserved(config)
			|| obj instanceof ILocalSite) {
			manager.add(preserveAction);
		}
		config = getSelectedConfiguration(obj, true);
		if (config != null) {
			manager.add(removePreservedAction);
		}
		IConfiguredSite site = getSelectedSite(obj);
		if (site != null) {
			IInstallConfiguration cfg = site.getInstallConfiguration();
			if (cfg != null && cfg.isCurrent()) {
				try {
					if (site.isExtensionSite() && !site.isNativelyLinked()) {
						manager.add(unlinkAction);
					}
				} catch (CoreException e) {
					UpdateUIPlugin.logException(e);
				}
			}
		}
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		if (obj instanceof IFeatureAdapter)
			manager.add(showStatusAction);
		super.fillContextMenu(manager);
		if (obj instanceof PreservedConfiguration
			|| obj instanceof IInstallConfiguration)
			manager.add(propertiesAction);

		//defect 14684
		//super.fillContextMenu(manager);
	}

	private IConfiguredSite getSelectedSite(Object obj) {
		if (obj instanceof ConfigurationSiteAdapter) {
			return ((ConfigurationSiteAdapter) obj).getConfigurationSite();
		}
		return null;
	}

	private void performUnlink() {
		IConfiguredSite site = getSelectedSite(getSelectedObject());
		if (site == null)
			return;
		IInstallConfiguration config = site.getInstallConfiguration();
		config.removeConfiguredSite(site);
		try {
			getLocalSite().save();
			UpdateUIPlugin.informRestartNeeded();
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
		}
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
	} /**
																														 * @see IInstallConfigurationChangedListener#installSiteAdded(ISite)
																														 */
	public void installSiteAdded(IConfiguredSite csite) {
		asyncRefresh();
	} /**
																														 * @see IInstallConfigurationChangedListener#installSiteRemoved(ISite)
																														 */
	public void installSiteRemoved(IConfiguredSite site) {
		asyncRefresh();
	} /**
																														 * @see IConfiguredSiteChangedListener#featureInstalled(IFeature)
																														 */
	public void featureInstalled(IFeature feature) {
		asyncRefresh();
	} /**
																														 * @see IConfiguredSiteChangedListener#featureUninstalled(IFeature)
																														 */
	public void featureRemoved(IFeature feature) {
		asyncRefresh();
	} /**
																														 * @see IConfiguredSiteChangedListener#featureUConfigured(IFeature)
																														 */
	public void featureConfigured(IFeature feature) {
	};
	/**
	 * @see IConfiguredSiteChangedListener#featureUConfigured(IFeature)
	 */
	public void featureUnconfigured(IFeature feature) {
	};
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
		if (viewer.getControl().isDisposed())
			return;
		display.asyncExec(new Runnable() {
			public void run() {
				if (!viewer.getControl().isDisposed())
					viewer.refresh();
			}
		});
	}
	private Object[] invertArray(Object[] array) {
		Object[] invertedArray = new Object[array.length];
		for (int i = 0; i < array.length; i++) {
			invertedArray[i] = array[array.length - 1 - i];
		}
		return invertedArray;
	}
	private int getStatusCode(IFeature feature, IStatus status) {
		int code = status.getCode();
		if (code == IFeature.STATUS_UNHAPPY) {
			if (status.isMultiStatus()) {
				IStatus[] children = status.getChildren();
				for (int i = 0; i < children.length; i++) {
					IStatus child = children[i];
					if (child.isMultiStatus())
						return code;
					if (child.getCode() != IFeature.STATUS_DISABLED)
						return code;
				}
				// If we are here, global status is unhappy
				// because one or more included features
				// is disabled.
				if (arePatchesObsolete(feature)) {
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
	private boolean arePatchesObsolete(IFeature feature) {
		// Check all the included features that
		// are unconfigured, and see if their patch 
		// references are better than the original.
		try {
			IFeatureReference[] irefs = feature.getIncludedFeatureReferences();
			for (int i = 0; i < irefs.length; i++) {
				IFeatureReference iref = irefs[i];
				IFeature ifeature = iref.getFeature();
				IConfiguredSite csite = ifeature.getSite().getConfiguredSite();
				if (!csite.isConfigured(ifeature)) {
					if (!isPatchHappy(ifeature))
						return false;
				}
			}
		} catch (CoreException e) {
			return false;
		}
		// All checks went well
		return true;
	}
	private boolean isPatchHappy(IFeature feature) throws CoreException {
		// If this is a patch and it includes 
		// another patch and the included patch
		// is disabled but the feature it was declared
		// to patch is now newer (and is presumed to
		// contain the now disabled patch), and
		// the newer patched feature is enabled,
		// a 'leap of faith' assumption can be
		// made:

		// Although the included patch is disabled,
		// the feature it was designed to patch
		// is now newer and most likely contains
		// the equivalent fix and more. Consequently,
		// we can claim that the status and the error
		// icon overlay are misleading because
		// all the right plug-ins are configured.
		IImport[] imports = feature.getImports();
		IImport patchReference = null;
		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				patchReference = iimport;
				break;
			}
		}
		if (patchReference == null)
			return false;
		VersionedIdentifier refVid = patchReference.getVersionedIdentifier();

		// Find the patched feature and 
		IConfiguredSite csite = feature.getSite().getConfiguredSite();
		if (csite == null)
			return false;

		IFeatureReference[] crefs = csite.getConfiguredFeatures();
		for (int i = 0; i < crefs.length; i++) {
			IFeatureReference cref = crefs[i];
			VersionedIdentifier cvid = cref.getVersionedIdentifier();
			if (cvid.getIdentifier().equals(refVid.getIdentifier())) {
				// This is the one.
				if (cvid.getVersion().isGreaterThan(refVid.getVersion())) {
					// Bingo: we found the referenced feature
					// and its version is greater - 
					// we can assume that it contains better code
					// than the patch that referenced the
					// older version.
					return true;
				}
			}
		}
		return false;
	}
}