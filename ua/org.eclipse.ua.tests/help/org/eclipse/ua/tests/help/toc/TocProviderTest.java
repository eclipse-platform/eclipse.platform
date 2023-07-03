/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.toc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.HelpPlugin;
import org.junit.Test;

public class TocProviderTest {

	/**
	 * Verify that the tocProvider extension in this plug-in contributes a TOC which can be linked to an anchor
	 */
	@Test
	public void testTocProvider() throws Exception {
		IToc[] tocs = HelpPlugin.getTocManager().getTocs("en");
		IToc uaToc = null;
		for (IToc toc : tocs) {
			if ("User Assistance Tests".equals(toc.getLabel())) {
				uaToc = toc;
			}
		}
		assertNotNull("User Assistance Tests not found", uaToc);
		ITopic[] children = uaToc.getTopics();
		int generatedParentTopics = 0;
		for (ITopic child : children) {
			if ("Generated Parent".equals(child.getLabel())) {
				generatedParentTopics++;
				assertEquals(4, child.getSubtopics().length);
			}
		}
		assertEquals(1, generatedParentTopics);

	}

}
