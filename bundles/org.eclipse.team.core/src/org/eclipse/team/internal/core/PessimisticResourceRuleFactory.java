/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * A pessimistic rule factory used to ensure older repository providers
 * are not broken by new scheduling rule locking. The workspace root
 * is returned for all rules.
 */
public class PessimisticResourceRuleFactory extends ResourceRuleFactory {

	IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#copyRule(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IResource)
	 */
	public ISchedulingRule copyRule(IResource source, IResource destination) {
		return root;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#createRule(org.eclipse.core.resources.IResource)
	 */
	public ISchedulingRule createRule(IResource resource) {
		return root;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#deleteRule(org.eclipse.core.resources.IResource)
	 */
	public ISchedulingRule deleteRule(IResource resource) {
		return root;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#modifyRule(org.eclipse.core.resources.IResource)
	 */
	public ISchedulingRule modifyRule(IResource resource) {
		return root;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#moveRule(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IResource)
	 */
	public ISchedulingRule moveRule(IResource source, IResource destination) {
		return root;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#refreshRule(org.eclipse.core.resources.IResource)
	 */
	public ISchedulingRule refreshRule(IResource resource) {
		return root;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceRuleFactory#validateEditRule(org.eclipse.core.resources.IResource[])
	 */
	public ISchedulingRule validateEditRule(IResource[] resources) {
		return root;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.team.ResourceRuleFactory#charsetRule(org.eclipse.core.resources.IResource)
	 */
	public ISchedulingRule charsetRule(IResource resource) {
		return root;
	}
}
