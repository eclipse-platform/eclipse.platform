package org.eclipse.update.tests.regularInstall;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved. 
 */
import java.io.File;
import java.net.URL;

import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.update.core.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.model.*;
import org.eclipse.update.internal.model.ConfigurationPolicyModel;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestLocalSite extends UpdateManagerTestCase {
	
	/**
	 * Test the getFeatures()
	 */
	public TestLocalSite(String arg0) {
		super(arg0);
	}
	
	public void testCreationConfigFile() throws Exception {

		//clean up
		SiteLocal siteLocal = (SiteLocal)SiteManager.getLocalSite();
		URL newURL = new URL(siteLocal.getLocationURL(),SiteLocal.SITE_LOCAL_FILE);
		File localFile = new File(newURL.getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)siteLocal.getCurrentConfiguration()).getURL().getFile()));	
		InternalSiteManager.localSite=null;	


		ILocalSite site = SiteManager.getLocalSite();
		site.save();
		URL location = ((SiteLocal)site).getLocationURL();
		String fileName = UpdateManagerUtils.getLocalRandomIdentifier(SiteLocal.DEFAULT_CONFIG_FILE, site.getCurrentConfiguration().getCreationDate());
		String filePath = new URL(location,fileName).getFile();
		File file = new File(filePath);
		assertTrue("config file hasn't been saved in :"+filePath, file.exists());
		assertTrue("Local site hasn't been saved in :"+localFile.getAbsolutePath(),localFile.exists());
		
		// cleanup
		UpdateManagerUtils.removeFromFileSystem(file);
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		

	}
	
	public void testDefaultConfigFile() throws Exception {

		//clean up
		SiteLocal siteLocal = (SiteLocal)SiteManager.getLocalSite();
		URL newURL = new URL(siteLocal.getLocationURL(),SiteLocal.SITE_LOCAL_FILE);
		File localFile = new File(newURL.getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)siteLocal.getCurrentConfiguration()).getURL().getFile()));	
		InternalSiteManager.localSite=null;

		ILocalSite site = SiteManager.getLocalSite();
		assertTrue("The local site does not contain an history of install configuration",site.getConfigurationHistory().length!=0);
		assertTrue("The local site does not contain an current install configuration",site.getCurrentConfiguration()!=null);
		assertTrue("The local site does not contain a default configuration site for the current install config",site.getCurrentConfiguration().getConfiguredSites().length!=0);
		
		System.out.println("Default Config Site is :"+site.getCurrentConfiguration().getConfiguredSites()[0].getSite().getURL().toExternalForm());
		
		// cleanup
		URL location = ((SiteLocal)site).getLocationURL();		
		String filePath = new URL(location,SiteLocal.DEFAULT_CONFIG_FILE).getFile();
		File file = new File(filePath);
		UpdateManagerUtils.removeFromFileSystem(file);		
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		

	}
	
