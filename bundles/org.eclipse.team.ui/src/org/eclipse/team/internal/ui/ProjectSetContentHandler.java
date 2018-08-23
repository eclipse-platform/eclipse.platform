/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ProjectSetContentHandler extends DefaultHandler {
	boolean inPsf = false;
	boolean inProvider = false;
	boolean inProject = false;
	Map<String, List<String>> map;
	String id;
	List<String> references;
	boolean isVersionOne = false;

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		String elementName = getElementName(namespaceURI, localName, qName);
		if (elementName.equals("psf")) { //$NON-NLS-1$
			map = new HashMap<>();
			inPsf = true;
			String version = atts.getValue("version"); //$NON-NLS-1$
			isVersionOne = version.equals("1.0"); //$NON-NLS-1$
			return;
		}
		if (isVersionOne) return;
		if (elementName.equals("provider")) { //$NON-NLS-1$
			if (!inPsf) throw new SAXException(TeamUIMessages.ProjectSetContentHandler_Element_provider_must_be_contained_in_element_psf_4);
			inProvider = true;
			id = atts.getValue("id"); //$NON-NLS-1$
			references = new ArrayList<>();
			return;
		}
		if (elementName.equals("project")) { //$NON-NLS-1$
			if (!inProvider) throw new SAXException(TeamUIMessages.ProjectSetContentHandler_Element_project_must_be_contained_in_element_provider_7);
			inProject = true;
			String reference = atts.getValue("reference"); //$NON-NLS-1$
			references.add(reference);
			return;
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		String elementName = getElementName(namespaceURI, localName, qName);
		if (elementName.equals("psf")) { //$NON-NLS-1$
			inPsf = false;
			return;
		}
		if (isVersionOne) return;
		if (elementName.equals("provider")) { //$NON-NLS-1$
			map.put(id, references);
			references = null;
			inProvider = false;
			return;
		}
		if (elementName.equals("project")) { //$NON-NLS-1$
			inProject = false;
			return;
		}
	}

	public Map<String, List<String>> getReferences() {
		return map;
	}

	public boolean isVersionOne() {
		return isVersionOne;
	}

	/*
	 * Couldn't figure out from the SAX API exactly when localName vs. qName is used.
	 * However, the XML for project sets doesn't use namespaces so either of the two names
	 * is fine. Therefore, use whichever one is provided.
	 */
	private String getElementName(String namespaceURI, String localName, String qName) {
		if (localName != null && localName.length() > 0) {
			return localName;
		} else {
			return qName;
		}
	}
}
