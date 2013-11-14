/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.core.regression;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.tests.core.TeamTest;

public class Bug_217673 extends TeamTest {

	public void test() throws CoreException {

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = workspace.getRoot().getProject(
				getUniqueString());
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

	public static Test suite() {
		return new TestSuite(Bug_217673.class);
	}

}
