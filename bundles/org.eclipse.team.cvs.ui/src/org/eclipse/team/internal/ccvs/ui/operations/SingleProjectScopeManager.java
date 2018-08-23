/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;

public final class SingleProjectScopeManager extends SynchronizationScopeManager {
	private final IProject project;
	public SingleProjectScopeManager(String name, ResourceMapping[] mappings, ResourceMappingContext context, boolean models, IProject project) {
		super(name, mappings, context, models);
		this.project = project;
	}
	public ISchedulingRule getSchedulingRule() {
		return project;
	}
}