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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A folder in the workspace.
 *
 * @since 3.0
 */
public class FolderSourceContainerType extends AbstractSourceContainerTypeDelegate {

	@Override
	public String getMemento(ISourceContainer container) throws CoreException {
		FolderSourceContainer folderSourceContainer = (FolderSourceContainer)container;
		Document document = newDocument();
		Element element = document.createElement("folder"); //$NON-NLS-1$
		element.setAttribute("path", folderSourceContainer.getContainer().getFullPath().toString()); //$NON-NLS-1$
		String nest = "false"; //$NON-NLS-1$
		if (folderSourceContainer.isComposite()) {
			nest = "true"; //$NON-NLS-1$
		}
		element.setAttribute("nest", nest);  //$NON-NLS-1$
		document.appendChild(element);
		return serializeDocument(document);
	}

	@Override
	public ISourceContainer createSourceContainer(String memento) throws CoreException {
		Node node = parseDocument(memento);
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			if ("folder".equals(element.getNodeName())) { //$NON-NLS-1$
				String string = element.getAttribute("path"); //$NON-NLS-1$
				if (string == null || string.length() == 0) {
					abort(SourceLookupMessages.FolderSourceContainerType_10, null);
				}
				String nest = element.getAttribute("nest"); //$NON-NLS-1$
				boolean nested = "true".equals(nest); //$NON-NLS-1$
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IFolder folder = workspace.getRoot().getFolder(IPath.fromOSString(string));
				return new FolderSourceContainer(folder, nested);
			}
			abort(SourceLookupMessages.FolderSourceContainerType_11, null);
		}
		abort(SourceLookupMessages.FolderSourceContainerType_12, null);
		return null;
	}
}
