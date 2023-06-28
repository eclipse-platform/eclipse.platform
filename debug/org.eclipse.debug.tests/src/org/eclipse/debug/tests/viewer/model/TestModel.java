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
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ICheckUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementCompareRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.swt.widgets.Display;

import org.junit.Assert;

/**
 * Test model for the use in unit tests.  This test model contains a set of
 * elements in a tree structure.  It contains utility methods for modifying the
 * model and for verifying that the viewer content matches the model.
 *
 * @since 3.6
 */
public class TestModel implements IElementContentProvider, IElementLabelProvider, IModelProxyFactory2 , IElementMementoProvider, IModelSelectionPolicyFactory {

	public static class TestElement extends PlatformObject {
		private final TestModel fModel;
		private final String fID;
		TestElement[] fChildren;
		String fLabelAppendix = ""; //$NON-NLS-1$
		boolean fExpanded;
		boolean fChecked;
		boolean fGrayed;

		public TestElement(TestModel model, String text, TestElement[] children) {
			this (model, text, false, false, children);
		}

		public TestElement(TestModel model, String text, boolean checked, boolean grayed, TestElement[] children) {
			fModel = model;
			fID = text;
			fChildren = children;
			fChecked = checked;
			fGrayed = grayed;
		}

		public TestModel getModel() {
			return fModel;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter.isInstance(fModel)) {
				return (T) fModel;
			}
			return null;
		}

		public String getID() {
			return fID;
		}

		public void setLabelAppendix(String appendix) {
			fLabelAppendix = appendix;
		}

		public String getLabel() {
			return fID + fLabelAppendix;
		}

		public TestElement[] getChildren() {
			return fChildren;
		}

		public boolean isExpanded() {
			return fExpanded;
		}

		public boolean getGrayed() {
			return fGrayed;
		}

		public boolean getChecked() {
			return fChecked;
		}

		public void setChecked(boolean checked, boolean grayed) {
			fChecked = checked;
			fGrayed = grayed;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TestElement && fID.equals(((TestElement)obj).fID);
		}

		@Override
		public int hashCode() {
			return fID.hashCode();
		}

		@Override
		public String toString() {
			return getLabel();
		}

