/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Operation which checks for the existence of the .project file
 * in a remote folder, or to retrieve the project name for one or more
 * folders based on what is in the .project file.
 * 
 * To check for meta file exitence, the operation can be run 
 * by executing the operation and then checking <code>metaFileExists</code>
 * Use the retrieveContent as false in the constructor, to avoid the 
 * overhead of retrieving the file content too.
 * 
 * To update the folders with project names, the operation can be run
 * by calling the static method <code>updateFoldersWithProjectName</code> 
 * or by executing the operation and then checking <code>getUpdatedFolders</code>
 * to retrieve updated folders.
 * Use the retrieveContent as true in the constructor to retrieve the content.
 * 
 * The <code>metaFileExists</code> flag is always updated regardless of the
 * retrieveContent constructor argument value
 */
public class ProjectMetaFileOperation extends CVSOperation {

	private ICVSRemoteFolder[] remoteFolders;
	private boolean metaFileExists;
	private boolean retrieveContent;
	
	/*
	 * Update a list of folders with their project names
	 * for those folders that have one.
	 */
	public static ICVSRemoteFolder[] updateFoldersWithProjectName(IWorkbenchPart part, ICVSRemoteFolder[] folders)
			throws InvocationTargetException, InterruptedException {
		ProjectMetaFileOperation op = new ProjectMetaFileOperation(part, folders, true /*retrieve metafile content*/);
		op.run();
		return op.getUpdatedFolders();
	}

	public ProjectMetaFileOperation(IWorkbenchPart part, ICVSRemoteFolder[] remoteFolders, boolean retrieveContent) {
		super(part);
		this.remoteFolders = remoteFolders;
		this.retrieveContent = retrieveContent;
	}
	
	/*
	 * Update the folders with a project name if the provided remote folder contains a non empty project name
	 * in its meta-file (i.e. .project file) 
	 * Set the metafile existence to true as needed
	 */
	private void checkForMetafileAndUpdateFoldersWithRemoteProjectName(ICVSRemoteFolder[] folders, IProgressMonitor monitor) throws CVSException {
		metaFileExists = false;
		monitor.beginTask(null, folders.length*100);
		for (int i = 0; i < folders.length; i++) {
			// make a copy of the folder so that we will not affect the original
			// folder when we refetch the members
			// TODO: this is a strange thing to need to do. We should fix this.
			ICVSRemoteFolder folder = (ICVSRemoteFolder) folders[i].forTag(folders[i].getTag());
		
			try {
				folder.members(Policy.subMonitorFor(monitor, 50));
			} catch (TeamException e) {
				throw CVSException.wrapException(e);
			}
			// Check for the existance of the .project file
			// and attempt to create an IProjectDescription of it
			// and extract the project name
			InputStream in = null;
			try {
				ICVSRemoteFile remote = (ICVSRemoteFile) folder.getFile(".project"); //$NON-NLS-1$
				//if we have gone so far, then a metafile exists.
				metaFileExists = true;
				// retrieve the file content optionally, if requested
				if (retrieveContent && folder instanceof RemoteFolder) {
					RemoteFolder rf = (RemoteFolder) folder;
					
					//load the project description from the retrieved metafile
					in = remote.getContents(Policy.subMonitorFor(monitor, 50));
					if (in == null || monitor.isCanceled()) {
						break;
					}
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IProjectDescription projectDesc = workspace.loadProjectDescription(in);
					
					//clone the remote folder into a remote project folder
					//set the project name
					RemoteProjectFolder rpf = new RemoteProjectFolder(rf, projectDesc.getName()); 
					// ... and update back our folder
					folders[i] = rpf;
				}
			} catch (TeamException e) {
				// We couldn't retrieve the project meta file so assume it doesn't
				// exist
			} catch (CoreException e) {
				// We couldn't read the project description, so assume the
				// metafile is not a metafile, or is incorrect
				// which is as if it does not exist
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					// ignore : we cannot read the file, so it's like it is not there
				}
			}
		}
		monitor.done();
	}

	
	@Override
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		checkForMetafileAndUpdateFoldersWithRemoteProjectName(remoteFolders, monitor);
	}
	
	/**
	 * Return true if the meta file exists remotely. This method should only be invoked
	 * after the operation has been executed;
	 * @return
	 */
	public boolean metaFileExists() {
		return metaFileExists;
	}

	/**
	 * @return the updated folders with project name from the remote project meta
	 *         information if the .project file was properly retrieved or the
	 *         unmodified folders if retrieval failed. This method should only be
	 *         invoked after the operation has been executed;
	 */
	public ICVSRemoteFolder[] getUpdatedFolders() {
		return remoteFolders;
	}

	@Override
	protected String getTaskName() {
		return CVSUIMessages.ProjectMetaFile_taskName;
	}

	@Override
	public boolean canRunAsJob() {
		// This operation should never be run in the background.
		return false;
	}
}
