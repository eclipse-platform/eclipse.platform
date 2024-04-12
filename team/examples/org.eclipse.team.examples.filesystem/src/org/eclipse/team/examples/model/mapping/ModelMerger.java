/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ResourceMappingMerger;
import org.eclipse.team.core.mapping.provider.MergeStatus;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.team.examples.model.ModelProject;

/**
 * A resource mapping merger for our example model
 */
public class ModelMerger extends ResourceMappingMerger {

	private final org.eclipse.team.examples.model.mapping.ExampleModelProvider provider;

	public ModelMerger(org.eclipse.team.examples.model.mapping.ExampleModelProvider provider) {
		this.provider = provider;
	}

	@Override
	protected org.eclipse.core.resources.mapping.ModelProvider getModelProvider() {
		return provider;
	}

	@Override
	public IStatus merge(IMergeContext mergeContext, IProgressMonitor monitor) throws CoreException {
		try {
			IStatus status;
			// Only override the merge for three-way synchronizations
			if (mergeContext.getType() == ISynchronizationContext.THREE_WAY) {
				monitor.beginTask("Merging model elements", 100);
				status = mergeModelElements(mergeContext, SubMonitor.convert(monitor, 50));
				// Stop the merge if there was a failure
				if (!status.isOK())
					return status;
				// We need to wait for any background processing to complete for the context
				// so the diff tree will be up-to-date when we delegate the rest of the merge
				// to the superclass
				try {
					Job.getJobManager().join(mergeContext, SubMonitor.convert(monitor, 50));
				} catch (InterruptedException e) {
					// Ignore
				}
				// Delegate the rest of the merge to the superclass
				status = super.merge(mergeContext, monitor);
			} else {
				status = super.merge(mergeContext, monitor);
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	/*
	 * Merge all the model element changes in the context
	 */
	private IStatus mergeModelElements(IMergeContext mergeContext, IProgressMonitor monitor) throws CoreException {
		try {
			IDiff[] modeDiffs = getModDiffs(mergeContext);
			List<IDiff> failures = new ArrayList<>();
			monitor.beginTask(null, 100 * modeDiffs.length);
			for (IDiff diff : modeDiffs) {
				if (!mergeModelElement(mergeContext, diff, SubMonitor.convert(monitor, 100))) {
					failures.add(diff);
				}
			}
			if (failures.size() > 0) {
				return new MergeStatus(FileSystemPlugin.ID, "Several objects could not be merged", getMappings(failures));
			}
			return Status.OK_STATUS;
		} finally {
			monitor.done();
		}
	}

	private ResourceMapping[] getMappings(List failures) {
		List<ResourceMapping> mappings = new ArrayList<>();
		for (Iterator iter = failures.iterator(); iter.hasNext();) {
			IDiff diff = (IDiff) iter.next();
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			ModelObjectDefinitionFile file = (ModelObjectDefinitionFile)ModelObject.create(resource);
			mappings.add(file.getAdapter(ResourceMapping.class));
		}
		return mappings.toArray(new ResourceMapping[mappings.size()]);
	}

	/*
	 * Return all the diffs for MOD files.
	 */
	private IDiff[] getModDiffs(IMergeContext mergeContext) {
		final List<IDiff> result = new ArrayList<>();
		mergeContext.getDiffTree().accept(getModelProjectTraversals(mergeContext), diff -> {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (ModelObjectDefinitionFile.isModFile(resource)) {
				result.add(diff);
			}
			return true;
		});
		return result.toArray(new IDiff[result.size()]);
	}

	/*
	 * Return a traversal that covers all the model projects in the scope of the merge.
	 */
	private ResourceTraversal[] getModelProjectTraversals(IMergeContext mergeContext) {
		IProject[] scopeProjects = mergeContext.getScope().getProjects();
		List<IResource> modelProjects = new ArrayList<>();
		for (IProject project : scopeProjects) {
			try {
				if (ModelProject.isModProject(project)) {
					modelProjects.add(project);
				}
			} catch (CoreException e) {
				FileSystemPlugin.log(e);
			}
		}
		if (modelProjects.isEmpty())
			return new ResourceTraversal[0];
		return new ResourceTraversal[] {
				new ResourceTraversal(modelProjects.toArray(new IResource[modelProjects.size()]),
						IResource.DEPTH_INFINITE, IResource.NONE)
		};
	}

	/*
	 * Merge the model definition file and all the element files it contains.
	 */
	private boolean mergeModelElement(IMergeContext mergeContext, IDiff diff, IProgressMonitor monitor) throws CoreException {
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			if (twd.getDirection() == IThreeWayDiff.INCOMING
					|| twd.getDirection() == IThreeWayDiff.CONFLICTING) {
				IResource resource = ResourceDiffTree.getResourceFor(diff);

				// First, check if a change conflicts with a deletion
				if (twd.getDirection() == IThreeWayDiff.CONFLICTING) {
					if (!resource.exists())
						return false;
					if (((IResourceDiff)twd.getRemoteChange()).getAfterState() == null)
						return false;
				}

				// First determine the element files and element file changes
				IResourceDiff remoteChange = (IResourceDiff)twd.getRemoteChange();
				IResource[] localElements = getReferencedResources(resource);
				IResource[] baseElements = getReferencedResources(resource.getProject().getName(), remoteChange.getBeforeState(), monitor);
				IResource[] remoteElements = getReferencedResources(resource.getProject().getName(), remoteChange.getAfterState(), monitor);
				IResource[] addedElements = getAddedElements(baseElements, remoteElements);
				// Trick: The removed elements can be obtained by reversing the base and remote and looking for added
				IResource[] removedElements = getAddedElements(remoteElements, baseElements);

				// Check to see if any removed elements have changed locally
				if (hasOutgoingChanges(mergeContext, removedElements)) {
					return false;
				}

				// Now try to merge all the element files involved
				Set<IResource> elementFiles = new HashSet<>();
				elementFiles.addAll(Arrays.asList(baseElements));
				elementFiles.addAll(Arrays.asList(localElements));
				elementFiles.addAll(Arrays.asList(remoteElements));
				if (!mergeElementFiles(mergeContext, elementFiles.toArray(new IResource[elementFiles.size()]), monitor)) {
					return false;
				}

				// Finally, merge the model definition
				if (!resource.exists()) {
					// This is a new model definition so just merge it
					IStatus status = mergeContext.merge(diff, false, monitor);
					if (!status.isOK())
						return false;
				} else {
					// Update the contents of the model definition file
					ModelObjectDefinitionFile file = (ModelObjectDefinitionFile)ModelObject.create(resource);
					elementFiles = new HashSet<>();
					elementFiles.addAll(Arrays.asList(localElements));
					elementFiles.addAll(Arrays.asList(addedElements));
					elementFiles.removeAll(Arrays.asList(removedElements));
					file.setElements(elementFiles.toArray(new IResource[elementFiles.size()]));
					// Let the merge context know we handled the file
					mergeContext.markAsMerged(diff, false, monitor);
				}
			}
		}
		return true;
	}

	private boolean mergeElementFiles(IMergeContext mergeContext, IResource[] resources, IProgressMonitor monitor) throws CoreException {
		IDiff[] diffs = getDiffs(mergeContext, resources);
		IStatus status = mergeContext.merge(diffs, false, monitor);
		return status.isOK();
	}

	private IDiff[] getDiffs(IMergeContext mergeContext, IResource[] resources) {
		Set<IDiff> diffSet = new HashSet<>();
		for (IResource resource : resources) {
			IDiff[] diffs = mergeContext.getDiffTree().getDiffs(resource, IResource.DEPTH_ZERO);
			diffSet.addAll(Arrays.asList(diffs));
		}
		return diffSet.toArray(new IDiff[diffSet.size()]);
	}

	private boolean hasOutgoingChanges(IMergeContext mergeContext, IResource[] removedElements) {
		FastDiffFilter fastDiffFilter = new FastDiffFilter() {
			@Override
			public boolean select(IDiff diff) {
				if (diff instanceof IThreeWayDiff) {
					IThreeWayDiff twd = (IThreeWayDiff) diff;
					return twd.getDirection() == IThreeWayDiff.OUTGOING || twd.getDirection() == IThreeWayDiff.CONFLICTING;
				}
				return false;
			}
		};
		for (IResource resource : removedElements) {
			if  (mergeContext.getDiffTree().hasMatchingDiffs(resource.getFullPath(), fastDiffFilter))
				return true;
		}
		return false;
	}

	private IResource[] getAddedElements(IResource[] baseElements, IResource[] remoteElements) {
		List<IResource> result = new ArrayList<>();
		Set<IResource> base = new HashSet<>();
		Collections.addAll(base, baseElements);
		for (IResource resource : remoteElements) {
			if (!base.contains(resource))
				result.add(resource);
		}
		return result.toArray(new IResource[result.size()]);
	}

	private IResource[] getReferencedResources(IResource resource) throws CoreException {
		if (resource instanceof IFile && resource.exists()) {
			return ModelObjectDefinitionFile.getReferencedResources(resource.getProject().getName(), (IFile) resource);
		}
		return new IResource[0];
	}

	private IResource[] getReferencedResources(String projectName, IFileRevision revision, IProgressMonitor monitor) throws CoreException {
		if (revision != null) {
			return ModelObjectDefinitionFile.getReferencedResources(projectName, revision.getStorage(monitor));
		}
		return new IResource[0];
	}

}
