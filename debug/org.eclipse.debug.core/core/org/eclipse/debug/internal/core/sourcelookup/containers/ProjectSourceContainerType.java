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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The type for creating/restoring a project source container.
 *
 * @since 3.0
 */
public class ProjectSourceContainerType extends AbstractSourceContainerTypeDelegate {

	@Override
	public String getMemento(ISourceContainer container) throws CoreException {
		ProjectSourceContainer project = (ProjectSourceContainer) container;
		Document document = newDocument();
		Element element = document.createElement("project"); //$NON-NLS-1$
		element.setAttribute("name", project.getContainer().getName()); //$NON-NLS-1$
		String referenced = "false"; //$NON-NLS-1$
		if (project.isSearchReferencedProjects()) {
			referenced = "true"; //$NON-NLS-1$
		}
		element.setAttribute("referencedProjects", referenced);  //$NON-NLS-1$
		document.appendChild(element);
		return serializeDocument(document);
	}

	@Override
	public ISourceContainer createSourceContainer(String memento) throws CoreException {
		Node node = parseDocument(memento);
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element)node;
			if ("project".equals(element.getNodeName())) { //$NON-NLS-1$
				String string = element.getAttribute("name"); //$NON-NLS-1$
				if (string == null || string.length() == 0) {
					abort(SourceLookupMessages.ProjectSourceContainerType_10, null);
				}
				String nest = element.getAttribute("referencedProjects"); //$NON-NLS-1$
				boolean ref = "true".equals(nest); //$NON-NLS-1$
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IProject project = workspace.getRoot().getProject(string);
				return new ProjectSourceContainer(project, ref);
			}
			abort(SourceLookupMessages.ProjectSourceContainerType_11, null);
		}
		abort(SourceLookupMessages.ProjectSourceContainerType_12, null);
		return null;
	}
}
