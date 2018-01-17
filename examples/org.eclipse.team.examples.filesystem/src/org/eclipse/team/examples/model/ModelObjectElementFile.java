/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public ModelObject[] getChildren() {
		return new ModelObject[0];
	}
	
	public ModelObject getParent() {
		return parent;
	}
	
	public void delete() throws CoreException {
		parent.remove(this);
		super.delete();
	}

}
