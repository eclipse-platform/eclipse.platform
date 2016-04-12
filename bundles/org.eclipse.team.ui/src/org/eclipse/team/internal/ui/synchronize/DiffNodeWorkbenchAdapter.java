/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class DiffNodeWorkbenchAdapter implements IWorkbenchAdapter {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object o) {
		DiffNode node = getDiffNode(o);
		return node != null ? node.getChildren() : new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object o) {
		DiffNode node = getDiffNode(o);
		if(node instanceof ISynchronizeModelElement) {
			return ((ISynchronizeModelElement)node).getImageDescriptor(o);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	@Override
	public String getLabel(Object o) {
		DiffNode node = getDiffNode(o);
		return node != null ? node.getName() : ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object o) {
		DiffNode node = getDiffNode(o);
		return node != null ? node.getParent() : null;
	}

	/*
	 * Return a diff node if the input object is a diff node or null otherwise.
	 */
	private DiffNode getDiffNode(Object element) {
		if(element instanceof DiffNode) {
			return (DiffNode)element;
		}
		return null;
	}
}
