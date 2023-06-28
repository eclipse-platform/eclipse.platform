/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core.sourcelookup.containers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * External archive source container type.
 *
 * @since 3.0
 */
public class ExternalArchiveSourceContainerType extends AbstractSourceContainerTypeDelegate {

	@Override
	public ISourceContainer createSourceContainer(String memento) throws CoreException {
		Node node = parseDocument(memento);
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			if ("archive".equals(element.getNodeName())) { //$NON-NLS-1$
				String string = element.getAttribute("path"); //$NON-NLS-1$
				if (string == null || string.length() == 0) {
					abort(SourceLookupMessages.ExternalArchiveSourceContainerType_10, null);
				}
				String detect = element.getAttribute("detectRoot"); //$NON-NLS-1$
				boolean auto = "true".equals(detect); //$NON-NLS-1$
				return new ExternalArchiveSourceContainer(string, auto);
			}
			abort(SourceLookupMessages.ExternalArchiveSourceContainerType_11, null);
		}
		abort(SourceLookupMessages.ExternalArchiveSourceContainerType_12, null);
		return null;
	}

	@Override
	public String getMemento(ISourceContainer container) throws CoreException {
		ExternalArchiveSourceContainer archive = (ExternalArchiveSourceContainer) container;
		Document document = newDocument();
		Element element = document.createElement("archive"); //$NON-NLS-1$
		element.setAttribute("path", archive.getName()); //$NON-NLS-1$
		String detectRoot = "false"; //$NON-NLS-1$
		if (archive.isDetectRoot()) {
			detectRoot = "true"; //$NON-NLS-1$
		}
		element.setAttribute("detectRoot", detectRoot);  //$NON-NLS-1$
		document.appendChild(element);
		return serializeDocument(document);
	}
}
