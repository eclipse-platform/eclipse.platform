/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
package org.eclipse.ant.tests.core.testplugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

public class AntTestPlugin extends AbstractUIPlugin {

	private static AntTestPlugin deflt;

	public AntTestPlugin() {
		super();
		deflt = this;
	}

	public static AntTestPlugin getDefault() {
		return deflt;
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public static void enableAutobuild(boolean enable) throws CoreException {
		// disable auto build
		IWorkspace workspace = AntTestPlugin.getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setAutoBuilding(enable);
		workspace.setDescription(desc);
	}

	public File getFileInPlugin(IPath path) {
		try {
			Bundle bundle = getDefault().getBundle();
			URL installURL = bundle.getEntry("/" + path.toString()); //$NON-NLS-1$
			URL localURL = FileLocator.toFileURL(installURL);
			return new File(localURL.getFile());
		}
		catch (IOException e) {
			return null;
		}
	}
}
