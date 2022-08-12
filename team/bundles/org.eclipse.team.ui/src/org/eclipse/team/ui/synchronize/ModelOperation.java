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
package org.eclipse.team.ui.synchronize;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.mapping.CompoundResourceTraversal;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.dialogs.AdditionalMappingsDialog;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * An abstract operation that uses an {@link ISynchronizationScopeManager} to
 * create an operation scope that includes the complete set of mappings that
 * must be included in the operation to ensure model consistency. The scope
 * generation phase will prompt the user if additional resources have been added
 * to the scope.
 *
 * @since 3.2
 */
public abstract class ModelOperation extends TeamOperation {

	private boolean previewRequested;
	private ISynchronizationScopeManager manager;

	/**
	 * Return the list of provides sorted by their extends relationship.
	 * Extended model providers will appear later in the list then those
	 * that extends them. The order of model providers that independant
	 * (i.e. no extends relationship between them) will be indeterminate.
	 * @param providers the model providers
	 * @return the list of provides sorted by their extends relationship
	 */
	public static ModelProvider[] sortByExtension(ModelProvider[] providers) {
		List<ModelProvider> result = new ArrayList<>();
		for (ModelProvider providerToInsert : providers) {
			int index = result.size();
			for (int j = 0; j < result.size(); j++) {
				ModelProvider provider = result.get(j);
				if (extendsProvider(providerToInsert, provider)) {
					index = j;
					break;
				}
			}
			result.add(index, providerToInsert);
		}
		return result.toArray(new ModelProvider[result.size()]);
	}

