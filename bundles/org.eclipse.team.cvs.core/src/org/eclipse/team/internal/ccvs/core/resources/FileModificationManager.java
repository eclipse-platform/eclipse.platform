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
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.ResourceStateChangeListeners;

/**
 * This class performs several functions related to determining the modified
 * status of files under CVS control. First, it listens for change delta's for
 * files and brodcasts them to all listeners. It also registers as a save
 * participant so that deltas generated before the plugin are loaded are not
 * missed. Secondly, it listens for CVS resource state change events and uses
 * these to properly mark files and folders as modified.
 */
public class FileModificationManager implements IResourceChangeListener {
	
	private static final QualifiedName UPDATE_TIMESTAMP = new QualifiedName(CVSProviderPlugin.ID, "update-timestamp"); //$NON-NLS-1$
	
	/* private */Set<IResource> modifiedResources = new HashSet<>();

	// consider the following changes types and ignore the others (e.g. marker and description changes are ignored)
	protected int INTERESTING_CHANGES = 	IResourceDelta.CONTENT | 
																	IResourceDelta.MOVED_FROM | 
																	IResourceDelta.MOVED_TO |
																	IResourceDelta.OPEN | 
																	IResourceDelta.REPLACED |
																	IResourceDelta.TYPE;

	/**
	 * Listen for file modifications and fire modification state changes
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) {
					IResource resource = delta.getResource();
					
					if (resource.getType()==IResource.PROJECT) {
						IProject project = (IProject)resource;
						if (!project.isAccessible()) {
							return false;
						}
						if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
							return false;
						} 
						if (RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId()) == null) {
							return false;
						}
					}
					
					if (resource.getType()==IResource.FILE && delta.getKind() == IResourceDelta.CHANGED && resource.exists()) {
						int flags = delta.getFlags();
						if((flags & INTERESTING_CHANGES) != 0) {
							resourceChanged(resource, false);
						}
					} else if (delta.getKind() == IResourceDelta.ADDED) {
						try {
							EclipseSynchronizer.getInstance().handleAdded(resource);
						} catch (CVSException e) {
							CVSProviderPlugin.log(e);
						}
						resourceChanged(resource, true);
					} else if (delta.getKind() == IResourceDelta.REMOVED) {
						try {
							EclipseSynchronizer.getInstance().handleDeleted(resource);
						} catch (CVSException e) {
							CVSProviderPlugin.log(e);
						}
						modifiedResources.add(resource);
					}

					return true;
				}
			});
			if (!modifiedResources.isEmpty()) {
				ResourceStateChangeListeners.getListener().resourceModified(
					modifiedResources.toArray(new IResource[modifiedResources.size()]));
				modifiedResources.clear();
			}
		} catch (CoreException e) {
			CVSProviderPlugin.log(e);
		}

	}

	/**
	 * Method updated flags the objetc as having been modfied by the updated
	 * handler. This flag is read during the resource delta to determine whether
	 * the modification made the file dirty or not.
	 * 
	 * @param mFile
	 */
	public void updated(ICVSFile mFile) {
		try {
			if (mFile instanceof EclipseFile) {
				IFile file = (IFile)mFile.getIResource();
				file.setSessionProperty(UPDATE_TIMESTAMP, Long.valueOf(file.getModificationStamp()));
			}
		} catch (CoreException e) {
			CVSProviderPlugin.log(e);
		}
	}
	
	/*
	 * Handle added and changed resources by signaling the change to the corresponding
	 * CVS resource and recording the change for broadcast to interested listeners.
	 */
	/* private */void resourceChanged(IResource resource, boolean addition) {
		if (isCleanUpdate(resource)) return;
		try {
			EclipseResource cvsResource = (EclipseResource)CVSWorkspaceRoot.getCVSResourceFor(resource);
			if (!cvsResource.isIgnored()) {
				cvsResource.handleModification(addition);
				modifiedResources.add(resource);
			}
			// see bug 170743
			// ignored .cvsignore should always be clean and do not affect the path
			if(cvsResource.getName().equals(".cvsignore") && cvsResource.isIgnored()){ //$NON-NLS-1$
				EclipseSynchronizer.getInstance().setModified((EclipseFile) cvsResource, ICVSFile.CLEAN);
				modifiedResources.add(resource);
			}
		} catch (CVSException e) {
			// Log the exception and continue
			CVSProviderPlugin.log(e);
		}
	}

	/**
	 * If the file was the result of a clean update, the cached timestamp will
	 * be removed.
	 * 
	 * @param resource
	 * @return boolean
	 */
	private boolean isCleanUpdate(IResource resource) {
		if(resource.getType() != IResource.FILE) return false;
		long modStamp = resource.getModificationStamp();
		Long whenWeWrote;
		try {
			whenWeWrote = (Long)resource.getSessionProperty(UPDATE_TIMESTAMP);
			resource.setSessionProperty(UPDATE_TIMESTAMP, null);
		} catch(CoreException e) {
			CVSProviderPlugin.log(e);
			whenWeWrote = null;
		}
		return (whenWeWrote!=null && whenWeWrote.longValue() == modStamp);
	}
}

