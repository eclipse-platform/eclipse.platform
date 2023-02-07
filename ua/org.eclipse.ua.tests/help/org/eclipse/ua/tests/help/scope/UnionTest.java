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

import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.base.scope.UnionScope;
import org.eclipse.ua.tests.help.other.UserTopic;
import org.junit.Test;

public class UnionTest {

	private AbstractHelpScope createScope(char char1, char char2) {
		AbstractHelpScope result = new UnionScope(
				new AbstractHelpScope[]{new MockScope(char1, true),
				new MockScope(char2, true)});
		return result;
	}

	@Test
	public void testInIn() {
		UserTopic topic = new UserTopic("ab", "http://www.eclipse.org", true);
		AbstractHelpScope scope = createScope('a', 'b');
		assertTrue(scope.inScope(topic));
	}

	@Test
	public void testInOut() {
		UserTopic topic = new UserTopic("a", "http://www.eclipse.org", true);
		AbstractHelpScope scope = createScope('a', 'b');
		assertTrue(scope.inScope(topic));
	}

	@Test
	public void testOutIn() {
		UserTopic topic = new UserTopic("b", "http://www.eclipse.org", true);
		AbstractHelpScope scope = createScope('a', 'b');
		assertTrue(scope.inScope(topic));
	}

	@Test
	public void testOutOut() {
		UserTopic topic = new UserTopic("c", "http://www.eclipse.org", true);
		AbstractHelpScope scope = createScope('a', 'b');
		assertFalse(scope.inScope(topic));
	}

}
