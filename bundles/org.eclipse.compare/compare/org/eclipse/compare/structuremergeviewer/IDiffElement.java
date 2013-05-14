/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.structuremergeviewer;

import org.eclipse.compare.ITypedElement;

/**
 * An <code>IDiffElement</code> is used in the <code>DiffTreeViewer</code>
 * to display the kind of change detected as the result of a two-way or three-way compare.
 * <p>
 * The base interface <code>ITypedElement</code> provides a name, a type, and an image.
 * <code>IDiffElement</code> adds API for maintaining a parent relationship.
 * <p>
 * <code>DiffTreeViewer</code> works on a tree of <code>IDiffElements</code>.
 * Leaf elements must implement the
 * <code>IDiffElement</code> interface, inner nodes the <code>IDiffContainer</code> interface.
 * <p>
 * <code>IDiffElement</code>s are typically created as the result of performing
 * a compare with the <code>Differencer</code>.
 * <p>
 * Clients may implement this interface, or use one of the standard implementations,
 * <code>DiffElement</code>, <code>DiffContainer</code>, or <code>DiffNode</code>.
 *
 * @see DiffTreeViewer
 * @see DiffElement
 * @see DiffContainer
 * @see DiffNode
 */
public interface IDiffElement extends ITypedElement {
	
	/**
	 * Returns the kind of difference as defined in <code>Differencer</code>.
	 *
	 * @return the kind of difference as defined in <code>Differencer</code>
	 */
	int getKind();

	/**
	 * Returns the parent of this element.
	 * If the object is the root of a hierarchy <code>null</code> is returned.
	 *
	 * @return the parent of this element, or <code>null</code> if the element has no parent
	 */
	IDiffContainer getParent();

	/**
	 * Sets the parent of this element.
	 *
	 * @param parent the new parent of this element, or <code>null</code> if this
	 *   element is to have no parent
	 */
	void setParent(IDiffContainer parent);
}
