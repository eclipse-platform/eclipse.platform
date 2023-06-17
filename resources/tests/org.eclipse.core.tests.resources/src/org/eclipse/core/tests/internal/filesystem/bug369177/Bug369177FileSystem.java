/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.filesystem.bug369177;

import java.net.URI;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.internal.filesystem.NullFileSystem;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Special file system implementation used by TestBug369177.
 */
public class Bug369177FileSystem extends NullFileSystem {
	static final String SCHEME_BUG_369177 = "bug369177";
	private static IFileSystem instance;

	public static IFileSystem getInstance() {
		return instance;
	}

	public Bug369177FileSystem() {
		super();
		instance = this;
	}

	private void runTestScenario() {
		try {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("project");
			project.getPersistentProperties();
			project.getDefaultCharset();
			project.getContentTypeMatcher();
		} catch (CoreException e) {
			throw new Error(e);
		}
	}

	@Override
	public IFileStore getStore(IPath path) {
		runTestScenario();
		return new Bug369177FileStore(path);
	}

	@Override
	public IFileStore getStore(URI uri) {
		runTestScenario();
		return new Bug369177FileStore(IPath.fromOSString(uri.getPath()));
	}
}
