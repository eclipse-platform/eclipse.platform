/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.compare.structuremergeviewer;

/**
 * Interface used to compare hierarchical structures.
 * It is used by the differencing engine.
 * <p>
 * Clients typically implement this interface in an adaptor class which
 * wrappers the objects to be compared.
 *
 * @see org.eclipse.compare.ResourceNode
 * @see Differencer
 */
public interface IStructureComparator {
	/**
	 * Returns an iterator for all children of this object or <code>null</code>
	 * if there are no children.
	 *
	 * @return an array with all children of this object, or an empty array if there are no children
	 */
	Object[] getChildren();

	/**
	 * Returns whether some other object is "equal to" this one
	 * with respect to a structural comparison. For example, when comparing
	 * Java class methods, <code>equals</code> would return <code>true</code>
	 * if two methods have the same signature (the argument names and the
	 * method body might differ).
	 *
	 * @param other the reference object with which to compare
	 * @return <code>true</code> if this object is the same as the other argument; <code>false</code> otherwise
	 * @see java.lang.Object#equals
	 */
	@Override
	boolean equals(Object other);
}