	private static boolean extendsProvider(ModelProvider providerToInsert, ModelProvider provider) {
		String[] extended = providerToInsert.getDescriptor().getExtendedModels();
		// First search immediate dependents
		for (String id : extended) {
			if (id.equals(provider.getDescriptor().getId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create a model operation that operates on the given scope.
	 * @param part the workbench part from which the merge was launched or <code>null</code>
	 * @param manager the scope manager for this operation
	 */
	protected ModelOperation(IWorkbenchPart part, ISynchronizationScopeManager manager) {
		super(part);
		this.manager = manager;
	}

	/**
	 * Run the operation. This method first ensures that the scope is built
	 * by calling {@link #initializeScope(IProgressMonitor)} and then invokes the
	 * {@link #execute(IProgressMonitor)} method.
	 * @param monitor a progress monitor
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public final void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		try {
			monitor.beginTask(null, 100);
			beginOperation(Policy.subMonitorFor(monitor, 5));
			execute(Policy.subMonitorFor(monitor, 90));
		} finally {
			endOperation(Policy.subMonitorFor(monitor, 5));
			monitor.done();
		}
	}

	/**
	 * Method called from {@link #run(IProgressMonitor)} before
	 * the {@link #execute(IProgressMonitor)} method is invoked.
	 * This is done to give the operation a chance to initialize
	 * any state required to execute. By default, the
	 * {@link ISynchronizationScopeManager} for this operation
	 * is initialized if it was not previously initialized.
	 * @param monitor a progress monitor
	 * @throws InvocationTargetException if an error occurs
	 */
	protected void beginOperation(IProgressMonitor monitor) throws InvocationTargetException {
		initializeScope(monitor);
	}

	/**
	 * Method called from {@link #run(IProgressMonitor)} after the
	 * {@link #execute(IProgressMonitor)} completes of if an exception
	 * is thrown from the {@link #beginOperation(IProgressMonitor)}
	 * or the {@link #execute(IProgressMonitor)}. By default,
	 * this method does nothing. Subclasses may override.
	 * @param monitor a progress monitor
	 */
	protected void endOperation(IProgressMonitor monitor) throws InvocationTargetException {
		// Do nothing by deafult
	}

	/**
	 * Adjust the input of the operation according to the selected
	 * resource mappings and the set of interested participants. This method
	 * will prompt the user in the following cases:
	 * <ol>
	 * <li>The scope contains additional resources than those in the input.
	 * <li>The scope has additional mappings from a model in the input
	 * <li>The input contains elements from multiple models
	 * </ol>
	 * <p>
	 * The scope of this operation will only be prepared once. Subsequent
	 * calls to this method will do nothing. Also, if the scope was provided
	 * as an argument to a constructor, this method will do nothing (i.e. the
	 * scope will not be prepared again and no prompting will occur).
	 * <p>
	 * Subclasses can customize how the scope is generated by overriding
	 * the {@link #getScopeManager()} to return a custom scope manager.
	 * @param monitor a progress monitor
	 */
	protected final void initializeScope(IProgressMonitor monitor) throws InvocationTargetException {
		try {
			if (!manager.isInitialized()) {
				manager.initialize(monitor);
				promptIfInputChange(monitor);
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	/**
	 * Prompt the user by calling {@link #promptForInputChange(String, IProgressMonitor)}
	 * if the scope of the operation was expanded (as described in
	 * {@link #initializeScope(IProgressMonitor)}).
	 * @param monitor a progress monitor
	 */
	protected void promptIfInputChange(IProgressMonitor monitor) {
		ISynchronizationScope inputScope = getScope().asInputScope();
		if (getScope().hasAdditionalMappings()) {
			boolean prompt = false;
			// There are additional mappings so we may need to prompt
			ModelProvider[] inputModelProviders = inputScope.getModelProviders();
			if (hasAdditionalMappingsFromIndependantModel(inputModelProviders, getScope().getModelProviders())) {
				// Prompt if the is a new model provider in the scope that is independant
				// of any of the input mappings
				prompt = true;
			} else if (getScope().hasAdditonalResources()) {
				// We definitely need to prompt to indicate that additional resources
				prompt = true;
			} else if (inputModelProviders.length == 1) {
				// We may need to prompt depending on the nature of the additional mappings
				// We need to prompt if the additional mappings are from the same model as
				// the input or if they are from a model that has no relationship to the input model
				String modelProviderId = inputModelProviders[0].getDescriptor().getId();
				ResourceMapping[] mappings = getScope().getMappings();
				for (ResourceMapping mapping : mappings) {
					if (inputScope.getTraversals(mapping) == null) {
						// This mapping was not in the input
						String id = mapping.getModelProviderId();
						if (id.equals(modelProviderId) && !modelProviderId.equals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID)) {
							prompt = true;
							break;
						} else if (isIndependantModel(modelProviderId, id)) {
							prompt = true;
							break;
						}
					}
				}
			} else {
				// We need to prompt if there are additional mappings from an input
				// provider whose traversals overlap those of the input mappings.
				for (ModelProvider provider : inputModelProviders) {
					String id = provider.getDescriptor().getId();
					ResourceMapping[] inputMappings = inputScope.getMappings(id);
					ResourceMapping[] scopeMappings = getScope().getMappings(id);
					if (inputMappings.length != scopeMappings.length) {
						// There are more mappings for this provider.
						// We need to see if any of the new ones overlap the old ones.
						for (ResourceMapping mapping : scopeMappings) {
							ResourceTraversal[] inputTraversals = inputScope.getTraversals(mapping);
							if (inputTraversals == null) {
								// This mapping was not in the input.
								// We need to prompt if the traversal for this mapping overlaps with
								// the input mappings for the model provider
								// TODO could check for project overlap first
								ResourceTraversal[] scopeTraversals = getScope().getTraversals(mapping);
								ResourceTraversal[] inputModelTraversals = getTraversals(inputScope, inputMappings);
								if (overlaps(scopeTraversals, inputModelTraversals)) {
									prompt = true;
									break;
								}
							}
						}
					}
				}
			}
			if (prompt) {
				String previewMessage = getPreviewRequestMessage();
				previewRequested = promptForInputChange(previewMessage, monitor);
			}
		}
	}

	/**
	 * Return a string to be used in the preview request on the scope prompt
	 * or <code>null</code> if a preview of the operation results is not possible.
	 * By default, <code>null</code> is returned but subclasses may override.
	 * @return a string to be used in the preview request on the scope prompt
	 * or <code>null</code> if a preview of the operation results is not possible
	 */
	protected String getPreviewRequestMessage() {
		return null;
	}

	private boolean hasAdditionalMappingsFromIndependantModel(ModelProvider[] inputModelProviders, ModelProvider[] modelProviders) {
		ModelProvider[] additionalProviders = getAdditionalProviders(inputModelProviders, modelProviders);
		for (ModelProvider additionalProvider : additionalProviders) {
			boolean independant = true;
			// Return true if the new provider is independant of all input providers
			for (ModelProvider inputProvider : inputModelProviders) {
				if (!isIndependantModel(additionalProvider.getDescriptor().getId(), inputProvider.getDescriptor().getId())) {
					independant = false;
				}
			}
			if (independant)
				return true;
		}
		return false;
	}

	private ModelProvider[] getAdditionalProviders(ModelProvider[] inputModelProviders, ModelProvider[] modelProviders) {
		Set<ModelProvider> input = new HashSet<>();
		List<ModelProvider> result = new ArrayList<>();
		input.addAll(Arrays.asList(inputModelProviders));
		for (ModelProvider provider : modelProviders) {
			if (!input.contains(provider))
				result.add(provider);
		}
		return result.toArray(new ModelProvider[result.size()]);
	}

	private boolean overlaps(ResourceTraversal[] scopeTraversals, ResourceTraversal[] inputModelTraversals) {
		for (ResourceTraversal inputTraversal : inputModelTraversals) {
			for (ResourceTraversal scopeTraversal : scopeTraversals) {
				if (overlaps(inputTraversal, scopeTraversal)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean overlaps(ResourceTraversal inputTraversal, ResourceTraversal scopeTraversal) {
		IResource[] inputRoots = inputTraversal.getResources();
		IResource[] scopeRoots = scopeTraversal.getResources();
		for (IResource scopeResource : scopeRoots) {
			for (IResource inputResource : inputRoots) {
				if (overlaps(scopeResource, scopeTraversal.getDepth(), inputResource, inputTraversal.getDepth()))
					return true;
			}
		}
		return false;
	}

	private boolean overlaps(IResource scopeResource, int scopeDepth, IResource inputResource, int inputDepth) {
		if (scopeResource.equals(inputResource))
			return true;
		if (scopeDepth == IResource.DEPTH_INFINITE && scopeResource.getFullPath().isPrefixOf(inputResource.getFullPath())) {
			return true;
		}
		if (scopeDepth == IResource.DEPTH_ONE && scopeResource.equals(inputResource.getParent())) {
			return true;
		}
		if (inputDepth == IResource.DEPTH_INFINITE && inputResource.getFullPath().isPrefixOf(scopeResource.getFullPath())) {
			return true;
		}
		if (inputDepth == IResource.DEPTH_ONE && inputResource.equals(scopeResource.getParent())) {
			return true;
		}
		return false;
	}

	private ResourceTraversal[] getTraversals(ISynchronizationScope inputScope, ResourceMapping[] inputMappings) {
		CompoundResourceTraversal result = new CompoundResourceTraversal();
		for (ResourceMapping mapping : inputMappings) {
			result.addTraversals(inputScope.getTraversals(mapping));
		}
		return result.asTraversals();
	}

	private boolean isIndependantModel(String modelProviderId, String id) {
		if (id.equals(modelProviderId))
			return false;
		IModelProviderDescriptor desc1 = ModelProvider.getModelProviderDescriptor(modelProviderId);
		IModelProviderDescriptor desc2 = ModelProvider.getModelProviderDescriptor(id);
		return !(isExtension(desc1, desc2) || isExtension(desc2, desc1));
	}

	/*
	 * Return whether the desc1 model extends the desc2 model
	 */
	private boolean isExtension(IModelProviderDescriptor desc1, IModelProviderDescriptor desc2) {
		String[] ids = desc1.getExtendedModels();
		// First check direct extension
		for (String id : ids) {
			if (id.equals(desc2.getId())) {
				return true;
			}
		}
		// Now check for indirect extension
		for (String id : ids) {
			IModelProviderDescriptor desc3 = ModelProvider.getModelProviderDescriptor(id);
			if (isExtension(desc3, desc2)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Prompt the user to inform them that additional resource mappings
	 * have been included in the operations.
	 * @param requestPreviewMessage message to be displayed for the option to force a preview
	 * (or <code>null</code> if the preview option should not be presented
	 * @param monitor a progress monitor
	 * @return whether a preview of the operation results was requested
	 * @throws OperationCanceledException if the user choose to cancel
	 */
	protected boolean promptForInputChange(String requestPreviewMessage, IProgressMonitor monitor) {
		return showAllMappings(requestPreviewMessage);
	}

	private boolean showAllMappings(final String requestPreviewMessage) {
		final boolean[] canceled = new boolean[] { false };
		final boolean[] forcePreview = new boolean[] { false };
		Display.getDefault().syncExec(() -> {
			AdditionalMappingsDialog dialog = new AdditionalMappingsDialog(getShell(), TeamUIMessages.ResourceMappingOperation_0, getScope(), getContext());
			dialog.setPreviewMessage(requestPreviewMessage);
			int result = dialog.open();
			canceled[0] = result != Window.OK;
			if (requestPreviewMessage != null) {
				forcePreview[0] = dialog.isForcePreview();
			}
		});

		if (canceled[0]) {
			throw new OperationCanceledException();
		}
		return forcePreview[0];
	}

	/**
	 * Return the synchronization context for the operation or <code>null</code>
	 * if the operation doesn't have one or if it has not yet been created.
	 * By default, the method always returns <code>null</code>. Subclasses may override.
	 * @return the synchronization context for the operation or <code>null</code>
	 */
	protected ISynchronizationContext getContext() {
		return null;
	}

	/**
	 * Execute the operation. This method is invoked after the
	 * scope has been generated.
	 * @param monitor a progress monitor
	 * @throws InvocationTargetException if an error occurs
	 * @throws InterruptedException if operation is interrupted
	 */
	protected abstract void execute(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException;

	/**
	 * Return the scope of this operation.
	 * @return the scope of this operation
	 */
	public ISynchronizationScope getScope() {
		return manager.getScope();
	}

	/**
	 * Return whether a preview of the operation before it is performed is
	 * desired.
	 * @return whether a preview of the operation before it is performed is
	 * desired
	 */
	public boolean isPreviewRequested() {
		return previewRequested;
	}

	/**
	 * Return the scope manager for this operation.
	 * @return the scope manager for this operation.
	 */
	protected ISynchronizationScopeManager getScopeManager() {
		return manager;
	}

}
