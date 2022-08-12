/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.resources.mapping;

import java.util.*;
import org.eclipse.core.internal.resources.mapping.ModelProviderManager;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Represents the provider of a logical model. The main purpose of this
 * API is to support batch operations on sets of <code>ResourceMapping</code>
 * objects that are part of the same model.
 *
 * <p>
 * This class may be subclassed by clients.
 * </p>
 * @see org.eclipse.core.resources.mapping.ResourceMapping
 * @since 3.2
 */
public abstract class ModelProvider extends PlatformObject {
	/**
	 * The model provider id of the Resources model.
	 */
	public static final String RESOURCE_MODEL_PROVIDER_ID = "org.eclipse.core.resources.modelProvider"; //$NON-NLS-1$

	private IModelProviderDescriptor descriptor;

	/**
	 * Returns the descriptor for the model provider of the given id
	 * or <code>null</code> if the provider has not been registered.
	 * @param id a model provider id.
	 * @return the descriptor for the model provider of the given id
	 * or <code>null</code> if the provider has not been registered
	 */
	public static IModelProviderDescriptor getModelProviderDescriptor(String id) {
		IModelProviderDescriptor[] descs = ModelProviderManager.getDefault().getDescriptors();
		for (IModelProviderDescriptor descriptor : descs) {
			if (descriptor.getId().equals(id)) {
				return descriptor;
			}
		}
		return null;
	}

