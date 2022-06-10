/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestCase;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.session.SessionTestSuite;

public class SampleCrashTest extends TestCase {
	public SampleCrashTest(String methodName) {
		super(methodName);
	}

	public void test1() {
		// Everything is fine...
		System.out.println(getName());
	}

	public void test2() {
		// crash
		System.out.println(getName());
		System.exit(2);
	}

	public void test3() {
		// Everything is again...
		System.out.println(getName());
	}

	public static Test suite() {
		SessionTestSuite sameSession = new SessionTestSuite(CoreTest.PI_HARNESS);
		sameSession.addTest(new SampleCrashTest("test1"));
		sameSession.addCrashTest(new SampleCrashTest("test2"));
		sameSession.addTest(new SampleCrashTest("test3"));
		return sameSession;
	}

}
