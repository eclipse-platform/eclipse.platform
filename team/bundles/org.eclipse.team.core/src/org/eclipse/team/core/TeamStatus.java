/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Status;

/**
 * Status that is returned from some Team operations or is the payload of
 * some TeamExceptions.
 *
 * @since 3.0
 */
public class TeamStatus extends Status implements ITeamStatus {

	private IResource resource;

	/**
	 * Create a new status object.
	 * @param severity the severity; one of <code>OK</code>,
	 *   <code>ERROR</code>, <code>INFO</code>, or <code>WARNING</code>
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param code the plug-in-specific status code, or <code>OK</code>
	 * @param message a human-readable message, localized to the
	 *    current locale
	 * @param exception a low-level exception, or <code>null</code> if not
	 *    applicable
	 * @param resource the resource associated with the exception
	 */
	public TeamStatus(int severity, String pluginId, int code, String message, Throwable exception, IResource resource) {
		super(severity, pluginId, code, message, exception);
		if (resource == null) {
			this.resource = ResourcesPlugin.getWorkspace().getRoot();
		} else {
			this.resource = resource;
		}
	}

	/**
	 * Return the resource associated with this status.
	 * @return Returns the resource.
	 */
	@Override
	public IResource getResource() {
		return resource;
	}
}
