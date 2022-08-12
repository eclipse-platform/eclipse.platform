/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.team.TeamHook;

/**
 * The internal abstract superclass of all {@link TeamHook} implementations.  This superclass
 * provides access to internal non-API methods that are not available from the API
 * package. Plugin developers should not subclass this class.
 *
 * @see TeamHook
 */
public class InternalTeamHook {
	/**
	 * Internal implementation of {@link TeamHook#setRuleFactory(IProject, IResourceRuleFactory)}.
	 */
	@SuppressWarnings("javadoc") // Suppress the "method in not visible" warning.
	protected void setRuleFactory(IProject project, IResourceRuleFactory factory) {
		Workspace workspace = ((Workspace) project.getWorkspace());
		((Rules) workspace.getRuleFactory()).setRuleFactory(project, factory);
	}
}
