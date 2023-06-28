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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.junit.Test;

/**
 * Tests to verify that the viewer properly handles selection changes.
 */
abstract public class SelectionTests extends AbstractViewerModelTest implements ITestModelUpdatesListenerConstants {

	@Override
	protected TestModelUpdatesListener createListener(IInternalTreeModelViewer viewer) {
		return new TestModelUpdatesListener(viewer, false, false);
	}

	private static class SelectionListener implements ISelectionChangedListener {
		private final List<SelectionChangedEvent> fEvents = new ArrayList<>(1);

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			fEvents.add(event);
		}
	}

	private TestModel makeMultiLevelModel() throws Exception {
		TestModel model = TestModel.simpleMultiLevel();
		fViewer.setAutoExpandLevel(-1);
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);
		return model;
	}

	/**
	 * In this test: - set selection to an element deep in the model - verify
	 * that selection chagned listener is called - verify that the selection is
	 * in the viewer is correct
	 */
	@Test
	public void testSimpleSetSelection() throws Exception {
		// Create the model and populate the view.
		TestModel model = makeMultiLevelModel();

		// Create a selection object to the deepest part of the tree.
		SelectionListener listener = new SelectionListener();
		fViewer.addSelectionChangedListener(listener);

		// Set the selection and verify that the listener is called.
		TreeSelection selection = new TreeSelection(model.findElement("3.3.3")); //$NON-NLS-1$
		fViewer.setSelection(selection, true, false);
		assertTrue(listener.fEvents.size() == 1);

		// Check that the new selection is what was requested.
		ISelection viewerSelection = fViewer.getSelection();
		assertEquals(selection, viewerSelection);
	}

	/**
	 * In this test verify that selection policy can prevent selection from
	 * being set and verify that a FORCE flag can override the selection policy.
	 */
	@Test
	public void testSelectionPolicy() throws Exception {
		// Create the model and populate the view.
		final TestModel model = makeMultiLevelModel();

		// Set the selection and verify it.
		TreeSelection selection_3_3_3 = new TreeSelection(model.findElement("3.3.3")); //$NON-NLS-1$
		fViewer.setSelection(selection_3_3_3, true, false);
		assertEquals(selection_3_3_3, fViewer.getSelection());

		model.setSelectionPolicy(new IModelSelectionPolicy() {

			@Override
			public ISelection replaceInvalidSelection(ISelection invalidSelection, ISelection newSelection) {
				return null;
			}

			@Override
			public boolean overrides(ISelection existing, ISelection candidate, IPresentationContext context) {
				return false;
			}

			@Override
			public boolean isSticky(ISelection selection, IPresentationContext context) {
				return true;
			}

			@Override
			public boolean contains(ISelection selection, IPresentationContext context) {
				return true;
			}
		});

		// Attempt to change selection and verify that old selection is still valid.
		TreeSelection selection_3_3_1 = new TreeSelection(model.findElement("3.3.1")); //$NON-NLS-1$
		fViewer.setSelection(selection_3_3_1, true, false);
		assertEquals(selection_3_3_3, fViewer.getSelection());

		// Now attempt to *force* selection and verify that new selection was set.
		fViewer.setSelection(selection_3_3_1, true, true);
		assertEquals(selection_3_3_1, fViewer.getSelection());

		// Create the an update delta to attempt to change selection back to
		// 3.3.3 and verify that selection did not get overriden.
		TreePath path_3_3_3 = model.findElement("3.3.3"); //$NON-NLS-1$
		ModelDelta baseDelta = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE);
		ModelDelta delta_3_3_3 = model.getElementDelta(baseDelta, path_3_3_3, false);
		delta_3_3_3.setFlags(IModelDelta.SELECT);
		fViewer.updateViewer(baseDelta);
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
		assertEquals(selection_3_3_1, fViewer.getSelection());

		// Add the *force* flag to the selection delta and update viewer again.
		// Verify that selection did change.
		delta_3_3_3.setFlags(IModelDelta.SELECT | IModelDelta.FORCE);
		fViewer.updateViewer(baseDelta);
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
		assertEquals(selection_3_3_3, fViewer.getSelection());
	}


	/**
	 * In this test: - set a seleciton to an element - then remove that element
	 * - update the view with remove delta -> The selection should be re-set to
	 * empty.
	 */
	@Test
	public void testSelectRemove() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);

		// Create the model and populate the view.
		TestModel model = makeMultiLevelModel();

		// Create a selection object to the deepest part of the tree.
		TreePath elementPath = model.findElement("3.3.3"); //$NON-NLS-1$
		TreeSelection selection = new TreeSelection(elementPath);

		// Set the selection.
		fViewer.setSelection(selection, true, false);

		// Remove the element
		TreePath removePath = model.findElement("3"); //$NON-NLS-1$
		TreePath parentPath = removePath.getParentPath();
		int removeIndex = model.getElement(parentPath).indexOf( model.getElement(removePath) );
		ModelDelta delta = model.removeElementChild(removePath.getParentPath(), removeIndex);

		// Configure a selection listener
		SelectionListener listener = new SelectionListener();
		fViewer.addSelectionChangedListener(listener);

		// Reset the listener and update the viewer.  With a remove
		// delta only wait for the delta to be processed.
		fListener.reset();
		model.postDelta(delta);
		waitWhile(t -> !fListener.isFinished(ITestModelUpdatesListenerConstants.MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Check to make sure the selection was made
		//assertTrue(listener.fEvents.size() == 1);

		// Check that the new selection is empty
		ISelection viewerSelection = fViewer.getSelection();
		assertTrue(viewerSelection.isEmpty());
	}


	/**
	 * In this test: - set a selection to an element - then remove that element
	 * - then refresh the view. -> The selection should be re-set to empty.
	 */
	@Test
	public void testSelectRemoveRefreshStruct() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);

		// Create the model and populate the view.
		TestModel model = makeMultiLevelModel();

		// Create a selection object to the deepest part of the tree.
		TreePath elementPath = model.findElement("3.3.3"); //$NON-NLS-1$
		TreeSelection selection = new TreeSelection(elementPath);

		// Set the selection.
		fViewer.setSelection(selection, true, false);

		// Remove the element
		TreePath removePath = model.findElement("3"); //$NON-NLS-1$
		TreePath parentPath = removePath.getParentPath();
		int removeIndex = model.getElement(parentPath).indexOf( model.getElement(removePath) );
		model.removeElementChild(removePath.getParentPath(), removeIndex);

		// Configure a selection listener
		SelectionListener listener = new SelectionListener();
		fViewer.addSelectionChangedListener(listener);

		// Reset the listener to ignore redundant updates.  When elements are removed
		// the viewer may still request updates for those elements.
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

		// Refresh the viewer
		model.postDelta( new ModelDelta(model.getRootElement(), IModelDelta.CONTENT) );
		waitWhile(t -> !fListener.isFinished(ITestModelUpdatesListenerConstants.ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		// Check to make sure the selection was made
		// Commented out until JFace bug 219887 is fixed.
		//assertTrue(listener.fEvents.size() == 1);

		// Check that the new selection is empty
		ISelection viewerSelection = fViewer.getSelection();
		assertTrue(viewerSelection.isEmpty());
	}
}
