/*******************************************************************************
 * Copyright (c) 2023 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.compare.contentmergeviewer.IIgnoreWhitespaceContributor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.junit.Assert;

public class SimpleIgnoreWhitespaceContributor implements IIgnoreWhitespaceContributor {

	private final IDocument document;
	private TreeMap<Integer /* start offset */, Integer /* end offset */> literalsByOffset = new TreeMap<>();

	public SimpleIgnoreWhitespaceContributor(IDocument document) {
		this.document = document;
		scanAndCreateLiteralsByOffset();
	}

	private void scanAndCreateLiteralsByOffset() {
		String s = document.get();
		boolean inStringLiteral = false;
		int start = 0, end = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' && inStringLiteral) {
				i++;
			} else if (c == '"') {
				inStringLiteral = !inStringLiteral;
				if (inStringLiteral) {
					start = i;
				} else {
					end = i;
					literalsByOffset.put(start, end);
				}
			}
		}
	}

	@Override
	public boolean isIgnoredWhitespace(int lineNumber, int columnNumber) {
		try {
			int offset = document.getLineOffset(lineNumber) + columnNumber;
			Entry<Integer, Integer> entry = literalsByOffset.floorEntry(offset);
			if (entry != null) {
				int start = entry.getKey();
				int end = entry.getValue();
				if (offset >= start && offset <= end) {
					return false; // part of literal - whitespace cannot be ignored
				}
			}
		} catch (BadLocationException e) {
			Assert.fail("BadLocationException not expected");
		}
		return true;
	}
}
