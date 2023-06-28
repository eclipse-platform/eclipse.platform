/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core.variables;

import org.eclipse.core.resources.IResource;

/**
 * Resolver for the <code>${container_*}</code> variables. Accepts an optional
 * argument that is interpretted as a full path to a container in the workspace.
 * <p>
 * Moved to debug core in 3.5, existed in debug.iu since 3.0.
 * </p>
 * @since 3.5
 */
public class ContainerResolver extends ResourceResolver {

	@Override
	protected IResource translateSelectedResource(IResource resource) {
		return resource.getParent();
	}

}
