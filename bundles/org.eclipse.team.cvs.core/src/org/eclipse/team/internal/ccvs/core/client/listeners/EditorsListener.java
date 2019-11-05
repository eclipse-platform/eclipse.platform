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
 *     CSC - Intial implementation
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client.listeners;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.EditorsInfo;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;


/**
 * 
 *
 * Listener for the Editors command
 * 
 * @author <a href="mailto:kohlwes@gmx.net">Gregor Kohlwes</a>
 * 
 */
public class EditorsListener extends CommandOutputListener {
	/**
	 *  List to store the EditorsInfos
	 */
	private List<EditorsInfo> infos = new LinkedList<>();
	
	/**
	 *  Name of the current file 
	 */
	private String fileName;

	/**
	 * Constructor EditorsListener.
	 */
	public EditorsListener() {
	}

	@Override
	public IStatus messageLine(
		String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor) {

		// If there is a file with multiple editors
		// then cvs will write the filename only 
		// in the first line and the following
		// line will start with a Tab
		if (line.startsWith("\t")) { //$NON-NLS-1$
			line = fileName + line;
		}
		EditorsInfo info = new EditorsInfo();
		StringTokenizer tokenizer = new StringTokenizer(line,"\t"); //$NON-NLS-1$
		int i = 0;
		while(tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			switch (i) {
				case 0:
					info.setFileName(token);
					fileName = token;
					break;
				case 1:
					info.setUserName(token);
					break;
				case 2:
					info.setDateString(token);
					break;
				case 3:
					info.setComputerName(token);
					break;
				default :
					break;
			}
			i++;			
		}
		
		infos.add(info);
		return OK;

	}
	/**
	 * Method getEditorsInfos.
	 * @return IEditorsInfo[]
	 */
	public EditorsInfo[] getEditorsInfos() {
		return infos.toArray(new EditorsInfo[infos.size()]);
	}

}
