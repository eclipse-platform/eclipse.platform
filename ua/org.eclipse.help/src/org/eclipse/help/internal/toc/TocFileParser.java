/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.internal.toc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.internal.dynamic.DocumentReader;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TocFileParser extends DefaultHandler {

	private DocumentReader reader;

	public TocContribution parse(TocFile tocFile) throws IOException, SAXException, ParserConfigurationException {
		if (reader == null) {
			reader = new DocumentReader();
		}
		try (InputStream in = tocFile.getInputStream()) {
			if (in != null) {
				Toc toc = (Toc) reader.read(in);
				TocContribution contribution = new TocContribution();
				contribution.setCategoryId(tocFile.getCategory());
				contribution.setContributorId(tocFile.getPluginId());
				contribution.setExtraDocuments(DocumentFinder.collectExtraDocuments(tocFile));
				contribution.setId(HrefUtil.normalizeHref(tocFile.getPluginId(), tocFile.getFile()));
				contribution.setLocale(tocFile.getLocale());
				contribution.setToc(toc);
				contribution.setPrimary(tocFile.isPrimary());
				return contribution;
			} else {
				throw new FileNotFoundException();
			}
		}
	}
}
