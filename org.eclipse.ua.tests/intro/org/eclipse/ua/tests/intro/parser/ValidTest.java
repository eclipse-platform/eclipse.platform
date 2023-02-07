/*******************************************************************************
 *  Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.intro.parser;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.ua.tests.intro.util.IntroModelSerializer;
import org.eclipse.ua.tests.intro.util.IntroModelSerializerTest;
import org.eclipse.ua.tests.util.FileUtil;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/*
 * Tests the intro parser on valid intro content.
 */
public class ValidTest {

	/*
	 * Ensure that org.eclipse.help.ui is started. It contributes extra content
	 * filtering that is used by this test. See UIContentFilterProcessor.
	 */
	@Before
	public void setUp() throws Exception {
		HelpUIPlugin.getDefault();
	}

	@Test
	public void testDynamicXHTML() throws IOException {
		singleConfigTest("org.eclipse.ua.tests.intro.config.dynamicXHTML");
	}

	@Test
	public void testDynamicXML() throws IOException {
		singleConfigTest("org.eclipse.ua.tests.intro.config.dynamicXML");
	}

	@Test
	public void testAnchors() throws IOException {
		singleConfigTest("org.eclipse.ua.tests.intro.config.anchors");
	}

	@Test
	public void testMixed() throws IOException {
		singleConfigTest("org.eclipse.ua.tests.intro.config.mixed");
	}

	@Test
	public void testStatic() throws IOException {
		singleConfigTest("org.eclipse.ua.tests.intro.config.static");
	}

	/*
	 * Test valid intro content. This goes through the test intro content
	 * (xml files and xhtml files) and serializes it using the
	 * IntroModelSerializer, then compares the result of the serialization
	 * with the expected result (the _expected.txt files).
	 */
	private void singleConfigTest(String configId) throws IOException {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.intro.config");
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		for (IConfigurationElement element : elements) {
			/*
			 * Only use the ones from this test plugin.
			 */
			if (element.getDeclaringExtension().getContributor().getName().equals(bundle.getSymbolicName())) {
				String content = element.getAttribute("content");
				String id = element.getAttribute("id");
				if (id.equals(configId)) {
					for (int x = 0; x < 10; x++) {
						 // Perform 10 times to better detect intermittent ordering bugs
						IntroModelRoot model = ExtensionPointManager.getInst().getModel(id);
						IntroModelSerializer serializer = new IntroModelSerializer(model);

						String expected = FileUtil.getContents(bundle, FileUtil.getResultFile(content));
						String actual = serializer.toString();
						Assert.assertEquals("The model parsed for intro did not match the expected result for: " + id, expected, actual);

						Map<String, String> map = IntroModelSerializerTest.getXHTMLFiles(model);
						Iterator<Entry<String, String>> iter = map.entrySet().iterator();
						while (iter.hasNext()) {
							Entry<String, String> entry = iter.next();
							String relativePath = entry.getKey();

							expected = FileUtil.getContents(bundle, FileUtil.getResultFile(relativePath));
							actual = entry.getValue();
							Assert.assertEquals("The XHTML generated for intro did not match the expected result for: " + relativePath, expected, actual);
						}
					}
					return;
				}
			}
		}
		fail("Config extension not found");
	}
}
