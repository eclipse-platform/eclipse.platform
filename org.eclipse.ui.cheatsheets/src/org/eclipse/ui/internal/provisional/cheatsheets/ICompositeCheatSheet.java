/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.provisional.cheatsheets;

import org.eclipse.ui.cheatsheets.ICheatSheetManager;

/**
 * Interface representing a composite cheatsheet.
 * A composite cheat sheet has a single root task, each task may have
 * zero or more children.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */

public interface ICompositeCheatSheet {

	/**
	 * The ID of the task explorer that should be the
	 * default for this composite cheat sheet. Can be null.
	 * @return the task explorer identifier or <code>null</code> if
	 * the view's default should be used.
	 */
	String getTaskExplorerId();

	/**
	 * The root task of this composite cheat sheet
	 * @return the root task object
	 */
	ICompositeCheatSheetTask getRootTask();

	/**
	 * Get the manager which allows data to be shared between tasks.
	 * @return the CheatSheetManager for this composite cheat cheet. May not be null.
	 */
	ICheatSheetManager getCheatSheetManager();
}
