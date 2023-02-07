/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.scope;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test that leaf topics with no href do not show in the tree
 * In this test empty means has no href
 */

import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.base.scope.ScopeUtils;
import org.eclipse.ua.tests.help.other.UserIndexEntry;
import org.eclipse.ua.tests.help.other.UserToc;
import org.eclipse.ua.tests.help.other.UserTopic;
import org.junit.Test;

public class EmptyLeafRemoval {
	@Test
	public void testEmptyLeafTopic() {
		UserTopic topic = new UserTopic("ab", null, true);
		assertFalse(ScopeUtils.showInTree(topic, new MockScope('a', false)));
	}

	@Test
	public void testEmptyParentOfEmptyLeafTopic() {
		UserTopic topic = new UserTopic("ab", null, true);
		UserTopic child = new UserTopic("ac", null, true);
		topic.addTopic(child);
		assertFalse(ScopeUtils.showInTree(topic, new MockScope('a', false)));
	}

	@Test
	public void testEmptyParentOfNonEmptyLeafTopic() {
		UserTopic topic = new UserTopic("ab", null, true);
		UserTopic child = new UserTopic("ac", "http://www.eclipse.org", true);
		topic.addTopic(child);
		assertTrue(ScopeUtils.showInTree(topic, new MockScope('a', false)));
	}

	@Test
	public void testEmptyParentOfNonEmptyLeafTopicHierarchical() {
		UserTopic topic = new UserTopic("ab", null, true);
		UserTopic child = new UserTopic("ac", "http://www.eclipse.org", true);
		topic.addTopic(child);
		assertTrue(ScopeUtils.showInTree(topic, new MockScope('a', true)));
	}

	@Test
	public void testEmptyGrandParentWithOutOfScopeChildTopicHierarchical() {
		UserTopic topic = new UserTopic("ab", null, true);
		UserTopic child = new UserTopic("c", "http://www.eclipse.org", true);
		UserTopic grandChild = new UserTopic("ac", "http://www.eclipse.org", true);
		topic.addTopic(child);
		child.addTopic(grandChild);
		assertFalse(ScopeUtils.showInTree(topic, new MockScope('a', true)));
	}

	@Test
	public void testEmptyGrandparentOfNonEmptyLeafTopicHierarchical() {
		UserTopic topic = new UserTopic("ab", null, true);
		UserTopic child = new UserTopic("ac", null, true);
		UserTopic grandChild = new UserTopic("ac", "http://www.eclipse.org", true);
		topic.addTopic(child);
		child.addTopic(grandChild);
		assertTrue(ScopeUtils.showInTree(topic, new MockScope('a', true)));
	}

	@Test
	public void testTocParentOfEmptyLeaf() {
		UserToc toc = new UserToc("ab", "http://www.eclipse.org", true);
		UserTopic child = new UserTopic("ac", null, true);
		toc.addTopic(child);
		AbstractHelpScope scope = new MockScope('a', false);
		assertFalse(ScopeUtils.showInTree(toc, scope));
	}

	@Test
	public void testTocParentOfNonEmptyLeaf() {
		UserToc toc = new UserToc("ab", "http://www.eclipse.org", true);
		UserTopic child = new UserTopic("ac", "http://www.eclipse.org", true);
		toc.addTopic(child);
		AbstractHelpScope scope = new MockScope('a', true);
		assertTrue(ScopeUtils.showInTree(toc, scope));
	}

	@Test
	public void testEntryParentOfEmptyLeaf() {
		UserIndexEntry entry = new UserIndexEntry("ab", true);
		UserTopic child = new UserTopic("ac", null, true);
		entry.addTopic(child);
		AbstractHelpScope scope = new MockScope('a', true);
		assertFalse(ScopeUtils.showInTree(entry, scope));
	}

	@Test
	public void testEntryParentOfNonEmptyLeaf() {
		UserIndexEntry entry = new UserIndexEntry("ab", true);
		UserTopic child = new UserTopic("ac", "http://www.eclipse.org", true);
		entry.addTopic(child);
		AbstractHelpScope scope = new MockScope('a', true);
		assertTrue(ScopeUtils.showInTree(entry, scope));
	}

	@Test
	public void testEntryGrandParentOfNonEmptyLeaf() {
		UserIndexEntry entry = new UserIndexEntry("ab", true);
		UserIndexEntry child = new UserIndexEntry("ab", true);
		UserTopic grandhild = new UserTopic("ac", "http://www.eclipse.org", true);
		entry.addEntry(child);
		child.addTopic(grandhild);
		AbstractHelpScope scope = new MockScope('a', true);
		assertTrue(ScopeUtils.showInTree(entry, scope));
	}

}
