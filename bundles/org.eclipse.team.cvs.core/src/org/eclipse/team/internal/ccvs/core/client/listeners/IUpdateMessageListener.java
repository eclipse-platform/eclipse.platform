/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.client.listeners;
 
import org.eclipse.team.internal.ccvs.core.ICVSFolder;

/**
 * This listener is used by RemoteFolder to listener for E and M messages
 * from the CVS server in order to determine the files and folders contained in a parent folder.
 */
public interface IUpdateMessageListener {
	/**
	 * Notification that a directory (which may or may not have been reported by 
	 * directoryInformation()) does not exist.
	 * 
	 * @param commandRoot the root directory of the command
	 * @param path the path of the directory relative to the commandRoot
	 */
	public void directoryDoesNotExist(ICVSFolder commandRoot, String path);
	/**
	 * Notification of information about a directory.
	 * 
	 * @param commandRoot the root directory of the command
	 * @param path the path of the directory relative to the commandRoot
	 * @param newDirectory true if the directory does not exist locally (i.e. in the commandRoot hierarchy)
	 */
	public void directoryInformation(ICVSFolder commandRoot, String path, boolean newDirectory);
	/**
	 * Notification of information about a file
	 * 
	 * @param type the type of update for the file (see Update for type constants)
	 * @param commandRoot the root directory of the command
	 * @param filename the path of the file relative to the commandRoot
	 */
	public void fileInformation(int type, ICVSFolder parent, String filename);
	/**
	 * Notification that a file does not exists remotely
	 * 
	 * @param commandRoot the root directory of the command
	 * @param filename the path of the file relative to the commandRoot
	 */
	public void fileDoesNotExist(ICVSFolder parent, String filename);
}
