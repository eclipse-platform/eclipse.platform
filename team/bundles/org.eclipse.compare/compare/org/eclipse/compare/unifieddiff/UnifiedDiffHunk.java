/*******************************************************************************
 * Copyright (c) 2026 SAP
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP - initial implementation
 *******************************************************************************/
package org.eclipse.compare.unifieddiff;

import java.util.Objects;

/**
 * Describes a unified-diff hunk resolved by the user. Its range identifies its
 * location in the editor document immediately before it is resolved.
 */
public final class UnifiedDiffHunk {

	/** How the hunk was resolved. */
	public enum Resolution {
		/** The compared/new hunk was selected. */
		ACCEPTED,
		/** The original/current hunk was retained. */
		REJECTED,
		/** The user manually removed or replaced the complete displayed hunk. */
		MANUALLY_DELETED
	}

	private final Resolution resolution;
	private final int line;
	private final int offset;
	private final int length;
	private final String editorCode;
	private final String comparedSourceCode;

	public UnifiedDiffHunk(Resolution resolution, int line, int offset, int length, String editorCode,
			String comparedSourceCode) {
		this.resolution = Objects.requireNonNull(resolution);
		this.line = line;
		this.offset = offset;
		this.length = length;
		this.editorCode = Objects.requireNonNull(editorCode);
		this.comparedSourceCode = Objects.requireNonNull(comparedSourceCode);
	}

	/** Returns whether this hunk was accepted, rejected, or manually deleted. */
	public Resolution getResolution() {
		return resolution;
	}

	/** Returns the one-based line at which this hunk starts before it is resolved. */
	public int getLine() {
		return line;
	}

	/** Returns the offset of this hunk before it is resolved. */
	public int getOffset() {
		return offset;
	}

	/** Returns the length of this hunk before it is resolved. */
	public int getLength() {
		return length;
	}

	/** Returns the code in the editor document when this diff was created. */
	public String getEditorCode() {
		return editorCode;
	}

	/** Returns the code in the source compared with the editor document. */
	public String getComparedSourceCode() {
		return comparedSourceCode;
	}
}
