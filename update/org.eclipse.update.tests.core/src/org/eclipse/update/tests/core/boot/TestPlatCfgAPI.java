/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.tests.core.boot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.Assert;
import org.eclipse.update.configurator.*;
import org.eclipse.update.configurator.IPlatformConfiguration.*;

public class TestPlatCfgAPI extends PlatformConfigurationTestCase {
			
	public TestPlatCfgAPI(String arg0) {
		super(arg0);
	}
	
	public void testBasic() throws Exception {
		
		// get new config object
		IPlatformConfiguration cfig = null;
		try {
			cfig = ConfiguratorUtils.getPlatformConfiguration(null);
		} catch (IOException e) {
			Assert.fail("0.0.0 "+e.toString());
		}
		ISiteEntry[] dflt = cfig.getConfiguredSites();
		Assert.assertEquals("0.0.1",dflt.length,0);
		
		// policy tests
		ISitePolicy p1 = cfig.createSitePolicy(ISitePolicy.USER_INCLUDE, null);
		Assert.assertEquals("1.0.0",p1.getType(),ISitePolicy.USER_INCLUDE);
		Assert.assertEquals("1.0.1",p1.getList().length,0);
		p1.setList(new String[] {"first"});
		Assert.assertEquals("1.0.2",p1.getList().length,1);
		Assert.assertEquals("1.0.3",p1.getList()[0],"first");
		
		ISitePolicy p2 = cfig.createSitePolicy(ISitePolicy.USER_EXCLUDE, new String[0]);
		Assert.assertEquals("1.1.0",p2.getType(),ISitePolicy.USER_EXCLUDE);
		Assert.assertEquals("1.1.1",p2.getList().length,0);
		p2.setList(new String[] {"first", "second"});
		Assert.assertEquals("1.1.2",p2.getList().length,2);
		Assert.assertEquals("1.1.3",p2.getList()[1],"second");
		
		// create some urls
		URL u1 = null;
		URL u2 = null;

		URL u4 = null;
		try {
            u1 = new URL("file:/d:/temp_test_dummy");
//			u2 = new URL("file://localhost/temp");
			new URL("http://some.server/temp/");
			u4 = new URL("http://bad.url");
		} catch (MalformedURLException e) {
			Assert.fail("2.0.0 unable to create URL "+e);
		}
				
		// site creation tests
		ISiteEntry s1 = cfig.createSiteEntry(u1,p2);
		Assert.assertEquals("3.0.0",s1.getURL(),u1);
		Assert.assertEquals("3.0.1",s1.getSitePolicy(),p2);
		s1.setSitePolicy(p1);
		Assert.assertEquals("3.0.2",s1.getSitePolicy(),p1);
		
//		ISiteEntry s2 = cfig.createSiteEntry(u2,p1);
//		Assert.assertEquals("3.1.0",s2.getURL(),u2);
//		Assert.assertEquals("3.1.1",s2.getSitePolicy(),p1);
//		s2.setSitePolicy(p2);
//		Assert.assertEquals("3.1.2",s2.getSitePolicy(),p2);
		
		// configure site tests
		Assert.assertEquals("3.3.0",cfig.getConfiguredSites().length,0);
		cfig.configureSite(s1);
		Assert.assertEquals("3.3.1",cfig.getConfiguredSites().length,1);
//		cfig.configureSite(s2);
//		Assert.assertEquals("3.3.2",cfig.getConfiguredSites().length,2);
//		
		// lookup site tests
		Assert.assertEquals("3.4.0",cfig.findConfiguredSite(u1),s1);
//		Assert.assertEquals("3.4.1",cfig.findConfiguredSite(u2),s2);
		Assert.assertNull("3.4.3",cfig.findConfiguredSite(u4));
		
		// unconfigure site tests
		cfig.unconfigureSite(s1);
		Assert.assertEquals("3.5.0",cfig.getConfiguredSites().length,0);
		Assert.assertNull("3.5.1",cfig.findConfiguredSite(u1));		
//		cfig.unconfigureSite(s2);
//		Assert.assertEquals("3.5.2",cfig.getConfiguredSites().length,0);
//		Assert.assertNull("3.5.3",cfig.findConfiguredSite(u2));	
	}
	
