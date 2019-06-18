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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.DeferredResourceChangeHandler;

/*
 * Listens to CVS meta-file changes and notifies the EclipseSynchronizer of changes made to sync files 
 * by 3rd parties.
 * 
 * If CVS meta-directories are created outside of the CVS plugin their team-private state will be set
 * by this listener however this change won't be known to other plugins because it does not generate 
 * a delta. As a result views, such as the navigator, may show CVS team-private directories. There
 * are some common scenarios where a user may (depending on the order of delta traversal)  see 
 * this behavior:
 * 
 * 1. A user has an existing CVS project outside of Eclipse. By creating the project in Eclipse to point
 * to the existing location the project's contents will be brought into Eclipse and the CVS folders
 * will be marlked as team-private but other delta listeners that have handled the event already won't receive
 * notification that the resource is now team-private. As a result, the user may have to close views or 
 * restart the workbench to have the CVS folders filtered.
 * 
 * 2. A user performs CVS command line operations outside of Eclipse that result in new CVS folders.
 * From Eclipse the refresh local will bring in the new folders and they will be marked as team-private.
 * But as in 1, they may not appear in the UI.
 * 
 * See: http://dev.eclipse.org/bugs/show_bug.cgi?id=12386
 */
public class SyncFileChangeListener implements IResourceChangeListener {
	
	// consider the following changes types and ignore the others (e.g. marker and description changes are ignored)
	protected int INTERESTING_CHANGES = 	IResourceDelta.CONTENT | 
																	IResourceDelta.MOVED_FROM | 
																	IResourceDelta.MOVED_TO |
																	IResourceDelta.OPEN | 
																	IResourceDelta.REPLACED |
																	IResourceDelta.TYPE;
	
	protected boolean isProjectOpening = false;
	
	protected static DeferredResourceChangeHandler deferredHandler = new DeferredResourceChangeHandler();
	
	/**
	 * This accessor is for use by test cases only.
	 * 
	 * @return Returns the deferredHandler.
	 */
	public static DeferredResourceChangeHandler getDeferredHandler() {
		return deferredHandler;
	}
	