/*	public void testInstallFeatureSaveConfig() throws Exception {

		//clean up
		SiteLocal siteLocal = (SiteLocal)SiteManager.getLocalSite();
		URL newURL = new URL(siteLocal.getLocationURL(),SiteLocal.SITE_LOCAL_FILE);
		File localFile = new File(newURL.getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)siteLocal.getCurrentConfiguration()).getURL().getFile()));	
		InternalSiteManager.localSite=null;		

		ILocalSite site = SiteManager.getLocalSite();
		ISite remoteSite = SiteManager.getSite(SOURCE_FILE_SITE_INSTALLED);
		IFeature feature = remoteSite.getFeatureReferences()[0].getFeature();
		int oldNumber = site.getCurrentConfiguration().getConfiguredSites().length;
		
		// we are not checking if this is read only
		IInstallConfiguration newConfig = site.cloneCurrentConfiguration();
		newConfig.setLabel("new Label");		
		//IInstallConfiguration newConfig = site.getCurrentConfiguration();
		IConfiguredSite configSite = newConfig.getConfiguredSites()[0];
		ConfigurationPolicyModel configPolicy = new BaseSiteLocalFactory().createConfigurationPolicyModel();
		configPolicy.setPolicy(IPlatformConfiguration.ISitePolicy.USER_INCLUDE);
		((ConfiguredSite)configSite).setConfigurationPolicy((ConfigurationPolicy)configPolicy);
		int oldNumberOfhistory = site.getConfigurationHistory().length;		
		site.addConfiguration(newConfig);	
		assertNotNull(feature);	

		((ConfiguredSite)configSite).isUpdatable(true);
		remove(feature,configSite);				
		configSite.install(feature,null,null);
				
		// teh current one points to a real fature
		// does not throw error.
		IConfiguredSite configSite2 = site.getCurrentConfiguration().getConfiguredSites()[0];
		assertTrue("No Configured features found",configSite.getConfiguredFeatures().length>0);
		IFeatureReference ref = configSite2.getConfiguredFeatures()[0];
		IFeature feature2 = ref.getFeature();
		//String configuredFeature = feature2.getLabel();

		assertEquals(feature2.getVersionedIdentifier().toString(),"org.eclipse.update.core.tests.feature3_1.0.0");
		assertTrue("Wrong id  version of feature",feature2.getVersionedIdentifier().toString().equalsIgnoreCase("org.eclipse.update.core.tests.feature3_1.0.0"));
		
		// only one feature configured
		assertTrue("too many features configured",configSite2.getConfiguredFeatures().length==1);
		
		// no feature unconfigured
		assertTrue("too many unconfigured features",((ConfiguredSite)configSite2).getConfigurationPolicy().getUnconfiguredFeatures().length==0);
		
		// test only 2 install config in local site
		//assertEquals("wrong number of history in Local site:",oldNumberOfhistory+1,site.getConfigurationHistory().length);
		
		// test same # of sites in current config
		assertTrue("Wrong number of config sites in current config",site.getCurrentConfiguration().getConfiguredSites().length==oldNumber);
		
		//test only one feature for the site
		assertTrue("wrong number of configured features for config site",site.getCurrentConfiguration().getConfiguredSites()[0].getConfiguredFeatures().length==1);
		
		// test only 2 activities
		assertTrue("Wrong number of activities for install config",site.getCurrentConfiguration().getActivities().length==2);

		site.save();		

		// check
		// there are 2 configuration
		String time = ""+site.getCurrentConfiguration().getCreationDate().getTime();
		URL location = ((SiteLocal)site).getLocationURL();
		File file = new File(new URL(location,"Config"+time+".xml").getFile());
		assertTrue("new configuration does not exist", file.exists());
		
		// cleanup
		localFile = new File(new URL(location,SiteLocal.SITE_LOCAL_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		localFile = new File(new URL(location,SiteLocal.DEFAULT_CONFIG_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		localFile = new File(new URL(location,Site.DEFAULT_FEATURE_PATH+File.separator+feature.getVersionedIdentifier().toString()).getFile());		
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		localFile = new File(new URL(location,Site.DEFAULT_FEATURE_PATH+File.separator+feature2.getVersionedIdentifier().toString()).getFile());		
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		UpdateManagerUtils.removeFromFileSystem(file);		
		localFile = new File(feature2.getURL().getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
	}
*/
	
	public void testRetriveConfig() throws Exception {

		//clean up
		SiteLocal siteLocal = (SiteLocal)SiteManager.getLocalSite();
		URL newURL = new URL(siteLocal.getLocationURL(),SiteLocal.SITE_LOCAL_FILE);
		File localFile = new File(newURL.getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)siteLocal.getCurrentConfiguration()).getURL().getFile()));	
		InternalSiteManager.localSite=null;		

		ILocalSite site = SiteManager.getLocalSite();
		ISite remoteSite = SiteManager.getSite(SOURCE_FILE_SITE_INSTALLED);
		IFeature feature = remoteSite.getFeatureReferences()[0].getFeature();
		int oldNumber = site.getCurrentConfiguration().getConfiguredSites().length;		
		
		// we are not checking if this is read only
		IInstallConfiguration newConfig = site.cloneCurrentConfiguration();
		newConfig.setLabel("new Label");		
		IConfiguredSite configSite = newConfig.getConfiguredSites()[0];
		ConfigurationPolicyModel configPolicy = new BaseSiteLocalFactory().createConfigurationPolicyModel();
		configPolicy.setPolicy(IPlatformConfiguration.ISitePolicy.USER_INCLUDE);
		((ConfiguredSite)configSite).setConfigurationPolicyModel(configPolicy);	
		int oldNumberOfhistory = site.getConfigurationHistory().length;			
		site.addConfiguration(newConfig);
		assertNotNull(feature);			
		
		((ConfiguredSite)configSite).isUpdatable(true);
		remove(feature,configSite);			
		configSite.install(feature,null,null);
		site.save();
		
		// we created the second xml file

		//do not cleanup, we want to reuse previously created local site
		// but force re-read of xml File
		InternalSiteManager.localSite=null;
		site = SiteManager.getLocalSite();
		
		// check
		// there are 2 configuration
		String time = ""+site.getCurrentConfiguration().getCreationDate().getTime();
		URL location = ((SiteLocal)site).getLocationURL();		
		//File file = new File(new URL(location,"Config"+time+".xml").getFile());
		File file = new File(new URL(location,SiteLocal.DEFAULT_CONFIG_PREFIX+time+".xml").getFile());		
		assertTrue("new configuration does not exist", file.exists());
		
		// teh current one points to a real fature
		// does not throw error.
		IConfiguredSite configSite2 = site.getCurrentConfiguration().getConfiguredSites()[0];
		IFeatureReference[] refs = configSite2.getConfiguredFeatures();
		boolean found = false;
		IFeature feature2 = null;
		for (int i = 0; i < refs.length; i++) {
			IFeature feature3 = refs[i].getFeature();			
			if ("org.eclipse.update.core.tests.feature3_1.0.0".equals(feature3.getVersionedIdentifier().toString())){
				feature2 = feature3;
				found = true;
			}		
		}
		assertNotNull("Feature 2 is Null",feature2);
		assertTrue("Wrong id  version of feature",feature2.getVersionedIdentifier().toString().equalsIgnoreCase("org.eclipse.update.core.tests.feature3_1.0.0"));
		
		// test only 2 install config in local site
		//assertEquals("wrong number of history in Local site:",oldNumberOfhistory+1,site.getConfigurationHistory().length);
		
		// test # of sites in current config
		assertTrue("Wrong number of config sites in current config",site.getCurrentConfiguration().getConfiguredSites().length==oldNumber);
		
		//test only one feature for the site
		assertTrue("wrong number of configured features for config site",site.getCurrentConfiguration().getConfiguredSites()[0].getConfiguredFeatures().length==1);
		
		// test only 2 activities
		assertTrue("Wrong number of activities for install config",site.getCurrentConfiguration().getActivities().length==2);
		
		
		// cleanup
		localFile = new File(new URL(location,SiteLocal.SITE_LOCAL_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		localFile = new File(new URL(location,SiteLocal.DEFAULT_CONFIG_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);			
		localFile = new File(new URL(location,Site.DEFAULT_FEATURE_PATH+File.separator+feature.getVersionedIdentifier().toString()).getFile());		
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)site.getCurrentConfiguration()).getURL().getFile()));				
		UpdateManagerUtils.removeFromFileSystem(file);		
	}

	public void testRetriveConfigHTTPInstall() throws Exception {

		//clean up
		SiteLocal siteLocal = (SiteLocal)SiteManager.getLocalSite();
		URL newURL = new URL(siteLocal.getLocationURL(),SiteLocal.SITE_LOCAL_FILE);
		File localFile = new File(newURL.getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)siteLocal.getCurrentConfiguration()).getURL().getFile()));	
		InternalSiteManager.localSite=null;		

		ILocalSite site = SiteManager.getLocalSite();
		ISite remoteSite = SiteManager.getSite(SOURCE_HTTP_SITE);
		IFeature feature = remoteSite.getFeatureReferences()[0].getFeature();
		
		// we are not checking if this is read only
		IInstallConfiguration newConfig = site.cloneCurrentConfiguration();
		newConfig.setLabel("new Label");		
		IConfiguredSite configSite = newConfig.getConfiguredSites()[0];
		ConfigurationPolicyModel configPolicy = new BaseSiteLocalFactory().createConfigurationPolicyModel();
		configPolicy.setPolicy(IPlatformConfiguration.ISitePolicy.USER_INCLUDE);
		((ConfiguredSite)configSite).setConfigurationPolicyModel(configPolicy);	
		int oldNumberOfhistory = site.getConfigurationHistory().length;			
		site.addConfiguration(newConfig);
		
	
		((ConfiguredSite)configSite).isUpdatable(true);				
		configSite.install(feature,null,null);
		site.save();

		//do not cleanup, we want to reuse previously created local site
		// but force re-read of xml File
		InternalSiteManager.localSite=null;
		site = SiteManager.getLocalSite();
		feature = remoteSite.getFeatureReferences()[0].getFeature();
		int oldNumber = site.getCurrentConfiguration().getConfiguredSites().length;		
		
		// check
		// there are 2 configuration
		String time = ""+site.getCurrentConfiguration().getCreationDate().getTime();
		URL location = ((SiteLocal)site).getLocationURL();		
		//File file = new File(new URL(location,"Config"+time+".xml").getFile());
		File file = new File(new URL(location,SiteLocal.DEFAULT_CONFIG_PREFIX+time+".xml").getFile());		
		assertTrue("new configuration does not exist", file.exists());
		
		// teh current one points to a real fature
		// does not throw error.
		IConfiguredSite configSite2 = site.getCurrentConfiguration().getConfiguredSites()[0];
		
		IFeatureReference[] refs = configSite2.getConfiguredFeatures();
		boolean found = false;
		IFeature feature2 = null;
		for (int i = 0; i < refs.length; i++) {
			IFeature feature3 = refs[i].getFeature();			
			if ("org.test1.ident1_1.0.0".equals(feature3.getVersionedIdentifier().toString())){
				feature2 = feature3;
				found = true;
			}		
		}

		//String configuredFeature = feature2.getLabel();
		assertTrue("cannot find feature org.test1.ident1_1.0.0 in configured Site",found);
		assertTrue("Wrong id  version of feature",feature2.getVersionedIdentifier().toString().equalsIgnoreCase("org.test1.ident1_1.0.0"));
		
		// test only 2 install config in local site
		//assertEquals("wrong number of history in Local site:",oldNumberOfhistory+1,site.getConfigurationHistory().length);
		
		// test same number of sites in current config
		assertTrue("Wrong number of config sites in current config",site.getCurrentConfiguration().getConfiguredSites().length==oldNumber);
		
		//test only one feature for the site
		assertTrue("wrong number of configured features for config site",site.getCurrentConfiguration().getConfiguredSites()[0].getConfiguredFeatures().length==1);
		
		// test only 2 activities
		assertTrue("Wrong number of activities for install config",site.getCurrentConfiguration().getActivities().length==2);
		
		
		// cleanup
		localFile = new File(new URL(location,SiteLocal.SITE_LOCAL_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		localFile = new File(new URL(location,SiteLocal.DEFAULT_CONFIG_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		localFile = new File(new URL(location,Site.DEFAULT_FEATURE_PATH+File.separator+feature.getVersionedIdentifier().toString()).getFile());		
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)site.getCurrentConfiguration()).getURL().getFile()));						
		UpdateManagerUtils.removeFromFileSystem(file);		
		localFile = new File(feature2.getURL().getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
	}

}

