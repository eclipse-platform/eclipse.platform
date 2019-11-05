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

 
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputListener;

/*
 * This class pares the output of the "cvs checkout -c" command which returns the list of modules 
 * defined in the CVSROOT/modules file.
 */
public class ModuleDefinitionsListener extends CommandOutputListener {

	// the last line read from the context (used to accumulate multi-line definitions)
	private String lastLine = ""; //$NON-NLS-1$
	
	private Map<String,String> moduleMap;
	
	public ModuleDefinitionsListener() {
		reset();
	}
	
	@Override
	public IStatus messageLine(
		String line,
		ICVSRepositoryLocation location,
		ICVSFolder commandRoot,
		IProgressMonitor monitor) {
		
		// Lines that start with a space indicate a multi line entry
		if( line.charAt(0) == ' ' ) {
			lastLine += line;
			line = lastLine;
		}
		else
			lastLine = line;
		
		// Use the module name as the key so that multi-line modules will be recorded properly
		int firstSpace = line.indexOf(" "); //$NON-NLS-1$
		if (firstSpace > -1) {
			String module = line.substring(0, firstSpace);
			moduleMap.put(module, line);
		}
		return OK;
	}
	
	public String[] getModuleExpansions() {
		return moduleMap.values().toArray(new String[moduleMap.size()]);
	}

	public void reset() {
		this.moduleMap = new HashMap<>();
	}
}
