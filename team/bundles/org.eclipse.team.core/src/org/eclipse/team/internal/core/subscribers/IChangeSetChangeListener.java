/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.runtime.IPath;


/**
 * Interface for registering change set change listeners with
 * the change set manager.
 */
public interface IChangeSetChangeListener {

	/**
	 * The given set has been added to the set manager.
	 * @param set the added set
	 */
	void setAdded(ChangeSet set);

	/**
	 * The default change set has change to be the given set.
	 * All new modifications will be placed in the default
	 * set.
	 * @param previousDefault
	 * @param set the default set
	 */
	void defaultSetChanged(ChangeSet previousDefault, ChangeSet set);

	/**
	 * The given set has been removed from the set manager.
	 * @param set the removed set
	 */
	void setRemoved(ChangeSet set);

	/**
	 * The title of the given set has changed.
	 * @param set the set whose title changed
	 */
	void nameChanged(ChangeSet set);

	/**
	 * The state of the given resources have change with respect to the
	 * given set. This means that the resource have either been added
	 * or removed from the set. Callers can use the resources contained
	 * in the set to determine if each resource is an addition or removal.
	 * @param set the set that has changed
	 * @param paths the paths of the resources whose containment state has changed w.r.t the set
	 */
	void resourcesChanged(ChangeSet set, IPath[] paths);

}
