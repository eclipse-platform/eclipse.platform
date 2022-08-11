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

import java.net.URI;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.variableresolvers.PathVariableResolver;

/**
 * Path Variable representing the parent directory of the variable provided
 * in argument, following the syntax:
 *
 * "${PARENT-COUNT-MyVariable}"
 *
 */
public class ParentVariableResolver extends PathVariableResolver {

	final public static String NAME = "PARENT"; //$NON-NLS-1$

	public ParentVariableResolver() {
		// nothing
	}

	@Override
	public String getValue(String variable, IResource resource) {
		int index = variable.indexOf('-');
		if (index == -1 || index == (variable.length() - 1))
			return null;

		String countRemaining = variable.substring(index + 1);
		index = countRemaining.indexOf('-');
		if (index == -1 || index == (variable.length() - 1))
			return null;

		String countString = countRemaining.substring(0, index);
		int count = 0;
		try {
			count = Integer.parseInt(countString);
			if (count < 0)
				return null;
		} catch (NumberFormatException e) {
			return null;
		}
		String argument = countRemaining.substring(index + 1);

		URI value = resource.getPathVariableManager().getURIValue(argument);
		if (value == null)
			return null;
		value = resource.getPathVariableManager().resolveURI(value);
		value = URIUtil.toURI(URIUtil.toPath(value).removeLastSegments(count));

		return value.toASCIIString();
	}
}
