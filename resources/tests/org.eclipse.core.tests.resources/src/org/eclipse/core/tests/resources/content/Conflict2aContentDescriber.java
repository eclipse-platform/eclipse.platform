/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.content;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.ITextContentDescriber;

/**
 * A content describer that looks for "conflict2a" at the beginning of the
 * stream as used by
 * org.eclipse.core.tests.resources.content.IContentTypeManagerTest.testFileSpecConflicts()
 */
public class Conflict2aContentDescriber implements ITextContentDescriber {

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		final byte[] expected = "conflict2a".getBytes(StandardCharsets.UTF_8);
		byte[] actual = new byte[expected.length];
		int read = contents.read(actual);
		if (read == actual.length && Arrays.equals(expected, actual)) {
			return VALID;
		}
		return INVALID;
	}

	@Override
	public int describe(Reader contents, IContentDescription description) throws IOException {
		throw new UnsupportedOperationException("Not used by test");
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return IContentDescription.ALL;
	}
}