	/**
	 * Returns the descriptors for all model providers that are registered.
	 *
	 * @return the descriptors for all model providers that are registered.
	 */
	public static IModelProviderDescriptor[] getModelProviderDescriptors() {
		return ModelProviderManager.getDefault().getDescriptors();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ModelProvider) {
			ModelProvider other = (ModelProvider) obj;
			return other.getDescriptor().getId().equals(getDescriptor().getId());
		}
		return super.equals(obj);
	}

	/**
	 * Returns the descriptor of this model provider. The descriptor
	 * is set during initialization so implements cannot call this method
	 * until after the <code>initialize</code> method is invoked.
	 * @return the descriptor of this model provider
	 */
	public final IModelProviderDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Returns the unique identifier of this model provider.
	 * <p>
	 * The model provider identifier is composed of the model provider's
	 * plug-in id and the simple id of the provider extension. For example, if
	 * plug-in <code>"com.xyz"</code> defines a provider extension with id
	 * <code>"myModelProvider"</code>, the unique model provider identifier will be
	 * <code>"com.xyz.myModelProvider"</code>.
	 * </p>
	 *
	 * @return the unique model provider identifier
	 */
	public final String getId() {
		return descriptor.getId();
	}

	/**
	 * Returns the resource mappings that cover the given resource.
	 * By default, an empty array is returned. Subclass may override
	 * this method but should consider overriding either
	 * {@link #getMappings(IResource[], ResourceMappingContext, IProgressMonitor)}
	 * or {@link #getMappings(ResourceTraversal[], ResourceMappingContext, IProgressMonitor)}
	 * if more context is needed to determine the proper mappings.
	 *
	 * @param resource the resource
	 * @param context a resource mapping context
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *     reporting is not desired
	 * @return the resource mappings that cover the given resource.
	 * @exception CoreException in case of error; depends on actual implementation
	 */
	public ResourceMapping[] getMappings(IResource resource, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		return new ResourceMapping[0];
	}

	/**
	 * Returns the set of mappings that cover the given resources.
	 * This method is used to map operations on resources to
	 * operations on resource mappings. By default, this method
	 * calls <code>getMapping(IResource)</code> for each resource.
	 * <p>
	 * Subclasses may override this method.
	 * </p>
	 *
	 * @param resources the resources
	 * @param context a resource mapping context
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *     reporting is not desired
	 * @return the set of mappings that cover the given resources
	 * @exception CoreException in case of error; depends on actual implementation
	 */
	public ResourceMapping[] getMappings(IResource[] resources, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		Set<ResourceMapping> mappings = new HashSet<>();
		for (IResource resource : resources) {
			ResourceMapping[] resourceMappings = getMappings(resource, context, monitor);
			if (resourceMappings.length > 0)
				mappings.addAll(Arrays.asList(resourceMappings));
		}
		return mappings.toArray(new ResourceMapping[mappings.size()]);
	}

	/**
	 * Returns the set of mappings that overlap with the given resource traversals.
	 * This method is used to map operations on resources to
	 * operations on resource mappings. By default, this method
	 * calls {@link #getMappings(IResource[], ResourceMappingContext, IProgressMonitor)}
	 * with the resources extracted from each traversal.
	 * <p>
	 * Subclasses may override this method.
	 * </p>
	 *
	 * @param traversals the traversals
	 * @param context a resource mapping context
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *     reporting is not desired
	 * @return the set of mappings that overlap with the given resource traversals
	 */
	public ResourceMapping[] getMappings(ResourceTraversal[] traversals, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		Set<ResourceMapping> result = new HashSet<>();
		for (ResourceTraversal traversal : traversals) {
			ResourceMapping[] mappings = getMappings(traversal.getResources(), context, monitor);
			result.addAll(Arrays.asList(mappings));
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	/**
	 * Returns a set of traversals that cover the given resource mappings. The
	 * provided mappings must be from this provider or one of the providers this
	 * provider extends.
	 * <p>
	 * The default implementation accumulates the traversals from the given
	 * mappings. Subclasses can override to provide a more optimal
	 * transformation.
	 * </p>
	 *
	 * @param mappings the mappings being mapped to resources
	 * @param context the context used to determine the set of traversals that
	 *     cover the mappings
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *     reporting is not desired
	 * @return a set of traversals that cover the given mappings
	 * @exception CoreException in case of error; depends on actual implementation
	 */
	public ResourceTraversal[] getTraversals(ResourceMapping[] mappings, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, mappings.length);
		List<ResourceTraversal> traversals = new ArrayList<>();
		for (ResourceMapping mapping : mappings) {
			Collections.addAll(traversals, mapping.getTraversals(context, subMonitor.newChild(1)));
		}
		return traversals.toArray(new ResourceTraversal[traversals.size()]);
	}

	@Override
	public int hashCode() {
		return getDescriptor().getId().hashCode();
	}

	/**
	 * This method is called by the model provider framework when the model
	 * provider is instantiated. This method should not be called by clients and
	 * cannot be overridden by subclasses. However, it invokes the
	 * <code>initialize</code> method once the descriptor is set so subclasses
	 * can override that method if they need to do additional initialization.
	 *
	 * @param desc the description of the provider as it appears in the plugin manifest
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public final void init(IModelProviderDescriptor desc) {
		if (descriptor != null) {
			// prevent subsequent calls from damaging this instance
			return;
		}
		descriptor = desc;
		initialize();
	}

	/**
	 * Initialization method that is called after the descriptor
	 * of this provider is set. Subclasses may override.
	 */
	protected void initialize() {
		// Do nothing
	}

	/**
	 * Validates the proposed changes contained in the given delta.
	 * <p>
	 * This method must return either a {@link ModelStatus}, or a {@link MultiStatus}
	 * whose children are {@link ModelStatus}. The severity of the returned status
	 * indicates the severity of the possible side-effects of the operation.  Any
	 * severity other than <code>OK</code> will be shown to the user. The
	 * message should be a human readable message that will allow the user to
	 * make a decision on whether to continue with the operation. The model
	 * provider id should indicate which model is flagging the possible side effects.
	 * <p>
	 * This default implementation accepts all changes and returns a status with
	 * severity <code>OK</code>. Subclasses should override to perform
	 * validation specific to their model.
	 * </p>
	 *
	 * @param delta a delta tree containing the proposed changes
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *     reporting is not desired
	 * @return a status indicating any potential side effects
	 *     on the model that provided this validator.
	 */
	public IStatus validateChange(IResourceDelta delta, IProgressMonitor monitor) {
		return new ModelStatus(IStatus.OK, ResourcesPlugin.PI_RESOURCES, descriptor.getId(), Status.OK_STATUS.getMessage());
	}
}
