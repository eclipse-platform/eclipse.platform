/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.runtime.QualifiedName;

/**
 * Content describers know how to retrieve metadata from
 * contents.
 * <p>
 * Note: It is expected that content describer implementations be declared in a package
 * that is exempt from plug-in activation (using the Eclipse-AutoStart bundle
 * manifest header). Since all describers are instantiated when the content type
 * framework is initialized, failure in complying with this requirement causes
 * premature activation, which must be avoided. Future implementations of the
 * framework might refuse to instantiate describers if doing so would trigger
 * activation of the corresponding plug-in.
 * </p>
 * <p>
 * Describers for text-based content types should implement
 * <code>ITextContentDescriber</code> instead.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>

 * @see IContentDescription
 * @since 3.0
 */
public interface IContentDescriber {
	/**
	 * Description result constant, indicating that it was not possible
	 * to determine whether the contents were valid for
	 * the intended content type.
	 *
	 * @see #describe
	 */
	int INDETERMINATE = 1;
	/**
	 * Description result constant, indicating the contents are invalid for
	 * the intended content type.
	 *
	 * @see #describe
	 */
	int INVALID = 0;
	/**
	 * Description result constant, indicating the contents are valid for
	 * the intended content type.
	 *
	 * @see #describe
	 */
	int VALID = 2;

	/**
	 * Tries to fill a description for the given contents. Returns an
	 * <code>int</code> indicating whether the given stream of bytes represents a
	 * valid sample for its corresponding content type. If no content description is
	 * provided, this method should perform content type validation.
	 * <p>
	 * The input stream must be kept open, and any IOExceptions while reading the
	 * stream should flow to the caller.
	 * </p>
	 *
	 * @param contents    the contents to be examined
	 * @param description a description to be filled in, or <code>null</code> if
	 *                    only content type validation is to be performed
	 * @return one of the following:
	 *         <ul>
	 *         <li><code>VALID</code></li>
	 *         <li><code>INVALID</code></li>
	 *         <li><code>INDETERMINATE</code></li>
	 *         </ul>
	 * @throws IOException if an I/O error occurs
	 * @see IContentDescription
	 * @see #VALID
	 * @see #INVALID
	 * @see #INDETERMINATE
	 */
	int describe(InputStream contents, IContentDescription description) throws IOException;

	/**
	 * Returns the properties supported by this describer.
	 *
	 * @return the supported properties
	 * @see #describe
	 */
	QualifiedName[] getSupportedOptions();
}
