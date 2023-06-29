/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The type for creating/restoring workspace source containers.
 *
 * @since 3.0
 */
public class WorkspaceSourceContainerType extends AbstractSourceContainerTypeDelegate {

	@Override
	public ISourceContainer createSourceContainer(String memento) throws CoreException {
		Node node = parseDocument(memento);
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			if ("workspace".equals(element.getNodeName())) { //$NON-NLS-1$
				return new WorkspaceSourceContainer();
			}
			abort(SourceLookupMessages.WorkspaceSourceContainerType_3, null);
		}
		abort(SourceLookupMessages.WorkspaceSourceContainerType_4, null);
		return null;
	}

	@Override
	public String getMemento(ISourceContainer container) throws CoreException {
		Document document = newDocument();
		Element element = document.createElement("workspace"); //$NON-NLS-1$
		document.appendChild(element);
		return serializeDocument(document);
	}
}
