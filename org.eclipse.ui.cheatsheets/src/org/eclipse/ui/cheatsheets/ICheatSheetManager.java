/*******************************************************************************
 *  Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.cheatsheets;

import java.util.Set;

/**
 * Manages the running of a cheat sheet.
 * <p>
 * Each cheat sheet that is opened in the UI is assigned its own cheat sheet
 * manager, which stays with it until the cheat sheet is completed (or
 * restarted). The cheat sheet manager is passed as a parameter to cheat
 * sheet-aware actions which implement {@link ICheatSheetAction}. The manager
 * carries arbitrary key-value data (strings) for the lifetime of a cheat sheet,
 * and can be accessed via {@link #getData(String)}and
 * {@link #setData(String, String)}. If the workbench is shut down while the
 * cheat sheet is in progress, this data will generally be saved and later
 * restored when the workbench is restarted and cheat sheet is resumed. The
 * manager also supports a {@link CheatSheetListener}(specified via the
 * "listener" attribute of the "cheatsheet" element in the cheat sheet content
 * file), which is kept informed of life cycle events over the course of the
 * cheat sheet's life time.
 * </p>
 *
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ICheatSheetManager {

	/**
	 * Returns the id of the cheat sheet managed by this manager.
	 *
	 * @return the cheat sheet id
	 */
	public String getCheatSheetID();

	/**
	 * Returns the data value associated with the given key.
	 *
	 * @param key the key
	 * @return the string data associated with the key, or
	 * <code>null</code> none
	 * @exception IllegalArgumentException if <code>key</code>
	 * is <code>null</code>
	 */
	public String getData(String key);

	/**
	 * Sets the data value associated with the given key.
	 * <p>
	 * Data associated with a cheat sheet manager is remembered
	 * for the life of the manager. All data is discarded when
	 * the cheat sheet is completed (or restarted).
	 * </p>
	 *
	 * @param key the key
	 * @param data the string data associated with the key,
	 * or <code>null</code> to remove
	 * @exception IllegalArgumentException if <code>key</code>
	 * is <code>null</code>
	 */
	public void setData(String key, String data);

	/**
	 * Get the cheat sheet manager for the enclosing composite cheat sheet.
	 * @return The cheat sheet manager for the composite cheat sheet which contains
	 * this cheat sheet as a task or <code>null</code> if this cheatsheet was not
	 * opened as a subtask of a composite cheat sheet.
	 * @since 3.2
	 */
	public ICheatSheetManager getParent();

	/**
	 * Get the keys for the data in this cheat sheet manager
	 * @return The set of keys.
	 * @since 3.2
	 */
	public Set<String> getKeySet();
}
