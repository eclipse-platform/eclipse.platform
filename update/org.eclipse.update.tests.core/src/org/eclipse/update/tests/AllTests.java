/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.update.tests.api.AllAPITests;
import org.eclipse.update.tests.branding.*;
//import org.eclipse.update.tests.configurations.AllConfigurationsTests;
import org.eclipse.update.tests.core.TestErrorRecoveryLog;
import org.eclipse.update.tests.core.boot.AllPlatformConfigurationTests;
import org.eclipse.update.tests.mirror.*;
import org.eclipse.update.tests.model.AllModelTests;
import org.eclipse.update.tests.nestedfeatures.AllNestedTests;
import org.eclipse.update.tests.parser.AllParserTests;
//import org.eclipse.update.tests.perfms.*;
import org.eclipse.update.tests.reconciliation.AllReconciliationTests;
import org.eclipse.update.tests.regularInstall.AllRegularInstallTests;
//import org.eclipse.update.tests.regularRemove.AllRegularRemoveTests;
import org.eclipse.update.tests.sitevalidation.AllSiteValidationTests;
//import org.eclipse.update.tests.standalone.*;
import org.eclipse.update.tests.types.AllTypesTests;
import org.eclipse.update.tests.uivalues.AllCoreUITests;

public class AllTests extends TestSuite {
	public AllTests(String name) {
		super(name);
	}
	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite();
		suite.setName("All Update Manager Tests");
		suite.addTest(new TestSuite(TestErrorRecoveryLog.class));
		suite.addTest(AllMirrorTests.suite());	
		suite.addTest(AllSiteValidationTests.suite());
		suite.addTest(AllPlatformConfigurationTests.suite());
		suite.addTest(AllRegularInstallTests.suite());
		suite.addTest(AllAPITests.suite());
		suite.addTest(AllParserTests.suite());
		suite.addTest(AllCoreUITests.suite());
//		suite.addTest(AllConfigurationsTests.suite());
		suite.addTest(AllTypesTests.suite());
//		suite.addTest(AllRegularRemoveTests.suite());
		suite.addTest(AllNestedTests.suite());	
		suite.addTest(AllReconciliationTests.suite());					
		suite.addTest(AllModelTests.suite());
		suite.addTest(AllBrandingTests.suite());
		//suite.addTest(AllStandaloneTests.suite());
		//suite.addTest(AllPerformanceTests.suite());
		return suite;
	}
}
