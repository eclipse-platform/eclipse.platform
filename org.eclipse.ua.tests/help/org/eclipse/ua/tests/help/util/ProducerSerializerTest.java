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

import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.eclipse.help.HelpSystem;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.ua.tests.util.FileUtil;
import org.eclipse.ua.tests.util.XHTMLUtil;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

/*
 * A utility for regenerating the _expected.txt files that contain the
 * expected result for the producer content model when serialized.
 * This reads all the TOC content from data/help/producer, passes it
 * through the dynamic content producer, then stores the result in a
 * text file, which is stored in the same directory as the original
 * xhtml file, as <original_name>_expected.txt.
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
public class ProducerSerializerTest {
	@Test
	public void testGenerateOutput() throws Exception {
		IToc[] tocs = HelpSystem.getTocs();
		String pluginRoot = FrameworkUtil.getBundle(getClass()).getLocation().substring("update@".length());
		for (IToc toc : tocs) {
			// only look for content in data/help/producer
			if (toc.getHref().contains("data/help/producer/")) {
				ITopic[] topics = toc.getTopics();
				// only goes one level deep - don't need subtopics here
				for (ITopic topic : topics) {
					String href = topic.getHref();
					String relativePath = href.substring(href.indexOf('/', 1));
					String absolutePath = pluginRoot + relativePath;
					String resultFile = FileUtil.getResultFile(absolutePath);

					try (PrintWriter out = new PrintWriter(new FileOutputStream(resultFile))) {
						String output = FileUtil.readString(HelpSystem.getHelpContent(href));
						output = XHTMLUtil.removeEnvironmentSpecificContent(output);
						out.print(output);
					}
				}
			}
		}
	}
}
