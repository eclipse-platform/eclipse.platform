/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.toc.TocFile;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.junit.Before;
import org.junit.Test;

/*
 * A utility for regenerating the _expected.txt files that contain the expected
 * result for the TOC model when serialized. This reads all the TOC content from
 * the plugin manifest (for this test plugin only), constructs the model, then
 * serializes the model to a text file, which is stored in the same directory as the
 * TOC xml file, as <original_name>_expected.txt.
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
public class TocModelSerializerTest {

	/*
	 * Ensure that org.eclipse.help.ui is started. It contributes extra content
	 * filtering that is used by this test. See UIContentFilterProcessor.
	 */
	@Before
	public void setUp() throws Exception {
		HelpUIPlugin.getDefault();
	}

	@Test
	public void testRunSerializer() throws IOException {
		/*
		Collection tocFiles = getTocFiles();
		TocBuilder builder = new TocBuilder();
		builder.build(tocFiles);
		Collection builtTocs = builder.getBuiltTocs();

		Iterator iter = builtTocs.iterator();
		while (iter.hasNext()) {
			Toc toc = (Toc)iter.next();
			TocFile file = toc.getTocFile();

			String pluginRoot = UserAssistanceTestPlugin.getDefault().getBundle().getLocation().substring("update@".length());
			String relativePath = file.getHref();
			String absolutePath = pluginRoot + relativePath;
			String resultFile = FileUtil.getResultFile(absolutePath);

			PrintWriter out = new PrintWriter(new FileOutputStream(resultFile));
			out.print(TocModelSerializer.serialize(toc));
			out.close();
		}
		*/
	}

	/**
	 * Find all the TOC files to use for this test.
	 */
	public static Collection<TocFile> getTocFiles() {
		Collection<TocFile> tocFiles = new ArrayList<>();
		IExtensionPoint xpt = Platform.getExtensionRegistry().getExtensionPoint(HelpPlugin.PLUGIN_ID, "toc");
		IExtension[] extensions = xpt.getExtensions();
		for (IExtension extension : extensions) {
			String pluginId = extension.getContributor().getName();
			if (pluginId.equals("org.eclipse.ua.tests")) {
				IConfigurationElement[] configElements = extension.getConfigurationElements();
				for (IConfigurationElement configElement : configElements) {
					if (configElement.getName().equals("toc")) {
						// only get files in data/help/toc/
						String href = configElement.getAttribute("file"); //$NON-NLS-1$
						if (href.startsWith("data/help/toc/")) {
							boolean isPrimary = "true".equals(configElement.getAttribute("primary")); //$NON-NLS-1$
							String extraDir = configElement.getAttribute("extradir"); //$NON-NLS-1$
							String categoryId = configElement.getAttribute("category"); //$NON-NLS-1$
							tocFiles.add(new TocFile(pluginId, href, isPrimary, Platform.getNL(), extraDir, categoryId));
						}
					}
				}
			}
		}
		return tocFiles;
	}
}
