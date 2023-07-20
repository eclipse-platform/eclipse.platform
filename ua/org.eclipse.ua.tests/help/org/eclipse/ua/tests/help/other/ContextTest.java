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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.internal.context.Context;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ContextTest {

	private static final String ENABLEMENT_CHEATSHEETS = "<enablement><with variable=\"platform\">" +
	"<test property=\"org.eclipse.core.runtime.isBundleInstalled\" args=\"org.eclipse.ui.cheatsheets\"/>" +
	 "</with></enablement>";
	private static final String ENABLEMENT_INVALID = "<enablement><with variable=\"platform\">" +
		"<test property=\"org.eclipse.core.runtime.isBundleInstalled\" args=\"org.eclipse.ui.invalid\"/>" +
		 "</with></enablement>";
	private static final String FILTER_OUT = "<filter name = \"plugin\" value = \"org.eclipse.ua.invalid\"/>";
	private static final String TOPIC_END = "</topic>";
	private static final String TOPIC_HEAD_ECLIPSE = "<topic href=\"http://www.eclipse.org\" label=\"enabled\">";
	private static final String TOPIC_OLD_FILTER_DISABLED = "<topic filter=\"plugin=org.eclipse.ua.invalid\" href=\"www.eclipse.org\""
		+ " label=\"Transformations and transformation configurations\"/>";
	private static final String CONTEXT_DESCRIPTION = "<description>Context Description</description>";
	private static final String EMPTY_DESCRIPTION = "<description></description>";
	private static final String CONTEXT_HEAD = "<context id=\"viewer\" title=\"Sample View\">";
	private static final String CONTEXT_HEAD_WITH_ATTRIBUTE = "<context id=\"viewer\" title=\"Sample View\" att=\"abc\">";
	private static final String TOPIC_ECLIPSE = "<topic href=\"http://www.eclipse.org\" label=\"eclipse\"/>";
	private static final String TOPIC_WITH_ENABLEMENT = TOPIC_HEAD_ECLIPSE + ENABLEMENT_CHEATSHEETS + TOPIC_END;
	private static final String TOPIC_DISABLED = TOPIC_HEAD_ECLIPSE + ENABLEMENT_INVALID + TOPIC_END;
	private static final String TOPIC_FILTER_OUT = TOPIC_HEAD_ECLIPSE + FILTER_OUT + TOPIC_END;
	private static final String END_CONTEXT = "</context>";

	@Before
	public void setUp() throws Exception {
		// Required for isEnabled() to work correctly
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_WORKBENCH);
	}

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
	public void testSimpleContext() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_ECLIPSE +
			END_CONTEXT;
		Context context;
		context = createContext(contextSource);
		assertEquals("Sample View", context.getTitle());
		assertEquals("Context Description", context.getText());
		IHelpResource[] related = context.getRelatedTopics();
		assertEquals(1, related.length);
		assertEquals("eclipse", related[0].getLabel());
	}

	@Test
	public void testContextWithEnablement() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			END_CONTEXT;
		Context context;
		context = createContext(contextSource);
		assertEquals("Sample View", context.getTitle());
		assertEquals("Context Description", context.getText());
		assertEquals("viewer", context.getId());
		IHelpResource[] related = context.getRelatedTopics();
		assertEquals(1, related.length);
		assertEquals("enabled", related[0].getLabel());
		assertTrue(related[0] instanceof IUAElement);
		IUAElement topic = (IUAElement)related[0];
		IUAElement[] topicChildren = topic.getChildren();
		assertEquals(1, topicChildren.length);
	}

	@Test
	public void testCopyContext() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			END_CONTEXT;
		Context context  = createContext(contextSource);
		Context context2 = new Context(context, "new id");
		Context context3 = new Context(context2, "new id2");
		assertEquals("Sample View", context.getTitle());
		assertEquals("Sample View", context2.getTitle());
		assertEquals("Context Description", context.getText());
		assertEquals("Context Description", context2.getText());
		assertEquals("viewer", context.getId());
		assertEquals("new id", context2.getId());
		assertEquals("new id2", context3.getId());

		IHelpResource[] related = context.getRelatedTopics();
		assertEquals(1, related.length);
		assertEquals("enabled", related[0].getLabel());
		assertTrue(related[0] instanceof IUAElement);
		Topic topic = (Topic)related[0];
		assertEquals("http://www.eclipse.org", topic.getHref());

		related = context2.getRelatedTopics();
		assertEquals(1, related.length);
		assertEquals("enabled", related[0].getLabel());
		assertTrue(related[0] instanceof IUAElement);
		topic = (Topic)related[0];
		assertEquals("http://www.eclipse.org", topic.getHref());

		related = context3.getRelatedTopics();
		assertEquals(1, related.length);
		assertEquals("enabled", related[0].getLabel());
		assertTrue(related[0] instanceof IUAElement);
		topic = (Topic)related[0];
		assertEquals("http://www.eclipse.org", topic.getHref());
	}

	@Test
	public void testEnablement() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			TOPIC_DISABLED +
			TOPIC_OLD_FILTER_DISABLED +
			END_CONTEXT;
		Context context  = createContext(contextSource);
		IHelpResource[] related = context.getRelatedTopics();
		assertEquals(3, related.length);
		assertTrue(((Topic)related[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related[1]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related[2]).isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testOldStyleFilteringOfCopies() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			TOPIC_OLD_FILTER_DISABLED +
			END_CONTEXT;
		Context context1  = createContext(contextSource);
		Context context2 = new Context(context1, "id");
		Context context3 = new Context(context2, "id2");

		IHelpResource[] related1 = context1.getRelatedTopics();
		assertEquals(2, related1.length);
		assertTrue(((Topic)related1[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related1[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related2 = context2.getRelatedTopics();
		assertEquals(2, related2.length);
		assertTrue(((Topic)related2[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related2[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related3 = context3.getRelatedTopics();
		assertEquals(2, related3.length);
		assertTrue(((Topic)related3[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related3[1]).isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testOldStyleFilteringOfCopies2() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			TOPIC_OLD_FILTER_DISABLED +
			END_CONTEXT;
		Context context1  = createContext(contextSource);
		Context context2 = new Context(context1, "id");
		Context context3 = new Context(context1, "id2");

		IHelpResource[] related1 = context1.getRelatedTopics();
		assertEquals(2, related1.length);
		assertTrue(((Topic)related1[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related1[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related2 = context2.getRelatedTopics();
		assertEquals(2, related2.length);
		assertTrue(((Topic)related2[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related2[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related3 = context3.getRelatedTopics();
		assertEquals(2, related3.length);
		assertTrue(((Topic)related3[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related3[1]).isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testFilteringOfCopies() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			TOPIC_FILTER_OUT +
			END_CONTEXT;
		Context context1  = createContext(contextSource);
		Context context2 = new Context(context1, "id");
		Context context3 = new Context(context2, "id2");
		IHelpResource[] related1 = context1.getRelatedTopics();
		assertEquals(2, related1.length);
		assertTrue(((Topic)related1[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related1[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related2 = context2.getRelatedTopics();
		assertEquals(2, related2.length);
		assertTrue(((Topic)related2[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related2[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related3 = context3.getRelatedTopics();
		assertEquals(2, related3.length);
		assertTrue(((Topic)related3[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related3[1]).isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testFilteringOfCopies2() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			TOPIC_FILTER_OUT +
			END_CONTEXT;
		Context context1  = createContext(contextSource);
		Context context2 = new Context(context1, "id");
		Context context3 = new Context(context1, "id2");

		IHelpResource[] related1 = context1.getRelatedTopics();
		assertEquals(2, related1.length);
		assertTrue(((Topic)related1[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related1[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related2 = context2.getRelatedTopics();
		assertEquals(2, related2.length);
		assertTrue(((Topic)related2[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related2[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related3 = context3.getRelatedTopics();
		assertEquals(2, related3.length);
		assertTrue(((Topic)related3[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related3[1]).isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testEnablementOfCopies() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			TOPIC_DISABLED +
			END_CONTEXT;
		Context context1  = createContext(contextSource);
		Context context2 = new Context(context1, "id");
		Context context3 = new Context(context2, "id2");
		IHelpResource[] related1 = context1.getRelatedTopics();
		assertEquals(2, related1.length);
		assertTrue(((Topic)related1[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related1[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related2 = context2.getRelatedTopics();
		assertEquals(2, related2.length);
		assertTrue(((Topic)related2[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related2[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related3 = context3.getRelatedTopics();
		assertEquals(2, related3.length);
		assertTrue(((Topic)related3[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related3[1]).isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testEnablementOfCopies2() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			TOPIC_DISABLED +
			END_CONTEXT;
		Context context1  = createContext(contextSource);
		Context context2 = new Context(context1, "id");
		Context context3 = new Context(context1, "id2");

		IHelpResource[] related1 = context1.getRelatedTopics();
		assertEquals(2, related1.length);
		assertTrue(((Topic)related1[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related1[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related2 = context2.getRelatedTopics();
		assertEquals(2, related2.length);
		assertTrue(((Topic)related2[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related2[1]).isEnabled(HelpEvaluationContext.getContext()));

		IHelpResource[] related3 = context3.getRelatedTopics();
		assertEquals(2, related3.length);
		assertTrue(((Topic)related3[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related3[1]).isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testEnablementOfCopies3() {
		final String contextSource = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_WITH_ENABLEMENT +
			TOPIC_DISABLED +
			END_CONTEXT;
		Context context1  = createContext(contextSource);
		Context context2 = new Context(context1, "id");
		Context context3 = new Context(context1, "id2");

		deleteAndInsert(context1);
		deleteAndInsert(context2);
		deleteAndInsert(context3);
	}

	private void deleteAndInsert(Context context) {
		IHelpResource[] related = context.getRelatedTopics();
		assertEquals(2, related.length);
		IHelpResource enabled= related[0];
		context.removeChild((UAElement) enabled);
		related = context.getRelatedTopics();
		assertEquals(1, related.length);
		Topic disabled = (Topic)related[0];
		assertFalse(disabled.isEnabled(HelpEvaluationContext.getContext()));
		context.insertBefore((UAElement) enabled, disabled);
		related = context.getRelatedTopics();
		assertEquals(2, related.length);
		assertTrue(((Topic)related[0]).isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(((Topic)related[1]).isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testContextWithAttribute() {
		final String contextSource = CONTEXT_HEAD_WITH_ATTRIBUTE +
			CONTEXT_DESCRIPTION +
			TOPIC_ECLIPSE +
			END_CONTEXT;
		Context context;
		context = createContext(contextSource);
		assertEquals("abc", context.getAttribute("att"));
	}

	@Test
	public void testContextWithoutDescription() {
		final String contextSource = CONTEXT_HEAD +
			TOPIC_ECLIPSE +
			END_CONTEXT;
		Context context;
		context = createContext(contextSource);
		assertNull(context.getText());
	}

	@Test
	public void testContextWithEmptyDescription() {
		final String contextSource = CONTEXT_HEAD +
			EMPTY_DESCRIPTION +
			TOPIC_ECLIPSE +
			END_CONTEXT;
		Context context;
		context = createContext(contextSource);
		assertEquals("", context.getText());
	}

	@Test
	public void testContextWithoutDescriptionSelfCatenation() {
		final String contextSource = CONTEXT_HEAD +
			TOPIC_ECLIPSE +
			END_CONTEXT;
		Context context1 = createContext(contextSource);
		Context context2 = createContext(contextSource);
		context1.mergeContext(context2);
		assertNull(context1.getText());
	}

	@Test
	public void testContextWithoutDescriptionMixedCatenation() {
		final String contextSourceEmpty = CONTEXT_HEAD +
			TOPIC_ECLIPSE +
			END_CONTEXT;
		final String contextSourceWithDesc = CONTEXT_HEAD +
			CONTEXT_DESCRIPTION +
			TOPIC_ECLIPSE +
			END_CONTEXT;
		Context context1 = createContext(contextSourceEmpty);
		Context context2 = createContext(contextSourceWithDesc);
		context1.mergeContext(context2);
		assertEquals("Context Description", context1.getText());
		Context context3 = createContext(contextSourceWithDesc);
		Context context4 = createContext(contextSourceEmpty);
		context3.mergeContext(context4);
		assertEquals("Context Description", context3.getText());
	}

	/*
	public void testCopyContextWithAttribute() {
		final String contextSource = CONTEXT_HEAD_WITH_ATTRIBUTE +
			CONTEXT_DESCRIPTION +
			TOPIC_ECLIPSE +
			END_CONTEXT;
		Context context1;
		context1 = createContext(contextSource);
		Context context2 = new Context(context1, "id");
		assertEquals("abc", context1.getAttribute("att"));
		assertEquals("abc", context2.getAttribute("att"));
	}
	*/

}
