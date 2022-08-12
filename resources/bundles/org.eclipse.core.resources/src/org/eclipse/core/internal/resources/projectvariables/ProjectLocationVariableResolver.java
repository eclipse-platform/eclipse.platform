/*******************************************************************************
 * Copyright (c) 2008, 2014 Freescale Semiconductor and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Freescale Semiconductor - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.resources.projectvariables;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.variableresolvers.PathVariableResolver;

/**
 *
 */
public class ProjectLocationVariableResolver extends PathVariableResolver {

	public static String NAME = "PROJECT_LOC"; //$NON-NLS-1$

	public ProjectLocationVariableResolver() {
		// nothing
	}

	@Override
	public String[] getVariableNames(String variable, IResource resource) {
		return new String[] {NAME};
	}

	@Override
	public String getValue(String variable, IResource resource) {
		if (resource.getProject().getLocationURI() != null)
			return resource.getProject().getLocationURI().toASCIIString();
		return null;
	}
}
