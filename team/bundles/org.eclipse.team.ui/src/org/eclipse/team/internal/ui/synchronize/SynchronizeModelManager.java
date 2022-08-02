/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IActionBars;

/**
 * Manages the models that can be displayed by a synchronize page
 */
public abstract class SynchronizeModelManager extends SynchronizePageActionGroup {

	private static final String P_LAST_PROVIDER = TeamUIPlugin.ID + ".P_LAST_MODELPROVIDER"; //$NON-NLS-1$

	private ISynchronizeModelProvider modelProvider;
	private List toggleModelProviderActions;
	private ISynchronizePageConfiguration configuration;
	private TreeViewerAdvisor advisor;

	/**
	 * Action that allows changing the model providers supported by this advisor.
	 */
	private class ToggleModelProviderAction extends Action implements IPropertyChangeListener {
		private ISynchronizeModelProviderDescriptor descriptor;
		protected ToggleModelProviderAction(ISynchronizeModelProviderDescriptor descriptor) {
			super(descriptor.getName(), IAction.AS_RADIO_BUTTON);
			setImageDescriptor(descriptor.getImageDescriptor());
			setToolTipText(descriptor.getName());
			this.descriptor = descriptor;
			update();
			configuration.addPropertyChangeListener(this);
		}

		@Override
		public void run() {
			if (!getSelectedProviderId().equals(descriptor.getId())) {
				setInput(descriptor.getId(), null);
			}
		}

