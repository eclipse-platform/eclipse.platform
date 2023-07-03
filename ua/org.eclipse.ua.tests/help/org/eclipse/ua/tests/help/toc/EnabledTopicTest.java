/*******************************************************************************
 *  Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.toc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.webapp.data.EnabledTopicUtils;
import org.junit.Test;

public class EnabledTopicTest {

	private static class ETopic implements ITopic {

		private String label;
		private boolean isEnabled;
		private List<ITopic> children = new ArrayList<>();

		public ETopic(String label, boolean isEnabled) {
			this.label = label;
			this.isEnabled = isEnabled;
		}

		@Override
		public ITopic[] getSubtopics() {
			return children.toArray(new ITopic[children.size()]);
		}

		@Override
		public IUAElement[] getChildren() {

			return getSubtopics();
		}

		@Override
		public boolean isEnabled(IEvaluationContext context) {
			return isEnabled;
		}

		@Override
		public String getHref() {
			return "http://www.eclipse.org";
		}

		@Override
		public String getLabel() {
			return label;
		}

		public void addSubTopic(ITopic subTopic) {
			children.add(subTopic);
		}
	}

	private static class NoHrefTopic extends ETopic {

		public NoHrefTopic(String label) {
			super(label, true);
		}

		@Override
		public String getHref() {
			return null;
		}

	}

	private static class EIndexEntry extends UAElement implements IIndexEntry  {

		private String keyword;
		private List<ITopic> topics = new ArrayList<>();
		private List<IIndexEntry> subEntries = new ArrayList<>();

		public EIndexEntry(String keyword) {
			super(keyword);
			this.keyword = keyword;
		}

		@Override
		public String getKeyword() {
			return keyword;
		}

		public void addSubEntry(IIndexEntry entry) {
			subEntries.add(entry);
		}

		public void addTopic(ITopic topic) {
			topics.add(topic);
		}

		@Override
		public IIndexEntry[] getSubentries() {
			return subEntries.toArray(new IIndexEntry[subEntries.size()]);
		}

		@Override
		public ITopic[] getTopics() {
			return topics.toArray(new ITopic[topics.size()]);
		}

		@Override
		public synchronized IUAElement[] getChildren() {
			List<IUAElement> all = new ArrayList<>();
			all.addAll(subEntries);
			all.addAll(topics);
			return all.toArray(new IUAElement[all.size()]);
		}
	}

	@Test
	public void testEnabledTopic() {
		assertTrue(EnabledTopicUtils.isEnabled(new ETopic("T1", true)));
		assertFalse(EnabledTopicUtils.isEnabled(new ETopic("T2", false)));
	}

	@Test
	public void testEnabledTopicsEmptyArray() throws Exception {
		ITopic[] enabled = EnabledTopicUtils.getEnabled(new ITopic[0]);
		assertTrue(enabled.length == 0);
	}

	@Test
	public void testEnabledTopicsAllEnabled() throws Exception {
		ITopic[] topics = new ITopic[2];
		topics[0] = new ETopic("T1", true);
		topics[1] = new ETopic("T2", true);
		ITopic[] enabled = EnabledTopicUtils.getEnabled(topics);
		assertTrue(enabled.length == 2);
		assertTrue(topics[0].getLabel().equals("T1"));
		assertTrue(topics[1].getLabel().equals("T2"));
	}

	@Test
	public void testEnabledTopicsAllDisabled() throws Exception {
		ITopic[] topics = new ITopic[2];
		topics[0] = new ETopic("T1", false);
		topics[1] = new ETopic("T2", false);
		ITopic[] enabled = EnabledTopicUtils.getEnabled(topics);
		assertTrue(enabled.length == 0);
	}

	@Test
	public void testEnabledTopicsMix() throws Exception {
		ITopic[] topics = new ITopic[4];
		topics[0] = new ETopic("T1", true);
		topics[1] = new ETopic("T2", false);
		topics[2] = new ETopic("T3", true);
		topics[3] = new ETopic("T4", false);
		ITopic[] enabled = EnabledTopicUtils.getEnabled(topics);
		assertEquals(2, enabled.length);
		assertEquals("T1", enabled[0].getLabel());
		assertEquals("T3", enabled[1].getLabel());
	}

	@Test
	public void testNoHref() {
		ITopic noHref = new NoHrefTopic("N1");
		assertFalse(EnabledTopicUtils.isEnabled(noHref));
	}

	@Test
	public void testNoHrefValidChild() {
		ETopic noHref = new NoHrefTopic("N1");
		noHref.addSubTopic(new ETopic("T1", true));
		assertTrue(EnabledTopicUtils.isEnabled(noHref));
	}

	@Test
	public void testNoHrefInvalidChild() {
		ETopic noHref = new NoHrefTopic("N1");
		noHref.addSubTopic(new ETopic("T1", false));
		assertFalse(EnabledTopicUtils.isEnabled(noHref));
	}

	@Test
	public void testNoHrefMixedChildren() {
		ETopic noHref = new NoHrefTopic("N1");
		noHref.addSubTopic(new ETopic("T1", false));
		noHref.addSubTopic(new ETopic("T2", true));
		assertTrue(EnabledTopicUtils.isEnabled(noHref));
	}

	@Test
	public void testNoHrefValidGrandchild() {
		ETopic noHref = new NoHrefTopic("N1");
		ETopic subTopic = new NoHrefTopic("N2");
		noHref.addSubTopic(subTopic);
		subTopic.addSubTopic(new ETopic("T2", true));
		assertTrue(EnabledTopicUtils.isEnabled(noHref));
	}

	@Test
	public void testNoHrefInvalidGrandchild() {
		ETopic noHref = new NoHrefTopic("N1");
		ETopic subTopic = new NoHrefTopic("N2");
		noHref.addSubTopic(subTopic);
		subTopic.addSubTopic(new ETopic("T2", false));
		assertFalse(EnabledTopicUtils.isEnabled(noHref));
	}

	@Test
	public void testEmptyIndexEntry() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		assertFalse(EnabledTopicUtils.isEnabled(entry1));
	}

	@Test
	public void testEnabledIndexEntry() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		entry1.addTopic(new ETopic("T1", true));
		assertTrue(EnabledTopicUtils.isEnabled(entry1));
	}

	@Test
	public void testDisabledIndexEntry() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		entry1.addTopic(new ETopic("T1", false));
		assertFalse(EnabledTopicUtils.isEnabled(entry1));
	}

	@Test
	public void testMixedIndexEntry() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		entry1.addTopic(new ETopic("T1", true));
		entry1.addTopic(new ETopic("T2", false));
		assertTrue(EnabledTopicUtils.isEnabled(entry1));
	}

	@Test
	public void testIndexEntryEnabledChild() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		EIndexEntry entry2 = new EIndexEntry("def");
		entry2.addTopic(new ETopic("T1", true));
		entry1.addSubEntry(entry2);
		assertTrue(EnabledTopicUtils.isEnabled(entry1));
	}

	@Test
	public void testIndexEntryEnabledGrandChild() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		EIndexEntry entry2 = new EIndexEntry("def");
		EIndexEntry entry3 = new EIndexEntry("ghi");
		entry1.addSubEntry(entry2);
		entry2.addSubEntry(entry3);
		entry3.addTopic(new ETopic("T1", true));
		assertTrue(EnabledTopicUtils.isEnabled(entry1));
	}

	@Test
	public void testIndexEntryDisabledChild() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		EIndexEntry entry2 = new EIndexEntry("def");
		entry2.addTopic(new ETopic("T1", false));
		entry1.addSubEntry(entry2);
		assertFalse(EnabledTopicUtils.isEnabled(entry1));
	}

	@Test
	public void testIndexEntryMixedChildren() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		EIndexEntry entry2 = new EIndexEntry("def");
		EIndexEntry entry3 = new EIndexEntry("ghi");
		entry2.addTopic(new ETopic("T1", false));
		entry3.addTopic(new ETopic("T2", true));
		entry1.addSubEntry(entry2);
		entry1.addSubEntry(entry3);
		assertTrue(EnabledTopicUtils.isEnabled(entry1));
	}

	@Test
	public void testEnabledIndexArrayEmpty() {
		IIndexEntry[] entries = new EIndexEntry[0];
		IIndexEntry[] filtered =EnabledTopicUtils.getEnabled(entries);
		assertEquals(0, filtered.length);
	}

	@Test
	public void testEnabledIndexArrayDisabled() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		EIndexEntry entry2 = new EIndexEntry("def");
		IIndexEntry[] entries = new EIndexEntry[]{entry1, entry2};
		IIndexEntry[] filtered =EnabledTopicUtils.getEnabled(entries);
		assertEquals(0, filtered.length);
	}

	@Test
	public void testEnabledIndexArrayEnabled() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		EIndexEntry entry2 = new EIndexEntry("def");
		entry1.addTopic(new ETopic("T1", true));
		entry2.addTopic(new ETopic("T2", true));
		IIndexEntry[] entries = new EIndexEntry[]{entry1, entry2};
		IIndexEntry[] filtered =EnabledTopicUtils.getEnabled(entries);
		assertEquals(2, filtered.length);
		assertEquals(filtered[0].getKeyword(), "abc");
		assertEquals(filtered[1].getKeyword(), "def");
	}

	@Test
	public void testEnabledIndexArrayMixed() {
		EIndexEntry entry1 = new EIndexEntry("abc");
		EIndexEntry entry2 = new EIndexEntry("def");
		EIndexEntry entry3 = new EIndexEntry("ghi");
		EIndexEntry entry4 = new EIndexEntry("jkl");
		entry2.addTopic(new ETopic("T1", true));
		entry4.addTopic(new ETopic("T2", true));
		IIndexEntry[] entries = new EIndexEntry[]{entry1, entry2, entry3, entry4};
		IIndexEntry[] filtered =EnabledTopicUtils.getEnabled(entries);
		assertEquals(2, filtered.length);
		assertEquals(filtered[0].getKeyword(), "def");
		assertEquals(filtered[1].getKeyword(), "jkl");
	}

}
