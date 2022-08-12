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
 *     Dan Rubel - project set serializer API
 *******************************************************************************/
package org.eclipse.team.core;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.core.Messages;

/**
 * An object for serializing and deserializing
 * references to projects.  Given a project, it can produce a
 * UTF-8 encoded String which can be stored in a file.
 * Given this String, it can load a project into the workspace.
 * It also provides a mechanism
 * by which repository providers can be notified when a project set is created and exported.
 *
 * @see RepositoryProviderType
 *
 * @since 2.1
 */
public abstract class ProjectSetCapability {

	/**
	 * Scheme constant (value "scm") indicating the SCM URI.
	 *
	 * @since 3.6
	 */
	public static final String SCHEME_SCM = "scm"; //$NON-NLS-1$

	/**
	 * Ensure that the provider type is backwards compatible by
	 * passing the project set serializer to the type if a serializer
	 * is registered. This is required for repository providers
	 * who implemented a project set capability in 2.1 (before the
	 * capability contained the serialization API) and have not
	 * released a 3.0 plugin yet. This method is
	 * called before project set export and import and can be used by
	 * other clients who work with project sets.
	 *
	 * @param type the provider type instance
	 * @param capability the capability that was obtained from the provider type
	 *
	 * @since 3.0
	 */
	public static void ensureBackwardsCompatible(RepositoryProviderType type, ProjectSetCapability capability) {
		if (capability != null) {
			IProjectSetSerializer oldSerializer = Team.getProjectSetSerializer(type.getID());
			if (oldSerializer != null) {
				capability.setSerializer(oldSerializer);
			}
		}
	}

	/**
	 * The old serialization interface
	 */
	private IProjectSetSerializer serializer;

	/**
	 * Notify the provider that a project set has been created at path. Only
	 * providers identified as having projects in the project set will be
	 * notified. The project set may or may not be created in a workspace
	 * project (thus may not be a resource).
	 *
	 * @param file the project set file that was created
	 * @param context a UI context object. This object will either be a
	 *                 com.ibm.swt.widgets.Shell or it will be null.
	 * @param monitor a progress monitor
	 *
	 * @deprecated should use or override
	 * projectSetCreated(File, ProjectSetSerializationContext, IProgressMonitor)
	 * instead
	 */
	@Deprecated
	public void projectSetCreated(File file, Object context, IProgressMonitor monitor) {
		//default is to do nothing
	}

	/**
	 * Notify the provider that a project set has been created at path. Only
	 * providers identified as having projects in the project set will be
	 * notified. The project set may or may not be created in a workspace
	 * project (thus may not be a resource).
	 *
	 * @param file the project set file that was created
	 * @param context
	 * 		the context in which the references are created
	 * 		(not <code>null</code>)
	 * @param monitor a progress monitor
	 *
	 * @since 3.0
	 */
	public void projectSetCreated(File file, ProjectSetSerializationContext context, IProgressMonitor monitor) {
		// Invoke old method by default
		projectSetCreated(file, context.getShell(), monitor);
	}

	/**
	 * For every project in providerProjects, return an opaque
	 * UTF-8 encoded String to act as a reference to that project.
	 * The format of the String is specific to the provider.
	 * The format of the String must be such that
	 * {@link #addToWorkspace(String[], ProjectSetSerializationContext, IProgressMonitor)}
	 * will be able to consume it and load the corresponding project.
	 * <p>
	 * This default implementation simply throws an exception
	 * indicating that no references can be created unless there
	 * is an IProjectSetSerializer registered for the repository
	 * provider type in which case the operation is delegated to the
	 * serializer.
	 * Subclasses are expected to override.
	 *
	 * @since 3.0
	 *
	 * @param providerProjects
	 * 		an array of projects for which references are needed
	 * 		(not <code>null</code> and contains no <code>null</code>s)
	 * @param context
	 * 		the context in which the references are created
	 * 		(not <code>null</code>)
	 * @param monitor
	 * 		a progress monitor or <code>null</code> if none
	 * @return
	 * 		an array containing exactly the same number of elements
	 * 		as the providerProjects argument
	 * 		where each element is a serialized reference string
	 * 		uniquely identifying the corresponding the project in the providerProjects array
	 * 		(not <code>null</code> and contains no <code>null</code>s)
	 * @throws TeamException
	 * 		thrown if there is a reference string cannot be created for a project
	 */
	public String[] asReference(
		IProject[] providerProjects,
		ProjectSetSerializationContext context,
		IProgressMonitor monitor)
		throws TeamException {

		if (serializer != null) {
			return serializer.asReference(providerProjects, context.getShell(), monitor);
		}
		throw new TeamException(Messages.ProjectSetCapability_0);
	}

