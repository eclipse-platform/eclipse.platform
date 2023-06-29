/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * A source lookup director directs the source lookup process
 * among a set of participants and source containers.
 * <p>
 * Clients may implement this interface. An abstract implementation
 * is provided by <code>AbstractSourceLookupDirector</code>, which
 * clients should subclass.
 * </p>
 * @since 3.0
 */
public interface ISourceLookupDirector extends IPersistableSourceLocator2 {

	/**
	 * Returns the launch configuration associated with this source
	 * lookup director, or <code>null</code> if none.
	 *
	 * @return the launch configuration associated with this source
	 * lookup director, or <code>null</code> if none
	 */
	ILaunchConfiguration getLaunchConfiguration();

	/**
	 * Returns the source lookup participants currently registered with
	 * this director, possibly an empty collection.
	 *
	 * @return the source lookup participants currently registered with
	 * this director, possibly an empty collection
	 */
	ISourceLookupParticipant[] getParticipants();

	/**
	 * Returns the source containers currently registered with this
	 * director, possibly an empty collection.
	 *
	 * @return the source containers currently registered with this
	 * director, possibly an empty collection
	 */
	ISourceContainer[] getSourceContainers();

	/**
	 * Sets the source containers this source lookup director
	 * should search when looking for source, possibly an empty collection.
	 *
	 * @param containers the source containers this source lookup director
	 * should search when looking for source, possibly an empty collection
	 */
	void setSourceContainers(ISourceContainer[] containers);

	/**
	 * Returns whether to search exhaustively for all source elements
	 * with the same name in all registered source containers, or
	 * whether to stop searching when the first source element matching
	 * the required name is found.
	 *
	 * @return whether to search exhaustively for all source elements
	 * with the same name
	 */
	boolean isFindDuplicates();

	/**
	 * Sets whether to search exhaustively for all source elements
	 * with the same name in all registered source containers, or
	 * whether to stop searching when the first source element matching
	 * the required name is found.
	 *
	 * @param findDuplicates whether to search exhaustively for all source elements
	 * with the same name
	 */
	void setFindDuplicates(boolean findDuplicates);

	/**
	 * Notifies this source lookup director that it should initialize
	 * its set of source lookup participants.
	 */
	void initializeParticipants();

	/**
	 * Returns whether this source director supports the given type
	 * of source location.
	 *
	 * @param type source container type
	 * @return whether this source director supports the given type
	 * of source location
	 */
	boolean supportsSourceContainerType(ISourceContainerType type);

	/**
	 * Clears any source lookup results associated with the given
	 * debug artifact, such that a subsequent lookup will force a new search
	 * to be performed.
	 *
	 * @param element debug artifact to clear source lookup results for
	 */
	void clearSourceElements(Object element);

	/**
	 * Adds the given source lookup participants to this director.
	 *
	 * @param participants participants to add
	 */
	void addParticipants(ISourceLookupParticipant[] participants);

	/**
	 * Removes the given source lookup participants from this director.
	 *
	 * @param participants participants to remove
	 */
	void removeParticipants(ISourceLookupParticipant[] participants);

	/**
	 * Returns the identifier of this type of source locator.
	 *
	 * @return the identifier of this type of source locator
	 */
	String getId();

	/**
	 * Returns the source path computer to use with this source lookup
	 * director, possibly <code>null</code>. By default, the source path
	 * computer returned is the one associated with this director's launch
	 * configuration's type. However, the source path computer can be specified
	 * programmatically by calling <code>setSourcePathComputer(...)</code>.
	 *
	 * @return the source path computer to use with this source lookup
	 *  director, possibly <code>null</code>
	 */
	ISourcePathComputer getSourcePathComputer();

	/**
	 * Sets the source path computer for this source lookup director.
	 * This method can be used to override the default source path computer
	 * for a launch configuration type. When <code>null</code> is specified
	 * the default source path computer will be used (i.e. the one associated
	 * with this director's launch configuration's type).
	 *
	 * @param computer source path computer or <code>null</code>
	 */
	void setSourcePathComputer(ISourcePathComputer computer);

	/**
	 * Returns a collection of source elements corresponding to the given debug
	 * artifact (for example, a stack frame or breakpoint). Returns an empty
	 * collection if no source elements are found.
	 * This participant's source lookup director specifies if duplicate
	 * source elements should be searched for, via <code>isFindDuplicates()</code>.
	 * When <code>false</code> the returned collection should contain at most one
	 * source element.
	 *
	 * @param object the debug artifact for which source needs to be found (e.g., stack frame)
	 * @return a collection of source elements corresponding to the given
	 *  debug artifact, possibly empty
	 * @exception CoreException if an exception occurs while searching for source
	 */
	Object[] findSourceElements(Object object) throws CoreException;

	/**
	 * Returns a source element that corresponds to the given debug artifact, or
	 * <code>null</code> if a source element could not be located. This is a
	 * generalization of <code>getSourceElement(IStackFrame)</code> to allow
	 * source to be found for other types of elements.
	 *
	 * @param element the debug artifact for which to locate source
	 * @return an object representing a source element.
	 */
	 Object getSourceElement(Object element);

	/**
	 * Answers if the objects should be considered as same source elements (e.g.
	 * resulting in the physically same storage element), independently if in
	 * the modelled world they may be different views on same data.
	 *
	 * @param element1 first source element
	 * @param element2 second source element
	 * @return Returns {@code true} if two source elements represent physically
	 *         same data. Default implementation just delegates to
	 *         {@link Objects#equals(Object, Object)}.
	 * @since 3.17
	 */
	default boolean equalSourceElements(Object element1, Object element2) {
		return Objects.equals(element1, element2);
	}

}
