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

import org.eclipse.compare.ITypedElement;
import org.eclipse.swt.graphics.Image;

/**
 * An abstract base implementation of the <code>IDiffElement</code> interface.
 * <p>
 * Subclasses may add behavior and state, and may override <code>getImage</code>
 * and <code>getType</code> to suit.
 * </p>
 */
public abstract class DiffElement implements IDiffElement {
	private int fKind;
	private IDiffContainer fParent;

	/**
	 * Creates a new <code>DiffElement</code> as a child of the given parent.
	 * If parent is not <code>null</code> the new element is added to the parent.
	 *
	 * @param parent the parent of this child; if not <code>null</code> this element is automatically added as a child
	 * @param kind the kind of change
	 */
	public DiffElement(IDiffContainer parent, int kind) {
		fParent= parent;
		fKind= kind;
		if (parent != null)
			parent.add(this);
	}

	/**
	 * The <code>DiffElement</code> implementation of this <code>ITypedInput</code>
	 * method returns <code>null</code>. Subclasses may re-implement to provide
	 * an image for this element.
	 * @return <code>null</code>.
	 */
	@Override
	public Image getImage() {
		return null;
	}

	/**
	 * The <code>DiffElement</code> implementation of this <code>ITypedElement</code>
	 * method returns <code>ITypedElement.UNKNOWN_TYPE</code>. Subclasses may
	 * re-implement to provide a type for this element.
	 * @return <code>ITypedElement.UNKNOWN_TYPE</code>.
	 */
	@Override
	public String getType() {
		return ITypedElement.UNKNOWN_TYPE;
	}

	/**
	 * Sets the kind of difference for this element.
	 *
	 * @param kind set the kind of difference this element represents
	 * @see Differencer
	 */
	public void setKind(int kind) {
		fKind= kind;
	}

	@Override
	public int getKind() {
		return fKind;
	}

	@Override
	public IDiffContainer getParent() {
		return fParent;
	}

	@Override
	public void setParent(IDiffContainer parent) {
		fParent= parent;
	}
}

