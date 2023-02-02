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
package org.eclipse.ua.tests.intro.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.ua.tests.util.FileUtil;
import org.eclipse.ua.tests.util.XHTMLUtil;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroHomePage;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.IntroPage;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.eclipse.ui.internal.intro.impl.presentations.BrowserIntroPartImplementation;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/*
 * A utility for regenerating the _expected.txt files that contain the expected
 * output for the intro model when serialized. This reads all the intro content from
 * the plugin manifest (for this test plugin only), constructs the intro model, then
 * serializes the model to a text file, which is stored in the same directory as the
 * intro xml file, as <original_name>_expected.txt.
 *
 * These files are used by the JUnit tests to compare the result with the expected
 * result.
 *
 * Usage:
 *
 * 1. Run this test as a JUnit plug-in test.
 * 2. Right-click in "Package Explorer -> Refresh".
 *
 * The new files should appear.
 */
public class IntroModelSerializerTest {

	/*
	 * Ensure that org.eclipse.help.ui is started. It contributes extra
	 * content filtering that is used by this test. See
	 * UIContentFilterProcessor.
	 */
	@Before
	public void setUp() throws Exception {
		HelpUIPlugin.getDefault();
	}

	@Test
	public void testRunSerializer() throws FileNotFoundException {
		/*
		 * Serialize the test intros.
		 */
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.intro.config");
		Bundle bundle = FrameworkUtil.getBundle(IntroModelSerializerTest.class);
		for (IConfigurationElement element : elements) {
			/*
			 * Only use the ones from this test plugin.
			 */
			if (element.getDeclaringExtension().getContributor().getName().equals(bundle.getSymbolicName())) {
				String pluginRoot = bundle.getLocation().substring("update@".length());
				String content = element.getAttribute("content");
				String id = element.getAttribute("id");

				/*
				 * First do the intro XML files.
				 */
				IntroModelRoot model = ExtensionPointManager.getInst().getModel(id);
				IntroModelSerializer serializer = new IntroModelSerializer(model);

				String file = FileUtil.getResultFile(pluginRoot + content);
				try (PrintWriter out = new PrintWriter(new FileOutputStream(file))) {
					out.print(serializer.toString());
				}

				/*
				 * Now do the intro XHTML files. Find all the XHTML files
				 * referenced from the model.
				 */
				Map<String, String> map = getXHTMLFiles(model);
				Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<String, String> entry = iter.next();
					file = FileUtil.getResultFile(pluginRoot + entry.getKey());
					try (PrintWriter out = new PrintWriter(new FileOutputStream(file))) {
						out.print(entry.getValue());
					}
				}
			}
		}
	}

	/*
	 * Search through the given model and find all XHTML files referred to by
	 * the model. Also loads the contents of the XHTML files. The result is a
	 * mapping of filenames relative to the test plugin to Strings, the
	 * contents of the XHTML files.
	 */
	public static Map<String, String> getXHTMLFiles(IntroModelRoot model) {
		Map<String, String> map = new HashMap<>();
		Collection<AbstractIntroPage> pages = new ArrayList<>();
		IntroHomePage home = model.getRootPage();
		if (home.isXHTMLPage()) {
			pages.add(home);
		}
		IntroPage[] otherPages = model.getPages();
		for (IntroPage otherPage : otherPages) {
			if (otherPage.isXHTMLPage()) {
				pages.add(otherPage);
			}
		}
		Iterator<AbstractIntroPage> iter = pages.iterator();
		while (iter.hasNext()) {
			AbstractIntroPage page = iter.next();
			BrowserIntroPartImplementation impl = new BrowserIntroPartImplementation();
			String xhtml = impl.generateXHTMLPage(page, (provider, incremental) -> {
				// dummy site
			});
			xhtml = XHTMLUtil.removeEnvironmentSpecificContent(xhtml);
			// filter windows-specific newline
			xhtml = xhtml.replaceAll("\r", "");
			// ignore all beginning and ending whitespace
			xhtml = xhtml.trim();
			map.put(page.getInitialBase() + page.getRawContent(), xhtml);
		}
		return map;
	}
}