		public void update() {
			setChecked(getSelectedProviderId().equals(descriptor.getId()));
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(SynchronizePageConfiguration.P_MODEL)) {
				update();
			}
		}
	}

	public SynchronizeModelManager(ISynchronizePageConfiguration configuration) {
		Assert.isNotNull(configuration, "configuration cannot be null"); //$NON-NLS-1$
		this.configuration = configuration;
		configuration.addActionContribution(this);
	}

	/**
	 * Initialize the model manager to be used with the provided advisor.
	 * @param advisor the tree viewer advisor
	 */
	public void setViewerAdvisor(TreeViewerAdvisor advisor) {
		this.advisor = advisor;
	}

	/**
	 * Return the list of supported model providers for this advisor.
	 * @return the supported models
	 */
	protected abstract ISynchronizeModelProviderDescriptor[] getSupportedModelProviders();

	/**
	 * Get the model provider that will be used to create the input
	 * for the adviser's viewer.
	 * @return the model provider
	 */
	protected abstract ISynchronizeModelProvider createModelProvider(String id);

	/**
	 * Return the provider that is currently active.
	 * @return the provider that is currently active
	 */
	public ISynchronizeModelProvider getActiveModelProvider() {
		return modelProvider;
	}

	protected String getDefaultProviderId() {
		String defaultLayout = TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCVIEW_DEFAULT_LAYOUT);
		if (defaultLayout.equals(IPreferenceIds.TREE_LAYOUT)) {
			return HierarchicalModelProvider.HierarchicalModelProviderDescriptor.ID;
		}
		if (defaultLayout.equals(IPreferenceIds.FLAT_LAYOUT)) {
			return FlatModelProvider.FlatModelProviderDescriptor.ID;
		}
		// Return compressed folder is the others were not a match
		return CompressedFoldersModelProvider.CompressedFolderModelProviderDescriptor.ID;
	}

	/**
	 * Return the id of the selected provider. By default, this is the
	 * id of the active provider. However, subclasses that use a composite
	 * may return an id that differs from that of the active provider
	 * and return an id of a sub-provider instead.
	 * @return the id of the selected provider
	 */
	protected String getSelectedProviderId() {
		ISynchronizeModelProvider provider = getActiveModelProvider();
		if (provider != null) {
			return provider.getDescriptor().getId();
		}
		return getDefaultProviderId();
	}

	/**
	 * Replace the active provider with a provider for the given id.
	 * The new provider is created and initialized and assigned
	 * as the input of the viewer.
	 * @param id the id used to configure the new model provider
	 * @param monitor a progress monitor
	 */
	protected void setInput(String id, IProgressMonitor monitor) {
		if(modelProvider != null) {
			modelProvider.saveState();
			modelProvider.dispose();
		}
		modelProvider = createModelProvider(id);
		saveProviderSettings(id);
		modelProvider.prepareInput(monitor);
		setInput();
	}

	/**
	 * Save the settings for the currently active provider
	 */
	protected void saveProviderSettings(String id) {
		IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
		if (pageSettings != null) {
			pageSettings.put(P_LAST_PROVIDER, id);
		}
	}

	/**
	 * Set the input of the viewer to the root model element.
	 */
	protected void setInput() {
		configuration.setProperty(SynchronizePageConfiguration.P_MODEL, modelProvider.getModelRoot());
		if(advisor != null)
			advisor.setInput(modelProvider);
	}

	/**
	 * Return the model root of the currently active model provider.
	 * This method will wait until the model is done updating.
	 * It is for use by test purposes only.
	 * @return the model root
	 */
	public ISynchronizeModelElement getModelRoot() {
		if (modelProvider != null && modelProvider instanceof SynchronizeModelProvider) {
			((SynchronizeModelProvider)modelProvider).waitUntilDone(new IProgressMonitor() {
				@Override
				public void beginTask(String name, int totalWork) {
				}
				@Override
				public void done() {
				}
				@Override
				public void internalWorked(double work) {
				}
				@Override
				public boolean isCanceled() {
					return false;
				}
				@Override
				public void setCanceled(boolean value) {
				}
				@Override
				public void setTaskName(String name) {
				}
				@Override
				public void subTask(String name) {
				}
				@Override
				public void worked(int work) {
					while (Display.getCurrent().readAndDispatch()) {}
				}
			});
			return modelProvider.getModelRoot();
		} else {
			return null;
		}
	}

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);
		ISynchronizeModelProviderDescriptor[] providers = getSupportedModelProviders();
		// We only need switching of layouts if there is more than one model provider
		if (providers.length > 1) {
			toggleModelProviderActions = new ArrayList();
			for (ISynchronizeModelProviderDescriptor provider : providers) {
				toggleModelProviderActions.add(new ToggleModelProviderAction(provider));
			}
		}
		// The input may of been set already. In that case, don't change it and
		// simply assign it to the view.
		if(modelProvider == null) {
			String defaultProviderId = getDefaultProviderId(); /* use providers prefered */
			IDialogSettings pageSettings = configuration.getSite().getPageSettings();
			if(pageSettings != null && pageSettings.get(P_LAST_PROVIDER) != null) {
				defaultProviderId = pageSettings.get(P_LAST_PROVIDER);
			}
			setInput(defaultProviderId, null);
		} else {
			setInput();
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		if (toggleModelProviderActions == null) return;
		IToolBarManager toolbar = actionBars.getToolBarManager();
		IMenuManager menu = actionBars.getMenuManager();
		IContributionItem group = findGroup(menu, ISynchronizePageConfiguration.LAYOUT_GROUP);
		if(menu != null && group != null) {
			MenuManager layout = new MenuManager(Utils.getString("action.layout.label", Policy.getActionBundle())); //$NON-NLS-1$
			menu.appendToGroup(group.getId(), layout);
			appendToMenu(null, layout);
		} else if(toolbar != null) {
			group = findGroup(toolbar, ISynchronizePageConfiguration.LAYOUT_GROUP);
			if (group != null) {
				appendToMenu(group.getId(), toolbar);
			}
		}
	}

	private void appendToMenu(String groupId, IContributionManager menu) {
		for (Iterator iter = toggleModelProviderActions.iterator(); iter.hasNext();) {
			if (groupId == null) {
				menu.add((Action) iter.next());
			} else {
				menu.appendToGroup(groupId, (Action) iter.next());
			}
		}
	}

	@Override
	public void dispose() {
		if(modelProvider != null) {
			modelProvider.dispose();
		}
		super.dispose();
	}

	/**
	 * Returns the configuration
	 * @return the configuration.
	 */
	@Override
	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Return the sync info set that is contained in the configuration.
	 * @return the sync info set that is contained in the configuration
	 */
	protected SyncInfoSet getSyncInfoSet() {
		return (SyncInfoSet)getConfiguration().getProperty(ISynchronizePageConfiguration.P_SYNC_INFO_SET);
	}
}
