/*******************************************************************************
 * Copyright (c) 2026 and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.tests.DebugTestExtension;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@code ConsolePatternMatcher} pattern matching on console text that
 * contains ANSI escape sequences.
 * <p>
 * Patterns are matched against the text with the escape sequences removed and
 * the reported {@link PatternMatchEvent} range is mapped back to the raw
 * document. The range must cover exactly the matched text plus any escape
 * sequences located inside the match. Escape sequences before or after the
 * match must not be part of the range, otherwise listeners that trim the
 * reported range would trim an invisible escape character instead of the
 * visible one.
 * </p>
 */
@ExtendWith(DebugTestExtension.class)
public class ConsolePatternMatcherTests {

	private static final String FILE_LINK_PATTERN = "\\([^\\s]*\\.java:\\d+\\)"; //$NON-NLS-1$

	private IOConsole console;

	@BeforeEach
	public void setUp() {
		console = new IOConsole("ConsolePatternMatcherTests", "test", null); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testFileLinkWithTrailingEscape() throws Exception {
		String text = "\tat org.example.Demo.main(Demo.java:10)\u001b[0m\n"; //$NON-NLS-1$
		PatternMatchEvent event = match(text);
		assertMatch(text, event, "(Demo.java:10)"); //$NON-NLS-1$
	}

	@Test
	public void testFileLinkWithInternalEscapes() throws Exception {
		String text = "\tat org.example.Demo.main(\u001b[1mDemo.java:10\u001b[m)\n"; //$NON-NLS-1$
		PatternMatchEvent event = match(text);
		assertMatch(text, event, "(\u001b[1mDemo.java:10\u001b[m)"); //$NON-NLS-1$
	}

	@Test
	public void testFileLinkWithLeadingEscape() throws Exception {
		String text = "\tat org.example.Demo.main \u001b[1m(Demo.java:10)\n"; //$NON-NLS-1$
		PatternMatchEvent event = match(text);
		assertMatch(text, event, "(Demo.java:10)"); //$NON-NLS-1$
	}

	@Test
	public void testFileLinkWithoutEscapes() throws Exception {
		String text = "\tat org.example.Demo.main(Demo.java:10)\n"; //$NON-NLS-1$
		PatternMatchEvent event = match(text);
		assertMatch(text, event, "(Demo.java:10)"); //$NON-NLS-1$
	}

	@Test
	public void testMultipleFileLinksWithEscapes() throws Exception {
		String text = "\u001b[31mCaused by: java.lang.NullPointerException\u001b[m\n" //$NON-NLS-1$
				+ "\tat org.example.Demo.main(\u001b[1mDemo.java:10\u001b[m)\n" //$NON-NLS-1$
				+ "\tat org.example.Other.run(\u001b[1mOther.java:42\u001b[m)\n"; //$NON-NLS-1$
		List<PatternMatchEvent> events = matchAll(text);
		assertEquals(2, events.size());
		assertMatch(text, events.get(0), "(\u001b[1mDemo.java:10\u001b[m)"); //$NON-NLS-1$
		assertMatch(text, events.get(1), "(\u001b[1mOther.java:42\u001b[m)"); //$NON-NLS-1$
	}

	private PatternMatchEvent match(String text) throws Exception {
		List<PatternMatchEvent> events = matchAll(text);
		assertEquals(1, events.size(), () -> "expected exactly one match in: " + text); //$NON-NLS-1$
		return events.get(0);
	}

	private List<PatternMatchEvent> matchAll(String text) throws Exception {
		List<PatternMatchEvent> events = new CopyOnWriteArrayList<>();
		console.addPatternMatchListener(new CollectingMatchListener(events));
		console.getDocument().set(text);
		// the match job family is the console itself
		Job.getJobManager().join(console, null);
		return events;
	}

	private static void assertMatch(String text, PatternMatchEvent event, String rawMatch) {
		assertEquals(text.indexOf(rawMatch), event.getOffset(), rawMatch);
		assertEquals(rawMatch.length(), event.getLength(), rawMatch);
	}

	private static final class CollectingMatchListener implements IPatternMatchListener {

		private final List<PatternMatchEvent> events;

		CollectingMatchListener(List<PatternMatchEvent> events) {
			this.events = events;
		}

		@Override
		public void connect(TextConsole console) {
			// nothing to do
		}

		@Override
		public void disconnect() {
			// nothing to do
		}

		@Override
		public void matchFound(PatternMatchEvent event) {
			events.add(event);
		}

		@Override
		public String getPattern() {
			return FILE_LINK_PATTERN;
		}

		@Override
		public int getCompilerFlags() {
			return 0;
		}

		@Override
		public String getLineQualifier() {
			return null;
		}
	}
}