		public int indexOf(TestElement child) {
			return Arrays.asList(fChildren).indexOf(child);
		}
	}

	private class ModelProxy extends AbstractModelProxy {
		@Override
		public void installed(Viewer viewer) {
			super.installed(viewer);
			ModelDelta rootDelta = TestModel.this.getBaseDelta(new ModelDelta(fInput, IModelDelta.NO_CHANGE));
			installSubModelProxies(fRootPath, rootDelta);
			fireModelChanged(rootDelta);
		}

		private void installSubModelProxies(TreePath path, ModelDelta delta) {
			TestElement element = getElement(path);
			if (element.fModel != TestModel.this) {
				// Found an element from a different model.  Install its proxy and return.
				delta.setFlags(delta.getFlags() | IModelDelta.INSTALL);
			} else {
				TestElement[] children = element.getChildren();

				for (int i = 0; i < children.length; i++) {
					installSubModelProxies(path.createChildPath(children[i]), delta.addNode(children[i], IModelDelta.NO_CHANGE));
				}
			}
		}
	}

	private TestElement fRoot;
	private Object fInput = null;
	private TreePath fRootPath = TreePath.EMPTY;
	private ModelProxy fModelProxy;
	private IModelSelectionPolicy fModelSelectionPolicy;
	private boolean fQueueingUpdates = false;
	private boolean fDelayUpdates = false;
	private List<IViewerUpdate> fQueuedUpdates = new LinkedList<>();

	/**
	 * Constructor private.  Use static factory methods instead.
	 */
	public TestModel() {}

	public TestElement getRootElement() {
		return fRoot;
	}

	public void setSelectionPolicy(IModelSelectionPolicy modelSelectionPolicy) {
		fModelSelectionPolicy = modelSelectionPolicy;
	}

	@Override
	public IModelSelectionPolicy createModelSelectionPolicyAdapter(Object element, IPresentationContext context) {
		return fModelSelectionPolicy;
	}

	public ModelDelta getBaseDelta(ModelDelta rootDelta) {
		ModelDelta delta = rootDelta;
		for (int i = 0; i < fRootPath.getSegmentCount(); i++) {
			ModelDelta subDelta = delta.getChildDelta(fRootPath.getSegment(i));
			if (subDelta == null) {
				subDelta = delta.addNode(fRootPath.getSegment(i), IModelDelta.NO_CHANGE);
			}
			delta = subDelta;
		}
		delta.setChildCount(getRootElement().getChildren().length);
		return delta;
	}

	public int getModelDepth() {
		return getDepth(getRootElement(), 0);
	}

	private int getDepth(TestElement element, int atDepth) {
		TestElement[] children = element.getChildren();
		if (children.length == 0) {
			return atDepth;
		}
		int depth = atDepth + 1;
		for (int i = 0; i < children.length; i++) {
			depth = Math.max(depth, getDepth(children[i], atDepth + 1));
		}

		return depth;
	}

	public void setQeueueingUpdate(boolean queueingUpdates) {
		fQueueingUpdates = queueingUpdates;
		if (!fQueueingUpdates) {
			processQueuedUpdates();
		}
	}

	public void setDelayUpdates(boolean delayUpdates) {
		fDelayUpdates = delayUpdates;
	}

	public List<IViewerUpdate> getQueuedUpdates() {
		return fQueuedUpdates;
	}

	public void processQueuedUpdates() {
		List<IViewerUpdate> updates = new ArrayList<>(fQueuedUpdates);
		fQueuedUpdates.clear();
		for (int i = 0; i < updates.size(); i++) {
			processUpdate(updates.get(i));
		}
	}

	public void processUpdate(IViewerUpdate update) {
		if (update instanceof IHasChildrenUpdate) {
			doHasChildrenUpdate((IHasChildrenUpdate)update);
		} else if (update instanceof IChildrenCountUpdate) {
			doChildrenCountUpdate((IChildrenCountUpdate)update);
		} else if (update instanceof IChildrenUpdate) {
			doChildrenUpdate((IChildrenUpdate)update);
		} else if (update instanceof ILabelUpdate) {
			doLabelUpdate((ILabelUpdate)update);
		} else if (update instanceof IElementCompareRequest) {
			doCompareElements((IElementCompareRequest)update);
		} else if (update instanceof IElementMementoRequest) {
			doEncodeElements((IElementMementoRequest)update);
		}
	}

	private void processUpdates(IViewerUpdate[] updates) {
		for (int i = 0; i < updates.length; i++) {
			processUpdate(updates[i]);
		}
	}

	private void doUpdate(final IViewerUpdate[] updates) {
		if (fQueueingUpdates) {
			fQueuedUpdates.addAll(Arrays.asList(updates));
		} else if (fDelayUpdates) {
			Display.getDefault().asyncExec(() -> processUpdates(updates));
		} else {
			processUpdates(updates);
		}
	}

	@Override
	public void update(IHasChildrenUpdate[] updates) {
		doUpdate(updates);
	}

	private void doHasChildrenUpdate(IHasChildrenUpdate update) {
		TestElement element = (TestElement)update.getElement();
		update.setHasChilren(element.getChildren().length > 0);
		update.done();
	}


	@Override
	public void update(IChildrenCountUpdate[] updates) {
		doUpdate(updates);
	}

	private void doChildrenCountUpdate(IChildrenCountUpdate update) {
		TestElement element = (TestElement)update.getElement();
		update.setChildCount(element.getChildren().length);
		update.done();
	}

	@Override
	public void update(IChildrenUpdate[] updates) {
		doUpdate(updates);
	}

	private void doChildrenUpdate(IChildrenUpdate update) {
		TestElement element = (TestElement)update.getElement();
		int endOffset = update.getOffset() + update.getLength();
		for (int j = update.getOffset(); j < endOffset; j++) {
			if (j < element.getChildren().length) {
				update.setChild(element.getChildren()[j], j);
			}
		}
		update.done();
	}

	@Override
	public void update(ILabelUpdate[] updates) {
		doUpdate(updates);
	}

	private void doLabelUpdate(ILabelUpdate update) {
		TestElement element = (TestElement)update.getElement();
		update.setLabel(element.getLabel(), 0);
		if (update instanceof ICheckUpdate &&
			Boolean.TRUE.equals(update.getPresentationContext().getProperty(ICheckUpdate.PROP_CHECK)))
		{
			((ICheckUpdate)update).setChecked(element.getChecked(), element.getGrayed());
		}
		update.done();
	}

	public final static String ELEMENT_MEMENTO_ID = "id"; //$NON-NLS-1$

	@Override
	public void compareElements(final IElementCompareRequest[] updates) {
		doUpdate(updates);
	}

	private void doCompareElements(IElementCompareRequest update) {
		String elementID = ((TestElement)update.getElement()).getID();
		String mementoID = update.getMemento().getString(ELEMENT_MEMENTO_ID);
		update.setEqual( elementID.equals(mementoID) );
		update.done();
	}

	@Override
	public void encodeElements(IElementMementoRequest[] updates) {
		doUpdate(updates);
	}

	private void doEncodeElements(IElementMementoRequest update) {
		String elementID = ((TestElement)update.getElement()).getID();
		update.getMemento().putString(ELEMENT_MEMENTO_ID, elementID);
		update.done();
	}

	/**
	 * @param context the context
	 * @param viewerInput the input
	 * @param path the path
	 * @param checked the checked state
	 */
	public void elementChecked(IPresentationContext context, Object viewerInput, TreePath path, boolean checked) {
		TestElement element = getElement(path);
		Assert.assertFalse(element.getGrayed());
		element.setChecked(checked, false);
	}

	@Override
	public IModelProxy createTreeModelProxy(Object input, TreePath path, IPresentationContext context) {
		fModelProxy = new ModelProxy();
		fInput = input;
		fRootPath = path;
		return fModelProxy;
	}

	public IModelProxy getModelProxy() {
		return fModelProxy;
	}

	public TestElement getElement(TreePath path) {
		if (path.getSegmentCount() == 0) {
			return getRootElement();
		} else {
			return (TestElement)path.getLastSegment();
		}
	}

	public void setAllExpanded() {
		doSetExpanded(fRoot);
	}

	private void doSetExpanded(TestElement element) {
		element.fExpanded = true;
		for (int i = 0; i < element.fChildren.length; i++) {
			doSetExpanded(element.fChildren[i]);
		}
	}

	public void setAllAppendix(String appendix) {
		doSetAllAppendix(fRoot, appendix);
	}

	private void doSetAllAppendix(TestElement element, String appendix) {
		element.setLabelAppendix(appendix);
		for (int i = 0; i < element.fChildren.length; i++) {
			doSetAllAppendix(element.fChildren[i], appendix);
		}
	}

	public void validateData(ITreeModelViewer viewer, TreePath path) {
		validateData(viewer, path, false);
	}

	public void validateData(ITreeModelViewer _viewer, TreePath path, boolean expandedElementsOnly) {
		validateData(_viewer, path, expandedElementsOnly, TestModelUpdatesListener.EMPTY_FILTER_ARRAY);
	}

	public void validateData(ITreeModelViewer _viewer, TreePath path, boolean expandedElementsOnly, ViewerFilter[] filters) {
		IInternalTreeModelViewer viewer = (IInternalTreeModelViewer)_viewer;
		TestElement element = getElement(path);
		if ( Boolean.TRUE.equals(_viewer.getPresentationContext().getProperty(ICheckUpdate.PROP_CHECK)) ) {
			Assert.assertTrue(element.getChecked() == viewer.getElementChecked(path));
			Assert.assertTrue(element.getGrayed() == viewer.getElementGrayed(path));
		}

		if (!expandedElementsOnly || path.getSegmentCount() == 0 || viewer.getExpandedState(path) ) {
			TestElement[] children = element.getChildren();

			int viewerIndex = 0;
			for (int i = 0; i < children.length; i++) {
				if (TestModelUpdatesListener.isFiltered(children[i], filters)) {
					continue;
				}
				Assert.assertEquals(children[i], viewer.getChildElement(path, viewerIndex));
				validateData(viewer, path.createChildPath(children[i]), expandedElementsOnly, filters);
				viewerIndex++;
			}
			Assert.assertEquals(viewerIndex, viewer.getChildCount(path));
		} else if (!viewer.getExpandedState(path)) {
			// If element not expanded, verify the plus sign.
			Assert.assertTrue(viewer.getHasChildren(path) == element.getChildren().length > 0);
		}
	}

	public void setRoot(TestElement root) {
		fRoot = root;
	}

	public void postDelta(IModelDelta delta) {
		fModelProxy.fireModelChanged(delta);
	}

	/** Create or retrieve delta for given path
	 * @param combine if then new deltas for the given path are created. If false existing ones are reused.
	 */
	public ModelDelta getElementDelta(ModelDelta baseDelta, TreePath path, boolean combine) {
		TestElement element = getRootElement();
		ModelDelta delta = baseDelta;

		for (int i = 0; i < path.getSegmentCount(); i++) {
			TestElement[] children = element.getChildren();
			delta.setChildCount(children.length);
			Object segment = path.getSegment(i);
			int j;
			for (j = 0; j < children.length; j++) {
				if (segment.equals(children[j])) {
					element = children[j];
					ModelDelta nextDelta = null;
					if (combine) {
						nextDelta = delta.getChildDelta(element);
					}
					if (nextDelta == null) {
						nextDelta = delta.addNode(element, j, IModelDelta.NO_CHANGE, element.getChildren().length);
					}
					delta = nextDelta;
					break;
				}
			}
			if (j == children.length) {
				throw new IllegalArgumentException("Invalid path"); //$NON-NLS-1$
			}
		}
		return delta;

	}

	private TreePath getRelativePath(TreePath path) {
		Object[] segments = new Object[path.getSegmentCount() - fRootPath.getSegmentCount()];
		for (int i = fRootPath.getSegmentCount(), _i = 0; i < path.getSegmentCount(); i++, _i++) {
			segments[_i] = path.getSegment(i);
		}
		return new TreePath(segments);
	}

	public ModelDelta appendElementLabel(TreePath path, String labelAppendix) {
		org.junit.Assert.assertTrue(path.startsWith(fRootPath, null));
		ModelDelta rootDelta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
		ModelDelta baseDelta = getBaseDelta(rootDelta);
		TreePath relativePath = getRelativePath(path);
		TestElement element = getElement(relativePath);
		ModelDelta delta = getElementDelta(baseDelta, relativePath, false);
		element.setLabelAppendix(labelAppendix);
		delta.setFlags(delta.getFlags() | IModelDelta.STATE);

		return rootDelta;
	}

	public ModelDelta setElementChecked(TreePath path, boolean checked, boolean grayed) {
		Assert.assertTrue(path.startsWith(fRootPath, null));
		ModelDelta rootDelta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
		ModelDelta baseDelta = getBaseDelta(rootDelta);
		TreePath relativePath = getRelativePath(path);
		TestElement element = getElement(relativePath);
		ModelDelta delta = getElementDelta(baseDelta, relativePath, false);
		element.setChecked(checked, grayed);
		delta.setFlags(delta.getFlags() | IModelDelta.STATE);

		return rootDelta;
	}

	public ModelDelta setElementChildren(TreePath path, TestElement[] children) {
		Assert.assertTrue(path.startsWith(fRootPath, null));
		ModelDelta rootDelta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
		ModelDelta baseDelta = getBaseDelta(rootDelta);
		TreePath relativePath = getRelativePath(path);

		// Find the parent element and generate the delta node for it.
		TestElement element = getElement(relativePath);
		ModelDelta delta = getElementDelta(baseDelta, relativePath, false);

		// Set the new children array
		element.fChildren = children;

		// Add the delta flag and update the child count in the parent delta.
		delta.setFlags(delta.getFlags() | IModelDelta.CONTENT);
		delta.setChildCount(children.length);

		return rootDelta;
	}

	public ModelDelta replaceElementChild(TreePath parentPath, int index, TestElement child) {
		ModelDelta rootDelta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
		ModelDelta baseDelta = getBaseDelta(rootDelta);
		TreePath relativePath = getRelativePath(parentPath);

		TestElement element = getElement(relativePath);
		ModelDelta delta= getElementDelta(baseDelta, relativePath, false);
		TestElement oldChild = element.fChildren[index];
		element.fChildren[index] = child;
		delta.addNode(oldChild, child, IModelDelta.REPLACED);
		// TODO: set replacement index!?!

		return rootDelta;
	}

	public ModelDelta addElementChild(TreePath parentPath, ModelDelta rootDelta, int index, TestElement newChild) {
		if (rootDelta == null) {
			rootDelta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
		}
		ModelDelta baseDelta = getBaseDelta(rootDelta);
		TreePath relativePath = getRelativePath(parentPath);

		// Find the parent element and generate the delta node for it.
		TestElement element = getElement(relativePath);
		ModelDelta delta= getElementDelta(baseDelta, relativePath, true);

		// Add the new element
		element.fChildren = doInsertElementInArray(element.fChildren, index, newChild);

		// Add the delta flag and update the child count in the parent delta.
		delta.setChildCount(element.getChildren().length);
		delta.addNode(newChild, index, IModelDelta.ADDED);

		return rootDelta;
	}

	public ModelDelta insertElementChild(TreePath parentPath, int index, TestElement newChild) {
		return insertElementChild(null, parentPath, index, newChild);
	}

	public ModelDelta insertElementChild(ModelDelta rootDelta, TreePath parentPath, int index, TestElement newChild) {
		if (rootDelta == null) {
			rootDelta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
		}
		ModelDelta baseDelta = getBaseDelta(rootDelta);
		TreePath relativePath = getRelativePath(parentPath);

		// Find the parent element and generate the delta node for it.
		TestElement element = getElement(relativePath);
		ModelDelta delta= getElementDelta(baseDelta, relativePath, false);

		// Add the new element
		element.fChildren = doInsertElementInArray(element.fChildren, index, newChild);

		// Add the delta flag and update the child count in the parent delta.
		delta.setChildCount(element.getChildren().length);
		delta.addNode(newChild, index, IModelDelta.INSERTED);

		return rootDelta;
	}

	private TestElement[] doInsertElementInArray(TestElement[] children, int index, TestElement newChild) {
		// Create the new children array add the element to it and set it to
		// the parent.
		TestElement[] newChildren = new TestElement[children.length + 1];
		System.arraycopy(children, 0, newChildren, 0, index);
		newChildren[index] = newChild;
		System.arraycopy(children, index, newChildren, index + 1, children.length - index);
		return newChildren;
	}

	public ModelDelta removeElementChild(TreePath parentPath, int index) {
		ModelDelta rootDelta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
		ModelDelta baseDelta = getBaseDelta(rootDelta);

		// Find the parent element and generate the delta node for it.
		TestElement element = getElement(parentPath);
		ModelDelta delta= getElementDelta(baseDelta, parentPath, false);

		// Create a new child array with the element removed
		TestElement[] children = element.getChildren();
		TestElement childToRemove = children[index];
		TestElement[] newChildren = new TestElement[children.length - 1];
		System.arraycopy(children, 0, newChildren, 0, index);
		System.arraycopy(children, index + 1, newChildren, index, children.length - index - 1);
		element.fChildren = newChildren;

		// Add the delta flag and update the child count in the parent delta.
		delta.setChildCount(element.getChildren().length);
		delta.addNode(childToRemove, index, IModelDelta.REMOVED);

		return rootDelta;
	}

	public ModelDelta makeElementDelta(TreePath path, int flags) {
		ModelDelta rootDelta = new ModelDelta(fInput, IModelDelta.NO_CHANGE);
		ModelDelta baseDelta = getBaseDelta(rootDelta);

		// Find the element and generate the delta node for it.
		ModelDelta delta= getElementDelta(baseDelta, path, false);

		delta.setFlags(flags);
		return rootDelta;
	}

	public TreePath findElement(String label) {
		return findElement(TreePath.EMPTY, label);
	}

	public TreePath findElement(TreePath startPath, String label) {
		TestElement element = getElement(startPath);
		for (int i = 0; i < element.getChildren().length; i++) {
			TestElement child = element.getChildren()[i];
			TreePath path = startPath.createChildPath(child);
			if ( label.equals(child.getLabel()) ) {
				return path;
			} else {
				TreePath subPath = findElement(path, label);
				if (subPath != null) {
					return subPath;
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return getElementString(fRoot, ""); //$NON-NLS-1$
	}

	public String getElementString(TestElement element, String indent) {
		StringBuilder builder = new StringBuilder();
		builder.append(indent);
		builder.append(element.toString());
		builder.append('\n');
		TestElement[] children = element.getChildren();
		for (int i = 0; i < children.length; i++) {
			builder.append(getElementString(children[i], indent + "  ")); //$NON-NLS-1$
		}
		return builder.toString();
	}

	public static TestModel simpleSingleLevel() {
		TestModel model = new TestModel();
		model.setRoot( new TestElement(model, "root", makeSingleLevelModelElements(model, 6, ""))); //$NON-NLS-1$ //$NON-NLS-2$
		return model;
	}

	public static TestElement[] makeSingleLevelModelElements(TestModel model, int length, String prefix) {
		TestElement[] elements = new TestElement[length];
		for (int i = 1; i <= length; i++) {
			String name = prefix + i;
			elements[i - 1] = new TestElement(model, name, new TestElement[0]);
		}
		return elements;
	}

	public static TestElement[] makeMultiLevelElements(TestModel model, int depth, String prefix) {
		TestElement[] elements = new TestElement[depth];
		for (int i = 0; i < depth; i++) {
			String name = prefix + i;
			elements[i] = new TestElement(model, name, makeMultiLevelElements(model, i, name + ".")); //$NON-NLS-1$
		}
		return elements;
	}

	public static TestElement[] makeMultiLevelElements2(TestModel model, int[] levelCounts, String prefix) {
		if (levelCounts.length == 0) {
			return new TestElement[0];
		}
		int count = levelCounts[0];
		int[] oldLevelCounts = levelCounts;
		levelCounts = new int[levelCounts.length - 1];
		System.arraycopy(oldLevelCounts, 1, levelCounts, 0, levelCounts.length);
		TestElement[] elements = new TestElement[count];
		for (int i = 0; i < count; i++) {
			String name = prefix + i;
			elements[i] = new TestElement(model, name, makeMultiLevelElements2(model, levelCounts, name + ".")); //$NON-NLS-1$
		}
		return elements;
	}

	public static TestModel simpleMultiLevel() {
		TestModel model = new TestModel();
		model.setRoot( new TestElement(model, "root", new TestElement[] { //$NON-NLS-1$
			new TestElement(model, "1", new TestElement[0]), //$NON-NLS-1$
			new TestElement(model, "2", true, false, new TestElement[] { //$NON-NLS-1$
				new TestElement(model, "2.1", true, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(model, "2.2", false, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(model, "2.3", true, false, new TestElement[0]), //$NON-NLS-1$
			}),
			new TestElement(model, "3", new TestElement[] { //$NON-NLS-1$
				new TestElement(model, "3.1", new TestElement[] { //$NON-NLS-1$
					new TestElement(model, "3.1.1", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.1.2", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.1.3", new TestElement[0]), //$NON-NLS-1$
				}),
				new TestElement(model, "3.2", new TestElement[] { //$NON-NLS-1$
					new TestElement(model, "3.2.1", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.2.2", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.2.3", new TestElement[0]), //$NON-NLS-1$
				}),
				new TestElement(model, "3.3", new TestElement[] { //$NON-NLS-1$
					new TestElement(model, "3.3.1", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.3.2", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.3.3", new TestElement[0]), //$NON-NLS-1$
				}),
			})
		}) );
		return model;
	}

	public static TestModel compositeMultiLevel() {
		TestModel m2 = new TestModel();
		m2.setRoot( new TestElement(m2, "m2.root", new TestElement[] { //$NON-NLS-1$
			new TestElement(m2, "m2.1", new TestElement[0]), //$NON-NLS-1$
			new TestElement(m2, "m2.2", true, false, new TestElement[] { //$NON-NLS-1$
				new TestElement(m2, "m2.2.1", true, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(m2, "m2.2.2", false, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(m2, "m2.2.3", true, false, new TestElement[0]), //$NON-NLS-1$
			}),
		}) );

		TestModel m3 = new TestModel();
		m3.setRoot( new TestElement(m3, "m3.root", new TestElement[] { //$NON-NLS-1$
			new TestElement(m3, "m3.1", new TestElement[0]), //$NON-NLS-1$
			new TestElement(m3, "m3.2", true, false, new TestElement[] { //$NON-NLS-1$
				new TestElement(m3, "m3.2.1", true, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(m3, "m3.2.2", false, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(m3, "m3.2.3", true, false, new TestElement[0]), //$NON-NLS-1$
			}),
		}) );

		TestModel m4 = new TestModel();
		m4.setRoot( new TestElement(m4, "m4.root", new TestElement[] { //$NON-NLS-1$
			new TestElement(m4, "m4.1", new TestElement[0]), //$NON-NLS-1$
			new TestElement(m4, "m4.2", true, false, new TestElement[] { //$NON-NLS-1$
				new TestElement(m4, "m4.2.1", true, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(m4, "m4.2.2", false, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(m4, "m4.2.3", true, false, new TestElement[0]), //$NON-NLS-1$
			}),
		}) );

		TestModel m1 = new TestModel();
		m1.setRoot( new TestElement(m1, "m1.root", new TestElement[] { //$NON-NLS-1$
			new TestElement(m1, "m1.1", new TestElement[0]), //$NON-NLS-1$
			new TestElement(m1, "m1.2", true, false, new TestElement[] { //$NON-NLS-1$
				m2.fRoot,
				m3.fRoot,
				m4.fRoot,
			}),
		}) );


		return m1;
	}

	public static TestModel simpleDeepMultiLevel() {
		TestModel model = new TestModel();
		model.setRoot( new TestElement(model, "root", new TestElement[] { //$NON-NLS-1$
			new TestElement(model, "1", new TestElement[0]), //$NON-NLS-1$
			new TestElement(model, "2", true, false, new TestElement[] { //$NON-NLS-1$
				new TestElement(model, "2.1", true, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(model, "2.2", false, true, new TestElement[0]), //$NON-NLS-1$
				new TestElement(model, "2.3", true, false, new TestElement[0]), //$NON-NLS-1$
			}),
			new TestElement(model, "3", new TestElement[] { //$NON-NLS-1$
				new TestElement(model, "3.1", new TestElement[] { //$NON-NLS-1$
					new TestElement(model, "3.1.1", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.1.2", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.1.3", new TestElement[0]), //$NON-NLS-1$
				}),
				new TestElement(model, "3.2", new TestElement[] { //$NON-NLS-1$
					new TestElement(model, "3.2.1", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.2.2", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.2.3", new TestElement[0]), //$NON-NLS-1$
				}),
				new TestElement(model, "3.3", new TestElement[] { //$NON-NLS-1$
					new TestElement(model, "3.3.1", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.3.2", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.3.3", new TestElement[0]), //$NON-NLS-1$
				}),
				new TestElement(model, "3.4", new TestElement[] { //$NON-NLS-1$
					new TestElement(model, "3.4.1", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.4.2", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.4.3", new TestElement[0]), //$NON-NLS-1$
				}),
				new TestElement(model, "3.5", new TestElement[] { //$NON-NLS-1$
					new TestElement(model, "3.5.1", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.5.2", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.5.3", new TestElement[0]), //$NON-NLS-1$
				}),
				new TestElement(model, "3.6", new TestElement[] { //$NON-NLS-1$
					new TestElement(model, "3.6.1", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.6.2", new TestElement[0]), //$NON-NLS-1$
					new TestElement(model, "3.6.3", new TestElement[] { //$NON-NLS-1$
						new TestElement(model, "3.6.3.1", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.2", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.4", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.5", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.6", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.7", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.8", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.9", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.10", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.11", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.12", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.13", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.14", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.15", new TestElement[0]), //$NON-NLS-1$
						new TestElement(model, "3.6.3.16", new TestElement[] { //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.1", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.2", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.4", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.5", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.6", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.7", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.8", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.9", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.10", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.11", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.12", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.13", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.14", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.15", new TestElement[0]), //$NON-NLS-1$
							new TestElement(model, "3.6.3.16.16", new TestElement[] { //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.1", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.2", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.4", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.5", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.6", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.7", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.8", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.9", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.10", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.11", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.12", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.13", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.14", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.15", new TestElement[0]), //$NON-NLS-1$
								new TestElement(model, "3.6.3.16.16.16", new TestElement[] { //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.1", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.2", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.4", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.5", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.6", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.7", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.8", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.9", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.10", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.11", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.12", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.13", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.14", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.15", new TestElement[0]), //$NON-NLS-1$
									new TestElement(model, "3.6.3.16.16.16.16", new TestElement[] { //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.1", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.2", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.4", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.5", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.6", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.7", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.8", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.9", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.10", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.11", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.12", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.13", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.14", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.15", new TestElement[0]), //$NON-NLS-1$
										new TestElement(model, "3.6.3.16.16.16.16.16", new TestElement[] { //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.1", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.2", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.4", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.5", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.6", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.7", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.8", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.9", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.10", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.11", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.12", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.13", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.14", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.15", new TestElement[0]), //$NON-NLS-1$
											new TestElement(model, "3.6.3.16.16.16.16.16.16", new TestElement[0]), //$NON-NLS-1$
										}),
									}),
								}),
							}),
						}),
					}),
				}),
			})
		}) );
		return model;
	}

}
