/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.model;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;


public class AntModelContentProvider implements ITreeContentProvider {

	protected static final Object[] EMPTY_ARRAY= new Object[0];
	
	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}
    
	/**
	 * do nothing
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentNode) {
		if (parentNode instanceof AntElementNode) {
			AntElementNode parentElement = (AntElementNode)parentNode;
			if (parentElement.hasChildren()) {
				List children= parentElement.getChildNodes();
				return children.toArray();
			} 
		} else if (parentNode instanceof IAntModel) {
			return new Object[] {((IAntModel)parentNode).getProjectNode()};
		}
		return EMPTY_ARRAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object aNode) {
		AntElementNode tempElement = (AntElementNode)aNode;
		return tempElement.getParentNode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object aNode) {
		return ((AntElementNode)aNode).hasChildren();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IAntModel ) {
		    AntProjectNode projectNode= ((IAntModel) inputElement).getProjectNode();
		    if (projectNode == null) {
				return new AntElementNode[0];
			} 
			
			return new Object[] {projectNode}; 
		}
		
		if (inputElement instanceof Object[]) {
			return (Object[])inputElement;
		}
		return EMPTY_ARRAY;
	}
}
