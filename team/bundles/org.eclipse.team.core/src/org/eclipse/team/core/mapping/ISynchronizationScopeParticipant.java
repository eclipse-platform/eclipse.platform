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
package org.eclipse.team.core.mapping;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;

/**
 * A scope participant is responsible for ensuring that the resources contained
 * within an {@link ISynchronizationScope} that overlap with the participant's
 * model provider stay up-to-date with the model elements (represented as
 * {@link ResourceMapping} instances) contained in the scope.
 *
 * <p>
 * Clients may implement this interface.
 *
 * @see org.eclipse.core.resources.mapping.ResourceMapping
 * @see ISynchronizationScopeManager
 *
 * @since 3.2
 */
public interface ISynchronizationScopeParticipant {

	/**
	 * Callback that the manager makes to participants when the state of
	 * resources that are contained in the resource mapping context of the
	 * manager change. This method will only be invoked when the context of the
	 * manager is a {@link RemoteResourceMappingContext} and the state of one or
	 * more resources changes w.r.t. the context. It is the responsibility of the
	 * participant to react to local changes that affect the resources in the
	 * scope by calling
	 * {@link ISynchronizationScope#refresh(ResourceMapping[])}.
	 *
	 * @param scope
	 *            the scope
	 * @param resources
	 *            the changed resources
	 * @param projects
	 *            projects that were either added or removed
	 * @return the resource mappings that need to be refreshed.
	 */
	ResourceMapping[] handleContextChange(
			ISynchronizationScope scope, IResource[] resources, IProject[] projects);

	/**
	 * Callback from the scope manager when the scope is no longer needed.
	 * This si done to give participants a chance to remove a
	 * registered {@link IResourceChangeListener} or any other listeners.
	 */
	void dispose();

}
