/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.core.internal.dtree;

import org.eclipse.core.runtime.IPath;

/**
 * The result of doing a lookup() in a data tree.
 */
public final class DataTreeLookup {

	public final IPath key;
	public final boolean isPresent;
	public final Object data;
	public final boolean foundInFirstDelta;

	/**
	 * Constructors for internal use only.  Use factory methods.
	 */
	private DataTreeLookup(IPath key, boolean isPresent, Object data, boolean foundInFirstDelta) {
		this.key = key;
		this.isPresent = isPresent;
		this.data = data;
		this.foundInFirstDelta = foundInFirstDelta;
	}

	/**
	 * Factory method for creating a new lookup object.
	 */
	public static DataTreeLookup newLookup(IPath nodeKey, boolean isPresent, Object data) {
		return new DataTreeLookup(nodeKey, isPresent, data, false);
	}

	/**
	 * Factory method for creating a new lookup object.
	 */
	public static DataTreeLookup newLookup(IPath nodeKey, boolean isPresent, Object data, boolean foundInFirstDelta) {
		return new DataTreeLookup(nodeKey, isPresent, data, foundInFirstDelta);
	}
}
