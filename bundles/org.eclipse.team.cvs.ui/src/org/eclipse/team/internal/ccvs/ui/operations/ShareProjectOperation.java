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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;

/**
 * Create a folder and any missing parents in the repository
 */
public class ShareProjectOperation extends CVSOperation {

	private ICVSRepositoryLocation location;
	private IProject project;
	private String moduleName;
	private Shell shell;

	public ShareProjectOperation(Shell shell, ICVSRepositoryLocation location, IProject project, String moduleName) {
		super(null);
		this.shell = shell;
		this.moduleName = moduleName;
		this.project = project;
		this.location = location;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		try {
			monitor.beginTask(getTaskName(), 100);
			// Create the remote module
			final ICVSRemoteFolder remote = createRemoteFolder(Policy.subMonitorFor(monitor, 50));
			// Map the project to the module in a workspace runnable
			final TeamException[] exception = new TeamException[] {null};
			ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) monitor1 -> {
				try {
					mapProjectToRemoteFolder(remote, monitor1);
				} catch (TeamException e) {
					exception[0] = e;
				}
			}, ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(project), 0, Policy.subMonitorFor(monitor, 100));
			if (exception[0] != null)
				throw exception[0];
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Create the remote folder to which the project is being mapped 
	 * (as well as any ancestors) and return it. If the remote folder does not
	 * exist remotely, this method will create it.
	 * @param monitor a progress monitor
	 * @return the existing remote folder to which the project is being mapped
	 * @throws CVSException
	 */
	protected ICVSRemoteFolder createRemoteFolder(IProgressMonitor monitor) throws CVSException {
		String projectName = project.getName();
		if (moduleName == null)
			moduleName = projectName;

		RemoteFolderTree root = new RemoteFolderTree(null, location, Path.EMPTY.toString(), null);
		Path path = new Path(null, moduleName);
		
		try {
			monitor.beginTask(getTaskName(), 100 * path.segmentCount());
			return ensureTreeExists(root, path, monitor);
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Map the project to the remote folder by associating the CVS
	 * Repository Provider with the project and, at the very least,
	 * assigning the folder sync info for the remote folder as the
	 * folder sync info for the project.
	 * @param remote the remote folder to which the project is being mapped
	 * @param monitor a progress monitor
	 * @throws CVSException
	 */
	protected void mapProjectToRemoteFolder(final ICVSRemoteFolder remote, IProgressMonitor monitor) throws TeamException {
		monitor.beginTask(null, IProgressMonitor.UNKNOWN);
		purgeAnyCVSFolders(Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
		// Link the project to the newly created module
		monitor.subTask(NLS.bind(CVSUIMessages.ShareProjectOperation_3, new String[] { project.getName(), remote.getRepositoryRelativePath() }));
		ICVSFolder folder = (ICVSFolder)CVSWorkspaceRoot.getCVSResourceFor(project);
		folder.setFolderSyncInfo(remote.getFolderSyncInfo());
		//Register it with Team.  If it already is, no harm done.
		RepositoryProvider.map(project, CVSProviderPlugin.getTypeId());
		monitor.done();
	}

	/*
	 * Create handles for all the children in the moduleName path
	 */
	private RemoteFolderTree createChild(RemoteFolderTree parent, String name, IProgressMonitor monitor) throws CVSException, TeamException {
		RemoteFolderTree child = new RemoteFolderTree(parent, name, location, new Path(null, parent.getRepositoryRelativePath()).append(name).toString(), null);
		parent.setChildren(new ICVSRemoteResource[] { child });
		if (child.exists(Policy.subMonitorFor(monitor, 50))) {
			// The child exists so get the handle that was received from the server
			return (RemoteFolderTree)parent.getFolder(name);
		} else {
			// Create the folder remotely
			createFolder(child, Policy.subMonitorFor(monitor, 50));
			return child;
		}
	}

	/*
	 * Ensure that all the folders in the tree exist
	 */
	private ICVSRemoteFolder ensureTreeExists(RemoteFolderTree folder, IPath path, IProgressMonitor monitor) throws TeamException {
		if (path.isEmpty()) return folder;
		String name = path.segment(0);
		RemoteFolderTree child = createChild(folder, name, monitor);
		return ensureTreeExists(child, path.removeFirstSegments(1), monitor);
	}
	
	private void createFolder(RemoteFolderTree folder, IProgressMonitor monitor) throws TeamException {
		Session s = new Session(location, folder.getParent());
		s.open(monitor, true /* open for modification */);
		try {
			IStatus status = Command.ADD.execute(s,
					Command.NO_GLOBAL_OPTIONS,
					Command.NO_LOCAL_OPTIONS,
					new String[] { folder.getName() },
					null,
					monitor);
			// If we get a warning, the operation most likely failed so check that the status is OK
			if (status.getCode() == CVSStatus.SERVER_ERROR  || ! status.isOK()) {
				throw new CVSServerException(status);
			}
		} finally {
			s.close();
		}
	}

	@Override
	protected String getTaskName() {
		return NLS.bind(CVSUIMessages.ShareProjectOperation_0, new String[] { project.getName(), moduleName }); 
	}

	/**
	 * @return Returns the project.
	 */
	public IProject getProject() {
		return project;
	}
	
	@Override
	public Shell getShell() {
		return shell;
	}
	
	/**
	 * Purge any CVS folders.
	 * 
	 * @param monitor a progress monitor
	 */
	private void purgeAnyCVSFolders(final IProgressMonitor monitor) {
		try {
			monitor.beginTask(null, IProgressMonitor.UNKNOWN);
			ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(project);
			folder.accept(new ICVSResourceVisitor() {
				@Override
				public void visitFile(ICVSFile file) throws CVSException {
					// nothing to do for files
				}
				@Override
				public void visitFolder(ICVSFolder folder) throws CVSException {
					monitor.subTask(NLS.bind(CVSUIMessages.ShareProjectOperation_2, new String[] { folder.getIResource().getFullPath().toString() } ));
					if (folder.isCVSFolder()) {
						// for now, just unmanage
						folder.unmanage(null);
					}
				}
			}, true /* recurse */);
		} catch (CVSException e) {
			// log the exception and return null
			CVSUIPlugin.log(e);
		} finally {
			monitor.done();
		}
	}
}
