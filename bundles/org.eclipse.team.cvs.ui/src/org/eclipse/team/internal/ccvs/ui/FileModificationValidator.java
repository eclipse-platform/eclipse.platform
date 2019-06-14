/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.ui.actions.EditorsAction;
import org.eclipse.team.internal.ccvs.ui.operations.UpdateOperation;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * IFileModificationValidator that is plugged into the CVS Repository Provider
 */
public class FileModificationValidator extends CVSCoreFileModificationValidator {
	
	public FileModificationValidator() {
	}
	
	@Override
	protected IStatus edit(IFile[] readOnlyFiles, FileModificationValidationContext context) {
		return edit(readOnlyFiles, getShell(context));
	}
	
	private Shell getShell(FileModificationValidationContext context) {
		if (context == null)
			return null;
		if (context.getShell() != null)
			return (Shell)context.getShell();
		return Utils.getShell(null, true);
	}

	private IStatus getStatus(InvocationTargetException e) {
		Throwable target = e.getTargetException();
		if (target instanceof TeamException) {
			return ((TeamException) target).getStatus();
		} else if (target instanceof CoreException) {
			return ((CoreException) target).getStatus();
		}
		return new Status(IStatus.ERROR, CVSUIPlugin.ID, 0, CVSUIMessages.internal, target); 
	}
		
	private IStatus edit(final IFile[] files, final Shell shell) {
		if (isPerformEdit()) {
			try {
				if (shell != null && !promptToEditFiles(files, shell)) {
					// The user didn't want to edit.
					// OK is returned but the file remains read-only
					throw new InterruptedException();
				}
				
				// see if the file is up to date
				if (shell != null && promptToUpdateFiles(files, shell)) {
					// The user wants to update the file
					// Run the update in a runnable in order to get a busy cursor.
					// This runnable is syncExeced in order to get a busy cursor
					IRunnableWithProgress updateRunnable = monitor -> performUpdate(files, monitor);
					if (isRunningInUIThread()) {
						// Only show a busy cursor if validate edit is blocking the UI
						CVSUIPlugin.runWithProgress(shell, false, updateRunnable);
					} else {
						// We can't show a busy cursor (i.e., run in the UI thread)
						// since this thread may hold locks and
						// running an edit in the UI thread could try to obtain the
						// same locks, resulting in a deadlock.
						updateRunnable.run(new NullProgressMonitor());
					}
				}
				
				// Run the edit in a runnable in order to get a busy cursor.
				// This runnable is syncExeced in order to get a busy cursor
				IRunnableWithProgress editRunnable = monitor -> {
					try {
						performEdit(files, monitor);
					} catch (CVSException e) {
						throw new InvocationTargetException(e);
					}
				};
				if (isRunningInUIThread()) {
					// Only show a busy cursor if validate edit is blocking the UI
					CVSUIPlugin.runWithProgress(shell, false, editRunnable);
				} else {
					// We can't show a busy cursor (i.e., run in the UI thread)
					// since this thread may hold locks and
					// running an edit in the UI thread could try to obtain the
					// same locks, resulting in a deadlock.
					editRunnable.run(new NullProgressMonitor());
				}
			} catch (InvocationTargetException e) {
				return getStatus(e);
			} catch (InterruptedException e) {
				// Must return an error to indicate that it is not OK to edit the files
				return new Status(IStatus.CANCEL, CVSUIPlugin.ID, 0, CVSUIMessages.FileModificationValidator_vetoMessage, null); //;
			}
		} else if (isPerformEditInBackground()) {
			IStatus status = setWritable(files);
			if (status.isOK())
				performEdit(files);
			return status;
		} else {
			// Allow the files to be edited without notifying the server
			return setWritable(files);
		}

		return Status.OK_STATUS;
		
	}
	
	@Override
	protected void scheduleEditJob(Job job) {
		job.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
		job.setProperty(IProgressConstants.ICON_PROPERTY, getOperationIcon());
		super.scheduleEditJob(job);
	}
	
	private URL getOperationIcon() {
		return FileLocator.find(CVSUIPlugin.getPlugin().getBundle(), new Path(ICVSUIConstants.ICON_PATH + ICVSUIConstants.IMG_CVS_PERSPECTIVE), null);
	}
	
	private boolean isRunningInUIThread() {
		return Display.getCurrent() != null;
	}

	private boolean promptToEditFiles(IFile[] files, Shell shell) throws InvocationTargetException, InterruptedException {
		if (files.length == 0)
			return true;		

		if(isNeverPrompt())	
			return true;

		// Contact the server to see if anyone else is editing the files
		EditorsAction editors = fetchEditors(files, shell);
		if (editors.isEmpty()) {
			if (isAlwaysPrompt()) 
				return (promptEdit(shell));
			return true;
		} else {
			return (editors.promptToEdit(shell));
		}
	}
	
