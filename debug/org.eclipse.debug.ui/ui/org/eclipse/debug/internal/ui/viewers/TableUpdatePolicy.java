/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * Default update policy updates a viewer based on model deltas.
 *
 * @since 3.2
 */
public class TableUpdatePolicy extends org.eclipse.debug.internal.ui.viewers.AbstractUpdatePolicy implements IModelChangedListener {

	@Override
	public void modelChanged(IModelDelta delta, IModelProxy proxy) {
		updateNodes(new IModelDelta[] {delta});
	}

	private void handleState(IModelDelta node) {
		AsynchronousViewer viewer = getViewer();
		if (viewer != null) {
			Object element = node.getElement();
			viewer.update(element);
			updateSelection(element, node.getFlags());
		}
	}
	private void handleContent(IModelDelta node) {
		AsynchronousViewer viewer = getViewer();
		if (viewer != null) {
			Object element = node.getElement();
			viewer.refresh(element);
			updateSelection(element, node.getFlags());
		}
	}

	private void updateSelection(Object element, int flags) {
		AsynchronousViewer viewer = getViewer();
		if (viewer != null) {
			if ((flags & IModelDelta.SELECT) != 0) {
				getViewer().setSelection(new StructuredSelection(element));
			}
		}
	}

	protected void updateNodes(IModelDelta[] nodes) {
		for (IModelDelta node : nodes) {
			int flags = node.getFlags();

			if ((flags & IModelDelta.STATE) != 0) {
				handleState(node);
			}
			if ((flags & IModelDelta.CONTENT) != 0) {
				handleContent(node);
			}
			if ((flags & IModelDelta.ADDED) != 0) {
				handleAdd(node);
			}
			if ((flags & IModelDelta.REMOVED) != 0) {
				handleRemove(node);
			}
			if ((flags & IModelDelta.REPLACED) != 0) {
				handleReplace(node);
			}
			if ((flags & IModelDelta.INSERTED) != 0) {
				handleInsert(node);
			}

			IModelDelta[] childNodes = node.getChildDeltas();
			updateNodes(childNodes);
		}
	}

	private void handleInsert(IModelDelta node) {
		AsynchronousTableViewer viewer = (AsynchronousTableViewer) getViewer();
		if (viewer != null) {
			viewer.insert(node.getElement(), node.getIndex());
			updateSelection(node.getElement(), node.getFlags());
		}
	}

	private void handleReplace(IModelDelta node) {
		AsynchronousTableViewer viewer = (AsynchronousTableViewer) getViewer();
		if (viewer != null) {
			viewer.replace(node.getElement(), node.getReplacementElement());
			updateSelection(node.getReplacementElement(), node.getFlags());
		}
	}

	protected void handleAdd(IModelDelta node) {
		((AsynchronousTableViewer) getViewer()).add(node.getElement());
		updateSelection(node.getElement(), node.getFlags());
	}

	protected void handleRemove(IModelDelta node) {
		((AsynchronousTableViewer) getViewer()).remove(node.getElement());
	}
}
