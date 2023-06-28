/*******************************************************************************
 * Copyright (c) 2009, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation = bug fixing
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.launch;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.internal.ui.viewers.model.VirtualCopyToClipboardActionDelegate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Copy action for debug view.  This action is specialized from the standard
 * copy action in a couple of ways:
 * <ul>
 *   <li>If debug view is in debug mode, then the selected element in
 *   breadcrumb is translated into the tree viewer, and then copied</li>
 *   <li> If an item is selected all the item's children are copied into
 *   clipbard.</li>
 * </ul>
 *
 * @since 3.5
 */
public class LaunchViewCopyToClipboardActionDelegate extends VirtualCopyToClipboardActionDelegate {

	@Override
	protected TreeItem[] getSelectedItems(TreeModelViewer clientViewer) {
		LaunchView view = (LaunchView)getView();
		if (view.isBreadcrumbVisible()) {
			ISelection selection = getSelection();
			if (selection instanceof ITreeSelection && getViewer() != null) {
				TreePath path = TreePath.EMPTY;
				if (!selection.isEmpty()) {
					path = ((ITreeSelection)selection).getPaths()[0];
				}
				return getSelectedItemsInTreeViewer(getViewer(), path);
			}
			return new TreeItem[0];
		} else {
			// Return tree selection plus children.
			TreeItem[] selection = clientViewer.getTree().getSelection();
			Set<Widget> set = new HashSet<>();
			collectChildItems(set, selection);
			return set.toArray(new TreeItem[set.size()]);
		}
	}

	/**
	 * Calculates selected items in viewer for given tree path.
	 * @param viewer Viewer to get items from.
	 * @param path Path for desired selection.
	 * @return Selected items.  If no selected items found, returns an empty
	 * array.
	 */
	private TreeItem[] getSelectedItemsInTreeViewer(TreeModelViewer viewer, TreePath path) {
		Widget item = viewer.findItem(path);
		Set<Widget> set = new HashSet<>();
		if (item instanceof TreeItem) {
			set.add(item);
			if (((TreeItem) item).getExpanded()) {
				collectChildItems(set, ((TreeItem) item).getItems());
			}
		} else if (item instanceof Tree) {
			collectChildItems(set, ((Tree)item).getItems());
		}
		return set.toArray(new TreeItem[set.size()]);
	}

	private void collectChildItems(Set<Widget> set, TreeItem[] items) {
		if (items == null) {
			return;
		}
		for (TreeItem item : items) {
			set.add(item);
			if (item.getExpanded()) {
				collectChildItems(set, item.getItems());
			}
		}
	}
}
