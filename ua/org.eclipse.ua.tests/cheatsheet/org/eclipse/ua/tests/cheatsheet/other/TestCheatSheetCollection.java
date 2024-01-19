/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.cheatsheet.other;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetCollectionElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCheatSheetCollection {

	private CheatSheetCollectionElement root;
	private CheatSheetElement csA;
	private CheatSheetElement csB;
	private CheatSheetElement cs12A;
	private CheatSheetElement cs2A;
	private CheatSheetCollectionElement c1;
	private CheatSheetCollectionElement c2;
	private CheatSheetCollectionElement c11;
	private CheatSheetCollectionElement c12;

	@BeforeEach
	public void setUp() throws Exception {
		root = new CheatSheetCollectionElement("rootPlugin", "rootId", "rootName", root);
		csA = new CheatSheetElement("A");
		csA.setID("idA");
		csB = new CheatSheetElement("B");
		csB.setID("idB");
		cs12A = new CheatSheetElement("12A");
		cs12A.setID("id12A");
		cs2A = new CheatSheetElement("2A");
		cs2A.setID("id2A");
		c1 = new CheatSheetCollectionElement("p1", "c1Id", "c1", root);
		c2 = new CheatSheetCollectionElement("p2", "c2Id", "c2", root);
		c11 = new CheatSheetCollectionElement("p11", "c11Id", "c11", c1);
		c12 = new CheatSheetCollectionElement("p12", "c12Id", "c12", c1);
		root.add(c1);
		root.add(csA);
		root.add(csB);
		root.add(c2);
		c1.add(c11);
		c1.add(c12);
		c2.add(cs2A);
		c12.add(cs12A);
	}

	@Test
	public void testRoot() {
		assertThat(root.getChildren()).hasSize(2);
		assertThat(root.getCheatSheets()).hasSize(2);
		assertFalse(root.isEmpty());
		assertEquals("rootName", root.getLabel(null));
		assertEquals("rootId", root.getId());
		assertEquals("rootPlugin", root.getPluginId());
	}

	@Test
	public void testTopLevelChildCategories() {
		Object[] children = root.getChildren();
		assertEquals(c1, children[0]);
		assertEquals(c2, children[1]);
		assertThat(c1.getChildren()).hasSize(2);
		assertThat(c1.getCheatSheets()).isEmpty();
		assertFalse(c1.isEmpty());
		assertThat(c2.getChildren()).isEmpty();
		assertThat(c2.getCheatSheets()).hasSize(1);
		assertFalse(c2.isEmpty());
	}

	@Test
	public void testTopLevelCheatsheets() {
		Object[] cheatsheets = root.getCheatSheets();
		assertEquals(csA, cheatsheets[0]);
		assertEquals(csB, cheatsheets[1]);
	}

	@Test
	public void testSecondLevelChildCategories() {
		Object[] children = c1.getChildren();
		assertEquals(c11, children[0]);
		assertEquals(c12, children[1]);
		assertThat(c11.getChildren()).isEmpty();
		assertThat(c11.getCheatSheets()).isEmpty();
		assertTrue(c11.isEmpty());
		assertThat(c12.getChildren()).isEmpty();
		assertThat(c12.getCheatSheets()).hasSize(1);
		assertFalse(c12.isEmpty());
	}

	@Test
	public void testFind() {
		assertEquals(csA, root.findCheatSheet("idA", true));
		assertEquals(csA, root.findCheatSheet("idA", false));
		assertNull(root.findCheatSheet("idC", true));
		assertEquals(cs12A, root.findCheatSheet("id12A", true));
		assertNull(root.findCheatSheet("id12A", false));
	}
}
