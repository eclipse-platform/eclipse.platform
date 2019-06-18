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
 *******************************************************************************/
package org.eclipse.team.core.mapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.MergeStatus;

/**
 * Abstract implementation of {@link IResourceMappingMerger}. This merger
 * delegates the merge of all resources covered by the mappings of the
 * model provider returned from {@link #getModelProvider()} back to the
 * merge context. Subclasses should override the {@link #merge(IMergeContext, IProgressMonitor)}
 * method in order to change this behavior.
 *
 * <p>
 * Clients may subclass this class.
 *
 * @see IResourceMappingMerger
 *
 * @since 3.2
 */
public abstract class ResourceMappingMerger implements IResourceMappingMerger {

	@Override
	public IStatus validateMerge(IMergeContext mergeContext, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	/**
	 * Return the model provider associated with this merger.
	 * @return Return the model provider associated with this merger.
	 */
	protected abstract ModelProvider getModelProvider();

	/**
	 * Return the scheduling rule required to merge all the
	 * changes in the context for the model provider of this merger.
	 * By default, return a rule that covers all the projects for the mappings
	 * that belong to the model provider of this merger.
	 * @param context the context that contains the changes to be merged
	 * @return the scheduling rule required by this merger to merge all
	 * the changes in the given context belonging to the merger's
	 * model provider.
	 * @see org.eclipse.team.core.mapping.IResourceMappingMerger#getMergeRule(org.eclipse.team.core.mapping.IMergeContext)
	 */
	@Override
	public ISchedulingRule getMergeRule(IMergeContext context) {
		ResourceMapping[] mappings = context.getScope().getMappings(getModelProvider().getId());
		ISchedulingRule rule = null;
		for (ResourceMapping mapping : mappings) {
			IProject[] mappingProjects = mapping.getProjects();
			for (IProject project : mappingProjects) {
				if (rule == null) {
					rule = project;
				} else {
					rule = MultiRule.combine(rule, project);
				}
			}
		}
		return rule;
	}

	/**
	 * A default implementation of merge that attempts to merge all the mappings
	 * in the context.
	 * @param mergeContext the context
	 * @param monitor a progress monitor
	 * @return a status indicating whether the merge was successful
	 * @throws CoreException if an error occurred
	 * @see org.eclipse.team.core.mapping.IResourceMappingMerger#merge(org.eclipse.team.core.mapping.IMergeContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus merge(IMergeContext mergeContext, IProgressMonitor monitor) throws CoreException {
		IDiff[] deltas = getSetToMerge(mergeContext);
		IStatus status = mergeContext.merge(deltas, false /* don't force */, monitor);
		return covertFilesToMappings(status, mergeContext);
	}

	private IDiff[] getSetToMerge(IMergeContext mergeContext) {
		ResourceMapping[] mappings = mergeContext.getScope().getMappings(getModelProvider().getDescriptor().getId());
		Set<IDiff> result = new HashSet<>();
		for (ResourceMapping mapping : mappings) {
			ResourceTraversal[] traversals = mergeContext.getScope().getTraversals(mapping);
			IDiff[] deltas = mergeContext.getDiffTree().getDiffs(traversals);
			Collections.addAll(result, deltas);
		}
		return result.toArray(new IDiff[result.size()]);
	}

	private IStatus covertFilesToMappings(IStatus status, IMergeContext mergeContext) {
		if (status.getCode() == IMergeStatus.CONFLICTS) {
			// In general, we can't say which mapping failed so return them all
			return new MergeStatus(status.getPlugin(), status.getMessage(), mergeContext.getScope().getMappings(getModelProvider().getDescriptor().getId()));
		}
		return status;
	}
}
