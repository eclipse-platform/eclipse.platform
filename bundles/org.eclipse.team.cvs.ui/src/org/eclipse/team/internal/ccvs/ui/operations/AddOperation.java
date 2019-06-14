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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Performs a "cvs add"
 */
public class AddOperation extends RepositoryProviderOperation {
		
	private Map fModesForExtensions;
	private Map fModesForFiles;
	
	public AddOperation(IWorkbenchPart part, ResourceMapping[] mappers) {
		super(part, mappers);
		fModesForExtensions= Collections.EMPTY_MAP;
		fModesForFiles= Collections.EMPTY_MAP;
	}

	public void addModesForExtensions(Map modes) {
		fModesForExtensions= modes;
	}
	
	public void addModesForNames(Map modes) {
		fModesForFiles= modes;
	}

	@Override
	protected void execute(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		if (resources.length == 0)
			return;
		add(provider, resources, recurse ? IResource.DEPTH_INFINITE : IResource.DEPTH_ONE, monitor);
	}
	
	@Override
	protected String getTaskName() {
		return CVSUIMessages.AddAction_adding; 
	}
	
	@Override
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.AddOperation_0, new String[] { provider.getProject().getName() }); 
	}
	
	/*
	 * Add the given resources to the project. 
	 * <p>
	 * The sematics follow that of CVS in the sense that any folders 
	 * being added are created remotely as a result of this operation 
	 * while files are created remotely on the next commit. 
	 * </p>
	 * <p>
	 * This method uses the team file type registry to determine the type
	 * of added files. If the extension of the file is not in the registry,
	 * the file is assumed to be binary.
	 * </p>
	 * <p>
	 * NOTE: for now we do three operations: one each for folders, text files and binary files.
	 * We should optimize this when time permits to either use one operations or defer server
	 * contact until the next commit.
	 * </p>
	 * 
	 * <p>
	 * There are special semantics for adding the project itself to the repo. In this case, the project 
	 * must be included in the resources array.
	 * </p>
	 */
	private void add(CVSTeamProvider provider, IResource[] resources, int depth, IProgressMonitor progress) throws CVSException {	
		
		// Visit the children of the resources using the depth in order to
		// determine which folders, text files and binary files need to be added
		// A TreeSet is needed for the folders so they are in the right order (i.e. parents created before children)
		final SortedSet<ICVSResource> folders = new TreeSet<>();
		// Sets are required for the files to ensure that files will not appear twice if there parent was added as well
		// and the depth isn't zero
		final Map /* from KSubstOption to Set */<KSubstOption, Set> files = new HashMap<>();
		final CVSException[] eHolder = new CVSException[1];
		for (IResource currentResource : resources) {
			try {		
				// Auto-add parents if they are not already managed
				IContainer parent = currentResource.getParent();
				ICVSResource cvsParentResource = CVSWorkspaceRoot.getCVSResourceFor(parent);
				while (parent.getType() != IResource.ROOT && parent.getType() != IResource.PROJECT && ! isManaged(cvsParentResource)) {
					folders.add(cvsParentResource);
					parent = parent.getParent();
					cvsParentResource = cvsParentResource.getParent();
				}
					
				// Auto-add children
				final TeamException[] exception = new TeamException[] { null };
				currentResource.accept(new IResourceVisitor() {
					@Override
					public boolean visit(IResource resource) {
						try {
							ICVSResource mResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
							// Add the resource is its not already managed and it was either
							// added explicitly (is equal currentResource) or is not ignored
							if (! isManaged(mResource) && (currentResource.equals(resource) || ! mResource.isIgnored())) {
								if (resource.getType() == IResource.FILE) {
									KSubstOption ksubst= getKSubstOption((IFile)resource);
									Set set = files.get(ksubst);
									if (set == null) {
										set = new HashSet();
										files.put(ksubst, set);
									}
									set.add(mResource);
								} else if (!isManagedProject(resource, mResource)){
									folders.add(mResource);
								}
							}
							// Always return true and let the depth determine if children are visited
							return true;
						} catch (CVSException e) {
							exception[0] = e;
							return false;
						}
					}

				}, depth, false);
				if (exception[0] != null) {
					throw exception[0];
				}
			} catch (CoreException e) {
				throw CVSException.wrapException(e);
			}
		}
		// If an exception occured during the visit, throw it here
		if (eHolder[0] != null)
			throw eHolder[0];
		
		// Add the folders, followed by files!
		progress.beginTask(null, files.size() * 10 + (folders.isEmpty() ? 0 : 10));
		try {
			if (!folders.isEmpty()) {
				Session session = new Session(getRemoteLocation(provider), getLocalRoot(provider), true /* output to console */);
				session.open(Policy.subMonitorFor(progress, 2), true /* open for modification */);
				try {
					IStatus status = Command.ADD.execute(
						session,
						Command.NO_GLOBAL_OPTIONS,
						Command.NO_LOCAL_OPTIONS,
						folders.toArray(new ICVSResource[folders.size()]),
						null,
						Policy.subMonitorFor(progress, 8));
					if (status.getCode() == CVSStatus.SERVER_ERROR) {
						throw new CVSServerException(status);
					}
				} finally {
					session.close();
				}
			}
			for (Map.Entry entry : files.entrySet()) {
				final KSubstOption ksubst = (KSubstOption) entry.getKey();
				final Set set = (Set) entry.getValue();
				Session session = new Session(getRemoteLocation(provider), getLocalRoot(provider), true /* output to console */);
				session.open(Policy.subMonitorFor(progress, 2), true /* open for modification */);
				try {
					IStatus status = Command.ADD.execute(
							session,
							Command.NO_GLOBAL_OPTIONS,
							new LocalOption[] { ksubst },
							(ICVSResource[])set.toArray(new ICVSResource[set.size()]),
							null,
							Policy.subMonitorFor(progress, 8));
					if (status.getCode() == CVSStatus.SERVER_ERROR) {
						throw new CVSServerException(status);
					}
				} finally {
					session.close();
				}
			}
		} finally {
			progress.done();
		}
	}
	
	/*
	 * Return true if the resource is a project that is already a CVS folder
	 */
	protected boolean isManagedProject(IResource resource, ICVSResource resource2) throws CVSException {
		return resource.getType() == IResource.PROJECT && ((ICVSFolder)resource2).isCVSFolder();
	}

	/*
	 * Consider a folder managed only if it's also a CVS folder
	 */
	protected boolean isManaged(ICVSResource cvsResource) throws CVSException {
		return cvsResource.isManaged() && (!cvsResource.isFolder() || ((ICVSFolder)cvsResource).isCVSFolder());
	}

	@Override
	protected String getErrorMessage(IStatus[] failures, int totalOperations) {
		return CVSUIMessages.AddAction_addFailed; 
	}
	
	protected KSubstOption getKSubstOption(IFile file) {
		final String extension= file.getFileExtension();
		final Integer mode;
		if (extension == null) {
			mode= (Integer)fModesForFiles.get(file.getName());
		} else { 
			mode= (Integer)fModesForExtensions.get(extension);
		}
		if (mode != null) {
			return mode.intValue() == Team.BINARY ? Command.KSUBST_BINARY : KSubstOption.getDefaultTextMode();            
		} else {
			return KSubstOption.fromFile(file);
		}
	}

}
