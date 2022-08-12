/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.examples.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ModelObjectElementFile extends ModelFile {

	public static final String MODEL_OBJECT_ELEMENTFILE_EXTENSION = "moe";

	private final ModelObjectDefinitionFile parent;

	public static boolean isMoeFile(IResource resource) {
		return resource instanceof IFile && MODEL_OBJECT_ELEMENTFILE_EXTENSION.equals(resource.getFileExtension());
	}

	public ModelObjectElementFile(ModelObjectDefinitionFile parent, IFile file) {
		super(file);
		this.parent = parent;
	}

	@Override
	public ModelObject[] getChildren() {
		return new ModelObject[0];
	}

	@Override
	public ModelObject getParent() {
		return parent;
	}

	@Override
	public void delete() throws CoreException {
		parent.remove(this);
		super.delete();
	}

}
