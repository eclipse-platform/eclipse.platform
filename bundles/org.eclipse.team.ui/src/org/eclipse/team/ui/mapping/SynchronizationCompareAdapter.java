/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.util.*;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ResourceDiffCompareInput;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * An abstract implementation of {@link ISynchronizationCompareAdapter}.
 * <p>
 * Clients may subclass this class.
 *
 * @since 3.2
 */
public abstract class SynchronizationCompareAdapter implements ISynchronizationCompareAdapter {

	/**
	 * Default implementation that is capable of returning a compare input for objects
	 * that adapt to {@link IFile}. Subclasses should override if compare inputs are
	 * available for other types of model elements.
	 * @see ISynchronizationCompareAdapter#asCompareInput(ISynchronizationContext, Object)
	 */
	@Override
	public ICompareInput asCompareInput(ISynchronizationContext context, Object o) {
		IResource resource = Utils.getResource(o);
		if (resource != null) {
			if (resource.getType() == IResource.FILE) {
				IDiff node = context.getDiffTree().getDiff(resource);
				if (node != null)
					return new ResourceDiffCompareInput(node, context);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ICompareAdapter#hasCompareInput(org.eclipse.team.core.mapping.ISynchronizationContext, java.lang.Object)
	 */
	@Override
	public boolean hasCompareInput(ISynchronizationContext context, Object object) {
		return asCompareInput(context, object) != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingPersistenceAdapter#getName(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
	@Override
	public String getName(ResourceMapping mapping) {
		Object object = mapping.getModelObject();
		IWorkbenchAdapter adapter = (IWorkbenchAdapter) Utils.getAdapter(
				object, IWorkbenchAdapter.class);
		if (adapter != null) {
			String label = adapter.getLabel(object);
			if (label != null)
				return label;
		}
		IResource resource = Utils.getResource(object);
		if (resource != null)
			return resource.getName();
		if (object instanceof ModelProvider) {
			ModelProvider provider = (ModelProvider) object;
			if (provider.getId().equals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID)) {
				return TeamUIMessages.SynchronizationCompareAdapter_0;
			}
			return provider.getDescriptor().getLabel();
		}
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingPersistenceAdapter#getFullPath(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
	@Override
	public String getPathString(ResourceMapping mapping) {
		Object object = mapping.getModelObject();
		IWorkbenchAdapter adapter = (IWorkbenchAdapter) Utils.getAdapter(
				object, IWorkbenchAdapter.class);
		if (adapter != null) {
			List segments = new ArrayList();
			Object parent = object;
			do {
				String segment = adapter.getLabel(parent);
				if (segment != null && segment.length() > 0)
					segments.add(0, segment);
				parent = adapter.getParent(parent);
			} while (parent != null);
			if (!segments.isEmpty()) {
				IPath path = Path.EMPTY;
				for (Iterator iter = segments.iterator(); iter.hasNext();) {
					String segment = (String) iter.next();
					path = path.append(segment);
				}
				return path.toString();
			}
		}
		return getName(mapping);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter#getImageDescriptor(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
	@Override
	public ImageDescriptor getImageDescriptor(ResourceMapping mapping) {
		Object object = mapping.getModelObject();
		ImageDescriptor image = getImageDescriptorFromWorkbenchAdapter(object);
		if (image != null)
			return image;
		IResource resource = Utils.getResource(object);
		if (resource != null) {
			image = getImageDescriptorFromWorkbenchAdapter(resource);
			if (image != null)
				return image;
		}
		if (object instanceof ModelProvider) {
			ModelProvider provider = (ModelProvider) object;
			ITeamContentProviderDescriptor desc = TeamUI.getTeamContentProviderManager().getDescriptor(provider.getId());
			if (desc != null)
				return desc.getImageDescriptor();
		}
		return null;
	}

	private ImageDescriptor getImageDescriptorFromWorkbenchAdapter(Object object) {
		IWorkbenchAdapter adapter = (IWorkbenchAdapter) Utils.getAdapter(
				object, IWorkbenchAdapter.class);
		if (adapter != null) {
			return adapter.getImageDescriptor(object);
		}
		return null;
	}

	/**
	 * Return the synchronization state of the resource mapping with respect to
	 * the given team state provider. This method is invoked from instances of
	 * {@link ITeamStateProvider} when the synchronization state description for
	 * an element is requested.
	 *
	 * @param provider
	 *            the team state provider
	 * @param mapping
	 *            the element
	 * @param stateMask
	 *            the state mask that indicates which state flags are desired
	 * @param monitor
	 *            a progress monitor
	 * @return the synchronization state of the element or -1 if the calculation
	 *         of the state should be done using the resources of the mapping.
	 * @throws CoreException
	 *
	 * @since 3.3
	 */
	@Override
	public int getSynchronizationState(ITeamStateProvider provider, ResourceMapping mapping, int stateMask, IProgressMonitor monitor) throws CoreException {
		return -1;
	}

}
