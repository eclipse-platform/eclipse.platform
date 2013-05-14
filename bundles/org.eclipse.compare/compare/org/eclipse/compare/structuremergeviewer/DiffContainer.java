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

import java.util.ArrayList;

/**
 * The standard implementation of a diff container element.
 * <p>
 * This class may be instantiated, or further subclassed.
 * </p>
 */
public abstract class DiffContainer extends DiffElement implements IDiffContainer {

	private static IDiffElement[] fgEmptyArray= new IDiffElement[0];
	private ArrayList fChildren;
	
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
		for (int i= 0; i < children.length; i++) {
			IDiffElement child= (IDiffElement) children[i];
			if (name.equals(child.getName()))
				return child;
		}
		return null;
	}

	/* (non Javadoc)
	 * see IDiffContainer.add
	 */
	public void add(IDiffElement diff) {
		if (fChildren == null)
			fChildren= new ArrayList();
		fChildren.add(diff);
		diff.setParent(this);
	}

	/*
	 * Removes the given child from this container.
	 * If the container becomes empty it is removed from its container.
	 */
	public void removeToRoot(IDiffElement child) {
		if (fChildren != null) {
			fChildren.remove(child);
			child.setParent(null);
			if (fChildren.size() == 0) {
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
	
	/* (non Javadoc)
	 * see IDiffContainer.hasChildren
	 */
	public boolean hasChildren() {
		return fChildren != null && fChildren.size() > 0;
	}

	/* (non Javadoc)
	 * see IDiffContainer.getChildren
	 */
	public IDiffElement[] getChildren() {
		if (fChildren != null)
			return (IDiffElement[]) fChildren.toArray(fgEmptyArray);
		return fgEmptyArray;
	}
}

