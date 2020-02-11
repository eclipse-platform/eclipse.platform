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
package org.eclipse.team.examples.model.mapping;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.team.examples.model.ModelContainer;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.team.examples.model.ModelObjectElementFile;

public abstract class ModelResourceMapping extends ResourceMapping {

	private final ModelObject object;

	public static ResourceMapping create(ModelObject object) {
		if (object instanceof ModelContainer) {
			return new ModelContainerResourceMapping((ModelContainer) object);
		}
		if (object instanceof ModelObjectDefinitionFile) {
			return new ModResourceMapping((ModelObjectDefinitionFile) object);
		}
		if (object instanceof ModelObjectElementFile) {
			return new MoeResourceMapping((ModelObjectElementFile) object);
		}
		return null;
	}

	protected ModelResourceMapping(ModelObject object) {
		this.object = object;
	}

	@Override
	public Object getModelObject() {
		return object;
	}

	@Override
	public String getModelProviderId() {
		return ExampleModelProvider.ID;
	}

	@Override
	public IProject[] getProjects() {
		return new IProject[] { (IProject)object.getProject().getResource() };
	}

}
