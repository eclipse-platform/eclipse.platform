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

package org.eclipse.ant.internal.ui.preferences;

import org.eclipse.ant.core.IAntClasspathEntry;

public interface IClasspathEntry extends IAntClasspathEntry {

	/**
	 * Returns the classpath entries that are the children of this classpath entry
	 *
	 * @return the child classpath entries of this entry
	 */
	public IAntClasspathEntry[] getEntries();

	/**
	 * Returns whether this classpath entries has child entries.
	 *
	 * @return whether <code>true</code> if this classpath entry has childern, <code>false</code> otherwise.
	 */
	public boolean hasEntries();

	/**
	 * Returns the parent of this classpath entry
	 *
	 * @return the parent of this classpath entry, or <code>null</code> if none.
	 */
	public IClasspathEntry getParent();
}
