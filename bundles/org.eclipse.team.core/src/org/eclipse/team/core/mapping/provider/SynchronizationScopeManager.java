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
package org.eclipse.team.core.mapping.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.mapping.CompoundResourceTraversal;
import org.eclipse.team.internal.core.mapping.ResourceMappingScope;
import org.eclipse.team.internal.core.mapping.ScopeChangeEvent;
import org.eclipse.team.internal.core.mapping.ScopeManagerEventHandler;

/**
 * Class for translating a set of <code>ResourceMapping</code> objects
 * representing a view selection into the complete set of resources to be
 * operated on.
 * <p>
 * Here's a summary of the scope generation algorithm:
 * <ol>
 * <li>Obtain selected mappings
 * <li>Project mappings onto resources using the appropriate context(s) in
 * order to obtain a set of ResourceTraverals
 * <li>Determine what model providers are interested in the targeted resources
 * <li>From those model providers, obtain the set of affected resource mappings
 * <li>If the original set is the same as the new set, we are done.
 * <li>If the set differs from the original selection, rerun the mapping
 * process for any new mappings
 * <ul>
 * <li>Only need to query model providers for mappings for new resources
 * <li>Keep repeating until no new mappings or resources are added
 * </ul>
 * </ol>
 * <p>
 * This implementation does not involve participants in the scope management
 * process. It is up to subclasses that wish to support a longer life cycle for
 * scopes to provide for participation. For example, the
 * {@link SubscriberScopeManager} class includes participates in the scope
 * management process.
 *
 * @see org.eclipse.core.resources.mapping.ResourceMapping
 * @see SubscriberScopeManager
 *
 * @since 3.2
 */
public class SynchronizationScopeManager extends PlatformObject implements ISynchronizationScopeManager {

	private static final int MAX_ITERATION = 10;
	private final ResourceMappingContext context;
	private final boolean consultModels;
	private ISynchronizationScope scope;
	private boolean initialized;
	private ScopeManagerEventHandler handler;
	private final String name;

