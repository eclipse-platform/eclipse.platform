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
package org.eclipse.team.internal.ccvs.core.client;

 
import java.util.HashSet;
import java.util.Set;

import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;

/**
 * Visit the CVS file structure, only sending files if they are modified.
 */
class ModifiedFileSender extends FileStructureVisitor {

	private final Set modifiedFiles;
	
	public ModifiedFileSender(Session session, LocalOption[] localOptions) {
		super(session, localOptions, false, true);
		modifiedFiles = new HashSet();
	}
	
	/**
	 * Override sendFile to only send modified files
	 */
	protected void sendFile(ICVSFile mFile) throws CVSException {
		// Only send the file if its modified
		if (mFile.isManaged() && mFile.isModified(null)) {
			super.sendFile(mFile);
			modifiedFiles.add(mFile);
		}
	}
	
	protected String getSendFileMessage() {
		return null;
	}
	
	/**
	 * Return all the files that have been send to the server
	 */
	public ICVSFile[] getModifiedFiles() {
		return (ICVSFile[]) modifiedFiles.toArray(new ICVSFile[modifiedFiles.size()]);
	}
}
