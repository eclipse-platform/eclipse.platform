/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This builder is for investigating a PR.  It creates java files on disk, then
 * does refresh local to bring them into the workspace.  It creates the same file
 * regardless of full vs. incremental build, or what the delta is.
 */
public class RefreshLocalJavaFileBuilder extends TestBuilder {
	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.refreshbuilder";

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		super.build(kind, args, monitor);

		IProject project = getProject();
		IFile file = project.getFile("A.java");
		IPath localLocation = project.getLocation().append(file.getName());
		java.io.File localFile = localLocation.toFile();
		try (FileOutputStream out = new FileOutputStream(localFile)) {
			if (localFile.exists()) {
				localFile.delete();
			}

			out.write("public class A {}".getBytes());
		} catch (IOException streamCloseIgnored) {
			// ignore;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		return null;
	}
}
