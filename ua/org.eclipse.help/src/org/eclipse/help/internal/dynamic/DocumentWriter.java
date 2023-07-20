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
package org.eclipse.help.internal.dynamic;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.help.internal.UAElement;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

public class DocumentWriter {

	private Transformer transformer;

	public String writeString(UAElement element, boolean xmlDecl) throws TransformerException, TransformerConfigurationException {
		return writeString(element.getElement(), xmlDecl);
	}

	public String writeString(Element element, boolean xmlDecl) throws TransformerException, TransformerConfigurationException {
		byte[] bytes = writeBytes(element, xmlDecl);
		String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);
		if (encoding == null) {
			encoding = "UTF-8"; //$NON-NLS-1$
		}
		try {
			return new String(bytes, encoding);
		}
		catch (UnsupportedEncodingException e) {
			return new String(bytes);
		}
	}

	public byte[] writeBytes(UAElement element, boolean xmlDecl) throws TransformerException, TransformerConfigurationException {
		return writeBytes(element.getElement(), xmlDecl);
	}

	public byte[] writeBytes(Element element, boolean xmlDecl) throws TransformerException, TransformerConfigurationException {
		Document document = element.getOwnerDocument();
		if (transformer == null) {
			@SuppressWarnings("restriction")
			TransformerFactory factory = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.createTransformerFactoryWithErrorOnDOCTYPE();
			transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		}
		DocumentType docType = document.getDoctype();
		Properties props = transformer.getOutputProperties();
		if (docType != null) {
			props.setProperty(OutputKeys.DOCTYPE_PUBLIC, docType.getPublicId());
			props.setProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
		}
		else {
			props.remove(OutputKeys.DOCTYPE_PUBLIC);
			props.remove(OutputKeys.DOCTYPE_SYSTEM);
		}
		props.setProperty(OutputKeys.OMIT_XML_DECLARATION, xmlDecl ? "no" : "yes"); //$NON-NLS-1$ //$NON-NLS-2$
		transformer.setOutputProperties(props);

		DOMSource source = new DOMSource(element);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(out);
		transformer.transform(source, result);
		return out.toByteArray();
	}
}
