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

import java.util.regex.Pattern;

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewerFilter;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.junit.Test;

/**
 * Tests that verify that the viewer property retrieves all the content
 * from the model.
 *
 * @since 3.8
 */
abstract public class FilterTests extends AbstractViewerModelTest implements ITestModelUpdatesListenerConstants {

	@Override
	protected TestModelUpdatesListener createListener(IInternalTreeModelViewer viewer) {
		return new TestModelUpdatesListener(viewer, true, true);
	}

	protected IInternalTreeModelViewer getInternalViewer() {
		return fViewer;
	}

	static class TestViewerFilter extends ViewerFilter {

		Pattern fPattern;
		TestViewerFilter(String pattern) {
			fPattern = Pattern.compile(pattern);
		}


		 @Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof TestElement) {
				TestElement te = (TestElement)element;
				return !fPattern.matcher(te.getLabel()).find();
			}

			return true;
		}
	}

	static class TestTMVFilter extends TreeModelViewerFilter {
		Pattern fPattern;
		Object fParentElement;
		TestTMVFilter(String pattern, Object parentElement) {
			fPattern = Pattern.compile(pattern);
			fParentElement = parentElement;
		}

		@Override
		public boolean isApplicable(ITreeModelViewer viewer, Object parentElement) {
			if (fParentElement != null) {
				return fParentElement.equals(parentElement);
			}

			return true;
		}

		 @Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof TestElement) {
				TestElement te = (TestElement)element;
				return !fPattern.matcher(te.getLabel()).find();
			}

			return true;
		}
	}

	@Test
	public void testSimpleSingleLevel() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		doTestSimpleLevel(model, new ViewerFilter[] { new TestViewerFilter("2") }); //$NON-NLS-1$
	}

	@Test
	public void testSimpleSingleLevelWithTMVFilter() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		doTestSimpleLevel(model, new ViewerFilter[] { new TestTMVFilter("2", model.getRootElement()) }); //$NON-NLS-1$
	}

	@Test
	public void testSimpleSingleLevelWithMixedFilters() throws Exception {
		TestModel model = TestModel.simpleSingleLevel();
		doTestSimpleLevel(model, new ViewerFilter[] { new TestTMVFilter("2", model.getRootElement()), new TestViewerFilter("1") }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSimpleMultiLevel() throws Exception {
		TestModel model = TestModel.simpleMultiLevel();
		doTestSimpleLevel(model, new ViewerFilter[] { new TestViewerFilter(".1"), new TestViewerFilter(".2") }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSimpleMultiLevelWithTMVFilter() throws Exception {
		TestModel model = TestModel.simpleMultiLevel();
		doTestSimpleLevel(model, new ViewerFilter[] { new TestTMVFilter(".1", null), new TestTMVFilter(".2", null) }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSimpleMultiLevelWithMixedFilters() throws Exception {
		TestModel model = TestModel.simpleMultiLevel();
		doTestSimpleLevel(model, new ViewerFilter[] { new TestViewerFilter(".1"), new TestTMVFilter(".2", null) }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void doTestSimpleLevel(TestModel model, ViewerFilter[] filters) throws Exception {

		// Make sure that all elements are expanded
		fViewer.setAutoExpandLevel(-1);

		fViewer.setFilters(filters);

		// Create the listener which determines when the view is finished updating.
		// fListener.reset(TreePath.EMPTY, model.getRootElement(), filters, -1, false, false);
		fListener.reset(TreePath.EMPTY, model.getRootElement(), filters, -1, true, true);

		// Set the viewer input (and trigger updates).
		fViewer.setInput(model.getRootElement());

		// Wait for the updates to complete.
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		model.validateData(fViewer, TreePath.EMPTY, false, filters);
	}

	@Test
	public void testLargeSingleLevel() throws Exception {
		doTestLargeSingleLevel(new ViewerFilter[] { new TestViewerFilter("2") }); //$NON-NLS-1$
	}

	@Test
	public void testLargeSingleLevelWithTMVFilter() throws Exception {
		doTestLargeSingleLevel(new ViewerFilter[] { new TestTMVFilter("2", null) }); //$NON-NLS-1$
	}

	private void doTestLargeSingleLevel(ViewerFilter[] filters) throws Exception {
		TestModel model = new TestModel();
		model.setRoot(new TestElement(model, "root", new TestElement[0])); //$NON-NLS-1$
		model.setElementChildren(TreePath.EMPTY, TestModel.makeSingleLevelModelElements(model, 3000, "model.")); //$NON-NLS-1$

		// Set filters
		fViewer.setFilters(filters);

		fListener.setFailOnRedundantUpdates(false);
		//fListener.setFailOnMultipleLabelUpdateSequences(false);
		fListener.reset();

		fViewer.setInput(model.getRootElement());

		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());
	}


	/**
	 * Replace an element that is not visible but filtered out. With an element
	 * that is NOT filtered out. Fire REPLACE delta.
	 */
	@Test
	public void testReplacedUnrealizedFilteredElement() throws Exception {
		doTestReplacedUnrealizedFilteredElement(new ViewerFilter[] { new TestViewerFilter("2") }); //$NON-NLS-1$
	}


	/**
	 * Replace an element that is not visible but filtered out. With an element
	 * that is NOT filtered out. Fire REPLACE delta.
	 */
	@Test
	public void testReplacedUnrealizedFilteredElementWithTMVFilter() throws Exception {
		doTestReplacedUnrealizedFilteredElement(new ViewerFilter[] { new TestTMVFilter("2", null) }); //$NON-NLS-1$
	}

	private void doTestReplacedUnrealizedFilteredElement(ViewerFilter[] filters) throws Exception {

		// Populate a view with a large model (only first 100 elements will be visible in virtual viewer).
		TestModel model = new TestModel();
		model.setRoot(new TestElement(model, "root", new TestElement[0])); //$NON-NLS-1$
		model.setElementChildren(TreePath.EMPTY, TestModel.makeSingleLevelModelElements(model, 300, "model.")); //$NON-NLS-1$

		fViewer.setFilters(filters);

		fListener.setFailOnRedundantUpdates(false);
		fListener.reset();

		// Populate the view (all elements containing a "2" will be filtered out.
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);

		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		// Switch out element "201" which is filtered out, with a "replaced element" which should NOT be
		// filtered out.
		TestElement replacedElement = new TestElement(model, "replaced element", new TestElement[0]); //$NON-NLS-1$
		IModelDelta replaceDelta = model.replaceElementChild(TreePath.EMPTY, 200, replacedElement);
		fListener.reset();
		model.postDelta(replaceDelta);
		waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());

		// Reposition the viewer to make element 100 the top element, making the replaced element visible.
		fListener.reset();
		fViewer.reveal(TreePath.EMPTY, 150);
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		// Verify that the replaced element is in viewer now (i.e. it's not filtered out.
		TreePath[] replacedElementPaths = fViewer.getElementPaths(replacedElement);
		assertTrue(replacedElementPaths.length != 0);
	}

	@Test
	public void testRefreshUnrealizedFilteredElement() throws Exception {
		doTestRefreshUnrealizedFilteredElement(new ViewerFilter[] { new TestViewerFilter("2") }); //$NON-NLS-1$
	}

	@Test
	public void testRefreshUnrealizedFilteredElementWithTMVFilter() throws Exception {
		doTestRefreshUnrealizedFilteredElement(new ViewerFilter[] { new TestTMVFilter("2", null) }); //$NON-NLS-1$
	}

	/**
	 * Replace an element that is not visible but filtered out.  With an element that is NOT filtered out.
	 * Fire CONTENT delta on parent.
	 */
	private void doTestRefreshUnrealizedFilteredElement(ViewerFilter[] filters) throws Exception {
		// Populate a view with a large model (only first 100 elements will be visible in virtual viewer).
		TestModel model = new TestModel();
		model.setRoot(new TestElement(model, "root", new TestElement[0])); //$NON-NLS-1$
		model.setElementChildren(TreePath.EMPTY, TestModel.makeSingleLevelModelElements(model, 300, "model.")); //$NON-NLS-1$

		fViewer.setFilters(filters);

		fListener.setFailOnRedundantUpdates(false);
		fListener.reset();

		// Populate the view (all elements containing a "2" will be filtered out.
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);

		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		// Switch out element "201" which is filtered out, with a "replaced element" which should NOT be
		// filtered out.
		TestElement replacedElement = new TestElement(model, "replaced element", new TestElement[0]); //$NON-NLS-1$
		model.replaceElementChild(TreePath.EMPTY, 200, replacedElement);
		fListener.reset();
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		// Reposition the viewer to make element 100 the top element, making the replaced element visible.
		fListener.reset();
		fViewer.reveal(TreePath.EMPTY, 150);
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		// Verify that the replaced element is in viewer now (i.e. it's not filtered out.
		TreePath[] replacedElementPaths = fViewer.getElementPaths(replacedElement);
		assertTrue(replacedElementPaths.length != 0);
	}

	@Test
	public void testRefreshToUnfilterElements() throws Exception {
		doTestRefreshToUnfilterElements(new ViewerFilter[] { new TestViewerFilter(".1"), new TestViewerFilter(".2") }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testRefreshToUnfilterElementsWithTMVFilter() throws Exception {
		doTestRefreshToUnfilterElements(new ViewerFilter[] { new TestTMVFilter(".1", null), new TestTMVFilter(".2", null) }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testRefreshToUnfilterElementsWithMixedFilters() throws Exception {
		doTestRefreshToUnfilterElements(new ViewerFilter[] { new TestViewerFilter(".1"), new TestTMVFilter(".2", null) }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Replace an element that is not visible but filtered out.  With an element that is NOT filtered out.
	 * Fire CONTENT delta on parent.
	 */
	private void doTestRefreshToUnfilterElements(ViewerFilter[] filters) throws Exception {
		ViewerFilter[] filters1 = filters;
		// Populate a view with a large model (only first 100 elements will be visible in virtual viewer).
		TestModel model = TestModel.simpleMultiLevel();

		fViewer.setFilters(filters);

		fListener.setFailOnRedundantUpdates(false);
		fListener.reset();

		// Make sure that all elements are expanded
		fViewer.setAutoExpandLevel(-1);

		// Populate the view (all elements containing a "2" will be filtered out.
		fViewer.setInput(model.getRootElement());
		TestUtil.waitForJobs(name.getMethodName(), 300, 5000);

		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		// Turn off filters and refresh.
		filters1 = new ViewerFilter[0];
		fViewer.setFilters(filters1);

		fListener.reset();
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		model.validateData(fViewer, TreePath.EMPTY, false, filters1);
	}

	@Test
	public void testPreserveExpandedOnMultLevelContent() throws Exception {
		//TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
		TestModel model = StateTests.alternatingSubsreesModel(6);

		// NOTE: WE ARE NOT EXPANDING ANY CHILDREN

		// Create the listener, only check the first level
		fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY, true);

		StateTests.expandAlternateElements(fListener, model, true);

		// Set a selection in view
		// Set a selection in view
		TreeSelection originalSelection = new TreeSelection(
new TreePath[] { model.findElement("5"), model.findElement("5.1"), model.findElement("6") }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		fViewer.setSelection(originalSelection);
		assertTrue( StateTests.areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

		// Set a filter to remove element "1"
		ViewerFilter[] filters = new ViewerFilter[] { new TestViewerFilter("^1$") }; //$NON-NLS-1$
		fViewer.setFilters(filters);

		// Note: Re-expanding nodes causes redundant updates.
		fListener.reset(false, false);
		fListener.addUpdates(getInternalViewer(), TreePath.EMPTY, model.getRootElement(), filters, -1, ALL_UPDATES_COMPLETE);

		// Post the refresh delta
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

		// Validate data
		model.validateData(fViewer, TreePath.EMPTY, true, filters);
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
		assertTrue( StateTests.areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

		// Note: in past it was observed sub-optimal coalescing in this test due
		// to scattered update requests from viewer.
		assertTrue( fListener.checkCoalesced(TreePath.EMPTY, 0, 6) );

		// Clear the filter, to re-add the element
		filters = new ViewerFilter[0];
		fViewer.setFilters(filters);

		// Refresh again to get the filtered element back
		fListener.reset();
		fListener.addUpdates(getInternalViewer(), TreePath.EMPTY, model.getRootElement(), filters, -1, ALL_UPDATES_COMPLETE);
		model.postDelta(new ModelDelta(model.getRootElement(), IModelDelta.CONTENT));
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());

		// Validate data
		model.validateData(fViewer, TreePath.EMPTY, true, filters);
		assertTrue(getInternalViewer().getExpandedState(model.findElement("2")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("3.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("4")) == false); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("5.1")) == true); //$NON-NLS-1$
		assertTrue(getInternalViewer().getExpandedState(model.findElement("6")) == false); //$NON-NLS-1$
		assertTrue( StateTests.areTreeSelectionsEqual(originalSelection, (ITreeSelection)fViewer.getSelection()) );

	}

}
