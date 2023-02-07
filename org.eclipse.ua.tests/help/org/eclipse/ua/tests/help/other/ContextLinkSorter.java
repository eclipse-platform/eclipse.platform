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

import org.eclipse.help.IContext2;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.ui.internal.views.ContextHelpSorter;
import org.junit.Test;

public class ContextLinkSorter {

	private static class TestResource implements IHelpResource {

		private String category;
		private String href;
		private String label;

		public TestResource(String label, String category, String href) {
			this.category = category;
			this.href = href;
			this.label = label;
		}

		@Override
		public String getHref() {
			return href;
		}

		@Override
		public String getLabel() {
			return label;
		}

		public String getCategory() {
			return category;
		}

	}

	private static class TestContext implements IContext2 {

		@Override
		public String getCategory(IHelpResource topic) {
			if (topic instanceof TestResource) {
				return ((TestResource)topic).getCategory();
			}
			return null;
		}

		@Override
		public String getStyledText() {
			return null;
		}

		@Override
		public String getTitle() {
			return null;
		}

		@Override
		public IHelpResource[] getRelatedTopics() {
			return null;
		}

		@Override
		public String getText() {
			return null;
		}

	}

	@Test
	public void testSortEmptyArray() {
		ContextHelpSorter sorter = new ContextHelpSorter(new TestContext());
		TestResource[] resources = new TestResource[0];
		sorter.sort(null, resources);
		assertEquals(0, resources.length);
	}

	@Test
	public void testSortSingleCategory() {
		ContextHelpSorter sorter = new ContextHelpSorter(new TestContext());
		TestResource[] resources = {
				new TestResource("a1", "c1", "http://www.a1.com"),
				new TestResource("a3", "c1", "http://www.a3.com"),
				new TestResource("a2", "c1", "http://www.a2.com")
		};
		sorter.sort(null, resources);
		assertEquals(3, resources.length);
		assertEquals("a1", resources[0].getLabel());
		assertEquals("a3", resources[1].getLabel());
		assertEquals("a2", resources[2].getLabel());
	}

	@Test
	public void testSortMultipleCategory() {
		ContextHelpSorter sorter = new ContextHelpSorter(new TestContext());
		TestResource[] resources = {
				new TestResource("a1", "c1", "http://www.a1.com"),
				new TestResource("a3", "c2", "http://www.a3.com"),
				new TestResource("a2", "c1", "http://www.a2.com"),
				new TestResource("a9", "c1", "http://www.a1.com"),
				new TestResource("a5", null, "http://www.a3.com"),
				new TestResource("a4", "c2", "http://www.a3.com"),
				new TestResource("a0", "c3", "http://www.a3.com"),
				new TestResource("a7", null, "http://www.a2.com")
		};
		sorter.sort(null, resources);
		assertEquals(8, resources.length);
		assertEquals("a1", resources[0].getLabel());
		assertEquals("a2", resources[1].getLabel());
		assertEquals("a9", resources[2].getLabel());
		assertEquals("a3", resources[3].getLabel());
		assertEquals("a4", resources[4].getLabel());
		assertEquals("a5", resources[5].getLabel());
		assertEquals("a7", resources[6].getLabel());
		assertEquals("a0", resources[7].getLabel());
	}

}
