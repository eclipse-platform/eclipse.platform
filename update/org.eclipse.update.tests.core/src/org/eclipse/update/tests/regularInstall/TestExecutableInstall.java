package org.eclipse.update.tests.regularInstall;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;

import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestExecutableInstall extends UpdateManagerTestCase {
	/**
	 * Constructor for Test1
	 */
	public TestExecutableInstall(String arg0) {
		super(arg0);
	}
	


	public void testFileSite() throws Exception{
		
		//cleanup target 
		File target = new File(TARGET_FILE_SITE.getFile());
		UpdateManagerUtils.removeFromFileSystem(target);		
		
		ISite remoteSite = SiteManager.getSite(SOURCE_FILE_SITE_INSTALLED);
		IFeatureReference[] remoteFeatureReference = remoteSite.getFeatureReferences();
		IFeature remoteFeature = remoteFeatureReference[0].getFeature();
		ISite localSite = SiteManager.getSite(TARGET_FILE_SITE);
		assertNotNull(remoteFeature);
		remove(remoteFeature,localSite);		
		localSite.install(remoteFeature,null,null);
		
		// verify
		String site = localSite.getURL().getFile();
		IPluginEntry[] entries = remoteFeature.getRawPluginEntries();
		assertTrue("no plugins entry",(entries!=null && entries.length!=0));
		String pluginName= entries[0].getVersionedIdentifier().toString();
		File pluginFile = new File(site,Site.DEFAULT_PLUGIN_PATH+pluginName);
		assertTrue("plugin files not installed locally",pluginFile.exists());

		File featureFile = new File(site,Site.DEFAULT_INSTALLED_FEATURE_PATH+remoteFeature.getVersionedIdentifier().toString());
		assertTrue("feature info not installed locally:"+featureFile,featureFile.exists());
		assertTrue("feature is a file, not a directory:"+featureFile,featureFile.isDirectory());

		
		File featureFileXML = new File(site,Site.DEFAULT_INSTALLED_FEATURE_PATH+remoteFeature.getVersionedIdentifier().toString()+File.separator+"feature.xml");
		assertTrue("feature info not installed locally: no feature.xml",featureFileXML.exists());
		//cleanup target 
		UpdateManagerUtils.removeFromFileSystem(target);
		
	}
}

