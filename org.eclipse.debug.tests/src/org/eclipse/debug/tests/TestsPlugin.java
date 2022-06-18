/*******************************************************************************
 * Copyright (c) 2009, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM - ongoing enhancements
 *******************************************************************************/
package org.eclipse.debug.tests;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The activator class controls the plug-in life cycle
 *
 * @since 3.6
 */
public class TestsPlugin {

	public static final String PLUGIN_ID = "org.eclipse.debug.tests"; //$NON-NLS-1$

	/**
	 * Returns the file corresponding to the specified path from within this bundle
	 * @param path
	 * @return the file corresponding to the specified path from within this bundle, or
	 * <code>null</code> if not found
	 */
	public static File getFileInPlugin(IPath path) {
		try {
			Bundle bundle = FrameworkUtil.getBundle(TestsPlugin.class);
			URL installURL = new URL(bundle.getEntry("/"), path.toString()); //$NON-NLS-1$
			URL localURL= FileLocator.toFileURL(installURL);//Platform.asLocalURL(installURL);
			return new File(localURL.getFile());
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Creates a new project with the specified name
	 * @param projectName
	 * @return a new project with the specified name
	 * @throws CoreException
	 */
	public static IProject createProject(String projectName) throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= root.getProject(projectName);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}

		if (!project.isOpen()) {
			project.open(null);
		}
		return project;
	}

}
