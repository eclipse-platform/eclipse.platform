/*******************************************************************************
 *  Copyright (c) 2023 Red Hat, Inc. and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_EARTH;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_MISSING;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_SIMPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class LinkedDotProjectTest {

	private void linkDotProject(IProject project) throws CoreException {
		IFile dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		if (dotProject.isLinked()) {
			return;
		}
		File targetDiskFile = ResourcesPlugin.getWorkspace().getRoot().getLocation().removeLastSegments(1).append(project.getName()).append(".project").toFile();
		if (!targetDiskFile.exists()) {
			try {
				targetDiskFile.getParentFile().mkdirs();
				Files.copy(dotProject.getLocation().toPath(), targetDiskFile.toPath());
			} catch (Exception ex) {
				throw new CoreException(Status.error(targetDiskFile + " cannot be created", ex)); //$NON-NLS-1$
			}
		}
		File sourceDiskFile = dotProject.getLocation().toFile();
		dotProject.createLink(IPath.fromFile(targetDiskFile), IResource.FORCE | IResource.REPLACE, null);
		sourceDiskFile.delete();
	}

	/**
	 * Tests the API method IProject#getNature
	 */
	@Test
	public void testGetNature() throws CoreException {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProject project = ws.getRoot().getProject("Project");

		//getNature on non-existent project should fail
		assertThrows(CoreException.class, () -> project.getNature(NATURE_SIMPLE));
		assertThrows(CoreException.class, () -> project.getNature(NATURE_MISSING));
		project.create(null);
		//getNature on closed project should fail
		assertThrows(CoreException.class, () -> project.getNature(NATURE_SIMPLE));
		assertThrows(CoreException.class, () -> project.getNature(NATURE_MISSING));
		project.open(new NullProgressMonitor());
		//
		linkDotProject(project);
		assertFalse(project.getLocation().isPrefixOf(project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME).getLocation()));
		//getNature on open project with no natures
		assertNull(project.getNature(NATURE_MISSING));
		assertNull(project.getNature(NATURE_EARTH));
		IProjectDescription desc = project.getDescription();
		desc.setNatureIds(new String[] { NATURE_EARTH });
		project.setDescription(desc, null);
		//getNature on open project with natures
		IProjectNature nature = project.getNature(NATURE_EARTH);
		assertNotNull(nature);
		assertNull(project.getNature(NATURE_MISSING));
		assertEquals(project, nature.getProject());
		assertFalse(project.getLocation().isPrefixOf(project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME).getLocation()));

		//ensure nature is preserved on copy
		IProject project2 = getWorkspace().getRoot().getProject("testGetNature.Destination");
		IProjectNature nature2 = null;
		project.copy(project2.getFullPath(), IResource.NONE, null);
		assertNotEquals(project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME).getLocationURI(), project2.getFile(IProjectDescription.DESCRIPTION_FILE_NAME).getLocationURI());
		assertTrue(project2.getLocation().isPrefixOf(project2.getFile(IProjectDescription.DESCRIPTION_FILE_NAME).getLocation()));
		nature2 = project2.getNature(NATURE_EARTH);
		assertNotNull(nature2);
		assertEquals(project2, nature2.getProject());
		assertEquals(project, nature.getProject());
	}

}
