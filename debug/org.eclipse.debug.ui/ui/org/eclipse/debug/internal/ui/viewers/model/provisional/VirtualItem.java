/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems and others.
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
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;

/**
 * Virtual item, which is analogous to the SWT's tree item.  This class is used
 * by the {@link VirtualTreeModelViewer}.
 *
 * @see VirtualTreeModelViewer
 * @since 3.8
 */
public class VirtualItem {

	// Data keys for display attributes of an item.
	public static String LABEL_KEY = "LABEL_KEY"; //$NON-NLS-1$
	public static String IMAGE_KEY = "IMAGE_KEY"; //$NON-NLS-1$
	public static String FONT_KEY = "FONT_KEY"; //$NON-NLS-1$
	public static String FOREGROUND_KEY = "FOREGROUND_KEY"; //$NON-NLS-1$
	public static String BACKGROUND_KEY = "BACKGROUND_KEY"; //$NON-NLS-1$

	public static String ELEMENT_DATA_KEY = "element"; //$NON-NLS-1$

	/**
	 * Index object of a tree item. It allows the indexes to be modified
	 * as items are inserted and removed.
	 */
	public static class Index implements Comparable<Object> {
		private Integer fIndexValue;

		public Index(int index) {
			fIndexValue = Integer.valueOf(index);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Index && ((Index)obj).fIndexValue.equals(fIndexValue);
		}

		@Override
		public int hashCode() {
			return fIndexValue.hashCode();
		}

		public void increment() {
			fIndexValue = Integer.valueOf(fIndexValue.intValue() + 1);
		}

		public void decrement() {
			fIndexValue = Integer.valueOf(fIndexValue.intValue() - 1);
		}

		public int intValue() {
			return fIndexValue.intValue();
		}

		@Override
		public int compareTo(Object obj) {
			return obj instanceof Index ? fIndexValue.compareTo(((Index)obj).fIndexValue) : 0;
		}

		@Override
		public String toString() {
			return fIndexValue.toString();
		}
	}

	/**
	 * Parent items of this item.
	 */
	final private VirtualItem fParent;

	/**
	 * The index of this item.
	 */
	final private Index fIndex;

	/**
	 * Map of child items.  The key to the map is the item's index, which
	 * must be the same object instance as the index in the item.  The tree map
	 * keeps the items sorted while allowing indexes (keys) to be modified as
	 * child items are inserted and removed.
	 */
	private Map<Index, VirtualItem> fItems = new TreeMap<>();

	/**
	 * Flag indicating whether this item has child items.
	 */
	private boolean fHasItems = false;

	/**
	 * Indicates that this item has been expanded.  It should only
	 * be set to <code>true</code> if fHasItems is <code>true</code>.
	 */
	private boolean fExpanded = false;

	/**
	 * The count of child items.  <code>-1</code> indicates that the count
	 * is not known.
	 */
	private int fItemCount = -1;

	/**
	 * The data held by this item.  It includes the element as well as the item
	 * display attributes.
	 */
	private Map<String, Object> fData = new HashMap<>(1);

	/**
	 * Flag indicating that the item needs to have it's label updated.
	 */
	private boolean fNeedsLabelUpdate = true;

	/**
	 * Flag indicating that the item's count needs to be updated.
	 */
	private boolean fNeedsCountUpdate = true;

	/**
	 * Flag indicating that the item's element needs to be updated.
	 */
	private boolean fNeedsDataUpdate = true;

	/**
	 * Indicates that this item has been disposed.
	 */
	private boolean fDisposed = false;


	/**
	 * Virtual item constructor.
	 * @param parent parent virtual item
	 * @param index index of the item in the parent
	 */
	public VirtualItem(VirtualItem parent, Index index) {
		fParent = parent;
		fIndex = index;
	}

	/**
	 * Clears the child item at the given index.
	 * @param index index of item to clear.
	 */
	public void clear(Index index) {
		VirtualItem item = fItems.remove(index);
		if (item != null) {
			item.dispose();
		}
	}

	/**
	 * Clears all child items.
	 *
	 * @since 3.9
	 */
	public void clearAll() {
		fData.clear();
		for (VirtualItem item : fItems.values()) {
			item.dispose();
		}
		fItems.clear();
	}
	/**
	 * Returns the parent item.
	 * @return parent item.
	 */
	public VirtualItem getParent() {
		return fParent;
	}

	/**
	 * @return Returns the index of this item.
	 */
	public Index getIndex() {
		return fIndex;
	}