	/*
	 * When a resource changes this method will detect if the changed resources is a meta file that has changed 
	 * by a 3rd party. For example, if the command line tool was run and then the user refreshed from local. To
	 * distinguish changes made by this class and thoses made by others a modification stamp is persisted with each
	 * metafile.
	 * 
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			final Set changedContainers = new HashSet();
			final Set externalDeletions = new HashSet();
			
			setProjectOpening(false);
			
			event.getDelta().accept(new IResourceDeltaVisitor() {

				public boolean visit(IResourceDelta delta) {
					IResource resource = delta.getResource();
					
					if(resource.getType()==IResource.ROOT) {
						// continue with the delta
						return true;
					}
					
					if (resource.getType() == IResource.PROJECT) {
						// If the project is not accessible, don't process it
						if (!resource.isAccessible()) return false;
						setProjectOpening((delta.getFlags() & IResourceDelta.OPEN) != 0);
					}
															
					String name = resource.getName();
					int kind = delta.getKind();
					
					// if the file has changed but not in a way that we care
					// then ignore the change (e.g. marker changes to files).
					if(kind == IResourceDelta.CHANGED && 
						(delta.getFlags() & INTERESTING_CHANGES) == 0) {
							return true;
					}
										
					if(name.equals(SyncFileWriter.CVS_DIRNAME)) {
						handleCVSDir((IContainer)resource, kind);
						// if the project is opening there is no need to notify about chagned CVs/ meta files
						// they will all be read from disk.
						if(isProjectOpening()) return false;
					} else {
						// Inform the synchronizer about folder creations
						if(isProjectOpening()) return true;
					}
					
					if(isMetaFile(resource)) {
						IResource[] toBeNotified = handleChangedMetaFile(resource);
						if(toBeNotified.length>0 && isModifiedBy3rdParty(resource)) {
							Collections.addAll(changedContainers, toBeNotified);
							if(Policy.DEBUG_METAFILE_CHANGES) {
								System.out.println("[cvs] metafile changed by 3rd party: " + resource.getFullPath()); //$NON-NLS-1$
							}
							return false; /*don't visit any children we have all the information we need*/
						}
					} else if(isIgnoreFile(resource) && isModifiedBy3rdParty(resource)) {
						deferredHandler.ignoreFileChanged((IFile)resource);
					} else if (isExternalDeletion(resource, kind)) {
						externalDeletions.add(resource);
					} else if (kind == IResourceDelta.ADDED && isRecreation(resource)) {
						deferredHandler.recreated(resource);
					}
					return true;
				}
			}, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
				
			if(!changedContainers.isEmpty() || !externalDeletions.isEmpty()) {
				EclipseSynchronizer.getInstance().syncFilesChangedExternally(
					(IContainer[])changedContainers.toArray(new IContainer[changedContainers.size()]),
					(IFile[]) externalDeletions.toArray(new IFile[externalDeletions.size()]));
			}			
		} catch(CoreException e) {
			CVSProviderPlugin.log(e);
		}
	}

	/**
	 * Treat a resource as an external deletion if 
	 *   - it is a file 
	 *   - the delta says the file was removed
	 *   - the file is not managed but its parent is a CVS folder
	 * 
	 * There will be some false positives but the reaction to this situation
	 * is to purge the cahced CVS meta-information so nothing bad will happen
	 * for the false positives.
	 * 
	 * @param resource
	 * @param kind
	 * @return
	 */
	protected boolean isExternalDeletion(IResource resource, int kind) {
		if (kind != IResourceDelta.REMOVED) return false;
		if (resource.getType() != IResource.FILE) return false;
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor((IFile)resource);
		try {
			return (!file.isManaged() && file.getParent().isCVSFolder() && file.getParent().exists());
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
			return false;
		}
	}

	/*
	 * Consider non-existing resources as being recently deleted and thus modified, and resources
	 * with modification stamps that differ from when the CVS plugin last modified the meta-file.
	 */
	protected boolean isModifiedBy3rdParty(IResource resource) {
		if(!resource.exists()) return true;
		long modStamp = resource.getModificationStamp();
		Long whenWeWrote;
		try {
			whenWeWrote = (Long)resource.getSessionProperty(SyncFileWriter.MODSTAMP_KEY);
		} catch(CoreException e) {
			CVSProviderPlugin.log(e);
			whenWeWrote = null;
		}
		return (whenWeWrote==null || whenWeWrote.longValue() != modStamp);
	}
	
	/*
	 * If it's a new CVS directory with the canonical child metafiles then mark it as team-private. Otherwise
	 * if changed or deleted
	 */	
	protected void handleCVSDir(IContainer cvsDir, int kind) {
		if((kind & IResourceDelta.ALL_WITH_PHANTOMS)!=0) {
			if(kind==IResourceDelta.ADDED) {
				// should this dir be made team-private? If it contains CVS/Root and CVS/Repository then yes!
				IFile rootFile = cvsDir.getFile(new Path(SyncFileWriter.ROOT));
				IFile repositoryFile = cvsDir.getFile(new Path(SyncFileWriter.REPOSITORY));
				if(rootFile.exists() && repositoryFile.exists() && !cvsDir.isTeamPrivateMember()) {
					try {
						// TODO: Is this considered a tree modification?
						cvsDir.setTeamPrivateMember(true);			
						if(Policy.DEBUG_METAFILE_CHANGES) {
							System.out.println("[cvs] found a new CVS meta folder, marking as team-private: " + cvsDir.getFullPath()); //$NON-NLS-1$
						}
					} catch(CoreException e) {
						CVSProviderPlugin.log(CVSException.wrapException(cvsDir, CVSMessages.SyncFileChangeListener_errorSettingTeamPrivateFlag, e)); 
					}
				}
			}
		}
	}
	
	protected boolean isIgnoreFile(IResource resource) {
		return resource.getType() == IResource.FILE &&
			resource.getName().equals(SyncFileWriter.IGNORE_FILE);
	}
	
	private boolean isRecreation(IResource resource) {
		return EclipseSynchronizer.getInstance().wasPhantom(resource);
	}
	
	/*
	 * It's a meta file if it's parent is a team-private CVS folder.
	 */
	protected boolean isMetaFile(IResource resource) {
		IContainer parent = resource.getParent();		
		return resource.getType() == IResource.FILE &&
					parent!=null && 
					parent.getName().equals(SyncFileWriter.CVS_DIRNAME) &&
					(parent.isTeamPrivateMember() || !parent.exists());
	}
	
	/*
	 * This is a meta file (e.g. folder/CVS/Entries), notify that 'folder' and it's immediate children 
	 * may have their CVS sync state changed. If the 'folder' is deleted than no notification is
	 * required.
	 */
	protected IContainer[] handleChangedMetaFile(IResource resource) {		
		IContainer changedContainer = resource.getParent().getParent();
		if(changedContainer.exists()) {
			return new IContainer[] {changedContainer};
		} else {
			return new IContainer[0];
		}
	}
	
	/**
	 * @return boolean
	 */
	public boolean isProjectOpening() {
		return isProjectOpening;
	}

	/**
	 * Sets the isProjectOpening.
	 * @param isProjectOpening The isProjectOpening to set
	 */
	public void setProjectOpening(boolean isProjectOpening) {
		this.isProjectOpening = isProjectOpening;
	}
}