	private boolean promptToUpdateFiles(IFile[] files, Shell shell) throws InvocationTargetException, InterruptedException {
		if (files.length == 0)
			return false;
		
		if (isNeverUpdate())
			return false;
		
		// Contact the server to see if the files are up-to-date
		if (needsUpdate(files, new NullProgressMonitor())) {
			if (isPromptUpdate())
				return (promptUpdate(shell));
			return true; // auto update
		}
		
		return false;
	}

	private boolean promptEdit(Shell shell) {
		// Open the dialog using a sync exec (there are no guarantees that we
		// were called from the UI thread
		final boolean[] result = new boolean[] { false };
		int flags = isRunningInUIThread() ? 0 : CVSUIPlugin.PERFORM_SYNC_EXEC;
		CVSUIPlugin
				.openDialog(shell,
						shell1 -> result[0] = MessageDialog.openQuestion(shell1,
								CVSUIMessages.FileModificationValidator_3, CVSUIMessages.FileModificationValidator_4),
						flags);
		return result[0];
	}

	private boolean promptUpdate(Shell shell) {
		// Open the dialog using a sync exec (there are no guarantees that we
		// were called from the UI thread
		final boolean[] result = new boolean[] { false };
		int flags = isRunningInUIThread() ? 0 : CVSUIPlugin.PERFORM_SYNC_EXEC;
		CVSUIPlugin
				.openDialog(shell,
						shell1 -> result[0] = MessageDialog.openQuestion(shell1,
								CVSUIMessages.FileModificationValidator_5, CVSUIMessages.FileModificationValidator_6),
						flags);
		return result[0];
	}

	private boolean isPerformEdit() {
		return ICVSUIConstants.PREF_EDIT_PROMPT_EDIT.equals(CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_EDIT_ACTION));
	}
	
	private boolean isPerformEditInBackground() {
		return ICVSUIConstants.PREF_EDIT_IN_BACKGROUND.equals(CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_EDIT_ACTION));
	}
	
	private EditorsAction fetchEditors(IFile[] files, Shell shell) throws InvocationTargetException, InterruptedException {
		final EditorsAction editors = new EditorsAction(getProvider(files), files);
		IRunnableWithProgress runnable = monitor -> editors.run(monitor);
		if (isRunningInUIThread()) {
			// Show a busy cursor if we are running in the UI thread
			CVSUIPlugin.runWithProgress(shell, false, runnable);
		} else {
			// We can't show a busy cursor (i.e., run in the UI thread)
			// since this thread may hold locks and
			// running a CVS operation in the UI thread could try to obtain the
			// same locks, resulting in a deadlock.
			runnable.run(new NullProgressMonitor());
		}
		return editors;
	}

	private boolean isNeverPrompt() {
		return ICVSUIConstants.PREF_EDIT_PROMPT_NEVER.equals(CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_EDIT_PROMPT));
	}

	private boolean isAlwaysPrompt() {
		return ICVSUIConstants.PREF_EDIT_PROMPT_ALWAYS.equals(CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_EDIT_PROMPT));
	}
	
	private boolean needsUpdate(IFile[] files, IProgressMonitor monitor) {
		try {
			CVSWorkspaceSubscriber subscriber = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
			subscriber.refresh(files, IResource.DEPTH_ZERO, monitor);
			for (IFile file : files) {
				SyncInfo info = subscriber.getSyncInfo(file);
				int direction = info.getKind() & SyncInfo.DIRECTION_MASK;
				if (direction == SyncInfo.CONFLICTING || direction == SyncInfo.INCOMING) {
					return true;
				}
			}
		} catch (TeamException e) {
			// Log the exception and assume we don't need to update it
			CVSProviderPlugin.log(e);
		}
		return false;
	}
	
	private void performUpdate(IFile[] files, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		// TODO: This obtains the project rule which can cause a rule violation
		new UpdateOperation(null /* no target part */, files, Command.NO_LOCAL_OPTIONS, null /* no tag */).run(monitor);
	}
	
	private boolean isPromptUpdate() {
		return ICVSUIConstants.PREF_UPDATE_PROMPT_IF_OUTDATED.equals(CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_UPDATE_PROMPT));
	}
	
	private boolean isNeverUpdate() {
		return ICVSUIConstants.PREF_UPDATE_PROMPT_NEVER.equals(CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_UPDATE_PROMPT));
	}
	
	@Override
	public ISchedulingRule validateEditRule(CVSResourceRuleFactory factory, IResource[] resources) {
		if (!isNeverUpdate()) {
			// We may need to perform an update so we need to obtain the lock on each project
			Set projects = new HashSet();
			for (IResource resource : resources) {
				if (isReadOnly(resource))
					projects.add(resource.getProject());
			}
			return createSchedulingRule(projects);
		}
		return internalValidateEditRule(factory, resources);
	}
}