	/**
	 * Finds the given item in the child items of this element.
	 * @param element Data object of the item to be found.
	 * @return Item if found, <code>null</code> if not.
	 */
	public VirtualItem findItem(Object element) {
		for (VirtualItem item : fItems.values()) {
			Object nextData = item.getData();
			if ( (element != null && element.equals(nextData)) || (element == null && nextData == null) ) {
				return item;
			}
		}
		return null;
	}

	/**
	 * @return Returns whether the data element of this item is stale.
	 */
	public boolean needsDataUpdate() {
		return fNeedsDataUpdate;
	}

	/**
	 * Marks the item as having a stale data item.
	 */
	public void setNeedsDataUpdate() {
		fNeedsDataUpdate = true;
	}

	/**
	 * Clears the stale status of the item's data element.
	 */
	public void clearNeedsDataUpdate() {
		fNeedsDataUpdate = false;
	}

	/**
	 * @return Returns whether the item has stale item count.
	 */
	public boolean needsCountUpdate() {
		return fNeedsCountUpdate;
	}

	/**
	 * Marks the item as having a stale child count.
	 */
	public void setNeedsCountUpdate() {
		fNeedsCountUpdate = true;
		fItemCount = -1;
	}

	/**
	 * Clears the stale status of the item's child count.
	 */
	public void clearNeedsCountUpdate() {
		fNeedsCountUpdate = false;
	}

	/**
	 * @return Returns whether the item has stale label.
	 */
	public boolean needsLabelUpdate() {
		return fNeedsLabelUpdate;
	}

	/**
	 * Marks the item as having a stale label data.
	 */
	public void setNeedsLabelUpdate() {
		fNeedsLabelUpdate = true;
	}

	/**
	 * Clears the stale status of the item's label.
	 */
	public void clearNeedsLabelUpdate() {
		fNeedsLabelUpdate = false;
	}

	/**
	 * @return Returns whether the item has been disposed.
	 */
	public boolean isDisposed() {
		return fDisposed;
	}

	/**
	 * Disposes the item.
	 */
	public void dispose() {
		clearAll();

		fDisposed = true;
		findTree().fireItemDisposed(this);
	}

	/**
	 * @param key Key to retrieve data for.
	 * @return Returns item data corresponding to given key.
	 */
	public Object getData (String key) {
		return fData.get(key);
	}

	/**
	 * Sets given data element for given key.
	 * @param key Key for data.
	 * @param data Data value.
	 */
	public void setData(String key, Object data) {
		fData.put(key, data);
	}

	/**
	 * Sets the item's data element.
	 * @param data Item's new element.
	 */
	public void setData(Object data) {
		fData.put(ELEMENT_DATA_KEY, data);
	}

	/**
	 * @return Returns item's data element.
	 */
	public Object getData () {
		return fData.get(ELEMENT_DATA_KEY);
	}

	/**
	 * Marks the given item as expanded or collapsed.
	 * @param expanded If true, item will be marked as expanded.
	 */
	public void setExpanded(boolean expanded) {
		if (fExpanded == expanded) {
			return;
		}
		fExpanded = expanded;

		if (fExpanded && getItemCount() == -1) {
			setNeedsCountUpdate();
		}


		Assert.isTrue(!fExpanded || hasItems());

		// If collapsed, make sure that all the children are collapsed as well.
		if (!fExpanded) {
			for (VirtualItem item : fItems.values()) {
				item.setExpanded(expanded);
			}
		}
	}

	/**
	 * @return Returns item's expanded state.
	 */
	public boolean getExpanded() {
		return fExpanded;
	}

	/**
	 * Sets the flag indicating whether item has child items.
	 * @param hasChildren Set to true if child has items.
	 */
	public void setHasItems(boolean hasChildren) {
		fHasItems = hasChildren;
		if (!fHasItems) {
			if (getItemCount() != 0) {
				setItemCount(0);
			}
		} else if (getItemCount() == 0) {
			setItemCount(-1);
		}
	}

	/**
	 * @return Returns true if item has child items.
	 */
	public boolean hasItems() {
		return fHasItems;
	}

