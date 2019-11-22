/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core;
 
import java.io.*;
import java.net.URI;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.*;
import org.eclipse.team.internal.ccvs.core.filehistory.CVSFileHistoryProvider;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.*;
import org.eclipse.team.internal.ccvs.core.util.*;
import org.eclipse.team.internal.core.streams.CRLFtoLFInputStream;
import org.eclipse.team.internal.core.streams.LFtoCRLFInputStream;

/**
 * CVS implementation of {@link RepositoryProvider}
 */
public class CVSTeamProvider extends RepositoryProvider {

	private static final ResourceRuleFactory RESOURCE_RULE_FACTORY = new CVSResourceRuleFactory();
	
	private static final boolean IS_CRLF_PLATFORM = Arrays.equals(
		System.getProperty("line.separator").getBytes(), new byte[] { '\r', '\n' }); //$NON-NLS-1$
	
	public static final IStatus OK = new Status(IStatus.OK, CVSProviderPlugin.ID, 0, CVSMessages.ok, null); 
	
	private CVSWorkspaceRoot workspaceRoot;
	private IProject project;
	
	private static MoveDeleteHook moveDeleteHook= new MoveDeleteHook();
	private static CVSCoreFileModificationValidator fileModificationValidator;
	private static CVSFileHistoryProvider fileHistoryProvider;
	
	// property used to indicate whether new directories should be discovered for the project
	private final static QualifiedName FETCH_ABSENT_DIRECTORIES_PROP_KEY = 
		new QualifiedName("org.eclipse.team.cvs.core", "fetch_absent_directories");  //$NON-NLS-1$  //$NON-NLS-2$
	// property used to indicate whether the project is configured to use Watch/edit
	private final static QualifiedName WATCH_EDIT_PROP_KEY = 
		new QualifiedName("org.eclipse.team.cvs.core", "watch_edit");  //$NON-NLS-1$  //$NON-NLS-2$

	/**
	 * Session property key used to indicate that the project, although not officially shared,
	 * is a target of a CVS operation.
	 */
	private static final QualifiedName TEMP_SHARED = new QualifiedName(CVSProviderPlugin.ID, "tempShare"); //$NON-NLS-1$
	
	/**
	 * Return whether the project is mapped to CVS or is the target of a CVS operation
	 * that will most likely lead to the project being shared.
	 * @param project the project
	 * @return whether the project is mapped to CVS or is the target of a CVS operation
	 * that will most likely lead to the project being shared
	 */
	public static boolean isSharedWithCVS(IProject project) {
		if (project.isAccessible()) {
			if (RepositoryProvider.isShared(project)) {
				RepositoryProvider provider = RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
				if (provider != null)
					return true;
			}
			try {
				Object sessionProperty = project.getSessionProperty(TEMP_SHARED);
				return sessionProperty != null && sessionProperty.equals(Boolean.TRUE);
			} catch (CoreException e) {
				CVSProviderPlugin.log(e);
			}
		}
		return false;
	}
	
	/**
	 * Mark the project as being a target of a CVS operation so the sync info management
	 * will occur.
	 * @param project the project
	 */
	public static void markAsTempShare(IProject project) {
		if (RepositoryProvider.isShared(project))
			return;
		try {
			project.setSessionProperty(CVSTeamProvider.TEMP_SHARED, Boolean.TRUE);
		} catch (CoreException e) {
			CVSProviderPlugin.log(e);
		}
	}
	
	/**
	 * Return the file modification validator used for all CVS repository providers.
	 * @return the file modification validator used for all CVS repository providers
	 */
	protected static CVSCoreFileModificationValidator internalGetFileModificationValidator() {
		if (CVSTeamProvider.fileModificationValidator == null) {
			CVSTeamProvider.fileModificationValidator = new CVSCoreFileModificationValidator();
		}
		return CVSTeamProvider.fileModificationValidator;
	}
	
