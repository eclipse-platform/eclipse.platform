/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.core.tests.session.samples;

import junit.framework.*;

public class SameSessionTest extends TestCase {

	static String lastTestCase;

	public SameSessionTest(String name) {
		super(name);
	}

	public void test1() {
		lastTestCase = getName();
	}

	public void test2() {
		assertEquals("test1", lastTestCase);
		lastTestCase = getName();
	}

	public void test3() {
		assertEquals("test2", lastTestCase);
	}

	public static Test suite() {
		TestSuite root = new TestSuite("root");
		TestSuite node1 = new TestSuite("node1");
		node1.addTest(new SameSessionTest("test1"));
		root.addTest(node1);
		TestSuite node2 = new TestSuite("node2");
		root.addTest(node2);
		TestSuite node21 = new TestSuite("node21");
		node2.addTest(node21);
		node21.addTest(new SameSessionTest("test2"));
		node2.addTest(new SameSessionTest("test3"));
		return root;
	}
}
