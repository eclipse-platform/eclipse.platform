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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ICheckUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.jface.viewers.TreePath;
import org.junit.Test;

/**
 * Tests that verify that the viewer property retrieves all the content
 * from the model.
 *
 * @since 3.6
 */
abstract public class ContentTests extends AbstractViewerModelTest implements ITestModelUpdatesListenerConstants {

	@Override
	protected TestModelUpdatesListener createListener(IInternalTreeModelViewer viewer) {
		return new TestModelUpdatesListener(viewer, true, true);
	}

	@Test
	public void testSimpleSingleLevel() throws Exception {
		// Create the model with test data
		TestModel model = TestModel.simpleSingleLevel();

		// Make sure that all elements are expanded
		fViewer.setAutoExpandLevel(-1);

		// Create the agent which forces the tree to populate
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);

		// Create the listener which determines when the view is finished updating.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, true);

		// Set the viewer input (and trigger updates).
		fViewer.setInput(model.getRootElement());

		// Wait for the updates to complete.
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());

		model.validateData(fViewer, TreePath.EMPTY);

		assertTrue( fListener.checkCoalesced(TreePath.EMPTY, 0, 6) );
	}

	@Test
	public void testSimpleMultiLevel() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);

		TestModel model = TestModel.simpleMultiLevel();
		fViewer.setAutoExpandLevel(-1);

		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, true);

		fViewer.setInput(model.getRootElement());

		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());

		model.validateData(fViewer, TreePath.EMPTY);

		assertTrue( fListener.checkCoalesced(TreePath.EMPTY, 0, 3) );
	}

	/**
	 * Modified test model that optionally captures (i.e. doesn't compete)
	 * udpates after filling in their data.
	 */
	static class TestModelWithCapturedUpdates extends TestModel {

		boolean fCaptureLabelUpdates = false;
		boolean fCaptureChildrenUpdates = false;

		List<IViewerUpdate> fCapturedUpdates = Collections.synchronizedList(new ArrayList<IViewerUpdate>());

		@Override
		public void update(IChildrenUpdate[] updates) {
			for (int i = 0; i < updates.length; i++) {
				TestElement element = (TestElement)updates[i].getElement();
				int endOffset = updates[i].getOffset() + updates[i].getLength();
				for (int j = updates[i].getOffset(); j < endOffset; j++) {
					if (j < element.getChildren().length) {
						updates[i].setChild(element.getChildren()[j], j);
					}
				}
				if (fCaptureChildrenUpdates) {
					fCapturedUpdates.add(updates[i]);
				} else {
					updates[i].done();
				}
			}
		}

		@Override
		public void update(ILabelUpdate[] updates) {
			for (int i = 0; i < updates.length; i++) {
				TestElement element = (TestElement)updates[i].getElement();
				updates[i].setLabel(element.getLabel(), 0);
				if (updates[i] instanceof ICheckUpdate &&
					Boolean.TRUE.equals(updates[i].getPresentationContext().getProperty(ICheckUpdate.PROP_CHECK)))
				{
					((ICheckUpdate)updates[i]).setChecked(element.getChecked(), element.getGrayed());
				}
				if (fCaptureLabelUpdates) {
					fCapturedUpdates.add(updates[i]);
				} else {
					updates[i].done();
				}
			}
		}
	}

	/**
	 * Test to make sure that label provider cancels stale updates and doesn't
	 * use data from stale updates to populate the viewer.<br>
	 * See bug 210027
	 */
	@Test
	public void testLabelUpdatesCompletedOutOfSequence1() throws Exception {
		TestModelWithCapturedUpdates model = new TestModelWithCapturedUpdates();
		model.fCaptureLabelUpdates = true;

		model.setRoot(new TestElement(model, "root", new TestElement[] { //$NON-NLS-1$
		new TestElement(model, "1", new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "2", new TestElement[0]), //$NON-NLS-1$
		}) );

		// Set input into the view to update it, but block children updates.
		// Wait for view to start retrieving content.
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);
		waitWhile(t -> model.fCapturedUpdates.size() < model.getRootElement().fChildren.length, createModelErrorMessage(model));

		List<IViewerUpdate> firstUpdates = model.fCapturedUpdates;
		model.fCapturedUpdates = Collections.synchronizedList(new ArrayList<IViewerUpdate>(2));

