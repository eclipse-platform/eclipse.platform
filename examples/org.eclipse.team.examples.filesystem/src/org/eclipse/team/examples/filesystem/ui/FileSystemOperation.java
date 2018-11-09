/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.filesystem.FileSystemProvider;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemSubscriber;
import org.eclipse.team.ui.synchronize.ModelOperation;
import org.eclipse.ui.IWorkbenchPart;

public abstract class FileSystemOperation extends ModelOperation {

	/**
	 * Create a scope manager for the file system example.
	 * @param name the name of the manager
	 * @param inputMappings the input mappings
	 * @return a scope manager
	 */
	public static SubscriberScopeManager createScopeManager(String name, ResourceMapping[] inputMappings) {
		return new SubscriberScopeManager(name, inputMappings, FileSystemSubscriber.getInstance(), true);
	}

	/**
	 * Create a file system operation.
	 * @param part the part from which the operation was launched
	 * @param manager the scope manager that provides the input to the operation
	 */
	protected FileSystemOperation(IWorkbenchPart part, SubscriberScopeManager manager) {
		super(part, manager);
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws InvocationTargetException {
		try {
			Map<FileSystemProvider, CompoundResourceTraversal> providerToTraversals = getProviderToTraversalsMap();
			monitor.beginTask(getTaskName(), providerToTraversals.size() * 100);
			monitor.setTaskName(getTaskName());
			for (FileSystemProvider provider : providerToTraversals.keySet()) {
				ResourceTraversal[] traversals = getTraversals(providerToTraversals, provider);
				execute(provider,
						traversals,
						SubMonitor.convert(monitor, 100));
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Return a map of FileSystemProvider to ResourceTraversals.
	 * @return a map of FileSystemProvider to ResourceTraversals
	 */
	private Map<FileSystemProvider, CompoundResourceTraversal> getProviderToTraversalsMap() {
		HashMap<FileSystemProvider, CompoundResourceTraversal> result = new HashMap<>();
		ISynchronizationScope scope = getScope();
		ResourceMapping[] mappings = scope.getMappings();
		for (ResourceMapping mapping : mappings) {
			ResourceTraversal[] traversals = scope.getTraversals(mapping);
			for (ResourceTraversal traversal : traversals) {
				IResource[] resources = traversal.getResources();
				for (IResource resource : resources) {
					recordResourceAndDepth(result, resource, traversal.getDepth());
				}
			}
		}
		return result;
	}

	/**
	 * Return the file system provider associated with the given project or <code>null</code>
	 * if the project is not mapped to the file system provider.
	 * @param project the project
	 * @return the file system provider associated with the given project
	 */
	protected FileSystemProvider getProviderFor(IProject project) {
		return (FileSystemProvider)RepositoryProvider.getProvider(project, FileSystemPlugin.PROVIDER_ID);
	}

	private void recordResourceAndDepth(HashMap<FileSystemProvider, CompoundResourceTraversal> providerToTraversals,
			IResource resource, int depth) {
		FileSystemProvider provider = getProviderFor(resource.getProject());
		if (provider != null) {
			CompoundResourceTraversal traversal = providerToTraversals.get(provider);
			if (traversal == null) {
				traversal = new CompoundResourceTraversal();
				providerToTraversals.put(provider, traversal);
			}
			traversal.addResource(resource, depth);
		}
	}

	/**
	 * Return the traversals that were accumulated for the given provider by the
	 * {@link #getProviderToTraversalsMap()} method.
	 * @param providerToTraversals the provider to traversals map
	 * @param provider the provider
	 * @return the traversals for the given provider
	 */
	private ResourceTraversal[] getTraversals(Map<FileSystemProvider, CompoundResourceTraversal> providerToTraversals,
			FileSystemProvider provider) {
		CompoundResourceTraversal traversal = providerToTraversals.get(provider);
		return traversal.asTraversals();
	}

	/**
	 * Execute the operation for the given provider and traversals.
	 * @param provider the provider
	 * @param traversals the traversals to be operated on
	 * @param monitor a progress monitor
	 * @throws CoreException
	 */
	protected abstract void execute(FileSystemProvider provider, ResourceTraversal[] traversals, IProgressMonitor monitor) throws CoreException;

	/**
	 * Return the task name for this operation.
	 * @return the task name for this operation
	 */
	protected abstract String getTaskName();

	@Override
	protected boolean canRunAsJob() {
		return true;
	}

}
