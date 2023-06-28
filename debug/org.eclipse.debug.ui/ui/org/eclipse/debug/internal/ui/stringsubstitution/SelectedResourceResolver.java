/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.stringsubstitution;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.debug.internal.core.variables.ResourceResolver;

/**
 * Resolves the currently selected resource.
 *
 * @since 3.5
 */
public class SelectedResourceResolver extends ResourceResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		IResource resource = SelectedResourceManager.getDefault().getSelectedResource();
		if (resource != null) {
			return translateToValue(resource, variable);
		}
		abort(MessageFormat.format(StringSubstitutionMessages.SelectedResourceResolver_0, new Object[] { getReferenceExpression(variable, argument) }), null);
		return null;
	}

}
