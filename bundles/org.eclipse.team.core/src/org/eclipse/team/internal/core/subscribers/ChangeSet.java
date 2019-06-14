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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;

/**
 * A set that contains a group of related changes
 */
public abstract class ChangeSet {

	private String name;

	/**
	 * Create a change set with no name. Subclasses
	 * that create a change set in this manner should
	 * provide a name before the set is used by other clients.
	 */
	protected ChangeSet() {
		super();
	}

	/**
	 * Create a change set with the given name.
	 * @param name the name of the change set
	 */
	public ChangeSet(String name) {
		this.name = name;
	}

	/**
	 * Return the resources that are contained in this set.
	 * @return the resources that are contained in this set
	 */
	public abstract IResource[] getResources();

	/**
	 * Return whether the set contains any files.
	 * @return whether the set contains any files
	 */
	public abstract boolean isEmpty();

	/**
	 * Return true if the given file is included in this set.
	 * @param local a local file
	 * @return true if the given file is included in this set
	 */
	public abstract boolean contains(IResource local);

	/**
	 * Remove the resource from the set.
	 * @param resource the resource to be removed
	 */
	public abstract void remove(IResource resource);

	/**
	 * Remove the resources from the set.
	 * @param resources the resources to be removed
	 */
	public void remove(IResource[] resources) {
		for (IResource resource : resources) {
			remove(resource);
		}
	}

	/**
	 * Remove the resource and it's descendants to the given depth.
	 * @param resource the resource to be removed
	 * @param depth the depth of the removal (one of
	 * <code>IResource.DEPTH_ZERO, IResource.DEPTH_ONE, IResource.DEPTH_INFINITE)</code>
	 */
	public abstract void rootRemoved(IResource resource, int depth);

	/**
	 * Return a comment describing the change.
	 * @return a comment describing the change
	 */
	public abstract String getComment();

	/**
	 * Return the name assigned to this set. The name should be
	 * unique.
	 * @return the name assigned to this set
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the change set. The name of a change
	 * set can be changed but it is up to subclass to notify
	 * any interested partied of the name change.
	 * @param name the new name for the set
	 */
	protected void setName(String name) {
		this.name = name;
	}

	/**
	 * Return whether the set contains descendants of the given resource
	 * to the given depth.
	 * @param resource the resource
	 * @param depth the depth
	 * @return whether the set contains descendants of the given resource
	 * to the given depth
	 */
	public abstract boolean containsChildren(IResource resource, int depth);
}
