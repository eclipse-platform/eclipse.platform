/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.harness.session.customization;

import java.nio.file.Path;
import org.eclipse.core.tests.harness.session.CustomSessionWorkspace;
import org.eclipse.core.tests.session.Setup;

/**
 * A session customization to use a custom workspace directory.
 */
public class CustomSessionWorkspaceDummy implements CustomSessionWorkspace {
	private Path workspaceDirectory;

	public CustomSessionWorkspaceDummy() {
		// nothing to initialize
	}

	@Override
	public CustomSessionWorkspace setWorkspaceDirectory(Path workspaceDirectory) {
		this.workspaceDirectory = workspaceDirectory;
		return this;
	}

	@Override
	public Path getWorkspaceDirectory() {
		return workspaceDirectory;
	}

	@Override
	public void prepareSession(Setup setup) throws Exception {
		// do nothing
	}

	@Override
	public void cleanupSession(Setup setup) throws Exception {
		// do nothing
	}

}
