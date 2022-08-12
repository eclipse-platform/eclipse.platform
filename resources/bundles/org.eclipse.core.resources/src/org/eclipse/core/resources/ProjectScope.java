/*******************************************************************************
 * Copyright (c) 2004, 2014 IBM Corporation and others.
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
package org.eclipse.core.resources;

import org.eclipse.core.internal.preferences.AbstractScope;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.osgi.service.prefs.Preferences;

/**
 * Object representing the project scope in the Eclipse preferences
 * hierarchy. Can be used as a context for searching for preference
 * values (in the <code>org.eclipse.core.runtime.IPreferencesService</code>
 * APIs) or for determining the correct preference node to set values in the store.
 * <p>
 * Project preferences are stored on a per project basis in the
 * project's content area as specified by <code>IProject#getLocation</code>.
 * </p><p>
 * The path for preferences defined in the project scope hierarchy
 * is as follows: <code>/project/&lt;projectName&gt;/&lt;qualifier&gt;</code>
 * </p>
 * <p>
 * This class is not intended to be subclassed. This class may be instantiated.
 * </p>
 * @see IProject#getLocation()
 * @since 3.0
 */
public final class ProjectScope extends AbstractScope {
	/**
	 * String constant (value of <code>"project"</code>) used for the scope name
	 * for this preference scope.
	 */
	public static final String SCOPE = "project"; //$NON-NLS-1$

	private final IProject project;

	/**
	 * Create and return a new project scope for the given project. The given
	 * project must not be <code>null</code>.
	 *
	 * @param context the project
	 * @exception IllegalArgumentException if the project is <code>null</code>
	 */
	public ProjectScope(IProject context) {
		super();
		if (context == null)
			throw new IllegalArgumentException();
		this.project = context;
	}

	@Override
	public IEclipsePreferences getNode(String qualifier) {
		if (qualifier == null)
			throw new IllegalArgumentException();
		IPreferencesService preferencesService = Platform.getPreferencesService();
		Preferences scopeNode = preferencesService.getRootNode().node(SCOPE);
		Preferences projectNode = scopeNode.node(project.getName());
		return (IEclipsePreferences) projectNode.node(qualifier);
	}

	@Override
	public IPath getLocation() {
		IPath location = project.getLocation();
		return location == null ? null : location.append(EclipsePreferences.DEFAULT_PREFERENCES_DIRNAME);
	}

	@Override
	public String getName() {
		return SCOPE;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProjectScope))
			return false;
		ProjectScope other = (ProjectScope) obj;
		return project.equals(other.project);
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 31 + project.getFullPath().hashCode();
	}
}
