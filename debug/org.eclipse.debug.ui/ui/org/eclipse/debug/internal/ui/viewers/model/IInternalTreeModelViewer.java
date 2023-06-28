/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems and others.
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
 *     IBM Corporation - ongoing bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model;

import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

/**
 * This interface must be implemented by the viewer which uses the
 * {@link TreeModelContentProvider} content provider.  It allows the content
 * provider to update the viewer with information retrieved from the
 * content, proxy, memento, and other element-based providers.
 *
 * @since 3.8
 */
public interface IInternalTreeModelViewer extends ITreeModelViewer {

	/**
	 * Returns this viewer's filters.
	 *
	 * @return an array of viewer filters
	 * @see org.eclipse.jface.viewers.StructuredViewer#setFilters(ViewerFilter[])
	 */
	@Override ViewerFilter[] getFilters();

	/**
	 * Reveals the given element in the viewer.
	 * @param path Path to the element's parent.
	 * @param index Index of the element to be revealed.
	 */
	void reveal(TreePath path, int index);

	/**
	 * Triggers an update of the given element's state.  If multiple instances
	 * of the given element are found in the tree, they will all be updated.
	 *
	 * @param element Element to update.
	 */
	void update(Object element);

	/**
	 * Sets the given object to be the element at the given index of the given parent.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 *
	 * @param parentOrTreePath Parent object, or a tree path of the parent element.
	 * @param index Index at which to set the new element.
	 * @param element Element object.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void replace(Object parentOrTreePath, final int index, Object element);

	/**
	 * Set the number of children of the given element or tree path. To set the
	 * number of children of the invisible root of the tree, you can pass the
	 * input object or an empty tree path.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 *
	 * @param elementOrTreePath The element, or tree path.
	 * @param count new value
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void setChildCount(final Object elementOrTreePath, final int count);

	/**
	 * Inform the viewer about whether the given element or tree path has
	 * children. Avoid calling this method if the number of children has
	 * already been set.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 *
	 * @param elementOrTreePath the element, or tree path
	 * @param hasChildren new value.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void setHasChildren(final Object elementOrTreePath, final boolean hasChildren);

	/**
	 * Performs auto expand on an element at the specified path if the auto expand
	 * level dictates the element should be expanded.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 *
	 * @param elementPath tree path to element to consider for expansion
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void autoExpand(TreePath elementPath);

	/**
	 * Sets whether the node corresponding to the given element or tree path is
	 * expanded or collapsed.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 *
	 * @param elementOrTreePath
	 *            the element, or the tree path to the element
	 * @param expanded
	 *            <code>true</code> if the node is expanded, and
	 *            <code>false</code> if collapsed
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void setExpandedState(Object elementOrTreePath, boolean expanded);

	/**
	 * Expands all ancestors of the given element or tree path so that the given
	 * element becomes visible in this viewer's tree control, and then expands
	 * the subtree rooted at the given element to the given level.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 *
	 * @param elementOrTreePath
	 *            the element
	 * @param level
	 *            non-negative level, or <code>ALL_LEVELS</code> to expand all
	 *            levels of the tree
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void expandToLevel(Object elementOrTreePath, int level);

	/**
	 * Removes the given element from the viewer. The selection is updated if
	 * necessary.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 * @param elementOrTreePath the element, or the tree path to the element
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void remove(Object elementOrTreePath);

	/**
	 * Removes the element at the specified index of the parent.  The selection is updated if required.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 *
	 * @param parentOrTreePath the parent element, the input element, or a tree path to the parent element
	 * @param index child index
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void remove(Object parentOrTreePath, final int index);

	/**
	 * Inserts the given element as a new child element of the given parent
	 * element at the given position. If this viewer has a sorter, the position
	 * is ignored and the element is inserted at the correct position in the
	 * sort order.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 * @param parentOrTreePath the parent element, or the tree path to the parent
	 *
	 * @param element the element
	 * @param position a 0-based position relative to the model, or -1 to indicate
	 * the last position
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void insert(Object parentOrTreePath, Object element, int position);

	/**
	 * Returns whether the candidate selection should override the current
	 * selection.
	 * @param current Current selection in viewer.
	 * @param candidate Proposed new selection.
	 * @return whether new selection should override the current
	 */
	boolean overrideSelection(ISelection current, ISelection candidate);

