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
 *     IBM Corporation - ongoing bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.debug.internal.ui.viewers.model.ILabelUpdateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.widgets.Display;

/**
 * Interface of an tree model viewer.  It declares the common methods for the
 * JFace-based {@link TreeModelViewer} and the UI-less
 * {@link VirtualTreeModelViewer}.
 *
 * @since 3.8
 */
public interface ITreeModelViewer extends ISelectionProvider {

	/**
	 * Constant indicating that all levels of the tree should be expanded or
	 * collapsed.
	 *
	 * @see #setAutoExpandLevel(int)
	 * @see #getAutoExpandLevel()
	 */
	int ALL_LEVELS = -1;

	/**
	 * Returns the Display object that this viewer is in.  The
	 * display object can be used by clients to access the display thread
	 * to call the viewer methods.
	 *
	 * @return The display.
	 */
	Display getDisplay();

	/**
	 * Returns this viewer's presentation context.
	 *
	 * @return presentation context
	 */
	IPresentationContext getPresentationContext();

	/**
	 * Returns the current input of this viewer, or <code>null</code>
	 * if none. The viewer's input provides the "model" for the viewer's
	 * content.
	 *
	 * @return Input object
	 */
	Object getInput();

	/**
	 * Sets the input of this viewer.  Setting the input resets the
	 * viewer's contents and triggers an update starting at the input
	 * element.
	 *
	 * @param object Input element, or <code>null</code> if none.
	 */
	void setInput(Object object);

	/**
	 * Returns the current selection in viewer.
	 *
	 * @return selection object
	 */
	@Override ISelection getSelection();

	/**
	 * Sets a new selection for this viewer and optionally makes it visible.
	 * The selection is not set if the model selection policy overrides the
	 * attempt to set the selection.
	 *
	 * @param selection the new selection
	 * @param reveal <code>true</code> if the selection is to be made
	 *   visible, and <code>false</code> otherwise
	 * @param force <code>true</code> if the selection should override the
	 *   model selection policy
	 */
	void setSelection(ISelection selection, boolean reveal, boolean force);

	/**
	 * Attempts to set the selection for this viewer and optionally makes it visible.
	 * The selection is not set if the model selection policy overrides the
	 * attempt to set the selection.
	 *
	 * @param selection the new selection
	 * @param reveal whether to make the selection visible after successfully setting
	 *  the selection
	 * @param force whether to force the selection (override the model selection policy)
	 * @return <code>true</code> if the selection was set and <code>false</code> if the
	 *  model selection policy overrides the selection attempt
	 */
	boolean trySelection(ISelection selection, boolean reveal, boolean force);

	/**
	 * Returns the auto-expand level.
	 *
	 * @return non-negative level, or <code>ALL_LEVELS</code> if all levels of
	 *         the tree are expanded automatically
	 * @see #setAutoExpandLevel
	 */
	int getAutoExpandLevel();

	/**
	 * Sets the auto-expand level to be used when the input of the viewer is set
	 * using {@link #setInput(Object)}. The value 0 means that there is no
	 * auto-expand; 1 means that the invisible root element is expanded (since
	 * most concrete implementations do not show the root element, there is usually
	 * no practical difference between using the values 0 and 1); 2 means that
	 * top-level elements are expanded, but not their children; 3 means that
	 * top-level elements are expanded, and their children, but not
	 * grandchildren; and so on.
	 * <p>
	 * The value <code>ALL_LEVELS</code> means that all subtrees should be
	 * expanded.
	 * </p>
	 *
	 * @param level
	 *            non-negative level, or <code>ALL_LEVELS</code> to expand all
	 *            levels of the tree
	 */
	void setAutoExpandLevel(int level);

