package org.eclipse.update.tests.regularInstall;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;
import java.net.URL;

import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.tests.UpdateManagerTestCase;
import org.eclipse.update.tests.UpdateTestsPlugin;

public class TestSpaceInInstall extends UpdateManagerTestCase {
	/**
	 * Constructor for Test1
	 */
	public TestSpaceInInstall(String arg0) {
		super(arg0);
	}

	/**
	 *
	 */
	public void testSpaceInURL() throws Exception {

		//cleanup target 
		URL testURL = new URL(TARGET_FILE_SITE,"test site space/"); 
		String testPath = testURL.getFile();
		File target = new File(testPath);
		UpdateManagerUtils.removeFromFileSystem(target);
		
		URL newURL = new File(dataPath + "Site with space/site.xml").toURL();
		ISite remoteSite = SiteManager.getSite(newURL);
		IFeatureReference[] featuresRef = remoteSite.getFeatureReferences();
		File file = new File(testURL.getFile());
		if (!file.exists()) file.mkdirs();
		ISite localSite = SiteManager.getSite(testURL);
		IFeature remoteFeature = null;
		
		// at least one executable feature and on packaged
		boolean execFeature = false;
		boolean packFeature = false;

		if (featuresRef.length==0) fail ("no feature found");
	
		for (int i = 0; i < featuresRef.length; i++) {
			remoteFeature = featuresRef[i].getFeature();
			remove(remoteFeature,localSite); 
			localSite.install(remoteFeature, null,null);
			
			if (remoteFeature.getFeatureContentProvider() instanceof FeaturePackagedContentProvider) packFeature = true;
			if (remoteFeature.getFeatureContentProvider() instanceof FeatureExecutableContentProvider) execFeature = true;

			// verify
			String site = testURL.getFile();
			IPluginEntry[] entries = remoteFeature.getPluginEntries();
			assertTrue("no plugins entry", (entries != null && entries.length != 0));
			String pluginName = entries[0].getVersionedIdentifier().toString();
			File pluginFile = new File(site, Site.DEFAULT_PLUGIN_PATH + pluginName);
			assertTrue("plugin files not installed locally", pluginFile.exists());

			File featureFile = new File(site, Site.DEFAULT_INSTALLED_FEATURE_PATH + remoteFeature.getVersionedIdentifier().toString());
			assertTrue("feature info not installed locally:"+featureFile, featureFile.exists());

			File featureFileXML = new File(site, Site.DEFAULT_INSTALLED_FEATURE_PATH + remoteFeature.getVersionedIdentifier().toString() + File.separator + "feature.xml");
			assertTrue("feature info not installed locally: no feature.xml", featureFileXML.exists());
		}

		if (!execFeature && !packFeature){
			fail("cannot find one executable and one package feature on teh site");
		}

		//cleanup target 
		UpdateManagerUtils.removeFromFileSystem(target);


	}
	
	
	/**
	 *
	 */
	public void testSpaceInHTTPURL() throws Exception {

		//cleanup target  
		File target = new File(TARGET_FILE_SITE.getFile());
		UpdateManagerUtils.removeFromFileSystem(target);
		
		String path = bundle.getString("HTTP_PATH_3");
		ISite remoteSite = SiteManager.getSite(new URL("http",getHttpHost(),getHttpPort(),path));		
		IFeatureReference[] featuresRef = remoteSite.getFeatureReferences();
		ISite localSite = SiteManager.getSite(TARGET_FILE_SITE);
		IFeature remoteFeature = null;
		
		// at least one executable feature and on packaged
		boolean execFeature = false;
		boolean packFeature = false;

		if (featuresRef.length==0) fail ("no feature found");
	
		for (int i = 0; i < featuresRef.length; i++) {
			remoteFeature = featuresRef[i].getFeature();
			localSite.install(remoteFeature, null,null);
			
			if (remoteFeature.getFeatureContentProvider() instanceof FeaturePackagedContentProvider) packFeature = true;
			if (remoteFeature.getFeatureContentProvider() instanceof FeatureExecutableContentProvider) execFeature = true;

			// verify
			String site = TARGET_FILE_SITE.getFile();			
			IPluginEntry[] entries = remoteFeature.getPluginEntries();
			assertTrue("no plugins entry", (entries != null && entries.length != 0));
			String pluginName = entries[0].getVersionedIdentifier().toString();
			File pluginFile = new File(site, Site.DEFAULT_PLUGIN_PATH + pluginName);
			assertTrue("plugin files not installed locally", pluginFile.exists());

			File featureFile = new File(site, Site.DEFAULT_INSTALLED_FEATURE_PATH + remoteFeature.getVersionedIdentifier().toString());
			assertTrue("feature info not installed locally:"+featureFile, featureFile.exists());

			File featureFileXML = new File(site, Site.DEFAULT_INSTALLED_FEATURE_PATH + remoteFeature.getVersionedIdentifier().toString() + File.separator + "feature.xml");
			assertTrue("feature info not installed locally: no feature.xml", featureFileXML.exists());
		}

		if (!execFeature && !packFeature){
			fail("cannot find one executable and one package feature on teh site");
		}

		//cleanup target 
		UpdateManagerUtils.removeFromFileSystem(target);


	}
		
		
}