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

import java.util.List;
import java.util.Objects;

/**
 * Describes one or more unified-diff hunks resolved by the user.
 * <p>
 * An event for an all-diffs action contains every hunk resolved by that action.
 */
public final class UnifiedDiffEvent {

	private final UnifiedDiff.ToolbarAction action;
	private final List<UnifiedDiffHunk> hunks;

	/**
	 * Creates an event for one or more resolved hunks.
	 *
	 * @param action the executed all-diffs action, or <code>null</code> for an
	 *            individual hunk resolution
	 * @param hunks the resolved hunks
	 */
	public UnifiedDiffEvent(UnifiedDiff.ToolbarAction action, List<UnifiedDiffHunk> hunks) {
		this.action = action;
		this.hunks = List.copyOf(Objects.requireNonNull(hunks));
		if (this.hunks.isEmpty()) {
			throw new IllegalArgumentException("Hunks cannot be empty"); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the executed all-diffs action, or <code>null</code> when this event
	 * describes an individual hunk resolution.
	 */
	public UnifiedDiff.ToolbarAction getAction() {
		return action;
	}

	/**
	 * Returns the resolved hunks. The returned list is immutable.
	 */
	public List<UnifiedDiffHunk> getHunks() {
		return hunks;
	}
}