//      // Change the model and run another update set.
		model.getElement(model.findElement("1")).setLabelAppendix(" - changed"); //$NON-NLS-1$ //$NON-NLS-2$
		model.getElement(model.findElement("2")).setLabelAppendix(" - changed"); //$NON-NLS-1$ //$NON-NLS-2$
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> model.fCapturedUpdates.size() < model.getRootElement().fChildren.length, createModelErrorMessage(model));

		// Complete the second set of children updates
		for (int i = 0; i < model.fCapturedUpdates.size(); i++) {
			((ILabelUpdate)model.fCapturedUpdates.get(i)).done();
		}

		// Then complete the first set.
		for (int i = 0; i < firstUpdates.size(); i++) {
			ILabelUpdate capturedUpdate = (ILabelUpdate)firstUpdates.get(i);
			assertTrue(capturedUpdate.isCanceled());
			capturedUpdate.done();
		}

		waitWhile(t -> !fListener.isFinished(CHILDREN_UPDATES), createListenerErrorMessage());

		// Check viewer data
		model.validateData(fViewer, TreePath.EMPTY);
	}

	private Function<AbstractDebugTest, String> createModelErrorMessage(TestModelWithCapturedUpdates model) {
		return t -> "Unxexpected model state: captured updates: " + model.fCapturedUpdates + ", root children: " + Arrays.toString(model.getRootElement().fChildren);
	}

	/**
	 * Test to make sure that label provider cancels stale updates and doesn't
	 * use data from stale updates to populate the viewer.<br>
	 * This version of the test changes the elements in the view, and not just
	 * the elements' labels. In this case, the view should still cancel stale
	 * updates.<br>
	 * See bug 210027
	 */
	@Test
	public void testLabelUpdatesCompletedOutOfSequence2() throws Exception {
		TestModelWithCapturedUpdates model = new TestModelWithCapturedUpdates();
		model.fCaptureLabelUpdates = true;

		model.setRoot(new TestElement(model, "root", new TestElement[] { //$NON-NLS-1$
		new TestElement(model, "1", new TestElement[0]), //$NON-NLS-1$
				new TestElement(model, "2", new TestElement[0]), //$NON-NLS-1$
		}) );

		// Set input into the view to update it, but block children updates.
		// Wait for view to start retrieving content.
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);
		waitWhile(t -> model.fCapturedUpdates.size() < model.getRootElement().fChildren.length, createModelErrorMessage(model));
		List<IViewerUpdate> firstUpdates = model.fCapturedUpdates;
		model.fCapturedUpdates = Collections.synchronizedList(new ArrayList<IViewerUpdate>(2));

		// Change the model and run another update set.
		model.setElementChildren(TreePath.EMPTY, new TestElement[] {
				new TestElement(model, "1-new", new TestElement[0]), //$NON-NLS-1$
				new TestElement(model, "2-new", new TestElement[0]), //$NON-NLS-1$
		});
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> model.fCapturedUpdates.size() < model.getRootElement().fChildren.length, createModelErrorMessage(model));

		// Complete the second set of children updates
		for (int i = 0; i < model.fCapturedUpdates.size(); i++) {
			((ILabelUpdate)model.fCapturedUpdates.get(i)).done();
		}

		// Then complete the first set.
		for (int i = 0; i < firstUpdates.size(); i++) {
			ILabelUpdate capturedUpdate = (ILabelUpdate)firstUpdates.get(i);
			assertTrue(capturedUpdate.isCanceled());
			capturedUpdate.done();
		}

		waitWhile(t -> !fListener.isFinished(CHILDREN_UPDATES), createListenerErrorMessage());

		// Check viewer data
		model.validateData(fViewer, TreePath.EMPTY);
	}

	/**
	 * Test to make sure that content provider cancels stale updates and doesn't
	 * use data from stale updates to populate the viewer.<br>
	 * Note: this test is disabled because currently the viewer will not issue
	 * a new update for an until the previous update is completed.  This is even
	 * if the previous update is canceled.  If this behavior is changed at some
	 * point, then this test should be re-enabled.<br>
	 * See bug 210027
	 */
	public void _x_testChildrenUpdatesCompletedOutOfSequence() throws Exception {
		TestModelWithCapturedUpdates model = new TestModelWithCapturedUpdates();
		model.fCaptureChildrenUpdates = true;

		model.setRoot(new TestElement(model, "root", new TestElement[] { //$NON-NLS-1$
		new TestElement(model, "1", new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "2", new TestElement[0]), //$NON-NLS-1$
		}) );

		// Set input into the view to update it, but block children updates.
		// Wait for view to start retrieving content.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !areCapturedChildrenUpdatesComplete(model.fCapturedUpdates, model.getRootElement().fChildren.length), createModelErrorMessage(model));
		IChildrenUpdate[] firstUpdates = model.fCapturedUpdates.toArray(new IChildrenUpdate[0]);
		model.fCapturedUpdates.clear();

		// Change the model and run another update set.
		model.setElementChildren(TreePath.EMPTY, new TestElement[] {
 new TestElement(model, "1-new", new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "2-new", new TestElement[0]), //$NON-NLS-1$
		});
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !areCapturedChildrenUpdatesComplete(model.fCapturedUpdates, model.getRootElement().fChildren.length), createModelErrorMessage(model));

		// Complete the second set of children updates
		for (int i = 0; i < model.fCapturedUpdates.size(); i++) {
			((IChildrenUpdate)model.fCapturedUpdates.get(i)).done();
		}

		// Then complete the first set.
		for (int i = 0; i < firstUpdates.length; i++) {
			firstUpdates[i].done();
		}

		waitWhile(t -> !fListener.isFinished(CHILDREN_UPDATES), createListenerErrorMessage());

		// Check viewer data
		model.validateData(fViewer, TreePath.EMPTY);
	}

	private boolean areCapturedChildrenUpdatesComplete(List<IViewerUpdate> capturedUpdates, int childCount) {
		List<Integer> expectedChildren = new ArrayList<>();
		for (int i = 0; i < childCount; i++) {
			expectedChildren.add(Integer.valueOf(i));
		}
		IChildrenUpdate[] updates = capturedUpdates.toArray(new IChildrenUpdate[0]);
		for (int i = 0; i < updates.length; i++) {
			for (int j = 0; j < updates[i].getLength(); j++) {
				expectedChildren.remove( Integer.valueOf(updates[i].getOffset() + j) );
			}
		}
		return expectedChildren.isEmpty();
	}
}
