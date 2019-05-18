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
package org.eclipse.team.internal.ccvs.core.client;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.client.listeners.UpdateListener;

public class Update extends Command {
	/*** Local options: specific to update ***/
	public static final LocalOption CLEAR_STICKY = new LocalOption("-A"); //$NON-NLS-1$
	public static final LocalOption IGNORE_LOCAL_CHANGES = new LocalOption("-C"); //$NON-NLS-1$
	public static final LocalOption RETRIEVE_ABSENT_DIRECTORIES = new LocalOption("-d"); //$NON-NLS-1$
	public static final LocalOption JOIN = new LocalOption("-j"); //$NON-NLS-1$
	
	/*** Default command output listener ***/
	private static final ICommandOutputListener DEFAULT_OUTPUT_LISTENER = new UpdateListener(null);
	
	/*** File information status returned from update ***/
	public static final int STATE_NONE = 0;							// no state information available
	public static final int STATE_ADDED_LOCAL = 1; 			// new file locally that was added but not comitted to server yet
	public static final int STATE_UNKOWN = 2; 						// new file locally but not added to server
	public static final int STATE_REMOTE_CHANGES = 3; 		// remote changes to an unmodified local file
	public static final int STATE_DELETED = 4; 						// removed locally but still exists on the server
	public static final int STATE_MODIFIED = 5; 					// modified locally
	public static final int STATE_CONFLICT = 6; 					// modified locally and on the server but cannot be auto-merged
	public static final int STATE_MERGEABLE_CONFLICT = 7;  // modified locally and on the server but can be auto-merged

	/**
	 * Makes a -r or -D or -A option for a tag.
	 * Valid for: checkout export history rdiff update
	 */
	public static LocalOption makeTagOption(CVSTag tag) {
		int type = tag.getType();
		switch (type) {
			case CVSTag.HEAD:
				return CLEAR_STICKY;
			default:
				return Command.makeTagOption(tag);
		}
	}
	
	protected Update() { }
	protected String getRequestId() {
		return "update"; //$NON-NLS-1$
	}
	
	protected ICommandOutputListener getDefaultCommandOutputListener() {
		return DEFAULT_OUTPUT_LISTENER;
	}
	
	protected ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException {			
		
		// Send all folders that are already managed to the server
		// even folders that are empty
		sendFileStructure(session, resources, localOptions, true, monitor);
		return resources;
	}
	
	/**
	 * On successful finish, prune empty directories if the -P or -D option was specified.
	 */
	protected IStatus commandFinished(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor,
		IStatus status) throws CVSException {
		// If we didn't succeed, don't do any post processing
		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			return status;
		}

		// If we are pruning (-P), then prune empty directories
		// Note, the CVS spec says that Date (-D) and version (-r) updates
		// should automatically prune but this is a problem for remote CVS handles
		// which fetch a level at a time
		if (PRUNE_EMPTY_DIRECTORIES.isElementOf(localOptions)) { 
			// Delete empty directories
			new PruneFolderVisitor().visit(session, resources);
			
		}
		return status;
	}
	
	protected LocalOption[] filterLocalOptions(Session session, GlobalOption[] globalOptions, LocalOption[] localOptions) {
		List<LocalOption> newOptions = new ArrayList<>(Arrays.asList(localOptions));
		
		if (shouldRetrieveAbsentDirectories(session) && ! RETRIEVE_ABSENT_DIRECTORIES.isElementOf(localOptions)) {
			newOptions.add(Update.RETRIEVE_ABSENT_DIRECTORIES);
		}

		// Prune empty directories if pruning is enabled and the command in not being run in non-update mode
		if (CVSProviderPlugin.getPlugin().getPruneEmptyDirectories() && ! PRUNE_EMPTY_DIRECTORIES.isElementOf(localOptions)) {
			if (! DO_NOT_CHANGE.isElementOf(globalOptions)) {
				newOptions.add(Command.PRUNE_EMPTY_DIRECTORIES);
			}
		}
		localOptions = newOptions.toArray(new LocalOption[newOptions.size()]);
		return super.filterLocalOptions(session, globalOptions, localOptions);
	}
	
	/**
	 * Return whether the update command should retrieve absent directories.
	 * @param session the session
	 * @return whether the update command should retrieve absent directories
	 */
	protected boolean shouldRetrieveAbsentDirectories(Session session) {
		// Look for absent directories if enabled and the option is not already included
		IResource resource = null;
		RepositoryProvider provider = null;
		// If there is a provider, use the providers setting
		try {
			resource = session.getLocalRoot().getIResource();
			if (resource != null) {
				provider = RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
				if (provider != null) {
					if (((CVSTeamProvider)provider).getFetchAbsentDirectories()) {
						return true;
					}
				}
			}
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
		// If there is no provider, use the global setting
		if (provider == null) {
			if (CVSProviderPlugin.getPlugin().getFetchAbsentDirectories()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * We allow unmanaged resources as long as there parents are managed.
	 * 
	 * @see Command#checkResourcesManaged(Session, ICVSResource[])
	 */
	protected void checkResourcesManaged(Session session, ICVSResource[] resources) throws CVSException {
		for (int i = 0; i < resources.length; ++i) {
			ICVSFolder folder;
			if (resources[i].isFolder()) {
				if (((ICVSFolder)resources[i]).isCVSFolder()) {
					folder = (ICVSFolder)resources[i];
				} else {
					folder = resources[i].getParent();
				}
			}
			else {
				folder = resources[i].getParent();
			}
			if (folder==null || (!folder.isCVSFolder() && folder.exists())) {
				if (folder == null)
					folder = (ICVSFolder)resources[i];
				IStatus status = new CVSStatus(IStatus.ERROR,CVSStatus.ERROR,NLS.bind(CVSMessages.Command_argumentNotManaged, new String[] { folder.getName() }),session.getLocalRoot());
				throw new CVSException(status);
			}
		}
	}

	@Override
	protected IStatus doExecute(
			Session session,
			GlobalOption[] globalOptions,
			LocalOption[] localOptions,
			String[] arguments,
			ICommandOutputListener listener,
			IProgressMonitor monitor)
			throws CVSException {
			
		session.setIgnoringLocalChanges(IGNORE_LOCAL_CHANGES.isElementOf(localOptions));
		try {
			return super.doExecute(
				session,
				globalOptions,
				localOptions,
				arguments,
				listener,
				monitor);
		} finally {
			session.setIgnoringLocalChanges(false);
		}

	}

}
