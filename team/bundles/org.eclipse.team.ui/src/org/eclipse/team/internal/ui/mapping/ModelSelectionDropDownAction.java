/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeChangeListener;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelOperation;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;

public class ModelSelectionDropDownAction extends Action implements ISynchronizationScopeChangeListener {

	private final ISynchronizePageConfiguration configuration;
	private MenuManager menuManager;
	private final Action showAllAction;
	private final org.eclipse.jface.util.IPropertyChangeListener listener;
	private final MenuCreator menuCreator;
	private final Action showAllFlatAction;

	private class MenuCreator implements IMenuCreator {
		@Override
		public void dispose() {
			if(menuManager != null) {
				menuManager.dispose();
				menuManager = null;
			}
		}
		@Override
		public Menu getMenu(Control parent) {
			Menu fMenu = null;
			if (menuManager == null) {
				menuManager = new MenuManager();
				fMenu = menuManager.createContextMenu(parent);
				menuManager.add(showAllAction);
				showAllAction.setChecked(getActiveProviderId().equals(ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE));
				showAllFlatAction.setChecked(isFlatEnabled());
				ModelProvider[] modelProviders = getEnabledModelProviders();
				if (modelProviders.length > 0)
					menuManager.add(new Separator());
				addModelsToMenu(modelProviders);
				menuManager.add(new Separator());
				menuManager.add(showAllFlatAction);

				menuManager.update(true);
			} else {
				fMenu = menuManager.getMenu();
			}
			return fMenu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			return null;
		}
	}

	public ModelSelectionDropDownAction(ISynchronizePageConfiguration configuration) {
		Utils.initAction(this, "action.pickModels."); //$NON-NLS-1$
		this.configuration = configuration;
		listener = event -> {
			if (event.getProperty() == ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER) {
				update();
			}
			if (event.getProperty().equals(ITeamContentProviderManager.PROP_ENABLED_MODEL_PROVIDERS)) {
				rebuildMenu();
			}
		};
		this.configuration.addPropertyChangeListener(listener);
		TeamUI.getTeamContentProviderManager().addPropertyChangeListener(listener);
		getSynchronizationContext().getScope().addScopeChangeListener(this);
		showAllAction = new Action(TeamUIMessages.ModelSelectionDropDownAction_0, IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				ModelSelectionDropDownAction.this.configuration.setProperty(
						ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER,
						ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE);
			}
		};
		//showAllAction.setImageDescriptor(TeamImages.getImageDescriptor(ITeamUIImages.IMG_HIERARCHICAL));
		//showAllAction.setActionDefinitionId("org.eclipse.team.ui.showAllModels"); //$NON-NLS-1$
		showAllFlatAction = new Action(TeamUIMessages.ModelSelectionDropDownAction_2, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				boolean checked = showAllFlatAction.isChecked();
				ModelSelectionDropDownAction.this.configuration.setProperty(
						ITeamContentProviderManager.PROP_PAGE_LAYOUT,
						checked ? ITeamContentProviderManager.FLAT_LAYOUT : ITeamContentProviderManager.TREE_LAYOUT);
			}
		};
		showAllFlatAction.setImageDescriptor(TeamImages.getImageDescriptor(ITeamUIImages.IMG_FLAT));
		//showAllAction.setActionDefinitionId("org.eclipse.team.ui.showAllModels"); //$NON-NLS-1$
		menuCreator = new MenuCreator();
		setMenuCreator(menuCreator);
		update();
	}

	private ISynchronizationContext getSynchronizationContext() {
		return (ISynchronizationContext)configuration.getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}

	public void dispose() {
		if (menuCreator != null)
			menuCreator.dispose();
		getSynchronizationContext().getScope().removeScopeChangeListener(this);
		configuration.removePropertyChangeListener(listener);
		TeamUI.getTeamContentProviderManager().removePropertyChangeListener(listener);
	}

	private ModelProvider[] getEnabledModelProviders() {
		Set<ModelProvider> result = new HashSet<>();
		ModelProvider[] providers = ((ModelSynchronizeParticipant)configuration.getParticipant()).getEnabledModelProviders();
		providers = ModelOperation.sortByExtension(providers);
		for (ModelProvider provider : providers) {
			ITeamContentProviderDescriptor desc = TeamUI.getTeamContentProviderManager().getDescriptor(provider.getId());
			if (desc != null && desc.isEnabled()) {
				result.add(provider);
			}
		}
		return result.toArray(new ModelProvider[result.size()]);
	}

	private void addModelsToMenu(ModelProvider[] modelProviders) {
		String id = getActiveProviderId();
		for (ModelProvider provider : modelProviders) {
			Action action = new ShowModelProviderAction(configuration, provider);
			action.setChecked(provider.getDescriptor().getId().equals(id));
			menuManager.add(action);
		}
	}

	private String getActiveProviderId() {
		String id = (String)configuration.getProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER);
		if (id == null)
			id = ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE;
		return id;
	}

	private ModelProvider getNextProvider() {
		ModelProvider[] providers = getSynchronizationContext().getScope().getModelProviders();
		if (providers.length == 0)
			return null;
		providers = ModelOperation.sortByExtension(providers);
		String id = getActiveProviderId();
		int index = 0;
		if (id != null) {
			for (int i = 0; i < providers.length; i++) {
				ModelProvider provider = providers[i];
				if (provider.getDescriptor().getId().equals(id)) {
					index = i + 1;
					break;
				}
			}
			if (index == providers.length)
				index = 0;
		}
		return providers[index];
	}

	public void update() {
		ModelProvider next = getNextProvider();
		if (next == null) return;
		String text = NLS.bind(TeamUIMessages.ModelSelectionDropDownAction_1, next.getDescriptor().getLabel());
		setToolTipText(text);
		setText(text);
		if (menuManager != null) {
			showAllAction.setChecked(getActiveProviderId().equals(ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE));
			showAllFlatAction.setChecked(isFlatEnabled());
			IContributionItem[] items = menuManager.getItems();
			for (IContributionItem item : items) {
				if (item instanceof ActionContributionItem) {
					ActionContributionItem aci = (ActionContributionItem) item;
					IAction a = aci.getAction();
					if (a instanceof ShowModelProviderAction) {
						ShowModelProviderAction action = (ShowModelProviderAction) a;
						action.setChecked(action.getProviderId().equals(getActiveProviderId()));
					}
				}
			}
		}
		// TODO: need to update the check mark
	}

	private boolean isFlatEnabled() {
		String p = (String)configuration.getProperty(ITeamContentProviderManager.PROP_PAGE_LAYOUT);
		return p != null && p.equals(ITeamContentProviderManager.FLAT_LAYOUT);
	}

	@Override
	public void run() {
		ModelProvider next = getNextProvider();
		if (next == null) return;
		Action action = new ShowModelProviderAction(configuration, next);
		action.run();
	}

	@Override
	public void scopeChanged(ISynchronizationScope scope, ResourceMapping[] newMappings, ResourceTraversal[] newTraversals) {
		if (newMappings.length > 0) {
			rebuildMenu();
		}
	}

	private void rebuildMenu() {
		Display display = TeamUIPlugin.getStandardDisplay();
		display.asyncExec(() -> {
			if(menuManager != null) {
				menuManager.dispose();
				menuManager = null;
			}
			update();
		});
	}
}
