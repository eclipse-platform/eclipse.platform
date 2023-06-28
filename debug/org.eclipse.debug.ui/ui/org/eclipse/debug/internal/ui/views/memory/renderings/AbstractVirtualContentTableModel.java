/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.util.ArrayList;

import org.eclipse.debug.internal.ui.viewers.AsynchronousTableModel;
import org.eclipse.debug.internal.ui.viewers.AsynchronousViewer;
import org.eclipse.debug.internal.ui.viewers.ModelNode;

abstract public class AbstractVirtualContentTableModel extends AsynchronousTableModel {

	public AbstractVirtualContentTableModel(AsynchronousViewer viewer) {
		super(viewer);
	}

	public Object[] getElements() {
		ModelNode[] nodes = getNodes(getRootNode().getElement());
		ArrayList<Object> result = new ArrayList<>();
		if (nodes != null) {
			for (ModelNode node : nodes) {
				ModelNode[] children = node.getChildrenNodes();
				if (children != null) {
					for (ModelNode child : children) {
						result.add(child.getElement());
					}
				}
			}

			return result.toArray();
		}
		return new Object[0];
	}

	public Object getElement(int idx) {
		Object[] elements = getElements();
		if (idx >= 0 && idx < elements.length) {
			return elements[idx];
		}

		return null;
	}

	public int indexOfElement(Object element) {
		Object[] elements = getElements();

		for (int i = 0; i < elements.length; i++) {
			if (elements[i] == element) {
				return i;
			}
		}
		return -1;
	}

	abstract public int indexOfKey(Object key);

	abstract public int columnOf(Object element, Object key);

	abstract public Object getKey(int idx);

	abstract public Object getKey(Object element);

	abstract public Object getKey(int idx, int col);

	public void handleViewerChanged() {

	}

}
