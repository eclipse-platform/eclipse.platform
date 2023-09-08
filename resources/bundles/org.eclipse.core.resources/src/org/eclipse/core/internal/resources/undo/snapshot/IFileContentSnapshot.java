/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - Adapted from classes in org.eclipse.ui.ide.undo and org.eclipse.ui.internal.ide.undo
 *******************************************************************************/
package org.eclipse.core.internal.resources.undo.snapshot;

import java.io.InputStream;
import org.eclipse.core.runtime.CoreException;

/**
 * IFileContentSnapshot is a description of a file's content.
 *
 * This class is not intended to be instantiated or used by clients.
 *
 * @since 3.20
 *
 */
public interface IFileContentSnapshot {
	/**
	 * Returns an open input stream on the contents of the file described. The
	 * client is responsible for closing the stream when finished.
	 *
	 * @return an input stream containing the contents of the file
	 * @throws CoreException
	 *             any CoreException encountered retrieving the contents
	 */
	InputStream getContents() throws CoreException;

	/**
	 * Returns whether this file content description still exists. If it does
	 * not exist, it will be unable to produce the contents.
	 *
	 * @return <code>true</code> if this description exists, and
	 *         <code>false</code> if it does not
	 */
	boolean exists();

	/**
	 * Returns the name of a charset encoding to be used when decoding the
	 * contents into characters. Returns <code>null</code> if a charset
	 * has not been explicitly specified.
	 *
	 * @return the name of a charset, or <code>null</code>
	 * @throws CoreException
	 *             any CoreException encountered while determining the character
	 *             set
	 *
	 */
	String getCharset() throws CoreException;
}
