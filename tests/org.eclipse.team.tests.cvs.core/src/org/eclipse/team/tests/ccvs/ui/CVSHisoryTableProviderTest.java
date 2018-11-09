/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui;

import java.util.List;

import junit.framework.Test;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.team.internal.ccvs.ui.CVSHistoryTableProvider;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class CVSHisoryTableProviderTest extends EclipseTest {

	public void testAllNegatives() {
		Display display = Display.getCurrent();
		Shell shell = new Shell(display);
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new FillLayout());

		CVSHistoryTableProvider provider = new CVSHistoryTableProvider();
		// empty settings
		TreeViewer treeViewer = provider.createTree(composite);
		Tree tree = treeViewer.getTree();
		Layout layout = tree.getLayout();

		// layout.getColumns(tree);
		Item[] items = (Item[]) ReflectionUtils.callMethod(layout,
				"getColumns", new Class[] { Composite.class },
				new Object[] { tree });
		assertEquals(6, items.length);

		// List columns = layout.columns;
		List<ColumnLayoutData> columns = (List<ColumnLayoutData>) ReflectionUtils.getField(
				layout, "columns");
		// same weight for all columns
		int weight = ((ColumnWeightData) columns.get(0)).weight;
		for (ColumnLayoutData column : columns) {
			assertTrue(column instanceof ColumnWeightData);
			ColumnWeightData c = (ColumnWeightData) column;
			assertTrue(c.weight > 0);
			assertEquals(weight, c.weight);
		}
		// layout.layout(tree, false /*ignored in TableLayout*/);
	}

	public void testAllZeros() {
		Display display = Display.getCurrent();
		Shell shell = new Shell(display);
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new FillLayout());

		CVSHistoryTableProvider provider = new CVSHistoryTableProvider();
		// provider.settings = createDialogSettings(...);
		ReflectionUtils.setField(provider, "settings",
				createDialogSettings(provider, new int[] { 0, 0, 0, 0, 0, 0 }));
		TreeViewer treeViewer = provider.createTree(composite);
		Tree tree = treeViewer.getTree();
		Layout layout = tree.getLayout();

		// layout.getColumns(tree);
		Item[] items = (Item[]) ReflectionUtils.callMethod(layout,
				"getColumns", new Class[] { Composite.class },
				new Object[] { tree });
		assertEquals(6, items.length);

		// List columns = layout.columns;
		List <ColumnLayoutData> columns = (List<ColumnLayoutData>) ReflectionUtils.getField(
				layout, "columns");
		// same weight for all columns
		int weight = ((ColumnWeightData) columns.get(0)).weight;
		for (ColumnLayoutData column : columns) {
			assertTrue(column instanceof ColumnWeightData);
			ColumnWeightData c = (ColumnWeightData) column;
			assertTrue(c.weight > 0);
			assertEquals(weight, c.weight);
		}
	}

	public void testNewBranchColumn() {
		Display display = Display.getCurrent();
		Shell shell = new Shell(display);
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new FillLayout());

		CVSHistoryTableProvider provider = new CVSHistoryTableProvider();
		// provider.settings = createDialogSettings(...);
		ReflectionUtils.setField(
				provider,
				"settings",
				createDialogSettings(provider, new int[] { 100, -1, 100, 100,
						100, 100 }));
		TreeViewer treeViewer = provider.createTree(composite);
		Tree tree = treeViewer.getTree();
		Layout layout = tree.getLayout();

		// layout.getColumns(tree);
		Item[] items = (Item[]) ReflectionUtils.callMethod(layout,
				"getColumns", new Class[] { Composite.class },
				new Object[] { tree });
		assertEquals(6, items.length);

		// List columns = layout.columns;
		List<ColumnLayoutData> columns = (List<ColumnLayoutData>) ReflectionUtils.getField(
				layout, "columns");
		for (ColumnLayoutData column : columns) {
			assertTrue(column instanceof ColumnPixelData);
			ColumnPixelData c = (ColumnPixelData) column;
			assertTrue(c.width > 0);
		}
		int branchesColumnWidth = ((ColumnPixelData) columns.get(1)).width;
		int pixels = new PixelConverter(tree)
				.convertWidthInCharsToPixels(CVSUIMessages.HistoryView_branches
						.length() + 4);
		assertEquals(pixels, branchesColumnWidth);
	}

	public void testAllPositives() {
		Display display = Display.getCurrent();
		Shell shell = new Shell(display);
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new FillLayout());

		CVSHistoryTableProvider provider = new CVSHistoryTableProvider();
		// provider.settings = createDialogSettings(...);
		ReflectionUtils.setField(
				provider,
				"settings",
				createDialogSettings(provider, new int[] { 100, 100, 100, 100,
						100, 100 }));
		TreeViewer treeViewer = provider.createTree(composite);
		Tree tree = treeViewer.getTree();
		Layout layout = tree.getLayout();

		// layout.getColumns(tree);
		Item[] items = (Item[]) ReflectionUtils.callMethod(layout,
				"getColumns", new Class[] { Composite.class },
				new Object[] { tree });
		assertEquals(6, items.length);

		// List columns = layout.columns;
		List<ColumnLayoutData> columns = (List<ColumnLayoutData>) ReflectionUtils.getField(
				layout, "columns");
		for (ColumnLayoutData column : columns) {
			assertTrue(column instanceof ColumnPixelData);
			ColumnPixelData c = (ColumnPixelData) column;
			assertEquals(100, c.width);
		}
	}

	public void testHiddenColumn() {
		Display display = Display.getCurrent();
		Shell shell = new Shell(display);
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new FillLayout());

		CVSHistoryTableProvider provider = new CVSHistoryTableProvider();
		// provider.settings = createDialogSettings(...);
		ReflectionUtils.setField(
				provider,
				"settings",
				createDialogSettings(provider, new int[] { 100, 0, 100, 100,
						100, 100 }));
		TreeViewer treeViewer = provider.createTree(composite);
		Tree tree = treeViewer.getTree();
		Layout layout = tree.getLayout();

		// layout.getColumns(tree);
		Item[] items = (Item[]) ReflectionUtils.callMethod(layout,
				"getColumns", new Class[] { Composite.class },
				new Object[] { tree });
		assertEquals(6, items.length);

		// List columns = layout.columns;
		List<ColumnPixelData> columns = (List<ColumnPixelData>) ReflectionUtils.getField(
				layout, "columns");
		ColumnPixelData[] columnsArray = columns.toArray(new ColumnPixelData[0]);
		assertEquals(100, columnsArray[0].width);
		assertEquals(0, columnsArray[1].width); // keep user settings
		assertEquals(100, columnsArray[2].width);
		assertEquals(100, columnsArray[3].width);
		assertEquals(100, columnsArray[4].width);
		assertEquals(100, columnsArray[5].width);
	}

	private IDialogSettings createDialogSettings(
			CVSHistoryTableProvider provider, int[] widths) {
		String sectionName = (String) ReflectionUtils.getField(provider,
				"CVS_HISTORY_TABLE_PROVIDER_SECTION");
		IDialogSettings settings = new DialogSettings(sectionName);

		assertEquals(6, widths.length);
		String key = (String) ReflectionUtils.getField(provider,
				"COL_REVISIONID_NAME");
		settings.put(key, widths[0]);
		key = (String) ReflectionUtils.getField(provider, "COL_BRANCHES_NAME");
		settings.put(key, widths[1]);
		key = (String) ReflectionUtils.getField(provider, "COL_TAGS_NAME");
		settings.put(key, widths[2]);
		key = (String) ReflectionUtils.getField(provider, "COL_DATE_NAME");
		settings.put(key, widths[3]);
		key = (String) ReflectionUtils.getField(provider, "COL_AUTHOR_NAME");
		settings.put(key, widths[4]);
		key = (String) ReflectionUtils.getField(provider, "COL_COMMENT_NAME");
		settings.put(key, widths[5]);

		return settings;
	}

	public static Test suite() {
		return suite(CVSHisoryTableProviderTest.class);
	}
}
