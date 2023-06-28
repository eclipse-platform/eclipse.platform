/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui;


/**
 * Implementors of this interface are notified whenever there is a change is
 * made to the launch history. This could be an addition or deletion from either
 * of the run or debug histories, a change to the last launched item, or a
 * change in favorites.
 */
public interface ILaunchHistoryChangedListener {

	/**
	 * Notification that the launch history has changed.  Any of the run history, debug history
	 * or last launched items could have changed.  To examine the history items, retrieve them
	 * from the <code>DebugUIPlugin</code>.
	 */
	void launchHistoryChanged();
}
