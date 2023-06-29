/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
 *     Mikhail Khodjaiants, QNX - Bug 110227: Possible infinite loop in ProjectSourceContainer
 *******************************************************************************/
package org.eclipse.debug.core.sourcelookup.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;

/**
 * A project in the workspace. Source is searched for in the root project
 * folder and all folders within the project recursively. Optionally,
 * referenced projects may be searched as well.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ProjectSourceContainer extends ContainerSourceContainer {

	boolean fReferencedProjects=false;
	/**
	 * Unique identifier for the project source container type
	 * (value <code>org.eclipse.debug.core.containerType.project</code>).
	 */
	public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier() + ".containerType.project"; //$NON-NLS-1$

	/**
	 * Constructs a project source container.
	 *
	 * @param project the project to search for source in
	 * @param referenced whether referenced projects should be considered
	 */
	public ProjectSourceContainer(IProject project, boolean referenced) {
		super(project, true);
		fReferencedProjects = referenced;
	}

	/**
	 * Returns whether referenced projects are considered.
	 *
	 * @return whether referenced projects are considered
	 */
	public boolean isSearchReferencedProjects() {
		return fReferencedProjects;
	}

	/**
	 * Returns the project this source container references.
	 *
	 * @return the project this source container references
	 */
	public IProject getProject() {
		return (IProject) getContainer();
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	@Override
	public boolean isComposite() {
		return true;
	}

	@Override
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		if (getProject().isOpen()) {
			if (isSearchReferencedProjects()) {
				IProject project = getProject();
				IProject[] projects = getAllReferencedProjects(project);
				ISourceContainer[] folders = super.createSourceContainers();
				List<ISourceContainer> all = new ArrayList<>(folders.length + projects.length);
				Collections.addAll(all, folders);
				for (IProject p : projects) {
					if (project.exists() && project.isOpen()) {
						ProjectSourceContainer container = new ProjectSourceContainer(p, false);
						container.init(getDirector());
						all.add(container);
					}
				}
				return all.toArray(new ISourceContainer[all.size()]);
			}
			return super.createSourceContainers();
		}
		return new ISourceContainer[0];
	}

	private IProject[] getAllReferencedProjects(IProject project) throws CoreException {
		Set<IProject> all = new HashSet<>();
		getAllReferencedProjects(all, project);
		return all.toArray(new IProject[all.size()]);
	}

	private void getAllReferencedProjects(Set<IProject> all, IProject project) throws CoreException {
		IProject[] refs = project.getReferencedProjects();
		for (IProject ref : refs) {
			if (!all.contains(ref) && ref.exists() && ref.isOpen()) {
				all.add(ref);
				getAllReferencedProjects(all, ref);
			}
		}
	}
}