	public void testSaveRestore() throws Exception {
				
		Assert.assertNotNull("0.0 Unable to obtain temp directory",tempDir);
		
		// get new config object
		IPlatformConfiguration cfig = null;
		try {
			cfig = ConfiguratorUtils.getPlatformConfiguration(null);
		} catch (IOException e) {
			Assert.fail("0.0.0 "+e.toString());
		}
		ISiteEntry[] sites = cfig.getConfiguredSites();
		Assert.assertEquals("0.0.1",sites.length,0);
				
		// create policies
		ISitePolicy p1 = cfig.createSitePolicy(ISitePolicy.USER_INCLUDE, null);
		ISitePolicy p2 = cfig.createSitePolicy(ISitePolicy.USER_EXCLUDE, new String[0]);		
		ISitePolicy p3 = cfig.createSitePolicy(ISitePolicy.USER_INCLUDE, new String[] {"first"});
		ISitePolicy p4 = cfig.createSitePolicy(ISitePolicy.USER_EXCLUDE, new String[] {"first", "second"});
						
		// create some urls
		URL u1 = null;
		URL u2 = null;
		URL u3 = null;
		URL u4 = null;

		try {
			u1 = new URL("file:d:/temp/");
			u2 = new URL("file://localhost/temp/");
			u3 = new URL("http://some.server/temp/");
			u4 = new URL("http://another.server/temp/");
			new URL("http://one.more.server/temp/");
		} catch (MalformedURLException e) {
			Assert.fail("1.0 unable to create URL "+e);
		}
		
		// create and configure sites
		ISiteEntry s1 = cfig.createSiteEntry(u1,p1);
		cfig.configureSite(s1);
		ISiteEntry s2 = cfig.createSiteEntry(u2,p2);
		cfig.configureSite(s2);
		ISiteEntry s3 = cfig.createSiteEntry(u3,p3);
		cfig.configureSite(s3);
		ISiteEntry s4 = cfig.createSiteEntry(u4,p4);
		cfig.configureSite(s4);
		sites = cfig.getConfiguredSites();
		Assert.assertEquals("1.1",sites.length,4);
		
		// do save
		try {
			cfig.save();
			Assert.fail("2.0 was expecting IOException");
		} catch (IOException e) {
		}
		
		URL cfigURL = null;
		try {
			cfigURL = new URL("file:"+tempDir+ "platform.xml");
		} catch(MalformedURLException e) {
			Assert.fail("2.1 unable to create URL "+e);
		}
		
		try {
			cfig.save(cfigURL);
		} catch (IOException e) {
			Assert.fail("2.2 "+e);
		}
		
		// reload configuration	
		try {
			ConfiguratorUtils.getPlatformConfiguration(cfigURL);
		} catch (IOException e) {
			Assert.fail("2.3 "+e.toString());
		}
		ISiteEntry[] newSites = cfig.getConfiguredSites();
		Assert.assertEquals("2.4",newSites.length,4);
		
		// check what we've got
		Assert.assertEquals("3.0.1",sites[0].getURL(),newSites[0].getURL());
		Assert.assertEquals("3.0.2",sites[0].getSitePolicy().getType(),newSites[0].getSitePolicy().getType());
		Assert.assertEquals("3.0.3",sites[0].getSitePolicy().getList(),newSites[0].getSitePolicy().getList());
		
		Assert.assertEquals("3.1.1",sites[1].getURL(),newSites[1].getURL());
		Assert.assertEquals("3.1.2",sites[1].getSitePolicy().getType(),newSites[1].getSitePolicy().getType());
		Assert.assertEquals("3.1.3",sites[1].getSitePolicy().getList(),newSites[1].getSitePolicy().getList());
		
		Assert.assertEquals("3.2.1",sites[2].getURL(),newSites[2].getURL());
		Assert.assertEquals("3.2.2",sites[2].getSitePolicy().getType(),newSites[2].getSitePolicy().getType());
		Assert.assertEquals("3.2.3",sites[2].getSitePolicy().getList(),newSites[2].getSitePolicy().getList());
		
		Assert.assertEquals("3.3.1",sites[3].getURL(),newSites[3].getURL());
		Assert.assertEquals("3.3.2",sites[3].getSitePolicy().getType(),newSites[3].getSitePolicy().getType());
		Assert.assertEquals("3.3.3",sites[3].getSitePolicy().getList(),newSites[3].getSitePolicy().getList());
		
	}
	
	public void testCurrentConfiguration() throws Exception {
				
		Assert.assertNotNull("0.0 Unable to obtain temp directory",tempDir);
		
		// get new config object
		IPlatformConfiguration cfig = null;
		cfig = ConfiguratorUtils.getCurrentPlatformConfiguration();
		cfig.getPluginPath();
		cfig.save();
	}
}

