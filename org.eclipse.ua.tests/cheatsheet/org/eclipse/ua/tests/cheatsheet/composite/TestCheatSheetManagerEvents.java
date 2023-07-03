/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.cheatsheet.composite;

import static org.junit.Assert.assertEquals;

import org.eclipse.ui.cheatsheets.CheatSheetListener;
import org.eclipse.ui.cheatsheets.ICheatSheetEvent;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.junit.Before;
import org.junit.Test;

public class TestCheatSheetManagerEvents {

	private CheatSheetElement element;
	private CheatSheetManager manager;
	private int handler1Calls;
	private int handler2Calls;

	private class Handler1 extends CheatSheetListener {
		@Override
		public void cheatSheetEvent(ICheatSheetEvent event) {
			handler1Calls++;
		}
	}

	private class Handler2 extends CheatSheetListener {
		@Override
		public void cheatSheetEvent(ICheatSheetEvent event) {
			handler2Calls++;
		}
	}

	@Before
	public void setUp() throws Exception {
		element = new CheatSheetElement("Name");
		manager = new CheatSheetManager(element);
		handler1Calls = 0;
		handler2Calls = 0;
	}

	@Test
	public void testNoHandler() {
		manager.fireEvent(ICheatSheetEvent.CHEATSHEET_STARTED);
	}

	@Test
	public void testOneHandler() {
		manager.addListener(new Handler1());
		manager.fireEvent(ICheatSheetEvent.CHEATSHEET_STARTED);
		assertEquals(1, handler1Calls);
	}

	@Test
	public void testTwoHandlers() {
		manager.addListener(new Handler1());
		manager.addListener(new Handler2());
		manager.fireEvent(ICheatSheetEvent.CHEATSHEET_STARTED);
		assertEquals(1, handler1Calls);
		assertEquals(1, handler2Calls);
	}

}
