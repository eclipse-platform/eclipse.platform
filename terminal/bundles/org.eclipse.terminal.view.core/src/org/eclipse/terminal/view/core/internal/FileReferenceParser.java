/*******************************************************************************
 * Copyright (c) 2026 Fabrizio Iannetti and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.view.core.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses terminal hover text to extract a {@link FileReference} containing
 * the file path and optional line and column numbers.
 * <p>
 * Supported formats:
 * <ul>
 *   <li>{@code path}</li>
 *   <li>{@code path:}</li>
 *   <li>{@code path:line}</li>
 *   <li>{@code path:line:}</li>
 *   <li>{@code path:line:column}</li>
 *   <li>{@code path:line:column:}</li>
 * </ul>
 * Line and column numbers must be strictly positive (≥ 1).
 * Windows drive letters ({@code C:\...}) are correctly distinguished
 * from the {@code :line} suffix.
 * </p>
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * </p>
 */
public class FileReferenceParser {

	private static final Pattern PATH_LINE_COL_PATTERN = Pattern.compile("^(.+):([1-9]\\d*):([1-9]\\d*):?$"); //$NON-NLS-1$
	private static final Pattern PATH_LINE_PATTERN = Pattern.compile("^(.+):([1-9]\\d*):?$"); //$NON-NLS-1$
	private static final String COLON = ":"; //$NON-NLS-1$

	private FileReferenceParser() {
	}

	/**
	 * Parses the given text into a {@link FileReference}.
	 * <p>
	 * Never returns {@code null}. If no line/column suffix is detected,
	 * the result contains the whole input as the path with line and column
	 * set to {@code -1}.
	 * </p>
	 *
	 * @param text the hover selection text, may be {@code null}
	 * @return a {@link FileReference}, never {@code null}
	 */
	public static FileReference parse(String text) {
		if (text == null || text.isEmpty()) {
			return new FileReference(text, -1, -1);
		}
		Matcher m = PATH_LINE_COL_PATTERN.matcher(text);
		if (m.matches()) {
			String path = m.group(1);
			int line = Integer.parseInt(m.group(2));
			int column = Integer.parseInt(m.group(3));
			if (!path.isEmpty()) {
				return new FileReference(path, line, column);
			}
		}
		m = PATH_LINE_PATTERN.matcher(text);
		if (m.matches()) {
			String path = m.group(1);
			int line = Integer.parseInt(m.group(2));
			if (!path.isEmpty()) {
				return new FileReference(path, line, -1);
			}
		}
		if (text.endsWith(COLON)) {
			text = text.substring(0, text.length() - COLON.length());
		}
		return new FileReference(text, -1, -1);
	}
}
