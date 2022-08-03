/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic;

import org.eclipse.core.resources.IResource;

/**
 * An <code>IResourceStateListener</code> recieves callbacks
 * when the repository state of resources change, i.e. a file gets checked
 * in, a folder gets checked out, a project is no longer shared, etc.
 */
public interface IResourceStateListener {
	/**
	 * Notifies this listener that the state of the resources has changed.
	 * @param resources	An array of resources with changed states or an empty array.
	 */
	void stateChanged(IResource[] resources);
}
