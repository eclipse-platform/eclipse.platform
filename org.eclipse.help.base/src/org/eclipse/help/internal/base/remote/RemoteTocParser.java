/***************************************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 **************************************************************************************************/
package org.eclipse.help.internal.base.remote;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.ITocContribution;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.internal.toc.TocContribution;
import org.xml.sax.SAXException;

public class RemoteTocParser {

	private DocumentReader reader;


	public ITocContribution[] parse(InputStream in, String urlStr) throws ParserConfigurationException,
			SAXException, IOException {
		if (reader == null) {
			reader = new DocumentReader();
		}
		UAElement root = reader.read(in);
		IUAElement[] children = root.getChildren();
		ITocContribution[] contributions = new ITocContribution[children.length];
		for (int i = 0; i < children.length; ++i) {
			UAElement child = (UAElement) children[i];
			IUAElement[] contribChildren = child.getChildren();
			String[] extraDocuments = new String[contribChildren.length - 1];
			for (int j = 0; j < extraDocuments.length; ++j) {
				extraDocuments[j] = ((UAElement) contribChildren[j + 1]).getAttribute("href"); //$NON-NLS-1$
			}
			TocContribution contribution = new TocContribution();
			contribution.setCategoryId(child.getAttribute("categoryId")); //$NON-NLS-1$

			String contributorID = child.getAttribute("contributorId"); //$NON-NLS-1$
			contribution.setContributorId(contributorID);

			// Link the contributor ID to a particular URL
			RemoteContentLocator.addContentPage(contributorID, urlStr);
			contribution.setExtraDocuments(extraDocuments);
			contribution.setId(child.getAttribute("id")); //$NON-NLS-1$
			contribution.setLocale(child.getAttribute("locale")); //$NON-NLS-1$
			contribution.setPrimary("true".equals(child.getAttribute("isPrimary"))); //$NON-NLS-1$//$NON-NLS-2$
			contribution.setToc((Toc) contribChildren[0]);
			contributions[i] = contribution;
		}
		return contributions;
	}
}
