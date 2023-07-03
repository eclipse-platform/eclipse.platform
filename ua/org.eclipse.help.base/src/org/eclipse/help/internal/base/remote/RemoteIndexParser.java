/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.base.remote;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.index.Index;
import org.eclipse.help.internal.index.IndexContribution;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * Converts indexes serialized by the IndexServlet on remote help server back
 * into model objects. The XML is similar to index XML files but not identical
 * (it has all indexes in one, has indexContribution elements, etc.
 */
public class RemoteIndexParser extends DefaultHandler {

	private DocumentReader reader;

	/*
	 * Parses the given serialized indexes and returns generated model objects.
	 */
	public IndexContribution[] parse(InputStream in) throws ParserConfigurationException, SAXException, IOException {
		if (reader == null) {
			reader = new DocumentReader();
		}
		UAElement root = reader.read(in);
		IUAElement[] children = root.getChildren();
		IndexContribution[] contributions = new IndexContribution[children.length];
		for (int i=0;i<children.length;++i) {
			UAElement child = (UAElement)children[i];
			IndexContribution contribution = new IndexContribution();
			contribution.setId(child.getAttribute("id")); //$NON-NLS-1$
			contribution.setLocale(child.getAttribute("locale")); //$NON-NLS-1$
			contribution.setIndex((Index)child.getChildren()[0]);
			contributions[i] = contribution;
		}
		return contributions;
	}
}
