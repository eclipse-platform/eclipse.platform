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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.help.IIndexSee;
import org.eclipse.help.IIndexSubpath;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.index.IndexEntry;
import org.eclipse.help.internal.index.IndexSee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IndexSeeTest {

	private static final String AGUILA = "\u00E1guila"; // 00E1 is an accented letter 'a'
	private static final String ECLIPSE = "eclipse";
	private static final String SDK = "sdk";
	private static final String VIEWS = "views";
	private static final String SEE_END = "</see>";
	private static final String SEE_HEAD_ECLIPSE = "<see keyword=\"eclipse\">";
	private static final String SEE_ECLIPSE = "<see keyword=\"eclipse\"/>";
	private static final String SEE_SDK = "<see keyword=\"sdk\"/>";
	private static final String SUBPATH_SDK = "<subpath keyword=\"sdk\">";
	private static final String SUBPATH_VIEWS = "<subpath keyword=\"views\">";
	private static final String SUBPATH_END = "</subpath>";
	private static final String SEE_ECLIPSE_SDK = SEE_HEAD_ECLIPSE +
	SUBPATH_SDK + SUBPATH_END + SEE_END;
	private static final String SEE_ECLIPSE_VIEWS = SEE_HEAD_ECLIPSE +
	SUBPATH_SDK + SUBPATH_END + SUBPATH_VIEWS + SUBPATH_END + SEE_END;
	private static final String SEE_ECLIPSE_SDK_VIEWS = SEE_HEAD_ECLIPSE +
	SUBPATH_SDK + SUBPATH_END + SUBPATH_VIEWS + SUBPATH_END + SEE_END;

	@BeforeEach
	public void setUp() throws Exception {
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_WORKBENCH);
	}

	private IndexSee createSee(final String elementSource) {
		IndexSee element;
		Document doc;
		try {
			doc = LocalEntityResolver.parse(elementSource);
		} catch (Exception e) {
			fail("Caught Exception");
			doc = null;
		}
		element = new IndexSee((Element) doc.getFirstChild());
		return element;
	}

	private IndexSee createSimpleSee(final String keyword) {
		IndexSee element;
		Document doc;
		String elementSource = "<see keyword=\"" + keyword + "\" />";
		try {
			doc = LocalEntityResolver.parse(elementSource);
		} catch (Exception e) {
			fail("Caught Exception");
			doc = null;
		}
		element = new IndexSee((Element) doc.getFirstChild());
		return element;
	}

	@Test
	public void testSimpleIndexSee() {
		IndexSee see;
		see = createSee(SEE_ECLIPSE);
		assertEquals(ECLIPSE, see.getKeyword());

	}

	@Test
	public void testCopySimpleIndexSee() {
		IndexSee see1;
		see1 = createSee(SEE_ECLIPSE);
		IndexSee see2 = new IndexSee(see1);
		assertEquals(ECLIPSE, see1.getKeyword());
		assertThat(see1.getSubpathElements()).isEmpty();
		assertEquals(ECLIPSE, see1.getKeyword());

		assertEquals(ECLIPSE, see2.getKeyword());
		assertThat(see2.getSubpathElements()).isEmpty();
		assertEquals(ECLIPSE, see2.getKeyword());
	}

	@Test
	public void testCopyIndexSeeWithSubpath() {
		IndexSee see1;
		see1 = createSee(SEE_ECLIPSE_SDK);
		IndexSee see2 = new IndexSee(see1);

		assertThat(see1.getSubpathElements()).hasSize(1);
		assertEquals(ECLIPSE, see1.getKeyword());
		assertEquals(SDK, see1.getSubpathElements()[0].getKeyword());

		assertThat(see2.getSubpathElements()).hasSize(1);
		assertEquals(ECLIPSE, see2.getKeyword());
		assertEquals(SDK, see2.getSubpathElements()[0].getKeyword());

	}

	@Test
	public void testCopyIndexSeeWithLongerSubpath() {
		IndexSee see1;
		see1 = createSee(SEE_ECLIPSE_SDK_VIEWS);
		IndexSee see2 = new IndexSee(see1);

		assertThat(see1.getSubpathElements()).hasSize(2);
		assertEquals(ECLIPSE, see1.getKeyword());
		assertEquals(SDK, see1.getSubpathElements()[0].getKeyword());
		assertEquals(VIEWS, see1.getSubpathElements()[1].getKeyword());

		assertThat(see2.getSubpathElements()).hasSize(2);
		assertEquals(ECLIPSE, see2.getKeyword());
		assertEquals(SDK, see2.getSubpathElements()[0].getKeyword());
		assertEquals(VIEWS, see2.getSubpathElements()[1].getKeyword());
	}

	@Test
	public void testCompareSimpleSame() {
		IndexSee see1 = createSee(SEE_ECLIPSE);
		IndexSee see2 = createSee(SEE_ECLIPSE);
		assertTrue (see1.equals(see2));
		assertEquals(0, see1.compareTo(see2));
		assertEquals(0, see2.compareTo(see1));
		assertEquals(see1.hashCode(), see2.hashCode());
	}

	@Test
	public void testCompareSimpleDifferent() {
		IndexSee see1 = createSee(SEE_ECLIPSE);
		IndexSee see2 = createSee(SEE_SDK);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	@Test
	public void testCompareCompoundSame() {
		IndexSee see1 = createSee(SEE_ECLIPSE_SDK);
		IndexSee see2 = createSee(SEE_ECLIPSE_SDK);
		assertTrue (see1.equals(see2));
		assertEquals(0, see1.compareTo(see2));
		assertEquals(0, see2.compareTo(see1));
		assertEquals(see1.hashCode(), see2.hashCode());
	}

	@Test
	public void testCompareCompoundDifferent() {
		IndexSee see1 = createSee(SEE_ECLIPSE_SDK);
		IndexSee see2 = createSee(SEE_ECLIPSE_VIEWS);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	@Test
	public void testCompareCompoundDifferentLengths() {
		IndexSee see1 = createSee(SEE_ECLIPSE_SDK);
		IndexSee see2 = createSee(SEE_ECLIPSE_SDK_VIEWS);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	@Test
	public void testCompare_AAA_abacus() {
		IndexSee see1 = createSimpleSee("AAA");
		IndexSee see2 = createSimpleSee("abacus");
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	@Test
	public void testCompare_abacus_ABC() {
		IndexSee see1 = createSimpleSee("abacus");
		IndexSee see2 = createSimpleSee(AGUILA);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	@Test
	public void testCompare_ABC_aguila() {
		IndexSee see1 = createSimpleSee("ABC");
		IndexSee see2 = createSimpleSee(AGUILA);
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	@Test
	public void testCompare_aguila_axe() {
		IndexSee see1 = createSimpleSee(AGUILA);
		IndexSee see2 = createSimpleSee("axe");
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) < 0);
		assertTrue(see2.compareTo(see1) > 0);
	}

	@Test
	public void testCompare_to_underscore() {
		IndexSee see1 = createSimpleSee("abc");
		IndexSee see2 = createSimpleSee("_xyz");
		assertFalse (see1.equals(see2));
		assertTrue(see1.compareTo(see2) > 0);
		assertTrue(see2.compareTo(see1) < 0);
	}

	@Test
	public void testUserSee() {
		UserIndexSee u1;
		u1 = createUserSee();
		IndexSee see = new IndexSee(u1);
		checkCreatedSee(see);
	}

	@Test
	public void testCopyUserSee() {
		UserIndexSee u1;
		u1 = createUserSee();
		IndexSee see = new IndexSee(u1);
		IndexSee see2 = new IndexSee(see);
		checkCreatedSee(see);
		checkCreatedSee(see2);
	}

	@Test
	public void testCreateTwiceUserSee() {
		UserIndexSee u1;
		u1 = createUserSee();
		IndexSee see = new IndexSee(u1);
		IndexSee see2 = new IndexSee(u1);
		checkCreatedSee(see);
		checkCreatedSee(see2);
	}

	@Test
	public void testSeeAlsoWithSiblingTopic() {
		UserIndexEntry entry = new UserIndexEntry("test", true);
		UserTopic topic = new UserTopic("label", "href.html", true);
		entry.addTopic(topic);
		UserIndexSee see = new UserIndexSee("check", true);
		entry.addSee(see);
		IndexEntry indexEntry = new IndexEntry(entry);
		IIndexSee[] sees = indexEntry.getSees();
		assertTrue(((IndexSee)sees[0]).isSeeAlso());
	}

	@Test
	public void testSeeAlsoWithSiblingEntry() {
		UserIndexEntry entry = new UserIndexEntry("test", true);
		UserIndexEntry subEntry = new UserIndexEntry("case", true);
		UserTopic topic = new UserTopic("label", "href.html", true);
		entry.addEntry(subEntry);
		subEntry.addTopic(topic);
		UserIndexSee see = new UserIndexSee("check", true);
		entry.addSee(see);
		IndexEntry indexEntry = new IndexEntry(entry);
		IIndexSee[] sees = indexEntry.getSees();
		assertTrue(((IndexSee)sees[0]).isSeeAlso());
	}

	@Test
	public void testSiblingSeesNotSeeAlso() {
		UserIndexEntry entry = new UserIndexEntry("test", true);
		UserIndexSee see1 = new UserIndexSee("check", true);
		entry.addSee(see1);
		UserIndexSee see2 = new UserIndexSee("verify", true);
		entry.addSee(see2);
		IndexEntry indexEntry = new IndexEntry(entry);
		IIndexSee[] sees = indexEntry.getSees();
		assertFalse(((IndexSee)sees[0]).isSeeAlso());
		assertFalse(((IndexSee)sees[1]).isSeeAlso());
	}

	private UserIndexSee createUserSee() {
		UserIndexSee u1;
		u1 = new UserIndexSee("eclipse", false);
		UserIndexSubpath u2 = new UserIndexSubpath("platform");
		UserIndexSubpath u3 = new UserIndexSubpath("ui");
		u1.addSubpath(u2);
		u1.addSubpath(u3);
		return u1;
	}

	private void checkCreatedSee(IndexSee see) {
		assertEquals("eclipse", see.getKeyword());
		IIndexSubpath[] subpath = see.getSubpathElements();
		assertThat(subpath).hasSize(2);
		assertEquals("platform", subpath[0].getKeyword());
		assertEquals("ui", subpath[1].getKeyword());
	}

}
