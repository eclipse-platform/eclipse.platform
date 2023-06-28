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
package org.eclipse.debug.internal.ui.viewers;

import java.util.ArrayList;
import java.util.List;


/**
 * @since 3.2
 *
 */
public class AsynchronousTableModel extends AsynchronousModel {

	/**
	 * Constructs a new table model.
	 *
	 * @param viewer the backing viewer
	 */
	public AsynchronousTableModel(AsynchronousViewer viewer) {
		super(viewer);
	}

	@Override
	protected void add(ModelNode parent, Object element) {}

	/**
	 * Adds the given elements to the table.
	 *
	 * @param elements the new elements to add
	 */
	public void add(Object[] elements) {
		TableAddRequestMonitor update = new TableAddRequestMonitor(getRootNode(), elements, this);
		requestScheduled(update);
		update.done();
	}

	/**
	 * Notification add request is complete.
	 *
	 * @param elements elements to add
	 */
	protected void added(Object[] elements) {
		List<Object> kids = null;
		boolean changed = false;
		synchronized (this) {
			ModelNode[] childrenNodes = getRootNode().getChildrenNodes();
			if (childrenNodes == null) {
				kids = new ArrayList<>(elements.length);
			} else {
				kids = new ArrayList<>(elements.length + childrenNodes.length);
				for (ModelNode childNode : childrenNodes) {
					kids.add(childNode.getElement());
				}
			}
			for (Object element : elements) {
				if (!kids.contains(element)) {
					kids.add(element);
					changed = true;
				}
			}
		}
		if (changed) {
			setChildren(getRootNode(), kids);
		}
	}

	/**
	 * Inserts the given elements to the table.
	 *
	 * @param elements the new elements to insert
	 * @param index the index to insert the elements at
	 */
	public void insert(Object[] elements, int index) {
		TableAddRequestMonitor update = new TableInsertRequestMonitor(getRootNode(), elements, index, this);
		requestScheduled(update);
		update.done();
	}

	/**
	 * Notification insert request is complete.
	 *
	 * @param elements elements to add
	 * @param index index to insert at
	 */
	protected void inserted(Object[] elements, int index) {
		List<Object> kids = null;
		boolean changed = false;
		synchronized (this) {
			ModelNode[] childrenNodes = getRootNode().getChildrenNodes();
			if (childrenNodes == null) {
				kids = new ArrayList<>(elements.length);
			} else {
				kids = new ArrayList<>(elements.length + childrenNodes.length);
				for (ModelNode childNode : childrenNodes) {
					kids.add(childNode.getElement());
				}
			}
			for (Object element : elements) {
				if (!kids.contains(element)) {
					kids.add(index, element);
					index++;
					changed = true;
				}
			}
		}
		if (changed) {
			setChildren(getRootNode(), kids);
		}
	}

	/**
	 * Removes the given elements from the table.
	 *
	 * @param elements the elements to remove
	 */
	public void remove(Object[] elements) {
		TableRemoveRequestMonitor update = new TableRemoveRequestMonitor(getRootNode(), elements, this);
		requestScheduled(update);
		update.done();
	}

	/**
	 * Notification remove request is complete.
	 *
	 * @param elements elements to remove
	 */
	protected void removed(Object[] elements) {
		List<Object> kids = null;
		boolean changed = false;
		synchronized (this) {
			ModelNode[] childrenNodes = getRootNode().getChildrenNodes();
			if (childrenNodes != null) {
				kids = new ArrayList<>(childrenNodes.length);
				for (ModelNode childrenNode : childrenNodes) {
					kids.add(childrenNode.getElement());
				}
				for (Object element : elements) {
					if (kids.remove(element)) {
						changed = true;
					}
				}
			}
		}
		if (changed) {
			setChildren(getRootNode(), kids);
		}
	}

	/**
	 * Adds the given elements to the table.
	 * @param element the element to replace
	 * @param replacement the element to replace the old element with
	 */
	public void replace(Object element, Object replacement) {
		TableReplaceRequestMonitor update = new TableReplaceRequestMonitor(getRootNode(), element, replacement, this);
		requestScheduled(update);
		update.done();
	}

	/**
	 * Notification add request is complete.
	 * @param element the element to be replaced
	 * @param replacement the element that replaced the old element
	 */
	protected void replaced(Object element, Object replacement) {
		Object[] filtered = filter(getRootNode().getElement(), new Object[] { replacement });
		if (filtered.length == 0) {
			remove(new Object[]{element});
			return;
		}
		List<ModelNode> list = new ArrayList<>();
		synchronized (this) {
			for (ModelNode node : getNodes(element)) {
				node.remap(replacement);
				list.add(node);
			}
		}
		if (!list.isEmpty()) {
			for (ModelNode node : list) {
				getViewer().nodeChanged(node);
			}
		}
	}
}
