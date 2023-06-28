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
 *     IBM Corporation - clean-up
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.junit.Test;

/**
 * Tests to verify that the viewer can save and restore correctly the expansion
 * state of elements.
 *
 * @since 3.6
 */
abstract public class StateTests extends AbstractViewerModelTest implements ITestModelUpdatesListenerConstants {

	@Override
	protected TestModelUpdatesListener createListener(IInternalTreeModelViewer viewer) {
		return new TestModelUpdatesListener(viewer, false, false);
	}

	protected IInternalTreeModelViewer getInternalViewer() {
		return fViewer;
	}

	@Test
	public void testUpdateViewer() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);

		TestModel model = TestModel.simpleMultiLevel();

		// Create the listener
		fListener.reset();
		fListener.addChildreUpdate(TreePath.EMPTY, 0);
		fListener.addChildreUpdate(TreePath.EMPTY, 1);
		fListener.addChildreUpdate(TreePath.EMPTY, 2);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		// Create the update delta
		TestElement element = model.getRootElement();
		TreePath path0 = TreePath.EMPTY;
		ModelDelta delta = new ModelDelta(model.getRootElement(), -1, IModelDelta.EXPAND, element.getChildren().length);
		ModelDelta updateDelta = delta;
		element = element.getChildren()[2];
		TreePath path1 = path0.createChildPath(element);
		delta = delta.addNode(element, 2, IModelDelta.EXPAND, element.fChildren.length);
		element = element.getChildren()[1];
		TreePath path2 = path1.createChildPath(element);
		delta = delta.addNode(element, 1, IModelDelta.EXPAND, element.fChildren.length);
		element = element.getChildren()[1];
		TreePath path3 = path2.createChildPath(element);
		delta = delta.addNode(element, 1, IModelDelta.SELECT);

		fListener.reset(false, false);

		fListener.addChildreUpdate(path0, 2);
		fListener.addHasChildrenUpdate(path1);
		fListener.addChildreCountUpdate(path1);
		fListener.addLabelUpdate(path1);
		fListener.addChildreUpdate(path1, 1);
		fListener.addHasChildrenUpdate(path2);
		fListener.addChildreCountUpdate(path2);
		fListener.addLabelUpdate(path2);
		fListener.addHasChildrenUpdate(path2);
		fListener.addChildreCountUpdate(path2);
		fListener.addChildreUpdate(path2, 1);
		fListener.addHasChildrenUpdate(path3);
		fListener.addLabelUpdate(path3);

		fViewer.updateViewer(updateDelta);
		waitWhile(t -> !fListener.isFinished(CONTENT_SEQUENCE_COMPLETE | LABEL_UPDATES), createListenerErrorMessage());

		// Extract the new state from viewer
		ModelDelta savedDelta = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(path0, savedDelta, IModelDelta.EXPAND | IModelDelta.SELECT);

		if (!deltaMatches(updateDelta, savedDelta) ) {
			fail("Expected:\n" + updateDelta + "\nGot:\n" + savedDelta); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	boolean deltaMatches(ModelDelta requested, ModelDelta received) {
		if ( requested.getElement().equals(received.getElement()) &&
			requested.getFlags() == received.getFlags() &&
			( requested.getChildCount() == -1 || requested.getChildCount() == received.getChildCount() )&&
			( requested.getIndex() == -1 || requested.getIndex() == received.getIndex()) &&
			 ((requested.getReplacementElement() != null && requested.getReplacementElement().equals(received.getReplacementElement())) ||
			  (requested.getReplacementElement() == null && received.getReplacementElement() == null)) &&
			  requested.getChildDeltas().length == received.getChildDeltas().length)
		{
			for (int i = 0; i < requested.getChildDeltas().length; i++) {
				ModelDelta requestedChildDelta = (ModelDelta)requested.getChildDeltas()[i];
				ModelDelta receivedChildDelta = received.getChildDelta(requestedChildDelta.getElement());
				if ( receivedChildDelta == null || !deltaMatches(requestedChildDelta, receivedChildDelta) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Creates a model in the pattern of:
	 *
	 * root
	 *   1
	 *     1.1
	 *       1.1.1
	 *   2
	 *     2.1
	 *       2.1.1
	 *   3
	 *     3.1
	 *       3.1.1
	 * ...
	 *   (size)
	 *     (size).1
	 *       (size).1.1
	 */
	static TestModel alternatingSubsreesModel(int size) {
		TestModel model = new TestModel();

		TestElement[] elements = new TestElement[size];
		for (int i = 0; i < size; i++) {
			String text = Integer.toString(i + 1);
			elements[i] =
				new TestElement(model, text, new TestElement[] {
 new TestElement(model, text + ".1", new TestElement[] { //$NON-NLS-1$
			new TestElement(model, text + ".1.1", new TestElement[0]) //$NON-NLS-1$
					})
				});
		}
		model.setRoot(new TestElement(model, "root", elements)); //$NON-NLS-1$

		return model;
	}

	/**
	 * Creates a model in the pattern of:
	 *
	 * <pre>
	 * root
	 *   1
	 *     1.1
	 *     1.2
	 *     ...
	 *     1.childrenCount
	 *   2
	 *     2.1
	 *     2.2
	 *     ...
	 *     2.childrenCount
	 *   3
	 *     3.1
	 *     3.2
	 *     ...
	 *     3.childrenCount
	 *   ...
	 *   (size)
	 *     (size).1
	 *     (size).2
	 *     ...
	 *     (size).childrenCount
	 * </pre>
	 *
	 * @param size The number of elements in the tree
	 * @param childrenCount Number of children of each element
	 * @param shouldReturnChildren The supplier dictates whether children should
	 *            be reported when fetched
	 * @return The model
	 */
	static TestModel alternatingSubtreesModelWithChildren(int size, int childrenCount, Supplier<Boolean> shouldReturnChildren) {
		TestModel model = new TestModel();
		TestElement[] elements = new TestElement[size];
		for (int i = 0; i < size; i++) {
			String text = Integer.toString(i + 1);

			TestElement[] children = new TestElement[childrenCount];
			for (int x = 0; x < childrenCount; x++) {
				children[x] = new TestElement(model, text + "." + (x + 1), new TestElement[0]);
			}

			elements[i] = new TestElement(model, text, children) {
				@Override
				public TestElement[] getChildren() {
					if (shouldReturnChildren.get()) {
						return super.getChildren();
					}

					return new TestElement[0];
				}
			};
		}

		model.setRoot(new TestElement(model, "root", elements));

		return model;
	}

	static boolean areTreeSelectionsEqual(ITreeSelection sel1, ITreeSelection sel2) {
		Set<TreePath> sel1Set = new HashSet<>();
		sel1Set.addAll( Arrays.asList(sel1.getPaths()) );

		Set<TreePath> sel2Set = new HashSet<>();
		sel2Set.addAll( Arrays.asList(sel2.getPaths()) );

		return sel1Set.equals(sel2Set);
	}

	static void expandAlternateElements(TestModelUpdatesListener listener, TestModel model, boolean waitForAllUpdates) throws Exception {
		listener.reset();
		listener.setFailOnRedundantUpdates(false);

		TestElement rootElement = model.getRootElement();
		TestElement[] children = rootElement.getChildren();
		ModelDelta rootDelta = new ModelDelta(rootElement, IModelDelta.NO_CHANGE);
		ModelDelta expandDelta = model.getBaseDelta(rootDelta);
		for (int i = 0; i < children.length; i++) {
			// Expand only odd children
			if (i % 2 == 1) {
				continue;
			}

			// Expand the element and the first child of each sub-element
			TestElement element = children[i];
			ModelDelta delta = expandDelta;
			int index = i;
			while (element.getChildren().length != 0) {
				TreePath elementPath = model.findElement(element.getLabel());
				listener.addUpdates(
					elementPath, element, 1,
					CHILD_COUNT_UPDATES | (waitForAllUpdates ? CHILDREN_UPDATES : 0) );
				delta = delta.addNode(element, index, IModelDelta.EXPAND, element.getChildren().length);
				element = element.getChildren()[0];
				index = 0;
			}
		}
		model.postDelta(rootDelta);

		TestUtil.waitWhile(t -> !listener.isFinished(CONTENT_SEQUENCE_COMPLETE | MODEL_CHANGED_COMPLETE), null, 30000, t -> "Listener not finished: " + listener);
	}

	@Test
	public void testPreserveExpandedOnRemove() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(6);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		expandAlternateElements(fListener, model, true);

		// Set a selection in view
		TreeSelection originalSelection = new TreeSelection(model.findElement("5.1")); //$NON-NLS-1$
		fViewer.setSelection(originalSelection);

		// Update the model
		ModelDelta delta = model.removeElementChild(TreePath.EMPTY, 0);

		// Remove delta should not generate any new updates
		fListener.reset();
		model.postDelta(delta);
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Validate data
		model.validateData(fViewer, TreePath.EMPTY, true);
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );
	}

	@Test
	public void testPreserveExpandedOnInsert() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(6);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		expandAlternateElements(fListener, model, true);

		// Set a selection in view
		TreeSelection originalSelection = new TreeSelection(model.findElement("5.1")); //$NON-NLS-1$
		fViewer.setSelection(originalSelection);

		// Update the model
		ModelDelta delta = model.insertElementChild(TreePath.EMPTY, 0, new TestElement(model, "0 - new", new TestElement[0])); //$NON-NLS-1$

		// Insert delta should generate updates only for the new element
		TreePath path = model.findElement("0 - new"); //$NON-NLS-1$
		// Note: redundant label updates on insert.
		fListener.reset(path, (TestElement)path.getLastSegment(), 0, false, false);
		fListener.addChildreUpdate(TreePath.EMPTY, 0);
		model.postDelta(delta);
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		// Validate data
		model.validateData(fViewer, TreePath.EMPTY, true);
		assertTrue(getInternalViewer().getExpandedState(model.findElement("1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("1.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );
	}

	@Test
	public void testPreserveExpandedOnMultLevelContent() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(6);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		expandAlternateElements(fListener, model, true);

		// Set a selection in view
		// Set a selection in view
		TreeSelection originalSelection = new TreeSelection(
new TreePath[] { model.findElement("5"), model.findElement("5.1"), model.findElement("6") }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		fViewer.setSelection(originalSelection);
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

		// Update the model
		model.removeElementChild(TreePath.EMPTY, 0);

		// Note: Re-expanding nodes causes redundant updates.
		fListener.reset(false, false);
		fListener.addUpdates(getInternalViewer(), TreePath.EMPTY, model.getRootElement(), -1, ALL_UPDATES_COMPLETE);

		// Create the delta which has nodes with CONTENT flag set at multiple levels.
		ModelDelta rootDelta = new ModelDelta(model.getRootElement(), IModelDelta.CONTENT);
		ModelDelta elementDelta = model.getElementDelta(rootDelta, model.findElement("3.1.1"), true); //$NON-NLS-1$
		elementDelta.setFlags(IModelDelta.CONTENT);

		// Post the multi-content update delta
		model.postDelta(rootDelta);
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

		// Validate data
		model.validateData(fViewer, TreePath.EMPTY, true);
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

		// Note: in past it was observed sub-optimal coalescing in this test due
		// to scattered update requests from viewer.
		assertTrue( fListener.checkCoalesced(TreePath.EMPTY, 0, 5) );
	}

	@Test
	public void testKeepCollapsedAfterRemovingAndReaddingChildrenInExpandedTree() throws Exception {
		boolean showChildren[] = new boolean[] { true };
		int size = 3;
		Supplier<Boolean> shouldShowChildren = () -> showChildren[0];

		TestModel model = alternatingSubtreesModelWithChildren(size, 10, shouldShowChildren);

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		/*
		 * 1. Trigger model update with expansion of all elements
		 */
		{
			fListener.reset();
			fListener.setFailOnRedundantUpdates(false);

			TestElement rootElement = model.getRootElement();
			TestElement[] children = rootElement.getChildren();
			ModelDelta rootDelta = new ModelDelta(rootElement, IModelDelta.NO_CHANGE);
			ModelDelta expandDelta = model.getBaseDelta(rootDelta);
			for (int i = 0; i < children.length; i++) {
				TestElement element = children[i];
				ModelDelta delta = expandDelta;
				int index = i;
				while (element.getChildren().length != 0) {
					TreePath elementPath = model.findElement(element.getLabel());
					fListener.addUpdates(elementPath, element, 1, CHILD_COUNT_UPDATES | CHILDREN_UPDATES);
					delta = delta.addNode(element, index, IModelDelta.EXPAND | IModelDelta.CONTENT, element.getChildren().length);
					element = element.getChildren()[0];
					index = 0;
				}
			}

			model.postDelta(rootDelta);

			TestUtil.waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE), null, 300000, t -> "Listener not finished: " + fListener);
		}

		/*
		 * 2. Trigger model change with no children
		 */
		{
			fListener.reset();
			fListener.setFailOnRedundantUpdates(false);

			showChildren[0] = false;

			TestElement rootElement = model.getRootElement();
			ModelDelta rootDelta = new ModelDelta(rootElement, IModelDelta.CONTENT);
			model.getBaseDelta(rootDelta);

			TreePath elementPath = TreePath.EMPTY;
			fListener.addUpdates(elementPath, rootElement, 2, CHILD_COUNT_UPDATES | CHILDREN_UPDATES);

			model.postDelta(rootDelta);

			TestUtil.waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE), null, 60000, t -> "Listener not finished: " + fListener);
		}

		/*
		 * 3. Trigger model change with expansion for first element and its
		 * first child selected
		 */
		{
			fListener.reset();
			fListener.setFailOnRedundantUpdates(false);

			showChildren[0] = true;

			TestElement rootElement = model.getRootElement();
			TestElement[] children = rootElement.getChildren();
			ModelDelta rootDelta = new ModelDelta(rootElement, IModelDelta.NO_CHANGE);
			ModelDelta delta = model.getBaseDelta(rootDelta);

			// Expand the element and select first child
			TestElement element = children[0];
			delta = delta.addNode(element, 0, IModelDelta.EXPAND, element.getChildren().length);

			TreePath elementPath = model.findElement(element.getLabel());
			fListener.addUpdates(elementPath, element, 2, CHILDREN_UPDATES);

			element = element.getChildren()[0];
			delta = delta.addNode(element, 0, IModelDelta.SELECT, -1);

			model.postDelta(rootDelta);

			TestUtil.waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE), null, 60000, t -> "Listener not finished: " + fListener);
		}

		/*
		 * 4. Trigger model change to update all elements and their plus state
		 */
		{
			fListener.reset();
			fListener.setFailOnRedundantUpdates(false);

			showChildren[0] = true;

			TestElement rootElement = model.getRootElement();
			ModelDelta rootDelta = new ModelDelta(rootElement, IModelDelta.CONTENT);
			model.getBaseDelta(rootDelta);

			TestElement element = rootElement.getChildren()[0];
			TreePath elementPath = model.findElement(element.getLabel());
			fListener.addUpdates(elementPath, element, 1, CHILDREN_UPDATES);

			model.postDelta(rootDelta);

			TestUtil.waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE), null, 6000000, t -> "Listener not finished: " + fListener);
		}

		/*
		 * Only first element should be expanded, all other should be collapsed
		 */
		for (int i = 1; i <= size; i++) {
			assertTrue(getInternalViewer().getExpandedState(model.findElement(Integer.toString(i))) == (i == 1));
		}
	}

	@Test
	public void testPreserveExpandedOnSubTreeContent() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = TestModel.simpleMultiLevel();

		// Expand all
		fViewer.setAutoExpandLevel(-1);

		// Create the listener,
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		// Turn off auto-expansion
		fViewer.setAutoExpandLevel(0);

		// Set a selection in view
		TreeSelection originalSelection = new TreeSelection(model.findElement("3.3.1")); //$NON-NLS-1$
		fViewer.setSelection(originalSelection);

		// Update the model
		model.addElementChild(model.findElement("3"), null, 0, new TestElement(model, "3.0 - new", new TestElement[0])); //$NON-NLS-1$ //$NON-NLS-2$

		// Create the delta for element "3" with content update.
		TreePath elementPath = model.findElement("3"); //$NON-NLS-1$
		ModelDelta rootDelta = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		ModelDelta elementDelta = model.getElementDelta(rootDelta, elementPath, true);
		elementDelta.setFlags(IModelDelta.CONTENT);

		// Note: Re-expanding nodes causes redundant updates.
		fListener.reset(false, false);
		fListener.addUpdates(getInternalViewer(), elementPath, model.getElement(elementPath), -1, ALL_UPDATES_COMPLETE);

		// Post the sub-tree update
		model.postDelta(rootDelta);
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

		// Validate data
		model.validateData(fViewer, TreePath.EMPTY, true);
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		// On windows, getExpandedState() may return true for an element with no children:
		// assertTrue(getCTargetViewer().getExpandedState(model.findElement("3.0 - new")) == false);
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.2")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.3")) == true); //$NON-NLS-1$
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );
	}

	@Test
	public void testPreserveExpandedOnContentStress() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(6);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		expandAlternateElements(fListener, model, true);

		// Set a selection in view
