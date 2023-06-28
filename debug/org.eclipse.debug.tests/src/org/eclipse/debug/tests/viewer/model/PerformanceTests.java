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
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests to measure the performance of the viewer updates.
 */
abstract public class PerformanceTests extends AbstractViewerModelTest implements ITestModelUpdatesListenerConstants {

	protected VisibleVirtualItemValidator fVirtualItemValidator;

	public String getDefaultScenarioId() {
		return this.getClass().getName() + '#' + name.getMethodName() + "()"; //$NON-NLS-1$
	}

	@Override
	protected TestModelUpdatesListener createListener(IInternalTreeModelViewer viewer) {
		return new TestModelUpdatesListener(viewer, false, false);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fVirtualItemValidator = new VisibleVirtualItemValidator(0, Integer.MAX_VALUE);
	}

	/**
	 * Depth (size) of the test model to be used in the tests.  This number allows
	 * the jface based tests to use a small enough model to fit on the screen, and
	 * for the virtual viewer to exercise the content provider to a greater extent.
	 */
	abstract protected int getTestModelDepth();

	@Test
	public void testRefreshStruct() throws Exception {
		TestModel model = new TestModel();
		model.setRoot(new TestElement(model, "root", new TestElement[0])); //$NON-NLS-1$
		model.setElementChildren(TreePath.EMPTY, TestModel.makeMultiLevelElements(model, getTestModelDepth(), "model.")); //$NON-NLS-1$

		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		Performance perf = Performance.getDefault();
		PerformanceMeter meter = perf.createPerformanceMeter(getDefaultScenarioId());
		try {
			for (int i = 0; i < 10; i++) {
				// Update the model
				model.setAllAppendix(" - pass " + i); //$NON-NLS-1$

				TestElement element = model.getRootElement();
				fListener.reset(TreePath.EMPTY, element, -1, false, false);

				meter.start();
				model.postDelta(new ModelDelta(element, IModelDelta.CONTENT));
				waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
				meter.stop();
				System.gc();
			}

			meter.commit();
			perf.assertPerformance(meter);
		} finally {
			meter.dispose();
		}
	}

