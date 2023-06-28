/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems and others.
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
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IVirtualItemValidator;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualItem;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualTree;

/**
 * Item validator for the virtual viewer which specifies that the given
 * range of items should be treated as visible.
 */
public class VisibleVirtualItemValidator implements IVirtualItemValidator {

	private int fStart = 0;
	private int fEnd = 0;

	public VisibleVirtualItemValidator(int startPosition, int length) {
		setVisibleRange(startPosition, length);
	}

	public void setVisibleRange(int startPosition, int length) {
		fStart = startPosition;
		fEnd = startPosition + length;
	}

	public int getStartPosition() {
		return fStart;
	}

	public int getLength() {
		return fEnd - fStart;
	}

	@Override
	public boolean isItemVisible(VirtualItem item) {
		int position = 0;
		while (item.getParent() != null) {
			position += item.getIndex().intValue();
			item = item.getParent();
		}
		return position >= fStart && position < fEnd || isSelected(item);
	}

	@Override
	public void showItem(VirtualItem item) {
		int length = fEnd - fStart;
		fStart = calcPosition(item);
		fEnd = fStart + length;
	}

	private int calcPosition(VirtualItem item) {
		int position = 0;
		while (item.getParent() != null) {
			position += item.getIndex().intValue();
			item = item.getParent();
		}
		return position;
	}

	private boolean isSelected(VirtualItem item) {
		VirtualItem[] selection = getSelection(item);
		for (int i = 0; i < selection.length; i++) {
			VirtualItem selectionItem = selection[i];
			while (selectionItem != null) {
				if (item.equals(selectionItem)) {
					return true;
				}
				selectionItem = selectionItem.getParent();
			}
		}
		return false;
	}

	private VirtualItem[] getSelection(VirtualItem item) {
		VirtualTree tree = getTree(item);
		if (tree != null) {
			return tree.getSelection();
		}
		return new VirtualItem[0];
	}

	private VirtualTree getTree(VirtualItem item) {
		while (item != null && !(item instanceof VirtualTree)) {
			item = item.getParent();
		}
		return (VirtualTree)item;
	}
}
