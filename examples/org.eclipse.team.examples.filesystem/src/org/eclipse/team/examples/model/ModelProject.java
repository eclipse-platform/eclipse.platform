/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ModelProject extends ModelContainer {

	public static boolean isModProject(IProject project) throws CoreException {
		if (! project.isOpen())
			return false;
		IProjectDescription description = project.getDescription();
		return description.hasNature(ModelNature.NATURE_ID);
	}

	public static void makeModProject(IProject project, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] natureIds = description.getNatureIds();
		List<String> result = new ArrayList<>();
		Collections.addAll(result, natureIds);
		result.add(ModelNature.NATURE_ID);
		description.setNatureIds(result.toArray(new String[result.size()]));
		project.setDescription(description, monitor);
	}

	public ModelProject(IProject project) {
		super(project);
	}

}
