package org.eclipse.update.tests;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;

import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.*;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.core.UpdateManagerPlugin;
import org.eclipse.update.internal.core.UpdateManagerUtils;
/**
 * All Help System Test cases must subclass this base Testcase.
 * See SampleTestCase.java for a template.
 */

public abstract class UpdateManagerTestCase extends TestCase {

	protected static ResourceBundle bundle;
	protected static String dataPath;

	protected static URL SOURCE_FILE_SITE;
	protected static URL SOURCE_FILE_SITE_INSTALLED;	
	protected static URL SOURCE_HTTP_SITE;
	protected static URL TARGET_FILE_SITE;

	private static final String DATA_PATH = "data/";

	/**
	 * Default Constructor
	 */
	public UpdateManagerTestCase(String name) {
		super(name);
		try {
			init();
		} catch (Exception e) {
			fail(e.toString());
			e.printStackTrace();
		}
	}

	protected static void init() throws MissingResourceException, IOException, MalformedURLException {

		IPluginDescriptor dataDesc = Platform.getPluginRegistry().getPluginDescriptor("org.eclipse.update.tests.core");
		URL resolvedURL = Platform.resolve(dataDesc.getInstallURL());
		URL dataURL = new URL(resolvedURL,DATA_PATH);
		dataPath = dataURL.getFile();
		String homePath = (System.getProperty("user.home")).replace(File.separatorChar,'/');

		if (bundle == null) {
			ClassLoader l = new URLClassLoader(new URL[] { dataURL }, null);
			bundle = ResourceBundle.getBundle("resources", Locale.getDefault(), l);
		}

		try {
			SOURCE_FILE_SITE = new File(dataPath).toURL();
			SOURCE_FILE_SITE_INSTALLED = new File(dataPath+"testAPI/").toURL();
			SOURCE_HTTP_SITE = new URL("http", getHttpHost(),getHttpPort(), bundle.getString("HTTP_PATH_1"));
			TARGET_FILE_SITE = new URL("file",null, homePath +"/target/");
		} catch (Exception e) {
			fail(e.toString());
			e.printStackTrace();
		}

		//cleanup target 
		File target = new File(homePath + "/target/");
		UpdateManagerUtils.removeFromFileSystem(target);
		
		// setup cache site to false
		InternalSiteManager.globalUseCache = false;
	}

	/**
	 * Simple implementation of setUp. Subclasses are prevented 
	 * from overriding this method to maintain logging consistency.
	 * umSetUp() should be overriden instead.
	 */
	protected final void setUp() throws Exception {
		System.out.println("----- " + this.getName());
		System.out.println(this.getName() + ": setUp...");
		umSetUp();
	}

	/**
	 * Sets up the fixture, for example, open a network connection.
	 * This method is called before a test is executed.
	 */
	protected void umSetUp() throws Exception {
		// do nothing.
	}

	/**
	 * Simple implementation of tearDown.  Subclasses are prevented 
	 * from overriding this method to maintain logging consistency.
	 * umTearDown() should be overriden instead.
	 */
	protected final void tearDown() throws Exception {
		System.out.println(this.getName() + ": tearDown...\n");
		umTearDown();
	}

	/**
	 * Tears down the fixture, for example, close a network connection.
	 * This method is called after a test is executed.
	 */
	protected void umTearDown() throws Exception {
		// do nothing.
	}

	protected static String getHttpHost(){
		return UpdateTestsPlugin.getWebAppServerHost();
	}

	protected static int getHttpPort(){
		return UpdateTestsPlugin.getWebAppServerPort();
	}

	protected void remove(IFeature feature, IConfiguredSite configSite) throws CoreException{
		ISite site = configSite.getSite();
		remove(feature,site);
	}

	protected void remove(IFeature feature, ISite site) throws CoreException{
		IFeatureReference ref = site.getFeatureReference(feature);
			// remove the plugins and features dir
			String sitePath = site.getURL().getFile();
			File file = null;
						
			String featureName = feature.getVersionedIdentifier().getIdentifier().toString()+"_"+feature.getVersionedIdentifier().getVersion().toString();
			file = new File(sitePath,"features"+File.separator+featureName);
System.out.println("****************************************Removing :"+file);				
				UpdateManagerUtils.removeFromFileSystem(file);		
			
			IPluginEntry[] entries = feature.getPluginEntries();
			for (int i = 0; i < entries.length; i++) {
				String name = entries[i].getVersionedIdentifier().getIdentifier().toString()+"_"+entries[i].getVersionedIdentifier().getVersion().toString()+File.separator;
				file = new File(sitePath,"plugins"+File.separator+name);
System.out.println("****************************************Removing :"+file);				
				UpdateManagerUtils.removeFromFileSystem(file);
			}
	}
}