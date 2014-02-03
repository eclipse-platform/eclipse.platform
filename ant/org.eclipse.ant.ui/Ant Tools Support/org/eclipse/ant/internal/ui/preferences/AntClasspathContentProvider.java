/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.preferences;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider that maintains a list of classpath entries which are shown in a tree
 * viewer.
 */
public class AntClasspathContentProvider implements ITreeContentProvider {
	private TreeViewer treeViewer;
	private ClasspathModel model= null;
	private boolean refreshEnabled= false;
	private boolean refreshRequested= false;
		
	public void add(IClasspathEntry parent, Object child) {
		Object newEntry= null;
		boolean added= false;
		if (parent == null || parent == model) {
			added= true;
			newEntry= model.addEntry(child);
			if (newEntry == null) {
				//entry already exists
				newEntry= model.createEntry(child, model);
				added= false;
			}
			parent= model;
		} else if (parent instanceof GlobalClasspathEntries) {
			GlobalClasspathEntries globalParent= (GlobalClasspathEntries) parent;
			newEntry= model.createEntry(child, globalParent);
			ClasspathEntry newClasspathEntry= (ClasspathEntry) newEntry;
			if (!globalParent.contains(newClasspathEntry)) {
				added= true;
				globalParent.addEntry(newClasspathEntry);
			}
		} 
		if (newEntry != null) {
			if (added) {
				treeViewer.add(parent, newEntry);
			}
			treeViewer.setExpandedState(parent, true);
			treeViewer.setSelection(new StructuredSelection(newEntry), true);
			refresh();
		}
	}
	
	public void add(int entryType, Object child) {
		Object newEntry= model.addEntry(entryType, child);
		if (newEntry != null) {
			treeViewer.add(getParent(newEntry), newEntry);
			refresh();
		}
	}

	public void removeAll() {
		model.removeAll();
		refresh();
	}
	
	private void refresh() {
		if (refreshEnabled) {
			treeViewer.refresh();
			refreshRequested= false;
		} else {
			refreshRequested= true;
		}
	}
	
	public void removeAllGlobalAntClasspathEntries() {
		model.removeAll(ClasspathModel.ANT_HOME);
		refresh();
	}

	/**
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof ClasspathEntry) {
			return ((ClasspathEntry)element).getParent();
		}
		if (element instanceof GlobalClasspathEntries) {
			return model;
		}
		
		return null;
	}

	/**
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof ClasspathEntry) {
			return false;
		}
		if (element instanceof GlobalClasspathEntries) {
			return ((GlobalClasspathEntries)element).hasEntries();
			
		} 
		
		if (element instanceof ClasspathModel) {
			return ((ClasspathModel) element).hasEntries();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		treeViewer = (TreeViewer) viewer;
		
		if (newInput != null) {
			model= (ClasspathModel)newInput;
		} else {
			if (model != null) {
				model.removeAll();
			}
			model= null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof GlobalClasspathEntries) {
			return ((GlobalClasspathEntries)parentElement).getEntries();
		}
		if (parentElement instanceof ClasspathModel) {
			return ((ClasspathModel)parentElement).getEntries();
		}
		if (parentElement == null) {
			List all= new ArrayList();
			Object[] topEntries= model.getEntries();
			for (int i = 0; i < topEntries.length; i++) {
				Object object = topEntries[i];
				if (object instanceof ClasspathEntry) {
					all.add(object);
				} else if (object instanceof GlobalClasspathEntries) {
					all.addAll(Arrays.asList(((GlobalClasspathEntries)object).getEntries()));
				}
			}
			return all.toArray();
		}
		
		return null;
	}

	public void remove(IStructuredSelection selection) {
		Object[] array= selection.toArray();
		model.removeAll(array);
		treeViewer.remove(array);
		refresh();
	}

	public ClasspathModel getModel() {
		return model;
	}

	public void setRefreshEnabled(boolean refreshEnabled) {
		this.refreshEnabled = refreshEnabled;
		treeViewer.getTree().setRedraw(refreshEnabled);
		if (refreshEnabled && refreshRequested) {
			refresh();
		}
	}

	/**
	 * @param currentParent
	 */
	public void setEntries(IClasspathEntry currentParent, List entries) {
		if (currentParent instanceof GlobalClasspathEntries) {
			GlobalClasspathEntries group= (GlobalClasspathEntries) currentParent;
			group.setEntries(entries);
		}
		
	}
}
