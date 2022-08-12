/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 * This nature will try to modify resources outside the project scope.
 */
public class Bug127562Nature extends TestNature {
	/**
	 * Constructor for SimpleNature.
	 */
	public Bug127562Nature() {
		super();
	}

	@Override
	public void configure() throws CoreException {
		super.configure();
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject("Project" + System.currentTimeMillis());
		p.create(null);
	}

	@Override
	public void deconfigure() throws CoreException {
		super.deconfigure();
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject("Project" + System.currentTimeMillis());
		p.create(null);
	}
}
