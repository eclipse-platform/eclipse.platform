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
package org.eclipse.team.internal.ccvs.core.util;

 
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;

/**
 * Cleanup any CVS folders that were copied by a builder. This will also clean up
 * CVS folders that were copied by the user since the last auto-build.
 */
public class BuildCleanupListener implements IResourceDeltaVisitor, IResourceChangeListener {
	
	public static IResource getResourceFor(IProject container, IResource destination, IPath originating) {
		switch(destination.getType()) {
			case IResource.FILE : return container.getFile(originating); 			
			case IResource.FOLDER: return container.getFolder(originating);
			case IResource.PROJECT: return ResourcesPlugin.getWorkspace().getRoot().getProject(originating.toString());
		}
		return destination;
	}
	
	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		boolean movedFrom = (delta.getFlags() & IResourceDelta.MOVED_FROM) > 0;
		switch (delta.getKind()) {
			case IResourceDelta.ADDED :
				// make sure the added resource isn't a phantom
				if (resource.exists()) {
					if (EclipseSynchronizer.getInstance().wasPhantom(resource)) {
						EclipseSynchronizer.getInstance().resourcesRecreated(new IResource[] { resource }, null);
					}
					if (resource.getType() == IResource.FOLDER) {
						if (resource.getName().equals(SyncFileWriter.CVS_DIRNAME)) {
							handleOrphanedSubtree(resource.getParent());
						} else {
							handleOrphanedSubtree((IContainer)resource);
						}
					}
				}
				break;
			case IResourceDelta.CHANGED :
				// This state means there is a resource before and after but changes were made by deleting and moving.
				// For files, we shouldn'd do anything.
				// For folders, we should purge the CVS info
				if (movedFrom && resource.getType() == IResource.FOLDER && resource.exists()) {
					// When folders are moved, purge the CVS folders
					return ! handleOrphanedSubtree((IContainer)resource);
				}
				break;
		}
		return true;
	}
	
	/*
	 * Determine if the container is an orphaned subtree. 
	 * If it is, handle it and return true. 
	 * Otherwise, return false
	 */
	private boolean handleOrphanedSubtree(IContainer container) {
		try {
			if (CVSWorkspaceRoot.isOrphanedSubtree(container)) {
				ICVSFolder mFolder = CVSWorkspaceRoot.getCVSFolderFor(container);
				mFolder.unmanage(null);
				return true;
			}
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
		return false;
	}
	
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			IResourceDelta root = event.getDelta();
			IResourceDelta[] projectDeltas = root.getAffectedChildren();
			for (IResourceDelta delta : projectDeltas) {
				IResource resource = delta.getResource();
				
				if (resource.getType() == IResource.PROJECT) {
					// If the project is not accessible, don't process it
					if (!resource.isAccessible()) continue;
				}
				
				RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
				
				// Make sure that the project is a CVS folder.
				ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(resource.getProject());
				if (provider != null) {
					try {
						if (! folder.isCVSFolder()) {
							RepositoryProvider.unmap(resource.getProject());
							provider = null;
						}
					} catch (TeamException e) {
						CVSProviderPlugin.log(e);
					}
				}
				
				// if a project is moved the originating project will not be associated with the CVS provider
				// however listeners will probably still be interested in the move delta.
				if ((delta.getFlags() & IResourceDelta.MOVED_TO) > 0) {																
					IResource destination = getResourceFor(resource.getProject(), resource, delta.getMovedToPath());
					provider = RepositoryProvider.getProvider(destination.getProject());
				}
				
				if(provider!=null) {
					// Traverse the delta is a runnable so that files are only written at the end
					folder.run(monitor -> {
						try {
							delta.accept(BuildCleanupListener.this);
						} catch (CoreException e) {
							Util.logError(CVSMessages.ResourceDeltaVisitor_visitError, e);
						}
					}, Policy.monitorFor(null));
				}
			}
		} catch (CVSException e) {
			Util.logError(CVSMessages.ResourceDeltaVisitor_visitError, e);
		}
	}

}
