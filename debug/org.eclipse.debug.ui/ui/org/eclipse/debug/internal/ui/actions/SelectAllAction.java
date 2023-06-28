/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IViewPart;

public abstract class SelectAllAction extends AbstractRemoveAllActionDelegate {

	private IViewPart fView;

	@Override
	public void init(IViewPart view) {
		fView = view;
		IDebugView debugView = getView().getAdapter(IDebugView.class);
		if (debugView != null) {
			debugView.setAction(getActionId(), getAction());
		}
		super.init(view);
	}

	protected IViewPart getView() {
		return fView;
	}

	protected abstract String getActionId();

	private void collectExpandedAndVisible(TreeItem[] items, List<TreeItem> result) {
		for (TreeItem item : items) {
			result.add(item);
			if (item.getExpanded()) {
				collectExpandedAndVisible(item.getItems(), result);
			}
		}
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action){
		if (!(getView() instanceof IDebugView)) {
			return;
		}
		Viewer viewer = ((IDebugView) getView()).getViewer();
		if (viewer instanceof TreeViewer) {
			ArrayList<TreeItem> allVisible= new ArrayList<>();
			Tree tree= ((TreeViewer) viewer).getTree();
			collectExpandedAndVisible(tree.getItems(), allVisible);
			tree.setSelection(allVisible.toArray(new TreeItem[allVisible.size()]));
			// force viewer selection change
			viewer.setSelection(viewer.getSelection());
		}
	}

}