	/**
	 * Convenience method for obtaining the set of resource
	 * mappings from all model providers that overlap
	 * with the given resources.
	 * @param traversals the resource traversals
	 * @param context the resource mapping context
	 * @param monitor a progress monitor
	 * @return the resource mappings
	 * @throws CoreException if an error occurs
	 */
	public static ResourceMapping[] getMappingsFromProviders(ResourceTraversal[] traversals,
			ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		Set<ResourceMapping> result = new HashSet<>();
		IModelProviderDescriptor[] descriptors = ModelProvider
				.getModelProviderDescriptors();
		for (IModelProviderDescriptor descriptor : descriptors) {
			ResourceMapping[] mappings = getMappings(descriptor, traversals,
					context, monitor);
			result.addAll(Arrays.asList(mappings));
			Policy.checkCanceled(monitor);
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	private static ResourceMapping[] getMappings(IModelProviderDescriptor descriptor,
			ResourceTraversal[] traversals,
			ResourceMappingContext context, IProgressMonitor monitor)
			throws CoreException {
		ResourceTraversal[] matchingTraversals = descriptor.getMatchingTraversals(
				traversals);
		return descriptor.getModelProvider().getMappings(matchingTraversals,
				context, monitor);
	}

	/**
	 * Create a scope manager that uses the given context to
	 * determine what resources should be included in the scope.
	 * If <code>consultModels</code> is <code>true</code> then
	 * the model providers will be queried in order to determine if
	 * additional mappings should be included in the scope
	 * @param name the name of the scope
	 * @param inputMappings the input mappings
	 * @param resourceMappingContext a resource mapping context
	 * @param consultModels whether model providers should be consulted
	 */
	public SynchronizationScopeManager(String name, ResourceMapping[] inputMappings, ResourceMappingContext resourceMappingContext, boolean consultModels) {
		this.name = name;
		this.context = resourceMappingContext;
		this.consultModels = consultModels;
		scope = createScope(inputMappings);
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Return the scheduling rule that is used when initializing and refreshing
	 * the scope. By default, a rule that covers all projects for the input mappings
	 * of the scope is returned. Subclasses may override.
	 *
	 * @return the scheduling rule that is used when initializing and refreshing
	 *         the scope
	 */
	public ISchedulingRule getSchedulingRule() {
		Set<IProject> projects = new HashSet<>();
		ResourceMapping[] mappings = scope.getInputMappings();
		for (ResourceMapping mapping : mappings) {
			Object modelObject = mapping.getModelObject();
			if (modelObject instanceof IResource) {
				IResource resource = (IResource) modelObject;
				if (resource.getType() == IResource.ROOT)
					// If the workspace root is one of the inputs,
					// then use the workspace root as the rule
					return ResourcesPlugin.getWorkspace().getRoot();
				projects.add(resource.getProject());
			} else {
				// If one of the inputs is not a resource, then use the
				// root as the rule since we don't know whether projects
				// can be added or removed
				return ResourcesPlugin.getWorkspace().getRoot();
			}
		}
		return MultiRule.combine(projects.toArray(new IProject[projects.size()]));
	}

	@Override
	public void initialize(
			IProgressMonitor monitor) throws CoreException {
		ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) this::internalPrepareContext,
				getSchedulingRule(), IResource.NONE, monitor);
	}

	@Override
	public ResourceTraversal[] refresh(final ResourceMapping[] mappings, IProgressMonitor monitor) throws CoreException {
		// We need to lock the workspace when building the scope
		final ResourceTraversal[][] traversals = new ResourceTraversal[][] { new ResourceTraversal[0] };
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.run((IWorkspaceRunnable) monitor1 -> traversals[0] = internalRefreshScope(mappings, true, monitor1),
				getSchedulingRule(), IResource.NONE, monitor);
		return traversals[0];
	}

	private void internalPrepareContext(IProgressMonitor monitor) throws CoreException {
		if (initialized)
			return;
		monitor.beginTask(null, IProgressMonitor.UNKNOWN);
		// Accumulate the initial set of mappings we need traversals for
		((ResourceMappingScope)scope).reset();
		ResourceMapping[] targetMappings = scope.getInputMappings();
		ResourceTraversal[] newTraversals;
		boolean firstTime = true;
		boolean hasAdditionalResources = false;
		int count = 0;
		do {
			Policy.checkCanceled(monitor);
			newTraversals = addMappingsToScope(targetMappings,
					Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
			if (newTraversals.length > 0 && consultModels) {
				ResourceTraversal[] adjusted = adjustInputTraversals(newTraversals);
				targetMappings = getMappingsFromProviders(adjusted,
						context,
						Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
				if (firstTime) {
					firstTime = false;
				} else if (!hasAdditionalResources) {
					hasAdditionalResources = newTraversals.length != 0;
				}
			}
		} while (consultModels & newTraversals.length != 0 && count++ < MAX_ITERATION);
		setHasAdditionalMappings(scope, consultModels && internalHasAdditionalMappings());
		setHasAdditionalResources(consultModels && hasAdditionalResources);
		monitor.done();
		initialized = true;
		fireMappingsChangedEvent(scope.getMappings(), scope.getTraversals());
	}

	private ResourceTraversal[] internalRefreshScope(ResourceMapping[] mappings, boolean checkForContraction, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(null, 100 * mappings.length + 100);
		ScopeChangeEvent change = new ScopeChangeEvent(scope);
		CompoundResourceTraversal refreshTraversals = new CompoundResourceTraversal();
		CompoundResourceTraversal removedTraversals = new CompoundResourceTraversal();
		for (ResourceMapping mapping : mappings) {
			ResourceTraversal[] previousTraversals = scope.getTraversals(mapping);
			ResourceTraversal[] mappingTraversals = mapping.getTraversals(
					context, Policy.subMonitorFor(monitor, 100));
			refreshTraversals.addTraversals(mappingTraversals);
			ResourceTraversal[] uncovered = getUncoveredTraversals(mappingTraversals);
			if (checkForContraction && previousTraversals != null && previousTraversals.length > 0) {
				ResourceTraversal[] removed = getUncoveredTraversals(mappingTraversals, previousTraversals);
				removedTraversals.addTraversals(removed);
			}
			if (uncovered.length > 0) {
				change.setExpanded(true);
				ResourceTraversal[] result = performExpandScope(mapping, mappingTraversals, uncovered, monitor);
				refreshTraversals.addTraversals(result);
			}
		}

		if (checkForContraction && removedTraversals.getRoots().length > 0) {
			// The scope may have contracted. The only way to handle this is to recalculate from scratch
			// TODO: This may not be thread safe
			((ResourceMappingScope)scope).reset();
			internalRefreshScope(scope.getInputMappings(), false, monitor);
			change.setContracted(true);
		}

		if (change.shouldFireChange())
			fireMappingsChangedEvent(change.getChangedMappings(), change.getChangedTraversals(refreshTraversals));
		monitor.done();
		return refreshTraversals.asTraversals();
	}

	private ResourceTraversal[] getUncoveredTraversals(
			ResourceTraversal[] newTraversals,
			ResourceTraversal[] previousTraversals) {
		CompoundResourceTraversal t = new CompoundResourceTraversal();
		t.addTraversals(newTraversals);
		return t.getUncoveredTraversals(previousTraversals);
	}

	private ResourceTraversal[] performExpandScope(
			ResourceMapping mapping, ResourceTraversal[] mappingTraversals,
			ResourceTraversal[] uncovered, IProgressMonitor monitor)
			throws CoreException {
		ResourceMapping ancestor = findAncestor(mapping);
		if (ancestor == null) {
			uncovered = addMappingToScope(mapping, mappingTraversals);
			addResourcesToScope(uncovered, monitor);
			return mappingTraversals;
		} else {
			ResourceTraversal[] ancestorTraversals = ancestor.getTraversals(
					context, Policy.subMonitorFor(monitor, 100));
			uncovered = addMappingToScope(ancestor, ancestorTraversals);
			addResourcesToScope(uncovered, monitor);
			return ancestorTraversals;
		}
	}

	private ResourceMapping findAncestor(ResourceMapping mapping) {
		ResourceMapping[] mappings = scope.getMappings(mapping.getModelProviderId());
		for (ResourceMapping m : mappings) {
			if (m.contains(mapping)) {
				return m;
			}
		}
		return null;
	}

	private ResourceTraversal[] getUncoveredTraversals(ResourceTraversal[] traversals) {
		return ((ResourceMappingScope)scope).getCompoundTraversal().getUncoveredTraversals(traversals);
	}

	private void addResourcesToScope(ResourceTraversal[] newTraversals, IProgressMonitor monitor) throws CoreException {
		if (!consultModels)
			return;
		ResourceMapping[] targetMappings;
		int count = 0;
		do {
			ResourceTraversal[] adjusted = adjustInputTraversals(newTraversals);
			targetMappings = getMappingsFromProviders(adjusted,
					context, Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
			newTraversals = addMappingsToScope(targetMappings,
					Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
		} while (newTraversals.length != 0 && count++ < MAX_ITERATION);
		if (!scope.hasAdditionalMappings()) {
			setHasAdditionalMappings(scope, internalHasAdditionalMappings());
		}
		if (!scope.hasAdditonalResources()) {
			setHasAdditionalResources(true);
		}
	}

	/*
	 * Fire a mappings changed event to any listeners on the scope.
	 * The new mappings are obtained from the scope.
	 * @param originalMappings the original mappings of the scope.
	 */
	private void fireMappingsChangedEvent(ResourceMapping[] newMappings, ResourceTraversal[] newTraversals) {
		((ResourceMappingScope)scope).fireTraversalsChangedEvent(newTraversals, newMappings);
	}

	/**
	 * Set whether the scope has additional mappings. This method is not
	 * intended to be overridden.
	 *
	 * @param hasAdditionalMappings a boolean indicating if the scope has
	 *            additional mappings
	 */
	protected final void setHasAdditionalMappings(
			ISynchronizationScope scope, boolean hasAdditionalMappings) {
		((ResourceMappingScope)scope).setHasAdditionalMappings(hasAdditionalMappings);
	}

	/**
	 * Set whether the scope has additional resources. This method is not
	 * intended to be overridden.
	 *
	 * @param hasAdditionalResources a boolean indicating if the scope has
	 *            additional resources
	 */
	protected final void setHasAdditionalResources(boolean hasAdditionalResources) {
		((ResourceMappingScope)scope).setHasAdditionalResources(hasAdditionalResources);
	}

	/**
	 * Create the scope that will be populated and returned by the builder. This
	 * method is not intended to be overridden by clients.
	 * @param inputMappings the input mappings
	 * @return a newly created scope that will be populated and returned by the
	 *         builder
	 */
	protected final ISynchronizationScope createScope(
			ResourceMapping[] inputMappings) {
		return new ResourceMappingScope(inputMappings, this);
	}

	/**
	 * Adjust the given set of input resources to include any additional
	 * resources required by a particular repository provider for the current
	 * operation. By default the original set is returned but subclasses may
	 * override. Overriding methods should return a set of resources that
	 * include the original resource either explicitly or implicitly as a child
	 * of a returned resource.
	 * <p>
	 * Subclasses may override this method to include additional resources.
	 *
	 * @param traversals the input resource traversals
	 * @return the input resource traversals adjusted to include any additional resources
	 *         required for the current operation
	 */
	protected ResourceTraversal[] adjustInputTraversals(ResourceTraversal[] traversals) {
		return traversals;
	}

	private ResourceTraversal[] addMappingsToScope(
			ResourceMapping[] targetMappings,
			IProgressMonitor monitor) throws CoreException {
		CompoundResourceTraversal result = new CompoundResourceTraversal();
		ResourceMappingContext context = this.context;
		for (ResourceMapping mapping : targetMappings) {
			if (scope.getTraversals(mapping) == null) {
				ResourceTraversal[] traversals = mapping.getTraversals(context,
						Policy.subMonitorFor(monitor, 100));
				ResourceTraversal[] newOnes = addMappingToScope(mapping, traversals);
				result.addTraversals(newOnes);
			}
			Policy.checkCanceled(monitor);
		}
		return result.asTraversals();
	}

	/**
	 * Add the mapping and its calculated traversals to the scope. Return the
	 * resources that were not previously covered by the scope. This method
	 * is not intended to be subclassed by clients.
	 *
	 * @param mapping the resource mapping
	 * @param traversals the resource mapping's traversals
	 * @return the resource traversals that were not previously covered by the scope
	 */
	protected final ResourceTraversal[] addMappingToScope(
			ResourceMapping mapping, ResourceTraversal[] traversals) {
		return ((ResourceMappingScope)scope).addMapping(mapping, traversals);
	}

	private boolean internalHasAdditionalMappings() {
		ResourceMapping[] inputMappings = scope.getInputMappings();
		ResourceMapping[] mappings = scope.getMappings();
		if (inputMappings.length == mappings.length) {
			Set<ResourceMapping> testSet = new HashSet<>();
			Collections.addAll(testSet, mappings);
			for (ResourceMapping mapping : inputMappings) {
				if (!testSet.contains(mapping)) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	public ResourceMappingContext getContext() {
		return context;
	}

	@Override
	public ISynchronizationScope getScope() {
		return scope;
	}

	@Override
	public void dispose() {
		if (handler != null)
			handler.shutdown();
	}

	/**
	 * Refresh the given mappings by recalculating the traversals for the
	 * mappings and adjusting the scope accordingly.
	 * @param mappings the mappings to be refreshed
	 */
	public void refresh(ResourceMapping[] mappings) {
		getHandler().refresh(mappings);
	}

	private synchronized ScopeManagerEventHandler getHandler() {
		if (handler == null)
			handler = new ScopeManagerEventHandler(this);
		return handler;
	}

	/**
	 * Returns the human readable name of this manager.  The name is never
	 * <code>null</code>.
	 * @return the name associated with this scope manager
	 */
	public String getName() {
		return name;
	}
}
