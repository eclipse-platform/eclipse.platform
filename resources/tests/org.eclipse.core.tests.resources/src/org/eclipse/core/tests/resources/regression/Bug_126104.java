/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests copying a file to a linked folder that does not exist on disk
 */
public class Bug_126104 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Test
	public void testBug() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("p1");
		IFile source = project.getFile("source");
		createInWorkspace(source);
		IFolder link = project.getFolder("link");
		IFileStore location = workspaceRule.getTempStore();
		link.createLink(location.toURI(), IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		IFile destination = link.getFile(source.getName());
		source.copy(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertThat(destination).matches(IResource::exists, "exists");

		//try the same thing with move
		removeFromWorkspace(destination);
		location.delete(EFS.NONE, createTestMonitor());
		source.move(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertThat(source).matches(not(IResource::exists), "not exists");
		assertThat(destination).matches(IResource::exists, "exists");
	}

}