	/**
	 * Returns the label data for the given element and for the given column,
	 * Returns <code>null</code> if the given element is not found or is not
	 * materialized in the virtual viewer.  Clients may listen to label update
	 * events to be notified when element labels are updated.
	 *
	 * @param path Path of the element.
	 * @param columnId ID of the column for which to return the label data.
	 * @return Label object containing the label information.  Can be
	 * <code>null</code> if the given element is not found or is not
	 * materialized in the virtual viewer.
	 */
	ViewerLabel getElementLabel(TreePath path, String columnId);

	/**
	 * Registers the specified listener for view update notifications.
	 *
	 * @param listener Listener to add
	 */
	void addViewerUpdateListener(IViewerUpdateListener listener);

	/**
	 * Removes the specified listener from update notifications.
	 *
	 * @param listener Listener to remove
	 */
	void removeViewerUpdateListener(IViewerUpdateListener listener);

	/**
	 * Registers the specified listener for state update notifications.
	 *
	 * @param listener Listener to add
	 */
	void addStateUpdateListener(IStateUpdateListener listener);

	/**
	 * Removes the specified listener from state update notifications.
	 *
	 * @param listener Listener to remove
	 */
	void removeStateUpdateListener(IStateUpdateListener listener);

	/**
	 * Registers the specified listener for view label update notifications.
	 *
	 * @param listener Listener to add
	 */
	void addLabelUpdateListener(ILabelUpdateListener listener);

	/**
	 * Removes the specified listener from view label update notifications.
	 *
	 * @param listener Listener to remove
	 */
	void removeLabelUpdateListener(ILabelUpdateListener listener);

	/**
	 * Registers the given listener for model delta notification.
	 * This listener is called immediately after the viewer processes
	 * the delta.
	 *
	 * @param listener Listener to add
	 */
	void addModelChangedListener(IModelChangedListener listener);

	/**
	 * Removes the given listener from model delta notification.
	 *
	 * @param listener Listener to remove
	 */
	void removeModelChangedListener(IModelChangedListener listener);

	/**
	 * Writes state information into a delta for the sub-tree at the given
	 * path.  It adds delta nodes and IModelDelta.EXPAND and IModelDelta.SELECT
	 * as it parses the sub-tree.
	 *
	 * @param path Path where to start saving the state.
	 * @param delta The delta where the state is to be saved.
	 * @param flagsToSave The flags to preserve during the state save.  The
	 * supported flags are <code>IModelDelta.SELECT</code>,
	 * <code>IModelDelta.EXPAND</code>, <code>IModelDelta.COLLAPSE</code>.
	 * @return Returns whether the state was saved for the given path.  Will
	 * return <code>false</code> if an element at the given path cannot
	 * be found.
	 */
	boolean saveElementState(TreePath path, ModelDelta delta, int flagsToSave);

	/**
	 * Causes the viewer to process the given delta as if it came from a
	 * model proxy.  This method is intended to be used to restore state
	 * saved using {@link #saveElementState(TreePath, ModelDelta, int)}.
	 *
	 * @param delta Delta to process.
	 */
	void updateViewer(IModelDelta delta);

	/**
	 * Triggers an update of the given element and its children.  If
	 * multiple instances of the given element are found in the tree,
	 * they will all be updated.
	 *
	 * @param element Element to update.
	 */
	void refresh(Object element);

	/**
	 * Triggers a full update of all the elements in the tree.
	 */
	void refresh();

	/**
	 * Returns the paths at which the given element is found realized in viewer
	 * or an empty array if not found.
	 *
	 * @param element Element to find.
	 * @return Array of paths for given element.
	 */
	TreePath[] getElementPaths(Object element);

	/**
	 * Returns filters currently configured in viewer.
	 *
	 * @return filter array in viewer.
	 */
	ViewerFilter[] getFilters();

	/**
	 * Add a new filter to use in viewer.
	 *
	 * @param filter Filter to add.
	 */
	void addFilter(ViewerFilter filter);

	/**
	 * Sets viewer filters to the filters in array.
	 *
	 * @param filters New filter array to use.
	 */
	void setFilters(ViewerFilter... filters);

 }