//        TreeSelection originalSelection = new TreeSelection(
//            new TreePath[] { model.findElement("5"), model.findElement("5.1"), model.findElement("6") });
		TreeSelection originalSelection = new TreeSelection(model.findElement("5.1.1")); //$NON-NLS-1$
		fViewer.setSelection(originalSelection);
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

		// Run this test ten times as we've seen intermittent failures related
		// to timing in it.
		for (int i = 0; i < 10; i++) {
			// Update the model
			model.removeElementChild(TreePath.EMPTY, 0);

			// Note: Re-expanding nodes causes redundant updates.
			fListener.reset(false, false);
			fListener.addUpdates(getInternalViewer(), TreePath.EMPTY, model.getRootElement(), -1, ALL_UPDATES_COMPLETE);
			model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
			waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

			// Validate data
			model.validateData(fViewer, TreePath.EMPTY, true);
			assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
			assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

			// Update the model again
			model.addElementChild(TreePath.EMPTY, null, 0, new TestElement(model, "1", new TestElement[0])); //$NON-NLS-1$

			// Note: Re-expanding nodes causes redundant updates.
			fListener.reset(false, false);
			fListener.addUpdates(getInternalViewer(), TreePath.EMPTY, model.getRootElement(), -1, ALL_UPDATES_COMPLETE);
			model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
			waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

			// Validate data
			model.validateData(fViewer, TreePath.EMPTY, true);
			assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
			assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
			assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );
		}
	}

	@Test
	public void testPreserveLargeModelOnContent() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(100);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset();

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);
		waitWhile(t -> !fListener.isFinished(CONTENT_SEQUENCE_COMPLETE), createListenerErrorMessage());

		expandAlternateElements(fListener, model, false);

		// Set a selection in view
		TreeSelection originalSelection = new TreeSelection(model.findElement("5.1.1")); //$NON-NLS-1$
		fViewer.setSelection(originalSelection);
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

		// Update the model
		model.removeElementChild(TreePath.EMPTY, 0);

		// Note: Re-expanding nodes causes redundant updates.
		fListener.reset(false, false);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(CONTENT_SEQUENCE_COMPLETE), createListenerErrorMessage());

		// Validate data
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

		// Update the model again
		model.addElementChild(TreePath.EMPTY, null, 0, new TestElement(model, "1", new TestElement[0])); //$NON-NLS-1$

		// Note: Re-expanding nodes causes redundant updates.
		fListener.reset(false, false);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(CONTENT_SEQUENCE_COMPLETE), createListenerErrorMessage());

		// Validate data
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );
	}

	/**
	 * This test verifies that if the model selects a new element following a
	 * content refresh, the state restore logic will not override the selection
	 * requested by the model.
	 */
	@Test
	public void testPreserveSelectionDeltaAfterContent() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = TestModel.simpleMultiLevel();

		// Expand all
		fViewer.setAutoExpandLevel(-1);

		// Create the listener.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		// Set a selection in view
		fViewer.setSelection(new TreeSelection(model.findElement("3.1.1"))); //$NON-NLS-1$

		// Reset the listener (ignore redundant updates)
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Refresh content.
		// Note: Wait only for the processing of the delta, not for all updates
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Update the viewer with new selection delta to something new in the view
		ModelDelta selectDelta = model.makeElementDelta(model.findElement("2.1"), IModelDelta.SELECT); //$NON-NLS-1$

		// Wait for the second model delta to process
		fListener.resetModelChanged();
		model.postDelta(selectDelta);
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Wait for all the updates to complete (note: we're not resetting the listener.
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

		// Check to make sure that the state restore didn't change the selection.
		assertEquals(new TreeSelection(model.findElement("2.1")), fViewer.getSelection()); //$NON-NLS-1$
	}

	@Test
	public void testPreserveCollapseDeltaAfterContent() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = TestModel.simpleMultiLevel();

		// Expand all
		fViewer.setAutoExpandLevel(-1);

		// Create the listener.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		// Turn off auto-expand
		fViewer.setAutoExpandLevel(0);

		// Reset the listener (ignore redundant updates)
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Refresh content.
		// Note: Wait only for the processing of the delta, not for all updates
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Update the viewer to collapse an element
		ModelDelta collapseDelta = model.makeElementDelta(model.findElement("3.1"), IModelDelta.COLLAPSE); //$NON-NLS-1$

		// Remove updates for the collapsed element from listener, because they
		// will never happen if the element remains collapsed.
		fListener.resetModelChanged();
		fListener.removeLabelUpdate(model.findElement("3.1.1")); //$NON-NLS-1$
		fListener.removeLabelUpdate(model.findElement("3.1.2")); //$NON-NLS-1$
		fListener.removeLabelUpdate(model.findElement("3.1.3")); //$NON-NLS-1$
		fListener.removeHasChildrenUpdate(model.findElement("3.1.1")); //$NON-NLS-1$
		fListener.removeHasChildrenUpdate(model.findElement("3.1.2")); //$NON-NLS-1$
		fListener.removeHasChildrenUpdate(model.findElement("3.1.3")); //$NON-NLS-1$
		fListener.removeChildreCountUpdate(model.findElement("3.1")); //$NON-NLS-1$
		fListener.removeChildrenUpdate(model.findElement("3.1"), 0); //$NON-NLS-1$
		fListener.removeChildrenUpdate(model.findElement("3.1"), 1); //$NON-NLS-1$
		fListener.removeChildrenUpdate(model.findElement("3.1"), 2); //$NON-NLS-1$

		// Wait for the second model delta to process
		model.postDelta(collapseDelta);
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Wait for all the updates to complete (note: we're not resetting the listener.
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

		// Check to make sure that the state restore didn't change the selection.
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == false); //$NON-NLS-1$
	}

	@Test
	public void testPreserveExpandDeltaAfterContent() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = TestModel.simpleMultiLevel();

		// Note: Do not auto-expand!

		// Create the listener.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		// Reset the listener (ignore redundant updates)
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, false, false);

		// Refresh content.
		// Note: Wait only for the processing of the delta, not for all updates
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Update the viewer to expand an element
		ModelDelta expandDelta = model.makeElementDelta(model.findElement("3.1"), IModelDelta.EXPAND); //$NON-NLS-1$

		// Wait for the second model delta to process
		fListener.resetModelChanged();
		model.postDelta(expandDelta);
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Wait for all the updates to complete (note: we're not resetting the listener.
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

		// Check to make sure that the state restore didn't change the selection.
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
	}

	@Test
	public void testSaveAndRestore1() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(6);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		// Expand some, but not all elements
		expandAlternateElements(fListener, model, true);

		// Set a selection in view
		fViewer.setSelection(new TreeSelection(new TreePath[] { model.findElement("5.1"), model.findElement("5.1.1"), model.findElement("6.1.1") })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Extract the original state from viewer
		ModelDelta originalState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, originalState, IModelDelta.EXPAND | IModelDelta.SELECT);

		// Set the viewer input to null.  This will trigger the view to save the viewer state.
		fListener.reset(false, false);
		fListener.addStateUpdates(getInternalViewer(), originalState, IModelDelta.EXPAND | IModelDelta.SELECT | IModelDelta.REVEAL);
		fViewer.setInput(null);
		waitWhile(t -> !fListener.isFinished(STATE_SAVE_COMPLETE | STATE_UPDATES), createListenerErrorMessage());

		// Set the viewer input back to the model.  When view updates are complete
		// the viewer
		// Note: disable redundant updates because the reveal delta triggers one.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, false, false);
		// TODO: add state updates somehow?
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());

		// Extract the restored state from viewer
		ModelDelta restoredState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, restoredState, IModelDelta.EXPAND | IModelDelta.SELECT);

		if (!deltaMatches(originalState, restoredState)) {
			fail("Expected:\n" + originalState + "\nGot:\n" + restoredState); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Test
	public void testSaveAndRestore2() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = TestModel.simpleMultiLevel();

		// expand all elements
		fViewer.setAutoExpandLevel(-1);

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		// Set a selection in view
		fViewer.setSelection(new TreeSelection(new TreePath[] { model.findElement("3.2"), model.findElement("3.2.1"), model.findElement("2") })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		fViewer.setSelection(new TreeSelection(model.findElement("3.2.3"))); //$NON-NLS-1$

		// Turn off the auto-expand now since we want to text the auto-expand logic
		fViewer.setAutoExpandLevel(-1);

		// Extract the original state from viewer
		ModelDelta originalState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, originalState, IModelDelta.EXPAND | IModelDelta.SELECT);

		// Set the viewer input to null.  This will trigger the view to save the viewer state.
		fListener.reset(true, false);
		fListener.addStateUpdates(getInternalViewer(), originalState, IModelDelta.EXPAND | IModelDelta.SELECT | IModelDelta.REVEAL);

		fViewer.setInput(null);
		waitWhile(t -> !fListener.isFinished(STATE_SAVE_COMPLETE | STATE_UPDATES), createListenerErrorMessage());

		// Set the viewer input back to the model.  When view updates are complete
		// the viewer
		// Note: disable redundant updates because the reveal delta triggers one.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, false, false);
		// TODO: add state updates somehow?
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());

		// Extract the restored state from viewer
		ModelDelta restoredState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, restoredState, IModelDelta.EXPAND | IModelDelta.SELECT);

		if (!deltaMatches(originalState, restoredState)) {
			fail("Expected:\n" + originalState + "\nGot:\n" + restoredState); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Test
	public void testSaveAndRestoreInputInstance() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(6);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		// Expand some, but not all elements
		expandAlternateElements(fListener, model, true);

		// Set a selection in view
		fViewer.setSelection(new TreeSelection(new TreePath[] { model.findElement("5.1"), model.findElement("5.1.1"), model.findElement("6.1.1") })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Extract the original state from viewer
		ModelDelta originalState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, originalState, IModelDelta.EXPAND | IModelDelta.SELECT);

		// Do not reset to null, just reset input to the same object.

		// Set the viewer input back to the model.  When view updates are complete
		// the viewer
		// Note: disable redundant updates because the reveal delta triggers one.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, false, false);
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());

		// Extract the restored state from viewer
		ModelDelta restoredState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, restoredState, IModelDelta.EXPAND | IModelDelta.SELECT);

		if (!deltaMatches(originalState, restoredState)) {
			fail("Expected:\n" + originalState + "\nGot:\n" + restoredState); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Test
	public void testSaveAndRestoreInputInstanceEquals() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(6);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		// Expand some, but not all elements
		expandAlternateElements(fListener, model, true);

		// Set a selection in view
		fViewer.setSelection(new TreeSelection(new TreePath[] { model.findElement("5.1"), model.findElement("5.1.1"), model.findElement("6.1.1") })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Extract the original state from viewer
		ModelDelta originalState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, originalState, IModelDelta.EXPAND | IModelDelta.SELECT);

		// Create a copy of the input object and set it to model.
		TestElement newRoot = new TestElement(model, model.getRootElement().getID(), model.getRootElement().getChildren());
		model.setRoot(newRoot);

		// Set the viewer input back to the model.  When view updates are complete
		// the viewer
		// Note: disable redundant updates because the reveal delta triggers one.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, false, false);

		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());

		// Extract the restored state from viewer
		ModelDelta restoredState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, restoredState, IModelDelta.EXPAND | IModelDelta.SELECT);

		if (!deltaMatches(originalState, restoredState)) {
			fail("Expected:\n" + originalState + "\nGot:\n" + restoredState); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Test
	public void testSaveAndRestoreLarge() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(100);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset();

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);
		waitWhile(t -> !fListener.isFinished(CONTENT_SEQUENCE_COMPLETE), createListenerErrorMessage());

		expandAlternateElements(fListener, model, false);

		// Set a selection in view
		TreeSelection originalSelection = new TreeSelection(model.findElement("5.1.1")); //$NON-NLS-1$
		fViewer.setSelection(originalSelection);
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

		// Extract the original state from viewer
		ModelDelta originalState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, originalState, IModelDelta.EXPAND | IModelDelta.SELECT);

		// Set the viewer input to null.  This will trigger the view to save the viewer state.
		fListener.reset();
		fListener.addStateUpdates(getInternalViewer(), originalState, IModelDelta.EXPAND | IModelDelta.SELECT | IModelDelta.REVEAL);

		fViewer.setInput(null);
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);
		waitWhile(t -> !fListener.isFinished(STATE_SAVE_COMPLETE | STATE_UPDATES), createListenerErrorMessage());

		// Set the viewer input back to the model.  When view updates are complete
		// the viewer
		// Note: disable redundant updates because the reveal delta triggers one.
		fListener.reset();
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);
		waitWhile(t -> !fListener.isFinished(CONTENT_SEQUENCE_COMPLETE), createListenerErrorMessage());

		// Validate data (only select visible elements).
		assertTrue(getInternalViewer().getExpandedState(model.findElement("1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("1.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );
	}

	/**
	 * This test saves state of a large tree. Then the tree is modified to
	 * contain much fewer elements. The restore logic should discard the rest of
	 * the saved state delta once all the elements are visible.
	 */
	@Test
	public void testSaveAndRestorePartialStateLarge() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = alternatingSubsreesModel(100);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset();

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);
		waitWhile(t -> !fListener.isFinished(CONTENT_SEQUENCE_COMPLETE), createListenerErrorMessage());

		expandAlternateElements(fListener, model, false);

		// Set a selection in view
		TreeSelection originalSelection = new TreeSelection(model.findElement("5.1.1")); //$NON-NLS-1$
		fViewer.setSelection(originalSelection);
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

		// Extract the original state from viewer
		ModelDelta originalState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, originalState, IModelDelta.EXPAND | IModelDelta.SELECT);

		// Set the viewer input to null.  This will trigger the view to save the viewer state.
		fListener.reset();
		fListener.addStateUpdates(getInternalViewer(), originalState, IModelDelta.EXPAND | IModelDelta.SELECT | IModelDelta.REVEAL);

		fViewer.setInput(null);
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);
		waitWhile(t -> !fListener.isFinished(STATE_SAVE_COMPLETE | STATE_UPDATES), createListenerErrorMessage());


		TestElement[] elements = model.getRootElement().getChildren();
		TestElement[] newElements = new TestElement[10];
		System.arraycopy(elements, 0, newElements, 0, newElements.length);
		model.setElementChildren(TreePath.EMPTY, newElements);

		// Set the viewer input back to the model.  When view updates are complete
		// the viewer
		// Note: disable redundant updates because the reveal delta triggers one.
		fListener.reset();
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);

		// MONITOR FOR THE STATE RESTORE TO COMPLETE
		waitWhile(t -> !fListener.isFinished(CONTENT_SEQUENCE_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

		// Validate data
		assertTrue(getInternalViewer().getExpandedState(model.findElement("1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("1.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
		assertTrue( areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );
	}

	@Test
	public void testPreserveCollapseAndSelectDeltaAfterSaveAndRestore() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = TestModel.simpleMultiLevel();

		// Expand all
		fViewer.setAutoExpandLevel(-1);

		// Create the listener.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		fViewer.setSelection(new TreeSelection(model.findElement("3"))); //$NON-NLS-1$

		// Turn off auto-expand
		fViewer.setAutoExpandLevel(0);

		// Set the viewer input to null.  This will trigger the view to save the viewer state.
		fListener.reset(false, false);
		fViewer.setInput(null);
		waitWhile(t -> !fListener.isFinished(STATE_SAVE_COMPLETE), createListenerErrorMessage());

		// Set the viewer input back to the model.  When view updates are complete
		// the viewer
		// Note: disable redundant updates because the reveal delta triggers one.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, false, false);
		fViewer.setInput(model.getRootElement());
		TreePath path = model.findElement("2"); //$NON-NLS-1$
		fListener.addUpdates(null, path, (TestElement)path.getLastSegment(), 0, STATE_UPDATES);
		path = model.findElement("3"); //$NON-NLS-1$
		fListener.addUpdates(null, path, (TestElement)path.getLastSegment(), 0, STATE_UPDATES);

		// Wait till we restore state of elements we want to collapse and select
		// Bug 372619 - Need to wait until proxy installed delta is processed before
		// posting the next delta.
		waitWhile(t -> !fListener.isFinished(STATE_RESTORE_STARTED | STATE_UPDATES | CHILDREN_UPDATES | MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Post first collapse delta
		fListener.resetModelChanged();
		model.postDelta(model.makeElementDelta(model.findElement("2"), IModelDelta.COLLAPSE)); //$NON-NLS-1$
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Post second collapse delta
		fListener.resetModelChanged();
		model.postDelta(model.makeElementDelta(model.findElement("3"), IModelDelta.COLLAPSE)); //$NON-NLS-1$
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Post select delta
		model.postDelta(model.makeElementDelta(model.findElement("1"), IModelDelta.SELECT)); //$NON-NLS-1$
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Wait for all the updates to complete (note: we're not resetting the listener).
		waitWhile(t -> !fListener.isFinished(STATE_RESTORE_COMPLETE), createListenerErrorMessage());

		// Check to make sure that the state restore didn't change the selection.
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == false); //$NON-NLS-1$
		assertEquals(new TreeSelection(model.findElement("1")), fViewer.getSelection()); //$NON-NLS-1$
	}

	/**
	 * Test for bug 359859.<br>
	 * This test verifies that RESTORE state is handled after SAVE previous
	 * state was completed
	 */
	@Test
	public void testSaveRestoreOrder() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = TestModel.simpleMultiLevel();
		model.setDelayUpdates(true);

		// Expand all
		fViewer.setAutoExpandLevel(-1);

		// Create the listener.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		// a new similar model
		TestModel copyModel = TestModel.simpleMultiLevel();

		// Trigger save - restore sequence.
		fListener.reset();
		fListener.expectRestoreAfterSaveComplete();
		fViewer.setInput(copyModel.getRootElement());
		waitWhile(t -> !fListener.isFinished(STATE_RESTORE_STARTED), createListenerErrorMessage());
		assertTrue("RESTORE started before SAVE to complete", fListener.isFinished(STATE_SAVE_COMPLETE)); //$NON-NLS-1$
	}

	/**
	 * This test tries to restore a viewer state while input == null. See: Bug
	 * 380288 - NPE switching to the Breakpoints View
	 */
	@Test
	public void testUpdateWithNullInput() throws Exception {
		TestModel model = TestModel.simpleMultiLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		ModelDelta expandedState = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		fViewer.saveElementState(TreePath.EMPTY, expandedState, IModelDelta.EXPAND);

		// Refresh the viewer so that updates are generated.
		fListener.reset();
		fListener.addChildreCountUpdate(TreePath.EMPTY);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));

		// Wait for the delta to be processed.
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CHILD_COUNT_UPDATES), createListenerErrorMessage());

		fViewer.setInput(null);
		fViewer.updateViewer(expandedState);

		waitWhile(t -> !fListener.isFinished(CONTENT_COMPLETE | VIEWER_UPDATES_RUNNING), createListenerErrorMessage());

	}
}