	@Test
	public void testRefreshStruct2() throws Exception {
		TestModel model = new TestModel();
		model.setRoot(new TestElement(model, "root", new TestElement[0])); //$NON-NLS-1$
		model.setElementChildren(TreePath.EMPTY, TestModel.makeMultiLevelElements2(model, new int[] { 2, 3000, 1 }, "model.")); //$NON-NLS-1$

		fViewer.setAutoExpandLevel(2);
		// Create the listener
		//fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);
		fListener.reset();

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE), createListenerErrorMessage());

		fVirtualItemValidator.setVisibleRange(0, 50);

		Performance perf = Performance.getDefault();
		PerformanceMeter meter = perf.createPerformanceMeter(getDefaultScenarioId());
		try {
			for (int i = 0; i < 100; i++) {
				// Update the model
				model.setAllAppendix(" - pass " + i); //$NON-NLS-1$

				TestElement element = model.getRootElement();
				//fListener.reset(TreePath.EMPTY, element, -1, false, false);
				fListener.reset();

				meter.start();
				model.postDelta(new ModelDelta(element, IModelDelta.CONTENT));
				waitWhile(t -> !fListener.isFinished(MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
				model.postDelta(new ModelDelta(element, IModelDelta.CONTENT));
				waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
				meter.stop();
				System.gc();
			}

			meter.commit();
			perf.assertPerformance(meter);
		} finally {
			meter.dispose();
		}
	}

	@Test
	public void testRefreshStructReplaceElements() throws Exception {
		TestModel model = new TestModel();
		model.setRoot(new TestElement(model, "root", new TestElement[0])); //$NON-NLS-1$
		model.setElementChildren(TreePath.EMPTY, TestModel.makeMultiLevelElements(model, getTestModelDepth(), "model.")); //$NON-NLS-1$

		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		Performance perf = Performance.getDefault();
		PerformanceMeter meter = perf.createPerformanceMeter(getDefaultScenarioId());
		try {
			for (int i = 0; i < 100; i++) {
				// Update the model
				model.setElementChildren(TreePath.EMPTY, TestModel.makeMultiLevelElements(model, getTestModelDepth(), "pass " + i + ".")); //$NON-NLS-1$ //$NON-NLS-2$

				TestElement element = model.getRootElement();
				fListener.reset(TreePath.EMPTY, element, -1, false, false);

				meter.start();
				model.postDelta(new ModelDelta(element, IModelDelta.CONTENT));
				waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
				meter.stop();
				System.gc();
			}

			meter.commit();
			perf.assertPerformance(meter);
		} finally {
			meter.dispose();
		}
	}

	@Test
	public void testRefreshList() throws Exception {
		TestModel model = new TestModel();
		model.setRoot(new TestElement(model, "root", new TestElement[0])); //$NON-NLS-1$
		int numElements = (int)Math.pow(2, getTestModelDepth());
		model.setElementChildren(TreePath.EMPTY, TestModel.makeSingleLevelModelElements(model, numElements, "model.")); //$NON-NLS-1$

		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		Performance perf = Performance.getDefault();
		PerformanceMeter meter = perf.createPerformanceMeter(getDefaultScenarioId());
		try {
			for (int i = 0; i < 100; i++) {
				// Update the model
				model.setAllAppendix(" - pass " + i); //$NON-NLS-1$

				TestElement element = model.getRootElement();
				fListener.reset(TreePath.EMPTY, element, -1, false, false);

				meter.start();
				model.postDelta(new ModelDelta(element, IModelDelta.CONTENT));
				waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
				meter.stop();
				System.gc();
			}

			meter.commit();
			perf.assertPerformance(meter);
		} finally {
			meter.dispose();
		}
	}

	@Test
	public void testSaveAndRestore() throws Exception {
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
		fViewer.setSelection(new TreeSelection(model.findElement("3.2.3"))); //$NON-NLS-1$

		// Turn off the auto-expand now since we want to text the auto-expand logic
		fViewer.setAutoExpandLevel(-1);

		Performance perf = Performance.getDefault();
		PerformanceMeter meter = perf.createPerformanceMeter(getDefaultScenarioId());
		try {
			for (int i = 0; i < 100; i++) {
				// Update the model
				model.setAllAppendix(" - pass " + i); //$NON-NLS-1$

				// Set the viewer input to null.  This will trigger the view to save the viewer state.
				fListener.reset(true, false);

				meter.start();
				fViewer.setInput(null);
				waitWhile(t -> !fListener.isFinished(STATE_SAVE_COMPLETE), createListenerErrorMessage());

				// Set the viewer input back to the model.  When view updates are complete
				// the viewer
				// Note: disable redundant updates because the reveal delta triggers one.
				fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, false, false);
				// TODO: add state updates somehow?
				fViewer.setInput(model.getRootElement());
				waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | STATE_RESTORE_COMPLETE), createListenerErrorMessage());
				meter.stop();
				System.gc();
			}

			meter.commit();
			perf.assertPerformance(meter);
		} finally {
			meter.dispose();
		}

	}

	@Test
	public void testRefreshListFiltered() throws Exception {
		TestModel model = new TestModel();
		model.setRoot(new TestElement(model, "root", new TestElement[0])); //$NON-NLS-1$
		int numElements = (int)Math.pow(2, getTestModelDepth());
		model.setElementChildren(TreePath.EMPTY, TestModel.makeSingleLevelModelElements(model, numElements, "model.")); //$NON-NLS-1$

		fViewer.setAutoExpandLevel(-1);

		// Create the listener
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

		fViewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof TestElement) {
					String id = ((TestElement)element).getID();
					if (id.startsWith("model.")) { //$NON-NLS-1$
						id = id.substring("model.".length()); //$NON-NLS-1$
					}
					if (id.length() >= 2 && (id.charAt(1) == '1' || id.charAt(1) == '3' || id.charAt(1) == '5' || id.charAt(1) == '7' || id.charAt(1) == '9')) {
						return false;
					}
				}
				return true;
			}
		});

		// Set the input into the view and update the view.
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);

		Performance perf = Performance.getDefault();
		PerformanceMeter meter = perf.createPerformanceMeter(getDefaultScenarioId());
		try {
			for (int i = 0; i < 100; i++) {
				// Update the model
				model.setAllAppendix(" - pass " + i); //$NON-NLS-1$

				TestElement element = model.getRootElement();
				fListener.reset(TreePath.EMPTY, element, -1, false, false);

				meter.start();
				model.postDelta(new ModelDelta(element, IModelDelta.CONTENT));
				waitWhile(t -> !fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE), createListenerErrorMessage());
				meter.stop();
				System.gc();
			}

			meter.commit();
			perf.assertPerformance(meter);
		} finally {
			meter.dispose();
		}
	}

}
