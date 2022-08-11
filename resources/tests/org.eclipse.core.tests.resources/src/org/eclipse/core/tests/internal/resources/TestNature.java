/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.resources;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 */
public class TestNature implements IProjectNature {
	private IProject project;

	/**
	 * Constructor for TestNature.
	 */
	public TestNature() {
		super();
	}

	/**
	 * @see IProjectNature#configure()
	 */
	@Override
	public void configure() throws CoreException {
		// do nothing
	}

	/**
	 * @see IProjectNature#deconfigure()
	 */
	@Override
	public void deconfigure() throws CoreException {
		// do nothing
	}

	/**
	 * @see IProjectNature#getProject()
	 */
	@Override
	public IProject getProject() {
		return project;
	}

	/**
	 * @see IProjectNature#setProject(IProject)
	 */
	@Override
	public void setProject(IProject project) {
		this.project = project;
	}
}
