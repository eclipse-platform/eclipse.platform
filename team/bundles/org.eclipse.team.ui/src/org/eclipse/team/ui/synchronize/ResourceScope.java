/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

	@Override
	public String getName() {
		return Utils.convertSelection(resources);
	}

	@Override
	public IResource[] getRoots() {
		return resources;
	}

	@Override
	public void dispose() {
		// Nothing to dispose
	}

	@Override
	public void saveState(IMemento memento) {
		if (resources != null) {
			for (IResource resource : resources) {
				IMemento rootNode = memento.createChild(CTX_ROOT);
				rootNode.putString(CTX_ROOT_PATH, resource.getFullPath().toString());
			}
		}
	}

	@Override
	protected void init(IMemento memento) {
		IMemento[] rootNodes = memento.getChildren(CTX_ROOT);
		if(rootNodes != null) {
			List<IResource> resources = new ArrayList<>();
			for (IMemento rootNode : rootNodes) {
				IPath path = new Path(rootNode.getString(CTX_ROOT_PATH));
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path, true /* include phantoms */);
				if(resource != null) {
					resources.add(resource);
				}
			}
			this.resources = resources.toArray(new IResource[resources.size()]);
		}
	}
}
