/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

 
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;

public class TagListener extends CommandOutputListener {

	@Override
	public IStatus messageLine(
		String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		
		// Received a warning in the form:
		// W folder/file : v1 already exists on version 1.2 : NOT MOVING tag to version 1.3
		// Indicate this as an error since no tagging was done
		if(line.length() >= 2 && line.charAt(0) == 'W' && line.charAt(1) == ' ') {
			return new CVSStatus(IStatus.ERROR, CVSStatus.TAG_ALREADY_EXISTS, line.substring(2), commandRoot);
		}
			
		return OK;
	}

	@Override
	public IStatus errorLine(
		String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor) {
			
		// Ignore the lines: Tagging folder1/folder2
		String serverMessage = getServerMessage(line, location);
		if ((serverMessage != null) && serverMessage.startsWith("Tagging")) { //$NON-NLS-1$
			return OK;
		}
		String rtagMessage = getServerRTagMessage(line, location);
		if(rtagMessage != null && rtagMessage.startsWith("Tagging") ) { //$NON-NLS-1$
			return OK;
		}
			
		return super.errorLine(line, location, commandRoot, monitor);
	}
	
}
