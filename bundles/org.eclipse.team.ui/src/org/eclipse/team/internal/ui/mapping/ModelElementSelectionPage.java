/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.GlobalRefreshElementSelectionPage;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.navigator.*;
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
		List result = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			IResource resource = roots[i];
			result.add(Utils.getResourceMapping(resource));
		}
		manager = new SynchronizationScopeManager(TeamUIMessages.ModelElementSelectionPage_0, (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]), 
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
		fViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
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
			List result = new ArrayList();
			IWorkingSet[] sets = getWorkingSets();
			for (int i = 0; i < sets.length; i++) {
				IWorkingSet set = sets[i];
				result.add(Utils.getResourceMapping(set));
			}
			return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
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
		List result = new ArrayList();
		Object[] objects = getRootElement();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			ResourceMapping mapping = Utils.getResourceMapping(object);
			if (mapping != null) {
				result.add(mapping);
			}
		}
		return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
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
		List allWorkingSetElements = new ArrayList();
		IWorkingSet[] workingSets = getWorkingSets();
		for (int i = 0; i < workingSets.length; i++) {
			IWorkingSet set = workingSets[i];
			allWorkingSetElements.addAll(computeSelectedResources(new StructuredSelection(set.getElements())));
		}
		getViewer().setCheckedElements(allWorkingSetElements.toArray());
		return !allWorkingSetElements.isEmpty();
	}

	private Collection computeSelectedResources(StructuredSelection selection) {
		List result = new ArrayList();
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
		for (int i = 0; i < mappings.length; i++) {
			ResourceMapping m = mappings[i];
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
				for (int i = 0; i < providers.length; i++) {
					if (isEnabled(providers[i])){
						foundEnabledModelProvider = true;
						break;
					}
				}
				
				if (!foundEnabledModelProvider){
					if (MessageDialog.openConfirm(getShell(), TeamUIMessages.ModelElementSelectionPage_AllModelsDisabledTitle, TeamUIMessages.ModelElementSelectionPage_AllModelsDisabledMessage)) {
						ArrayList teamProviderDescriptors = new ArrayList();
						for (int i = 0; i < providers.length; i++)
							teamProviderDescriptors.add(TeamUI.getTeamContentProviderManager().getDescriptor(providers[i].getId()));
						
						ITeamContentProviderDescriptor[] desc = (ITeamContentProviderDescriptor[]) teamProviderDescriptors.toArray(new ITeamContentProviderDescriptor[teamProviderDescriptors.size()]);
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
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						manager.initialize(monitor);
						initialized = true;
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
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
			ArrayList disabledProviders = new ArrayList();
			for (int i = 0; i < providers.length; i++) {
				if (!providers[i].getId().equals(modelProviderId)) {
					disabledProviders.add(providers[i]);
				}
			}

			for (Iterator iterator = disabledProviders.iterator(); iterator.hasNext();) {
				ModelProvider disable = (ModelProvider) iterator.next();
				fViewer.setChecked(disable, false);
			}
		}
			
		
	}

}
