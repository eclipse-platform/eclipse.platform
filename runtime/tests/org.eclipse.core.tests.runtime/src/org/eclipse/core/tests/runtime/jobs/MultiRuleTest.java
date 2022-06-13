/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime.jobs;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;

/**
 * Tests for {@link MultiRule}.
 */
public class MultiRuleTest extends AbstractJobTest {

	public void testCombine() {
		ISchedulingRule child1 = new PathRule("/a");
		ISchedulingRule child2 = new PathRule("/b/c");
		ISchedulingRule childOfChild1 = new PathRule("/a/b");
		ISchedulingRule nonChild = new PathRule("/z/d");
		MultiRule multi1 = new MultiRule(new ISchedulingRule[] {child1, child2});

		//add multi to its own children
		assertEquals("1.0", multi1, MultiRule.combine(new ISchedulingRule[] {multi1}));
		assertEquals("1.1", multi1, MultiRule.combine(new ISchedulingRule[] {multi1, child1, child2, childOfChild1}));
		assertEquals("1.2", multi1, MultiRule.combine(multi1, child2));
		assertEquals("1.3", multi1, MultiRule.combine(childOfChild1, multi1));

		//null
		assertEquals("1.4", null, MultiRule.combine(null, null));
		assertEquals("1.5", multi1, MultiRule.combine(null, multi1));
		assertEquals("1.6", child1, MultiRule.combine(child1, null));

		MultiRule result = (MultiRule) MultiRule.combine(multi1, nonChild);
		assertTrue("2.0" + result, result.contains(multi1));
		assertTrue("2.1", result.contains(nonChild));

	}

	public void testContains() {
		ISchedulingRule child1 = new PathRule("/a");
		ISchedulingRule child2 = new PathRule("/b/c");
		ISchedulingRule childOfChild1 = new PathRule("/a/b");
		ISchedulingRule nonChild = new PathRule("/z/d");
		MultiRule multi1 = new MultiRule(new ISchedulingRule[] {child1, child2});
		MultiRule multi2 = new MultiRule(new ISchedulingRule[] {childOfChild1});

		assertTrue("1.0", multi1.contains(child1));
		assertTrue("1.1", multi1.contains(child2));
		assertTrue("1.2", !multi1.contains(nonChild));
		assertTrue("1.3", multi1.contains(childOfChild1));
		assertTrue("1.4", multi1.contains(multi2));
		assertTrue("1.5", !multi2.contains(multi1));
		assertTrue("1.6", multi1.contains(multi1));
	}

	public void testIsConflicting() {
		ISchedulingRule child1 = new PathRule("/a");
		ISchedulingRule child2 = new PathRule("/b/c");
		ISchedulingRule childOfChild1 = new PathRule("/a/b");
		ISchedulingRule nonChild = new PathRule("/z/d");
		MultiRule multi1 = new MultiRule(new ISchedulingRule[] {child1, child2});
		MultiRule multi2 = new MultiRule(new ISchedulingRule[] {childOfChild1, nonChild});

		assertTrue("1.0", multi1.isConflicting(child1));
		assertTrue("1.1", multi1.isConflicting(child2));
		assertTrue("1.2", !multi1.isConflicting(nonChild));
		assertTrue("1.3", multi1.isConflicting(childOfChild1));
		assertTrue("1.4", multi1.isConflicting(multi2));
		assertTrue("1.5", multi2.isConflicting(multi1));
		assertTrue("1.6", multi1.isConflicting(multi1));
	}
}
