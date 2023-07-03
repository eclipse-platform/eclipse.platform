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

package org.eclipse.ua.tests.cheatsheet.other;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetCollectionElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.junit.Test;

public class TestCheatSheetCategories {

	private static final String TEST_CATEGORY = "org.eclipse.ua.tests.cheatsheet.cheatSheetsTestCat";

	@Test
	public void testForCollection() {
		CheatSheetCollectionElement cheatSheets =
			CheatSheetRegistryReader.getInstance().getCheatSheets();
		Object[] subCategories = cheatSheets.getChildren();
		for (Object subCategorie : subCategories) {
			assertTrue(subCategorie instanceof CheatSheetCollectionElement);
		}
	}

	@Test
	public void testFindTestCategory() {
		CheatSheetCollectionElement cheatSheets =
			CheatSheetRegistryReader.getInstance().getCheatSheets();
		CheatSheetCollectionElement testCat = findChildCategory(cheatSheets, TEST_CATEGORY);
		assertNotNull("Cannot find category org.eclipse.ua.tests.cheatsheet.cheatSheetsTestCat",
							testCat);
	}

	@Test
	public void testFindQualifiedSubcategory() {
		CheatSheetCollectionElement cheatSheets =
			CheatSheetRegistryReader.getInstance().getCheatSheets();
		CheatSheetCollectionElement testCat = findChildCategory(cheatSheets, TEST_CATEGORY);
		CheatSheetCollectionElement subCat = findChildCategory(testCat,
				"org.eclipse.ua.tests.subcategory");
		assertNotNull(subCat);
	}

	@Test
	public void testFindCsInUnqualifiedSubcategory() {
		CheatSheetCollectionElement cheatSheets =
			CheatSheetRegistryReader.getInstance().getCheatSheets();
		CheatSheetCollectionElement testCat = findChildCategory(cheatSheets, TEST_CATEGORY);
		CheatSheetCollectionElement subCat = findChildCategory(testCat,
				"org.eclipse.ua.tests.subcategory");
		CheatSheetElement unqual = findCheatsheet(subCat,
				"org.eclipse.ua.tests.cheatsheet.subcategory.simple");
		assertNotNull(unqual);
	}

	@Test
	public void testFindCsInQualifiedSubcategory() {
		CheatSheetCollectionElement cheatSheets =
			CheatSheetRegistryReader.getInstance().getCheatSheets();
		CheatSheetCollectionElement testCat = findChildCategory(cheatSheets, TEST_CATEGORY);
		CheatSheetCollectionElement subCat = findChildCategory(testCat,
				"org.eclipse.ua.tests.subcategory");
		CheatSheetElement qual = findCheatsheet(subCat,
				"org.eclipse.ua.tests.cheatsheet.subcategory.qualified");
		assertNotNull(qual);
	}

	private CheatSheetCollectionElement findChildCategory(
			CheatSheetCollectionElement collection, String id) {
		Object[] subCategories = collection.getChildren();
		for (Object subCategorie : subCategories) {
			 CheatSheetCollectionElement child = (CheatSheetCollectionElement)subCategorie;
			 if (child.getId().equals(id)) {
				 return child;
			 }
		}
		return null;
	}
	private CheatSheetElement findCheatsheet(
			CheatSheetCollectionElement collection, String id) {
		Object[] cheatSheets = collection.getCheatSheets();
		for (Object cheatSheet : cheatSheets) {
			 CheatSheetElement child = (CheatSheetElement)cheatSheet;
			 if (child.getID().equals(id)) {
				 return child;
			 }
		}
		return null;
	}

}
