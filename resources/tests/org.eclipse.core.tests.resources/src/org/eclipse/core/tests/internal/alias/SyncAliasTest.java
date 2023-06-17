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
 *     Igor Fedorenko - testNestedProjects
 *******************************************************************************/
package org.eclipse.core.tests.internal.alias;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests out of sync cases and refreshLocal in the presence of duplicate
 * resources.
 */
public class SyncAliasTest extends ResourceTest {
	/**
	 * Tests synchronization in presence of nested projects.
	 * See bug 244315 for details.
	 */
	public void testNestedProjects() throws CoreException {
		final IWorkspaceRoot root = getWorkspace().getRoot();
		File fsRoot = root.getLocation().toFile();
		File fsParent = new File(fsRoot, "parent");
		fsParent.mkdirs();
		final File childProject = new File(fsParent, "nestedProject");
		childProject.mkdirs();
		final IProject nestedProject = root.getProject("nestedProject");
		final IFolder nestedTarget = nestedProject.getFolder("target");

		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			//create top level project
			IProject parent = root.getProject("parent");
			parent.create(monitor);
			parent.open(monitor);
			parent.refreshLocal(IResource.DEPTH_INFINITE, monitor);

			//create project physically nested in top level project
			IProjectDescription description = getWorkspace().newProjectDescription("nestedProject");
			description.setLocation(IPath.fromOSString(childProject.getAbsolutePath()));
			nestedProject.create(description, monitor);
			nestedProject.open(monitor);

			//now create a folder in the nested project, which should also cause it to appear in parent
			nestedTarget.create(false /*force*/, true /*local*/, monitor);
		}, new NullProgressMonitor());

		//at this point the nested target exists, but the alias in the parent project has not been created
		//IProject parent = root.getProject("parent");
		//assertTrue(parent.getFolder("nestedProject/target").exists()); -> this will fail

		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			File file = new File(childProject, "target/classes/META-INF/ejb.xml");
			file.getParentFile().mkdirs();
			try {
				FileOutputStream fos = new FileOutputStream(file);
				fos.close();
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", "Test failed due to unexpected IOException"));
			}
			nestedTarget.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}, new NullProgressMonitor());
	}
}
