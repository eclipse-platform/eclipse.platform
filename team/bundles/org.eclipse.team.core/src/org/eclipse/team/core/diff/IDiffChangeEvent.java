/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.core.diff;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

/**
 * A change event that describes changes that have occurred
 * in an {@link IDiffTree}.
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IDiffChangeEvent {

	/**
	 * Returns the tree that has been changed.
	 * @return the tree that has been changed.
	 */
	public IDiffTree getTree();

	/**
	 * Returns the delta nodes that have been added to the delta tree.
	 * @return the delta nodes that have been added to the delta tree
	 */
	public IDiff[] getAdditions();

	/**
	 * Return the paths of the delta nodes that have been removed from the delta tree.
	 * @return the paths of the delta nodes that have been removed from the delta tree
	 */
	public IPath[] getRemovals();

	/**
	 * Return the delta nodes contained in the delta tree that have changed in some way.
	 * @return the delta nodes contained in the delta tree that have changed
	 */
	public IDiff[] getChanges();

	/**
	 * Return any errors that occurred while this change was taking place.
	 * @return any errors that occurred while this change was taking place
	 */
	public IStatus[] getErrors();
}
