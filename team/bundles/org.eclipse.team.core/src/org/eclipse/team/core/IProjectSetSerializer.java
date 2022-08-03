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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * IProjectSetSerializer manages the serializing and deserializing
 * of references to projects.  Given a project, it can produce a
 * UTF-8 encoded String which can be stored in a file.
 * Given this String, it can create in the workspace an IProject.
 *
 * @since 2.0
 *
 * @deprecated
 *       Use {@link org.eclipse.team.core.RepositoryProviderType#getProjectSetCapability()}
 *       to obtain an instance of {@link org.eclipse.team.core.ProjectSetCapability} instead.
 */

@Deprecated
public interface IProjectSetSerializer {

	/**
	 * For every IProject in providerProjects, return an opaque
	 * UTF-8 encoded String to act as a reference to that project.
	 * The format of the String is specific to the provider.
	 * The format of the String must be such that
	 * IProjectSetSerializer.addToWorskpace() will be able to
	 * consume it and recreate a corresponding project.
	 * @see IProjectSetSerializer#addToWorkspace(String[] referenceStrings, String filename, Object context, IProgressMonitor monitor)
	 *
	 * @param providerProjects  an array of projects that the serializer should create
	 *   text references for
	 * @param context  a UI context object. This object will either be a
	 *                 com.ibm.swt.widgets.Shell or it will be null.
	 * @param monitor  a progress monitor
	 * @return String[] an array of serialized reference strings uniquely identifying the projects
	 * @throws TeamException on failures; depends on concrete implementation
	 */
	public String[] asReference(IProject[] providerProjects, Object context, IProgressMonitor monitor) throws TeamException;

	/**
	 * For every String in referenceStrings, create in the workspace a
	 * corresponding IProject.  Return an Array of the resulting IProjects.
	 * Result is unspecified in the case where an IProject of that name
	 * already exists. In the case of failure, a TeamException must be thrown.
	 * The opaque strings in referenceStrings are guaranteed to have been previously
	 * produced by IProjectSetSerializer.asReference().
	 * @see IProjectSetSerializer#asReference(IProject[] providerProjects, Object context, IProgressMonitor monitor)
	 *
	 * @param referenceStrings  an array of reference strings uniquely identifying the projects
	 * @param filename  the name of the file that the references were read from. This is included
	 *   in case the provider needs to deduce relative paths
	 * @param context  a UI context object. This object will either be a
	 *                 com.ibm.swt.widgets.Shell or it will be null.
	 * @param monitor  a progress monitor
	 * @return IProject[]  an array of projects that were created
	 * @throws TeamException on failures; depends on concrete implementation
	 */
	public IProject[] addToWorkspace(String[] referenceStrings, String filename, Object context, IProgressMonitor monitor) throws TeamException;
}
