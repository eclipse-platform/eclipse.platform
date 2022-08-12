/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.resources.team;

import java.util.HashSet;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;

/**
 * Default implementation of IResourceRuleFactory. The teamHook extension
 * may subclass to provide more specialized scheduling rules for workspace operations that
 * they participate in.
 *
 * @see IResourceRuleFactory
 * @since 3.0
 */
public class ResourceRuleFactory implements IResourceRuleFactory {
	private final IWorkspace workspace;

	/**
	 * Creates a new default resource rule factory. This constructor must only
	 * be called by subclasses.
	 * @deprecated this constructor relies on the workspace already initialized and
	 *             accessible via static access, instead the
	 *             {@link #ResourceRuleFactory(IWorkspace)} constructor should be used.
	 */
	@Deprecated
	protected ResourceRuleFactory() {
		this(ResourcesPlugin.getWorkspace());
	}

	/**
	 * Creates a new resource rule factory for the given workspace. This constructor
	 * must only be called by subclasses.
	 *
	 * @since 3.17
	 */
	protected ResourceRuleFactory(IWorkspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#buildRule</code>.
	 * This default implementation always returns the workspace root.
	 * <p>
	 * Subclasses may not currently override this method.
	 *
	 * @see org.eclipse.core.resources.IResourceRuleFactory#buildRule()
	 */
	@Override
	public final ISchedulingRule buildRule() {
		return workspace.getRoot();
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#charsetRule</code>.
	 * This default implementation always returns the project of the resource
	 * whose charset setting is being changed, or <code>null</code> if the
	 * resource is the workspace root.
	 * <p>
	 * Subclasses may override this method. The rule provided by an overriding
	 * method must at least contain the rule from this default implementation.
	 * </p>
	 *
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#charsetRule(IResource)
	 * @since 3.1
	 */
	@Override
	public ISchedulingRule charsetRule(IResource resource) {
		if (resource.getType() == IResource.ROOT)
			return null;
		return resource.getProject();
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#derivedRule</code>.
	 * This default implementation always returns <code>null</code>.
	 * <p>
	 * Subclasses may not currently override this method.
	 *
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#derivedRule(IResource)
	 * @since 3.6
	 */
	@Override
	public final ISchedulingRule derivedRule(IResource resource) {
		return null;
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#copyRule</code>.
	 * This default implementation always returns the parent of the destination
	 * resource.
	 * <p>
	 * Subclasses may override this method. The rule provided by an overriding
	 * method must at least contain the rule from this default implementation.
	 *
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#copyRule(IResource, IResource)
	 */
	@Override
	public ISchedulingRule copyRule(IResource source, IResource destination) {
		//source is not modified, destination is created
		return parent(destination);
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#createRule</code>.
	 * This default implementation always returns the parent of the resource
	 * being created.
	 * <p>
	 * Subclasses may override this method. The rule provided by an overriding
	 * method must at least contain the rule from this default implementation.
	 *
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#createRule(IResource)
	 */
	@Override
	public ISchedulingRule createRule(IResource resource) {
		return parent(resource);
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#deleteRule</code>.
	 * This default implementation always returns the parent of the resource
	 * being deleted.
	 * <p>
	 * Subclasses may override this method. The rule provided by an overriding
	 * method must at least contain the rule from this default implementation.
	 *
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#deleteRule(IResource)
	 */
	@Override
	public ISchedulingRule deleteRule(IResource resource) {
		return parent(resource);
	}

	private boolean isReadOnly(IResource resource) {
		ResourceAttributes attributes = resource.getResourceAttributes();
		return attributes == null ? false : attributes.isReadOnly();
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#markerRule</code>.
	 * This default implementation always returns <code>null</code>.
	 * <p>
	 * Subclasses may not currently override this method.
	 *
	 * @see org.eclipse.core.resources.IResourceRuleFactory#markerRule(IResource)
	 */
	@Override
	public final ISchedulingRule markerRule(IResource resource) {
		return null;
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#modifyRule</code>.
	 * This default implementation returns the resource being modified, or the
	 * parent resource if modifying a project description file.
	 * Note that this must encompass any rule required by the <code>validateSave</code> hook.
	 * <p>
	 * Subclasses may override this method. The rule provided by an overriding
	 * method must at least contain the rule from this default implementation.
	 *
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#modifyRule(IResource)
	 * @see FileModificationValidator#validateSave(IFile)
	 * @see IProjectDescription#DESCRIPTION_FILE_NAME
	 */
	@Override
	public ISchedulingRule modifyRule(IResource resource) {
		IPath path = resource.getFullPath();
		//modifying the project description may cause linked resources to be created or deleted
		if (path.segmentCount() == 2 && path.segment(1).equals(IProjectDescription.DESCRIPTION_FILE_NAME))
			return parent(resource);
		return resource;
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#moveRule</code>.
	 * This default implementation returns a rule that combines the parent
	 * of the source resource and the parent of the destination resource.
	 * <p>
	 * Subclasses may override this method. The rule provided by an overriding
	 * method must at least contain the rule from this default implementation.
	 *
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#moveRule(IResource, IResource)
	 */
	@Override
	public ISchedulingRule moveRule(IResource source, IResource destination) {
		//move needs the parent of both source and destination
		return MultiRule.combine(parent(source), parent(destination));
	}

	/**
	 * Convenience method to return the parent of the given resource,
	 * or the resource itself for projects and the workspace root.
	 * @param resource the resource to compute the parent of
	 * @return the parent resource for folders and files, and the
	 * resource itself for projects and the workspace root.
	 */
	protected final ISchedulingRule parent(IResource resource) {
		switch (resource.getType()) {
			case IResource.ROOT :
			case IResource.PROJECT :
				return resource;
			default :
				return resource.getParent();
		}
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#refreshRule</code>.
	 * This default implementation always returns the parent of the resource
	 * being refreshed.
	 * <p>
	 * Subclasses may override this method. The rule provided by an overriding
	 * method must at least contain the rule from this default implementation.
	 *
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#refreshRule(IResource)
	 */
	@Override
	public ISchedulingRule refreshRule(IResource resource) {
		return parent(resource);
	}

	/**
	 * Default implementation of <code>IResourceRuleFactory#validateEditRule</code>.
	 * This default implementation returns a rule that combines the parents of
	 * all read-only resources, or <code>null</code> if there are no read-only
	 * resources.
	 * <p>
	 * Subclasses may override this method. The rule provided by an overriding
	 * method must at least contain the rule from this default implementation.
	 *
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#validateEditRule(IResource[])
	 */
	@Override
	public ISchedulingRule validateEditRule(IResource[] resources) {
		if (resources.length == 0)
			return null;
		//optimize rule for single file
		if (resources.length == 1)
			return isReadOnly(resources[0]) ? parent(resources[0]) : null;
		//need a lock on the parents of all read-only files
		HashSet<ISchedulingRule> rules = new HashSet<>();
		for (IResource resource : resources)
			if (isReadOnly(resource))
				rules.add(parent(resource));
		if (rules.isEmpty())
			return null;
		if (rules.size() == 1)
			return rules.iterator().next();
		ISchedulingRule[] ruleArray = rules.toArray(new ISchedulingRule[rules.size()]);
		return new MultiRule(ruleArray);
	}
}
