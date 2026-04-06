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
 * SAP - initial implementation
 *******************************************************************************/
package org.eclipse.compare.unifieddiff;

public class UnifiedDiffMode {
	/**
	 * Diffs are directly applied in the editor. Users have the possibility to keep
	 * or undo the applied diffs.
	 */
	public static final UnifiedDiffMode REPLACE_MODE = new UnifiedDiffMode("REPLACE_MODE"); //$NON-NLS-1$
	/**
	 * The source in the editor is not modified. Diffs are shown as code mining and
	 * users have the possibility to apply or cancel individual diffs.
	 */
	public static final UnifiedDiffMode OVERLAY_MODE = new UnifiedDiffMode("OVERLAY_MODE"); //$NON-NLS-1$
	/**
	 * The source in the editor is not modified. Diffs are shown as code mining and
	 * users cannot apply or dismiss the diffs (read-only mode).
	 */
	public static final UnifiedDiffMode OVERLAY_READ_ONLY_MODE = new UnifiedDiffMode("OVERLAY_READ_ONLY_MODE"); //$NON-NLS-1$

	private final String name;

	private UnifiedDiffMode(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