	/**
	 * For every String in <code>referenceStrings</code>, load the corresponding project into the workspace.
	 * The opaque strings in <code>referenceStrings</code> are guaranteed to have been previously
	 * produced by {@link #asReference(IProject[], ProjectSetSerializationContext, IProgressMonitor)}.
	 * The {@link #confirmOverwrite(ProjectSetSerializationContext, IProject[])} method is called with an array of projects
	 * for which projects of the same name already exists in the workspace.
	 * <p>
	 * Callers from within a UI context should wrap a call to this method
	 * inside a <code>WorkspaceModifyOperation</code> so that events generated as a result
	 * of this operation are deferred until the outermost operation
	 * has successfully completed.
	 * <p>
	 * This default implementation simply throws an exception
	 * indicating that no projects can be loaded unless there
	 * is an {@link IProjectSetSerializer} registered for the repository
	 * provider type in which case the operation is delegated to the
	 * serializer.
	 * Subclasses are expected to override.
	 *
	 * @since 3.0
	 *
	 * @param referenceStrings
	 * 		an array of reference strings uniquely identifying the projects
	 * 		(not <code>null</code> and contains no <code>null</code>s)
	 * @param context
	 * 		the context in which the projects are loaded
	 * 		(not <code>null</code>)
	 * @param monitor
	 * 		a progress monitor or <code>null</code> if none
	 * @return IProject[]
	 * 		an array of projects that were loaded
	 * 		excluding those projects already existing and not overwritten
	 * 		(not <code>null</code>, contains no <code>null</code>s)
	 * @throws TeamException
	 * 		thrown if there is a problem loading a project into the workspace.
	 * 		If an exception is thrown, then the workspace is left in an unspecified state
	 * 		where some of the referenced projects may be loaded or partially loaded, and others may not.
	 */
	public IProject[] addToWorkspace(
		String[] referenceStrings,
		ProjectSetSerializationContext context,
		IProgressMonitor monitor)
		throws TeamException {

		if (serializer != null) {
			return serializer.addToWorkspace(referenceStrings, context.getFilename(), context.getShell(), monitor);
		}
		throw new TeamException(Messages.ProjectSetCapability_1);
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// Internal utility methods for subclasses
	//
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Determine if any of the projects already exist
	 * and confirm which of those projects are to be overwritten.
	 *
	 * @since 3.0
	 *
	 * @param context
	 * 		the context in which the projects are loaded
	 * 		(not <code>null</code>)
	 * @param projects
	 * 		an array of proposed projects to be loaded
	 * 		(not <code>null</code>, contains no <code>null</code>s)
	 * @return
	 * 		an array of confirmed projects to be loaded
	 * 		or <code>null</code> if the operation is to be canceled.
	 * @throws TeamException on failure
	 */
	protected IProject[] confirmOverwrite(
		ProjectSetSerializationContext context,
		IProject[] projects)
		throws TeamException {

		// Build a collection of existing projects

		final Collection<IProject> existingProjects = new ArrayList<>();
		for (IProject eachProj : projects) {
			if (eachProj.exists()) {
				existingProjects.add(eachProj);
			} else if (new File(eachProj.getParent().getLocation().toFile(), eachProj.getName()).exists()) {
				existingProjects.add(eachProj);
			}
		}
		if (existingProjects.isEmpty())
			return projects;

		// Confirm the overwrite

		IProject[] confirmed =
			context.confirmOverwrite(
				existingProjects.toArray(
					new IProject[existingProjects.size()]));
		if (confirmed == null)
			return null;
		if (existingProjects.size() == confirmed.length)
			return projects;

		// Return the amended list of projects to be loaded

		Collection<IProject> result = new ArrayList<>(projects.length);
		result.addAll(Arrays.asList(projects));
		result.removeAll(existingProjects);
		for (IProject eachProj : confirmed) {
			if (existingProjects.contains(eachProj))
				result.add(eachProj);
		}
		return result.toArray(new IProject[result.size()]);
	}

	/*
	 * Set the serializer to the one registered. The serializer
	 * will be used if subclasses do not override asReference
	 * and addToWorkspace
	 */
	void setSerializer(IProjectSetSerializer serializer) {
		this.serializer = serializer;
	}

	/**
	 * Return the URI for the given reference string or <code>null</code>
	 * if this capability does not support file system schemes as defined by
	 * the <code>org.eclipse.core.filesystem.filesystems</code> extension
	 * point.
	 * @see #getProject(String)
	 * @param referenceString a reference string obtained from
	 * {@link #asReference(IProject[], ProjectSetSerializationContext, IProgressMonitor)}
	 * @return the URI for the given reference string or <code>null</code>
	 * @since 3.2
	 */
	public URI getURI(String referenceString) {
		return null;
	}

	/**
	 * Return the name of the project that is the target of the given
	 * reference string or <code>null</code> if this capability does not
	 * support parsing of reference strings.
	 * @see #getURI(String)
	 * @param referenceString  reference string obtained from
	 * {@link #asReference(IProject[], ProjectSetSerializationContext, IProgressMonitor)}
	 * @return  the name of the project that is the target of the given
	 * reference string or <code>null</code>
	 * @since 3.2
	 */
	public String getProject(String referenceString) {
		return null;
	}

	/**
	 * Convert the given URI and projectName to a reference string that can be
	 * passed to the
	 * {@link #addToWorkspace(String[], ProjectSetSerializationContext, IProgressMonitor)}
	 * method. The scheme of the provided URI must match the scheme of the
	 * repository provider type from which this capability was obtained.
	 * <p>
	 * Since 3.7 SCM URIs are also accepted.
	 * </p>
	 * <p>
	 * The default implementation returns <code>null</code>. Subclasses may
	 * override.
	 * </p>
	 *
	 * @see #SCHEME_SCM
	 * @param uri
	 *            the URI that identifies the location of the project in the
	 *            repository.
	 * @param projectName
	 *            the name of the project to use. If <code>null</code>, use a
	 *            project name from the provided SCM URI. If the URI does not
	 *            contain the project name the last segment of the URI's path is
	 *            used. If this fails, <code>null</code> is returned.
	 * @return the reference string representing a project that can be loaded
	 *         into the workspace or <code>null</code>, if the URI and name
	 *         cannot be translated into a reference string
	 * @since 3.2
	 */
	public String asReference(URI uri, String projectName) {
		return null;
	}
}
