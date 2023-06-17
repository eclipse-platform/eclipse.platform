/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem;

import java.io.File;

import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.examples.filesystem.history.FileSystemHistoryProvider;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemResourceVariant;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemSubscriber;

/**
 * This example illustrates how to create a concrete implementation of a <code>RepositoryProvider</code>
 * that uses the file system to act as the repository. See the plugin.xml file for the xml required
 * to register this provider with the Team extension point <code>org.eclipse.team.core.repository</code>.
 * The plugin.xml file also contains examples of how to filter menu items using a repository provider's
 * ID.
 *
 * <p>
 * This example provider illustrates the following:
 * <ol>
 * <li>simple working implementation of <code>RepositoyProvider</code>
 * <li>storage of a persistent property with the project (which provides the target location for the provider)
 * <li>access to an instance of <code>SimpleAccessOperations</code> for performing simple file operations
 * </ol>
 *
 * <p>
 * Additional functionality that will be illustrated in the future include:
 * <ol>
 * <li>Validate Save/Validate Edit
 * <li>Move/Delete Hook
 * <li>Project Sets
 * <li>Use of the workspace synchronizer (ISynchronizer)
 * <li>Use of decorators
 * <li>combining streams and progress monitors to get responsive UI
 * </ol>
 *
 */
public class FileSystemProvider extends RepositoryProvider {

	/*
	 * Create a custom rule factory to allow more optimistic concurrency
	 */
	private static final ResourceRuleFactory RESOURCE_RULE_FACTORY = new ResourceRuleFactory() {
		// Just need a subclass to instantiate
	};

	// The location of the folder on file system where the repository is stored.
	private IPath root;

	// The QualifiedName that is used to persist the location across workspace as a persistent property on a resource
	private static QualifiedName FILESYSTEM_REPO_LOC = new QualifiedName(FileSystemPlugin.ID, "disk_location"); //$NON-NLS-1$

	private static FileSystemHistoryProvider fileHistoryProvider;

	/**
	 * Create a new FileSystemProvider.
	 */
	public FileSystemProvider() {
		super();
	}

	/**
	 * This method is invoked when the provider is mapped to a project.
	 * Although we have access to the project at this point (using
	 * <code>getProject()</code>, we don't know the root location so
	 * there is nothing we can do yet.
	 *
	 * @see org.eclipse.team.core.RepositoryProvider#configureProject()
	 */
	@Override
	public void configureProject() {
		FileSystemSubscriber.getInstance().handleRootChanged(getProject(), true /* added */);
	}

	/**
	 * This method is invoked when the provider is unmapped from its
	 * project.
	 *
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	@Override
	public void deconfigure() throws CoreException {
		// Clear the persistant property containing the location
		getProject().setPersistentProperty(FILESYSTEM_REPO_LOC, null);
		FileSystemSubscriber.getInstance().handleRootChanged(getProject(), false /* removed */);
	}

	/**
	 * Return the provider ID as specified in the plugin.xml
	 *
	 * @see RepositoryProvider#getID()
	 */
	@Override
	public String getID() {
		return FileSystemPlugin.PROVIDER_ID;
	}

	/**
	 * Set the file system location for the provider. This mist be invoked after
	 * the provider is mapped and configured but before the provider is used to
	 * perform any operations.
	 *
	 * @param location the path representing the location where the project contents will be stored.
	 * @throws TeamException
	 */
	public void setTargetLocation(String location) throws TeamException {

		// set the instance variable to the provided path
		root = IPath.fromOSString(location);

		// ensure that the location is a folder (if it exists)
		File file = new File(location);
		if (file.exists() && !file.isDirectory()) {
			throw new TeamException(Policy.bind("FileSystemProvider.mustBeFolder", location)); //$NON-NLS-1$
		}

		// record the location as a persistant property so it will be remembered across platform invokations
		try {
			getProject().setPersistentProperty(FILESYSTEM_REPO_LOC, location);
		} catch (CoreException e) {
			throw FileSystemPlugin.wrapException(e);
		}
	}

	/**
	 * Returns the folder in the file system to which the provider is connected.
	 * Return <code>null</code> if there is no location or there was a problem
	 * determining it.
	 *
	 * @return IPath The path to the root of the repository.
	 */
	public IPath getRoot() {
		if (root == null) {
			try {
				String location = getProject().getPersistentProperty(FILESYSTEM_REPO_LOC);
				if (location == null) {
					return null;
				}
				root = IPath.fromOSString(location);
			} catch (CoreException e) {
				// log the problem and carry on
				FileSystemPlugin.log(e);
				return null;
			}
		}
		return root;
	}

	/**
	 * Return an object that provides the operations for transfering data
	 * to and from the provider's location.
	 */
	public FileSystemOperations getOperations() {
		return new FileSystemOperations(this);
	}

	@Override
	public IFileModificationValidator getFileModificationValidator() {
		return getFileModificationValidator2();
	}

	@Override
	public FileModificationValidator getFileModificationValidator2() {
		return new org.eclipse.team.examples.filesystem.FileModificationValidator(this);
	}

	/**
	 * Return the resource variant for the local resource using the bytes to
	 * identify the variant.
	 * @param resource the resource
	 * @param bytes the bytes that identify the resource variant
	 * @return the resource variant handle
	 */
	public IResourceVariant getResourceVariant(IResource resource, byte[] bytes) {
		if (bytes == null) return null;
		File file = getFile(resource);
		if (file == null) return null;
		return new FileSystemResourceVariant(file, bytes);
	}

	/**
	 * Return the resource variant for the local resource.
	 * @param resource the resource
	 * @return the resource variant
	 */
	public IResourceVariant getResourceVariant(IResource resource) {
		File file = getFile(resource);
		if (file == null || !file.exists()) return null;
		return new FileSystemResourceVariant(file);
	}

	/**
	 * Return the <code>java.io.File</code> that the given resource maps to.
	 * Return <code>null</code> if the resource is not a child of this provider's
	 * project.
	 * @param resource the resource
	 * @return the file that the resource maps to.
	 */
	public java.io.File getFile(IResource resource) {
		if (resource.getProject().equals(getProject())) {
			IPath rootdir = getRoot();
			return new File(rootdir.append(resource.getProjectRelativePath()).toOSString());
		}
		return null;
	}

	@Override
	public IResourceRuleFactory getRuleFactory() {
		return RESOURCE_RULE_FACTORY;
	}

	@Override
	public IFileHistoryProvider getFileHistoryProvider() {
		if (FileSystemProvider.fileHistoryProvider == null) {
			FileSystemProvider.fileHistoryProvider = new FileSystemHistoryProvider();
		}
		return FileSystemProvider.fileHistoryProvider;
	}

}
