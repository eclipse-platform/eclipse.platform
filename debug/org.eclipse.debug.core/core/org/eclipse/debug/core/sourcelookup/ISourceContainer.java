/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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
package org.eclipse.debug.core.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * A source container is a container of source code. A source container is
 * capable of searching for source elements by name. For example, a source
 * container may be a project or a directory capable of searching for files
 * by name. A source container may be a composite container - i.e. contain
 * other source containers.
 * <p>
 * When a source container is created and added to a source director, the
 * source container's <code>dispose()</code> method is called when the
 * source director is disposed. Clients creating source containers for other
 * purposes must dispose of containers themselves.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see ISourceLookupParticipant
 * @see ISourceContainerType
 * @since 3.0
 */
public interface ISourceContainer extends IAdaptable {

	/**
	 * Notification this source container has been added to the given
	 * source lookup director.
	 *
	 * @param director the director this container has been added to
	 */
	void init(ISourceLookupDirector director);

	/**
	 * Returns a collection of source elements in this container corresponding to the
	 * given name. Returns an empty collection if no source elements are found.
	 * This source container's source lookup director specifies if duplicate
	 * source elements should be searched for, via <code>isFindDuplicates()</code>.
	 * When <code>false</code> the returned collection should contain at most one
	 * source element. If this is a composite container, the containers contained
	 * by this container are also searched.
	 * <p>
	 * The format of the given name is implementation specific but generally conforms
	 * to the format of a file name. If a source container does not recognize the
	 * name format provided, an empty collection should be returned. A source container
	 * may or may not require names to be fully qualified (i.e. be qualified with directory
	 * names).
	 * </p>
	 * @param name the name of the source element to search for
	 * @return a collection of source elements corresponding to the given name
	 * @exception CoreException if an exception occurs while searching for source elements
	 */
	Object[] findSourceElements(String name) throws CoreException;

	/**
	 * The name of this source container that can be used for presentation purposes.
	 * For example, the name of a project.
	 *
	 * @return the name of this source container
	 */
	String getName();

	/**
	 * Returns the source containers this container is composed of. An empty
	 * collection is returned if this container is not a composite container.
	 * For example, a workspace source container may be composed of project source
	 * containers.
	 *
	 * @return the source containers this container is composed of, possibly
	 *  an empty collection
	 * @exception CoreException if unable to retrieve source containers
	 */
	ISourceContainer[] getSourceContainers() throws CoreException;

	/**
	 * Returns whether this container is a composite container. A composite
	 * container is composed of other source containers. For example, a workspace
	 * source container may be composed of project source containers.
	 *
	 * @return whether this container is a composite container
	 */
	boolean isComposite();

	/**
	 * Returns this container's type.
	 *
	 * @return this container's type
	 */
	ISourceContainerType getType();

	/**
	 * Disposes this source container. This method is called when the source
	 * director associated with this source container is disposed.
	 */
	void dispose();

}
