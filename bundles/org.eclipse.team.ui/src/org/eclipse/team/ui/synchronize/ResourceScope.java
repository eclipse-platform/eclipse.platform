/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IMemento;

/**
 * A synchronize scope whose roots are a set of resources.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ResourceScope extends AbstractSynchronizeScope {

	/*
	 * Constants used to save and restore this scope
	 */
	private final static String CTX_ROOT = "resource_scope_roots"; //$NON-NLS-1$
	private final static String CTX_ROOT_PATH = "resource_scope_root_resource"; //$NON-NLS-1$

	/*
	 * The resources that define this scope
	 */
	private IResource[] resources;

	/**
	 * Create the resource scope for the given resources
	 *
	 * @param resources the resources that define this scope
	 */
	public ResourceScope(IResource[] resources) {
		this.resources = resources;
	}

	/**
	 * Create this scope from it's previously saved state
	 *
	 * @param memento persisted state that can be restored
	 */
	protected ResourceScope(IMemento memento) {
		super(memento);
	}

	/**
	 * Set the resources that define this scope
	 *
	 * @param resources the resources that define this scope
	 */
	public void setResources(IResource[] resources) {
		this.resources = resources;
		fireRootsChanges();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ScopableSubscriberParticipant.ISynchronizeScope#getName()
	 */
	@Override
	public String getName() {
		return Utils.convertSelection(resources);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ScopableSubscriberParticipant.ISynchronizeScope#getRoots()
	 */
	@Override
	public IResource[] getRoots() {
		return resources;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ScopableSubscriberParticipant.ISynchronizeScope#dispose()
	 */
	@Override
	public void dispose() {
		// Nothing to dispose
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.ScopableSubscriberParticipant.ISynchronizeScope#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		if (resources != null) {
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				IMemento rootNode = memento.createChild(CTX_ROOT);
				rootNode.putString(CTX_ROOT_PATH, resource.getFullPath().toString());
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeScope#init(org.eclipse.ui.IMemento)
	 */
	@Override
	protected void init(IMemento memento) {
		IMemento[] rootNodes = memento.getChildren(CTX_ROOT);
		if(rootNodes != null) {
			List resources = new ArrayList();
			for (int i = 0; i < rootNodes.length; i++) {
				IMemento rootNode = rootNodes[i];
				IPath path = new Path(rootNode.getString(CTX_ROOT_PATH));
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path, true /* include phantoms */);
				if(resource != null) {
					resources.add(resource);
				}
			}
			this.resources = (IResource[]) resources.toArray(new IResource[resources.size()]);
		}
	}
}
