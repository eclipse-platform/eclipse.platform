/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshElementSelectionPage;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.INavigatorContentServiceListener;
import org.eclipse.ui.navigator.NavigatorContentServiceFactory;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class ModelElementSelectionPage extends GlobalRefreshElementSelectionPage implements INavigatorContentServiceListener {

	private INavigatorContentService service;
	private ISynchronizationScopeManager manager;
	private ContainerCheckedTreeViewer fViewer;
	private boolean initialized;

	public ModelElementSelectionPage(IResource[] roots) {
		super("elementSelection"); //$NON-NLS-1$
		setDescription(TeamUIMessages.GlobalRefreshResourceSelectionPage_2);
		setTitle(TeamUIMessages.GlobalRefreshResourceSelectionPage_3);
		List<ResourceMapping> result = new ArrayList<>();
		for (IResource resource : roots) {
			result.add(Utils.getResourceMapping(resource));
		}
		manager = new SynchronizationScopeManager(TeamUIMessages.ModelElementSelectionPage_0, result.toArray(new ResourceMapping[result.size()]),
						ResourceMappingContext.LOCAL_CONTEXT, true);
	}

	@Override
	protected ContainerCheckedTreeViewer createViewer(Composite top) {
		GridData data;
		fViewer = new ContainerCheckedTreeViewer(top, SWT.BORDER);
		service = NavigatorContentServiceFactory.INSTANCE.createContentService(CommonViewerAdvisor.TEAM_NAVIGATOR_CONTENT, fViewer);
		service.bindExtensions(TeamUI.getTeamContentProviderManager().getContentProviderIds(manager.getScope()), true);
		service.getActivationService().activateExtensions(TeamUI.getTeamContentProviderManager().getContentProviderIds(manager.getScope()), true);
		service.addListener(this);
		data = new GridData(GridData.FILL_BOTH);
		//data.widthHint = 200;
		data.heightHint = 100;
		fViewer.getControl().setLayoutData(data);
		fViewer.setContentProvider(service.createCommonContentProvider());
		fViewer.setLabelProvider(new DecoratingLabelProvider(service.createCommonLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		fViewer.addCheckStateListener(event -> {
			Object element = event.getElement();
			//If the workspace model has been checked, switch the scope to workspace
			if (event.getChecked() && element instanceof ModelProvider && ((ModelProvider) element).getId().equals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID)) {
					setWorkspaceSelected(true);
			} else {
				//Get the resource mapping from the element
				ResourceMapping mapping = Utils.getResourceMapping(element);
				if (mapping != null) {
					if (!(element instanceof ModelProvider)) {
						uncheckOtherModels(mapping.getModelProviderId());
						event.getCheckable().setChecked(event.getElement(), event.getChecked());
					}
					updateOKStatus();
				} else
					updateOKStatus();
			}
		});
		fViewer.getTree().addTreeListener(new TreeListener(){

			@Override
			public void treeCollapsed(TreeEvent e) {
				//no-op
			}

			@Override
			public void treeExpanded(TreeEvent e) {
				if (isWorkingSetSelected())
					checkWorkingSetElements();
			}
		});
		fViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
		return fViewer;
	}

	public ResourceMapping[] getSelectedMappings() {
		if (isWorkingSetSelected()) {
			List<ResourceMapping> result = new ArrayList<>();
			IWorkingSet[] sets = getWorkingSets();
			for (IWorkingSet set : sets) {
				result.add(Utils.getResourceMapping(set));
			}
			return result.toArray(new ResourceMapping[result.size()]);
		}
		if (isWorkspaceSelected()) {
			try {
				ModelProvider provider = ModelProvider.getModelProviderDescriptor(ModelProvider.RESOURCE_MODEL_PROVIDER_ID).getModelProvider();
				ResourceMapping mapping = Utils.getResourceMapping(provider);
				if (mapping != null) {
					return new ResourceMapping[] {mapping };
				}
			} catch (CoreException e) {
				// Shouldn't happen
				TeamUIPlugin.log(e);
			}
			ResourceMapping[] mappings = manager.getScope().getMappings(ModelProvider.RESOURCE_MODEL_PROVIDER_ID);
			return mappings;
		}
		List<ResourceMapping> result = new ArrayList<>();
		Object[] objects = getRootElement();
		for (Object object : objects) {
			ResourceMapping mapping = Utils.getResourceMapping(object);
			if (mapping != null) {
				result.add(mapping);
			}
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	@Override
	public void dispose() {
		service.dispose();
		super.dispose();
	}

	@Override
	protected void checkAll() {
		getViewer().setCheckedElements(manager.getScope().getModelProviders());
	}

	@Override
	protected boolean checkWorkingSetElements() {
		List<Object> allWorkingSetElements = new ArrayList<>();
		IWorkingSet[] workingSets = getWorkingSets();
		for (IWorkingSet set : workingSets) {
			allWorkingSetElements.addAll(computeSelectedResources(new StructuredSelection(set.getElements())));
		}
		getViewer().setCheckedElements(allWorkingSetElements.toArray());
		return !allWorkingSetElements.isEmpty();
	}

	private Collection<Object> computeSelectedResources(StructuredSelection selection) {
		List<Object> result = new ArrayList<>();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object element = iter.next();
			ResourceMapping mapping = Utils.getResourceMapping(element);
			if (mapping != null && scopeContainsMapping(mapping)) {
				result.add(element);
			}
		}
		return result;
	}

	private boolean scopeContainsMapping(ResourceMapping mapping) {
		ResourceMapping[] mappings = manager.getScope().getMappings();
		for (ResourceMapping m : mappings) {
			if (m.contains(mapping)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onLoad(INavigatorContentExtension anExtension) {
		anExtension.getStateModel().setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE, manager.getScope());
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && !initialized) {
			initialize();
			if (initialized) {
				//check to see if all models are disabled
				ISynchronizationScope syncScope = manager.getScope();
				ModelProvider[] providers = syncScope.getModelProviders();
				boolean foundEnabledModelProvider = false;
				for (ModelProvider provider : providers) {
					if (isEnabled(provider)) {
						foundEnabledModelProvider = true;
						break;
					}
				}

				if (!foundEnabledModelProvider){
					if (MessageDialog.openConfirm(getShell(), TeamUIMessages.ModelElementSelectionPage_AllModelsDisabledTitle, TeamUIMessages.ModelElementSelectionPage_AllModelsDisabledMessage)) {
						ArrayList<ITeamContentProviderDescriptor> teamProviderDescriptors = new ArrayList<>();
						for (ModelProvider provider : providers) {
							teamProviderDescriptors.add(TeamUI.getTeamContentProviderManager().getDescriptor(provider.getId()));
						}

						ITeamContentProviderDescriptor[] desc = teamProviderDescriptors.toArray(new ITeamContentProviderDescriptor[teamProviderDescriptors.size()]);
						TeamUI.getTeamContentProviderManager().setEnabledDescriptors(desc);
					}
				}
				service.bindExtensions(TeamUI.getTeamContentProviderManager().getContentProviderIds(syncScope), true);
				service.getActivationService().activateExtensions(TeamUI.getTeamContentProviderManager().getContentProviderIds(syncScope), true);
				fViewer.setInput(syncScope);
				initializeScopingHint();
			}
		}
	}

	private void initialize() {
		try {
			getContainer().run(true, true, monitor -> {
				try {
					manager.initialize(monitor);
					initialized = true;
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			Utils.handleError(getShell(), e, null, null);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private boolean isEnabled(ModelProvider provider) {
		ITeamContentProviderDescriptor desc = TeamUI.getTeamContentProviderManager().getDescriptor(provider.getId());
		return (desc != null && desc.isEnabled());
	}

	private void uncheckOtherModels(String modelProviderId) {

		if (!isSelectedResourcesSelected()) {
			ModelProvider[] providers = manager.getScope().getModelProviders();
			ArrayList<ModelProvider> disabledProviders = new ArrayList<>();
			for (ModelProvider provider : providers) {
				if (!provider.getId().equals(modelProviderId)) {
					disabledProviders.add(provider);
				}
			}

			for (ModelProvider disable : disabledProviders) {
				fViewer.setChecked(disable, false);
			}
		}


	}

}