	/**
	 * Returns whether the node corresponding to the given element or tree path
	 * is expanded or collapsed.
	 *
	 * @param elementOrTreePath
	 *            the element
	 * @return <code>true</code> if the node is expanded, and
	 *         <code>false</code> if collapsed
	 */
	boolean getExpandedState(Object elementOrTreePath);

	/**
	 * Returns whether the node corresponding to the given element or tree path
	 * has any child elements.
	 *
	 * @param elementOrTreePath Path to element
	 * @return Returns whether the given element has children.
	 */
	boolean getHasChildren(Object elementOrTreePath);

	/**
	 * Returns the child count of the element at the given path. <br>
	 * Note: The child count may be incorrect if the element is not
	 * expanded in the tree.
	 *
	 * @param path Path to get count for.
	 * @return The child count.
	 */
	int getChildCount(TreePath path);

	/**
	 * Returns the element which is a child of the element at the
	 * given path, with the given index.
	 *
	 * @param path Path to parent element.
	 * @param index Index of child element.
	 * @return Child element.
	 */
	Object getChildElement(TreePath path, int index);

	/**
	 * Returns the tree path of the element that is at the top of the
	 * viewer.
	 *
	 * @return the tree path of the element at the top of the
	 * viewer.
	 */
	TreePath getTopElementPath();

	/**
	 * Finds the index of the given element with a parent of given path.
	 *
	 * @param parentPath Path of parent element.
	 * @param element Element to find.
	 *
	 * @return The element's index, or -1 if not found.
	 */
	int findElementIndex(TreePath parentPath, Object element);

	/**
	 * Returns a boolean indicating whether all the child elements of the
	 * given parent have been realized already.
	 *
	 * @param parentPath Path of parent element.
	 * @return true if all children realized
	 */
	boolean getElementChildrenRealized(TreePath parentPath);

	/**
	 * Clears the selection in the viewer, if any, without firing
	 * selection change notification. This is only to be used by
	 * the platform.
	 */
	void clearSelectionQuiet();

	/**
	 * Sets the element's display information.
	 * <p>
	 * This method should only be called by the viewer framework.
	 * </p>
	 *
	 * @param path Element path.
	 * @param numColumns Number of columns in the data.
	 * @param labels Array of labels.  The array cannot to be
	 * <code>null</code>, but values within the array may be.
	 * @param images Array of image descriptors, may be <code>null</code>.
	 * @param fontDatas Array of fond data objects, may be <code>null</code>.
	 * @param foregrounds Array of RGB values for foreground colors, may be
	 * <code>null</code>.
	 * @param backgrounds Array of RGB values for background colors, may be
	 * <code>null</code>.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	void setElementData(TreePath path, int numColumns, String[] labels, ImageDescriptor[] images, FontData[] fontDatas, RGB[] foregrounds, RGB[] backgrounds);

	/**
	 * Returns identifiers of the visible columns in this viewer, or <code>null</code>
	 * if there is currently no column presentation.
	 *
	 * @return visible columns or <code>null</code>
	 */
	String[] getVisibleColumns();

	/**
	 * Sets the element check state data.
	 *
	 * @param path Path of element to check.
	 * @param checked if true, item will be checked
	 * @param grayed if true item will be grayed
	 */
	void setElementChecked(TreePath path, boolean checked, boolean grayed);

	/**
	 * Retrieves the element check state.
	 *
	 * @param path Path of element to return check state for.
	 * @return the element checked state
	 */
	boolean getElementChecked(TreePath path);

	/**
	 * Retrieves the element's check box grayed state.
	 *
	 * @param path Path of element to return grayed state for.
	 * @return the element grayed state
	 */
	boolean getElementGrayed(TreePath path);
}
