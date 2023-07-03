/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model.loader;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.ui.internal.intro.impl.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 *
 */
public class IntroContentParser {

	private static String TAG_INTRO_CONTENT = "introContent"; //$NON-NLS-1$
	private static String TAG_HTML = "html"; //$NON-NLS-1$

	private Document document;
	private boolean hasXHTMLContent;

	/**
	 * Creates a config parser assuming that the passed content represents a URL
	 * to the content file.
	 */
	public IntroContentParser(String content) {
		try {
			document = parse(content);
			if (document != null) {
				// xml file is loaded. It can be either XHTML or intro XML.
				Element rootElement = document.getDocumentElement();
				// DocumentType docType = document.getDoctype();
				if (rootElement.getTagName().equals(TAG_INTRO_CONTENT)) {
					// intro xml file.
					hasXHTMLContent = false;
				} else if (rootElement.getTagName().equals(TAG_HTML)) {
					// rely on root element to detect if we have an XHTML file
					// and not on doctype. We need to support xhtml files with
					// no doctype.
					hasXHTMLContent = true;
				} else
					// not intro XML nor XHTML.
					document = null;
			}
		} catch (Exception e) {
			Log.error("Could not load Intro content file: " + content, e); //$NON-NLS-1$
		}
	}


	private Document parse(String fileURI) {
		Document document = null;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
			docFactory.setValidating(false);
			// if this is not set, Document.getElementsByTagNameNS() will fail.
			docFactory.setNamespaceAware(true);
			docFactory.setExpandEntityReferences(false);
			DocumentBuilder parser = docFactory.newDocumentBuilder();
			parser.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader(""))); //$NON-NLS-1$
			document = parser.parse(fileURI);
			return document;

		} catch (SAXParseException spe) {
			StringBuilder buffer = new StringBuilder("IntroParser error in line "); //$NON-NLS-1$
			buffer.append(spe.getLineNumber());
			buffer.append(", uri "); //$NON-NLS-1$
			buffer.append(spe.getSystemId());
			buffer.append("\n"); //$NON-NLS-1$
			buffer.append(spe.getMessage());

			// Use the contained exception.
			Exception x = spe;
			if (spe.getException() != null)
				x = spe.getException();
			Log.error(buffer.toString(), x);

		} catch (SAXException sxe) {
			Exception x = sxe;
			if (sxe.getException() != null)
				x = sxe.getException();
			Log.error(x.getMessage(), x);

		} catch (ParserConfigurationException pce) {
			// Parser with specified options can't be built
			Log.error(pce.getMessage(), pce);

		} catch (IOException ioe) {
			Log.error(ioe.getMessage(), ioe);
		}
		return null;
	}


	/**
	 * Returned the DOM representing the intro xml content file. May return null
	 * if parsing the file failed.
	 *
	 * @return Returns the document.
	 */
	public Document getDocument() {
		return document;
	}

	public boolean hasXHTMLContent() {
		return hasXHTMLContent;
	}


	public static String convertToString(Document document) {
		try {
			// identity xslt.
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			DOMSource source = new DOMSource(document);

			StringWriter stringWriter = new StringWriter();
			StreamResult result = new StreamResult(stringWriter);

			// setup properties, for doctype.
			DocumentType docType = document.getDoctype();
			if (docType != null) {
				String value = docType.getSystemId();
				// transformer.clearParameters();
				transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, value);
				value = document.getDoctype().getPublicId();
				transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, value);
				transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes"); //$NON-NLS-1$
				// transformer.setOutputProperty(OutputKeys.MEDIA_TYPE,
				// "text/html");
				// transformer
				// .setOutputProperty(OutputKeys.ENCODING, "iso-8859-1");
			} else
				Log
					.warning("XHTML file used to display this Intro page does not have a Document type defined. " //$NON-NLS-1$
							+ "XHTML requires document types to be defined."); //$NON-NLS-1$

			transformer.transform(source, result);
			return stringWriter.toString();

		} catch (TransformerConfigurationException tce) {
			// Error generated by the parser
			Log.error("Transformer Config error: " + tce.getMessage(), null); //$NON-NLS-1$
			// Use the contained exception, if any
			Throwable x = tce;
			if (tce.getException() != null)
				x = tce.getException();
			Log.error("Transformer Stack trace: ", x); //$NON-NLS-1$

		} catch (TransformerException te) {
			// Error generated by the parser
			Log.error("Transformer error: " + te.getMessage(), te); //$NON-NLS-1$
			// Use the contained exception, if any
			Throwable x = te;
			if (te.getException() != null)
				x = te.getException();
			Log.error("Transformer Stack trace: ", x); //$NON-NLS-1$

		}
		return null;

	}



}
