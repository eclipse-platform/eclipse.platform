/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem;

import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.RepositoryProviderType;

/**
 * The file system repository provider types
 */
public class FileSystemProviderType extends RepositoryProviderType {

	@Override
	public ProjectSetCapability getProjectSetCapability() {
		// Create an empty project set capability to test backwards compatibility
		return new ProjectSetCapability() {};
	}

}
