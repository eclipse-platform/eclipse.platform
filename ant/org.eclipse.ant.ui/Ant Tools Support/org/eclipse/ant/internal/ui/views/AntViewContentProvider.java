/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - current implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ant.internal.ui.model.AntModelContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;


public class AntViewContentProvider extends AntModelContentProvider {
	
	private TreeViewer fTreeViewer;
	private List fElements= new ArrayList();
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object anInputElement) {
		if (anInputElement instanceof Object[]) {
			return fElements.toArray();
		}
		return EMPTY_ARRAY;
	}
	
	public void add(Object o) {
		if (fElements.contains(o)) {
			return;
		}
		fElements.add(o);
		fTreeViewer.add(fTreeViewer.getInput(), o);
		fTreeViewer.setSelection(new StructuredSelection(o), true);
	}
	
	public void addAll(Object[] o) {
		fElements.addAll(Arrays.asList(o));
		fTreeViewer.add(fTreeViewer.getInput(), o);
	}
	
	/**
	 * do nothing
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fTreeViewer= (TreeViewer)viewer;
	}
	
	public void remove(Object o) {
		fElements.remove(o);
		fTreeViewer.remove(o);
	}
	
	public void removeAll() {
		fTreeViewer.remove(fTreeViewer.getInput(), fElements.toArray());
		fElements.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}
}
