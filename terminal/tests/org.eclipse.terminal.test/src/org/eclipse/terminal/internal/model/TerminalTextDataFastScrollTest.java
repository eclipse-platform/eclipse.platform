/*******************************************************************************
 * Copyright (c) 2007, 2026 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 *******************************************************************************/
package org.eclipse.terminal.internal.model;

import static org.eclipse.terminal.internal.model.TerminalTextTestHelper.fillSimple;
import static org.eclipse.terminal.internal.model.TerminalTextTestHelper.toSimple;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.terminal.model.ITerminalTextData;
import org.junit.jupiter.api.Test;

public class TerminalTextDataFastScrollTest extends AbstractITerminalTextDataTest {
	@Override
	protected ITerminalTextData makeITerminalTextData() {
		return new TerminalTextDataFastScroll(3);
	}

	/**
	 * Scrolling a top-anchored region while the ring has unused capacity must not leave scrolled
	 * out text where a later {@link ITerminalTextData#addLine()} grows into.
	 */
	@Test
	public void testScrollTopAnchoredRegionThenGrow() {
		ITerminalTextData term = new TerminalTextDataFastScroll(7);
		fillSimple(term, "012345");
		term.scroll(0, 5, -1);
		term.addLine();
		assertEquals("1234 5 ", toSimple(term));
	}

	/**
	 * Repeated top-anchored scrolls on a full ring, so the rotation also runs from a non-zero
	 * offset.
	 */
	@Test
	public void testScrollTopAnchoredRegionRepeatedly() {
		ITerminalTextData term = new TerminalTextDataFastScroll(6);
		fillSimple(term, "012345");
		term.scroll(0, 6, -2);
		assertEquals("2345  ", toSimple(term));
		term.setChar(4, 0, 'a', null);
		term.setChar(5, 0, 'b', null);
		term.scroll(0, 5, -1);
		assertEquals("345a b", toSimple(term));
		term.scroll(0, 5, -1);
		assertEquals("45a  b", toSimple(term));
	}

}
