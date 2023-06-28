/*******************************************************************************
 * Copyright (c) 2009, 2011 Wind River Systems and others.
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
package org.eclipse.debug.internal.ui.viewers.model;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStateUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewerFilter;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;

/**
 * {@link TreeModelViewer} content provider interface.
 *
 * @since 3.5
 */
public interface ITreeModelContentProvider extends ILazyTreePathContentProvider {

	/**
	 * Bit-mask which allows all possible model delta flags.
	 *
	 * @since 3.6
	 * @see #setModelDeltaMask(int)
	 */
	int ALL_MODEL_DELTA_FLAGS = ~0;

	/**
	 * Bit-mask which allows only flags which control selection and expansion.
	 *
	 * @since 3.6
	 * @see #setModelDeltaMask(int)
	 */
	int CONTROL_MODEL_DELTA_FLAGS =
		IModelDelta.EXPAND | IModelDelta.COLLAPSE | IModelDelta.SELECT | IModelDelta.REVEAL | IModelDelta.FORCE;

	/**
	 * Bit-mask which allows only flags which update viewer's information
	 * about the model.
	 *
	 * @since 3.6
	 * @see #setModelDeltaMask(int)
	 */
	int UPDATE_MODEL_DELTA_FLAGS =
		IModelDelta.ADDED | IModelDelta.CONTENT | IModelDelta.INSERTED | IModelDelta.INSTALL | IModelDelta.REMOVED |
		IModelDelta.REPLACED | IModelDelta.STATE | IModelDelta.UNINSTALL;

	/**
	 * Translates and returns the given child index from the viewer coordinate
	 * space to the model coordinate space.
	 *
	 * @param parentPath path to parent element
	 * @param index index of child element in viewer (filtered) space
	 * @return index of child element in model (raw) space
	 */
	int viewToModelIndex(TreePath parentPath, int index);

	/**
	 * Translates and returns the given child count from the viewer coordinate
	 * space to the model coordinate space.
	 *
	 * @param parentPath path to parent element
	 * @param count number of child elements in viewer (filtered) space
	 * @return number of child elements in model (raw) space
	 */
	int viewToModelCount(TreePath parentPath, int count);

	/**
	 * Translates and returns the given child index from the model coordinate
	 * space to the viewer coordinate space.
	 *
	 * @param parentPath path to parent element
	 * @param index index of child element in model (raw) space
	 * @return index of child element in viewer (filtered) space or -1 if filtered
	 */
	int modelToViewIndex(TreePath parentPath, int index);

	/**
	 * Returns whether the children of given element should be filtered.
	 * <p>This method is used to determine whether any of the registered filters
	 * that extend {@link TreeModelViewerFilter} are applicable to the given
	 * element.  If so, then children of given element should be filtered
	 * prior to populating them in the viewer.
	 *
	 * @param parentElement
	 *            the parent element
	 * @return whether there are any {@link TreeModelViewerFilter} filters
	 * applicable to given parent
	 */
	boolean areTreeModelViewerFiltersApplicable(Object parentElement);

	/**
	 * Returns whether the given element is filtered.
	 *
	 * @param parentElementOrTreePath
	 *            the parent element or path
	 * @param element
	 *            the child element
	 * @return whether to filter the element
	 */
	boolean shouldFilter(Object parentElementOrTreePath, Object element);

	/**
	 * Notification the given element is being unmapped.
	 *
	 * @param path Path to unmap
	 */
	void unmapPath(TreePath path);

	/**
	 * Sets the bit mask which will be used to filter the {@link IModelDelta}
	 * coming from the model.  Any delta flags which are hidden by the mask
	 * will be ignored.
	 *
	 * @param mask for <code>IModelDelta</code> flags
	 *
	 * @since 3.6
	 */
	void setModelDeltaMask(int mask);

	/**
	 * Returns the current model delta mask.
	 *
	 * @return bit mask used to filter model delta events.
	 *
	 * @see #setModelDeltaMask(int)
	 * @since 3.6
	 */
	int getModelDeltaMask();

	/**
	 * Translates and returns the given child count from the model coordinate
	 * space to the viewer coordinate space.
	 *
	 * @param parentPath path to parent element
	 * @param count child count element in model (raw) space
	 * @return child count in viewer (filtered) space
	 */
	int modelToViewChildCount(TreePath parentPath, int count);

	/**
	 * Registers the specified listener for view update notifications.
	 * @param listener Listener to add
	 */
	void addViewerUpdateListener(IViewerUpdateListener listener);

	/**
	 * Removes the specified listener from update notifications.
	 * @param listener Listener to remove
	 */
	void removeViewerUpdateListener(IViewerUpdateListener listener);

	/**
	 * Registers the given listener for model delta notification.
	 * This listener is called immediately after the viewer processes
	 * the delta.
	 * @param listener Listener to add
	 */
	void addModelChangedListener(IModelChangedListener listener);

	/**
	 * Removes the given listener from model delta notification.
	 * @param listener Listener to remove
	 */
	void removeModelChangedListener(IModelChangedListener listener);

	/**
	 * Causes the content provider to save the expansion and selection state
	 * of given element.  The state is then restored as the tree is lazily
	 * re-populated.
	 * @param path Path of the element to save.
	 */
	void preserveState(TreePath path);

	/**
	 * Registers the specified listener for state update notifications.
	 * @param listener Listener to add
	 * @since 3.6
	 */
	void addStateUpdateListener(IStateUpdateListener listener);

	/**
	 * Removes the specified listener from state update notifications.
	 * @param listener Listener to remove
	 * @since 3.6
	 */
	void removeStateUpdateListener(IStateUpdateListener listener);

	/**
	 * Instructs the content provider to process the given model delta.  This
	 * mechanism can be used to control the view's layout (expanding, selecting
	 * , etc.)
	 *
	 * @param delta The model delta to process.
	 * @param mask Mask that can be used to suppress processing of some of the
	 * delta flags
	 *
	 * @since 3.6
	 */
	void updateModel(IModelDelta delta, int mask);

	/**
	 * Instructs the content provider to cancel any pending state changes
	 * (i.e. SELECT, REVEAL, EXPAND, COLLAPSE) for the given path.  Pending
	 * state changes are changes the the viewer plans to re-apply to the
	 * viewer following a refresh.  If the user changes viewer state while
	 * the viewer is being refreshed, user's change should override the
	 * previous state.
	 *
	 * @param path Path of the element for which to cancel pending changes.
	 * @param flags Flags indicating the changes to cancel.
	 */
	void cancelRestore(TreePath path, int flags);

	/**
	 * Notifies the content provider that a client called {@link Viewer#setInput(Object)},
	 * and the viewer input is changed.
	 * This method is guaranteed to be called after {@link IContentProvider#inputChanged(Viewer, Object, Object)}
	 *
	 * @param viewer The viewer that uses this content provider.
	 * @param oldInput Old input object.
	 * @param newInput New input object.
	 *
	 * @since 3.8
	 */
	void postInputChanged(IInternalTreeModelViewer viewer, Object oldInput, Object newInput);

	/**
	 * Notifies the receiver that the given element has had its
	 * checked state modified in the viewer.
	 *
	 * @param path Path of the element that had its checked state changed
	 * @param checked The new checked state of the element
	 * @return false if the check state should not change
	 */
	boolean setChecked(TreePath path, boolean checked);
}
