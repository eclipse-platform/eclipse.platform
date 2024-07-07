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

import static org.eclipse.core.tests.harness.FileSystemHelper.deleteOnShutdownRecursively;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.core.tests.harness.session.CustomSessionWorkspace;
import org.eclipse.core.tests.session.Setup;

/**
 * A session customization to use a custom workspace directory.
 */
public class CustomSessionWorkspaceImpl implements CustomSessionWorkspace {
	private Path workspaceDirectory;
	private static final String TEMP_DIR_PREFIX = "eclipse_session_workspace";

	public CustomSessionWorkspaceImpl() {
		// nothing to initialize
	}

	@Override
	public CustomSessionWorkspace setWorkspaceDirectory(Path workspaceDirectory) {
		Objects.requireNonNull(workspaceDirectory);
		this.workspaceDirectory = workspaceDirectory;
		deleteOnShutdownRecursively(workspaceDirectory);
		return this;
	}

	@Override
	public Path getWorkspaceDirectory() throws IOException {
		if (workspaceDirectory == null) {
			setWorkspaceDirectory(Files.createTempDirectory(TEMP_DIR_PREFIX));
		}
		return workspaceDirectory;
	}

	@Override
	public void prepareSession(Setup setup) throws Exception {
		setup.setEclipseArgument(Setup.DATA, getWorkspaceDirectory().toString());
	}

	@Override
	public void cleanupSession(Setup setup) throws Exception {
		// nothing to cleanup in this customization
	}

}
