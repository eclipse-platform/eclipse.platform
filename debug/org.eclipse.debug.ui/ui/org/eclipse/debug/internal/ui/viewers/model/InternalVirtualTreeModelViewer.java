/*******************************************************************************
 * Copyright (c) 2009, 2018 Wind River Systems and others.
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
 *     IBM Corporation - ongoing bug fixing
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentationFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStateUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IVirtualItemListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IVirtualItemValidator;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualItem;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualItem.Index;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualTree;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;

/**
 * A tree model viewer without a UI component.
 * @since 3.5
 */
@SuppressWarnings("deprecation")
public class InternalVirtualTreeModelViewer extends Viewer
	implements IVirtualItemListener,
			   org.eclipse.debug.internal.ui.viewers.model.ITreeModelViewer,
			   IInternalTreeModelViewer
{

	/**
	 * Memento type for the visible columns for a presentation context.
	 * A memento is created for each column presentation keyed by column number
	 */
	private static final String VISIBLE_COLUMNS = "VISIBLE_COLUMNS";     //$NON-NLS-1$

	/**
	 * Memento type for whether columns are visible for a presentation context.
	 * Booleans are keyed by column presentation id
	 */
	private static final String SHOW_COLUMNS = "SHOW_COLUMNS";     //$NON-NLS-1$
	/**
	 * Memento key for the number of visible columns in a VISIBLE_COLUMNS memento
	 * or for the width of a column
	 */
	private static final String SIZE = "SIZE";   //$NON-NLS-1$
	/**
	 * Memento key prefix a visible column
	 */
	private static final String COLUMN = "COLUMN";   //$NON-NLS-1$

	/**
	 * Item's tree path cache
	 */
	private static final String TREE_PATH_KEY = "TREE_PATH_KEY"; //$NON-NLS-1$

	/**
	 * Viewer filters currently configured for viewer.
	 */
	private ViewerFilter[] fFilters = new ViewerFilter[0];

	/**
	 * The display that this virtual tree viewer is associated with. It is used
	 * for access to the UI thread.
	 */
	private Display fDisplay;

	/**
	 * The object that allows the model to identify what this view
	 * is presenting.
	 */
	private IPresentationContext fContext;

	/**
	 * Input into the viewer.
	 */
	private Object fInput;

	/**
	 * The tree of items in this viewer.
	 */
	private VirtualTree fTree;

	/**
	 * Mapping of data elements in the tree to the items that hold them.  The
	 * tree may contain the same element in several places, so the map values
	 * are lists.
	 */
	private Map<Object, List<VirtualItem>> fItemsMap = new HashMap<>();

	/**
	 * Whether to notify the content provider when an element is unmapped.
	 * Used to suppress the notification during an associate operation.
	 */
	private boolean fNotifyUnmap = true;

	/**
	 * The label provider, must be the tree model provider.
	 */
	private TreeModelLabelProvider fLabelProvider;

	/**
	 * The content provider must be a tree model provider.
	 */
	private TreeModelContentProvider fContentProvider;

	/**
	 * Flag indicating whether the viewer is currently executing an operation
	 * at the end of which the selection will be restored.
	 */
	private boolean fPreservingSelecction;

	/**
	 * Flag indicating that the selection should not be restored at the end
	 * of a preserving-selection operation.
	 */
	private boolean fRestoreSelection;

	/**
	 * Level to which the tree should automatically expand elements.
	 * <code>-1<code> indicates that all levels shoudl be expanded.
	 */
	private int fAutoExpandToLevel = 0;

	/**
	 * Current column presentation or <code>null</code>
	 */
	private IColumnPresentation fColumnPresentation = null;

	/**
	 * Map of columns presentation id to its visible columns id's (String[])
	 * When a columns presentation is not in the map, default settings are used.
	 */
	private Map<String, String[]> fVisibleColumns = new HashMap<>();

	/**
	 * Map of column presentation id to whether columns should be displayed
	 * for that presentation (the user can toggle columns on/off when a
	 * presentation is optional.
	 */
	private Map<String, Boolean> fShowColumns = new HashMap<>();

	/**
	 * Runnable for validating the virtual tree.  It is scheduled to run in the
	 * UI thread whenever a tree validation is requested.
	 */
	private Runnable fValidateRunnable;

	public InternalVirtualTreeModelViewer(Display display, int style, IPresentationContext context, IVirtualItemValidator itemValidator) {
		fDisplay = display;
		fContext = context;
		fTree = new VirtualTree(style, itemValidator);
		fTree.addItemListener(this);

		fContentProvider = new TreeModelContentProvider();
		fLabelProvider = new TreeModelLabelProvider(this);

		if ((style & SWT.POP_UP) != 0) {
			getContentProvider().setModelDeltaMask(~ITreeModelContentProvider.CONTROL_MODEL_DELTA_FLAGS);
		}
	}

	@Override
	public Object getInput() {
		return fInput;
	}

	@Override
	public Control getControl() {
		// The virtual viewer does not have an SWT control associated with it.
		// Fortunately this method is not used by the base Viewer class.
		return null;
	}

	@Override
	public Display getDisplay() {
		return fDisplay;
	}

	@Override
	public void setInput(Object input) {
		Object oldInput = fInput;
		getContentProvider().inputChanged(this, oldInput, input);
		fItemsMap.clear();
		fTree.clearAll();
		fInput = input;
		mapElement(fInput, getTree());
		getContentProvider().postInputChanged(this, oldInput  , input);
		fTree.setData(fInput);
		fTree.setSelection(EMPTY_ITEMS_ARRAY);
		inputChanged(fInput, oldInput);
		refresh();
	}

	@Override
	public void replace(Object parentElementOrTreePath, final int index, Object element) {
		VirtualItem[] selectedItems = fTree.getSelection();
		TreeSelection selection = (TreeSelection) getSelection();
		VirtualItem[] itemsToDisassociate;
		if (parentElementOrTreePath instanceof TreePath) {
			TreePath elementPath = ((TreePath) parentElementOrTreePath).createChildPath(element);
			itemsToDisassociate = findItems(elementPath);
		} else {
			itemsToDisassociate = findItems(element);
		}

		for (VirtualItem parentItem : findItems(parentElementOrTreePath)) {
			if (index < parentItem.getItemCount()) {
				VirtualItem item = parentItem.getItem(new Index(index));
				selection = adjustSelectionForReplace(selectedItems, selection, item, element, parentItem.getData());
				// disassociate any different item that represents the
				// same element under the same parent (the tree)
				for (VirtualItem itemToDisassociate : itemsToDisassociate) {
					if (itemToDisassociate != item && itemToDisassociate.getParent() == parentItem) {
						disassociate(itemToDisassociate);
						itemToDisassociate.getParent().clear(itemToDisassociate.getIndex());
					}
				}
				//Object oldData = item.getData();
				associate(element, item);
				doUpdate(item);
				for (VirtualItem childitem : item.getItems()) {
					childitem.setNeedsDataUpdate();
				}
			}
		}
		// Restore the selection if we are not already in a nested
		// preservingSelection:
		if (!fPreservingSelecction) {
			internalSetSelection(selection, false);
			// send out notification if old and new differ
			ISelection newSelection = getSelection();
			if (!newSelection.equals(selection)) {
				handleInvalidSelection(selection, newSelection);
			}
		}
		validate();
	}

	public VirtualTree getTree() {
		return fTree;
	}

	@Override
	public void insert(Object parentOrTreePath, Object element, int position) {
		if (parentOrTreePath instanceof TreePath) {
			VirtualItem parentItem = findItem((TreePath) parentOrTreePath);
			if (parentItem != null) {
				VirtualItem item = parentItem.addItem(position);
				item.setData(element);
				mapElement(element, item);
				doUpdate(item);
			}
		} else {
			// TODO: Implement insert() for element
		}
		validate();
	}

	@Override
	public void remove(final Object parentOrTreePath, final int index) {
		final List<TreePath> oldSelection = new LinkedList<>(Arrays.asList(((TreeSelection) getSelection()).getPaths()));
		preservingSelection(() -> {
			TreePath removedPath = null;
			for (VirtualItem parentItem : findItems(parentOrTreePath)) {
				if (parentItem.isDisposed()) {
					continue;
				}

				// Parent item is not expanded so just update its contents so that
				// the plus sign gets refreshed.
				if (!parentItem.getExpanded()) {
					parentItem.setNeedsCountUpdate();
					parentItem.setItemCount(-1);
					virtualLazyUpdateHasChildren(parentItem);
				}

				if (index < parentItem.getItemCount()) {
					VirtualItem item = parentItem.getItem(new VirtualItem.Index(index));

					if (item.getData() != null) {
						removedPath = getTreePathFromItem(item);
						disassociate(item);
					}
					parentItem.remove(item.getIndex());
				}
			}

			if (removedPath != null) {
				boolean removed = false;
				for (Iterator<TreePath> it = oldSelection.iterator(); it.hasNext();) {
					TreePath path = it.next();
					if (path.startsWith(removedPath, null)) {
						it.remove();
						removed = true;
					}
				}
				if (removed) {
					setSelection(new TreeSelection(oldSelection.toArray(new TreePath[oldSelection.size()])), false);
				}
			}
		});
	}

	@Override
	public void remove(Object elementOrPath) {
		if (elementOrPath.equals(getInput()) || TreePath.EMPTY.equals(elementOrPath)) {
			setInput(null);
			return;
		}

		for (VirtualItem item : findItems(elementOrPath)) {
			disassociate(item);
			item.getParent().remove(item.getIndex());
		}
	}

	private TreeSelection adjustSelectionForReplace(VirtualItem[] selectedItems, TreeSelection selection,
		VirtualItem item, Object element, Object parentElement)
	{
		if (item.getData() != null || selectedItems.length == selection.size() || parentElement == null) {
			// Don't do anything - we are not seeing an instance of bug 185673
			return selection;
		}
		for (VirtualItem selectedItem : selectedItems) {
			if (item == selectedItem) {
				// The current item was selected, but its data is null.
				// The data will be replaced by the given element, so to keep
				// it selected, we have to add it to the selection.
				TreePath[] originalPaths = selection.getPaths();
				int length = originalPaths.length;
				TreePath[] paths = new TreePath[length + 1];
				System.arraycopy(originalPaths, 0, paths, 0, length);
				// set the element temporarily so that we can call getTreePathFromItem
				item.setData(element);
				paths[length] = getTreePathFromItem(item);
				item.setData(null);
				return new TreeSelection(paths, selection.getElementComparer());
			}
		}
		// The item was not selected, return the given selection
		return selection;
	}

//    private VirtualTreeSelection adjustSelectionForReplace(VirtualTreeSelection selection, VirtualItem item,
//        Object element, Object parentElement)
//    {
//        if (selection.getItems().containsKey(item)) {
//            if (item.getData() == null) {
//                // The current item was selected, but its data is null.
//                // The data will be replaced by the given element, so to keep
//                // it selected, we have to add it to the selection.
//
//                // set the element temporarily so that we can call getTreePathFromItem
//                item.setData(element);
//                TreePath path = getTreePathFromItem(item);
//                item.setData(null);
//
//                Map map = new LinkedHashMap(selection.getItems());
//                map.put(item, path);
//                TreePath[] paths = new TreePath[selection.getPaths().length + 1];
//                int i = 0;
//                for (Iterator itr = map.values().iterator(); itr.hasNext();) {
//                    TreePath nextPath = (TreePath)itr.next();
//                    if (nextPath != null) {
//                        paths[i++] = nextPath;
//                    }
//                }
//                return new VirtualTreeSelection(map, paths);
//            } else if (!item.getData().equals(element)) {
//                // The current item was selected by the new element is
//                // different than the previous element in the item.
//                // Remove this item from selection.
//                Map map = new LinkedHashMap(selection.getItems());
//                map.remove(item);
//                TreePath[] paths = new TreePath[selection.getPaths().length - 1];
//                int i = 0;
//                for (Iterator itr = map.values().iterator(); itr.hasNext();) {
//                    TreePath nextPath = (TreePath)itr.next();
//                    if (nextPath != null) {
//                        paths[i++] = nextPath;
//                    }
//                }
//                return new VirtualTreeSelection(map, paths);
//            }
//        }
//        if (item.getData() != null || selection.getItems().size() == selection.size() || parentElement == null) {
//            // Don't do anything - we are not seeing an instance of bug 185673
//            return selection;
//        }
//        if (item.getData() == null && selection.getItems().containsKey(item)) {
//        }
//        // The item was not selected, return the given selection
//        return selection;
//    }


	@Override
	public void reveal(TreePath path, final int index) {
		VirtualItem parentItem = findItem(path);
		if (parentItem != null && parentItem.getItemCount() >= index) {
			VirtualItem revealItem = parentItem.getItem(new Index(index));
			getTree().showItem(revealItem);
			getTree().validate();
		}
		// TODO: implement reveal()
	}

	@Override
	public int findElementIndex(TreePath parentPath, Object element) {
		VirtualItem parentItem = findItem(parentPath);
		if (parentItem != null) {
			VirtualItem item = parentItem.findItem(element);
			if (item != null) {
				return item.getIndex().intValue();
			}
		}
		return -1;
	}

	@Override
	public boolean getElementChildrenRealized(TreePath parentPath) {
		VirtualItem parentItem = findItem(parentPath);
		if (parentItem != null) {
			return !parentItem.childrenNeedDataUpdate();
		}
		return true;
	}


	private ITreeModelLabelProvider getLabelProvider() {
		return fLabelProvider;
	}

	private ITreeModelContentProvider getContentProvider() {
		return fContentProvider;
	}

	public static int ALL_LEVELS = -1;

	@Override
	public void refresh() {
		refresh(fTree);
		validate();
	}

	@Override
	public void refresh(Object element) {
		for (VirtualItem item : findItems(element)) {
			refresh(item);
			validate();
		}
	}

	private void refresh(VirtualItem item) {
		getContentProvider().preserveState(getTreePathFromItem(item));

		if (!item.needsDataUpdate()) {
			if (item.getParent() != null) {
				item.setNeedsLabelUpdate();
				virtualLazyUpdateHasChildren(item);
			}

			for (VirtualItem childItem : item.getItems()) {
				childItem.setNeedsDataUpdate();
			}
		}
		refreshStruct(item);
	}

	private void refreshStruct(VirtualItem item) {
		boolean expanded = false;
		if (item.getParent() == null) {
			// root item
			virtualLazyUpdateChildCount(item);
			expanded = true;
		} else {
			if (item.getExpanded()) {
				virtualLazyUpdateData(item);
				expanded = true;
			}
		}

		VirtualItem[] items = item.getItems();
		for (int i = 0; i < items.length; i++) {
			if (expanded) {
				refreshStruct(items[i]);
			} else {
				item.clear(new VirtualItem.Index(i));
			}
		}
	}

	private void validate() {
		if (fValidateRunnable == null) {
			fValidateRunnable = () -> {
				if (!fTree.isDisposed()) {
					fValidateRunnable = null;
					fTree.validate();
				}
			};
			getDisplay().asyncExec(fValidateRunnable);
		}
	}

	@Override
	protected void inputChanged(Object input, Object oldInput) {
		resetColumns(input);
	}

	@Override
	public int getAutoExpandLevel() {
		return fAutoExpandToLevel;
	}

	@Override
	public void setAutoExpandLevel(int level) {
		fAutoExpandToLevel = level;
	}

	public VirtualItem findItem(TreePath path) {
		if (path.getSegmentCount() == 0) {
			return fTree;
		}

		List<VirtualItem> itemsList = fItemsMap.get(path.getLastSegment());
		if (itemsList != null) {
			for (VirtualItem item : itemsList) {
				if (path.equals(getTreePathFromItem(item))) {
					return item;
				}
			}
		}

		return null;
	}

	static private final VirtualItem[] EMPTY_ITEMS_ARRAY = new VirtualItem[0];

	public VirtualItem[] findItems(Object elementOrTreePath) {
		Object element = elementOrTreePath;
		if (elementOrTreePath instanceof TreePath) {
			TreePath path = (TreePath)elementOrTreePath;
			if (path.getSegmentCount() == 0) {
				return new VirtualItem[] { getTree() };
			}
			element = path.getLastSegment();
		}
		List<VirtualItem> itemsList = fItemsMap.get(element);
		if (itemsList == null) {
			return EMPTY_ITEMS_ARRAY;
		} else {
			return itemsList.toArray(new VirtualItem[itemsList.size()]);
		}
	}

	@Override
	public void setElementData(TreePath path, int numColumns, String[] labels, ImageDescriptor[] images,
		FontData[] fontDatas, RGB[] foregrounds, RGB[] backgrounds) {
		VirtualItem item = findItem(path);
		if (item != null) {
			item.setData(VirtualItem.LABEL_KEY, labels);
			item.setData(VirtualItem.IMAGE_KEY, images);
			item.setData(VirtualItem.FOREGROUND_KEY, foregrounds);
			item.setData(VirtualItem.BACKGROUND_KEY, backgrounds);
			item.setData(VirtualItem.FONT_KEY, fontDatas);
		}
	}

	@Override
	public void setChildCount(final Object elementOrTreePath, final int count) {
		preservingSelection(() -> {
			for (VirtualItem item : findItems(elementOrTreePath)) {
				for (VirtualItem element : item.getItems()) {
					if (element.getData() != null && element.getIndex().intValue() >= count) {
						disassociate(element);
					}
				}

				item.setItemCount(count);
			}
		});
		validate();
	}

	@Override
	public void setHasChildren(final Object elementOrTreePath, final boolean hasChildren) {
		preservingSelection(() -> {
			for (VirtualItem item : findItems(elementOrTreePath)) {
				if (!hasChildren) {
					for (VirtualItem element : item.getItems()) {
						if (element.getData() != null) {
							disassociate(element);
						}
					}
				}

				item.setHasItems(hasChildren);
				if (hasChildren) {
					if (!item.getExpanded()) {
						item.setItemCount(-1);
					} else {
						virtualLazyUpdateChildCount(item);
					}
				}
			}
		});
	}

	@Override
	public boolean getHasChildren(Object elementOrTreePath) {
		VirtualItem[] items = findItems(elementOrTreePath);
		if (items.length > 0) {
			return items[0].hasItems();
		}
		return false;
	}

	private void virtualLazyUpdateHasChildren(VirtualItem item) {
		TreePath treePath;
		treePath = getTreePathFromItem(item);
		item.clearNeedsCountUpdate();
		getContentProvider().updateHasChildren(treePath);
	}

	private void virtualLazyUpdateChildCount(VirtualItem item) {
		item.clearNeedsCountUpdate();
		getContentProvider().updateChildCount(getTreePathFromItem(item), item.getItemCount());
	}

	private void virtualLazyUpdateData(VirtualItem item) {
		item.clearNeedsDataUpdate();
		getContentProvider().updateElement(getTreePathFromItem(item.getParent()), item.getIndex().intValue());
	}

	private void virtualLazyUpdateLabel(VirtualItem item) {
		item.clearNeedsLabelUpdate();
		if ( !getLabelProvider().update(getTreePathFromItem(item)) ) {
			if (item.getData() instanceof String) {
				item.setData(VirtualItem.LABEL_KEY, new String[] { (String)item.getData() } );
			}
		}
	}

	private TreePath getTreePathFromItem(VirtualItem item) {
		List<Object> segments = new LinkedList<>();
		while (item.getParent() != null) {
			segments.add(0, item.getData());
			item = item.getParent();
		}
		return new TreePath(segments.toArray());
	}

	private void unmapElement(Object element, VirtualItem item) {
		if (fNotifyUnmap) {
			// TODO: should we update the filter with the "new non-identical element"?
			IContentProvider provider = getContentProvider();
			if (provider instanceof TreeModelContentProvider) {
				((TreeModelContentProvider) provider).unmapPath((TreePath) item.getData(TREE_PATH_KEY));
			}
		}

		List<VirtualItem> itemsList = fItemsMap.get(element);
		if (itemsList != null) {
			itemsList.remove(item);
			if (itemsList.isEmpty()) {
				fItemsMap.remove(element);
			}
		}
	}

	private void mapElement(Object element, VirtualItem item) {
		// Get the items set for given element, if it doesn't exist, create it.
		// When retrieving the set, also remove it from the map, it will be
		// re-inserted to make sure that the new instance of element is used
		// in case the element has changed but the elment is equal to the old
		// one.
		List<VirtualItem> itemsList = fItemsMap.remove(element);
		if (itemsList == null) {
			itemsList = new ArrayList<>(1);
		}

		if (!itemsList.contains(item)) {
			itemsList.add(item);
		}

		// Insert the set back into the map.
		fItemsMap.put(element, itemsList);

		item.setData(TREE_PATH_KEY, getTreePathFromItem(item));
	}

	@Override
	public void revealed(VirtualItem item) {
		if (item.needsDataUpdate()) {
			virtualLazyUpdateData(item);
		} else if (item.getData() != null) {
			if (item.needsLabelUpdate()) {
				virtualLazyUpdateLabel(item);
			}
			if (item.needsCountUpdate() && item.getExpanded()) {
				virtualLazyUpdateChildCount(item);
			}
		}
	}

	@Override
	public void disposed(VirtualItem item) {
		if (!fTree.isDisposed()) {
			Object data = item.getData();
			if (data != null) {
				unmapElement(data, item);
			}
		}
	}

	private void associate(Object element, VirtualItem item) {
		Object data = item.getData();
		if (data != null && data != element && data.equals(element)) {
			// elements are equal but not identical
			// -> being removed from map, but should not change filters
			try {
				fNotifyUnmap = false;
				doAssociate(element, item);
			} finally {
				fNotifyUnmap = true;
			}
		} else {
			doAssociate(element, item);
		}

	}

	private void doAssociate(Object element, VirtualItem item) {
		Object data = item.getData();
		if (data != null && data != element && data.equals(element)) {
			// workaround for PR 1FV62BT
			// assumption: elements are equal but not identical
			// -> remove from map but don't touch children
			unmapElement(data, item);
			item.setData(element);
			mapElement(element, item);
		} else {
			// recursively disassociate all
			if (data != element) {
				if (data != null) {
					unmapElement(element, item);
					disassociate(item);
				}
				item.setData(element);
			}
			// Always map the element, even if data == element,
			// since unmapAllElements() can leave the map inconsistent
			// See bug 2741 for details.
			mapElement(element, item);
		}
	}

	private void disassociate(VirtualItem item) {
		unmapElement(item.getData(), item);

		// Clear the map before we clear the data
		item.setData(null);

		// Disassociate the children
		for (VirtualItem childItem : item.getItems()) {
			if (childItem.getData() != null) {
				disassociate(childItem);
			}
		}
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		setSelection(selection, reveal, false);
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal, boolean force) {
		trySelection(selection, reveal, force);
	}

	@Override
	public boolean trySelection(ISelection selection, boolean reveal, boolean force) {
		if (!force && !overrideSelection(getSelection(), selection)) {
			return false;
		}

		if (!fPreservingSelecction) {
			internalSetSelection(selection, reveal);
			fireSelectionChanged(new SelectionChangedEvent(this, selection));
		} else {
			fRestoreSelection = false;
			internalSetSelection(selection, reveal);
		}
		return true;
	}

	private void internalSetSelection(ISelection selection, boolean reveal) {
		if (selection instanceof ITreeSelection) {
			TreePath[] paths = ((ITreeSelection) selection).getPaths();
			List<VirtualItem> newSelection = new ArrayList<>(paths.length);
			for (TreePath path : paths) {
				// Use internalExpand since item may not yet be created. See
				// 1G6B1AR.
				VirtualItem item = findItem(path);
				if (item != null) {
					newSelection.add(item);
				}
			}
			fTree.setSelection(newSelection.toArray(new VirtualItem[newSelection.size()]));

			// Although setting the selection in the control should reveal it,
			// setSelection may be a no-op if the selection is unchanged,
			// so explicitly reveal items in the selection here.
			// See bug 100565 for more details.
			if (reveal && newSelection.size() > 0) {
				// Iterate backwards so the first item in the list
				// is the one guaranteed to be visible
				for (int i = (newSelection.size() - 1); i >= 0; i--) {
					fTree.showItem(newSelection.get(i));
				}
			}
		} else {
			fTree.setSelection(EMPTY_ITEMS_ARRAY);
		}

		// Make sure that the new selection is properly revealed.
		validate();
	}

	@Override
	public void update(Object element) {
		for (VirtualItem item : findItems(element)) {
			doUpdate(item);
		}
	}

	public void doUpdate(VirtualItem item) {
		item.setNeedsLabelUpdate();
		validate();
	}

	@Override
	public ISelection getSelection() {
		if (fTree.isDisposed()) {
			return TreeSelection.EMPTY;
		}
		VirtualItem[] items = fTree.getSelection();
		ArrayList<TreePath> list = new ArrayList<>(items.length);
		Map<VirtualItem, TreePath> map = new LinkedHashMap<>(items.length * 4 / 3);
		for (VirtualItem item : items) {
			TreePath path = null;
			if (item.getData() != null) {
				path = getTreePathFromItem(item);
				list.add(path);
			}
			map.put(item, path);
		}
		return new TreeSelection(list.toArray(new TreePath[list.size()]));
	}

	private void preservingSelection(Runnable updateCode) {

		ISelection oldSelection = null;
		try {
			// preserve selection
			oldSelection = getSelection();
			fPreservingSelecction = fRestoreSelection = true;

			// perform the update
			updateCode.run();

		} finally {
			fPreservingSelecction = false;

			// restore selection
			if (fRestoreSelection) {
				internalSetSelection(oldSelection, false);
			}

			// send out notification if old and new differ
			ISelection newSelection = getSelection();
			if (!newSelection.equals(oldSelection)) {
				handleInvalidSelection(oldSelection, newSelection);
			}
		}
	}

	@Override
	public void expandToLevel(Object elementOrTreePath, int level) {
		VirtualItem[] items = findItems(elementOrTreePath);
		if (items.length > 0) {
			expandToLevel(items[0], level);
		}
		validate();
	}

	@Override
	public void setExpandedState(Object elementOrTreePath, boolean expanded) {
		for (VirtualItem item : findItems(elementOrTreePath)) {
			item.setExpanded(expanded);
		}
		validate();
	}

	@Override
	public boolean getExpandedState(Object elementOrTreePath) {
		VirtualItem[] items = findItems(elementOrTreePath);
		if (items.length > 0) {
			return items[0].getExpanded();
		}
		return false;
	}

	private void expandToLevel(VirtualItem item, int level) {
		if (level == ALL_LEVELS || level > 0) {
			if (!item.hasItems()) {
				return;
			}

			item.setExpanded(true);

			if (item.getData() == null) {
				virtualLazyUpdateData(item);
				// Cannot expand children if data is null.
				return;
			}

			if (level == ALL_LEVELS || level > 1) {
				int newLevel = (level == ALL_LEVELS ? ALL_LEVELS
						: level - 1);
				for (VirtualItem element : item.getItems()) {
					expandToLevel(element, newLevel);
				}
			}
		}
	}

	private void handleInvalidSelection(ISelection selection, ISelection newSelection) {
		IModelSelectionPolicy selectionPolicy = ViewerAdapterService.getSelectionPolicy(selection, getPresentationContext());
		if (selectionPolicy != null) {
			while (!selection.equals(newSelection)) {
				ISelection temp = newSelection;
				selection = selectionPolicy.replaceInvalidSelection(selection, newSelection);
				if (selection == null) {
					selection = TreeSelection.EMPTY;
				}
				if (!temp.equals(selection)) {
					internalSetSelection(selection, false);
					newSelection = getSelection();
				} else {
					break;
				}
			}
		}

		fireSelectionChanged(new SelectionChangedEvent(this, newSelection));
	}

	/**
	 * Returns whether the candidate selection should override the current
	 * selection.
	 *
	 * @param current Current selection in viewer
	 * @param candidate New potential selection requested by model.
	 * @return true if candidate selection should be set to viewer.
	 */
	@Override
	public boolean overrideSelection(ISelection current, ISelection candidate) {
		IModelSelectionPolicy selectionPolicy = ViewerAdapterService.getSelectionPolicy(current, getPresentationContext());
		if (selectionPolicy == null) {
			return true;
		}
		if (selectionPolicy.contains(candidate, getPresentationContext())) {
			return selectionPolicy.overrides(current, candidate, getPresentationContext());
		}
		return !selectionPolicy.isSticky(current, getPresentationContext());
	}

	@Override
	public ViewerFilter[] getFilters() {
		return fFilters;
	}

	@Override
	public void addFilter(ViewerFilter filter) {
		ViewerFilter[] newFilters = new ViewerFilter[fFilters.length + 1];
		System.arraycopy(fFilters, 0, newFilters, 0, fFilters.length);
		newFilters[fFilters.length] = filter;
		fFilters = newFilters;
	}

	@Override
	public void setFilters(ViewerFilter... filters) {
		fFilters = filters;
	}

	public void dispose() {
		if (fColumnPresentation != null) {
			fColumnPresentation.dispose();
		}

		if (fContentProvider != null) {
			fContentProvider.dispose();
			fContentProvider = null;
		}
		if (fLabelProvider != null) {
			fLabelProvider.dispose();
			fLabelProvider = null;
		}

		fTree.removeItemListener(this);
		fTree.dispose();
	}

	/**
	 * Returns this viewer's presentation context.
	 *
	 * @return presentation context
	 */
	@Override
	public IPresentationContext getPresentationContext() {
		return fContext;
	}

	/**
	 * Configures the columns for the given viewer input.
	 *
	 * @param input new viewer input
	 */
	private void resetColumns(Object input) {
		if (input != null) {
			// only change columns if the input is non-null (persist when empty)
			IColumnPresentationFactory factory = ViewerAdapterService.getColumnPresentationFactory(input);
			PresentationContext context = (PresentationContext) getPresentationContext();
			String type = null;
			if (factory != null) {
				type = factory.getColumnPresentationId(context, input);
			}
			if (type != null && factory != null) {
				if (fColumnPresentation != null) {
					if (!fColumnPresentation.getId().equals(type)) {
						// dispose old, create new
						fColumnPresentation.dispose();
						fColumnPresentation = null;
					}
				}
				if (fColumnPresentation == null) {
					fColumnPresentation = factory.createColumnPresentation(context, input);
					if (fColumnPresentation != null) {
						fColumnPresentation.init(context);
						configureColumns();
					}
				}
			} else {
				if (fColumnPresentation != null) {
					fColumnPresentation.dispose();
					fColumnPresentation = null;
					configureColumns();
				}
			}
		}
	}

	/**
	 * Configures the columns based on the current settings.
	 */
	protected void configureColumns() {
		if (fColumnPresentation != null) {
			IColumnPresentation build = null;
			if (isShowColumns(fColumnPresentation.getId())) {
				build = fColumnPresentation;
			}
			buildColumns(build);
		} else {
			// get rid of columns
			buildColumns(null);
		}
	}

	/**
	 * Toggles columns on/off for the current column presentation, if any.
	 *
	 * @param show whether to show columns if the current input supports
	 *  columns
	 */
	public void setShowColumns(boolean show) {
		if (show) {
			if (!isShowColumns()) {
				fShowColumns.remove(fColumnPresentation.getId());
			}
		} else {
			if (isShowColumns()){
				fShowColumns.put(fColumnPresentation.getId(), Boolean.FALSE);
			}
		}
		refreshColumns();
	}

	/**
	 * Refreshes the columns in the view, based on the viewer input.
	 */
	protected void refreshColumns() {
		configureColumns();
		refresh();
	}

	/**
	 * @return Returns true if columns are being displayed currently.
	 */
	public boolean isShowColumns() {
		if (fColumnPresentation != null) {
			return isShowColumns(fColumnPresentation.getId());
		}
		return false;
	}

	/**
	 * Returns whether columns can be toggled on/off for the current input.
	 *
	 * @return whether columns can be toggled on/off for the current input
	 */
	public boolean canToggleColumns() {
		return fColumnPresentation != null && fColumnPresentation.isOptional();
	}

	protected boolean isShowColumns(String columnPresentationId) {
		Boolean bool = fShowColumns.get(columnPresentationId);
		if (bool == null) {
			return true;
		}
		return bool.booleanValue();
	}

	/**
	 * Creates new columns for the given presentation.
	 *
	 * @param presentation presentation context to build columns for.
	 */
	protected void buildColumns(IColumnPresentation presentation) {
		PresentationContext presentationContext = (PresentationContext) getPresentationContext();
		if (presentation != null) {
			presentationContext.setColumns(getVisibleColumns());
		} else {
			presentationContext.setColumns(null);
		}
	}

	/**
	 * Returns identifiers of the visible columns in this viewer, or <code>null</code>
	 * if there is currently no column presentation.
	 *
	 * @return visible columns or <code>null</code>
	 */
	@Override
	public String[] getVisibleColumns() {
		if (isShowColumns()) {
			IColumnPresentation presentation = getColumnPresentation();
			if (presentation != null) {
				String[] columns = fVisibleColumns.get(presentation.getId());
				if (columns == null) {
					return presentation.getInitialColumns();
				}
				return columns;
			}
		}
		return null;
	}

	/**
	 * Sets the id's of visible columns, or <code>null</code> to set default columns.
	 * Only affects the current column presentation.
	 *
	 * @param ids visible columns
	 */
	public void setVisibleColumns(String[] ids) {
		if (ids != null && ids.length == 0) {
			ids = null;
		}
		IColumnPresentation presentation = getColumnPresentation();
		if (presentation != null) {
			fVisibleColumns.remove(presentation.getId());
			if (ids != null) {
				// put back in table if not default
				String[] columns = presentation.getInitialColumns();
				if (columns.length == ids.length) {
					for (int i = 0; i < columns.length; i++) {
						if (!ids[i].equals(columns[i])) {
							fVisibleColumns.put(presentation.getId(), ids);
							break;
						}
					}
				} else {
					fVisibleColumns.put(presentation.getId(), ids);
				}
			}
			PresentationContext presentationContext = (PresentationContext) getPresentationContext();
			presentationContext.setColumns(getVisibleColumns());
			refreshColumns();
		}
	}

	/**
	 * Returns the current column presentation for this viewer, or <code>null</code>
	 * if none.
	 *
	 * @return column presentation or <code>null</code>
	 */
	public IColumnPresentation getColumnPresentation() {
		return fColumnPresentation;
	}

	/**
	 * Save viewer state into the given memento.
	 *
	 * @param memento Memento to write state to.
	 */
	public void saveState(IMemento memento) {
		if (!fShowColumns.isEmpty()) {
			for (Entry<String, Boolean> entry : fShowColumns.entrySet()) {
				IMemento sizes = memento.createChild(SHOW_COLUMNS, entry.getKey());
				sizes.putString(SHOW_COLUMNS, entry.getValue().toString());
			}
		}
		if (!fVisibleColumns.isEmpty()) {
			for (Entry<String, String[]> entry : fVisibleColumns.entrySet()) {
				IMemento visible = memento.createChild(VISIBLE_COLUMNS, entry.getKey());
				String[] columns = entry.getValue();
				visible.putInteger(SIZE, columns.length);
				for (int i = 0; i < columns.length; i++) {
					visible.putString(COLUMN+i, columns[i]);
				}
			}
		}
		// save presentation context properties
		IPresentationContext context = getPresentationContext();
		if (context instanceof PresentationContext) {
			PresentationContext pc = (PresentationContext) context;
			pc.saveProperites(memento);

		}
	}

	/**
	 * Initializes viewer state from the memento
	 *
	 * @param memento Memento to read state from.
	 */
	public void initState(IMemento memento) {
		for (IMemento child : memento.getChildren(SHOW_COLUMNS)) {
			String id = child.getID();
			Boolean bool = Boolean.valueOf(child.getString(SHOW_COLUMNS));
			if (!bool.booleanValue()) {
				fShowColumns.put(id, bool);
			}
		}
		for (IMemento child : memento.getChildren(VISIBLE_COLUMNS)) {
			String id = child.getID();
			Integer integer = child.getInteger(SIZE);
			if (integer != null) {
				int length = integer.intValue();
				String[] columns = new String[length];
				for (int j = 0; j < length; j++) {
					columns[j] = child.getString(COLUMN+j);
				}
				fVisibleColumns.put(id, columns);
			}
		}
		// restore presentation context properties
		// save presentation context properties
		IPresentationContext context = getPresentationContext();
		if (context instanceof PresentationContext) {
			PresentationContext pc = (PresentationContext) context;
			pc.initProperties(memento);
		}
	}

	@Override
	public void addViewerUpdateListener(IViewerUpdateListener listener) {
		getContentProvider().addViewerUpdateListener(listener);
	}

	@Override
	public void removeViewerUpdateListener(IViewerUpdateListener listener) {
		ITreeModelContentProvider cp = getContentProvider();
		if (cp !=  null) {
			cp.removeViewerUpdateListener(listener);
		}
	}

	@Override
	public void addModelChangedListener(IModelChangedListener listener) {
		getContentProvider().addModelChangedListener(listener);
	}

	@Override
	public void removeModelChangedListener(IModelChangedListener listener) {
		ITreeModelContentProvider cp = getContentProvider();
		if (cp !=  null) {
			cp.removeModelChangedListener(listener);
		}
	}

	@Override
	public void addStateUpdateListener(IStateUpdateListener listener) {
		getContentProvider().addStateUpdateListener(listener);
	}

	@Override
	public void removeStateUpdateListener(IStateUpdateListener listener) {
		ITreeModelContentProvider cp = getContentProvider();
		if (cp !=  null) {
			cp.removeStateUpdateListener(listener);
		}
	}

	@Override
	public void addLabelUpdateListener(ILabelUpdateListener listener) {
		getLabelProvider().addLabelUpdateListener(listener);
	}

	@Override
	public void removeLabelUpdateListener(ILabelUpdateListener listener) {
		getLabelProvider().removeLabelUpdateListener(listener);
	}

	/**
	 * Performs auto expand on an element at the specified path if the auto expand
	 * level dictates the element should be expanded.
	 *
	 * @param elementPath tree path to element to consider for expansion
	 */
	@Override
	public void autoExpand(TreePath elementPath) {
		int level = getAutoExpandLevel();
		if (level > 0 || level == org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer.ALL_LEVELS) {
			if (level == org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer.ALL_LEVELS || level > elementPath.getSegmentCount()) {
				expandToLevel(elementPath, 1);
			}
		}
	}

	@Override
	public int getChildCount(TreePath path) {
		int childCount = -1;
		VirtualItem[] items = findItems(path);
		if (items.length > 0) {
			childCount = items[0].getItemCount();
			// Mimic the jface viewer behavior which returns 1 for child count
			// for an item that has children but is not yet expanded.
			// Return 0, if we do not know if the item has children.
			if (childCount == -1) {
				childCount = items[0].hasItems() ? 1 : 0;
			}
		}
		return childCount;
	}

	@Override
	public Object getChildElement(TreePath path, int index) {
		VirtualItem[] items = findItems(path);
		if (items.length > 0) {
			if (index < items[0].getItemCount()) {
				return items[0].getItem(new VirtualItem.Index(index)).getData();
			}
		}
		return null;
	}

	@Override
	public TreePath getTopElementPath() {
		return null;
	}

	@Override
	public boolean saveElementState(TreePath path, ModelDelta delta, int flagsToSave) {
		VirtualTree tree = getTree();
		VirtualItem[] selection = tree.getSelection();
		Set<VirtualItem> set = new HashSet<>();
		Collections.addAll(set, selection);

		VirtualItem parent = findItem(path);

		if (parent != null) {
			delta.setChildCount(((TreeModelContentProvider)getContentProvider()).viewToModelCount(path, parent.getItemCount()));
			if (parent.getExpanded()) {
				if ((flagsToSave & IModelDelta.EXPAND) != 0) {
					delta.setFlags(delta.getFlags() | IModelDelta.EXPAND);
				}
			} else if ((flagsToSave & IModelDelta.COLLAPSE) != 0 && parent.hasItems()){
				delta.setFlags(delta.getFlags() | IModelDelta.COLLAPSE);
			}

			if (set.contains(parent) && (flagsToSave & IModelDelta.SELECT) != 0) {
				delta.setFlags(delta.getFlags() | IModelDelta.SELECT);
			}

			for (VirtualItem item : parent.getItems()) {
				doSaveElementState(path, delta, item, set, flagsToSave);
			}
			return true;
		} else {
			return false;
		}
	}

	private void doSaveElementState(TreePath parentPath, ModelDelta delta, VirtualItem item, Collection<VirtualItem> set, int flagsToSave) {
		Object element = item.getData();
		if (element != null) {
			boolean expanded = item.getExpanded();
			boolean selected = set.contains(item);
			int flags = IModelDelta.NO_CHANGE;
			if (expanded && (flagsToSave & IModelDelta.EXPAND) != 0) {
				flags = flags | IModelDelta.EXPAND;
			}
			if (!expanded && (flagsToSave & IModelDelta.COLLAPSE) != 0 && item.hasItems()){
				flags = flags | IModelDelta.COLLAPSE;
			}
			if (selected && (flagsToSave & IModelDelta.SELECT) != 0) {
				flags = flags | IModelDelta.SELECT;
			}
			if (expanded || flags != IModelDelta.NO_CHANGE) {
				int modelIndex = ((TreeModelContentProvider)getContentProvider()).viewToModelIndex(parentPath, item.getIndex().intValue());
				TreePath elementPath = parentPath.createChildPath(element);
				int numChildren = ((TreeModelContentProvider)getContentProvider()).viewToModelCount(elementPath, item.getItemCount());
				ModelDelta childDelta = delta.addNode(element, modelIndex, flags, numChildren);
				if (expanded) {
					for (VirtualItem childItem : item.getItems()) {
						doSaveElementState(elementPath, childDelta, childItem, set, flagsToSave);
					}
				}
			}
		}
	}

	@Override
	public void updateViewer(IModelDelta delta) {
		getContentProvider().updateModel(delta, ITreeModelContentProvider.ALL_MODEL_DELTA_FLAGS);
	}

	@Override
	public ViewerLabel getElementLabel(TreePath path, String columnId) {
		if (path.getSegmentCount() == 0) {
			return null;
		}

		int columnIdx = -1;
		String[] visibleColumns = getVisibleColumns();
		if (columnId != null && visibleColumns != null) {
			int i = 0;
			for (i = 0; i < visibleColumns.length; i++) {
				if (columnId.equals(getVisibleColumns()[i])) {
					columnIdx = i;
					break;
				}
			}
			if (i == visibleColumns.length) {
				return null;
			}
		} else {
			columnIdx = 0;
		}
		VirtualItem item = findItem(path);

		if (item != null) {
			ViewerLabel label = new ViewerLabel(getText(item, columnIdx), getImage(item, columnIdx));
			label.setFont(getFont(item, columnIdx));
			label.setBackground(getBackground(item, columnIdx));
			label.setForeground(getForeground(item, columnIdx));
			return label;
		}
		return null;
	}

	@Override
	public TreePath[] getElementPaths(Object element) {
		VirtualItem[] items = findItems(element);
		TreePath[] paths = new TreePath[items.length];
		for (int i = 0; i < items.length; i++) {
			paths[i] = getTreePathFromItem(items[i]);
		}
		return paths;
	}


	public String getText(VirtualItem item, int columnIdx) {
		String[] texts = (String[])item.getData(VirtualItem.LABEL_KEY);
		if (texts != null && texts.length > columnIdx) {
			return texts[columnIdx];
		}
		return null;
	}

	public Image getImage(VirtualItem item, int columnIdx) {
		ImageDescriptor[] imageDescriptors = (ImageDescriptor[]) item.getData(VirtualItem.IMAGE_KEY);
		if (imageDescriptors != null && imageDescriptors.length > columnIdx) {
			return getLabelProvider().getImage(imageDescriptors[columnIdx]);
		}
		return null;
	}

	public Font getFont(VirtualItem item, int columnIdx) {
		FontData[] fontDatas = (FontData[]) item.getData(VirtualItem.FONT_KEY);
		if (fontDatas != null) {
			return getLabelProvider().getFont(fontDatas[columnIdx]);
		}
		return null;
	}

	public Color getForeground(VirtualItem item, int columnIdx) {
		RGB[] rgbs = (RGB[]) item.getData(VirtualItem.FOREGROUND_KEY);
		if (rgbs != null) {
			return getLabelProvider().getColor(rgbs[columnIdx]);
		}
		return null;
	}

	public Color getBackground(VirtualItem item, int columnIdx) {
		RGB[] rgbs = (RGB[]) item.getData(VirtualItem.BACKGROUND_KEY);
		if (rgbs != null) {
			return getLabelProvider().getColor(rgbs[columnIdx]);
		}
		return null;
	}

	@Override
	public void clearSelectionQuiet() {
		getTree().setSelection(EMPTY_ITEMS_ARRAY);
	}

	@Override
	public boolean getElementChecked(TreePath path) {
		// Not supported
		return false;
	}

	@Override
	public boolean getElementGrayed(TreePath path) {
		// Not supported
		return false;
	}

	@Override
	public void setElementChecked(TreePath path, boolean checked, boolean grayed) {
		// Not supported
	}

	@Override
	public String toString() {
		return getTree().toString();
	}
}
