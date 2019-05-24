/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.examples.model.ui.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.ide.IContributorResourceAdapter2;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.IExtensionStateModel;

public class ThirdPartyActionProvider extends CommonActionProvider {

	private Action exampleAction;

	public ThirdPartyActionProvider() {
		// Nothing to do
	}

	/**
	 * Return the configuration from the synchronize page that contains
	 * the common viewer.
	 * @return the configuration from the synchronize page that contains
	 * the common viewer
	 */
	protected final ISynchronizePageConfiguration getSynchronizePageConfiguration() {
		return (ISynchronizePageConfiguration)getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION);
	}

	/**
	 * Return the extension state model for the content provider associated with
	 * action provider.
	 * @return the extension state model for the content provider associated with
	 * action provider
	 */
	protected final IExtensionStateModel getExtensionStateModel() {
		return getActionSite().getExtensionStateModel();
	}

	/**
	 * Return the synchronization context to which the actions of this provider
	 * apply.
	 * @return the synchronization context to which the actions of this provider
	 * apply
	 */
	protected final ISynchronizationContext getSynchronizationContext() {
		return (ISynchronizationContext)getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}

	@Override
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		exampleAction = new Action("3rd Party Action") {
			@Override
			public void run() {
				StringBuilder buffer = new StringBuilder();
				boolean addComma = false;
				IStructuredSelection selection = (IStructuredSelection)getContext().getSelection();
				ResourceMapping[] mappings = getResourceMappings(selection.toArray());
				for (ResourceMapping mapping : mappings) {
					ISynchronizationCompareAdapter adapter = getCompareAdpater(mapping);
					if (adapter != null) {
						String name = adapter.getName(mapping);
						if (addComma) {
							buffer.append(", ");
						}
						buffer.append(name);
						addComma = true;
					}
				}
				MessageDialog.openInformation(getActionSite().getViewSite().getShell(), "Example Action", "You have executed a third party action on the selected elements: " + buffer.toString());
			}
		};
	}

	protected ISynchronizationCompareAdapter getCompareAdpater(ResourceMapping mapping) {
		if (mapping != null) {
			ModelProvider provider = mapping.getModelProvider();
			if (provider != null) {
				Object o = provider.getAdapter(ISynchronizationCompareAdapter.class);
				if (o instanceof ISynchronizationCompareAdapter) {
					return (ISynchronizationCompareAdapter) o;
				}
			}
		}
		return null;
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		menu.add(exampleAction);
	}

	private ResourceMapping[] getResourceMappings(Object[] objects) {
		List<ResourceMapping> result = new ArrayList<>();
		for (Object object : objects) {
			ResourceMapping mapping = getResourceMapping(object);
			if (mapping != null)
				result.add(mapping);
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	private ResourceMapping getResourceMapping(Object o) {
		if (o instanceof ResourceMapping) {
			return (ResourceMapping) o;
		}
		if (o instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) o;
			Object adapted = adaptable.getAdapter(ResourceMapping.class);
			if (adapted instanceof ResourceMapping) {
				return(ResourceMapping) adapted;
			}
			adapted = adaptable.getAdapter(IContributorResourceAdapter.class);
			if (adapted instanceof IContributorResourceAdapter2) {
				IContributorResourceAdapter2 cra = (IContributorResourceAdapter2) adapted;
				return cra.getAdaptedResourceMapping(adaptable);
			}
		} else {
			Object adapted = Platform.getAdapterManager().getAdapter(o, ResourceMapping.class);
			if (adapted instanceof ResourceMapping) {
				return(ResourceMapping) adapted;
			}
		}
		return null;
	}
}
