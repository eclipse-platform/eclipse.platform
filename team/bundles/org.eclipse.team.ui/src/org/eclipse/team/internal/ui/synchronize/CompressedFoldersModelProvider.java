/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Alexander Gurov - bug 230853
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class CompressedFoldersModelProvider extends HierarchicalModelProvider {

	protected class UnchangedCompressedDiffNode extends UnchangedResourceModelElement {
		public UnchangedCompressedDiffNode(IDiffContainer parent, IResource resource) {
			super(parent, resource);
		}

		@Override
		public String getName() {
			IResource resource = getResource();
			return resource.getProjectRelativePath().toString();
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			return TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPRESSED_FOLDER);
		}
	}

	/**
	 * A compressed folder appears under a project and contains out-of-sync resources
	 */
	public class CompressedFolderDiffNode extends SyncInfoModelElement {

		public CompressedFolderDiffNode(IDiffContainer parent, SyncInfo info) {
			super(parent, info);
		}

		@Override
		public String getName() {
			IResource resource = getResource();
			return resource.getProjectRelativePath().toString();
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			return TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPRESSED_FOLDER);
		}
	}

	public static class CompressedFolderModelProviderDescriptor implements ISynchronizeModelProviderDescriptor {
		public static final String ID = TeamUIPlugin.ID + ".modelprovider_compressedfolders"; //$NON-NLS-1$
		@Override
		public String getId() {
			return ID;
		}
		@Override
		public String getName() {
			return TeamUIMessages.CompressedFoldersModelProvider_0;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPRESSED_FOLDER);
		}
	}
	private static final CompressedFolderModelProviderDescriptor compressedDescriptor = new CompressedFolderModelProviderDescriptor();

	public CompressedFoldersModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(configuration, set);
	}

	public CompressedFoldersModelProvider(
			AbstractSynchronizeModelProvider parentProvider,
			ISynchronizeModelElement modelRoot,
			ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(parentProvider, modelRoot, configuration, set);
	}

	@Override
	public ISynchronizeModelProviderDescriptor getDescriptor() {
		return compressedDescriptor;
	}

	@Override
	public ViewerComparator getViewerComparator() {
		return new SynchronizeModelElementComparator() {
			@Override
			protected int compareNames(IResource resource1, IResource resource2) {
				if (resource1.getType() == IResource.FOLDER && resource2.getType() == IResource.FOLDER) {
					return getComparator().compare(resource1.getProjectRelativePath().toString(), resource2.getProjectRelativePath().toString());
				}
				return super.compareNames(resource1, resource2);
			}
		};
	}

	@Override
	protected IDiffElement[] createModelObjects(ISynchronizeModelElement container) {
		IResource resource = null;
		if (container == getModelRoot()) {
			resource = ResourcesPlugin.getWorkspace().getRoot();
		} else {
			resource = container.getResource();
		}
		if(resource != null) {
			if (resource.getType() == IResource.PROJECT) {
				return getProjectChildren(container, (IProject)resource);
			}
			if (resource.getType() == IResource.FOLDER) {
				return getFolderChildren(container, resource);
			}
		}
		return super.createModelObjects(container);
	}

	private IDiffElement[] getFolderChildren(ISynchronizeModelElement parent, IResource resource) {
		// Folders will only contain out-of-sync children
		IResource[] children = getSyncInfoTree().members(resource);
		List<IDiffElement> result = new ArrayList<>();
		for (IResource child : children) {
			if (child.getType() == IResource.FILE) {
				result.add(createModelObject(parent, child));
			}
		}
		return result.toArray(new IDiffElement[result.size()]);
	}

	private IDiffElement[] getProjectChildren(ISynchronizeModelElement parent, IProject project) {
		// The out-of-sync elements could possibly include the project so the code
		// below is written to ignore the project
		SyncInfo[] outOfSync = getSyncInfoTree().getSyncInfos(project, IResource.DEPTH_INFINITE);
		Set<IDiffElement> result = new HashSet<>();
		Set<IResource> resourcesToShow = new HashSet<>();
		for (SyncInfo info : outOfSync) {
			IResource local = info.getLocal();
			if (local.getProjectRelativePath().segmentCount() == 1 && local.getType() == IResource.FILE) {
				resourcesToShow.add(local);
			} else {
				if (local.getType() == IResource.FILE) {
					resourcesToShow.add(local.getParent());
				} else if (local.getType() == IResource.FOLDER){
					resourcesToShow.add(local);
				}
			}
		}
		for (IResource resource : resourcesToShow) {
			result.add(createModelObject(parent, resource));
		}

		return result.toArray(new IDiffElement[result.size()]);
	}

	@Override
	protected ISynchronizeModelElement createModelObject(ISynchronizeModelElement parent, IResource resource) {
		if (resource.getType() == IResource.FOLDER) {
			SyncInfo info = getSyncInfoTree().getSyncInfo(resource);
			ISynchronizeModelElement newNode;
			if(info != null) {
				newNode = new CompressedFolderDiffNode(parent, info);
			} else {
				newNode = new UnchangedCompressedDiffNode(parent, resource);
			}
			addToViewer(newNode);
			return newNode;
		}
		return super.createModelObject(parent, resource);
	}

	/**
	 * Update the viewer for the sync set additions in the provided event.
	 * This method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	@Override
	protected void handleResourceAdditions(ISyncInfoTreeChangeEvent event) {
		SyncInfo[] infos = event.getAddedResources();
		for (SyncInfo info : infos) {
			addResource(info);
		}
	}

	@Override
	protected void addResource(SyncInfo info) {
		IResource local = info.getLocal();
		ISynchronizeModelElement existingNode = getModelObject(local);
		if (existingNode == null) {
			if (local.getType() == IResource.FILE) {
				ISynchronizeModelElement parentNode = getModelObject(local.getParent());
				if (parentNode == null) {
					ISynchronizeModelElement projectNode = getModelObject(local.getProject());
					if (projectNode == null) {
						projectNode = createModelObject(getModelRoot(), local.getProject());
					}
					if (local.getParent().getType() == IResource.PROJECT) {
						parentNode = projectNode;
					} else {
						parentNode = createModelObject(projectNode, local.getParent());
					}
				}
				createModelObject(parentNode, local);
			} else {
				ISynchronizeModelElement projectNode = getModelObject(local.getProject());
				if (projectNode == null) {
					projectNode = createModelObject(getModelRoot(), local.getProject());
				}
				if (local.getProject() != local) {
					createModelObject(projectNode, local);
				}
			}
		} else {
			// Either The folder node was added as the parent of a newly added out-of-sync file
			// or the file was somehow already there so just refresh
			handleChange(existingNode, info);
		}
	}

	@Override
	protected void handleResourceRemovals(ISyncInfoTreeChangeEvent event) {
		IResource[] roots = event.getRemovedSubtreeRoots();

		// First, deal with any projects that have been removed
		List<IResource> removedProjects = new ArrayList<>();
		for (IResource resource : roots) {
			if (resource.getType() == IResource.PROJECT) {
				removeFromViewer(resource);
				removedProjects.add(resource);
			}
		}

		IResource[] resources = event.getRemovedResources();
		List<IResource> resourcesToRemove = new ArrayList<>();
		List<SyncInfo> resourcesToAdd = new ArrayList<>();
		for (IResource resource : resources) {
			if (!removedProjects.contains(resource.getProject())) {
				if (resource.getType() == IResource.FILE) {
					if (isCompressedParentEmpty(resource) && !isOutOfSync(resource.getParent())) {
						// The parent compressed folder is also empty so remove it
						resourcesToRemove.add(resource.getParent());
					} else {
						resourcesToRemove.add(resource);
					}
				} else {
					// A folder has been removed (i.e. is in-sync)
					// but may still contain children
					resourcesToRemove.add(resource);
					resourcesToAdd.addAll(Arrays.asList(getSyncInfosForFileMembers((IContainer)resource)));
				}
			}
		}
		if (!resourcesToRemove.isEmpty()) {
			removeFromViewer(resourcesToRemove.toArray(new IResource[resourcesToRemove.size()]));
		}
		if (!resourcesToAdd.isEmpty()) {
			addResources(resourcesToAdd.toArray(new SyncInfo[resourcesToAdd.size()]));
		}
	}

	@Override
	protected int getLogicalModelDepth(IResource resource) {
		if(resource.getType() == IResource.PROJECT) {
			return IResource.DEPTH_INFINITE;
		} else {
			return IResource.DEPTH_ONE;
		}
	}

	private boolean isCompressedParentEmpty(IResource resource) {
		IContainer parent = resource.getParent();
		if (parent == null
				|| parent.getType() == IResource.ROOT
				|| parent.getType() == IResource.PROJECT) {
			return false;
		}
		return !hasFileMembers(parent);
	}

	private boolean hasFileMembers(IContainer parent) {
		// Check if the sync set has any file children of the parent
		IResource[] members = getSyncInfoTree().members(parent);
		for (IResource member : members) {
			if (member.getType() == IResource.FILE) {
				return true;
			}
		}
		// The parent does not contain any files
		return false;
	}

	private SyncInfo[] getSyncInfosForFileMembers(IContainer parent) {
		// Check if the sync set has any file children of the parent
		List<SyncInfo> result = new ArrayList<>();
		IResource[] members = getSyncInfoTree().members(parent);
		for (IResource member : members) {
			SyncInfo info = getSyncInfoTree().getSyncInfo(member);
			if (info != null) {
				result.add(info);
			}
			if (member instanceof IContainer) {
				result.addAll(Arrays.asList(this.getSyncInfosForFileMembers((IContainer) member)));
			}
		}
		return result.toArray(new SyncInfo[result.size()]);
	}
}
