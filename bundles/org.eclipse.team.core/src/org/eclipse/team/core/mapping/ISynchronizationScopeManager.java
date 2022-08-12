/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;

/**
 * A scope manager is responsible for ensuring that the resources contained
 * within an {@link ISynchronizationScope} stay up-to-date with the model
 * elements (represented as {@link ResourceMapping} instances) contained in the
 * scope. The task of keeping a scope up-to-date is accomplished by obtaining
 * {@link ISynchronizationScopeParticipant} instances for each model that has
 * elements contained in the scope.
 *
 * @see org.eclipse.core.resources.mapping.ResourceMapping
 * @see SynchronizationScopeManager
 * @see ISynchronizationScopeParticipant
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients can instead subclass {@link SynchronizationScopeManager}
 */
public interface ISynchronizationScopeManager {

	/**
	 * Return the scope that is managed by this manager.
	 * @return the scope that is managed by this manager
	 */
	ISynchronizationScope getScope();

	/**
	 * Return whether the scope has been initialized.
	 * @return whether the scope has been initialized.
	 */
	boolean isInitialized();

	/**
	 * Build the scope that is used to determine the complete set of resource
	 * mappings, and hence resources, that an operation should be performed on.
	 * <p>
	 * This method obtains a lock on the workspace root to avoid workspace
	 * changes while calculating the scope.
	 * @param monitor a progress monitor
	 * when building the scope
	 *
	 * @throws CoreException if an error occurs
	 */
	void initialize(IProgressMonitor monitor) throws CoreException;

	/**
	 * Refresh the scope of this manager for the given mappings.
	 * Changes in the scope will be reported as a property change
	 * event fired from the scope. Clients should call this method
	 * when a change in the workspace or a change issued from this
	 * manager have resulted in a change in the resources that
	 * should be included in the scope.
	 * @param mappings the mappings to be refreshed
	 * @param monitor a progress monitor
	 * @return a set of traversals that cover the given mappings
	 * @throws CoreException if an error occurs
	 */
	ResourceTraversal[] refresh(ResourceMapping[] mappings, IProgressMonitor monitor) throws CoreException;

	/**
	 * Method to be invoked when the scope of this
	 * manager is no longer needed. It is typically the
	 * responsibility of the client that creates a scope manager
	 * to dispose it.
	 */
	void dispose();

}
