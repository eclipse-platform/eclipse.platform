/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

/**
 * Interface which defines the protocol for translating a set of
 * <code>ResourceMapping</code> objects representing a view selection into the
 * complete set of resources to be operated on.
 *
 * @see org.eclipse.core.resources.mapping.ResourceMapping
 * @see ISynchronizationScopeManager
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 *              Instead, clients should use a
 *              {@link ISynchronizationScopeManager} to generate a resource
 *              mapping scope from a set of input resource mappings.
 */
public interface ISynchronizationScope {

	/**
	 * Return the projects that bound this scope.
	 * The projects returned will depend on the type of context used
	 * to generate this scope. If the context is a local context,
	 * all workspace projects are returned. If it is a remote context,
	 * the projects are the same as those returned from
	 * {@link RemoteResourceMappingContext#getProjects()}
	 * @return the projects that bound this scope
	 */
	IProject[] getProjects();

	/**
	 * Return the resource mapping context that the scope
	 * uses to obtain traversals from resource mappings
	 * in order to determine what resources are in the scope.
	 *
	 * @see ResourceMapping#getTraversals(ResourceMappingContext, org.eclipse.core.runtime.IProgressMonitor)
	 *
	 * @return the resource mapping context that the scope
	 * uses to obtain traversals from resource mappings
	 */
	ResourceMappingContext getContext();

	/**
	 * Return the array of mappings that acted as the input to the scope builder
	 * that was used to build this scope. This set usually come from a view
	 * selection but could come from another source. In most cases, clients will
	 * want to call the {@link #getMappings()} method instead of this one as it
	 * returns the complete set of mappings to be operated on.
	 *
	 * @return the set of mappings that acted as the input to the scope builder
	 *         that was used to build this scope
	 */
	public ResourceMapping[] getInputMappings();

	/**
	 * Return a scope that only contains the input mappings of this
	 * scope.
	 * @return a scope that only contains the input mappings of this
	 * scope
	 */
	public ISynchronizationScope asInputScope();

	/**
	 * Return an array of all of the mappings to be operated on. The returned
	 * mappings were included in the operation during the scope building
	 * process. The returned mappings may be the same as the input mappings but
	 * may also be a super set. Clients can call the
	 * {@link #hasAdditionalMappings()} method to determine if the two sets are
	 * the same or not.
	 *
	 * @return an array of all of the mappings to be operated on.
	 */
	public ResourceMapping[] getMappings();

	/**
	 * Return an array of traversals that cover the resource mappings to be
	 * operated on as returned by the {@link #getMappings()} method. The
	 * traversals were calculated during the scope building process and cached
	 * with the scope.
	 *
	 * @return the complete set of mappings to be operated on
	 */
	public ResourceTraversal[] getTraversals();

	/**
	 * Return the resources that are the roots of this scope.
	 * The roots are determined by collecting the roots of
	 * the traversals that define this scope.
	 * @return the resources that are the roots of this scope
	 */
	public IResource[] getRoots();

	/**
	 * Return whether the given resource is contained in this scope.
	 * A resource is contained by the scope if it is contained in at
	 * least one of the traversals that define this scope.
	 * @param resource the resource to be tested
	 * @return whether the given resource is contained in this scope
	 */
	public boolean contains(IResource resource);

	/**
	 * Add a scope change listener that will get invoked when a
	 * property of the receiver changes. Adding a listener that is
	 * already added has no effect.
	 *
	 * @param listener the listener to be added
	 */
	public void addScopeChangeListener(ISynchronizationScopeChangeListener listener);

	/**
	 * Remove a scope change listener. Removing an unregistered listener
	 * has no effect.
	 *
	 * @param listener the listener to be removed
	 */
	public void removeScopeChangeListener(ISynchronizationScopeChangeListener listener);

	/**
	 * Return an array of traversals that cover the given resource mapping to be
	 * operated on. The traversals were calculated during the scope building
	 * process and cached with the scope.
	 *
	 * @param mapping a resource mapping being operated on
	 * @return the traversals that cover the given resource mapping (or
	 *         <code>null</code> if the mapping is not contained in the input)
	 */
	public ResourceTraversal[] getTraversals(ResourceMapping mapping);

	/**
	 * Return whether the scope has additional mappings added to the input
	 * mappings during the scope building process.
	 *
	 * @return whether the input has additional mappings added to the seed
	 *         mappings
	 */
	public boolean hasAdditionalMappings();

	/**
	 * Return whether the scope has additional resources added due to additional
	 * resource mappings.
	 *
	 * @return whether the input has additional resources added due to
	 *         additional resource mappings
	 */
	public boolean hasAdditonalResources();

	/**
	 * Return all the model providers that have mappings in this scope.
	 *
	 * @return all the model providers that have mappings in this scope
	 */
	public ModelProvider[] getModelProviders();

	/**
	 * Return all the mappings to be operated on for the given model provider
	 * id.
	 *
	 * @param modelProviderId the id of the model provider
	 * @return all the mappings for the given model provider id
	 */
	public ResourceMapping[] getMappings(String modelProviderId);

	/**
	 * Return the set of traversals that cover the mappings for
	 * the given model provider.
	 * @param modelProviderId the model provider id
	 * @return the set of traversals that cover the mappings for
	 * the given model provider
	 */
	public ResourceTraversal[] getTraversals(String modelProviderId);

	/**
	 * Return the resource mapping in the scope associated with the given model
	 * object or <code>null</code> if there isn't one. This method has no knowledge
	 * of hierarchical models so it only matches directly against the mappings
	 * that are contained in the scope.
	 * @param modelObject the model object
	 * @return the mapping for the model object that is contained in this scope
	 */
	public ResourceMapping getMapping(Object modelObject);

	/**
	 * Refresh the given mapping asynchronously. This method
	 * is called by {@link ISynchronizationScopeParticipant}
	 * instances when they detect changes that require the scope
	 * to be adjusted.
	 * @param mappings the mappings to be refreshed.
	 */
	void refresh(ResourceMapping[] mappings);

}
