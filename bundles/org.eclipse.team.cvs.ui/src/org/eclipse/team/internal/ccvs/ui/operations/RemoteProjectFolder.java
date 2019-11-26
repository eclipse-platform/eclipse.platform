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
 *     Philippe Ombredanne - bug 84808
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;

/**
 * This  specialized RemoteFolder represents a RemoteFolder that contains
 * a .project metafile and has an additional field representing the project
 * name retrieved from this .project metafile
 */
public class RemoteProjectFolder extends RemoteFolder {

	protected String projectName;

	/**
	 * The Constructor for the RemoteProjectFolder
	 * @param folder the original RemoteFolder to 'clone'
	 * @param projectName the project name retrieved from the project metafile
	 */
	public RemoteProjectFolder(RemoteFolder folder, String projectName) {
		super((RemoteFolder) folder.getParent(), folder.getName(), folder.getRepository(),  
				folder.getRepositoryRelativePath(), folder.getTag(), folder.getFolderSyncInfo().getIsStatic());
		this.projectName = projectName;
	}

	/**
	 * @return true is the project name has been set and is not null or empty, false otherwise.
	 */
	public boolean hasProjectName() {
		if (isProjectNameEmpty()) 
			return false;
		return true;
	}

	/**
	 * @return the project name derived from the project description The name is guaranteed to be a null or a non empty string
	 */
	public String getProjectName() {
		if (isProjectNameEmpty())
			return null;
		return projectName;
	}
	
	private boolean isProjectNameEmpty() {
		return projectName == null || projectName.isEmpty();
	}
}
