/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.core;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.*;

public class RepositoryProviderTypeBic extends RepositoryProviderType {
	File createdFile;

	@Override
	public ProjectSetCapability getProjectSetCapability() {
		return new ProjectSetCapability() {
			@Override
			public IProject[] addToWorkspace(String[] referenceStrings,
					ProjectSetSerializationContext context,
					IProgressMonitor monitor) throws TeamException {
				return new IProject[0];
			}

			@Override
			public void projectSetCreated(File file,
					ProjectSetSerializationContext context,
					IProgressMonitor monitor) {
				createdFile = file;
			}
		};
	}

	public File getCreatedFile() {
		return createdFile;
	}

	/**
	 * Sets the createdFile.
	 * @param createdFile The createdFile to set
	 */
	public void setCreatedFile(File createdFile) {
		this.createdFile = createdFile;
	}

}
