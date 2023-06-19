/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.search;

import java.io.InputStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.xhtml.DynamicXHTMLProcessor;
import org.eclipse.help.search.SearchParticipantXML;
import org.xml.sax.Attributes;

/**
 * The search participant responsible for indexing XHTML documents.
 */
public class XHTMLSearchParticipant extends SearchParticipantXML {

	private static final String KEYWORDS = "keywords"; //$NON-NLS-1$
	private static final String META_TAG = "meta"; //$NON-NLS-1$
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
	private static final String CONTENT_ATTRIBUTE = "content"; //$NON-NLS-1$
	private String title;
	private String summary;
	private boolean hasDescriptionMetaTag = false;

	@Override
	protected void handleEndElement(String name, IParsedXMLContent data) {
	}

	@Override
	protected void handleStartElement(String name, Attributes attributes, IParsedXMLContent data) {
		title = null;
		if (META_TAG.equalsIgnoreCase(name)) {
			String nameAttribute = attributes.getValue(NAME_ATTRIBUTE);
			if (DESCRIPTION.equalsIgnoreCase(nameAttribute)) {
				String descriptionAttribute = attributes.getValue(CONTENT_ATTRIBUTE);
				if (descriptionAttribute != null) {
					hasDescriptionMetaTag = true;
					data.addToSummary(descriptionAttribute);
					data.addText(" "); //$NON-NLS-1$
					data.addText(descriptionAttribute);
					data.addText(" "); //$NON-NLS-1$
				}
			} else if (KEYWORDS.equalsIgnoreCase(nameAttribute)) {
				String keywordsAttribute = attributes.getValue(CONTENT_ATTRIBUTE);
				if (keywordsAttribute != null) {
					data.addText(" "); //$NON-NLS-1$
					data.addText(keywordsAttribute);
					data.addText(" "); //$NON-NLS-1$
				}
			}
		}
	}

	@Override
	protected void handleStartDocument(IParsedXMLContent data) {
		hasDescriptionMetaTag = false;
	}

	@Override
	protected void handleText(String text, IParsedXMLContent data) {
		String stackPath = getElementStackPath();
		IPath path = IPath.fromOSString(stackPath);
		if (path.segment(1).equalsIgnoreCase("body") &&  //$NON-NLS-1$
			!isSkipped(path.segment(path.segmentCount() -1))) {
			data.addText(text);
			if (!hasDescriptionMetaTag) {
				data.addToSummary(text);
			}
		} else if (path.segment(1).equalsIgnoreCase("head")) { //$NON-NLS-1$
			if (path.segment(path.segmentCount() -1).equalsIgnoreCase("title")) { //$NON-NLS-1$
				if (title == null) {
					title = text;
				} else {
					title = title + text;
				}
				data.setTitle(title);
			}
		}
	}

	private boolean isSkipped(String tag) {
		return tag.equals("script"); //$NON-NLS-1$
	}

	@Override
	protected InputStream preprocess(InputStream in, String name, String locale) {
		try {
			return DynamicXHTMLProcessor.process(name, in, locale, false);
		}
		catch (Throwable t) {
			String msg = "An error occured while pre-processing help XHTML document \"" + name + "\" for search indexing"; //$NON-NLS-1$ //$NON-NLS-2$
			Platform.getLog(getClass()).error(msg, t);
			return in;
		}
	}

	public String getSummary() {
		return summary;
	}
}