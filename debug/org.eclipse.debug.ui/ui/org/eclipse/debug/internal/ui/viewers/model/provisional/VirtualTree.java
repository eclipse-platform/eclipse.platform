/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems and others.
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
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;


/**
 * Tree of virtual items that is analogous to SWT's tree control.  The tree is used
 * by the {@link VirtualTreeModelViewer}.
 *
 * @see VirtualTreeModelViewer
 * @since 3.8
 */
public class VirtualTree extends VirtualItem {

	/**
	 * Lazy virtual tree does not retrieve elements or labels,
	 * except for the selected elements.
	 */
	private boolean fLazy;

	private IVirtualItemValidator fValidator;

	private class SelectedItemValidator implements IVirtualItemValidator {
		@Override
		public boolean isItemVisible(VirtualItem item) {
			// visible items.  For now only mark the selected items as visible.
			for (VirtualItem selectionItem : fSelection) {
				while (selectionItem != null) {
					if (item.equals(selectionItem)) {
						return true;
					}
					selectionItem = selectionItem.getParent();
				}
			}
			return false;
		}

		@Override
		public void showItem(VirtualItem item) {
		}
	}

	/**
	 * Set of listeners of the virtual tree.
	 */
	private Set<IVirtualItemListener> fVirtualItemListeners = new HashSet<>(1);

	/**
	 * The currently selected items.  This array contains only
	 * the leaf items which are selected.
	 */
	private VirtualItem[] fSelection = new VirtualItem[0];

	/**
	 * Constructs the virtual tree with the given style and validator.
	 *
	 * @param style The style flag.  Only SWT.VIRTUAL flag is used.
	 * @param validator Item validator used to determine item visibility.
	 */
	public VirtualTree(int style, IVirtualItemValidator validator) {
		super(null, new VirtualItem.Index(0));
		fLazy = (style & SWT.VIRTUAL) != 0;
		if (fLazy && validator == null) {
			fValidator = new SelectedItemValidator();
		} else {
			fValidator = validator;
		}
		clearNeedsLabelUpdate();
		clearNeedsDataUpdate();
	}

	/**
	 * Disposes the virtual tree.
	 */
	@Override
	public void dispose() {
		super.dispose();
		fVirtualItemListeners.clear();
	}

	@Override
	public void setNeedsCountUpdate() {
		super.setNeedsCountUpdate();
		clearNeedsLabelUpdate();
		clearNeedsDataUpdate();
	}

	@Override
	public void setNeedsLabelUpdate() {
		// no-op
	}

	@Override
	public void setData(String key, Object data) {
		super.setData(key, data);
		if (data == null) {
			clearNeedsDataUpdate();
		}
	}

	/**
	 * Adds a listener for when virtual items are revealed in the view.
	 * @param listener Listener to add to list of listeners.
	 */
	public void addItemListener(IVirtualItemListener listener) {
		fVirtualItemListeners.add(listener);
	}

	public void removeItemListener(IVirtualItemListener listener) {
		fVirtualItemListeners.remove(listener);
	}

	public VirtualItem[] getSelection() {
		return fSelection;
	}

	public void setSelection(VirtualItem[] items) {
		fSelection = items;
	}

	public void showItem(VirtualItem item) {
		if (fValidator != null) {
			fValidator.showItem(item);
		}
	}

	public void fireItemDisposed(VirtualItem item) {
		for (IVirtualItemListener listener : fVirtualItemListeners) {
			listener.disposed(item);
		}
	}

	public void fireItemRevealed(VirtualItem item) {
		for (IVirtualItemListener listener : fVirtualItemListeners) {
			listener.revealed(item);
		}
	}

	@Override
	public void setData(Object data) {
		super.setData(data);
		// The root item always has children as long as the input is non-null,
		// so that it should be expanded.
		setHasItems(data != null);
	}

	@Override
	public void setHasItems(boolean hasChildren) {
		super.setHasItems(hasChildren);
		// The root item is always expanded as long as it has children.
		if (hasChildren) {
			setExpanded(true);
		}
	}

	/**
	 * Returns whether the given item is considered visible by the tree as
	 * determined by its virtual item validator.
	 *
	 * @param item Item to check.
	 * @return true if items is vislble.
	 * @see IVirtualItemValidator
	 */
	public boolean isItemVisible(VirtualItem item) {
		if (fLazy) {
			return fValidator.isItemVisible(item);
		}
		return true;
	}

	/**
	 * Validates the entire tree.
	 */
	public void validate() {
		validate(VirtualTree.this);
	}

	/**
	 * Validates the item and its children, identifying children which were
	 * revealed and need to be updated.
	 *
	 * @param item The item which to validate.
	 */
	public void validate(VirtualItem item) {
		if (item.needsDataUpdate()) {
			if (isItemVisible(item)) {
				fireItemRevealed(item);
			}
		} else if (item.getData() != null) {
			if ( item.needsLabelUpdate() || (item.needsCountUpdate() && item.hasItems() && item.getExpanded()) ) {
				if (isItemVisible(item)) {
					fireItemRevealed(item);
				}
			}

			if (item.getData() != null && item.getItemCount() > 0 && item.getExpanded()) {
				for (int i = 0; i < item.getItemCount(); i++) {
					validate(item.getItem(new Index(i)));
				}
			}
		}
	}
}
