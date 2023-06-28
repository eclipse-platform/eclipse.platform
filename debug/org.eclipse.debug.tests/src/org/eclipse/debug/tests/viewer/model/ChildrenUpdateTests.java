/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.model.ChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.ILabelUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.TreeModelContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStateUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;

/**
 * Tests coalescing of children update requests.
 *
 * @since 3.3
 */
public class ChildrenUpdateTests extends AbstractDebugTest {

	class BogusModelContentProvider extends TreeModelContentProvider {

		@Override
		protected IInternalTreeModelViewer getViewer() {
			return new IInternalTreeModelViewer(){

				@Override
				public void setSelection(ISelection selection) {}
				@Override
				public void removeSelectionChangedListener(ISelectionChangedListener listener) {}
				@Override
				public void addSelectionChangedListener(ISelectionChangedListener listener) {}
				@Override
				public void updateViewer(IModelDelta delta) {}
				@Override
				public void setSelection(ISelection selection, boolean reveal, boolean force) {}
				@Override
				public void clearSelectionQuiet() {}
				@Override
				public boolean trySelection(ISelection selection, boolean reveal, boolean force) { return true; }
				@Override
				public void setInput(Object object) {}
				@Override
				public void setAutoExpandLevel(int level) {}
				@Override
				public boolean saveElementState(TreePath path, ModelDelta delta, int flags) { return true; }
				@Override
				public void removeStateUpdateListener(IStateUpdateListener listener) {}
				@Override
				public void removeViewerUpdateListener(IViewerUpdateListener listener) {}
				@Override
				public void removeModelChangedListener(IModelChangedListener listener) {}
				@Override
				public void removeLabelUpdateListener(ILabelUpdateListener listener) {}
				@Override
				public void addViewerUpdateListener(IViewerUpdateListener listener) {}
				@Override
				public void addStateUpdateListener(IStateUpdateListener listener) {}
				@Override
				public void addModelChangedListener(IModelChangedListener listener) {}
				@Override
				public void addLabelUpdateListener(ILabelUpdateListener listener) {}
				@Override
				public void update(Object element) {}
				@Override
				public void setHasChildren(Object elementOrTreePath, boolean hasChildren) {}
				@Override
				public void setExpandedState(Object elementOrTreePath, boolean expanded) {}
				@Override
				public void setChildCount(Object elementOrTreePath, int count) {}
				@Override
				public void reveal(TreePath path, int index) {}
				@Override
				public void replace(Object parentOrTreePath, int index, Object element) {}
				@Override
				public void remove(Object parentOrTreePath, int index) {}
				@Override
				public void remove(Object elementOrTreePath) {}
				@Override
				public void refresh() {}
				@Override
				public void refresh(Object element) {}

				@Override
				public ISelection getSelection() {
					return null;
				}

				@Override
				public IPresentationContext getPresentationContext() {
					return null;
				}

				@Override
				public Object getInput() {
					return null;
				}

				@Override
				public ViewerLabel getElementLabel(TreePath path, String columnId) {
					return null;
				}

				@Override
				public Display getDisplay() {
					return DebugUIPlugin.getStandardDisplay();
				}

				@Override
				public int getAutoExpandLevel() {
					return 0;
				}


				@Override
				public boolean overrideSelection(ISelection current, ISelection candidate) {
					return false;
				}

				@Override
				public void insert(Object parentOrTreePath, Object element, int position) {
				}

				@Override
				public TreePath getTopElementPath() {
					return null;
				}

				@Override
				public ViewerFilter[] getFilters() {
					return null;
				}

				@Override
				public void addFilter(ViewerFilter filter) {}
				@Override
				public void setFilters(ViewerFilter... filters) {
				}

				@Override
				public boolean getExpandedState(Object elementOrTreePath) {
					return false;
				}

				@Override
				public Object getChildElement(TreePath path, int index) {
					return null;
				}

				@Override
				public boolean getHasChildren(Object elementOrTreePath) {
					return false;
				}

				@Override
				public int getChildCount(TreePath path) {
					return 0;
				}

				@Override
				public int findElementIndex(TreePath parentPath, Object element) {
					return 0;
				}

				@Override
				public void expandToLevel(Object elementOrTreePath, int level) {
				}

				@Override
				public void autoExpand(TreePath elementPath) {
				}

				@Override
				public boolean getElementChildrenRealized(TreePath parentPath) {
					return false;
				}

				@Override
				public boolean getElementChecked(TreePath path) {
					return false;
				}

				@Override
				public boolean getElementGrayed(TreePath path) {
					return false;
				}

				@Override
				public void setElementChecked(TreePath path, boolean checked, boolean grayed) {
				}

				@Override
				public TreePath[] getElementPaths(Object element) {
					return null;
				}
				@Override
				public void setElementData(TreePath path, int numColumns, String[] labels, ImageDescriptor[] images,
					FontData[] fontDatas, RGB[] foregrounds, RGB[] backgrounds) {
				}
				@Override
				public String[] getVisibleColumns() {
					return null;
				}
			};
		}
	}

	protected TreeModelContentProvider getContentProvider() {
		return new BogusModelContentProvider();
	}

	/**
	 * Tests coalescing of requests
	 */
	@Test
	public void testCoalesce () {
		Object element = new Object();
		TreeModelContentProvider cp = getContentProvider();
		ChildrenUpdate update1 = new ChildrenUpdate(cp, element, TreePath.EMPTY, element, 1, null);
		ChildrenUpdate update2 = new ChildrenUpdate(cp, element, TreePath.EMPTY, element, 2, null);
		assertTrue("Should coalesce", update1.coalesce(update2)); //$NON-NLS-1$
		assertEquals("Wrong offset", 1, update1.getOffset()); //$NON-NLS-1$
		assertEquals("Wrong length", 2, update1.getLength()); //$NON-NLS-1$

		update2 = new ChildrenUpdate(cp, element, TreePath.EMPTY, element, 3, null);
		assertTrue("Should coalesce", update1.coalesce(update2)); //$NON-NLS-1$
		assertEquals("Wrong offset", 1, update1.getOffset()); //$NON-NLS-1$
		assertEquals("Wrong length", 3, update1.getLength()); //$NON-NLS-1$

		update2 = new ChildrenUpdate(cp, element, TreePath.EMPTY, element, 2, null);
		assertTrue("Should coalesce", update1.coalesce(update2)); //$NON-NLS-1$
		assertEquals("Wrong offset", 1, update1.getOffset()); //$NON-NLS-1$
		assertEquals("Wrong length", 3, update1.getLength()); //$NON-NLS-1$

		update2 = new ChildrenUpdate(cp, element, TreePath.EMPTY, element, 5, null);
		assertFalse("Should not coalesce", update1.coalesce(update2)); //$NON-NLS-1$
		assertEquals("Wrong offset", 1, update1.getOffset()); //$NON-NLS-1$
		assertEquals("Wrong length", 3, update1.getLength()); //$NON-NLS-1$
	}
}
