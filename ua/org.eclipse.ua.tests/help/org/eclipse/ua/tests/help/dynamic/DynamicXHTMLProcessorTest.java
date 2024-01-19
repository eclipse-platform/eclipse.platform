/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.help.internal.xhtml.DynamicXHTMLProcessor;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.ua.tests.util.XMLUtil;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXException;

public class DynamicXHTMLProcessorTest {

	@Before
	public void setUp() throws Exception {
		// activate the UI plug-in for UI filtering ability
		HelpUIPlugin.getDefault();
	}

	private String process(String path) throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		try (InputStream is = getProcessedInput(path, bundle)) {
			return readStream(is);
		}
	}

	protected InputStream getProcessedInput(String path, Bundle bundle)
			throws IOException, SAXException, ParserConfigurationException,
			TransformerException, TransformerConfigurationException {
		URL url = bundle.getEntry(path);
		if(url == null ) {
			throw new IOException("No entry to '"+path+"' could be found or caller does not have the appropriate permissions.");//$NON-NLS-1$ //$NON-NLS-2$
		}
		try (InputStream in = url.openStream()) {
			String href = '/' + bundle.getBundleId() + path;
			return DynamicXHTMLProcessor.process(href, in, "en", true);
		}
	}

	private String readStream(InputStream is) throws Exception {
		try (InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			StringBuilder buffer = new StringBuilder();
			char[] cbuf = new char[256];
			int len;
			do {
				len = inputStreamReader.read(cbuf);
				if (len > 0) {
					buffer.append(cbuf, 0, len);
				}
			} while (len >= 0);
			return buffer.toString();
		}
	}

	@Test
	public void testXhtmlNoCollapseAnchor() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchor.xhtml");
		assertThat(processed.indexOf("</a>")).as("anchor collapsed in " + processed).isGreaterThan(0);
	}

	@Test
	public void testXhtmlNoCollapseParagraph() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchor.xhtml");
		assertThat(processed.indexOf("</p>")).as("paragraph collapsed in " + processed).isGreaterThan(0);
	}

	@Test
	public void testXhtmlNoCollapseAnchorIC() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchorWithComment.xhtml");
		assertThat(processed.indexOf("</a>")).as("anchor collapsed in " + processed).isGreaterThan(0);
	}

	@Test
	public void testXhtmlNoCollapseParagraphIC() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchorWithComment.xhtml");
		assertThat(processed.indexOf("</p>")).as("paragraph collapsed in " + processed).isGreaterThan(0);
	}

	@Test
	public void testXhtmlNoCollapseDiv() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyDiv.xhtml");
		assertThat(processed.indexOf("</div>")).as("div collapsed in " + processed).isGreaterThan(0);
	}

	@Test
	public void testXhtmlNoCollapseScript() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchor.xhtml");
		assertThat(processed.indexOf("</div>")).as("div collapsed in " + processed).isGreaterThan(0);
	}

	@Test
	public void testValidXML() throws Exception {
		String processed = process("data/help/dynamic/xhtml/emptyAnchor.xhtml");
		XMLUtil.assertParseableXML(processed);
	}

}
