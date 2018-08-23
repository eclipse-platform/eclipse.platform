/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core.mappings.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;

public class ModelFile extends ModelObject {

	public static final String MODEL_FILE_EXTENSION = "mod";

	public static boolean isModFile(IResource resource) {
		if (resource instanceof IFile) {
			String fileExtension = resource.getFileExtension();
			if (fileExtension != null)
				return fileExtension.equals(MODEL_FILE_EXTENSION);
		}
		return false;
	}

	public static IResource[] getReferencedResources(String projectName,
			IStorage storage) {
		if (storage == null)
			return new IResource[0];
		List result = new ArrayList();
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}

	public ModelFile(IFile file) {
		super(file);
	}

	public ModelObject[] getChildren() throws CoreException {
		return null;
	}

	public String getName() {
		String name = super.getName();
		int index = name.lastIndexOf(".");
		return name.substring(0, index);
	}

}
