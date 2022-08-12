/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.session;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.session.TestDescriptor;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * The actual tests are contributed by another plug-in we don't have access to.
 */
public class SaveParticipantTest {
	public static Test suite() {
		TestSuite suite = new WorkspaceSessionTestSuite("org.eclipse.core.tests.resources.saveparticipant", SaveParticipantTest.class.getName());
		suite.addTest(new TestDescriptor("org.eclipse.core.tests.resources.saveparticipant.SaveManagerTest", "test1"));
		suite.addTest(new TestDescriptor("org.eclipse.core.tests.resources.saveparticipant.SaveManagerTest", "test2"));
		suite.addTest(new TestDescriptor("org.eclipse.core.tests.resources.saveparticipant.SaveManagerTest", "test3"));
		return suite;
	}
}
