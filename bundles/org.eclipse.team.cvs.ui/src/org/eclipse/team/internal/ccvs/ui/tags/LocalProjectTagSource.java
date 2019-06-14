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
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;

/**
 * Tag source that gets its tags from the projects exist in the workspace
 */
public class LocalProjectTagSource extends TagSource {
	
	public static TagSource create(IProject seedProject) {
		try {
			ICVSRemoteFolder seedFolder = ((ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(seedProject));
			ICVSRemoteFolder[] remoteFolders = getProjectRemoteFolders();
			if (remoteFolders.length == 1) {
				// There are no other projects to get tags from so return null
				return null;
			}
			return new LocalProjectTagSource(seedFolder, remoteFolders);
		} catch (CVSException e) {
			// Log and return null
			CVSUIPlugin.log(e);
			return null;
		}
	}
	
	private ICVSRemoteFolder seedFolder;
	private ICVSRemoteFolder[] remoteFolders;

	private  LocalProjectTagSource(ICVSRemoteFolder seedFolder, ICVSRemoteFolder[] remoteFolders) {
		this.seedFolder = seedFolder;
		this.remoteFolders = remoteFolders;
	}

	/*
	 * Return the list of remote folders for the projects in the workspace mapped to the given repository
	 */
	private static ICVSRemoteFolder[] getProjectRemoteFolders() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List<ICVSRemoteFolder> result = new ArrayList<>();
		for (IProject project : projects) {
			try {
				if (project.isAccessible() && RepositoryProvider.isShared(project)) {
					ICVSRemoteFolder remote = (ICVSRemoteFolder)CVSWorkspaceRoot.getRemoteResourceFor(project);
					if (remote != null) {
						result.add(remote);
					}
				}
			} catch (CVSException e) {
				// Log and continue
				CVSUIPlugin.log(e);
			}
		}
		return result.toArray(new ICVSRemoteFolder[result.size()]);
	}

	@Override
	public CVSTag[] refresh(boolean bestEffort, IProgressMonitor monitor) throws TeamException {
		// This tag source should not be refreshed
		return new CVSTag[0];
	}

	@Override
	public ICVSRepositoryLocation getLocation() {
		return seedFolder.getRepository();
	}

	@Override
	public String getShortDescription() {
		return NLS.bind(CVSUIMessages.LocalProjectTagSource_0, new String[] { Integer.toString(remoteFolders.length) }); 
	}

	@Override
	public void commit(CVSTag[] tags, boolean replace, IProgressMonitor monitor) throws CVSException {
		// Does not commit tags
	}

	@Override
	public ICVSResource[] getCVSResources() {
		return remoteFolders;
	}
	
	@Override
	public CVSTag[] getTags(int type) {
		if (type == CVSTag.HEAD || type == BASE) {
			return super.getTags(type);
		}
		// Accumulate the tags for all folders
		Set<CVSTag> allTags = new HashSet<>();
		for (ICVSRemoteFolder folder : remoteFolders) {
			CVSTag[] tags = SingleFolderTagSource.getTags(folder, type);
			allTags.addAll(Arrays.asList(tags));
		}
		// Exclude the tags for the seedFolder
		CVSTag[] tags = SingleFolderTagSource.getTags(seedFolder, type);
		allTags.removeAll(Arrays.asList(tags));
		return allTags.toArray(new CVSTag[allTags.size()]);
	}

}
