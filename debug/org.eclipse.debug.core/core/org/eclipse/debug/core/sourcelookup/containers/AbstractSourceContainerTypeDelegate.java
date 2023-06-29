/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.debug.core.sourcelookup.containers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainerTypeDelegate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Common function for source container type delegates.
 * <p>
 * Clients implementing source container delegates should subclass this class.
 * </p>
 * @since 3.0
 */
public abstract class AbstractSourceContainerTypeDelegate implements ISourceContainerTypeDelegate {

	/**
	 * Throws an exception with the given message and underlying exception.
	 *
	 * @param message error message
	 * @param exception underlying exception, or <code>null</code>
	 * @throws CoreException if a problem is encountered
	 */
	protected void abort(String message, Throwable exception) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, message, exception);
		throw new CoreException(status);
	}

	/**
	 * Creates and returns a new XML document.
	 *
	 * @return a new XML document
	 * @throws CoreException if unable to create a new document
	 */
	protected Document newDocument()throws CoreException {
		return DebugPlugin.newDocument();
	}

	/**
	 * Returns the given XML document as a string.
	 *
	 * @param document document to serialize
	 * @return the given XML document as a string
	 * @throws CoreException if unable to serialize the document
	 */
	protected String serializeDocument(Document document) throws CoreException {
		return DebugPlugin.serializeDocument(document);
	}

	/**
	 * Parses the given XML document, returning its root element.
	 *
	 * @param document XML document as a string
	 * @return the document's root element
	 * @throws CoreException if unable to parse the document
	 */
	protected Element parseDocument(String document) throws CoreException {
		return DebugPlugin.parseDocument(document);
	}
}
