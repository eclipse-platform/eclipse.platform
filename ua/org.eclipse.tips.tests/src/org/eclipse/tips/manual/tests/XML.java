/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.manual.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class XML {

	public static void main(String[] args)
			throws ParserConfigurationException, TransformerException, SAXException, IOException {
		String pExpression = "<with variable=\"activeWorkbenchWindow.activePerspective\"><equals value=\"org.eclipse.jdt.ui.JavaPerspective\"></equals></with>";
		pExpression = "<enablement>" + pExpression + "</enablement>";
		pExpression = "<?xml version=\"1.0\"?>" + pExpression;
		System.out.println(pExpression);
		@SuppressWarnings("restriction")
		DocumentBuilder builder = org.eclipse.core.internal.runtime.XmlProcessorFactory
				.createDocumentBuilderWithErrorOnDOCTYPE();
		Document doc = builder.parse(new ByteArrayInputStream(pExpression.getBytes()));
		Element element2 = (Element) doc.getElementsByTagName("enablement").item(0);
		@SuppressWarnings("restriction")
		TransformerFactory transformerFactory = org.eclipse.core.internal.runtime.XmlProcessorFactory
				.createTransformerFactoryWithErrorOnDOCTYPE();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(element2);
		StreamResult result = new StreamResult(System.out);
		transformer.transform(source, result);
	}
}
