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

import java.net.URI;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.resources.team.TeamHook;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.RepositoryProvider;

/**
 * This class forwards TeamHook callbacks to the proper RepositoryProvider
 */
public class TeamHookDispatcher extends TeamHook {

	private static final ResourceRuleFactory defaultFactory = new ResourceRuleFactory() {};
	private static TeamHookDispatcher instance;

	public static void setProviderRuleFactory(IProject project, IResourceRuleFactory factory) {
		if (instance != null) {
			if (factory == null) {
				factory = defaultFactory;
			}
			instance.setRuleFactory(project, factory);
		}
	}

	public TeamHookDispatcher() {
		instance = this;
	}

	/**
	 * @see org.eclipse.core.resources.team.TeamHook#validateCreateLink(org.eclipse.core.resources.IFile, int, org.eclipse.core.runtime.IPath)
	 */
	public IStatus validateCreateLink(IFile file, int updateFlags, IPath location) {
		RepositoryProvider provider = getProvider(file);
		if (provider == null) {
			return super.validateCreateLink(file, updateFlags, location);
		} else {
			return provider.validateCreateLink(file, updateFlags, location);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.team.TeamHook#validateCreateLink(org.eclipse.core.resources.IFile, int, java.net.URI)
	 */
	public IStatus validateCreateLink(IFile file, int updateFlags, URI location) {
		RepositoryProvider provider = getProvider(file);
		if (provider == null) {
			return super.validateCreateLink(file, updateFlags, location);
		} else {
			return provider.validateCreateLink(file, updateFlags, location);
		}
	}

	/**
	 * @see org.eclipse.core.resources.team.TeamHook#validateCreateLink(org.eclipse.core.resources.IFolder, int, org.eclipse.core.runtime.IPath)
	 */
	public IStatus validateCreateLink(IFolder folder, int updateFlags, IPath location) {
		RepositoryProvider provider = getProvider(folder);
		if (provider == null) {
			return super.validateCreateLink(folder, updateFlags, location);
		} else {
			return provider.validateCreateLink(folder, updateFlags, location);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.team.TeamHook#validateCreateLink(org.eclipse.core.resources.IFolder, int, java.net.URI)
	 */
	public IStatus validateCreateLink(IFolder folder, int updateFlags, URI location) {
		RepositoryProvider provider = getProvider(folder);
		if (provider == null) {
			return super.validateCreateLink(folder, updateFlags, location);
		} else {
			return provider.validateCreateLink(folder, updateFlags, location);
		}
	}

	/**
	 * Method getProvider.
	 * @param folder
	 * @return RepositoryProvider
	 */
	private RepositoryProvider getProvider(IResource resource) {
		return RepositoryProvider.getProvider(resource.getProject());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.team.TeamHook#getRuleFactory(org.eclipse.core.resources.IProject)
	 */
	public IResourceRuleFactory getRuleFactory(IProject project) {
		if (RepositoryProvider.isShared(project)) {
			RepositoryProvider provider = getProvider(project);
			// Provider can be null if the provider plugin is not available
			if (provider != null) {
				return provider.getRuleFactory();
			}
		}
		// Use the default provided by the superclass
		return super.getRuleFactory(project);
	}

}
