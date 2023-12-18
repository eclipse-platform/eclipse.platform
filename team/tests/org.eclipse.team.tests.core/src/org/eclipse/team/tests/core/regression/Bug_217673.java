/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.team.tests.core.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setReadOnly;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.eclipse.team.core.RepositoryProvider;
import org.junit.Rule;
import org.junit.Test;

public class Bug_217673 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Test
	public void test() throws CoreException {
		IWorkspace workspace = getWorkspace();
		final IProject project = workspace.getRoot().getProject(
				createUniqueString());
		project.create(null);
		project.open(null);
		IResource resource = project.getFile(".project");
		IPath linkTarget = getRandomLocation();
		try {
			RepositoryProvider.map(project,
					PessimisticRepositoryProvider.NATURE_ID);
			PessimisticRepositoryProvider.markWritableOnEdit = true;
			setReadOnly(resource, true);
			linkTarget.toFile().mkdir();
			project.getFolder("test").createLink(linkTarget, IResource.NONE,
					null);
			assertTrue(".project should no longer be read-only",
					!isReadOnly(resource));
		} finally {
			PessimisticRepositoryProvider.markWritableOnEdit = false;
			RepositoryProvider.unmap(project);
			linkTarget.toFile().delete();
		}
	}

	private boolean isReadOnly(IResource resource) {
		ResourceAttributes resourceAttributes = resource
				.getResourceAttributes();
		return resourceAttributes.isReadOnly();
	}

}
