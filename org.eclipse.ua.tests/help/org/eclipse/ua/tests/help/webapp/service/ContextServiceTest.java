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

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.ua.tests.help.remote.ContextServletTest;
import org.junit.Test;
import org.w3c.dom.Element;

public class ContextServiceTest extends ContextServletTest {

	@Override
	protected Element[] getContextsFromServlet(String phrase)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/context?id="
				+ URLEncoder.encode(phrase, StandardCharsets.UTF_8));
		return makeServletCall(url);
	}

	@Override
	protected Element[] getContextsUsingLocale(String phrase, String locale)
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/context?id="
				+ URLEncoder.encode(phrase, StandardCharsets.UTF_8) + "&lang=" + locale);
		return makeServletCall(url);
	}

	@Test
	public void testContextServiceXMLSchema()
			throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/vs/service/context?id=org.eclipse.ua.tests.test_cheatsheets&lang=en");
		URL schemaUrl = new URL("http", "localhost", port, "/help/test/schema/xml/context.xsd");
		String schema = schemaUrl.toString();
		String uri = url.toString();
		String result = SchemaValidator.testXMLSchema(uri, schema);

		assertEquals("URL: \"" + uri + "\" is ", "valid", result);
	}

	@Test
	public void testContextServiceJSONSchema()
			throws Exception {
//		fail("Not yet implemented.");
	}

}
