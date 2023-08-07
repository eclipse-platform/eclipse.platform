/*******************************************************************************
 * Copyright (c) 2009, 2013 Wind River Systems and others.
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

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.jface.viewers.TreePath;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests which verify the check box support.  This test is very similar to the
 * content test except that the extending class should create a viewer with
 * the SWT.CHECK style enabled. <br>
 * Most of the  check box verification is performed in the test model.
 *
 * @since 3.6
 */
abstract public class CheckTests extends AbstractViewerModelTest {

	@Override
	protected TestModelUpdatesListener createListener(IInternalTreeModelViewer viewer) {
		return new TestModelUpdatesListener(viewer, false, false);
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
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the viewer input (and trigger updates).
		fViewer.setInput(model.getRootElement());

		// Wait for the updates to complete.
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());

		model.validateData(fViewer, TreePath.EMPTY);
	}

	@Test
	public void testSimpleMultiLevel() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);

		TestModel model = TestModel.simpleMultiLevel();
		fViewer.setAutoExpandLevel(-1);

		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		fViewer.setInput(model.getRootElement());

		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());

		model.validateData(fViewer, TreePath.EMPTY);
	}

	@Test
	public void testCheckReceiver() throws Exception {
		// Initial setup
		TestModel model = TestModel.simpleSingleLevel();
		fViewer.setAutoExpandLevel(-1);
		// TreeModelViewerAutopopulateAgent autopopulateAgent = new
		// TreeModelViewerAutopopulateAgent(fViewer);
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);
		fViewer.setInput(model.getRootElement());

		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		TestElement element = model.getRootElement().getChildren()[0];
		boolean initialCheckState = element.getChecked();
		TreePath elementPath = new TreePath(new Object[] { element });
		ModelDelta delta = model.setElementChecked(elementPath, true, false);

		fListener.reset(elementPath, element, -1, true, false);
		model.postDelta(delta);

		waitWhile(t -> !fListener.isFinished(ITestModelUpdatesListenerConstants.MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		Assert.assertTrue(element.getChecked() != initialCheckState);
	}

	@Test
	public void testUpdateCheck() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);

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
		ModelDelta delta = model.setElementChecked(elementPath, false, false);

		fListener.reset(elementPath, element, -1, true, false);
		model.postDelta(delta);
		waitWhile(t -> !fListener.isFinished(ITestModelUpdatesListenerConstants.LABEL_COMPLETE | ITestModelUpdatesListenerConstants.MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);
	}
}
