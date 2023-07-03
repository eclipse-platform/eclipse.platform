/*******************************************************************************
 * Copyright (c) 2009, 2015 Wind River Systems and others.
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

import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStateUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Specialized tree viewer which displays only sub-tree of a full model.
 *
 * @since 3.5
 */
public class SubTreeModelViewer extends TreeModelViewer {

	/**
	 * The tree path in the model to the root element of this viewer.
	 */
	private TreePath fRootPath = TreePath.EMPTY;

	/**
	 * Viewer delegate that content and label providers refer to for viewer data.
	 */
	private DelegatingTreeModelViewer fDelegatingViewer;

	/**
	 * @return Returns the root element's model tree path.
	 */
	public TreePath getRootPath() {
		return fRootPath;
	}

	public SubTreeModelViewer(Composite parent, int style, IPresentationContext context) {
		super(parent, style, context);
	}

	/**
	 * Sets the viewer's input and root element's path
	 *
	 * @param input New viewer input.
	 * @param rootPath New root element path.
	 */
	public void setInput(Object input, TreePath rootPath) {
		fRootPath = rootPath;
		super.setInput(input);
	}

	/**
	 * A proxy for the sub tree viewer which is given to the content and
	 * label providers.  It translates the sub-tree paths in the viewer to the
	 * full model paths that the providers expect.
	 */
	public class DelegatingTreeModelViewer extends Viewer
		implements IInternalTreeModelViewer
	{
		@Override
		public void reveal(TreePath path, int index) {
			if (path.startsWith(fRootPath, null)) {
				SubTreeModelViewer.this.reveal(createSubPath(path), index);
			}
		}

		@Override
		public void replace(Object parentOrTreePath, int index, Object element) {
			if (parentOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)parentOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					SubTreeModelViewer.this.replace(createSubPath(path), index, element);
				}
			} else {
				SubTreeModelViewer.this.replace(parentOrTreePath, index, element);
			}
		}

		@Override
		public void setChildCount(Object elementOrTreePath, int count) {
			if (elementOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)elementOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					SubTreeModelViewer.this.setChildCount(createSubPath(path), count);
				}
			} else {
				SubTreeModelViewer.this.setChildCount(elementOrTreePath, count);
			}
		}

		@Override
		public void setHasChildren(Object elementOrTreePath, boolean hasChildren) {
			if (elementOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)elementOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					SubTreeModelViewer.this.setHasChildren(createSubPath(path), hasChildren);
				}
			} else {
				SubTreeModelViewer.this.setHasChildren(elementOrTreePath, hasChildren);
			}
		}

		@Override
		public void autoExpand(TreePath elementPath) {
			// not supported
		}

		@Override
		public void setExpandedState(Object elementOrTreePath, boolean expanded) {
			if (elementOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)elementOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					SubTreeModelViewer.this.setExpandedState(createSubPath(path), expanded);
				}
			} else {
				SubTreeModelViewer.this.setExpandedState(elementOrTreePath, expanded);
			}
		}

		@Override
		public void expandToLevel(Object elementOrTreePath, int level) {
			if (elementOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)elementOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					SubTreeModelViewer.this.expandToLevel(createSubPath(path), level);
				}
			} else {
				SubTreeModelViewer.this.expandToLevel(elementOrTreePath, level);
			}
		}

		@Override
		public void remove(Object elementOrTreePath) {
			if (elementOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)elementOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					SubTreeModelViewer.this.remove(createSubPath(path));
				}
			} else {
				SubTreeModelViewer.this.remove(elementOrTreePath);
			}
		}

		@Override
		public void remove(Object parentOrTreePath, final int index) {
			if (parentOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)parentOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					SubTreeModelViewer.this.remove(createSubPath(path), index);
				}
			} else {
				SubTreeModelViewer.this.remove(parentOrTreePath, index);
			}
		}

		@Override
		public void insert(Object parentOrTreePath, Object element, int position) {
			if (parentOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)parentOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					SubTreeModelViewer.this.insert(createSubPath(path), element, position);
				}
			} else {
				SubTreeModelViewer.this.insert(parentOrTreePath, element, position);
			}
		}

		@Override
		public boolean getExpandedState(Object elementOrTreePath) {
			if (elementOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)elementOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					return SubTreeModelViewer.this.getExpandedState(createSubPath(path));
				}
			} else {
				return SubTreeModelViewer.this.getExpandedState(elementOrTreePath);
			}
			return false;
		}

		@Override
		public int getChildCount(TreePath path) {
			if (path.startsWith(fRootPath, null)) {
				return SubTreeModelViewer.this.getChildCount(createSubPath(path));
			}
			return -1;
		}

		@Override
		public boolean getHasChildren(Object elementOrTreePath) {
			if (elementOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)elementOrTreePath;
				if (path.startsWith(fRootPath, null)) {
					return SubTreeModelViewer.this.getHasChildren(createSubPath(path));
				}
			} else {
				return SubTreeModelViewer.this.getHasChildren(elementOrTreePath);
			}
			return false;
		}

		@Override
		public Object getChildElement(TreePath path, int index) {
			if (path.startsWith(fRootPath, null)) {
				return SubTreeModelViewer.this.getChildElement(createSubPath(path), index);
			}
			return null;
		}

		@Override
		public TreePath getTopElementPath() {
			return createFullPath(SubTreeModelViewer.this.getTopElementPath());
		}

		@Override
		public int findElementIndex(TreePath parentPath, Object element) {
			if (parentPath.startsWith(fRootPath, null)) {
				return SubTreeModelViewer.this.findElementIndex(createSubPath(parentPath), element);
			}
			return -1;
		}

		@Override
		public boolean getElementChildrenRealized(TreePath parentPath) {
			if (parentPath.startsWith(fRootPath, null)) {
				return SubTreeModelViewer.this.getElementChildrenRealized(createSubPath(parentPath));
			}
			return true;
		}

		@Override
		public void setElementData(TreePath path, int numColumns, String[] labels, ImageDescriptor[] images, FontData[] fontDatas, RGB[] foregrounds, RGB[] backgrounds) {
			if (path.startsWith(fRootPath, null)) {
				SubTreeModelViewer.this.setElementData(createSubPath(path), numColumns, labels, images, fontDatas, foregrounds, backgrounds);
			}
		}

		@Override
		public Control getControl() {
			return SubTreeModelViewer.this.getControl();
		}

		@Override
		public Object getInput() {
			return SubTreeModelViewer.this.getInput();
		}

		@Override
		public ISelection getSelection() {
			return SubTreeModelViewer.this.getSelection();
		}

		@Override
		public void refresh() {
			SubTreeModelViewer.this.refresh();
		}

		@Override
		public void setInput(Object input) {
			SubTreeModelViewer.this.setInput(input);
		}

		@Override
		public void setSelection(ISelection selection, boolean reveal) {
			SubTreeModelViewer.this.setSelection(selection, reveal);
		}

		@Override
		public String[] getVisibleColumns() {
			 return SubTreeModelViewer.this.getVisibleColumns();
		}

		@Override
		public void addLabelUpdateListener(ILabelUpdateListener listener) {
			SubTreeModelViewer.this.addLabelUpdateListener(listener);
		}

		@Override
		public void addModelChangedListener(IModelChangedListener listener) {
			SubTreeModelViewer.this.addModelChangedListener(listener);
		}

		@Override
		public void addStateUpdateListener(IStateUpdateListener listener) {
			SubTreeModelViewer.this.addStateUpdateListener(listener);
		}

		@Override
		public void addViewerUpdateListener(IViewerUpdateListener listener) {
			SubTreeModelViewer.this.addViewerUpdateListener(listener);
		}

		@Override
		public int getAutoExpandLevel() {
			return SubTreeModelViewer.this.getAutoExpandLevel();
		}

		@Override
		public Display getDisplay() {
			return SubTreeModelViewer.this.getDisplay();
		}

		@Override
		public ViewerLabel getElementLabel(TreePath path, String columnId) {
			return SubTreeModelViewer.this.getElementLabel(path, columnId);
		}

		@Override
		public IPresentationContext getPresentationContext() {
			return SubTreeModelViewer.this.getPresentationContext();
		}

		@Override
		public void removeLabelUpdateListener(ILabelUpdateListener listener) {
			SubTreeModelViewer.this.removeLabelUpdateListener(listener);
		}

		@Override
		public void removeModelChangedListener(IModelChangedListener listener) {
			SubTreeModelViewer.this.removeModelChangedListener(listener);
		}

		@Override
		public void removeStateUpdateListener(IStateUpdateListener listener) {
			SubTreeModelViewer.this.removeStateUpdateListener(listener);
		}

		@Override
		public void removeViewerUpdateListener(IViewerUpdateListener listener) {
			SubTreeModelViewer.this.removeViewerUpdateListener(listener);
		}

		@Override
		public boolean saveElementState(TreePath path, ModelDelta delta, int deltaFlags) {
			return SubTreeModelViewer.this.saveElementState(path, delta, deltaFlags);
		}

		@Override
		public void setAutoExpandLevel(int level) {
			SubTreeModelViewer.this.setAutoExpandLevel(level);
		}

		@Override
		public void setSelection(ISelection selection, boolean reveal, boolean force) {
			SubTreeModelViewer.this.setSelection(selection, reveal, force);
		}

		@Override
		public boolean trySelection(ISelection selection, boolean reveal, boolean force) {
			return SubTreeModelViewer.this.trySelection(selection, reveal, force);
		}

		@Override
		public void updateViewer(IModelDelta delta) {
			SubTreeModelViewer.this.updateViewer(delta);
		}


		@Override
		public ViewerFilter[] getFilters() {
			return SubTreeModelViewer.this.getFilters();
		}

		@Override
		public void addFilter(ViewerFilter filter) {
			SubTreeModelViewer.this.addFilter(filter);
		}

		@Override
		public void setFilters(ViewerFilter... filters) {
			SubTreeModelViewer.this.setFilters(filters);
		}

		@Override
		public boolean overrideSelection(ISelection current, ISelection candidate) {
			return SubTreeModelViewer.this.overrideSelection(current, candidate);
		}

		@Override
		public void refresh(Object element) {
			SubTreeModelViewer.this.refresh(element);
		}

		@Override
		public void update(Object element) {
			SubTreeModelViewer.this.update(element);
		}

		@Override
		public void clearSelectionQuiet() {
			SubTreeModelViewer.this.clearSelectionQuiet();
		}

		@Override
		public TreePath[] getElementPaths(Object element) {
			TreePath[] subViewerPaths = SubTreeModelViewer.this.getElementPaths(element);
			TreePath[] retVal = new TreePath[subViewerPaths.length];
			for (int i = 0; i < subViewerPaths.length; i++) {
				retVal[i] = createFullPath(subViewerPaths[i]);
			}
			return retVal;
		}

		@Override
		public boolean getElementChecked(TreePath path) {
			return SubTreeModelViewer.this.getElementChecked(createSubPath(path));
		}

		@Override
		public boolean getElementGrayed(TreePath path) {
			return SubTreeModelViewer.this.getElementGrayed(createSubPath(path));
		}

		@Override
		public void setElementChecked(TreePath path, boolean checked, boolean grayed) {
			SubTreeModelViewer.this.setElementChecked(createSubPath(path), checked, grayed);
		}
	}


	/**
	 * Delegating content provider.  It translates all the calls to the
	 * underlying content provider to use full model tree paths.
	 */
	private class SubTreeModelContentProvider implements ITreeModelContentProvider {

		private TreeModelContentProvider fBaseProvider;

		public SubTreeModelContentProvider() {
			fBaseProvider = new TreeModelContentProvider();
		}

		@Override
		public void updateHasChildren(TreePath path) {
			fBaseProvider.updateHasChildren(createFullPath(path));
		}

		@Override
		public void updateChildCount(TreePath path, int currentChildCount) {
			fBaseProvider.updateChildCount(createFullPath(path), currentChildCount);
		}

		@Override
		public void updateElement(TreePath parentPath, int viewIndex) {
			fBaseProvider.updateElement(createFullPath(parentPath), viewIndex);
		}

		@Override
		public int viewToModelCount(TreePath parentPath, int count) {
			return fBaseProvider.viewToModelCount(createFullPath(parentPath), count);
		}

		@Override
		public int viewToModelIndex(TreePath parentPath, int index) {
			return fBaseProvider.viewToModelIndex(createFullPath(parentPath), index);
		}

		@Override
		public void addModelChangedListener(IModelChangedListener listener) {
			fBaseProvider.addModelChangedListener(listener);
		}

		@Override
		public void preserveState(TreePath subPath) {
			fBaseProvider.preserveState(createFullPath(subPath));
		}

		@Override
		public void addStateUpdateListener(IStateUpdateListener listener) {
			fBaseProvider.addStateUpdateListener(listener);
		}

		@Override
		public void addViewerUpdateListener(IViewerUpdateListener listener) {
			fBaseProvider.addViewerUpdateListener(listener);
		}

		@Override
		public int getModelDeltaMask() {
			return fBaseProvider.getModelDeltaMask();
		}

		@Override
		public int modelToViewChildCount(TreePath parentPath, int count) {
			return fBaseProvider.modelToViewChildCount(createFullPath(parentPath), count);
		}

		@Override
		public int modelToViewIndex(TreePath parentPath, int index) {
			return fBaseProvider.modelToViewIndex(createFullPath(parentPath), index);
		}

		@Override
		public void removeModelChangedListener(IModelChangedListener listener) {
			fBaseProvider.removeModelChangedListener(listener);
		}

		@Override
		public void removeStateUpdateListener(IStateUpdateListener listener) {
			fBaseProvider.removeStateUpdateListener(listener);
		}

		@Override
		public void removeViewerUpdateListener(IViewerUpdateListener listener) {
			fBaseProvider.removeViewerUpdateListener(listener);
		}

		@Override
		public void setModelDeltaMask(int mask) {
			fBaseProvider.setModelDeltaMask(mask);
		}

		@Override
		public boolean areTreeModelViewerFiltersApplicable(Object parentElement) {
			return fBaseProvider.areTreeModelViewerFiltersApplicable(parentElement);
		}

		@Override
		public boolean shouldFilter(Object parentElementOrTreePath, Object element) {
			if (parentElementOrTreePath instanceof TreePath) {
				TreePath path = (TreePath)parentElementOrTreePath;
				return fBaseProvider.shouldFilter(createFullPath(path), element);
			} else {
				return fBaseProvider.shouldFilter(parentElementOrTreePath, element);
			}

		}

		@Override
		public void unmapPath(TreePath path) {
			fBaseProvider.unmapPath(createFullPath(path));
		}

		@Override
		public void updateModel(IModelDelta delta, int mask) {
			fBaseProvider.updateModel(delta, mask);
		}

		@Override
		public TreePath[] getParents(Object element) {
			// Not used
			return null;
		}

		@Override
		public void cancelRestore(TreePath path, int flags) {
			fBaseProvider.cancelRestore(createFullPath(path), flags);
		}

		@Override
		public void dispose() {
			fBaseProvider.dispose();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fBaseProvider.inputChanged(fDelegatingViewer, oldInput, newInput);
		}

		@Override
		public void postInputChanged(IInternalTreeModelViewer viewer,
				Object oldInput, Object newInput) {
			fBaseProvider.postInputChanged(viewer, oldInput, newInput);
		}

		@Override
		public boolean setChecked(TreePath path, boolean checked) {
			return fBaseProvider.setChecked(createFullPath(path), checked);
		}

	}

	/**
	 * Delegating label provider.  It translates all the calls to the
	 * underlying label provider to use full model tree paths.
	 */
	private class SubTreeModelLabelProvider extends ColumnLabelProvider
		implements ITreeModelLabelProvider
	{

		private TreeModelLabelProvider fBaseProvider;

		public SubTreeModelLabelProvider(IInternalTreeModelViewer viewer) {
			fBaseProvider = new TreeModelLabelProvider(viewer);
		}

		@Override
		public boolean update(TreePath elementPath) {
			return fBaseProvider.update( createFullPath(elementPath) );
		}

		@Override
		public void addLabelUpdateListener(ILabelUpdateListener listener) {
			fBaseProvider.addLabelUpdateListener(listener);
		}

		@Override
		public Color getColor(RGB rgb) {
			return fBaseProvider.getColor(rgb);
		}

		@Override
		public Font getFont(FontData fontData) {
			return fBaseProvider.getFont(fontData);
		}

		@Override
		public Image getImage(ImageDescriptor descriptor) {
			return fBaseProvider.getImage(descriptor);
		}

		@Override
		public void removeLabelUpdateListener(ILabelUpdateListener listener) {
			fBaseProvider.removeLabelUpdateListener(listener);
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			fBaseProvider.addListener(listener);
		}

		@Override
		public void dispose() {
			fBaseProvider.dispose();
			super.dispose();
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return fBaseProvider.isLabelProperty(element, property);
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			fBaseProvider.removeListener(listener);
		}
	}

	private TreePath createFullPath(TreePath subPath) {
		if (fRootPath == null) {
			return TreePath.EMPTY;
		}

		Object[] segments = new Object[fRootPath.getSegmentCount() + subPath.getSegmentCount()];
		for (int i = 0; i < fRootPath.getSegmentCount(); i++) {
			segments[i] = fRootPath.getSegment(i);
		}
		for (int i = 0; i < subPath.getSegmentCount(); i++) {
			segments[i + fRootPath.getSegmentCount()] = subPath.getSegment(i);
		}
		return new TreePath(segments);
	}

	private TreePath createSubPath(TreePath fullPath) {
		if (fRootPath == null) {
			return TreePath.EMPTY;
		}

		if (fullPath.getSegmentCount() <= fRootPath.getSegmentCount()) {
			return TreePath.EMPTY;
		}
		Object[] segments = new Object[fullPath.getSegmentCount() - fRootPath.getSegmentCount()];
		for (int i = 0; i < segments.length; i++) {
			segments[i] = fullPath.getSegment(i + fRootPath.getSegmentCount());
		}
		return new TreePath(segments);
	}

	private DelegatingTreeModelViewer getDelegatingViewer() {
		if (fDelegatingViewer == null) {
			fDelegatingViewer = new DelegatingTreeModelViewer();
		}
		return fDelegatingViewer;
	}

	@Override
	protected ITreeModelContentProvider createContentProvider() {
		return new SubTreeModelContentProvider();
	}

	@Override
	protected ITreeModelLabelProvider createLabelProvider() {
		return new SubTreeModelLabelProvider(getDelegatingViewer());
	}


}
