package org.eclipse.update.tests.api;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.tests.UpdateManagerTestCase;
import java.io.File;
import junit.framework.*;

/**
 * Manages the API tests
 */
public class AllAPITests extends UpdateManagerTestCase {
	/**
	 * Constructor
	 */
	public AllAPITests(String name) {
		super(name);
	}
	
	/**
	 * List of API tests
	 */
	public static Test suite() throws Exception {
		TestSuite suite = new TestSuite();
		suite.setName("API Tests");

		suite.addTest(new TestSuite(TestSiteAPI.class));
		suite.addTest(new TestSuite(TestLocalSiteAPI.class));		
		suite.addTest(new TestSuite(TestUpdateManagerUtilsAPI.class));		
		suite.addTest(new TestSuite(TestDefaultExecutableFeatureAPI.class));
		suite.addTest(new TestSuite(TestDefaultPackageFeatureAPI.class));
		suite.addTest(new TestSuite(TestPluginContainerAPI.class));
		suite.addTest(new TestSuite(TestSiteManagerAPI.class));		
		suite.addTest(new TestSuite(TestNestedFeatureAPI.class));
		suite.addTest(new TestSuite(TestFeatureContentProvider_FileFilterAPI.class));
		return suite;
	}
}