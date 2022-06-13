/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.core.runtime.content;

import java.io.*;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * An object that performs content type matching queries.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see IContentTypeManager#getMatcher(IContentTypeManager.ISelectionPolicy, IScopeContext)
 * @since 3.1
 */
public interface IContentTypeMatcher {
	/**
	 * Returns the preferred content type for the given contents and file name.
	 * <p>
	 * Returns <code>null</code> if no associated content types are
	 * found.
	 * </p>
	 * <p>
	 * If a file name is not provided, the entire content type registry will be
	 * queried. For performance reasons, it is highly recommended
	 * to provide a file name if available.
	 * </p>
	 * <p>
	 * Any IOExceptions that may occur while reading the given input stream
	 * will flow to the caller. The input stream will not be closed by this
	 * operation.
	 * </p>
	 *
	 * @param contents an input stream
	 * @param fileName the file name associated to the contents, or <code>null</code>
	 * @return the preferred content type associated to the given file name, or <code>null</code>
	 * @throws IOException if an error occurs while reading the contents
	 */
	IContentType findContentTypeFor(InputStream contents, String fileName) throws IOException;

	/**
	 * Returns the preferred content type for the given file name. If multiple content types
	 * are associated with the given file name, the one considered the most appropriated will
	 * be returned. If there are no content types associated, <code>null</code> is returned.
	 *
	 * @param fileName the name of the file
	 * @return the preferred content type associated to the given file name, or <code>null</code>
	 */
	IContentType findContentTypeFor(String fileName);

	/**
	 * Returns the content types associated to the given contents and file name.
	 * <p>
	 * Returns an empty array if no associated content types are found.
	 * </p>
	 * <p>
	 * If a file name is not provided, the entire content type registry will be
	 * queried. For performance reasons, it is highly recommended
	 * to provide a file name if available.
	 * </p>
	 * <p>
	 * Any IOExceptions that may occur while reading the given input stream
	 * will flow to the caller.  The input stream will not be closed by this
	 * operation.
	 * </p>
	 *
	 * @param contents an input stream
	 * @param fileName the file name associated to the contents, or <code>null</code>
	 * @return all content types associated to the given contents and file name
	 * @throws IOException if an error occurs while reading the contents
	 */
	IContentType[] findContentTypesFor(InputStream contents, String fileName) throws IOException;

	/**
	 * Returns all content types known by the platform that are associated to the given file name.
	 * <p>
	 * Returns an empty array if there are no content types associated.
	 * </p>
	 *
	 * @param fileName the name of the file
	 * @return all content types associated to the given file spec
	 */
	IContentType[] findContentTypesFor(String fileName);

	/**
	 * Tries to obtain a description for the given contents and file name.
	 * <p>
	 * Any IOExceptions that may occur while reading the given input stream
	 * will flow to the caller.  The input stream will not be closed by this
	 * operation.
	 * </p>
	 * <p>
	 * If a file name is not provided, the entire content type registry will be
	 * queried. For performance reasons, it is highly recommended
	 * to provide a file name if available.
	 * </p>
	 *
	 * @param contents the contents to be interpreted
	 * @param fileName the file name associated to the contents, or <code>null</code>
	 * @param options an array of keys for all properties that should be
	 * described, or <code>IContentDescription.ALL</code>,  for all of them
	 * @return a content description if one could be obtained, or <code>null</code>
	 * @throws IOException if an error occurs while reading the contents
	 * @see IContentDescription
	 */
	IContentDescription getDescriptionFor(InputStream contents, String fileName, QualifiedName[] options) throws IOException;

	/**
	 * Tries to obtain a description for the given contents and file name.
	 * <p>
	 * Any IOExceptions that may occur while reading the given input stream
	 * will flow to the caller.  The reader will not be closed by this
	 * operation.
	 * </p>
	 * <p>
	 * If a file name is not provided, the entire content type registry will be
	 * queried. For performance reasons, it is highly recommended
	 * to provide a file name if available.
	 * </p>
	 *
	 * @param contents the contents to be interpreted
	 * @param fileName the file name associated to the contents, or <code>null</code>
	 * @param options an array of keys for all properties that should be
	 * described, or <code>IContentDescription.ALL</code>,  for all of them
	 * @return a content description if one could be obtained, or <code>null</code>
	 * @throws IOException if an error occurs while reading the contents
	 * @see IContentDescription
	 */
	IContentDescription getDescriptionFor(Reader contents, String fileName, QualifiedName[] options) throws IOException;
}