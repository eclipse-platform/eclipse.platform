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

/**
 * Receives a notification when a unified-diff hunk is resolved or an all-diffs
 * action completes.
 */
@FunctionalInterface
public interface UnifiedDiffEventListener {

	/**
	 * Invoked on the UI thread when a unified-diff hunk is resolved or an
	 * all-diffs action completes.
	 *
	 * @param event the resolved hunk or completed all-diffs action
	 */
	void unifiedDiffResolved(UnifiedDiffEvent event);
}
