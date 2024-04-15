/*******************************************************************************
 * Copyright (c) 2019 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentAdapter;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;
import org.eclipse.ui.internal.console.ConsoleDocumentAdapter;
import org.junit.Test;

/**
 * Tests {@link ConsoleDocumentAdapter}.
 * <p>
 * Primary tests fixed width mode and calculation of {@link TextChangingEvent}s.
 * </p>
 */
@SuppressWarnings("restriction")
public class ConsoleDocumentAdapterTests extends AbstractDebugTest {

	/**
	 * Test {@link ConsoleDocumentAdapter#setText(String)}.
	 */
	@Test
	public void testSetText() {
		final ExpectingTextChangeListener eventListener = new ExpectingTextChangeListener(true, null);
		final IDocumentAdapter docAdapter = new ConsoleDocumentAdapter(-1);
		docAdapter.setDocument(new Document());
		docAdapter.addTextChangeListener(eventListener);

		final String text = "123456";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.SET));
		docAdapter.setText(text);
		assertContent(docAdapter, text);
		assertNumberOfLines(docAdapter, 1);
		checkLineMapping(docAdapter, null);

		for (String s : new String[] { "", null }) {
			docAdapter.setText(s);
			assertContent(docAdapter, "");
			assertNumberOfLines(docAdapter, 1);
			checkLineMapping(docAdapter, null);
		}
	}

	/**
	 * Test fixed width line wrap. (mostly with changes affecting a single line)
	 */
	@Test
	public void testLineWrap() {
		final Random rand = new Random(4);
		final int wrapWidth = 10;
		final IDocumentAdapter docAdapter = new ConsoleDocumentAdapter(wrapWidth);
		final ExpectingTextChangeListener eventListener = new ExpectingTextChangeListener(false, docAdapter);
		assertEquals("Failed to set width.", wrapWidth, ((ConsoleDocumentAdapter) docAdapter).getWidth());
		docAdapter.setDocument(new Document());
		docAdapter.addTextChangeListener(eventListener);
		// repeated add should not result in repeated events
		docAdapter.addTextChangeListener(eventListener);

		// initialize document
		final String initText = "012345";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.SET));
		docAdapter.setText(initText);
		assertNumberOfLines(docAdapter, 1);
		assertLine(docAdapter, 0, initText);
		checkLineMapping(docAdapter, rand);

		// add text without new line wrap
		String addText = "67";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 1);
		assertLine(docAdapter, 0, "01234567");
		checkLineMapping(docAdapter, rand);

		// add text with one new line wrap
		addText = "89AB";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 2);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "AB");
		checkLineMapping(docAdapter, rand);

		// insert text in wrapped line without new line wrap
		int offset = 6;
		addText = "---";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, 0, addText.length(), 1, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 0, addText);
		assertNumberOfLines(docAdapter, 2);
		assertLine(docAdapter, 0, "012345---6");
		assertLine(docAdapter, 1, "789AB");
		checkLineMapping(docAdapter, rand);

		// remove last insert
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", 3, 0, 1, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 3, "");
		assertNumberOfLines(docAdapter, 2);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "AB");
		checkLineMapping(docAdapter, rand);

		// add text with two new line wrap
		addText = "CDEFGHIJabcdefghijKL";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 2));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 4);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KL");
		checkLineMapping(docAdapter, rand);

		// replace text without new line wrap
		String replaceText = "~~~";
		offset = 3;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, replaceText, replaceText.length(), replaceText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, replaceText.length(), replaceText);
		assertNumberOfLines(docAdapter, 4);
		assertLine(docAdapter, 0, "012~~~6789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KL");
		checkLineMapping(docAdapter, rand);

		// remove text with one removed line wrap
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", 3, 0, 3, 2));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 3, "");
		assertNumberOfLines(docAdapter, 3);
		assertLine(docAdapter, 0, "012" + "6789ABC");
		assertLine(docAdapter, 1, "DEFGHIJabc");
		assertLine(docAdapter, 2, "defghijKL");
		checkLineMapping(docAdapter, rand);

		// insert text with one new line wrap
		addText = "345";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, 0, addText.length(), 2, 3));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 0, addText);
		assertNumberOfLines(docAdapter, 4);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KL");
		checkLineMapping(docAdapter, rand);

		// add text ending at fixed width border
		addText = "MNOPQRST";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 4);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertEquals("Got wrong line index.", 3, docAdapter.getLineAtOffset(40));
		checkLineMapping(docAdapter, rand);

		// add text starting at fixed width border
		addText = "kl";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 5);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "kl");
		checkLineMapping(docAdapter, rand);

		// insert text starting at fixed width border
		addText = ">";
		offset = docAdapter.getCharCount() - 2;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, 0, addText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 0, addText);
		assertNumberOfLines(docAdapter, 5);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, ">kl");
		checkLineMapping(docAdapter, rand);

		// delete character at fixed width border
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", 1, 0, 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 1, "");
		assertNumberOfLines(docAdapter, 5);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "kl");
		checkLineMapping(docAdapter, rand);

		// add newline to start... new line
		addText = "\n";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 6);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "kl");
		assertLine(docAdapter, 5, "");
		assertEquals("Got wrong line.", 4, docAdapter.getLineAtOffset(docAdapter.getCharCount() - 2));
		assertEquals("Got wrong line.", 4, docAdapter.getLineAtOffset(docAdapter.getCharCount() - 1));
		assertEquals("Got wrong line.", 5, docAdapter.getLineAtOffset(docAdapter.getCharCount()));
		assertEquals("Get wrong content.", "\n", docAdapter.getTextRange(42, 1));
		checkLineMapping(docAdapter, rand);

		// add text ending at fixed width border
		addText = "UVWXYZ.,:;";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 6);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "kl");
		assertLine(docAdapter, 5, "UVWXYZ.,:;");
		assertEquals("Got wrong line.", 5, docAdapter.getLineAtOffset(docAdapter.getCharCount()));
		assertEquals("Get wrong content.", "l\nU", docAdapter.getTextRange(41, 3));
		checkLineMapping(docAdapter, rand);

		// remove text without line wrap change
		offset = docAdapter.getOffsetAtLine(docAdapter.getLineCount() - 1) + 2;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", 4, 0, 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 4, "");
		assertNumberOfLines(docAdapter, 6);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "kl");
		assertLine(docAdapter, 5, "UV" + ".,:;");
		checkLineMapping(docAdapter, rand);

		// replace and insert text and end at fixed width border
		replaceText = "WXYZ.,:;";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, replaceText, 4, replaceText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 4, replaceText);
		assertNumberOfLines(docAdapter, 6);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "kl");
		assertLine(docAdapter, 5, "UVWXYZ.,:;");
		assertEquals("Got wrong line.", 5, docAdapter.getLineAtOffset(docAdapter.getCharCount()));
		checkLineMapping(docAdapter, rand);

		// replace text without length change
		offset = docAdapter.getCharCount() - 3;
		replaceText = "~~~";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, replaceText, replaceText.length(), replaceText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, replaceText.length(), replaceText);
		assertNumberOfLines(docAdapter, 6);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "kl");
		assertLine(docAdapter, 5, "UVWXYZ.~~~");
		assertEquals("Got wrong line.", 5, docAdapter.getLineAtOffset(docAdapter.getCharCount()));
		checkLineMapping(docAdapter, rand);

		// insert text with one new line wrap
		addText = ",:;";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, 0, addText.length(), 0, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 0, addText);
		assertNumberOfLines(docAdapter, 7);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "kl");
		assertLine(docAdapter, 5, "UVWXYZ.,:;");
		assertLine(docAdapter, 6, "~~~");
		checkLineMapping(docAdapter, rand);

		// manipulate text so both document lines end at wrap border
		addText = "mnopqrst";
		offset = 42;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, 0, addText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 0, addText);
		replaceText = "uvwxyz_-=|";
		offset = docAdapter.getCharCount() - 3;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, replaceText, 3, replaceText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 3, replaceText);
		assertNumberOfLines(docAdapter, 7);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abcdefghij");
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "klmnopqrst"); // <-- real line delimiter here
		assertLine(docAdapter, 5, "UVWXYZ.,:;");
		assertLine(docAdapter, 6, "uvwxyz_-=|");
		assertEquals("Got wrong line.", 4, docAdapter.getLineAtOffset(49));
		assertEquals("Got wrong line.", 4, docAdapter.getLineAtOffset(50));
		assertEquals("Got wrong line.", 5, docAdapter.getLineAtOffset(51));
		assertEquals("Got wrong line.", 6, docAdapter.getLineAtOffset(docAdapter.getCharCount()));
		checkLineMapping(docAdapter, rand);

		// remove last wrapped line
		offset = docAdapter.getOffsetAtLine(docAdapter.getLineCount() - 1);
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", 10, 0, 1, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 10, "");
		assertNumberOfLines(docAdapter, 6);
		checkLineMapping(docAdapter, rand);

		// replace text ending at wrap border without new line wrap
		replaceText = "~~~";
		offset = 7;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, replaceText, replaceText.length(), replaceText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, replaceText.length(), replaceText);
		assertNumberOfLines(docAdapter, 6);
		assertLine(docAdapter, 0, "0123456~~~");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		checkLineMapping(docAdapter, rand);

		// remove 5th line including it's real line delimiter
		offset = docAdapter.getOffsetAtLine(5 - 1);
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", 11, 0, 1, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 11, "");
		assertNumberOfLines(docAdapter, 5);
		assertLine(docAdapter, 3, "KLMNOPQRST");
		assertLine(docAdapter, 4, "UVWXYZ.,:;");
		checkLineMapping(docAdapter, rand);

		// remove text ending at wrap border
		int remove = 3;
		offset = 7;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", remove, 0, 4, 4));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, remove, "");
		assertNumberOfLines(docAdapter, 5);
		assertLine(docAdapter, 0, "0123456ABC");
		assertLine(docAdapter, 1, "DEFGHIJabc");
		assertLine(docAdapter, 2, "defghijKLM");
		assertLine(docAdapter, 3, "NOPQRSTUVW");
		assertLine(docAdapter, 4, "XYZ.,:;");
		checkLineMapping(docAdapter, rand);

		assertFalse("Some expected change events were not received.", eventListener.hasPendingExpections());
		docAdapter.removeTextChangeListener(eventListener);
		clearDocument(docAdapter);
	}

	/**
	 * Test inserting text containing line delimiters.
	 */
	@Test
	public void testMultilineInserts() {
		final Random rand = new Random(4);
		final int wrapWidth = 10;
		final IDocumentAdapter docAdapter = new ConsoleDocumentAdapter(wrapWidth);
		final ExpectingTextChangeListener eventListener = new ExpectingTextChangeListener(false, docAdapter);
		assertEquals("Failed to set width.", wrapWidth, ((ConsoleDocumentAdapter) docAdapter).getWidth());
		docAdapter.setDocument(new Document());
		docAdapter.addTextChangeListener(eventListener);

		// add 4 new document lines with 1 line wrapped
		String addText = "012345\nABCDEFGHIJa\r\n\nklmnopqrst\r\nuv";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, 0, addText, 0, addText.length(), 0, 5));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(0, 0, addText);
		assertNumberOfLines(docAdapter, 6);
		assertLine(docAdapter, 0, "012345");
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "a");
		assertLine(docAdapter, 3, "");
		assertLine(docAdapter, 4, "klmnopqrst");
		assertLine(docAdapter, 5, "uv");
		checkLineMapping(docAdapter, rand);

		// insert newline at begin of line
		addText = "\n";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, 0, addText, 0, addText.length(), 0, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(0, 0, addText);
		assertNumberOfLines(docAdapter, 7);
		assertLine(docAdapter, 0, "");
		assertLine(docAdapter, 1, "012345");
		checkLineMapping(docAdapter, rand);

		// insert text + newline at begin of line
		addText = "9876543210?\n";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, 0, addText, 0, addText.length(), 0, 2));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(0, 0, addText);
		assertNumberOfLines(docAdapter, 9);
		assertLine(docAdapter, 0, "9876543210");
		assertLine(docAdapter, 1, "?");
		assertLine(docAdapter, 2, "");
		checkLineMapping(docAdapter, rand);

		// insert newline + text at begin of line
		addText = "\r\nfoo";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, 0, addText, 0, addText.length(), 1, 2));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(0, 0, addText);
		assertNumberOfLines(docAdapter, 10);
		assertLine(docAdapter, 0, "");
		assertLine(docAdapter, 1, "foo9876543");
		assertLine(docAdapter, 2, "210?");
		assertLine(docAdapter, 3, "");
		checkLineMapping(docAdapter, rand);

		// insert text + newline + text at begin of line
		addText = ".,:;\r\nbar";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, 0, addText, 0, addText.length(), 0, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(0, 0, addText);
		assertNumberOfLines(docAdapter, 11);
		assertLine(docAdapter, 0, ".,:;");
		assertLine(docAdapter, 1, "bar");
		assertLine(docAdapter, 2, "foo9876543");
		checkLineMapping(docAdapter, rand);

		// insert 2 newline at end of line
		addText = "\r\n\n";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 2));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 13);
		assertLine(docAdapter, 10, "uv");
		assertLine(docAdapter, 11, "");
		assertLine(docAdapter, 12, "");
		checkLineMapping(docAdapter, rand);

		// insert text + 2 newline at end of line (one line wrapping)
		addText = "BCDEFGHIJKbc\n\n";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 3));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 16);
		assertLine(docAdapter, 12, "BCDEFGHIJK");
		assertLine(docAdapter, 13, "bc");
		assertLine(docAdapter, 14, "");
		assertLine(docAdapter, 15, "");
		checkLineMapping(docAdapter, rand);

		// insert 2 newline + text at end of line
		addText = "\r\nLMNOPQR\nlmnopqrst";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 2));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 18);
		assertLine(docAdapter, 15, "");
		assertLine(docAdapter, 16, "LMNOPQR");
		assertLine(docAdapter, 17, "lmnopqrst");
		checkLineMapping(docAdapter, rand);

		// insert text + 2 newline + text at end of line (+one line wrapping)
		addText = "uVW\nvwxy\r\n1357902468";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, docAdapter.getCharCount(), addText, 0, addText.length(), 0, 3));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, addText);
		assertNumberOfLines(docAdapter, 21);
		assertLine(docAdapter, 17, "lmnopqrstu");
		assertLine(docAdapter, 18, "VW");
		assertLine(docAdapter, 19, "vwxy");
		assertLine(docAdapter, 20, "1357902468");
		checkLineMapping(docAdapter, rand);

		// insert newline inside (wrapped) line
		int offset = docAdapter.getOffsetAtLine(17) + 8;
		addText = "\n";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, 0, addText.length(), 1, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 0, addText);
		assertNumberOfLines(docAdapter, 21);
		assertLine(docAdapter, 17, "lmnopqrs");
		assertLine(docAdapter, 18, "tuVW");
		assertLine(docAdapter, 19, "vwxy");
		checkLineMapping(docAdapter, rand);

		// insert newline + text + newline in existing line
		offset = docAdapter.getOffsetAtLine(19) + 2;
		addText = "\n[()]\r\n";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, 0, addText.length(), 0, 2));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 0, addText);
		assertNumberOfLines(docAdapter, 23);
		assertLine(docAdapter, 19, "vw");
		assertLine(docAdapter, 20, "[()]");
		assertLine(docAdapter, 21, "xy");
		checkLineMapping(docAdapter, rand);

		// insert newline + long text + newline in existing line
		offset = docAdapter.getOffsetAtLine(20);
		addText = "\r\nCDEFGHIJKLcdefghijklMNOPQR\n";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, 0, addText.length(), 0, 4));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 0, addText);
		assertNumberOfLines(docAdapter, 27);
		assertLine(docAdapter, 20, "");
		assertLine(docAdapter, 21, "CDEFGHIJKL");
		assertLine(docAdapter, 22, "cdefghijkl");
		assertLine(docAdapter, 23, "MNOPQR");
		assertLine(docAdapter, 24, "[()]");
		checkLineMapping(docAdapter, rand);

		// insert newline inside (wrapped) line
		offset = docAdapter.getOffsetAtLine(21) + 9;
		addText = "\n";
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, 0, addText.length(), 2, 2));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, 0, addText);
		assertNumberOfLines(docAdapter, 27);
		assertLine(docAdapter, 21, "CDEFGHIJK");
		assertLine(docAdapter, 22, "Lcdefghijk");
		assertLine(docAdapter, 23, "lMNOPQR");
		checkLineMapping(docAdapter, rand);

		assertFalse("Some expected change events were not received.", eventListener.hasPendingExpections());
		docAdapter.removeTextChangeListener(eventListener);
		clearDocument(docAdapter);
	}

	/**
	 * Test text remove affecting than one line.
	 */
	@Test
	public void testMultilineRemove() {
		final Random rand = new Random(4);
		final int wrapWidth = 10;
		final ExpectingTextChangeListener eventListener = new ExpectingTextChangeListener(true, null);
		final IDocumentAdapter docAdapter = new ConsoleDocumentAdapter(wrapWidth);
		assertEquals("Failed to set width.", wrapWidth, ((ConsoleDocumentAdapter) docAdapter).getWidth());
		docAdapter.setDocument(new Document());
		docAdapter.addTextChangeListener(eventListener);

		// prepare one wrapped line
		clearDocument(docAdapter);
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "0123456789");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "ABCDE");
		// remove over wrap border but wrap remains
		int offset = 8;
		int remove = 4;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", remove, 0, 1, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, remove, "");
		assertNumberOfLines(docAdapter, 2);
		assertLine(docAdapter, 0, "01234567CD");
		assertLine(docAdapter, 1, "E");
		checkLineMapping(docAdapter, rand);

		// perform empty change event
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, 0, "", 0, 0, 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(0, 0, "");
		assertFalse("Some expected change events were not received.", eventListener.hasPendingExpections());
		checkLineMapping(docAdapter, rand);

		// remove over wrap border (removes wrap)
		offset = 8;
		remove = 3;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", remove, 0, 1, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, remove, "");
		assertNumberOfLines(docAdapter, 1);
		assertLine(docAdapter, 0, "01234567");
		checkLineMapping(docAdapter, rand);

		// prepare one double wrapped line
		clearDocument(docAdapter);
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "0123456789");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "ABCDEFGHIJ");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "abcde");
		// remove over two wrap border
		offset = 8;
		remove = 14;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", remove, 0, 2, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, remove, "");
		assertNumberOfLines(docAdapter, 2);
		assertLine(docAdapter, 0, "01234567cd");
		assertLine(docAdapter, 1, "e");
		checkLineMapping(docAdapter, rand);

		// remove at fixed width border
		offset = 10;
		remove = 1;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", remove, 0, 1, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, remove, "");
		assertNumberOfLines(docAdapter, 1);
		assertLine(docAdapter, 0, "01234567cd");
		checkLineMapping(docAdapter, rand);

		// prepare two unwrapped lines
		clearDocument(docAdapter);
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "01234567\n");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "ABCDEF");
		// remove line delimiter (produces wrapped line)
		offset = 8;
		remove = 1;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", remove, 0, 1, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, remove, "");
		assertNumberOfLines(docAdapter, 2);
		assertLine(docAdapter, 0, "01234567AB");
		assertLine(docAdapter, 1, "CDEF");
		checkLineMapping(docAdapter, rand);

		// prepare some more lines
		clearDocument(docAdapter);
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "01234567\n");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "ABCDEFGHIJ");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "abcdefghij");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "KLMN\r\n");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "klmno\n");
		assertLine(docAdapter, 5, "");
		// remove multiple lines
		offset = 5;
		remove = 36;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", remove, 0, 5, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, remove, "");
		assertNumberOfLines(docAdapter, 1);
		assertLine(docAdapter, 0, "01234");
		checkLineMapping(docAdapter, rand);

		// prepare more lines
		clearDocument(docAdapter);
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "01234567\n");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "\n");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "\r\n");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "\n");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, docAdapter.getLineDelimiter());
		assertNumberOfLines(docAdapter, 6);
		// remove empty lines
		// (and do not start in first line like most tests before)
		offset = 10;
		remove = 3;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, "", remove, 0, 2, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, remove, "");
		assertNumberOfLines(docAdapter, 4);
		checkLineMapping(docAdapter, rand);

		assertFalse("Some expected change events were not received.", eventListener.hasPendingExpections());
	}

	/**
	 * Test text change affecting and inserting more than one line.
	 */
	@Test
	public void testMultilineReplace() {
		final Random rand = new Random(4);
		final int wrapWidth = 10;
		final ExpectingTextChangeListener eventListener = new ExpectingTextChangeListener(true, null);
		final IDocumentAdapter docAdapter = new ConsoleDocumentAdapter(wrapWidth);
		assertEquals("Failed to set width.", wrapWidth, ((ConsoleDocumentAdapter) docAdapter).getWidth());
		docAdapter.setDocument(new Document());
		docAdapter.addTextChangeListener(eventListener);

		docAdapter.setText("0123~~~AB");

		// remove in single line and insert newline
		String addText = "\n";
		int offset = 4;
		int length = 3;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, length, addText.length(), 0, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, length, addText);
		assertNumberOfLines(docAdapter, 2);
		assertLine(docAdapter, 0, "0123"); // \n
		assertLine(docAdapter, 1, "AB");
		checkLineMapping(docAdapter, rand);

		// replace newline with more text and a new newline
		addText = "456789abc\r\n+";
		length = 2;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, length, addText.length(), 1, 2));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, length, addText);
		assertNumberOfLines(docAdapter, 3);
		assertLine(docAdapter, 0, "0123456789");
		assertLine(docAdapter, 1, "abc"); // \r\n
		assertLine(docAdapter, 2, "+B");
		checkLineMapping(docAdapter, rand);

		// remove and insert newline
		addText = "<->\nABCDEFGHIJabc\n#";
		length = 12;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, length, addText.length(), 2, 3));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, length, addText);
		assertNumberOfLines(docAdapter, 4);
		assertLine(docAdapter, 0, "0123<->"); // \n
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "abc"); // \n
		assertLine(docAdapter, 3, "#B");
		checkLineMapping(docAdapter, rand);

		// insert newline and replace wrap
		addText = "\n>=<";
		offset = 18;
		length = 3;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, null, null, null, null, 1, 1));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, length, addText);
		assertNumberOfLines(docAdapter, 4);
		assertLine(docAdapter, 0, "0123<->"); // \n
		assertLine(docAdapter, 1, "ABCDEFGHIJ"); // \n
		assertLine(docAdapter, 2, ">=<"); // \n
		assertLine(docAdapter, 3, "#B");
		checkLineMapping(docAdapter, rand);

		// replace last (real) line with new content
		addText = "*+";
		offset = docAdapter.getCharCount() - 2;
		length = 1;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, length, addText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, length, addText);
		assertNumberOfLines(docAdapter, 4);
		assertLine(docAdapter, 2, ">=<"); // \n
		assertLine(docAdapter, 3, "*+B");
		checkLineMapping(docAdapter, rand);

		// replace last character (at fixed width border) with new content
		docAdapter.setText("");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "0123456789");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "ABCDEFGHIJ");
		docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, "a");
		addText = "$b";
		offset = docAdapter.getCharCount() - 1;
		length = 1;
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGING, offset, addText, length, addText.length(), 0, 0));
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.CHANGED));
		docAdapter.replaceTextRange(offset, length, addText);
		assertNumberOfLines(docAdapter, 3);
		assertLine(docAdapter, 1, "ABCDEFGHIJ");
		assertLine(docAdapter, 2, "$b");
		checkLineMapping(docAdapter, rand);

		assertFalse("Some expected change events were not received.", eventListener.hasPendingExpections());
	}

	/**
	 * Test line wrapping is correctly updated if fixed width changed.
	 */
	@Test
	public void testSetWidth() {
		final Random rand = new Random(7);
		final ConsoleDocumentAdapter docAdapter = new ConsoleDocumentAdapter(-1);
		docAdapter.setDocument(new Document());

		final String line0_0 = "#123456789";
		final String line0_1 = "ABCDEF";
		final String line0 = line0_0 + line0_1;
		final String line1 = "";
		final String line2 = "abcd";
		final String text = line0 + "\n" + line1 + "\r\n" + line2;
		docAdapter.setText(text);
		assertEquals("Get text range failed.", "#", docAdapter.getTextRange(0, 1));

		// check with initial disabled fixed width mode
		assertLine(docAdapter, 0, line0);
		assertLine(docAdapter, 1, line1);
		assertLine(docAdapter, 2, line2);
		assertEquals("Adapter content length wrong.", text.length(), docAdapter.getCharCount());
		checkLineMapping(docAdapter, rand);

		// test with enabled fixed width mode
		docAdapter.setWidth(10);
		docAdapter.setWidth(10); // intentional double set
		assertLine(docAdapter, 0, line0_0);
		assertLine(docAdapter, 1, line0_1);
		assertLine(docAdapter, 2, line1);
		assertLine(docAdapter, 3, line2);
		assertEquals("Adapter content length wrong.", text.length(), docAdapter.getCharCount());
		checkLineMapping(docAdapter, rand);

		// test with disabled fixed width mode after it was enabled
		docAdapter.setWidth(-1);
		assertLine(docAdapter, 0, line0);
		assertLine(docAdapter, 1, line1);
		assertLine(docAdapter, 2, line2);
		assertEquals("Adapter content length wrong.", text.length(), docAdapter.getCharCount());
		checkLineMapping(docAdapter, rand);

		// test with fixed width mode but no virtual wrappings
		docAdapter.setWidth(80);
		assertLine(docAdapter, 0, line0);
		assertLine(docAdapter, 1, line1);
		assertLine(docAdapter, 2, line2);
		assertEquals("Adapter content length wrong.", text.length(), docAdapter.getCharCount());
		checkLineMapping(docAdapter, rand);

		// add a listener for the next width change
		final ExpectingTextChangeListener eventListener = new ExpectingTextChangeListener(false, docAdapter);
		docAdapter.addTextChangeListener(eventListener);
		eventListener.addExpectation(new TextEventExpectation(TextChangeEventType.SET));

		// test extreme small console
		docAdapter.setWidth(1);
		int expectedLines = Math.max(line0.length(), 1);
		expectedLines += Math.max(line1.length(), 1);
		expectedLines += Math.max(line2.length(), 1);
		assertNumberOfLines(docAdapter, expectedLines);
		checkLineMapping(docAdapter, rand);
		assertFalse("Some expected change events were not received.", eventListener.hasPendingExpections());
	}

	/**
	 * Some test cases derived from JavaDoc examples of {@link IDocumentAdapter}
	 * (and its super interfaces).
	 */
	@Test
	public void testInterfaceContract() {
		final ConsoleDocumentAdapter docAdapter = new ConsoleDocumentAdapter(-1);
		docAdapter.setDocument(new Document());
		assertNotNull("Adapter has no legal line delimiter.", docAdapter.getLineDelimiter());

		for (int width : new int[] { -1, 80 }) {
			docAdapter.setWidth(width);

			// test against documentation of
			// IDocumentAdapter#getLineAtOffset(int)
			docAdapter.setText("\r\n\r\n");
			checkLineMapping(docAdapter, null);
			assertEquals("Wrong line for offset.", 0, docAdapter.getLineAtOffset(0));
			assertEquals("Wrong line for offset.", 0, docAdapter.getLineAtOffset(1));
			assertEquals("Wrong line for offset.", 1, docAdapter.getLineAtOffset(2));
			assertEquals("Wrong line for offset.", 1, docAdapter.getLineAtOffset(3));
			assertEquals("Wrong line for offset.", 2, docAdapter.getLineAtOffset(4));
			assertEquals("Wrong line for offset.", docAdapter.getLineCount() - 1, docAdapter.getLineAtOffset(docAdapter.getCharCount()));

			// test against documentation of IDocumentAdapter#getLineCount()
			docAdapter.setText(null);
			assertNumberOfLines(docAdapter, 1);
			docAdapter.setText("");
			assertNumberOfLines(docAdapter, 1);
			docAdapter.setText("a\n");
			assertNumberOfLines(docAdapter, 2);
			docAdapter.setText("\n\n");
			assertNumberOfLines(docAdapter, 3);

			// test against documentation of
			// IDocumentAdapter#getOffsetAtLine(int)
			docAdapter.setText("\r\ntest\r\n");
			checkLineMapping(docAdapter, null);
			assertEquals("Wrong offset for line.", 0, docAdapter.getOffsetAtLine(0));
			assertEquals("Wrong offset for line.", 2, docAdapter.getOffsetAtLine(1));
			assertEquals("Wrong offset for line.", 8, docAdapter.getOffsetAtLine(2));
			docAdapter.setText("");
			assertEquals("Wrong offset for line.", 0, docAdapter.getOffsetAtLine(0));
		}
	}

	/**
	 * Test
	 * {@link IDocumentAdapter#setDocument(org.eclipse.jface.text.IDocument)}
	 * and ensure old document is indeed disconnected and not notified anymore.
	 */
	@Test
	public void testChangeDocument() {
		final IDocumentAdapter docAdapter = new ConsoleDocumentAdapter(-1);
		final IDocument doc1 = new Document();
		final IDocument doc2 = new Document();
		final String text1 = "foo";
		final String text2 = "bar";

		docAdapter.setDocument(doc1);
		docAdapter.setText(text1);
		assertContent(docAdapter, text1);

		final String doc1Text = doc1.get();
		docAdapter.setDocument(doc2);
		docAdapter.setText(text2);
		assertContent(docAdapter, text2);
		assertEquals("Document was changed after disconnect.", doc1Text, doc1.get());

		docAdapter.setDocument(null);
	}

	/**
	 * Test if invalid arguments produces error log messages.
	 */
	@Test
	public void testInvalidInvocations() {
		final AtomicInteger expectedErrors = new AtomicInteger(0);
		final ILogListener logListener = (status, plugin) -> {
			if (status.matches(IStatus.ERROR)) {
				expectedErrors.decrementAndGet();
			}
		};
		try {
			Platform.addLogListener(logListener);

			final ConsoleDocumentAdapter docAdapter = new ConsoleDocumentAdapter(-1);
			docAdapter.setDocument(null);
			docAdapter.setDocument(new Document());
			try {
				docAdapter.addTextChangeListener(null);
				fail("Exception not thrown.");
			} catch (IllegalArgumentException ex) {
				// expected behavior
			}
			try {
				docAdapter.removeTextChangeListener(null);
				fail("Exception not thrown.");
			} catch (IllegalArgumentException ex) {
				// expected behavior
			}

			for (int width : new int[] { -1, -2, 80 }) {
				docAdapter.setWidth(width);
				if (width != -2) {
					docAdapter.setText("?");
				} else {
					// In non fixed mode the documents line tracker is used for
					// most queries. Document may use different trackers if test
					// is set or replaced.
					docAdapter.replaceTextRange(0, docAdapter.getCharCount(), "?");
				}

				expectedErrors.set(3);
				docAdapter.getLine(Integer.MIN_VALUE);
				docAdapter.getLine(-1);
				// getLine(1) should be invalid as well but the current
				// implementation uses getLineInformation and ListLineTracker
				// returns information for this non existing line and
				// TreeLineTracke does so for compatibility reasons
				docAdapter.getLine(Integer.MAX_VALUE);
				assertEquals("Too much success with width: " + width, 0, expectedErrors.get());

				expectedErrors.set(4);
				docAdapter.getLineAtOffset(Integer.MIN_VALUE);
				docAdapter.getLineAtOffset(-1);
				docAdapter.getLineAtOffset(1);
				docAdapter.getLineAtOffset(2);
				docAdapter.getLineAtOffset(Integer.MAX_VALUE);
				assertEquals("Too much success with width: " + width, 0, expectedErrors.get());

				expectedErrors.set(4);
				docAdapter.getOffsetAtLine(Integer.MIN_VALUE);
				docAdapter.getOffsetAtLine(-1);
				docAdapter.getOffsetAtLine(1);
				docAdapter.getOffsetAtLine(Integer.MAX_VALUE);
				assertEquals("Too much success with width: " + width, 0, expectedErrors.get());

				expectedErrors.set(6);
				docAdapter.getTextRange(-1, 1);
				docAdapter.getTextRange(0, -1);
				docAdapter.getTextRange(1, -1);
				docAdapter.getTextRange(-1, 2);
				docAdapter.getTextRange(0, 2);
				docAdapter.getTextRange(0, 3);
				assertEquals("Too much success with width: " + width, 0, expectedErrors.get());

				expectedErrors.set(10);
				docAdapter.replaceTextRange(2, 0, "");
				docAdapter.replaceTextRange(-1, 0, "");
				docAdapter.replaceTextRange(0, 2, "");
				docAdapter.replaceTextRange(0, -1, "");
				docAdapter.replaceTextRange(1, 1, "");
				docAdapter.replaceTextRange(0, 2, "foo");
				docAdapter.replaceTextRange(0, -1, "foo");
				docAdapter.replaceTextRange(1, 1, "foo");
				docAdapter.replaceTextRange(1, -1, "foo");
				docAdapter.replaceTextRange(2, 0, "foo");
				assertEquals("Too much success with width: " + width, 0, expectedErrors.get());
			}
		} finally {
			Platform.removeLogListener(logListener);
		}
	}

	/**
	 * Test adapter with a larger number of lines. (especially tests array grow)
	 */
	@Test
	public void testManyLines() {
		final ConsoleDocumentAdapter docAdapter = new ConsoleDocumentAdapter(80);
		docAdapter.setDocument(new Document());
		final String lineDelimiter = docAdapter.getLineDelimiter();
		final int n = 6000;
		for (int i = 0; i < n; i++) {
			docAdapter.replaceTextRange(docAdapter.getCharCount(), 0, lineDelimiter);
		}
		assertNumberOfLines(docAdapter, n + 1);
		docAdapter.setWidth(-1);
		assertNumberOfLines(docAdapter, n + 1);
		clearDocument(docAdapter);
	}

	private static void assertContent(IDocumentAdapter docAdapter, String content) {
		assertEquals("Adapter returned wrong content.", content, docAdapter.getTextRange(0, docAdapter.getCharCount()));
	}

	private static void assertNumberOfLines(IDocumentAdapter docAdapter, int expectedLines) {
		assertEquals("Adapter has wrong line count.", expectedLines, docAdapter.getLineCount());
	}

	private static void assertLine(IDocumentAdapter docAdapter, int lineIndex, String expectedContent) {
		assertEquals("Adapter returned wrong content for line " + lineIndex + ".", expectedContent, docAdapter.getLine(lineIndex));
	}

	/**
	 * Goes through every line in adapted document and checks if value of
	 * {@link IDocumentAdapter#getOffsetAtLine(int)} for a line returns the same
	 * line again if put into {@link IDocumentAdapter#getLineAtOffset(int)}.
	 * <p>
	 * If random source is provided test is more intensive and to some extend
	 * tests {@link IDocumentAdapter#getLine(int)}.
	 * </p>
	 *
	 * @param docAdapter document adapter to test
	 * @param rand Optional. If provided will request line from random offset of
	 *            this line instead of first and check lines in random order.
	 */
	private static void checkLineMapping(IDocumentAdapter docAdapter, Random rand) {
		final int n = docAdapter.getLineCount();
		final List<Integer> lines = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			lines.add(i);
		}
		if (rand != null) {
			Collections.shuffle(lines, rand);
		}
		for (int lineIndex : lines) {
			int offset = docAdapter.getOffsetAtLine(lineIndex);
			if (rand != null) {
				int lineLength = docAdapter.getLine(lineIndex).length();
				if (lineLength > 0) {
					offset += rand.nextInt(lineLength);
				}
			}
			final int testLineIndex = docAdapter.getLineAtOffset(offset);
			assertEquals("Got wrong line with offset " + offset + ".", lineIndex, testLineIndex);
		}
	}

	private static void clearDocument(IDocumentAdapter docAdapter) {
		docAdapter.setText("");
		assertEquals("Document not cleared.", 0, docAdapter.getCharCount());
		assertNumberOfLines(docAdapter, 1);
		assertLine(docAdapter, 0, "");
	}

	private static class TextEventExpectation {
		// common attributes
		/** Expected text change event type. */
		public final TextChangeEventType type;

		// text changing only attributes
		/**
		 * Expected value of {@link TextChangingEvent#start} or
		 * <code>null</code> to ignore.
		 */
		public final Integer start;
		/**
		 * Expected value of {@link TextChangingEvent#newText} or
		 * <code>null</code> to ignore.
		 */
		public final String newText;
		/**
		 * Expected value of {@link TextChangingEvent#replaceCharCount} or
		 * <code>null</code> to ignore.
		 */
		public final Integer replaceCharCount;
		/**
		 * Expected value of {@link TextChangingEvent#newCharCount} or
		 * <code>null</code> to ignore.
		 */
		public final Integer newCharCount;
		/**
		 * Expected value of {@link TextChangingEvent#replaceLineCount} or
		 * <code>null</code> to ignore.
		 */
		public final Integer replaceLineCount;
		/**
		 * Expected value of {@link TextChangingEvent#newLineCount} or
		 * <code>null</code> to ignore.
		 */
		public final Integer newLineCount;

		public TextEventExpectation(TextChangeEventType type) {
			this(type, null, null, null, null, null, null);
		}

		public TextEventExpectation(TextChangeEventType type, Integer start, String newText, Integer replaceCharCount, Integer newCharCount, Integer replaceLineCount, Integer newLineCount) {
			super();
			this.type = type;
			this.start = start;
			this.newText = newText;
			this.replaceCharCount = replaceCharCount;
			this.newCharCount = newCharCount;
			this.replaceLineCount = replaceLineCount;
			this.newLineCount = newLineCount;
		}
	}

	/**
	 * A {@link TextChangeListener} which has some expectations about generated
	 * text change events and is not gladly disappointed.
	 */
	private static class ExpectingTextChangeListener implements TextChangeListener {

		final Queue<TextEventExpectation> expectations = new LinkedList<>();
		final boolean allowUnexpectedEvents;
		/**
		 * The {@link IDocumentAdapter} generating the events. If set some
		 * additional checks are performed.
		 */
		final IDocumentAdapter docAdapter;
		/**
		 * If all events are checked ({@link #allowUnexpectedEvents} = false)
		 * and {@link #docAdapter} is set do some additional validations.
		 */
		TextChangingEvent lastEvent;
		int eventLineBeforeChange = -1;
		int linesBeforeReplace = -1;
		int lengthBeforeReplace = -1;

		public ExpectingTextChangeListener(boolean allowUnexpectedEvents, IDocumentAdapter docAdapter) {
			super();
			this.allowUnexpectedEvents = allowUnexpectedEvents;
			this.docAdapter = docAdapter;
		}

		public void addExpectation(TextEventExpectation expectation) {
			expectations.add(expectation);
		}

		@Override
		public void textChanging(TextChangingEvent event) {
			final TextEventExpectation expectation = checkCommon(TextChangeEventType.CHANGING);
			if (expectation != null) {
				if (expectation.start != null) {
					assertEquals("event.start", (int) expectation.start, event.start);
				}
				if (expectation.newText != null) {
					assertEquals("event.newText", expectation.newText, event.newText);
				}
				if (expectation.replaceCharCount != null) {
					assertEquals("event.replaceCharCount", (int) expectation.replaceCharCount, event.replaceCharCount);
				}
				if (expectation.newCharCount != null) {
					assertEquals("event.newCharCount", (int) expectation.newCharCount, event.newCharCount);
				}
				if (expectation.replaceLineCount != null) {
					assertEquals("event.replaceLineCount", (int) expectation.replaceLineCount, event.replaceLineCount);
				}
				if (expectation.newLineCount != null) {
					assertEquals("event.newLineCount", (int) expectation.newLineCount, event.newLineCount);
				}
			}

			if (!allowUnexpectedEvents && docAdapter != null) {
				lastEvent = event;
				eventLineBeforeChange = docAdapter.getLineAtOffset(event.start);
				linesBeforeReplace = docAdapter.getLineCount();
				lengthBeforeReplace = docAdapter.getCharCount();

				final int charactersBehindEventStart = docAdapter.getCharCount() - event.start;
				assertTrue("Tried to remove more characters than available.", event.replaceCharCount <= charactersBehindEventStart);

				final int linesBehindEvent = docAdapter.getLineCount() - docAdapter.getLineAtOffset(event.start);
				assertTrue("Tried to remove more lines than available.", event.replaceLineCount <= linesBehindEvent);
			}
		}

		@Override
		public void textChanged(TextChangedEvent event) {
			checkCommon(TextChangeEventType.CHANGED);

			if (docAdapter != null && lastEvent != null) {
				final int lastEventOffset = lastEvent.start;
				final int lastEventLineIndex = docAdapter.getLineAtOffset(lastEventOffset);
				assertTrue("Line of event offset " + lastEventOffset + " has moved up.", eventLineBeforeChange <= lastEventLineIndex);

				// check if predicted changes are correct
				final int predictedDocLength = lengthBeforeReplace - lastEvent.replaceCharCount + lastEvent.newCharCount;
				assertEquals("New widget length not as announce by changing event.", predictedDocLength, docAdapter.getCharCount());
				final int predictedDocLines = linesBeforeReplace - lastEvent.replaceLineCount + lastEvent.newLineCount;
				assertEquals("New widget line number not as announce by changing event.", predictedDocLines, docAdapter.getLineCount());
				assertEquals("Inserted text not found in document.", lastEvent.newText, docAdapter.getTextRange(lastEvent.start, lastEvent.newText.length()));
			}
		}

		@Override
		public void textSet(TextChangedEvent event) {
			checkCommon(TextChangeEventType.SET);
		}

		public boolean hasPendingExpections() {
			return !expectations.isEmpty();
		}

		private TextEventExpectation checkCommon(TextChangeEventType eventType) {
			final TextEventExpectation expectation = expectations.poll();
			if (expectation == null && allowUnexpectedEvents) {
				return null;
			}
			assertNotNull("Unexpected event.", expectation);
			if (expectation.type != eventType && allowUnexpectedEvents) {
				return null;
			}
			assertEquals("Wrong event type.", expectation.type, eventType);
			return expectation;
		}
	}

	public enum TextChangeEventType {
		CHANGING, CHANGED, SET,
	}
}
