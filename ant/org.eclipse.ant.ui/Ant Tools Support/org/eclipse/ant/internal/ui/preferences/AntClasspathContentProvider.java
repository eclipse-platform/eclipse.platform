/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider that maintains a list of classpath entries which are shown in a tree viewer.
 */
public class AntClasspathContentProvider implements ITreeContentProvider {
	private TreeViewer treeViewer;
	private ClasspathModel model = null;
	private boolean refreshEnabled = false;
	private boolean refreshRequested = false;

	public void add(IClasspathEntry parent, Object child) {
		Object newEntry = null;
		boolean added = false;
		if (parent == null || parent == model) {
			added = true;
			newEntry = model.addEntry(child);
			if (newEntry == null) {
				// entry already exists
				newEntry = model.createEntry(child, model);
				added = false;
			}
			parent = model;
		} else if (parent instanceof GlobalClasspathEntries) {
			GlobalClasspathEntries globalParent = (GlobalClasspathEntries) parent;
			newEntry = model.createEntry(child, globalParent);
			ClasspathEntry newClasspathEntry = (ClasspathEntry) newEntry;
			if (!globalParent.contains(newClasspathEntry)) {
				added = true;
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
		Object newEntry = model.addEntry(entryType, child);
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
			refreshRequested = false;
		} else {
			refreshRequested = true;
		}
	}

	public void removeAllGlobalAntClasspathEntries() {
		model.removeAll(ClasspathModel.ANT_HOME);
		refresh();
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof ClasspathEntry) {
			return ((ClasspathEntry) element).getParent();
		}
		if (element instanceof GlobalClasspathEntries) {
			return model;
		}

		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof ClasspathEntry) {
			return false;
		}
		if (element instanceof GlobalClasspathEntries) {
			return ((GlobalClasspathEntries) element).hasEntries();

		}

		if (element instanceof ClasspathModel) {
			return ((ClasspathModel) element).hasEntries();
		}
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void dispose() {
		// do nothing

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		treeViewer = (TreeViewer) viewer;

		if (newInput != null) {
			model = (ClasspathModel) newInput;
		} else {
			if (model != null) {
				model.removeAll();
			}
			model = null;
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof GlobalClasspathEntries) {
			return ((GlobalClasspathEntries) parentElement).getEntries();
		}
		if (parentElement instanceof ClasspathModel) {
			return ((ClasspathModel) parentElement).getEntries();
		}
		if (parentElement == null) {
			List<Object> all = new ArrayList<>();
			for (IAntClasspathEntry object : model.getEntries()) {
				if (object instanceof ClasspathEntry) {
					all.add(object);
				} else if (object instanceof GlobalClasspathEntries) {
					all.addAll(Arrays.asList(((GlobalClasspathEntries) object).getEntries()));
				}
			}
			return all.toArray();
		}

		return null;
	}

	public void remove(IStructuredSelection selection) {
		Object[] array = selection.toArray();
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
	public void setEntries(IClasspathEntry currentParent, List<IAntClasspathEntry> entries) {
		if (currentParent instanceof GlobalClasspathEntries) {
			GlobalClasspathEntries group = (GlobalClasspathEntries) currentParent;
			group.setEntries(entries);
		}

	}
}