	/**
	 * Sets the item's child count.
	 * @param count Child count.
	 */
	public void setItemCount(int count) {
		fItemCount = count;
		for (Iterator<Entry<Index, VirtualItem>> itr = fItems.entrySet().iterator(); itr.hasNext();) {
			Entry<Index, VirtualItem> entry = itr.next();
			int index = entry.getKey().intValue();
			if (index >= count) {
				VirtualItem item = entry.getValue();
				item.dispose();
				itr.remove();
			}
		}
		if (fItemCount == 0) {
			if (hasItems()) {
				setHasItems(false);
			}
			if (getExpanded()) {
				setExpanded(false);
			}
		} else {
			if (!hasItems()) {
				setHasItems(true);
			}
		}
	}

	/**
	 * @return  Returns item's child count.
	 */
	public int getItemCount() {
		return fItemCount;
	}

	/**
	 * Returns the child item at given index.  Child item is created if needed.
	 *
	 * @param index Index of the child item.
	 * @return Child item.
	 */
	public VirtualItem getItem(Index index) {
		ensureItems();

		VirtualItem item = fItems.get(index);
		if (item == null) {
			item = new VirtualItem(this, index);
			fItems.put(index, item);
		}
		return item;
	}

	/**
	 * @return Returns true if any of the child items need a data update.
	 */
	public boolean childrenNeedDataUpdate() {
		if (getItemCount() == 0) {
			return false;
		}
		if (fItems == null || fItems.size() != fItemCount) {
			return true;
		}
		for (VirtualItem child : fItems.values()) {
			if (child.needsDataUpdate()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns an array of current child items.  The returned array contains
	 * only the items that have been created.  It may not contain as many items as the
	 * item count.
	 *
	 * @return Child items array.
	 */
	public VirtualItem[] getItems() {
		return fItems.values().toArray(new VirtualItem[fItems.size()]);
	}

	/**
	 * Adds a child item at the given index position.
	 * @param position The index position to inser the new item at.
	 * @return Returns the added item.
	 */
	public VirtualItem addItem(int position) {
		if (!fHasItems) {
			fHasItems = true;
		}
		if (fItemCount < 0) {
			fItemCount = 0;
		}

		// Increment all items with an index higher than the given position.
		fItemCount++;
		ensureItems();
		for (Index childIndex : fItems.keySet()) {
			if (childIndex.intValue() >= position) {
				childIndex.increment();
			}
		}

		// Note: the same index object used to create the item has to
		// be used as the key into the map.
		Index childIndex = new Index(position);
		VirtualItem newChild = new VirtualItem(this, childIndex);
		fItems.put(childIndex, newChild);
		return newChild;
	}

	/**
	 * Removes the item at the given index.
	 * @param position Index of the item to remove.
	 */
	public void remove(Index position) {
		fItemCount--;
		if (fItemCount < 0) {
			fHasItems = false;
		}

		ensureItems();

		VirtualItem removedItem = null;
		for (Iterator<Entry<Index, VirtualItem>> itr = fItems.entrySet().iterator(); itr.hasNext();) {
			Entry<Index, VirtualItem> entry = itr.next();
			Index childIndex = entry.getKey();
			if (childIndex.intValue() > position.intValue()) {
				childIndex.decrement();
			} else if (childIndex.intValue() == position.intValue()) {
				removedItem = entry.getValue();
				removedItem.dispose();
				itr.remove();
			}
		}
	}

	private void ensureItems() {
		if (fItems == null) {
			fItems = new HashMap<>(Math.max(1, Math.min(fItemCount, 16)));
		}
	}

	private VirtualTree findTree() {
		VirtualItem item = this;
		while (!(item instanceof VirtualTree)) {
			item = item.fParent;
		}
		return (VirtualTree)item;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		toStringItem(buffer, IInternalDebugCoreConstants.EMPTY_STRING);
		return buffer.toString();
	}

	void toStringItem(StringBuilder buffer, String indent) {
		buffer.append(indent);
		buffer.append(toStringElement());
		buffer.append("\n"); //$NON-NLS-1$
		indent = indent + "  "; //$NON-NLS-1$
		for (int i = 0; i < fItemCount; i++) {
			VirtualItem item = fItems.get(new Index(i));
			if (item != null) {
				item.toStringItem(buffer, indent);
			} else {
				buffer.append("<no item>\n"); //$NON-NLS-1$
			}
		}
	}

	private String toStringElement() {
		String[] label = (String[])fData.get(LABEL_KEY);
		if (label != null && label.length != 0) {
			return label[0];
		}
		Object data = fData.get(ELEMENT_DATA_KEY);
		if (data != null) {
			return data.toString();
		}
		return "<no data>"; //$NON-NLS-1$
	}
}
