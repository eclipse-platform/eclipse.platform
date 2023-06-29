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

/**
 * A source lookup participant participates in source lookup by searching an ordered
 * list of source containers for source elements corresponding to a debug artifact.
 * For example, a participant may be asked to find source corresponding to a stack
 * frame or breakpoint. An implementation of a source lookup participant is debug
 * model specific, since it must convert the debug model artifact (stack frame,
 * breakpoint, etc.),  into a source name that can be recognized by a source container
 * (<code>ISourceContainer</code>), to search for source elements. Source containers
 * are generally debug model independent, whereas source lookup participants are
 * debug model specific.
 * <p>
 * Clients may implement this interface. An abstract implementation is
 * provided by <code>AbstractSourceLookupParticipant</code>, which clients
 * should subclass.
 * </p>
 * @since 3.0
 */
public interface ISourceLookupParticipant {

	/**
	 * Notification this participant has been added to the specified
	 * source lookup director.
	 *
	 * @param director the source lookup director that this participant
	 *  has been added to
	 */
	void init(ISourceLookupDirector director);

	/**
	 * Returns a collection of source elements corresponding to the given debug
	 * artifact (for example, a stack frame or breakpoint). Returns an empty
	 * collection if no source elements are found.
	 * This participant's source lookup director specifies if duplicate
	 * source elements should be searched for, via <code>isFindDuplicates()</code>.
	 * When <code>false</code> the returned collection should contain at most one
	 * source element.
	 * <p>
	 * If the given debug artifact is not recognized by this participant, an empty
	 * collection is returned. Otherwise, this participant generates a source name
	 * from the given artifact and performs a search for associated source elements
	 * in its source containers.
	 * </p>
	 * @param object the debug artifact for which source needs to be found (e.g., stack frame)
	 * @return a collection of source elements corresponding to the given
	 *  debug artifact, possibly empty
	 * @exception CoreException if an exception occurs while searching for source
	 */
	Object[] findSourceElements(Object object) throws CoreException;

	/**
	 * Returns the source file name associated with the given debug artifact that
	 * source needs to be found for, or <code>null</code> if none.
	 *
	 * @param object the debug artifact for which source needs to be found (e.g., stack frame)
	 * @return the source file name associated with the given debug artifact,
	 *  or <code>null</code> if none.
	 * @throws CoreException if unable to determine a source file name
	 */
	String getSourceName(Object object) throws CoreException;

	/**
	 * Disposes this source lookup participant. This method is called when
	 * the source lookup director associated with this participant is
	 * disposed.
	 */
	void dispose();

	/**
	 * Notification that the source lookup containers in the given source
	 * lookup director have changed.
	 *
	 * @param director source lookup director that is directing this
	 * participant
	 */
	void sourceContainersChanged(ISourceLookupDirector director);
}
