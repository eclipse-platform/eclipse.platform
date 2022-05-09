/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - Fix #120
 *******************************************************************************/
package org.eclipse.core.tests.resources.refresh;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.refresh.*;
import org.eclipse.core.tests.resources.session.TestBug316182;

/**
 * Refresh provider depicting bug 316182.
 */
public class Bug316182RefreshProvider extends RefreshProvider {
	@Override
	public IRefreshMonitor installMonitor(IResource resource, IRefreshResult result) {
		try {
			IProject project = resource.getWorkspace().getRoot().getProject("project_TestBug316182");
			project.getPersistentProperties();
			project.getDefaultCharset();
			project.getContentTypeMatcher();
		} catch (Exception e) {
			// remember the exception
			TestBug316182.CAUGHT_EXCEPTION = e;
		}
		return null;
	}
}
