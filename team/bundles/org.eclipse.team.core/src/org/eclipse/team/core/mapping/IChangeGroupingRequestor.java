/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.mapping;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Interface used to allow model tooling to request that a particular set of modified files
 * be committed together to the repository. It is used by the {@link ChangeTracker} class to
 * track changes and request that they be grouped when appropriate. Clients may obtain an
 * instance of this interface from a repository provider plug-in using the adapter manager in the
 * following way:
 * <pre>
 *  RepositoryProvderType type = ....
 * 	Object o = type.getAdapter(IChangeGroupingRequestor.class);
 * 	if (o instanceof IChangeGroupingRequestor) {
 * 		return (IChangeGroupingRequestor) o;
 *	}
 * </pre>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see ChangeTracker
 * @since 3.3
 */
public interface IChangeGroupingRequestor {

	/**
	 * Issue a request to group the provided files together when the changes
	 * are committed or checked-in to the repository.
	 * @param project the project that contains the files
	 * @param files the files
	 * @param nameHint a name hint for the resulting set
	 * @throws CoreException if an error occurs
	 */
	public void ensureChangesGrouped(IProject project, IFile[] files, String nameHint) throws CoreException;

	/**
	 * Return whether the given file is modified with respect to the repository.
	 * In other words, return whether the file contains changes that need to be committed
	 * or checked-in to the repository.
	 * @param file the file
	 * @return whether the given file is modified with respect to the repository
	 * @throws CoreException if an error occurs while trying to determine the modification state
	 * of the file
	 */
	public boolean isModified(IFile file) throws CoreException;

}
