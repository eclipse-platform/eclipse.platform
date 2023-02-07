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
package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.help.ITopic;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.internal.toc.Toc;
import org.junit.Test;

public class TocObjectTest {

	private static final String TITLE_1 = "Toc Title";
	private static final String TOPIC_LABEL_1 = "Topic Label1";
	private static final String TOPIC_LABEL_2 = "Topic Label2";
	private static final String TOPIC_LABEL_3 = "Topic Label3";
	private static final String ANCHOR1 = "#A1";
	private static final String ANCHOR2 = "#A2";
	private static final String PATH = "/org.eclipse.ua.tests/file.html";

	@Test
	public void testLookupUnanchoredHref() {
		UserToc	utoc = new UserToc(TITLE_1, null, true);
		UserTopic	utopic = new UserTopic(TOPIC_LABEL_1, PATH, true);
		utoc.addTopic(utopic);
		Toc toc = new Toc(utoc);
		ITopic topic = toc.getTopic(PATH);
		assertNotNull(topic);
		assertEquals(topic.getHref(), PATH);
	}

	@Test
	public void testLookupAnchoredHrefByAnchor() {
		UserToc	utoc = new UserToc(TITLE_1, null, true);
		UserTopic	utopic = new UserTopic(TOPIC_LABEL_1, PATH + ANCHOR1, true);
		utoc.addTopic(utopic);
		Toc toc = new Toc(utoc);
		ITopic topic = toc.getTopic(PATH + ANCHOR1);
		assertNotNull(topic);
		assertEquals(topic.getHref(), PATH + ANCHOR1);
	}

	@Test
	public void testLookupAnchoredHref() {
		UserToc	utoc = new UserToc(TITLE_1, null, true);
		UserTopic	utopic = new UserTopic(TOPIC_LABEL_1, PATH + ANCHOR1, true);
		utoc.addTopic(utopic);
		Toc toc = new Toc(utoc);
		ITopic topic = toc.getTopic(PATH);
		assertNotNull(topic);
		assertEquals(topic.getLabel(), TOPIC_LABEL_1);
	}

	@Test
	public void testUnanchoredFoundFirst() {
		UserToc	utoc = new UserToc(TITLE_1, null, true);
		UserTopic	utopic = new UserTopic(TOPIC_LABEL_1, PATH + ANCHOR1, true);
		utoc.addTopic(utopic);
		utopic = new UserTopic(TOPIC_LABEL_2, PATH, true);
		utoc.addTopic(utopic);
		utopic = new UserTopic(TOPIC_LABEL_3, PATH + ANCHOR2, true);
		utoc.addTopic(utopic);
		Toc toc = new Toc(utoc);
		ITopic topic = toc.getTopic(PATH);
		assertNotNull(topic);
		assertEquals(topic.getLabel(), TOPIC_LABEL_2);
	}

	@Test
	public void testGetTopicNull() {
		UserToc	utoc = new UserToc(TITLE_1, null, true);
		Toc toc = new Toc(utoc);
		ITopic emptyTopic = toc.getTopic(null);
		assertTrue(emptyTopic.getChildren().length == 0);
		assertTrue(emptyTopic.isEnabled(HelpEvaluationContext.getContext()));
	}

}
