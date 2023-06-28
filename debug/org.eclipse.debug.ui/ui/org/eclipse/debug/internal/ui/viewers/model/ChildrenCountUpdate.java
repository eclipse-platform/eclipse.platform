/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
 *     Wind River Systems - Fix for viewer state save/restore [188704]
 *     Pawel Piech (Wind River) - added support for a virtual tree model viewer (Bug 242489)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.jface.viewers.TreePath;

/**
 * @since 3.3
 */
class ChildrenCountUpdate extends ViewerUpdateMonitor implements IChildrenCountUpdate {

	/**
	 * Child count result.
	 */
	private int fCount = 0;

	/**
	 * Other child count updates for the same content provider.  Coalesced requests are
	 * batched together into an array.
	 */
	private List<ViewerUpdateMonitor> fBatchedRequests = null;

	/**
	 * Flag whether filtering is enabled in viewer.  If filtering is enabled, then a
	 * children update is performed on child elements to filter them as part of the
	 * child count calculation.
	 */
	private boolean fShouldFilter = false;

	/**
	 * Children indexes which are currently filtered.  When updating child count, also need
	 * to verify that currently filtered children are still filtered.
	 */
	private int[] fFilteredChildren = null;

	/**
	 * Children update used to filter children.
	 */
	private ChildrenUpdate fChildrenUpdate;

	/**
	 * Constructor
	 * @param provider the content provider to use for the update
	 * @param viewerInput the current input
	 * @param elementPath the path of the element to update
	 * @param element the element to update
	 * @param elementContentProvider the content provider for the element
	 */
	public ChildrenCountUpdate(TreeModelContentProvider provider, Object viewerInput, TreePath elementPath, Object element, IElementContentProvider elementContentProvider) {
		super(provider, viewerInput, elementPath, element, elementContentProvider, provider.getPresentationContext());
		fShouldFilter = provider.areTreeModelViewerFiltersApplicable(element);
		fFilteredChildren = provider.getFilteredChildren(elementPath);
	}

	@Override
	public synchronized void cancel() {
		if (fChildrenUpdate != null) {
			fChildrenUpdate.cancel();
		}
		super.cancel();
	}

	@Override
	protected synchronized void scheduleViewerUpdate() {
		// If filtering is enabled perform child update on all children in order to update
		// viewer filters.
		if (fShouldFilter || fFilteredChildren != null) {
			if (fChildrenUpdate == null) {
				int startIdx;
				int count;
				if (fShouldFilter) {
					startIdx = 0;
					count = getCount();
				} else {
					startIdx =  fFilteredChildren[0];
					int endIdx = fFilteredChildren[fFilteredChildren.length - 1];
					count = endIdx - startIdx + 1;
				}

				fChildrenUpdate = new ChildrenUpdate(getContentProvider(), getViewerInput(), getElementPath(), getElement(), startIdx, count, getElementContentProvider()) {
					@Override
					protected void performUpdate() {
						performUpdate(true);
						ChildrenCountUpdate.super.scheduleViewerUpdate();
					}

					@Override
					protected void scheduleViewerUpdate() {
						execInDisplayThread(() -> {
							if (!getContentProvider().isDisposed() && !isCanceled()) {
								performUpdate();
							}
						});
					}
				};
				execInDisplayThread(() -> fChildrenUpdate.startRequest());
				return;
			}
		} else {
			super.scheduleViewerUpdate();
		}
	}

	@Override
	protected void performUpdate() {
		int viewCount = fCount;
		TreePath elementPath = getElementPath();
		if (viewCount == 0) {
			getContentProvider().clearFilters(elementPath);
		} else {
			getContentProvider().setModelChildCount(elementPath, fCount);
			viewCount = getContentProvider().modelToViewChildCount(elementPath, fCount);
		}
		if (DebugUIPlugin.DEBUG_CONTENT_PROVIDER && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(getPresentationContext())) {
			DebugUIPlugin.trace("setChildCount(" + getElement() + ", modelCount: " + fCount + " viewCount: " + viewCount + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		// Special case for element 0 in a set of filtered elements:
		// Child 0 is automatically updated by the tree at the same time that the child count is requested. Therefore,
		// If this child count update filtered out this element, it needs to be updated again.
		if (fShouldFilter && getContentProvider().isFiltered(elementPath, 0)) {
			getContentProvider().updateElement(elementPath, 0);
		}
		getContentProvider().getViewer().setChildCount(elementPath, viewCount);
		getContentProvider().getStateTracker().restorePendingStateOnUpdate(getElementPath(), -1, true, true, false);
	}

	@Override
	public void setChildCount(int numChildren) {
		fCount = numChildren;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("IChildrenCountUpdate: "); //$NON-NLS-1$
		buf.append(getElement());
		return buf.toString();
	}

	@Override
	boolean coalesce(ViewerUpdateMonitor request) {
		if (request instanceof ChildrenCountUpdate) {
			if (getElementPath().equals(request.getElementPath())) {
				// duplicate request
				return true;
			} else if (getElementContentProvider().equals(request.getElementContentProvider())) {
				if (fBatchedRequests == null) {
					fBatchedRequests = new ArrayList<>(4);
					fBatchedRequests.add(this);
				}
				fBatchedRequests.add(request);
				return true;
			}
		}
		return false;
	}

	@Override
	void startRequest() {
		if (fBatchedRequests == null) {
			getElementContentProvider().update(new IChildrenCountUpdate[]{this});
		} else {
			IChildrenCountUpdate[] updates = fBatchedRequests.toArray(new IChildrenCountUpdate[fBatchedRequests.size()]);
			// notify that the other updates have also started to ensure correct sequence
			// of model updates - **** start at index 1 since the first (0) update has
			// already notified the content provider that it has started.
			for (int i = 1; i < updates.length; i++) {
				getContentProvider().updateStarted((ViewerUpdateMonitor) updates[i]);
			}
			getElementContentProvider().update(updates);
		}
	}

	@Override
	boolean containsUpdate(TreePath path) {
		if (getElementPath().equals(path)) {
			return true;
		} else if (fBatchedRequests != null) {
			for (ViewerUpdateMonitor request : fBatchedRequests) {
				if (request.getElementPath().equals(path)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	int getPriority() {
		return 2;
	}

	@Override
	TreePath getSchedulingPath() {
		TreePath path = getElementPath();
		if (path.getSegmentCount() > 0) {
			return path.getParentPath();
		}
		return path;
	}

	int getCount() {
		return fCount;
	}

	@Override
	protected boolean doEquals(ViewerUpdateMonitor update) {
		return
			update instanceof ChildrenCountUpdate &&
			getViewerInput().equals(update.getViewerInput()) &&
			getElementPath().equals(update.getElementPath());
	}

	@Override
	protected int doHashCode() {
		return getClass().hashCode() + getViewerInput().hashCode() + getElementPath().hashCode();
	}
}
