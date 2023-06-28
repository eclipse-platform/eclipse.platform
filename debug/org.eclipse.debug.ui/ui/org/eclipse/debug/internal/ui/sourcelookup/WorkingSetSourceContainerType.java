/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
import org.eclipse.debug.ui.sourcelookup.WorkingSetSourceContainer;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The factory for creating/restoring working set source containers.
 *
 * @since 3.0
 */
public class WorkingSetSourceContainerType extends AbstractSourceContainerTypeDelegate {

	@Override
	public String getMemento(ISourceContainer container) throws CoreException {
		WorkingSetSourceContainer workingSet = (WorkingSetSourceContainer) container;
		Document doc = newDocument();
		Element node = doc.createElement("workingSet"); //$NON-NLS-1$
		node.setAttribute("name", workingSet.getName()); //$NON-NLS-1$
		doc.appendChild(node);
		return serializeDocument(doc);
	}

	@Override
	public ISourceContainer createSourceContainer(String memento)throws CoreException {
		IWorkingSet workingSet = null;
		Exception ex = null;
		try {
			Element root = null;
			DocumentBuilder parser =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
			StringReader reader = new StringReader(memento);
			InputSource source = new InputSource(reader);
			root = parser.parse(source).getDocumentElement();

			String name = root.getAttribute("name");//$NON-NLS-1$

			if (isEmpty(name)) {
				abort(SourceLookupUIMessages.sourceSearch_initError,null);
			}
			workingSet = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
			//check that set still exists
			if (workingSet == null) {
				abort(SourceLookupUIMessages.sourceSearch_initError, null);
			}
			return new WorkingSetSourceContainer(workingSet);

		} catch (ParserConfigurationException e) {
			ex = e;
		} catch (SAXException e) {
			ex = e;
		} catch (IOException e) {
			ex = e;
		}

		abort(SourceLookupUIMessages.sourceSearch_initError, ex);
		return null;
	}

	private boolean isEmpty(String string) {
		return string == null || string.length() == 0;
	}


}
