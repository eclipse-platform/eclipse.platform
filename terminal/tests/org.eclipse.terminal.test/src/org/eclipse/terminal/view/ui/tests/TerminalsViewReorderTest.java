/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.view.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.view.ui.internal.view.TerminalsView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for reordering terminal tabs in the terminals view (see
 * <a href="https://github.com/eclipse-platform/eclipse.platform/issues/2679">issue 2679</a>).
 */
public class TerminalsViewReorderTest {

	private static Display display = null;

	@BeforeAll
	public static void createDisplay() {
		if (Display.getCurrent() == null) {
			display = new Display();
		}
	}

	@AfterAll
	public static void disposeDisplay() {
		if (display != null) {
			display.dispose();
			display = null;
		}
	}

	@Test
	public void dropOverAnotherTabTargetsThatTab() {
		assertEquals(2, TerminalsView.computeReorderIndex(0, 2, 4));
	}

	@Test
	public void dropOverItselfIsNoOp() {
		assertEquals(-1, TerminalsView.computeReorderIndex(2, 2, 4));
	}

	@Test
	public void dropNextToTheTabsTargetsTheLastPosition() {
		assertEquals(3, TerminalsView.computeReorderIndex(1, -1, 4));
	}

	@Test
	public void dropNextToTheTabsWhileAlreadyLastIsNoOp() {
		assertEquals(-1, TerminalsView.computeReorderIndex(3, -1, 4));
	}

	@Test
	public void singleTabIsNeverReordered() {
		assertEquals(-1, TerminalsView.computeReorderIndex(0, -1, 1));
	}

	/**
	 * Verifies the {@link CTabFolder#moveItem(int, int)} contract the reorder feature relies on:
	 * the items are reordered and the previously selected item stays selected.
	 */
	@Test
	public void moveItemReordersAndKeepsSelection() {
		Shell shell = new Shell(display);
		try {
			CTabFolder folder = new CTabFolder(shell, SWT.NONE);
			CTabItem a = newItem(folder, "A");
			CTabItem b = newItem(folder, "B");
			newItem(folder, "C");
			newItem(folder, "D");

			folder.setSelection(b);

			// Move "A" (index 0) to position 2: expected order is B, C, A, D.
			folder.moveItem(0, 2);

			assertEquals("B", folder.getItem(0).getText());
			assertEquals("C", folder.getItem(1).getText());
			assertEquals("A", folder.getItem(2).getText());
			assertEquals("D", folder.getItem(3).getText());
			assertEquals(2, folder.indexOf(a));

			// The selected item is unchanged even though its index moved.
			assertSame(b, folder.getSelection());
		} finally {
			shell.dispose();
		}
	}

	private static CTabItem newItem(CTabFolder folder, String text) {
		CTabItem item = new CTabItem(folder, SWT.CLOSE);
		item.setText(text);
		return item;
	}
}
