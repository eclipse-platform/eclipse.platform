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
package org.eclipse.update.internal.ui.views;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.help.WorkbenchHelp;
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
	private SiteStateAction siteStateAction;
	private IUpdateModelChangedListener modelListener;
	private boolean refreshLock=false;
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
	private static final String KEY_SAVING_ERRORS =
		"ConfigurationView.savingErrors";

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
			super(UpdateUI.getString(KEY_SAVED_FOLDER));
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
			super(UpdateUI.getString(KEY_HISTORY_FOLDER));
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
		public void inputChanged(
			Viewer viewer,
			Object oldInput,
			Object newInput) {
			if (newInput == null)
				return;
			updateTitle(newInput);
		}
		/**
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parent) {
			if (parent instanceof UpdateModel) {
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
				return getFeatures(adapter, !showUnconf);
			}
			if (parent instanceof ConfiguredFeatureAdapter) {
				return ((ConfiguredFeatureAdapter) parent).getIncludedFeatures(
					null);
			}
			return new Object[0];
		}

		private Object[] getConfigurationSites(final IInstallConfiguration config) {
			final Object[][] bag = new Object[1][];
			BusyIndicator
				.showWhile(
					getViewer().getControl().getDisplay(),
					new Runnable() {
				public void run() {
					IConfiguredSite[] sites = config.getConfiguredSites();
					Object[] adapters = new Object[sites.length];
					for (int i = 0; i < sites.length; i++) {
						adapters[i] =
							new ConfiguredSiteAdapter(config, sites[i]);
					}
					bag[0] = adapters;
				}
			});
			return bag[0];
		}

		public Object getParent(Object child) {
			return null;
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof ConfiguredFeatureAdapter) {
				return ((ConfiguredFeatureAdapter) parent).hasIncludedFeatures(
					null);
			}
			if (parent instanceof ConfiguredSiteAdapter) {
				IConfiguredSite site =
					((ConfiguredSiteAdapter) parent).getConfiguredSite();
				boolean showUnconf = showUnconfFeaturesAction.isChecked();
				if (!showUnconf && site.isEnabled() == false)
					return false;
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
				AboutInfo info = UpdateUI.getDefault().getAboutInfo();
				String productName = info.getProductName();
				if (productName != null)
					return productName;
				return UpdateUI.getString(KEY_CURRENT);
			}
			if (obj instanceof IInstallConfiguration) {
				IInstallConfiguration config = (IInstallConfiguration) obj;
				return config.getLabel();
			}
			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfiguredSite();
				ISite site = csite.getSite();
				return site.getURL().toString();
			}
			if (obj instanceof IFeatureAdapter) {
				try {
					IFeature feature = ((IFeatureAdapter) obj).getFeature(null);
					if (feature instanceof MissingFeature) {
						return UpdateUI.getFormattedMessage(
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
			UpdateLabelProvider provider =
				UpdateUI.getDefault().getLabelProvider();
			if (obj instanceof ILocalSite)
				return eclipseImage;
			if (obj instanceof IFeatureAdapter) {
				return getFeatureImage(provider, (IFeatureAdapter) obj);
			}
			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfiguredSite();
				int flags =
					csite.isUpdatable() ? 0 : UpdateLabelProvider.F_LINKED;
				if (csite.isEnabled() == false)
					flags |= UpdateLabelProvider.F_UNCONFIGURED;
				ImageDescriptor desc = provider.getLocalSiteDescriptor(csite);
				return provider.get(desc, flags);
			}
			if (obj instanceof SavedFolder) {
				return provider.get(UpdateUIImages.DESC_SAVED_OBJ);
			}
			if (obj instanceof HistoryFolder) {
				return provider.get(UpdateUIImages.DESC_HISTORY_OBJ);
			}
			if (obj instanceof PreservedConfiguration) {
				obj = ((PreservedConfiguration) obj).getConfiguration();
			}
			if (obj instanceof IInstallConfiguration) {
				IInstallConfiguration config = (IInstallConfiguration) obj;
				int flags =
					config.isCurrent() ? SharedLabelProvider.F_CURRENT : 0;

				boolean currentTimeline = isCurrentTimeline(config);
				if (!currentTimeline)
					flags |= SharedLabelProvider.F_MOD;
				return provider.get(UpdateUIImages.DESC_CONFIG_OBJ, flags);
			}
			return null;
		}

		private boolean isCurrentTimeline(IInstallConfiguration config) {
			ILocalSite localSite = getLocalSite();
			if (localSite == null)
				return true;
			IInstallConfiguration cconfig = localSite.getCurrentConfiguration();
			return config.getTimeline() == cconfig.getTimeline();
		}

		private Image getFeatureImage(
			UpdateLabelProvider provider,
			IFeatureAdapter adapter) {
			boolean configured = true;
			boolean updated = false;
			boolean current = true;
			if (adapter instanceof IConfiguredFeatureAdapter) {
				IConfiguredFeatureAdapter cadapter =
					(IConfiguredFeatureAdapter) adapter;
				configured = cadapter.isConfigured();
				updated = cadapter.isUpdated();
				current = cadapter.getInstallConfiguration().isCurrent();
			}
			ILocalSite localSite = getLocalSite();
			try {
				IFeature feature = adapter.getFeature(null);
				if (feature instanceof MissingFeature) {
					MissingFeature mfeature = (MissingFeature) feature;
					if (mfeature.isOptional() == false)
						return provider.get(
							UpdateUIImages.DESC_FEATURE_OBJ,
							UpdateLabelProvider.F_ERROR);
					return provider.get(
						UpdateUIImages.DESC_NOTINST_FEATURE_OBJ);
				}
				int code = IFeature.STATUS_HAPPY;
				ImageDescriptor baseDesc;
				int flags = 0;
				if (current) {
					IStatus status = localSite.getFeatureStatus(feature);
					code = getStatusCode(feature, status);
				}
				boolean efix = feature.isPatch();
				baseDesc =
					efix
						? UpdateUIImages.DESC_EFIX_OBJ
						: (configured
							? UpdateUIImages.DESC_FEATURE_OBJ
							: UpdateUIImages.DESC_UNCONF_FEATURE_OBJ);
				switch (code) {
					case IFeature.STATUS_UNHAPPY :
						flags |= UpdateLabelProvider.F_ERROR;
						break;
					case IFeature.STATUS_AMBIGUOUS :
						flags |= UpdateLabelProvider.F_WARNING;
						break;
					default :
						if (configured && updated)
							flags |= UpdateLabelProvider.F_UPDATED;
						break;
				}
				return provider.get(baseDesc, flags);
			} catch (CoreException e) {
				return provider.get(
					UpdateUIImages.DESC_FEATURE_OBJ,
					UpdateLabelProvider.F_ERROR);
			}
		}
	}

	public ConfigurationView() {
		UpdateUI.getDefault().getLabelProvider().connect(this);
		initializeImages();
		savedFolder = new SavedFolder();
		historyFolder = new HistoryFolder();
	}

	private void initializeImages() {
		ImageDescriptor edesc = UpdateUIImages.DESC_APP_OBJ;
		AboutInfo info = UpdateUI.getDefault().getAboutInfo();
		if (info.getWindowImage() != null)
			edesc = info.getWindowImage();
		eclipseImage = UpdateUI.getDefault().getLabelProvider().get(edesc);
	}

	public void initProviders() {
		final TreeViewer treeViewer = getTreeViewer();
		treeViewer.setContentProvider(new LocalSiteProvider());
		treeViewer.setInput(UpdateUI.getDefault().getUpdateModel());
		treeViewer.setLabelProvider(new LocalSiteLabelProvider());
		treeViewer.setSorter(new ConfigurationSorter());
		treeViewer.addFilter(new ViewerFilter() {
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
			UpdateUI.logException(e);
		}
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		modelListener = new IUpdateModelChangedListener() {
			public void objectsAdded(Object parent, Object[] children) {
			}
			public void objectsRemoved(Object parent, Object[] children) {
			}
			public void objectChanged(final Object obj, String property) {
				if (refreshLock) return;
				getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
						treeViewer.refresh();
					}
				});
			}
		};
		model.addUpdateModelChangedListener(modelListener);
		WorkbenchHelp.setHelp(
			getControl(),
			"org.eclipse.update.ui.ConfigurationView");
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
				try {
					ILocalSite localSite = SiteManager.getLocalSite();
					IInstallConfiguration config =
						localSite.getCurrentConfiguration();
					IConfiguredSite[] sites = config.getConfiguredSites();
					Object[] result = new Object[sites.length];
					for (int i = 0; i < sites.length; i++) {
						result[i] =
							new ConfiguredSiteAdapter(config, sites[i]);
					}
					if (!initialized) {
						config.addInstallConfigurationChangedListener(
							ConfigurationView.this);
						initialized = true;
					}
					bag[0] = result;
				} catch (CoreException e) {
					UpdateUI.logException(e);
					bag[0] = new Object[0];
				}
			}
		});
		return bag[0];
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		if (initialized) {
			try {
				ILocalSite localSite = SiteManager.getLocalSite();
				localSite.removeLocalSiteChangedListener(this);
				IInstallConfiguration config =
					localSite.getCurrentConfiguration();
				config.removeInstallConfigurationChangedListener(this);
			} catch (CoreException e) {
				UpdateUI.logException(e);
			}
			initialized = false;
		}
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		model.removeUpdateModelChangedListener(modelListener);
		super.dispose();
	}
	private Object getSelectedObject() {
		ISelection selection = getTreeViewer().getSelection();
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
		getTreeViewer().setExpandedState(historyFolder, true);
		getTreeViewer().setSelection(
			new StructuredSelection(historyFolder),
			true);
	}
	public void selectCurrentConfiguration() {
		getTreeViewer().setSelection(
			new StructuredSelection(getLocalSite()),
			true);
	}

	public void expandPreservedConfigurations() {
		getTreeViewer().refresh(savedFolder);
		getTreeViewer().setExpandedState(savedFolder, true);
	}

	private boolean canPreserve() {
		ISelection selection = getTreeViewer().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.isEmpty())
				return false;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (!(obj instanceof IInstallConfiguration
					|| obj instanceof ILocalSite))
					return false;
			}
			return true;
		}
		return false;
	}

	private void doPreserve() {
		ISelection selection = getTreeViewer().getSelection();
		ILocalSite localSite;
		ArrayList errors = new ArrayList();
		int nsaved = 0;
		try {
			localSite = SiteManager.getLocalSite();
		} catch (CoreException e) {
			UpdateUI.logException(e);
			return;
		}
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.isEmpty())
				return;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				IInstallConfiguration target =
					getSelectedConfiguration(obj, false);
				if (target == null)
					continue;
				try {
					localSite.addToPreservedConfigurations(target);
					nsaved++;
				} catch (CoreException e) {
					errors.add(
						new Status(
							IStatus.ERROR,
							UpdateUI.getPluginId(),
							IStatus.OK,
							null,
							e));
				}
			}
		}
		if (nsaved > 0) {
			try {
				localSite.save();
			} catch (CoreException e) {
				UpdateUI.logException(e);
			}
			getTreeViewer().refresh(savedFolder);
			getTreeViewer().expandToLevel(savedFolder, 1);
		}
		if (errors.size() > 0) {
			IStatus[] children =
				(IStatus[]) errors.toArray(new IStatus[errors.size()]);
			String message = UpdateUI.getString(KEY_SAVING_ERRORS);
			MultiStatus status =
				new MultiStatus(
					UpdateUI.getPluginId(),
					IStatus.OK,
					children,
					message,
					null);
			CoreException e = new CoreException(status);
			UpdateUI.logException(e);
		}
	}

	private boolean canDelete() {
		ISelection selection = getTreeViewer().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.isEmpty())
				return false;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (!(obj instanceof PreservedConfiguration))
					return false;
			}
			return true;
		}
		return false;
	}

	private void doDelete() {
		ISelection selection = getTreeViewer().getSelection();
		ILocalSite localSite;

		if (!confirmDeletion())
			return;

		int nremoved = 0;
		try {
			localSite = SiteManager.getLocalSite();
		} catch (CoreException e) {
			UpdateUI.logException(e);
			return;
		}
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.isEmpty())
				return;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				IInstallConfiguration target = null;
				if (obj instanceof PreservedConfiguration)
					target = ((PreservedConfiguration) obj).getConfiguration();
				if (target == null)
					continue;
				localSite.removeFromPreservedConfigurations(target);
				nremoved++;
			}
		}
		if (nremoved > 0) {
			try {
				localSite.save();
			} catch (CoreException e) {
				UpdateUI.logException(e);
			}
			getTreeViewer().refresh(savedFolder);
		}
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
		initDrillDown();
		final IDialogSettings settings =
			UpdateUI.getDefault().getDialogSettings();
		boolean showUnconfState = settings.getBoolean(STATE_SHOW_UNCONF);
		showUnconfFeaturesAction = new Action() {
			public void run() {
				getTreeViewer().refresh();
				//getTreeViewer().refresh();
				settings.put(
					STATE_SHOW_UNCONF,
					showUnconfFeaturesAction.isChecked());
			}
		};
		WorkbenchHelp.setHelp(
			showUnconfFeaturesAction,
			"org.eclipse.update.ui.CofigurationView_showUnconfFeaturesAction");
		showUnconfFeaturesAction.setText(
			UpdateUI.getString(KEY_SHOW_UNCONF_FEATURES));
		showUnconfFeaturesAction.setImageDescriptor(
			UpdateUIImages.DESC_UNCONF_FEATURE_OBJ);
		showUnconfFeaturesAction.setChecked(showUnconfState);
		showUnconfFeaturesAction.setToolTipText(
			UpdateUI.getString(KEY_SHOW_UNCONF_FEATURES_TOOLTIP));
		super.makeActions();
		initDrillDown();
		revertAction = new Action() {
			public void run() {
				Object obj = getSelectedObject();
				IInstallConfiguration target =
					getSelectedConfiguration(obj, false);
				if (target != null)
					RevertSection.performRevert(target);
			}
		};
		revertAction.setText(UpdateUI.getString(KEY_RESTORE));
		WorkbenchHelp.setHelp(
			revertAction,
			"org.eclipse.update.ui.CofigurationView_revertAction");

		showStatusAction = new Action() {
			public void run() {
				Object obj = getSelectedObject();
				try {
					if (obj instanceof IFeatureAdapter) {
						IFeature feature =
							((IFeatureAdapter) obj).getFeature(null);
						showFeatureStatus(feature);
					}
				} catch (CoreException e) {
					UpdateUI.logException(e);
				}
			}
		};
		WorkbenchHelp.setHelp(
			showStatusAction,
			"org.eclipse.update.ui.CofigurationView_showStatusAction");
		showStatusAction.setText(UpdateUI.getString(KEY_SHOW_STATUS));

		preserveAction = new Action() {
			public void run() {
				doPreserve();
			}
		};
		WorkbenchHelp.setHelp(
			preserveAction,
			"org.eclipse.update.ui.CofigurationView_preserveAction");
		preserveAction.setText(UpdateUI.getString(KEY_PRESERVE));
		removePreservedAction = new Action() {
			public void run() {
				doDelete();
			}
		};
		WorkbenchHelp.setHelp(
			removePreservedAction,
			"org.eclipse.update.ui.CofigurationView_removePreservedAction");
		removePreservedAction.setText(UpdateUI.getString(KEY_REMOVE_PRESERVED));

		unlinkAction = new Action() {
			public void run() {
				performUnlink();
			}
		};
		WorkbenchHelp.setHelp(
			unlinkAction,
			"org.eclipse.update.ui.CofigurationView_unlinkAction");
		unlinkAction.setText(UpdateUI.getString(KEY_UNLINK));

		siteStateAction = new SiteStateAction();

		propertiesAction =
			new PropertyDialogAction(
				UpdateUI.getActiveWorkbenchShell(),
				getTreeViewer());
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
				msg = UpdateUI.getString(KEY_MISSING_OPTIONAL_STATUS);
			} else {
				severity = IStatus.ERROR;
				msg = UpdateUI.getString(KEY_MISSING_STATUS);
			}
			status =
				new Status(severity, UpdateUI.PLUGIN_ID, IStatus.OK, msg, null);
		} else {
			status = getLocalSite().getFeatureStatus(feature);
		}
		String title = UpdateUI.getString(KEY_STATUS_TITLE);
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
					getControl().getShell(),
					title,
					null,
					status);
				break;
			case IStatus.WARNING :
				MessageDialog.openWarning(
					getControl().getShell(),
					title,
					status.getMessage());
				break;
			case IStatus.INFO :
			default :
				if (message == null || message.length() == 0)
					message = UpdateUI.getString(KEY_STATUS_DEFAULT);
				MessageDialog.openInformation(
					getControl().getShell(),
					title,
					message);
		}
	}

	protected void fillActionBars(IActionBars bars) {
		IToolBarManager tbm = bars.getToolBarManager();
		tbm.add(showUnconfFeaturesAction);
		tbm.add(new Separator());
		addDrillDownAdapter(bars);
		tbm.add(new Separator());
		tbm.add(collapseAllAction);
	}
	protected void fillContextMenu(IMenuManager manager) {
		Object obj = getSelectedObject();
		IInstallConfiguration config = getSelectedConfiguration(obj, false);
		if (config != null && !config.isCurrent()) {
			manager.add(revertAction);
			manager.add(new Separator());
		}
		if (canPreserve())
			manager.add(preserveAction);
		if (canDelete())
			manager.add(removePreservedAction);

		IConfiguredSite site = getSelectedSite(obj);
		if (site != null) {
			IInstallConfiguration cfg = site.getInstallConfiguration();
			if (cfg != null && cfg.isCurrent()) {
				try {
					if (site.isExtensionSite() && !site.isNativelyLinked()) {
						manager.add(unlinkAction);
					}
				} catch (CoreException e) {
					UpdateUI.logException(e);
				}
			}
			siteStateAction.setSite(site);
			manager.add(siteStateAction);
		}
		manager.add(new Separator());
		addDrillDownAdapter(manager);
		manager.add(new Separator());
		if (obj instanceof IConfiguredFeatureAdapter) {
			IConfiguredFeatureAdapter adapter = (IConfiguredFeatureAdapter) obj;
			if (adapter.getInstallConfiguration().isCurrent())
				manager.add(showStatusAction);
		}
		super.fillContextMenu(manager);
		if (obj instanceof PreservedConfiguration
			|| obj instanceof IInstallConfiguration)
			manager.add(propertiesAction);

		//defect 14684
		//super.fillContextMenu(manager);
	}

	private IConfiguredSite getSelectedSite(Object obj) {
		if (obj instanceof ConfiguredSiteAdapter) {
			return ((ConfiguredSiteAdapter) obj).getConfiguredSite();
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
			UpdateUI.informRestartNeeded();
		} catch (CoreException e) {
			UpdateUI.logException(e);
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
			UpdateUI.logException(e);
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
			UpdateUI.logException(e);
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
		if (getControl().isDisposed())
			return;
		display.asyncExec(new Runnable() {
			public void run() {
				if (!getControl().isDisposed())
					getTreeViewer().refresh();
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
				IFeature ifeature = iref.getFeature(null);
				IConfiguredSite csite =
					ifeature.getSite().getCurrentConfiguredSite();
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
		IConfiguredSite csite = feature.getSite().getCurrentConfiguredSite();
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

	private Object[] getFeatures(
		final IConfiguredSiteAdapter siteAdapter,
		final boolean configuredOnly) {
		final IConfiguredSite csite = siteAdapter.getConfiguredSite();
		final Object[][] bag = new Object[1][];
		refreshLock=true;

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
					UpdateUI.getString("ConfigurationView.loading"),
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
			getViewSite().getWorkbenchWindow().run(true, false, op);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
		}
		finally {
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
						cf.getConfiguredSite(),
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
		IConfiguredSite csite,
		ArrayList children,
		boolean configured) {
		try {
			IIncludedFeatureReference[] included =
				feature.getIncludedFeatureReferences();
			for (int i = 0; i < included.length; i++) {
				IFeature childFeature;
				try {
					childFeature =
						included[i].getFeature(!configured, csite, null);
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
	protected Object getRootObject() {
		return UpdateUI.getDefault().getUpdateModel();
	}
}
