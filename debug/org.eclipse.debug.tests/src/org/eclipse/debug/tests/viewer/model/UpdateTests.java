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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.jface.viewers.TreePath;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests to verify that the viewer property updates following changes in the
 * model, following simple content update deltas from the model.
 *
 * @since 3.6
 */
abstract public class UpdateTests extends AbstractViewerModelTest implements ITestModelUpdatesListenerConstants {

	@Override
	protected TestModelUpdatesListener createListener(IInternalTreeModelViewer viewer) {
		return new TestModelUpdatesListener(viewer, false, false);
	}

	/**
	 * This test: - creates a simple model - replaces the list of elements with
	 * a shorter list of elements - refreshes the viewer
	 */
	@Test
	public void testRemoveElements() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		// Update the model
		TestElement root = model.getRootElement();
		TreePath rootPath = new TreePath(new Object[] {});
		TestElement[] newElements = new TestElement[] {
 new TestElement(model, "1", new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "2", new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "3", new TestElement[0]), //$NON-NLS-1$
		};
		model.setElementChildren(rootPath, newElements);

		// Reset the listener to NOT fail on redundant updates.
		// When elements are remvoed from the model and the model is
		// refreshed the viewer will issue an IChildrenUpdate for the
		// missing elements as an optimization.
		fListener.reset(rootPath, root, -1, false, false);

		model.postDelta(new ModelDelta(root, IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);
	}

	/**
	 * This test: - creates a simple model - sets a list of children to one of
	 * the elements - refreshes the viewer
	 */
	@Test
	public void testAddNewChildren() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		// Update the model
		TestElement element = model.getRootElement().getChildren()[0];
		TreePath elementPath = new TreePath(new Object[] { element });
		TestElement[] newChildren = new TestElement[] {
 new TestElement(model, "1.1", new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "1.2", new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "1.3", new TestElement[0]), //$NON-NLS-1$
		};
		model.setElementChildren(elementPath, newChildren);

		// Reset the viewer to ignore redundant updates.  The '1' element
		// will be updated for "hasChildren" before it is expanded, which is
		// expected.
		TreePath rootPath = TreePath.EMPTY;
		TestElement rootElement = model.getRootElement();
		fListener.reset(rootPath, rootElement, -1, false, false);

		// Refresh the viewer
		model.postDelta(new ModelDelta(rootElement, IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);
	}


	private void removeElement(TestModel model, int index, boolean validate) throws Exception {
		ModelDelta delta = model.removeElementChild(TreePath.EMPTY, index);

		// Remove delta should generate no new updates, but we still need to wait for the event to
		// be processed.
		fListener.reset();
		model.postDelta(delta);
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		if (validate) {
			model.validateData(fViewer, TreePath.EMPTY);
		}
	}

