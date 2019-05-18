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
package org.eclipse.team.core.mapping;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * The purpose of this interface is to provide support for model level
 * auto-merging. It is helpful in the cases where a file may contain multiple
 * model elements or a model element consists of multiple files. It can also be
 * used for cases where there is a one-to-one mapping between model elements and
 * files, although <code>IStorageMerger</code> can also be used in that case.
 *
 * Clients should group resource mappings by model provider and then attempt to
 * obtain a merger from the model provider using the adaptable mechanism as
 * follows:
 *
 * <pre>
 *      Object o = mapping.getModelProvider().getAdapter(IResourceMappingMerger.class);
 *      if (o instanceof IResourceMappingMerger.class) {
 *         IResourceMappingMerger merger = (IResourceMappingMerger)o;
 *         ...
 *      }
 * </pre>
 *
 * @see ResourceMappingMerger
 * @see IStorageMerger
 * @see org.eclipse.core.resources.mapping.ResourceMapping
 * @see org.eclipse.core.resources.mapping.ModelProvider
 * @see org.eclipse.team.core.mapping.IMergeContext
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Client should subclass {@link ResourceMappingMerger} instead.
 */
public interface IResourceMappingMerger {

	/**
	 * Attempt to automatically merge the mappings of the merge context(<code>MergeContext#getMappings()</code>).
	 * The merge context provides access to the out-of-sync resources (<code>MergeContext#getSyncInfoTree()</code>)
	 * associated with the mappings to be merged. The set of provided mappings
	 * may come from multiple model providers. A particular implementation of
	 * this interface should only merge the mappings associated with their model
	 * provider. Also, the set of resources may contain additional resources
	 * that are not part of the mappings being merged. Implementors of this
	 * interface should use the mappings to determine which resources to merge
	 * and what additional semantics can be used to attempt the merge.
	 * <p>
	 * The type of merge to be performed depends on what is returned by the
	 * <code>MergeContext#getType()</code> method. If the type is
	 * <code>MergeContext.TWO_WAY</code> the merge will replace the local
	 * contents with the remote contents, ignoring any local changes. For
	 * <code>THREE_WAY</code>, the base is used to attempt to merge remote
	 * changes with local changes.
	 * <p>
	 * If merging was not possible for one or more of the mappings to which this
	 * merge applies, these mappings should be returned in an
	 * <code>MergeStatus</code> whose code is
	 * <code>MergeStatus.CONFLICTS</code> and which provides access to the
	 * mappings which could not be merged. Note that it is up to the model to
	 * decide whether it wants to break one of the provided resource mappings
	 * into several sub-mappings and attempt auto-merging at that level.
	 *
	 * @param mergeContext a context that provides access to the resources
	 *            involved in the merge. The context must not be
	 *            <code>null</code>.
	 * @param monitor a progress monitor
	 * @return a status indicating the results of the operation. A code of
	 *         <code>MergeStatus.CONFLICTS</code> indicates that some or all
	 *         of the resource mappings could not be merged. The mappings that
	 *         were not merged are available using
	 *         <code>MergeStatus#getConflictingMappings()</code>
	 * @throws CoreException if errors occurred
	 */
	public IStatus merge(IMergeContext mergeContext,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * Return the scheduling rule that is required to merge
	 * all the changes that apply to this merger in the given
	 * context. When calling {@link #merge(IMergeContext, IProgressMonitor)},
	 * clients must ensure that they either have obtained
	 * a rule that covers the rule returned by this method or
	 * they must not hold any rule.
	 * @param context the context that contains the changes to be merged
	 * @return the scheduling rule required by this merger to merge all
	 * the changes in the given context belonging to the merger's
	 * model provider.
	 */
	public ISchedulingRule getMergeRule(IMergeContext context);

	/**
	 * Validate an auto-merge for the given context. This
	 * method must be invoked for all mergers involved
	 * in the merge before the auto-merge is attempted.
	 * The purpose of the validation is to indicate whether there
	 * are conditions in the merge context that make an auto-merge
	 * undesirable. The purpose is not to indicate that conflicts
	 * exist (this is done by the <code>merge</code> method) but instead
	 * to indicate that the nature of one of more incoming changes
	 * is such that performing an auto-merge may be undesirable.
	 * <p>
	 * Clients should validate before performing the merge and, if
	 * any of the returned status are not OK, should prompt the
	 * user to make them aware of the potential side effects.
	 * The user may still decide to attempt an auto-merge, in which case
	 * the client may still invoke the <code>merge</code> method.
	 *
	 * @param mergeContext a context that provides access to the resources
	 *            involved in the merge. The context must not be
	 *            <code>null</code>.
	 * @param monitor a progress monitor
	 * @return a status indicating any potential side effects of
	 * performing an auto-merge.
	 */
	public IStatus validateMerge(IMergeContext mergeContext,
			IProgressMonitor monitor);

}
