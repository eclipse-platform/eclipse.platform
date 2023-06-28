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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.function.Function;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentationFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests to verify that the viewer properly handles initial columns width.
 */
public class ColumnPresentationTests extends AbstractDebugTest implements ITestModelUpdatesListenerConstants {
	private Display fDisplay;
	private Shell fShell;
	private TreeModelViewer fViewer;
	private TestModelUpdatesListener fListener;
	private boolean fResized = false;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		createViewer();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		destroyViewer();
		super.tearDown();
	}

	void createViewer() {
		fDisplay = PlatformUI.getWorkbench().getDisplay();
		fShell = new Shell(fDisplay);
		fShell.setSize(800, 600);
		fShell.setLayout(new FillLayout());
		fViewer = new TreeModelViewer(fShell, SWT.VIRTUAL, new PresentationContext("TestViewer")); //$NON-NLS-1$
		fViewer.getTree().addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				fResized = true;
			}

			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
		fListener = new TestModelUpdatesListener(fViewer, false, false);
		fShell.open();
		TestUtil.processUIEvents();
	}

	void destroyViewer() throws Exception {
		fListener.dispose();
		fViewer.getPresentationContext().dispose();
		// Close the shell.
		fShell.close();
		TestUtil.processUIEvents();
	}

	void recreateViewer() throws Exception {
		destroyViewer();
		createViewer();
	}

	static class MyColumnPresentation implements IColumnPresentation {
		private IPresentationContext fContext;
		protected String[] columnIds;

		MyColumnPresentation() {
			this (makeDefaultColumnIds());
		}

		static String[] makeDefaultColumnIds() {
			String[] columnIds = new String[5];
			for (int i = 0; i < columnIds.length; i++) {
				columnIds[i] = "ColumnId_" + i; //$NON-NLS-1$
			}
			return columnIds;
		}

		MyColumnPresentation(String[] columnIds) {
			this.columnIds = columnIds;
		}

		@Override
		public void init(IPresentationContext context) {
			fContext = context;
		}

		@Override
		public void dispose() {
			fContext = null;
		}

		@Override
		public ImageDescriptor getImageDescriptor(String id) {
			return null;
		}

		protected IPresentationContext getPresentationContext() {
			return fContext;
		}

		@Override
		public String[] getAvailableColumns() {
			return columnIds;
		}

		@Override
		public String[] getInitialColumns() {
			return columnIds;
		}

		@Override
		public String getHeader(String id) {
			if (Arrays.asList(columnIds).indexOf(id) != -1) {
				return id;
			}
			return null;
		}

		@Override
		public String getId() {
			return "MyColumnPresentationID"; //$NON-NLS-1$
		}

		@Override
		public boolean isOptional() {
			return true;
		}

	}

	static class MyColumnPresentation2 extends MyColumnPresentation implements IColumnPresentation2 {
		int[] repliedWidths;

		MyColumnPresentation2() {
			super();
			for (int i = 0; i < columnIds.length; i++) {
				columnIds[i] += "_2"; //$NON-NLS-1$
			}
			repliedWidths = new int[columnIds.length];
			Arrays.fill(repliedWidths, -2);
		}

		@Override
		public int getInitialColumnWidth(String id, int treeWidgetWidth,
				String[] visibleColumnIds) {
			for (int i = 0; i < columnIds.length; i++) {
				if (id.equals(columnIds[i]) && i == 0) {
					repliedWidths[i] = 2 * (treeWidgetWidth / visibleColumnIds.length);
					return repliedWidths[i];
				}
				if (id.equals(columnIds[i]) && (i == 1 || i == 2)) {
					repliedWidths[i] = (treeWidgetWidth / visibleColumnIds.length) / 2;
					return repliedWidths[i];
				}
				if (id.equals(columnIds[i]) && i == 3) {
					repliedWidths[i] = (treeWidgetWidth / visibleColumnIds.length);
					return repliedWidths[i];
				}
				if (id.equals(columnIds[i]) && i == 4) {
					repliedWidths[i] = -1;
					return repliedWidths[i];
				}
			}
			return -1;
		}

		@Override
		public String getId() {
			return "MyColumnPresentation2ID"; //$NON-NLS-1$
		}
	}

	static class MyModel extends TestModel implements IColumnPresentationFactory {
		MyColumnPresentation colPresenation;

		MyModel(MyColumnPresentation cp1) {
			colPresenation = cp1;
		}

		@Override
		public IColumnPresentation createColumnPresentation(
				IPresentationContext context, Object element) {
			if (colPresenation != null) {
				return colPresenation;
			}
			return null;
		}

		@Override
		public String getColumnPresentationId(IPresentationContext context,
				Object element) {
			if (colPresenation != null) {
				return colPresenation.getId();
			}
			return null;
		}

	}

	private TestModel makeModel(MyColumnPresentation cp, String rootSufffix) throws Exception {
		MyModel model = new MyModel(cp);
		model.setRoot(new TestElement(model, "root" + rootSufffix, new TestElement[] { //$NON-NLS-1$
		new TestElement(model, "1", true, true, new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "2", true, false, new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "3", false, true, new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "4", false, false, new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "5", new TestElement[0]), //$NON-NLS-1$
		new TestElement(model, "6", new TestElement[0]) })); //$NON-NLS-1$
		fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);
		fViewer.setInput(model.getRootElement());
		waitWhile(t -> !fListener.isFinished(), createListenerErrorMessage());
		model.validateData(fViewer, TreePath.EMPTY);
		return model;
	}

	/**
	 * In this test: verify that tree viewer can handle IColumnPresentation
	 * implementation without requiring the presentation being
	 * IColumnPresentation2 (i.e. ensure we do not break backward compatibility
	 * of interface). Also, we verify that the initial columns width is the
	 * average of tree width / number of visible columns, which is the logic in
	 * InternalTreeModelViewer.
	 */
	@Test
	public void testInitialColumnAverageWidth() throws Exception {
		fResized = false;

		MyColumnPresentation colPre = new MyColumnPresentation();
		makeModel(colPre, "m1"); //$NON-NLS-1$
		Tree tree = fViewer.getTree();
		TreeColumn[] columns = tree.getColumns();
		int treeWidth = tree.getSize().x;
		int avgWidth = treeWidth / columns.length;

		// Resizing the tree invalidates the test.
		if (fResized) {
			return;
		}

		for (int i = 0; i < columns.length - 1; i++) {
			assertEquals(avgWidth, columns[i].getWidth());
		}
		// Checking of the width of the last column is not reliable.
		// I.e. it's handled differenty on different platforms.
		//int remainder = treeWidth % columns.length;
		//assertEquals(avgWidth + remainder, columns[columns.length - 1].getWidth());
	}

	/**
	 * In this test: verify that tree viewer can handle IColumnPresentation2.
	 * Also, we verify that the initial columns width is the width computed by
	 * the IColumnPresentation2 implementation.
	 */
	@Test
	public void testInitialColumnWidth() throws Exception {
		fResized = false;

		MyColumnPresentation2 colPre = new MyColumnPresentation2();
		makeModel(colPre, "m2"); //$NON-NLS-1$
		Tree tree = fViewer.getTree();
		TreeColumn[] columns = tree.getColumns();

		// Resizing the tree invalidates the test.
		if (fResized) {
			return;
		}

		for (int i = 0; i < columns.length; i++) {
			int width = colPre.repliedWidths[i];
			if (width != -1) {
				assertEquals(width, columns[i].getWidth());
			}
		}
	}

	/**
	 * In this test: verify that tree viewer can handle IColumnPresentation2.
	 * Also, we verify that the initial columns width from IColumnPresentation2
	 * is not used when there are user settings inside the viewer which are
	 * created from user resizing columns.
	 */
	@Test
	public void testRespectUserSettings() throws Exception {
		MyColumnPresentation2 colPre = new MyColumnPresentation2();
		makeModel(colPre, "m2"); //$NON-NLS-1$
		TreeColumn[] columns = fViewer.getTree().getColumns();
		// simulate user resizing each column width
		int[] newWidths = new int[columns.length];
		for (int i = 0; i < columns.length; i++) {
			newWidths[i] = columns[i].getWidth() + 10;
			columns[i].setWidth(newWidths[i]);
		}
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}
		// get InternalTreeModelViewer to rebuild columns due to change of
		// model and presentation - first set to another model and column
		// presentation, then switch to a model with original presentation.
		makeModel(new MyColumnPresentation(), "m1"); //$NON-NLS-1$
		makeModel(colPre, "m3"); //$NON-NLS-1$
		// verify user resized widths are used instead of the initial widths from IColumnPresentation2
		columns = fViewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}

		// get InternalTreeModelViewer to rebuild columns due to hide and show columns
		fViewer.setShowColumns(false);
		TestUtil.processUIEvents();
		waitWhile(t -> fViewer.getTree().getColumns().length > 0, createColumnsErrorMessage());
		fViewer.setShowColumns(true);
		TestUtil.processUIEvents();
		waitWhile(t -> fViewer.getTree().getColumns().length != newWidths.length, createColumnsErrorMessage());
		// verify user resized widths are used instead of the initial widths from IColumnPresentation2
		columns = fViewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}
	}

	/**
	 * In this test: verify that tree viewer can handle IColumnPresentation2.
	 * Also, we verify that the initial columns width from IColumnPresentation2
	 * is not used when there are user settings inside the viewer which are
	 * restored from memento, e.g., restoring workspace, etc.
	 */
	@Test
	public void testRespectMemento() throws Exception {
		MyColumnPresentation2 colPre = new MyColumnPresentation2();
		makeModel(colPre, "m2"); //$NON-NLS-1$
		TreeColumn[] columns = fViewer.getTree().getColumns();
		// simulate user resizing each column width
		int[] newWidths = new int[columns.length];
		for (int i = 0; i < columns.length; i++) {
			newWidths[i] = columns[i].getWidth() + 10;
			columns[i].setWidth(newWidths[i]);
		}
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}
		// save memento
		final XMLMemento memento = XMLMemento.createWriteRoot("TEST"); //$NON-NLS-1$
		fViewer.saveState(memento);
		// throw away any settings inside the viewer and create a new viewer
		// with memento settings, this is the same effect resulted from closing
		// and opening workspace again.
		recreateViewer();
		fViewer.initState(memento);
		// get InternalTreeModelViewer to rebuild columns
		makeModel(colPre, "m2"); //$NON-NLS-1$
		// verify widths from memento are used instead of the initial widths from IColumnPresentation2
		columns = fViewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newWidths[i], columns[i].getWidth());
		}
	}

	/**
	 * In this test: verify that tree viewer can handle the column presentation
	 * changing its available column IDs between runs (bug 360015).
	 */
	@Test
	public void testChangedColumnIds() throws Exception {
		MyColumnPresentation colPre = new MyColumnPresentation();

		makeModel(colPre, "m1"); //$NON-NLS-1$
		TreeColumn[] columns = fViewer.getTree().getColumns();
		// Select visible columns
		fViewer.setVisibleColumns(new String[] { colPre.columnIds[0] });
		TestUtil.processUIEvents();
		waitWhile(t -> fViewer.getTree().getColumns().length != 1, createColumnsErrorMessage());

		// get InternalTreeModelViewer to rebuild columns due to change of
		// model and presentation - first set to another model and column
		// presentation, then switch to a model with original presentation.
		makeModel(new MyColumnPresentation2(), "m2"); //$NON-NLS-1$

		String[] newColumnIds = MyColumnPresentation.makeDefaultColumnIds();
		newColumnIds[0] = "new_column_id"; //$NON-NLS-1$
		colPre = new MyColumnPresentation(newColumnIds);

		makeModel(colPre, "m3"); //$NON-NLS-1$

		// verify user resized widths are used instead of the initial widths from IColumnPresentation2
		columns = fViewer.getTree().getColumns();
		for (int i = 0; i < columns.length; i++) {
			assertEquals(newColumnIds[i], columns[i].getText());
		}
	}

	private Function<AbstractDebugTest, String> createColumnsErrorMessage() {
		return t -> "Unexpected columns number: " + fViewer.getTree().getColumns().length;
	}

	private Function<AbstractDebugTest, String> createListenerErrorMessage() {
		return t -> "Listener not finished: " + fListener;
	}
}