	private void addElement(TestModel model, String label, int position, boolean validate) throws Exception {
		ModelDelta delta = model.addElementChild(TreePath.EMPTY, null, position, new TestElement(model, label, new TestElement[0]));

		// Remove delta should generate no new updates, but we still need to wait for the event to
		// be processed.
		fListener.reset();
		model.postDelta(delta);

		if (validate) {
			waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CONTENT_SEQUENCE_COMPLETE | LABEL_SEQUENCE_COMPLETE), createListenerErrorMessage());
			model.validateData(fViewer, TreePath.EMPTY);
		} else {
			waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
		}
	}

	private void insertElement(TestModel model, String label, int position, boolean validate) throws Exception {
		ModelDelta delta = model.insertElementChild(TreePath.EMPTY, position, new TestElement(model, label, new TestElement[0]));

		// Remove delta should generate no new updates, but we still need to wait for the event to
		// be processed.
		fListener.reset();
		model.postDelta(delta);

		if (validate) {
			waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CONTENT_SEQUENCE_COMPLETE | LABEL_SEQUENCE_COMPLETE), createListenerErrorMessage());
			model.validateData(fViewer, TreePath.EMPTY);
		} else {
			waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
		}
	}

	@Test
	public void testRepeatedAddRemoveElement() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);

		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		// Update the model
		removeElement(model, 2, true);
		addElement(model, "3-new", 3, true); //$NON-NLS-1$
		removeElement(model, 4, true);
		addElement(model, "5-new", 5, true); //$NON-NLS-1$
		removeElement(model, 1, true);
		addElement(model, "1-new", 1, true); //$NON-NLS-1$
		removeElement(model, 3, true);
		addElement(model, "4-new", 4, true); //$NON-NLS-1$
	}

	/**
	 * This test verifies that when the viewer processes a delta that causes
	 * viewer updates it initiates the model update sequence before it finishes
	 * processing the delta.
	 */
	@Test
	public void testNotifyUpdatesTartedOnModelChanged() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		// Refresh the viewer so that updates are generated.
		fListener.reset();
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));

		// Wait for the delta to be processed.
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		Assert.assertTrue( fListener.isFinished(CONTENT_SEQUENCE_STARTED) );
	}


	/**
	 * This test case attempts to create a race condition between processing of
	 * the content updates and processing of add/remove model deltas. <br>
	 * See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=304066">bug
	 * 304066</a>
	 */
	@Test
	public void testContentPlusAddRemoveUpdateRaceConditionsElement() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		// Create a listener to listen only to a children count update for the root.
		TestModelUpdatesListener childrenCountUpdateListener = new TestModelUpdatesListener(fViewer, false, false);

		for (int i = 0; i < 10; i++) {
			String pass = "pass #" + i; //$NON-NLS-1$

			// Request a content update for view
			childrenCountUpdateListener.reset();
			childrenCountUpdateListener.addChildreCountUpdate(TreePath.EMPTY);
			model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
			// Wait until the delta is processed
			waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

			removeElement(model, 5, false);
			removeElement(model, 4, false);
			removeElement(model, 3, false);
			removeElement(model, 2, false);
			removeElement(model, 1, false);
			removeElement(model, 0, false);

			// Wait until the children count update is completed using the count from
			// before elements were removed.
			waitWhile(t -> !childrenCountUpdateListener.isFinished(CHILD_COUNT_UPDATES), createListenerErrorMessage());

			insertElement(model, "1 - " + pass, 0, false); //$NON-NLS-1$
			insertElement(model, "2 - " + pass, 1, false); //$NON-NLS-1$
			insertElement(model, "3 - " + pass, 2, false); //$NON-NLS-1$
			insertElement(model, "4 - " + pass, 3, false); //$NON-NLS-1$
			insertElement(model, "5 - " + pass, 4, false); //$NON-NLS-1$
			insertElement(model, "6 - " + pass, 5, false); //$NON-NLS-1$

			waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
			model.validateData(fViewer, TreePath.EMPTY);

		}

		childrenCountUpdateListener.dispose();
	}


	/**
	 * This test case attempts to create a race condition between processing of
	 * the content updates and processing of add/remove model deltas. <br>
	 * See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=304066">bug
	 * 304066</a>
	 */
	@Test
	public void testInsertAtInvalidIndex() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		// Insert element at the end of the list.
		final int insertIndex = model.getRootElement().getChildren().length;
		ModelDelta delta = model.insertElementChild(TreePath.EMPTY, insertIndex, new TestElement(model, "last - invalid index", new TestElement[0])); //$NON-NLS-1$
		// Change insert index to out of range
		delta.accept((visitorDelta, depth) -> {
			if ((visitorDelta.getFlags() & IModelDelta.INSERTED) != 0) {
				((ModelDelta) visitorDelta).setIndex(insertIndex + 1);
				return false;
			}
			return true;
		});

		// Remove delta should generate no new updates, but we still need to wait for the event to
		// be processed.
		fListener.reset();
		model.postDelta(delta);

		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CONTENT_SEQUENCE_COMPLETE | LABEL_SEQUENCE_COMPLETE), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);
	}

	/**
	 * This test forces the viewer to reschedule pending content updates due to
	 * a remove event from the model.
	 *
	 * @see org.eclipse.debug.internal.ui.viewers.model.ModelContentProvider#rescheduleUpdates
	 */
	@Test
	public void testRescheduleUpdates() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		for (int i = 0; i < 5; i++) {
			// Refresh the viewer so that updates are generated.
			TestElement rootElement = model.getRootElement();
			fListener.reset();
			fListener.addUpdates(TreePath.EMPTY, model.getRootElement(), 1, CHILD_COUNT_UPDATES);
			model.postDelta(new ModelDelta(rootElement, IModelDelta.CONTENT));

			// Wait for the delta to be processed.
			waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CHILD_COUNT_UPDATES | CHILDREN_UPDATES_STARTED), createListenerErrorMessage());

			// Update the model
			removeElement(model, 0, true);
			addElement(model, "1", 0, true); //$NON-NLS-1$
		}
	}

	/**
	 * This test forces the viewer to cancel updates then process them at once.
	 * <p>
	 * - Wait until CHILDREN COUNT update started then refresh<br>
	 * - Process queued updates in order.<br>
	 * </p>
	 */
	@Test
	public void testCanceledUpdates1() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);


		model.setQeueueingUpdate(true);

		for (int i = 0; i < 5; i++) {
			// Refresh the viewer so that updates are generated.
			fListener.reset();
			model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));

			// Wait for the delta to be processed.
			waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CHILD_COUNT_UPDATES_STARTED), createListenerErrorMessage());

		}

		model.setQeueueingUpdate(false);
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

	}

	/**
	 * This test forces the viewer to cancel updates then process them at once.
	 * <p>
	 * - Wait until CHILDREN COUNT update started then refresh<br>
	 * - Process queued updates in REVERSE order.<br>
	 * </p>
	 */
	@Test
	public void testCanceledUpdates2() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);


		model.setQeueueingUpdate(true);

		for (int i = 0; i < 5; i++) {
			// Refresh the viewer so that updates are generated.
			fListener.reset();
			model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));

			// Wait for the delta to be processed.
			waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CHILD_COUNT_UPDATES_STARTED), createListenerErrorMessage());

		}

		ArrayList<IViewerUpdate> updates = new ArrayList<>(model.getQueuedUpdates());
		model.getQueuedUpdates().clear();
		for (int i = updates.size() - 1; i >= 0; i--) {
			model.processUpdate(updates.get(i));
		}

		model.setQeueueingUpdate(false);
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());
	}

	/**
	 * This test forces the viewer to cancel updates then process them at once.
	 * <p>
	 * - Wait until CHILDREN update started then refresh<br>
	 * - Process queued updates in order.<br>
	 * </p>
	 */
	@Test
	public void testCanceledUpdates3() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);


		model.setQeueueingUpdate(true);

		for (int i = 0; i < 5; i++) {
			// Refresh the viewer so that updates are generated.
			fListener.reset();
			model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));

			long start = System.currentTimeMillis();
			// Wait for the delta to be processed.
			while (!fListener.isFinished(MODEL_CHANGED_COMPLETE | CHILDREN_UPDATES_STARTED)
					&& System.currentTimeMillis() - start < testTimeout) {
				completeQueuedUpdatesOfType(model, IChildrenCountUpdate.class);
				completeQueuedUpdatesOfType(model, IHasChildrenUpdate.class);
				TestUtil.processUIEvents();
			}
		}

		model.setQeueueingUpdate(false);
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

	}

	/**
	 * This test forces the viewer to cancel updates then process them at once.
	 * <p>
	 * - Wait until CHILDREN update started then refresh<br>
	 * - Process queued updates in REVERSE order.<br>
	 * </p>
	 */
	@Test
	public void testCanceledUpdates4() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);


		model.setQeueueingUpdate(true);

		for (int i = 0; i < 5; i++) {
			// Refresh the viewer so that updates are generated.
			fListener.reset();
			model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));

			long start = System.currentTimeMillis();
			// Wait for the delta to be processed.
			while (!fListener.isFinished(MODEL_CHANGED_COMPLETE | CHILDREN_UPDATES_STARTED)
					&& System.currentTimeMillis() - start < testTimeout) {
				completeQueuedUpdatesOfType(model, IChildrenCountUpdate.class);
				completeQueuedUpdatesOfType(model, IHasChildrenUpdate.class);
				TestUtil.processUIEvents();
			}

		}

		ArrayList<IViewerUpdate> updates = new ArrayList<>(model.getQueuedUpdates());
		model.getQueuedUpdates().clear();
		for (int i = updates.size() - 1; i >= 0; i--) {
			model.processUpdate(updates.get(i));
		}

		model.setQeueueingUpdate(false);
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());
	}

	/**
	 * This test removes an element while there are updates running on its
	 * sub-tree. With a precise timing this operation caused Bug 373790.
	 * <p>
	 * See Bug 373790 - Debug view stays busy after Resume
	 * </p>
	 *
	 * @see org.eclipse.debug.internal.ui.viewers.model.ModelContentProvider#rescheduleUpdates
	 */
	@Test
	public void testCancelUpdatesOnRemoveElementWhileUpdatingSubTree() throws Exception {
		TestModel model = TestModel.simpleMultiLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		// Refresh the viewer so that updates are generated.
		fListener.reset();
		TreePath path = model.findElement("2"); //$NON-NLS-1$
		fListener.addUpdates(path, model.getElement(path), 1, CHILD_COUNT_UPDATES);
		fListener.addChildreUpdate(path, 0);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));

		// Wait for the delta to be processed and child updates for "2" to get started.
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CHILD_COUNT_UPDATES | CHILDREN_UPDATES_RUNNING), createListenerErrorMessage());

		// Remove element "2"
		removeElement(model, 1, true);

		// Wait for all updates to finish.
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());
	}

	/**
	 * This test forces the viewer to cancel updates upon setInput().
	 * <p>
	 * - Wait until CHILDREN update started then refresh<br>
	 * - Process queued updates in order.<br>
	 * </p>
	 */
	@Test
	public void testCanceledUpdatesOnSetInput() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		model.setQeueueingUpdate(false);

		// Refresh the viewer so that updates are generated.
		fListener.reset();
		fListener.addChildreCountUpdate(TreePath.EMPTY);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));

		// Wait for the delta to be processed.
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CHILD_COUNT_UPDATES), createListenerErrorMessage());

		TestModel model2 = new TestModel();
		model2.setRoot(new TestElement(model2, "root", new TestElement[0])); //$NON-NLS-1$
		fViewer.setInput(model2.getRootElement());

		waitWhile(t -> !fListener.isFinished(CONTENT_COMPLETE | VIEWER_UPDATES_RUNNING), createListenerErrorMessage());

	}

	/**
	 * This test forces the viewer to cancel updates upon setInput().
	 * <p>
	 * - Wait until CHILDREN update started then refresh<br>
	 * - Process queued updates in order.<br>
	 * </p>
	 */
	@Test
	public void testCanceledUpdatesOnSetNullInput() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		model.setQeueueingUpdate(false);

		// Refresh the viewer so that updates are generated.
		fListener.reset();
		fListener.addChildreCountUpdate(TreePath.EMPTY);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));

		// Wait for the delta to be processed.
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE | CHILD_COUNT_UPDATES), createListenerErrorMessage());

		fViewer.setInput(null);

		waitWhile(t -> !fListener.isFinished(CONTENT_COMPLETE | VIEWER_UPDATES_RUNNING), createListenerErrorMessage());

	}

	private void completeQueuedUpdatesOfType(TestModel model, Class<?> updateClass) {
		List<IViewerUpdate> updatesToComplete = new LinkedList<>();

		for (Iterator<IViewerUpdate> itr = model.getQueuedUpdates().iterator(); itr.hasNext();) {
			IViewerUpdate update = itr.next();
			if (updateClass.isInstance(update)) {
				updatesToComplete.add(update);
				itr.remove();
			}
		}
		if (updatesToComplete != null) {
			for (Iterator<IViewerUpdate> itr = updatesToComplete.iterator(); itr.hasNext();) {
				model.processUpdate(itr.next());
			}
		}
	}

}
