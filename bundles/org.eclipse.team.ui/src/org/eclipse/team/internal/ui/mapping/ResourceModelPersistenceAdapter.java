/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.SynchronizationCompareAdapter;
import org.eclipse.ui.*;

public class ResourceModelPersistenceAdapter extends SynchronizationCompareAdapter {

	private static final String RESOURCES = "resources"; //$NON-NLS-1$
	private static final String RESOURCE_PATH = "resourcePath"; //$NON-NLS-1$
	private static final String RESOURCE_TYPE = "resourceType"; //$NON-NLS-1$
	private static final String WORKING_SETS = "workingSets"; //$NON-NLS-1$
	private static final String WORKING_SET_NAME = "workingSetName"; //$NON-NLS-1$
	private static final String MODEL_PROVIDERS = "modelProviders"; //$NON-NLS-1$
	private static final String MODEL_PROVIDER_ID = "modelProviderId"; //$NON-NLS-1$

	public ResourceModelPersistenceAdapter() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.SynchronizationCompareAdapter#save(org.eclipse.core.resources.mapping.ResourceMapping[], org.eclipse.ui.IMemento)
	 */
	@Override
	public void save(ResourceMapping[] mappings, IMemento memento) {
		for (int i = 0; i < mappings.length; i++) {
			ResourceMapping mapping = mappings[i];
			Object object = mapping.getModelObject();
			if (object instanceof IResource) {
				IResource resource = (IResource) object;
				IMemento child = memento.createChild(RESOURCES);
				child.putInteger(RESOURCE_TYPE, resource.getType());
				child.putString(RESOURCE_PATH, resource.getFullPath().toString());
			} else if (object instanceof IWorkingSet) {
				IWorkingSet ws = (IWorkingSet) object;
				IMemento child = memento.createChild(WORKING_SETS);
				child.putString(WORKING_SET_NAME, ws.getName());
			} else if (object instanceof ModelProvider) {
				ModelProvider provider = (ModelProvider) object;
				IMemento child = memento.createChild(MODEL_PROVIDERS);
				child.putString(MODEL_PROVIDER_ID, provider.getId());
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.SynchronizationCompareAdapter#restore(org.eclipse.ui.IMemento)
	 */
	@Override
	public ResourceMapping[] restore(IMemento memento) {
		IMemento[] children = memento.getChildren(RESOURCES);
		List result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IMemento child = children[i];
			Integer typeInt = child.getInteger(RESOURCE_TYPE);
			if (typeInt == null)
				continue;
			int type = typeInt.intValue();
			String pathString = child.getString(RESOURCE_PATH);
			if (pathString == null)
				continue;
			IPath path = new Path(pathString);
			IResource resource;
			switch (type) {
			case IResource.ROOT:
				resource = ResourcesPlugin.getWorkspace().getRoot();
				break;
			case IResource.PROJECT:
				resource = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
				break;
			case IResource.FILE:
				resource = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				break;
			case IResource.FOLDER:
				resource = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
				break;
			default:
				resource = null;
				break;
			}
			if (resource != null) {
				ResourceMapping mapping = Utils.getResourceMapping(resource);
				if (mapping != null) {
					result.add(mapping);
				}
			}
		}
		children = memento.getChildren(WORKING_SETS);
		for (int i = 0; i < children.length; i++) {
			IMemento child = children[i];
			String name = child.getString(WORKING_SET_NAME);
			if (name == null)
				continue;
			IWorkingSet set = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
			if (set != null) {
				ResourceMapping mapping = Utils.getResourceMapping(set);
				if (mapping != null)
					result.add(mapping);
			}
		}
		children = memento.getChildren(MODEL_PROVIDERS);
		for (int i = 0; i < children.length; i++) {
			IMemento child = children[i];
			String id = child.getString(MODEL_PROVIDER_ID);
			if (id == null)
				continue;
			IModelProviderDescriptor desc = ModelProvider.getModelProviderDescriptor(id);
			if (desc == null)
				continue;
			try {
				ModelProvider provider = desc.getModelProvider();
				if (provider != null) {
					ResourceMapping mapping = Utils.getResourceMapping(provider);
					if (mapping != null)
						result.add(mapping);
				}
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		return (ResourceMapping[]) result.toArray(new ResourceMapping[result.size()]);
	}

}