	/**
	 * No-arg Constructor for IProjectNature conformance
	 */
	public CVSTeamProvider() {
	}

	@Override
	public void deconfigure() {
	}
	
	@Override
	public void deconfigured() {
		// when a nature is removed from the project, notify the synchronizer that
		// we no longer need the sync info cached. This does not affect the actual CVS
		// meta directories on disk, and will remain unless a client calls unmanage().
		try {
			EclipseSynchronizer.getInstance().deconfigure(getProject(), null);
			internalSetWatchEditEnabled(null);
			internalSetFetchAbsentDirectories(null);
		} catch(CVSException e) {
			// Log the exception and let the disconnect continue
			CVSProviderPlugin.log(e);
		}
		ResourceStateChangeListeners.getListener().projectDeconfigured(getProject());
	}
	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
		this.workspaceRoot = new CVSWorkspaceRoot(project);
		// We used to check to see if the project had CVS folders and log
		// if it didn't However, in some scenarios, the project can be mapped
		// before the CVS folders have been created (see bug 173610)
	}

	/**
	 * Return the remote location to which the receiver's project is mapped.
	 */
	public ICVSRepositoryLocation getRemoteLocation() throws CVSException {
		try {
			return workspaceRoot.getRemoteLocation();
		} catch (CVSException e) {
			// If we can't get the remote location, we should disconnect since nothing can be done with the provider
			try {
				RepositoryProvider.unmap(project);
			} catch (TeamException ex) {
				CVSProviderPlugin.log(ex);
			}
			// We need to trigger a decorator refresh					
			throw e;
		}
	}
	
	public CVSWorkspaceRoot getCVSWorkspaceRoot() {
		return workspaceRoot;
	}
	
	/*
	 * Generate an exception if the resource is not a child of the project
	 */
	private void checkIsChild(IResource resource) throws CVSException {
		if (!isChildResource(resource))
			throw new CVSException(new Status(IStatus.ERROR, CVSProviderPlugin.ID, TeamException.UNABLE, 
				NLS.bind(CVSMessages.CVSTeamProvider_invalidResource, (new Object[] {resource.getFullPath().toString(), project.getName()})), 
				null));
	}
	
	/*
	 * Get the arguments to be passed to a commit or update
	 */
	private String[] getValidArguments(IResource[] resources, LocalOption[] options) throws CVSException {
		List<String> arguments = new ArrayList<>(resources.length);
		for (IResource resource : resources) {
			checkIsChild(resource);
			IPath cvsPath = resource.getFullPath().removeFirstSegments(1);
			if (cvsPath.segmentCount() == 0) {
				arguments.add(Session.CURRENT_LOCAL_FOLDER);
			} else {
				arguments.add(cvsPath.toString());
			}
		}
		return arguments.toArray(new String[arguments.size()]);
	}
	
	private ICVSResource[] getCVSArguments(IResource[] resources) {
		ICVSResource[] cvsResources = new ICVSResource[resources.length];
		for (int i = 0; i < cvsResources.length; i++) {
			cvsResources[i] = CVSWorkspaceRoot.getCVSResourceFor(resources[i]);
		}
		return cvsResources;
	}
	
	/*
	 * This method expects to be passed an InfiniteSubProgressMonitor
	 */
	public void setRemoteRoot(ICVSRepositoryLocation location, IProgressMonitor monitor) throws TeamException {

		// Check if there is a differnece between the new and old roots	
		final String root = location.getLocation(false);
		if (root.equals(workspaceRoot.getRemoteLocation())) 
			return;
	
		try {
			workspaceRoot.getLocalRoot().run(progress -> {
				try {
					// 256 ticks gives us a maximum of 1024 which seems reasonable for folders is a project
					progress.beginTask(null, 100);
					final IProgressMonitor monitor1 = Policy.infiniteSubMonitorFor(progress, 100);
					monitor1.beginTask(null, 256);  

					// Visit all the children folders in order to set the root in the folder sync info
					workspaceRoot.getLocalRoot().accept(new ICVSResourceVisitor() {
						public void visitFile(ICVSFile file) throws CVSException {}
						public void visitFolder(ICVSFolder folder) throws CVSException {
							monitor1.worked(1);
							FolderSyncInfo info = folder.getFolderSyncInfo();
							if (info != null) {
								monitor1.subTask(NLS.bind(CVSMessages.CVSTeamProvider_updatingFolder, new String[] { info.getRepository() })); 
								MutableFolderSyncInfo newInfo = info.cloneMutable();
								newInfo.setRoot(root);
								folder.setFolderSyncInfo(newInfo);
								folder.acceptChildren(this);
							}
						}
					});
				} finally {
					progress.done();
				}
			}, monitor);
		} finally {
			monitor.done();
		}
	}
	
	/*
	 * Helper to indicate if the resource is a child of the receiver's project
	 */
	private boolean isChildResource(IResource resource) {
		return resource.getProject().getName().equals(project.getName());
	}
	
	public void configureProject() throws CoreException {
		getProject().setSessionProperty(TEMP_SHARED, null);
		ResourceStateChangeListeners.getListener().projectConfigured(getProject());
	}
	/**
	 * Sets the keyword substitution mode for the specified resources.
	 * <p>
	 * Applies the following rules in order:
	 * </p>
	 * <ul>
	 *   <li>If a file is not managed, skips it.</li>
	 *   <li>If a file is not changing modes, skips it.</li>
	 *   <li>If a file is being changed from binary to text, corrects line delimiters
	 *       then commits it, then admins it.</li>
	 *   <li>If a file is added, changes the resource sync information locally.</li>
	 *   <li>Otherwise commits the file (with FORCE to create a new revision), then admins it.</li>
	 * </ul>
	 * All files that are admin'd are committed with FORCE to prevent other developers from
	 * casually trying to commit pending changes to the repository without first checking out
	 * a new copy.  This is not a perfect solution, as they could just as easily do an UPDATE
	 * and not obtain the new keyword sync info.
	 * 
	 * @param changeSet a map from IFile to KSubstOption
	 * @param monitor the progress monitor
	 * @return a status code indicating success or failure of the operation
	 * 
	 * @throws TeamException
	 */
	public IStatus setKeywordSubstitution(final Map /* from IFile to KSubstOption */ changeSet,
		final String comment,
		IProgressMonitor monitor) throws TeamException {
		final IStatus[] result = new IStatus[] { ICommandOutputListener.OK };
		workspaceRoot.getLocalRoot().run(monitor1 -> {
			final Map /* from KSubstOption to List of String */ filesToAdmin = new HashMap();
			final Collection<ICVSFile> filesToCommitAsText = new HashSet<>(); // need fast lookup
			final boolean useCRLF = IS_CRLF_PLATFORM && (CVSProviderPlugin.getPlugin().isUsePlatformLineend());

			/*** determine the resources to be committed and/or admin'd ***/
			for (Iterator it1 = changeSet.entrySet().iterator(); it1.hasNext();) {
				Map.Entry entry1 = (Map.Entry) it1.next();
				IFile file = (IFile) entry1.getKey();
				KSubstOption toKSubst1 = (KSubstOption) entry1.getValue();

				// only set keyword substitution if resource is a managed file
				checkIsChild(file);
				ICVSFile mFile = CVSWorkspaceRoot.getCVSFileFor(file);
				if (! mFile.isManaged()) continue;
				
				// only set keyword substitution if new differs from actual
				byte[] syncBytes = mFile.getSyncBytes();
				KSubstOption fromKSubst = ResourceSyncInfo.getKeywordMode(syncBytes);
				if (toKSubst1.equals(fromKSubst)) continue;
				
				// change resource sync info immediately for an outgoing addition
				if (ResourceSyncInfo.isAddition(syncBytes)) {
					mFile.setSyncBytes(ResourceSyncInfo.setKeywordMode(syncBytes, toKSubst1), ICVSFile.UNKNOWN);
					continue;
				}

				// nothing do to for deletions
				if (ResourceSyncInfo.isDeletion(syncBytes)) continue;

				// file exists remotely so we'll have to commit it
				if (fromKSubst.isBinary() && ! toKSubst1.isBinary()) {
					// converting from binary to text
					cleanLineDelimiters(file, useCRLF, new NullProgressMonitor()); // XXX need better progress monitoring
					// remember to commit the cleaned resource as text before admin
					filesToCommitAsText.add(mFile);
				}
				// remember to admin the resource
				List list1 = (List) filesToAdmin.get(toKSubst1);
				if (list1 == null) {
					list1 = new ArrayList();
					filesToAdmin.put(toKSubst1, list1);
				}
				list1.add(mFile);
			}
		
			/*** commit then admin the resources ***/
			// compute the total work to be performed
			int totalWork = filesToCommitAsText.size() + 1;
			for (Iterator it2 = filesToAdmin.values().iterator(); it2.hasNext();) {
				List list2 = (List) it2.next();
				totalWork += list2.size();
				totalWork += 1; // Add 1 for each connection that needs to be made
			}
			if (totalWork != 0) {
				monitor1.beginTask(CVSMessages.CVSTeamProvider_settingKSubst, totalWork); 
				try {
					// commit files that changed from binary to text
					// NOTE: The files are committed as text with conversions even if the
					//       resource sync info still says "binary".
					if (!filesToCommitAsText.isEmpty()) {
						Session session1 = new Session(workspaceRoot.getRemoteLocation(), workspaceRoot.getLocalRoot(), true /* output to console */);
						session1.open(Policy.subMonitorFor(monitor1, 1), true /* open for modification */);
						try {
							String keywordChangeComment = comment;
							if (keywordChangeComment == null || keywordChangeComment.length() == 0)
								keywordChangeComment = CVSMessages.CVSTeamProvider_changingKeywordComment; 
							result[0] = Command.COMMIT.execute(
								session1,
								Command.NO_GLOBAL_OPTIONS,
								new LocalOption[] { Command.DO_NOT_RECURSE, Commit.FORCE,
									Command.makeArgumentOption(Command.MESSAGE_OPTION, keywordChangeComment) },
								filesToCommitAsText.toArray(new ICVSResource[filesToCommitAsText.size()]),
								filesToCommitAsText,
								null, 
								Policy.subMonitorFor(monitor1, filesToCommitAsText.size()));
						} finally {
							session1.close();
						}

						// if errors were encountered, abort
						if (! result[0].isOK()) return;
					}
					
					// admin files that changed keyword substitution mode
					// NOTE: As confirmation of the completion of a command, the server replies
					//       with the RCS command output if a change took place.  Rather than
					//       assume that the command succeeded, we listen for these lines
					//       and update the local ResourceSyncInfo for the particular files that
					//       were actually changed remotely.
					for (Iterator it3 = filesToAdmin.entrySet().iterator(); it3.hasNext();) {
						Map.Entry entry2 = (Map.Entry) it3.next();
						final KSubstOption toKSubst2 = (KSubstOption) entry2.getKey();
						final List list3 = (List) entry2.getValue();
						// do it
						Session session2 = new Session(workspaceRoot.getRemoteLocation(), workspaceRoot.getLocalRoot(), true /* output to console */);
						session2.open(Policy.subMonitorFor(monitor1, 1), true /* open for modification */);
						try {
							result[0] = Command.ADMIN.execute(
								session2,
								Command.NO_GLOBAL_OPTIONS,
								new LocalOption[] { toKSubst2 },
								(ICVSResource[]) list3.toArray(new ICVSResource[list3.size()]),
								new AdminKSubstListener(toKSubst2),
								Policy.subMonitorFor(monitor1, list3.size()));
						} finally {
							session2.close();
						}
						// if errors were encountered, abort
						if (! result[0].isOK()) return;
					}
				} finally {
					monitor1.done();
				}
			}
		}, Policy.monitorFor(monitor));
		return result[0];
	}
	
	/**
	 * This method translates the contents of a file from binary into text (ASCII).
	 * Fixes the line delimiters in the local file to reflect the platform's
	 * native encoding.  Performs CR/LF -&gt; LF or LF -&gt; CR/LF conversion
	 * depending on the platform but does not affect delimiters that are
	 * already correctly encoded.
	 */
	public static void cleanLineDelimiters(IFile file, boolean useCRLF, IProgressMonitor progress)
		throws CVSException {
		try {
			// convert delimiters in memory
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			InputStream is = new BufferedInputStream(file.getContents());
			try {
				// Always convert CR/LF into LFs
				is = new CRLFtoLFInputStream(is);
				if (useCRLF) {
					// For CR/LF platforms, translate LFs to CR/LFs
					is = new LFtoCRLFInputStream(is);
				}
				for (int b; (b = is.read()) != -1;) bos.write(b);
				bos.close();
			} finally {
				is.close();
			}
			// write file back to disk with corrected delimiters if changes were made
			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			file.setContents(bis, false /*force*/, false /*keepHistory*/, progress);
		} catch (CoreException e) {
			throw CVSException.wrapException(file, CVSMessages.CVSTeamProvider_cleanLineDelimitersException, e); 
		} catch (IOException e) {
			throw CVSException.wrapException(file, CVSMessages.CVSTeamProvider_cleanLineDelimitersException, e); 
		}
	}
	
	@Override
	public String getID() {
		return CVSProviderPlugin.getTypeId();
	}
	
	@Override
	public IMoveDeleteHook getMoveDeleteHook() {
		return moveDeleteHook;
	}
	
	@Override
	public IFileModificationValidator getFileModificationValidator() {
		return getFileModificationValidator2();
	}
	
	@Override
	public FileModificationValidator getFileModificationValidator2() {
		return internalGetFileModificationValidator();
	}
	
	/**
	 * Checkout (cvs edit) the provided resources so they can be modified locally and committed.
	 * This will make any read-only resources in the list writable and will notify the server
	 * that the file is being edited. This notification may be done immediately or at some 
	 * later point depending on whether contact with the server is possible at the time of 
	 * invocation or the value of the notify server parameter.
	 * 
	 * The recurse parameter is equivalent to the cvs local options -l (<code>true</code>) and 
	 * -R (<code>false</code>). The notifyServer parameter can be used to defer server contact
	 * until the next command. This may be appropriate if no shell or progress monitor is available
	 * to the caller. The notification bit field indicates what temporary watches are to be used while
	 * the file is being edited. The possible values that can be ORed together are ICVSFile.EDIT, 
	 * ICVSFile.UNEDIT and ICVSFile.COMMIT. There pre-ORed convenience values ICVSFile.NO_NOTIFICATION
	 * and ICVSFile.NOTIFY_ON_ALL are also available.
	 * 
	 * @param resources the resources to be edited
	 * @param recurse indicates whether to recurse (-R) or not (-l)
	 * @param notifyServer indicates whether to notify the server now, if possible,
	 *     or defer until the next command.
	 * @param notifyForWrittable 
	 * @param notification the temporary watches.
	 * @param progress progress monitor to provide progress indication/cancellation or <code>null</code>
	 * @exception CVSException if this method fails.
	 * @since 2.1
	 * 
	 * @see CVSTeamProvider#unedit
	 */
	public void edit(IResource[] resources, boolean recurse, boolean notifyServer, final boolean notifyForWritable, final int notification, IProgressMonitor progress) throws CVSException {
		final int notify;
		if (notification == ICVSFile.NO_NOTIFICATION) {
			if (CVSProviderPlugin.getPlugin().isWatchOnEdit()) {
				notify = ICVSFile.NOTIFY_ON_ALL;
			} else {
				notify = ICVSFile.NO_NOTIFICATION;
			}
		} else {
			notify = notification;
		}
		notifyEditUnedit(resources, recurse, notifyServer, new ICVSResourceVisitor() {
			public void visitFile(ICVSFile file) throws CVSException {
				if (notifyForWritable || file.isReadOnly())
					file.edit(notify, notifyForWritable, Policy.monitorFor(null));
			}
			public void visitFolder(ICVSFolder folder) throws CVSException {
				// nothing needs to be done here as the recurse will handle the traversal
			}
		}, null /* no scheduling rule */, progress);
	}
	
	/**
	 * Unedit the given resources. Any writable resources will be reverted to their base contents
	 * and made read-only and the server will be notified that the file is no longer being edited.
	 * This notification may be done immediately or at some 
	 * later point depending on whether contact with the server is possible at the time of 
	 * invocation or the value of the notify server parameter.
	 * 
	 * The recurse parameter is equivalent to the cvs local options -l (<code>true</code>) and 
	 * -R (<code>false</code>). The notifyServer parameter can be used to defer server contact
	 * until the next command. This may be appropriate if no shell or progress monitor is available
	 * to the caller.
	 * 
	 * @param resources the resources to be unedited
	 * @param recurse indicates whether to recurse (-R) or not (-l)
	 * @param notifyServer indicates whether to notify the server now, if possible,
	 *     or defer until the next command.
	 * @param progress progress monitor to provide progress indication/cancellation or <code>null</code>
	 * @exception CVSException if this method fails.
	 * @since 2.1
	 * 
	 * @see CVSTeamProvider#edit
	 */
	public void unedit(IResource[] resources, boolean recurse, boolean notifyServer, IProgressMonitor progress) throws CVSException {
		notifyEditUnedit(resources, recurse, notifyServer, new ICVSResourceVisitor() {
			public void visitFile(ICVSFile file) throws CVSException {
				if (!file.isReadOnly())
					file.unedit(Policy.monitorFor(null));
			}
			public void visitFolder(ICVSFolder folder) throws CVSException {
				// nothing needs to be done here as the recurse will handle the traversal
			}
		}, getProject() /* project scheduling rule */, progress);
	}
	
	/*
	 * This method captures the common behavior between the edit and unedit methods.
	 */
	private void notifyEditUnedit(final IResource[] resources, final boolean recurse, final boolean notifyServer, final ICVSResourceVisitor editUneditVisitor, ISchedulingRule rule, IProgressMonitor monitor) throws CVSException {
		final CVSException[] exception = new CVSException[] { null };
		IWorkspaceRunnable workspaceRunnable = monitor1 -> {
			final ICVSResource[] cvsResources = getCVSArguments(resources);
			
			// mark the files locally as being checked out
			try {
				for (int i = 0; i < cvsResources.length; i++) {
					cvsResources[i].accept(editUneditVisitor, recurse);
				}
			} catch (CVSException e2) {
				exception[0] = e2;
				return;
			}
			
			// send the noop command to the server in order to deliver the notifications
			if (notifyServer) {
				monitor1.beginTask(null, 100);
				Session session = new Session(workspaceRoot.getRemoteLocation(), workspaceRoot.getLocalRoot(), true);
				try {
					try {
						session.open(Policy.subMonitorFor(monitor1, 10), true /* open for modification */);
					} catch (CVSException e1) {
						// If the connection cannot be opened, just exit normally.
						// The notifications will be sent when a connection can be made
						return;
					}
					Command.NOOP.execute(
						session,
						Command.NO_GLOBAL_OPTIONS, 
						Command.NO_LOCAL_OPTIONS, 
						cvsResources, 
						null, 
						Policy.subMonitorFor(monitor1, 90));
				} catch (CVSException e3) {
					exception[0] = e3;
				} finally {
					session.close();
					monitor1.done();
				}
			}
		};
		try {
			ResourcesPlugin.getWorkspace().run(workspaceRunnable, rule, 0, Policy.monitorFor(monitor));
		} catch (CoreException e) {
			if (exception[0] == null) {
				throw CVSException.wrapException(e);
			} else {
				CVSProviderPlugin.log(CVSException.wrapException(e));
			}
		}
		if (exception[0] != null) {
			throw exception[0];
		}
	}
	
	/**
	 * Gets the etchAbsentDirectories.
	 * @return Returns a boolean
	 */
	public boolean getFetchAbsentDirectories() throws CVSException {
		try {
			String property = getProject().getPersistentProperty(FETCH_ABSENT_DIRECTORIES_PROP_KEY);
			if (property == null) return CVSProviderPlugin.getPlugin().getFetchAbsentDirectories();
			return Boolean.valueOf(property).booleanValue();
		} catch (CoreException e) {
			throw new CVSException(new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.CVSTeamProvider_errorGettingFetchProperty, new String[] { project.getName() }), e, project)); 
		}
	}
	
	/**
	 * Sets the fetchAbsentDirectories.
	 * @param etchAbsentDirectories The etchAbsentDirectories to set
	 */
	public void setFetchAbsentDirectories(boolean fetchAbsentDirectories) throws CVSException {
		internalSetFetchAbsentDirectories(fetchAbsentDirectories ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
	}

	public void internalSetFetchAbsentDirectories(String fetchAbsentDirectories) throws CVSException {
		try {
			getProject().setPersistentProperty(FETCH_ABSENT_DIRECTORIES_PROP_KEY, fetchAbsentDirectories);
		} catch (CoreException e) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.CVSTeamProvider_errorSettingFetchProperty, new String[] { project.getName() }), e, project);
			throw new CVSException(status); 
		}
	}
	
	@Override
	public boolean canHandleLinkedResources() {
		return true;
	}
	
	@Override
	public boolean canHandleLinkedResourceURI() {
		return true;
	}

	@Override
	public IStatus validateCreateLink(IResource resource, int updateFlags, IPath location) {
		return internalValidateCreateLink(resource);
	}

	private IStatus internalValidateCreateLink(IResource resource) {
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(resource.getParent().getFolder(new Path(resource.getName())));
		try {
			if (cvsFolder.isCVSFolder()) {
				// There is a remote folder that overlaps with the link so disallow
				return new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.CVSTeamProvider_overlappingRemoteFolder, new String[] { resource.getFullPath().toString() }),resource); 
			} else {
				ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(resource.getParent().getFile(new Path(resource.getName())));
				if (cvsFile.isManaged()) {
					// there is an outgoing file deletion that overlaps the link so disallow
					return new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.CVSTeamProvider_overlappingFileDeletion, new String[] { resource.getFullPath().toString() }),resource); 
				}
			}
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}
	
	@Override
	public IStatus validateCreateLink(IResource resource, int updateFlags, URI location) {
		return internalValidateCreateLink(resource);
	}
	
	/**
	 * Get the editors of the resources by calling the <code>cvs editors</code> command.
	 * 
	 * @author <a href="mailto:gregor.kohlwes@csc.com,kohlwes@gmx.net">Gregor Kohlwes</a>
	 * @param resources
	 * @param progress
	 * @return IEditorsInfo[]
	 * @throws CVSException
	 */
	public EditorsInfo[] editors(
		IResource[] resources,
		IProgressMonitor progress)
		throws CVSException {

		// Build the local options
		LocalOption[] commandOptions = new LocalOption[] {
		};
		progress.worked(10);
		// Build the arguments list
		String[] arguments = getValidArguments(resources, commandOptions);

		// Build the listener for the command
		EditorsListener listener = new EditorsListener();

		// Check if canceled
		if (progress.isCanceled()) {
			return new EditorsInfo[0];
		}
		// Build the session
		Session session =
			new Session(
				workspaceRoot.getRemoteLocation(),
				workspaceRoot.getLocalRoot());

		// Check if canceled
		if (progress.isCanceled()) {
			return new EditorsInfo[0];
		}
		progress.beginTask(null, 100);
		try {
			// Opening the session takes 20% of the time
			session.open(Policy.subMonitorFor(progress, 20), false /* read-only */);

			if (!progress.isCanceled()) {
				// Execute the editors command
				Command.EDITORS.execute(
					session,
					Command.NO_GLOBAL_OPTIONS,
					commandOptions,
					arguments,
					listener,
					Policy.subMonitorFor(progress, 80));
			}
		} finally {
			session.close();
			progress.done();
		}
		// Return the infos about the editors
		return listener.getEditorsInfos();
	}

	/**
	 * Return the commit comment template that was provided by the server.
	 * 
	 * @return String
	 * @throws CVSException
	 */
	public String getCommitTemplate() throws CVSException {
		ICVSFolder localFolder = getCVSWorkspaceRoot().getLocalRoot();
		ICVSFile templateFile = CVSWorkspaceRoot.getCVSFileFor(
			SyncFileWriter.getTemplateFile(
				(IContainer)localFolder.getIResource()));
		if (!templateFile.exists()) return null;
		InputStream in = new BufferedInputStream(templateFile.getContents());
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int b;
			do {
				b = in.read();
				if (b != -1)
					out.write((byte)b);
			} while (b != -1);
			out.close();
			return new String(out.toString());
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// Since we already have the contents, just log this exception
				CVSProviderPlugin.log(CVSException.wrapException(e));
			}
		}
	}
	
	/**
	 * Return true if the project is configured to use watch/edit. A project will use 
	 * watch/edit if it was checked out when the global preference to use watch/edit is
	 * turned on.
	 * @return boolean
	 */
	public boolean isWatchEditEnabled() throws CVSException {
		IProject project = getProject();
		try {
			String property = (String)project.getSessionProperty(WATCH_EDIT_PROP_KEY);
			if (property == null) {
				property = project.getPersistentProperty(WATCH_EDIT_PROP_KEY);
				if (property == null) {
					// The persistant property for the project was never set (i.e. old project)
					// Use the global preference to determine if the project is using watch/edit
					return CVSProviderPlugin.getPlugin().isWatchEditEnabled();
				} else {
					project.setSessionProperty(WATCH_EDIT_PROP_KEY, property);
				}
			}
			return Boolean.valueOf(property).booleanValue();
		} catch (CoreException e) {
			if (project.isAccessible()) {
				// We only care if the project still exists
				IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.CVSTeamProvider_errorGettingWatchEdit, new String[] { project.getName() }), e, project);
				throw new CVSException(status); 
			}
		}
		return false;
	}
	
	public void setWatchEditEnabled(boolean enabled) throws CVSException {
		internalSetWatchEditEnabled(enabled ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
	}
	
	private void internalSetWatchEditEnabled(String enabled) throws CVSException {
		try {
			IProject project = getProject();
			project.setPersistentProperty(WATCH_EDIT_PROP_KEY, enabled);
			project.setSessionProperty(WATCH_EDIT_PROP_KEY, enabled);
		} catch (CoreException e) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.CVSTeamProvider_errorSettingWatchEdit, new String[] { project.getName() }), e, project);
			throw new CVSException(status); 
		}
	}
	
	@Override
	public IResourceRuleFactory getRuleFactory() {
		return RESOURCE_RULE_FACTORY;
	}

	@Override
	public IFileHistoryProvider getFileHistoryProvider() {
			if (CVSTeamProvider.fileHistoryProvider == null) {
				CVSTeamProvider.fileHistoryProvider = new CVSFileHistoryProvider();
			}
			return CVSTeamProvider.fileHistoryProvider;
	}
}
