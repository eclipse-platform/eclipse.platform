/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.webapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.ua.tests.help.remote.IndexServletTest;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class IndexServiceTest extends IndexServletTest {

	@Override
	protected Node getIndexContributions( String locale)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/index?lang=" + locale);
		try (InputStream is = url.openStream()) {
			InputSource inputSource = new InputSource(is);
			Document document = LocalEntityResolver.parse(inputSource);
			Node root = document.getFirstChild();
			assertThat(root.getNodeName()).isEqualTo("indexContributions");
			return root;
		}
	}

	@Test
	public void testIndexServiceXMLSchema()
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/index?lang=en");
		URL schemaUrl = new URL("http", "localhost", port, "/help/test/schema/xml/index.xsd");
		String schema = schemaUrl.toString();
		String uri = url.toString();
		String result = SchemaValidator.testXMLSchema(uri, schema);

		assertThat(result).as("URL: " + uri).isEqualTo("valid");
	}

	@Test
	public void testIndexServiceJSONSchema()
			throws Exception {
//		fail("Not yet implemented.");
	}

}
