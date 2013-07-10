/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core.provider;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class AllTestsProvider extends EclipseTest {

	public AllTestsProvider() {
		super();
	}

	public AllTestsProvider(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		//suite.addTestSuite(ModuleTest.class);
		suite.addTest(ImportTest.suite());
		suite.addTest(RemoteResourceTest.suite());
		suite.addTest(CVSProviderTest.suite());
		suite.addTest(ResourceDeltaTest.suite());
		suite.addTest(WatchEditTest.suite());
		suite.addTest(LinkResourcesTest.suite());
		suite.addTest(IsModifiedTests.suite());

		// Disabled since they are unstable, see https://bugs.eclipse.org/409126
//		suite.addTest(RepositoryRootTest.suite());

		return new CVSTestSetup(suite);
	}
}
