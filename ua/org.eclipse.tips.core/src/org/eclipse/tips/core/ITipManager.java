/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tips.core.internal.TipManager;

/**
 * The ITipManager interface.
 *
 */
public interface ITipManager {

	/**
	 * Indicates whether already read tips must be served or not.
	 *
	 * @return true or false
	 * @see TipManager#setServeReadTips(boolean)
	 */
	boolean mustServeReadTips();

	/**
	 * Consults TipManager to determine the Tip's read status.
	 *
	 * @param tip the tip to query for its read status
	 * @return true if the tip is read, false otherwise.
	 */
	boolean isRead(Tip tip);

	/**
	 * Instructs the TipManager to mark this tip as read.
	 *
	 * @param tip the tip to set as read.
	 * @return this
	 */
	ITipManager setAsRead(Tip tip);

	/**
	 * Central place of logging for the Tip Framework.
	 *
	 * @param status the {@link IStatus} which may not be null
	 * @return this
	 */
	ITipManager log(IStatus status);

	/**
	 * Binds the passed provider to this manager. After registration, ITipManager
	 * implementations should asynchronously call the
	 * {@link TipProvider#loadNewTips(org.eclipse.core.runtime.IProgressMonitor)}
	 * method.
	 *
	 * @param provider the {@link TipProvider} to register which may not be null.
	 * @return this
	 */
	ITipManager register(TipProvider provider);

	/**
	 * Opens the Tip of the Day dialog.
	 *
	 * @param startUp When called from a startup situation, true must be passed for
	 *                <code>startup</code>. If in a manual starting situation, false
	 *                must be passed. This enables the manager to decide to skip
	 *                opening the dialog at startup (e.g., no new tip items).
	 *
	 * @return this
	 *
	 * @see #isOpen()
	 */
	ITipManager open(boolean startUp);

	/**
	 * Returns the open state.
	 *
	 * @return true if this manager is open, false otherwise.
	 */
	boolean isOpen();

	/**
	 * Indicates if this manager has providers with unread tips. Be aware that
	 * subsequent calls to this method may return different results based on the
	 * async nature of loading providers.
	 *
	 * @return true if this {@link TipManager} has providers with tips.
	 * @see TipProvider#getTips()
	 */
	boolean hasContent();
}