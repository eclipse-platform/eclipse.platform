/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.cvsresources;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class AllTestsCVSResources extends EclipseTest {
	public static Test suite() {	
		TestSuite suite = new TestSuite();
		suite.addTest(ResourceSyncInfoTest.suite());
		suite.addTest(EclipseSynchronizerTest.suite());
		suite.addTest(EclipseFolderTest.suite());
		suite.addTest(ResourceSyncBytesTest.suite());
		suite.addTest(CVSURITest.suite());
		return suite; 	
	}	
	
	public AllTestsCVSResources(String name) {
		super(name);
	}
	
	public AllTestsCVSResources() {
		super();
	}
}


