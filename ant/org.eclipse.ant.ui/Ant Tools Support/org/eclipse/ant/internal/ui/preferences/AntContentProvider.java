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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 * Content provider that maintains a generic list of objects which are shown in a table viewer.
 */
public class AntContentProvider implements IStructuredContentProvider {
	protected List<Object> elements = new ArrayList<>();
	protected TableViewer tableViewer;
	private ViewerComparator fComparator = null;
	/**
	 * @since 3.5
	 */
	private boolean defaultcomparator = true;

	/**
	 * Default Constructor
	 */
	public AntContentProvider() {
	}

	/**
	 * Constructor
	 * 
	 * @param defaultcomparator
	 *            if the default comparator should be used for the returned data
	 * @since 3.5
	 */
	public AntContentProvider(boolean defaultcomparator) {
		this.defaultcomparator = defaultcomparator;
	}

	/**
	 * Add an element to the current listing of elements and to the underlying viewer. Does nothing if the listing already contains the given element
	 */
	public void add(Object o) {
		if (elements.contains(o)) {
			return;
		}
		elements.add(o);
		tableViewer.add(o);
		tableViewer.setSelection(new StructuredSelection(o), true);
	}

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return elements.toArray();
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		tableViewer = (TableViewer) viewer;
		elements.clear();
		if (newInput != null) {
			if (defaultcomparator) {
				tableViewer.setComparator(getComparator());
			}
			List<?> list;
			if (newInput instanceof List) {
				list = ((List<?>) newInput);
			} else {
				list = Arrays.asList((Object[]) newInput);
			}
			elements.addAll(list);
		}
	}

	/**
	 * Removes the given element from the listing of elements and from the backing viewer.
	 */
	public void remove(Object o) {
		elements.remove(o);
		tableViewer.remove(o);
	}

	/**
	 * Removes the given selection of the listing of elements and from the backing viewer
	 */
	public void remove(IStructuredSelection selection) {
		Object[] array = selection.toArray();
		elements.removeAll(Arrays.asList(array));
		tableViewer.remove(array);
	}

	/**
	 * Returns the default comparator which compares the {@link #toString()} value of the elements for ordering
	 * 
	 * @return the default comparator
	 */
	protected ViewerComparator getComparator() {
		if (fComparator == null) {
			fComparator = new ViewerComparator() {
				@Override
				public int compare(Viewer viewer, Object e1, Object e2) {
					return e1.toString().compareToIgnoreCase(e2.toString());
				}
			};
		}
		return fComparator;
	}
}
