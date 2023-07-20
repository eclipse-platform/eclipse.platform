/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.context.Context;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ContextMergeTest {

	private static final String ENABLEMENT_CHEATSHEETS = "<enablement><with variable=\"platform\">" +
	"<test property=\"org.eclipse.core.runtime.isBundleInstalled\" args=\"org.eclipse.ui.cheatsheets\"/>" +
	 "</with></enablement>";
	private static final String TOPIC_END = "</topic>";
	private static final String TOPIC_HEAD_ECLIPSE = "<topic href=\"http://www.eclipse.org\" label=\"enabled\">";
	private static final String CONTEXT_DESCRIPTION = "<description>Context Description</description>";
	private static final String CONTEXT_HEAD = "<context id=\"viewer\" title=\"Sample View\">";
	private static final String TOPIC_ECLIPSE = "<topic href=\"http://www.eclipse.org\" label=\"eclipse\"/>";
	private static final String TOPIC_BUGZILLA = "<topic href=\"http://www.eclipse.org/bugzilla\" label=\"bugzilla\"/>";
	private final String TOPIC_WITH_ENABLEMENT = TOPIC_HEAD_ECLIPSE + ENABLEMENT_CHEATSHEETS + TOPIC_END;
	private static final String END_CONTEXT = "</context>";

	private Context createContext(final String contextSource) {
		Context context;
		Document doc;
		try {
			doc = LocalEntityResolver.parse(contextSource);
		} catch (Exception e) {
			fail("Caught Exception");
			doc = null;
		}
		context = new Context((Element) doc.getFirstChild());
		return context;
	}

	@Test
	public void testContextMerge() {
		final String contextSource1 = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_ECLIPSE +
			TOPIC_WITH_ENABLEMENT +
			END_CONTEXT;
		final String contextSource2 = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_BUGZILLA +
			END_CONTEXT;
		Context context1;
		Context context2;
		context1 = createContext(contextSource1);
		context2 = createContext(contextSource2);
		context1.mergeContext(context2);
		assertEquals("Sample View", context1.getTitle());
		assertEquals("Context Description", context1.getText());
		assertEquals("viewer", context1.getId());
		IHelpResource[] related = context1.getRelatedTopics();
		assertEquals(3, related.length);
		assertEquals("eclipse", related[0].getLabel());
		assertEquals("enabled", related[1].getLabel());
		assertEquals("bugzilla", related[2].getLabel());
		assertTrue(related[0] instanceof IUAElement);
		IUAElement topic = (IUAElement)related[1];
		IUAElement[] topicChildren = topic.getChildren();
		assertEquals(1, topicChildren.length);
	}

}
