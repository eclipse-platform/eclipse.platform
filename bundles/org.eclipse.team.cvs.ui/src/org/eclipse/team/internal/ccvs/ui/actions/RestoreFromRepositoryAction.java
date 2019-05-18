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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Command.QuietOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.ResizableWizardDialog;
import org.eclipse.team.internal.ccvs.ui.wizards.RestoreFromRepositoryWizard;

/**
 * RestoreFromRepositoryAction allows to restore a file that has previously been
 * deleted from the CVS repository.
 */
public class RestoreFromRepositoryAction extends WorkspaceTraversalAction {

	private static final String ATTIC = "Attic/"; //$NON-NLS-1$
	private static final int ATTIC_LENGTH = ATTIC.length();

	/*
	 * This class handles the output from "cvs log -R ..." where -R
	 * indicates that only the RCS file name is to be returned. Files
	 * that have been deleted will be in the Attic. The Attic may also
	 * contains files that exist on a branch but not in HEAD
	 */
	class AtticLogListener extends CommandOutputListener {
		private static final String RCS_FILE_POSTFIX = ",v"; //$NON-NLS-1$
		private static final String LOGGING_PREFIX = "Logging "; //$NON-NLS-1$
		ICVSFolder currentFolder;
		List<ICVSFile> atticFiles = new ArrayList<>();
		
		@Override
		public IStatus messageLine(
					String line,
					ICVSRepositoryLocation location,
					ICVSFolder commandRoot,
					IProgressMonitor monitor) {
			
			// Extract the file name and path from the RCS path
			// String filePath = line.substring(index);
			// Find all RCS file names that contain "Attic"
			int start = line.lastIndexOf(Session.SERVER_SEPARATOR);
			if (start != -1) {
				String fileName = line.substring(start + 1);
				if (fileName.endsWith(RCS_FILE_POSTFIX)) {
					fileName = fileName.substring(0, fileName.length() - RCS_FILE_POSTFIX.length());
				}
				if (currentFolder != null) {
					try {
						ICVSFile file = currentFolder.getFile(fileName);
						if (!file.exists())
							atticFiles.add(file);
					} catch (CVSException e) {
						return e.getStatus();
					}
				} else {
					// Executed for every message line when in quiet mode.
					// See bug 238334
					CVSRepositoryLocation repo = (CVSRepositoryLocation)location;
					// If exists, remove root directory
					if (line.startsWith(repo.getRootDirectory())) {
						String repoPath = line.substring(repo.getRootDirectory().length());
						try {
							String cmdRootRelativePath = commandRoot.getRepositoryRelativePath();
							// Remove command root path
							String path = repoPath.substring(repoPath.indexOf(cmdRootRelativePath)	+ cmdRootRelativePath.length());
							// Remove filename at the end
							String folderPath = path.substring(0, path.indexOf(fileName));
							// The "raw" folderPath contains CVS's 'Attic/' segment when a file has been deleted from cvs.
							if (folderPath.endsWith(ATTIC)) {
								folderPath = folderPath.substring(0, folderPath.length() - ATTIC_LENGTH);
							}
							// A separator means the same as "current folder"
							if (folderPath.equals(Session.SERVER_SEPARATOR))
								folderPath = Session.CURRENT_LOCAL_FOLDER;
							ICVSFolder folder = commandRoot.getFolder(folderPath);
							ICVSFile file = folder.getFile(fileName);
							if (!file.exists())
								atticFiles.add(file);
						} catch (CVSException e) {
							return e.getStatus();
						}
					}
				}
			}
			return OK;
		}
		
		@Override
		public IStatus errorLine(
			String line,
			ICVSRepositoryLocation location,
			ICVSFolder commandRoot,
			IProgressMonitor monitor) {
			
			CVSRepositoryLocation repo = (CVSRepositoryLocation)location;
			String folderPath = repo.getServerMessageWithoutPrefix(line, SERVER_PREFIX);
			if (folderPath != null) {
				if (folderPath.startsWith(LOGGING_PREFIX)) {
					folderPath = folderPath.substring(LOGGING_PREFIX.length());
					try {
						currentFolder = commandRoot.getFolder(folderPath);
					} catch (CVSException e) {
						return e.getStatus();
					}
					return OK;
				}
			}
			return super.errorLine(line, location, commandRoot, monitor);
		}

		public ICVSFile[] getAtticFilePaths() {
			return atticFiles.toArray(new ICVSFile[atticFiles.size()]);
		}
	}
	
	@Override
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IContainer resource = (IContainer)getSelectedResources()[0];
		ICVSFile[] files = fetchDeletedFiles(resource);
		if (files == null) return;
		if (files.length == 0) {
			MessageDialog.openInformation(getShell(), CVSUIMessages.RestoreFromRepositoryAction_noFilesTitle, NLS.bind(CVSUIMessages.RestoreFromRepositoryAction_noFilesMessage, new String[] { resource.getName() })); //
			return;
		}
		RestoreFromRepositoryWizard wizard = new RestoreFromRepositoryWizard(resource, files);
		WizardDialog dialog = new ResizableWizardDialog(getShell(), wizard);
		dialog.setMinimumPageSize(1000, 250);
		dialog.open();
	}

	@Override
	public boolean isEnabled() {
		IResource[] resources = getSelectedResources();
		if (resources.length != 1) return false;
		if (resources[0].getType() == IResource.FILE) return false;
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor((IContainer)resources[0]);
		try {
			if (!folder.isCVSFolder()) return false;
		} catch (CVSException e) {
			return isEnabledForException(e);
		}
		return true;
	}
	
	private ICVSFile[] fetchDeletedFiles(final IContainer parent) {
		final ICVSFile[][] files = new ICVSFile[1][0];
		files[0] = null;
		try {
			run((IRunnableWithProgress) monitor -> {
				try {
					ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(parent);
					FolderSyncInfo info = folder.getFolderSyncInfo();
					ICVSRepositoryLocation location = KnownRepositories.getInstance().getRepository(info.getRoot());
					files[0] = fetchFilesInAttic(location, folder, monitor);
				} catch (CVSException e) {
					throw new InvocationTargetException(e);
				}
			}, true, PROGRESS_DIALOG);
		} catch (InvocationTargetException e) {
			handle(e);
		} catch (InterruptedException e) {
			return null;
		}
		return files[0];
	}
	
	/*
	 * Fetch the RCS paths (minus the Attic segment) of all files in the Attic.
	 * This path includes the repository root path.
	 */
	private ICVSFile[] fetchFilesInAttic(ICVSRepositoryLocation location, ICVSFolder parent, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		AtticLogListener listener = new AtticLogListener();
		Session session = new Session(location, parent, true /* output to console */);
		session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
		try {
			QuietOption quietness = CVSProviderPlugin.getPlugin().getQuietness();
			try {
				CVSProviderPlugin.getPlugin().setQuietness(Command.VERBOSE);
				IStatus status = Command.LOG.execute(
					session,
					Command.NO_GLOBAL_OPTIONS,
					new LocalOption[] { Log.RCS_FILE_NAMES_ONLY },
					new ICVSResource[] { parent }, listener,
					Policy.subMonitorFor(monitor, 90));
				if (status.getCode() == CVSStatus.SERVER_ERROR) {
					throw new CVSServerException(status);
				}
			} finally {
				CVSProviderPlugin.getPlugin().setQuietness(quietness);
				monitor.done();
			}
		} finally {
			session.close();
		}
		return listener.getAtticFilePaths();
	}
}
