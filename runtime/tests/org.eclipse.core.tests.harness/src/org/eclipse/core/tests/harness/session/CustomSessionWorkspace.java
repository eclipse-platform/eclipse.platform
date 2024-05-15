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
package org.eclipse.core.tests.harness.session;

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.core.tests.harness.session.customization.SessionCustomization;

/**
 * A session customization to use a custom workspace directory.
 */
public interface CustomSessionWorkspace extends SessionCustomization {

	/**
	 * {@return the path of the used workspace directory}
	 */
	public Path getWorkspaceDirectory() throws IOException;

	/**
	 * Sets the given workspace directory. If not called, a temporary folder is used
	 * as the workspace directory.
	 *
	 * @param workspaceDirectory the path of the directory to place the workspace
	 *                           in, must not be {@code null}
	 *
	 * @return this
	 */
	public CustomSessionWorkspace setWorkspaceDirectory(Path workspaceDirectory);

}
