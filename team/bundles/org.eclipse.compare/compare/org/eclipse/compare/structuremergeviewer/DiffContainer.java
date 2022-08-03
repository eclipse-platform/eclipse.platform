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

import java.util.ArrayList;

/**
 * The standard implementation of a diff container element.
 * <p>
 * This class may be instantiated, or further subclassed.
 * </p>
 */
public abstract class DiffContainer extends DiffElement implements IDiffContainer {
	private static IDiffElement[] fgEmptyArray= new IDiffElement[0];
	private ArrayList<IDiffElement> fChildren;

	/**
	 * Creates a new container with the specified kind under the given parent.
	 *
	 * @param parent under which the new container is added as a child or <code>null</code>.
	 * @param kind of difference (defined in <code>Differencer</code>).
	 */
	public DiffContainer(IDiffContainer parent, int kind) {
		super(parent, kind);
	}

	/**
	 * Tries to find the child with the given name.
	 * Returns <code>null</code> if no such child exists.
	 *
	 * @param name of the child to find
	 * @return the first element with a matching name
	 */
	public IDiffElement findChild(String name) {
		Object[] children= getChildren();
		for (Object c : children) {
			IDiffElement child = (IDiffElement) c;
			if (name.equals(child.getName()))
				return child;
		}
		return null;
	}

	@Override
	public void add(IDiffElement diff) {
		if (fChildren == null)
			fChildren= new ArrayList<>();
		fChildren.add(diff);
		diff.setParent(this);
	}

	@Override
	public void removeToRoot(IDiffElement child) {
		if (fChildren != null) {
			fChildren.remove(child);
			child.setParent(null);
			if (fChildren.isEmpty()) {
				IDiffContainer p= getParent();
				if (p != null)
					p.removeToRoot(this);
			}
		}
	}

	/**
	 * Removes the given child (non-recursively) from this container.
	 *
	 * @param child to remove
	 */
	public void remove(IDiffElement child) {
		if (fChildren != null) {
			fChildren.remove(child);
			child.setParent(null);
		}
	}

	@Override
	public boolean hasChildren() {
		return fChildren != null && fChildren.size() > 0;
	}

	@Override
	public IDiffElement[] getChildren() {
		if (fChildren != null)
			return fChildren.toArray(fgEmptyArray);
		return fgEmptyArray;
	}
}
