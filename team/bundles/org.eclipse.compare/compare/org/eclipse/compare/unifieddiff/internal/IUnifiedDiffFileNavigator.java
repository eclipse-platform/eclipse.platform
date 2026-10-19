/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.unifieddiff.internal;

/**
 * Moves a unified diff that spans several files from one file to the next.
 * Attached to the viewer of the file currently shown, see
 * {@link UnifiedDiffManager#setFileNavigator}.
 */
public interface IUnifiedDiffFileNavigator {

	/**
	 * Called when Next or Previous runs past the last or first diff of the file.
	 * Returns <code>false</code> to wrap around within the file instead.
	 */
	boolean endReached(boolean next);

	/** Called once every diff of the file has been kept, reverted or hidden. */
	void diffsFinished();

	/** Whether the file was reached going backwards, so that its last diff is revealed first. */
	boolean startsAtLastDiff();
}
