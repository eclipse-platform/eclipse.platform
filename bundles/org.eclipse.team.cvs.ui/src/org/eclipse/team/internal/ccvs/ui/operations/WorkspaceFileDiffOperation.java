/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.ui.IWorkbenchPart;

public class WorkspaceFileDiffOperation extends FileDiffOperation {

	public WorkspaceFileDiffOperation(IWorkbenchPart part, ResourceMapping[] mappings, LocalOption[] options, File file, boolean isMultiPatch, boolean includeFullPathInformation, IPath patchRoot) {
		super(part, mappings, options, file, isMultiPatch, includeFullPathInformation, patchRoot);
	}
	
	protected void copyFile() throws CVSException {
		
		IWorkspaceRoot root =ResourcesPlugin.getWorkspace().getRoot();
		String filePath = this.file.getPath();
		IFile finalFile = root.getFileForLocation(new Path(filePath));
		if(finalFile == null) {
			throw new CVSException("File '" + filePath + "' can not be found in workspace."); //$NON-NLS-1$ //$NON-NLS-2$ 
		}
		InputStream fileInputStream = null;
		try {
			fileInputStream = new BufferedInputStream(new FileInputStream(tempFile));
			if(!finalFile.exists()) {
				finalFile.create(fileInputStream, IResource.FORCE , null);
			} else {
				finalFile.setContents(fileInputStream, IResource.FORCE, null);
			}	
		} catch (FileNotFoundException e) {
			throw CVSException.wrapException(e); 
		} catch (CoreException e) {
			throw CVSException.wrapException(e); 
		}
		finally{
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					// Ignore
				} finally {					
					if (tempFile != null)
						tempFile.delete();
				}
			}
		}
	}

}
