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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexEntry2;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.index.IndexEntry;
import org.eclipse.help.internal.index.IndexSee;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IndexEntryTest {

	private static final String ECLIPSE = "eclipse";
	private static final String BUGZILLA = "bugzilla";
	private static final String BUGZILLA_HREF = "https://bugs.eclipse.org/bugs/";
	private static final String INVALID_INSTALLED = "<with variable=\"platform\">" +
	"<test property=\"org.eclipse.core.runtime.isBundleInstalled\" args=\"org.eclipse.ui.invalid\"/></with>";
	private static final String CS_INSTALLED = "<with variable=\"platform\">" +
		"<test property=\"org.eclipse.core.runtime.isBundleInstalled\" args=\"org.eclipse.ui.cheatsheets\"/></with>";
	private static final String ENABLEMENT_CHEATSHEETS = "<enablement>" + CS_INSTALLED + "</enablement>";
	private static final String ENABLEMENT_INVALID = "<enablement>" +  INVALID_INSTALLED  + "</enablement>";
	private static final String FILTER_IN = "<filter name = \"plugin\" value = \"org.eclipse.ua.tests\"/>";
	private static final String FILTER_OUT = "<filter name = \"plugin\" value = \"org.eclipse.ua.invalid\"/>";
	private static final String NEGATED_FILTER_IN = "<filter name = \"plugin\" value = \"!org.eclipse.ua.tests\"/>";
	private static final String NEGATED_FILTER_OUT = "<filter name = \"plugin\" value = \"!org.eclipse.ua.invalid\"/>";
	private static final String ENTRY_END = "</entry>";
	private static final String ENTRY_HEAD_ECLIPSE = "<entry keyword=\"eclipse\">";
	private static final String ENTRY_ECLIPSE = "<entry keyword=\"eclipse\"/>";
	private static final String ENTRY_BUGZILLA = "<entry keyword=\"bugzilla\"/>";
	private static final String TOPIC_BUGZILLA = "<topic href=\"https://bugs.eclipse.org/bugs/\" label=\"bugzilla\"/>";
	private static final String SEE_ALSO_SDK = "<see keyword=\"sdk\"/>";

	private final String ENTRY_WITH_ENABLEMENT = ENTRY_HEAD_ECLIPSE + ENABLEMENT_CHEATSHEETS + ENTRY_END;
	private final String ENTRY_NOT_ENABLED = ENTRY_HEAD_ECLIPSE + ENABLEMENT_INVALID + ENTRY_END;
	private final String ENTRY_FILTER_IN = ENTRY_HEAD_ECLIPSE + FILTER_IN + ENTRY_END;
	private final String ENTRY_FILTER_OUT = ENTRY_HEAD_ECLIPSE + FILTER_OUT + ENTRY_END;
	private final String ENTRY_FILTER_MIXED = ENTRY_HEAD_ECLIPSE + FILTER_IN + FILTER_OUT + ENTRY_END;
	private static final String ENTRY_OLD_FILTER = "<entry filter=\"plugin=org.eclipse.ua.tests\" "
		+ " keyword=\"Transformations and transformation configurations\"/>";
	private static final String ENTRY_OLD_FILTER_DISABLED = "<entry filter=\"plugin=org.eclipse.ua.invalid\" "
		+ " keyword=\"Transformations and transformation configurations\"/>";
	private static final String ENTRY_OLD_FILTER_IN__NEGATED = "<entry filter=\"plugin!=org.eclipse.ua.tests\" "
		+ " keyword=\"Transformations and transformation configurations\"/>";
	private static final String ENTRY_OLD_FILTER_OUT_NEGATED = "<entry filter=\"plugin!=org.eclipse.ua.invalid\" "
		+ " keyword=\"Transformations and transformation configurations\"/>";
	private final String ENTRY_WITH_CHILD = ENTRY_HEAD_ECLIPSE + ENTRY_BUGZILLA + ENTRY_END;
	private final String ENTRY_WITH_TOPIC = ENTRY_HEAD_ECLIPSE + TOPIC_BUGZILLA + ENTRY_END;
	private final String ENTRY_WITH_SEE = ENTRY_HEAD_ECLIPSE + SEE_ALSO_SDK + ENTRY_END;

	@Before
	public void setUp() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_WORKBENCH);
	}

	private IndexEntry createEntry(final String elementSource) {
		IndexEntry element;
		Document doc;
		try {
			doc = LocalEntityResolver.parse(elementSource);
		} catch (Exception e) {
			fail("Caught Exception");
			doc = null;
		}
		element = new IndexEntry((Element) doc.getFirstChild());
		return element;
	}

	@Test
	public void testSimpleIndexEntry() {
		IndexEntry entry;
		entry = createEntry(ENTRY_ECLIPSE);
		assertEquals(ECLIPSE, entry.getKeyword());
		assertEquals(0, entry.getTopics().length);
		assertEquals(0, entry.getSubentries().length);
		assertEquals(0, entry.getSees().length);
	}

	@Test
	public void testCopySimpleIndexEntry() {
		IndexEntry entry1;
		entry1 = createEntry(ENTRY_ECLIPSE);
		IndexEntry entry2 = new IndexEntry(entry1);
		assertEquals(ECLIPSE, entry1.getKeyword());
		assertEquals(ECLIPSE, entry2.getKeyword());
	}

	@Test
	public void testCopyIndexEntryWithChild() {
		IndexEntry entry1;
		entry1 = createEntry(ENTRY_WITH_CHILD);
		IndexEntry entry2 = new IndexEntry(entry1);

		assertEquals(1, entry1.getSubentries().length);
		IndexEntry child1 = (IndexEntry)entry1.getSubentries()[0];
		assertEquals(BUGZILLA, child1.getKeyword());

		assertEquals(1, entry2.getSubentries().length);
		IndexEntry child2 = (IndexEntry)entry2.getSubentries()[0];
		assertEquals(BUGZILLA, child2.getKeyword());
		assertEquals(1, entry2.getSubentries().length);
	}

	@Test
	public void testCopyIndexEntryWithTopic() {
		IndexEntry entry1;
		entry1 = createEntry(ENTRY_WITH_TOPIC);
		IndexEntry entry2 = new IndexEntry(entry1);

		assertEquals(0, entry1.getSubentries().length);
		assertEquals(1, entry1.getTopics().length);
		Topic child1 = (Topic)entry1.getTopics()[0];
		assertEquals(BUGZILLA, child1.getLabel());
		assertEquals(BUGZILLA_HREF, child1.getHref());

		assertEquals(0, entry2.getSubentries().length);
		assertEquals(1, entry2.getTopics().length);
		Topic child2 = (Topic)entry2.getTopics()[0];
		assertEquals(BUGZILLA, child2.getLabel());
		assertEquals(BUGZILLA_HREF, child2.getHref());
	}

	@Test
	public void testCopyIndexEntryWithSee() {
		IndexEntry entry1;
		entry1 = createEntry(ENTRY_WITH_SEE);
		IndexEntry entry2 = new IndexEntry(entry1);

		assertEquals(0, entry1.getSubentries().length);
		assertEquals(1, entry1.getSees().length);
		IndexSee child1 = (IndexSee)entry1.getSees()[0];
		assertEquals("sdk", child1.getKeyword());

		assertEquals(0, entry2.getSubentries().length);
		assertEquals(1, entry2.getSees().length);
		IndexSee child2 = (IndexSee)entry2.getSees()[0];
		assertEquals("sdk", child2.getKeyword());
	}

	@Test
	public void testEnabledIndexEntry() {
		IndexEntry entry;
		entry = createEntry(ENTRY_WITH_ENABLEMENT);
		assertTrue(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testDisabledIndexEntry() {
		IndexEntry entry;
		entry = createEntry(ENTRY_NOT_ENABLED);
		assertFalse(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testCopyDisabledIndexEntry() {
		IndexEntry entry1;
		entry1 = createEntry(ENTRY_NOT_ENABLED);
		IndexEntry entry2 = new IndexEntry(entry1);
		IndexEntry entry3 = new IndexEntry(entry2);
		assertFalse(entry1.isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(entry2.isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(entry3.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testCompoundEnablement() {
		IndexEntry entry;
		entry = createEntry(ENTRY_HEAD_ECLIPSE + "<enablement>"
				+ CS_INSTALLED
				+ INVALID_INSTALLED
				+ "</enablement>" + ENTRY_END);
		assertFalse(entry.isEnabled(HelpEvaluationContext.getContext()));
		entry = createEntry(ENTRY_HEAD_ECLIPSE + "<enablement><and>"
				+ INVALID_INSTALLED
				+ CS_INSTALLED
				+ "</and></enablement>" + ENTRY_END);
		assertFalse(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testOldStyleEnablement() {
		IndexEntry entry;
		entry = createEntry(ENTRY_OLD_FILTER);
		assertTrue(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testOldStyleDisabled() {
		IndexEntry entry;
		entry = createEntry(ENTRY_OLD_FILTER_DISABLED);
		assertFalse(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testOldStyleNegated() {
		IndexEntry entry;
		entry = createEntry(ENTRY_OLD_FILTER_IN__NEGATED);
		assertFalse(entry.isEnabled(HelpEvaluationContext.getContext()));
		entry = createEntry(ENTRY_OLD_FILTER_OUT_NEGATED);
		assertTrue(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testCopyOldStyleDisabled() {
		IndexEntry entry1;
		entry1 = createEntry(ENTRY_OLD_FILTER_DISABLED);
		IndexEntry entry2 = new IndexEntry(entry1);
		IndexEntry entry3 = new IndexEntry(entry2);
		assertFalse(entry1.isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(entry2.isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(entry3.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testFilterIn() {
		IndexEntry entry;
		entry = createEntry(ENTRY_FILTER_IN);
		assertTrue(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testFilterOut() {
		IndexEntry entry;
		entry = createEntry(ENTRY_FILTER_OUT);
		assertFalse(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testFilterMixed() {
		IndexEntry entry;
		entry = createEntry(ENTRY_FILTER_MIXED);
		assertFalse(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testNegatedFilters() {
		IndexEntry entry;
		entry = createEntry(ENTRY_HEAD_ECLIPSE + NEGATED_FILTER_IN + ENTRY_END);
		assertFalse(entry.isEnabled(HelpEvaluationContext.getContext()));
		entry = createEntry(ENTRY_HEAD_ECLIPSE + NEGATED_FILTER_OUT + ENTRY_END);
		assertTrue(entry.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testCopyFilterOut() {
		IndexEntry entry1;
		entry1 = createEntry(ENTRY_FILTER_OUT);
		IndexEntry entry2 = new IndexEntry(entry1);
		IndexEntry entry3 = new IndexEntry(entry2);
		assertFalse(entry1.isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(entry2.isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(entry3.isEnabled(HelpEvaluationContext.getContext()));
	}

	@Test
	public void testUserEntry() {
		UserIndexEntry u1 = createUserEntry();
		checkEntryChildEnablement(u1);
		IndexEntry entry = new IndexEntry(u1);
		assertEquals("java", entry.getKeyword());
		assertTrue(entry.isEnabled(HelpEvaluationContext.getContext()));
		checkCreatedEntry(entry);
	}

	@Test
	public void testUserEntryChildEnablement() {
		UserIndexEntry u1 = createUserEntry();
		IndexEntry entry = new IndexEntry(u1);
		assertEquals("java", entry.getKeyword());
		assertTrue(entry.isEnabled(HelpEvaluationContext.getContext()));
		checkEntryChildEnablement(entry);
	}

	@Test
	public void testCopyUserEntry() {
		UserIndexEntry u1 = createUserEntry();
		IndexEntry entry1 = new IndexEntry(u1);
		IndexEntry entry2 = new IndexEntry(entry1);
		checkCreatedEntry(entry1);
		checkCreatedEntry(entry2);
	}

	private void checkCreatedEntry(IIndexEntry2 entry) {
		assertEquals("java", entry.getKeyword());
		assertTrue(entry.isEnabled(HelpEvaluationContext.getContext()));
		IIndexEntry[] subentries = entry.getSubentries();
		ITopic[] topics = entry.getTopics();
		IIndexSee[] sees = entry.getSees();
		assertEquals(2, subentries.length);
		assertEquals(1, sees.length);
		assertEquals(3,topics.length);
		assertEquals("jdt", subentries[0].getKeyword());
		assertEquals("compiler", subentries[1].getKeyword());
		assertEquals("label1", topics[0].getLabel());
		assertEquals("label2", topics[1].getLabel());
		assertEquals("label3", topics[2].getLabel());
		assertEquals("href1", topics[0].getHref());
		assertEquals("href2", topics[1].getHref());
		assertEquals("href3", topics[2].getHref());
		assertEquals("beans", sees[0].getKeyword());
	}

	private void checkEntryChildEnablement(IIndexEntry2 entry) {
		IIndexEntry[] subentries = entry.getSubentries();
		ITopic[] topics = entry.getTopics();
		IIndexSee[] sees = entry.getSees();
		assertEquals(2, subentries.length);
		assertEquals(1, sees.length);
		assertEquals(3,topics.length);
		assertTrue(subentries[0].isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(subentries[1].isEnabled(HelpEvaluationContext.getContext()));
		assertTrue(topics[0].isEnabled(HelpEvaluationContext.getContext()));
		assertFalse(topics[1].isEnabled(HelpEvaluationContext.getContext()));
		assertTrue(topics[2].isEnabled(HelpEvaluationContext.getContext()));
		assertTrue(sees[0].isEnabled(HelpEvaluationContext.getContext()));
	}

	private UserIndexEntry createUserEntry() {
		UserIndexEntry u1;
		u1 = new UserIndexEntry("java", true);
		UserIndexEntry u2 = new UserIndexEntry("jdt", true);
		UserIndexEntry u3 = new UserIndexEntry("compiler", false);
		UserTopic t1 = new UserTopic("label1", "href1", true);
		UserTopic t2 = new UserTopic("label2", "href2", false);
		UserTopic t3 = new UserTopic("label3", "href3", true);
		UserIndexSee s1 = new UserIndexSee("beans", true);
		u1.addEntry(u2);
		u1.addEntry(u3);
		u1.addTopic(t1);
		u1.addTopic(t2);
		u1.addTopic(t3);
		u1.addSee(s1);
		return u1;
	}

}
