package org.eclipse.update.tests.parser;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.core.model.DefaultSiteParser;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.core.SiteFileFactory;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.tests.UpdateManagerTestCase;
import org.xml.sax.SAXParseException;

public class TestSiteParse extends UpdateManagerTestCase {

	/**
	 * Constructor for Test1
	 */
	public TestSiteParse(String arg0) {
		super(arg0);
	}

	public void testParse() throws Exception {

		URL remoteUrl = new URL(SOURCE_FILE_SITE + "xmls/site1/");
		ISite remoteSite = SiteManager.getSite(remoteUrl);

		//IFeatureReference[] feature = remoteSite.getFeatureReferences();
		//ICategory[] categories = remoteSite.getCategories();

		String path = remoteUrl.getFile();
		String path2 = remoteSite.getDescription().getURL().getFile();
		assertEquals(new File(path + "index.html"), new File(path2));

	}

	public void testNumberOfFeatures() throws Exception {

		URL remoteURL = new URL("http", getHttpHost(), getHttpPort(), bundle.getString("HTTP_PATH_2"));
		ISite remoteSite = SiteManager.getSite(remoteURL);

		IFeatureReference[] feature = remoteSite.getFeatureReferences();
		assertEquals(feature.length, 2);

	}

	public void testParseValid1() throws Exception {

		URL remoteURL = new URL(SOURCE_FILE_SITE + "parsertests/site.xml");
		DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
		URL resolvedURL = URLEncoder.encode(remoteURL);		
		SiteModel remoteSite = parser.parse(resolvedURL.openStream());
		remoteSite.resolve(remoteURL, null);

		FeatureReferenceModel[] feature = remoteSite.getFeatureReferenceModels();
		CategoryModel[] categories = remoteSite.getCategoryModels();
		ArchiveReferenceModel[] archives = remoteSite.getArchiveReferenceModels();

		assertTrue("Wrong number of features", feature.length == 6);
		assertTrue("Wrong number of categories", categories.length == 3);
		assertTrue("Wrong number of archives", archives.length == 0);

		String path = new URL(SOURCE_FILE_SITE + "parsertests/").getFile();
		String path2 = remoteSite.getDescriptionModel().getURL().getFile();
		assertEquals(path + "index.html", path2);

	}

	public void testParseValid2() throws Exception {

		URL remoteURL = new URL(SOURCE_FILE_SITE + "parsertests/reddot.xml");
		DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
		URL resolvedURL = URLEncoder.encode(remoteURL);		
		SiteModel remoteSite = parser.parse(resolvedURL.openStream());
		remoteSite.resolve(remoteURL, null);

		FeatureReferenceModel[] feature = remoteSite.getFeatureReferenceModels();
		CategoryModel[] categories = remoteSite.getCategoryModels();
		ArchiveReferenceModel[] archives = remoteSite.getArchiveReferenceModels();

		assertTrue("Wrong number of features", feature.length == 2);
		assertTrue("Wrong number of categories", categories.length == 1);
		assertTrue("Wrong number of archives", archives.length == 2);

		String valideString = "This category contains all of the<currently>available versions of Red Dot feature.<greeting>Hello, world!</greeting>";
		assertEquals(valideString,remoteSite.getCategoryModels()[0].getDescriptionModel().getAnnotation());

		String path = new URL(SOURCE_FILE_SITE + "parsertests/").getFile();
		String path2 = remoteSite.getDescriptionModel().getURL().getFile();
		assertEquals(path + "index.html", path2);

	}

	public void testParseValid3() throws Exception {

		URL remoteURL = new URL(SOURCE_FILE_SITE + "parsertests/reddot1.xml");
		DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
		URL resolvedURL = URLEncoder.encode(remoteURL);		
		SiteModel remoteSite = parser.parse(resolvedURL.openStream());
		remoteSite.resolve(remoteURL, null);

		FeatureReferenceModel[] feature = remoteSite.getFeatureReferenceModels();
		CategoryModel[] categories = remoteSite.getCategoryModels();
		ArchiveReferenceModel[] archives = remoteSite.getArchiveReferenceModels();

		assertTrue("Wrong number of features", feature.length == 2);
		assertTrue("Wrong number of categories", categories.length == 1);
		assertTrue("Wrong number of archives", archives.length == 2);

		String valideString = "This category contains all of the<currently>available versions of Red Dot feature.";
		assertEquals(valideString,remoteSite.getCategoryModels()[0].getDescriptionModel().getAnnotation());

		String path = new URL(SOURCE_FILE_SITE + "parsertests/").getFile();
		String path2 = remoteSite.getDescriptionModel().getURL().getFile();
		assertEquals(path + "index.html", path2);

	}

	public void testParseValid4() throws Exception {

		URL remoteURL = new URL(SOURCE_FILE_SITE + "SiteURLTest/data/site.xml");
		DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
		URL resolvedURL = URLEncoder.encode(remoteURL);		
		SiteModel remoteSite = parser.parse(resolvedURL.openStream());
		remoteSite.resolve(remoteURL, null);

		FeatureReferenceModel[] feature = remoteSite.getFeatureReferenceModels();
		//CategoryModel[] categories = remoteSite.getCategoryModels();
		ArchiveReferenceModel[] archives = remoteSite.getArchiveReferenceModels();

		assertTrue("Wrong number of features", feature.length == 2);
		assertTrue("Wrong number of archives", archives.length == 3);
		
		URL path1 = new URL(SOURCE_FILE_SITE+"SiteURLTest/data/artifacts/features/helpFeature.jar");
		assertEquals(path1,feature[0].getURL());
		URL path2 = new URL(SOURCE_FILE_SITE+"SiteURLTest/data/artifacts/plugins/help.jar");
		assertEquals(path2,archives[0].getURL());
		

		String path = new URL(SOURCE_FILE_SITE + "SiteURLTest/data/info/").getFile();
		String path3 = remoteSite.getDescriptionModel().getURL().getFile();
		assertEquals(path + "siteInfo.html", path3);

	}

	public void testParseValid5() throws Exception {

		URL remoteURL = new URL(SOURCE_FILE_SITE + "parsertests/site2.xml");
		DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
		URL resolvedURL = URLEncoder.encode(remoteURL);		
		SiteModel remoteSite = parser.parse(resolvedURL.openStream());
		remoteSite.resolve(remoteURL, null);

		FeatureReferenceModel[] featureRef = remoteSite.getFeatureReferenceModels();
		//CategoryModel[] categories = remoteSite.getCategoryModels();
		ArchiveReferenceModel[] archives = remoteSite.getArchiveReferenceModels();

		assertTrue("Wrong number of features", featureRef.length == 1);
		assertTrue("Wrong number of archives", archives.length == 0);
		
		try { 
			((FeatureReference)featureRef[0]).getFeature();
		} catch (CoreException e){
			Throwable e1 = e.getStatus().getException();
				if (e1.getMessage().indexOf("not-eclipse")==-1){
					throw e;
				}
		}		
	}

	public void testParseValid6() throws Exception {

		URL remoteURL = new URL(SOURCE_FILE_SITE + "parsertests/site4.xml");
		DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
		URL resolvedURL = URLEncoder.encode(remoteURL);		
		SiteModel remoteSite = parser.parse(resolvedURL.openStream());
		remoteSite.resolve(remoteURL, null);

		FeatureReferenceModel[] featureRef = remoteSite.getFeatureReferenceModels();
		//CategoryModel[] categories = remoteSite.getCategoryModels();
		ArchiveReferenceModel[] archives = remoteSite.getArchiveReferenceModels();

		assertTrue("Wrong number of features", featureRef.length == 2);
		assertTrue("Wrong number of archives", archives.length == 0);
		
		try {
			((FeatureReference)featureRef[0]).getFeature();
		} catch (CoreException e){
			Throwable e1 = e.getStatus().getException();
			String msg = e1.getMessage().replace(File.separatorChar,'/');
				if (msg.indexOf("_1.0.0.jar/feature.xml")==-1){
					throw e;
				}
		}
	}

	public void testParseUnknownCategory() throws Exception {

		URL remoteURL = new URL(SOURCE_FILE_SITE + "parsertests/site3.xml");
		DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
		URL resolvedURL = URLEncoder.encode(remoteURL);		
		SiteModel remoteSite = parser.parse(resolvedURL.openStream());
		remoteSite.resolve(remoteURL, null);

		FeatureReferenceModel[] featureRef = remoteSite.getFeatureReferenceModels();
		ICategory[] categories = ((SiteFeatureReference)featureRef[0]).getCategories();
		assertTrue(categories.length==0);
	}	
	
		public void testParseValid7() throws Exception {

		try {
			URL remoteURL =
				new URL(SOURCE_FILE_SITE + "parsertests/site7.xml");
			DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
			URL resolvedURL = URLEncoder.encode(remoteURL);		
			SiteModel remoteSite = parser.parse(resolvedURL.openStream());
			remoteSite.resolve(remoteURL, null);

		} catch (SAXParseException e) {
			fail("Exception should not be thrown"+e.getMessage());			
		}
	}
	
	public void testParseValid8() throws Exception {

		try {
			URL remoteURL =
				new URL(SOURCE_FILE_SITE + "parsertests/site8.xml");
			DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
			URL resolvedURL = URLEncoder.encode(remoteURL);		
			SiteModel remoteSite = parser.parse(resolvedURL.openStream());
			remoteSite.resolve(remoteURL, null);

		} catch (SAXParseException e) {
			fail("Exception should not be thrown"+e.getMessage());			
		}
	}	
	
	public void testParseValid9() throws Exception {

		try {
			URL remoteURL =
				new URL(SOURCE_FILE_SITE + "parsertests/site9.xml");
			DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
			URL resolvedURL = URLEncoder.encode(remoteURL);		
			SiteModel remoteSite = parser.parse(resolvedURL.openStream());
			remoteSite.resolve(remoteURL, null);
		} catch (SAXParseException e) {
			fail("Exception should not be thrown"+e.getMessage());			
		}
	}		
	
	public void testParseValid10() throws Exception {

		SiteModel remoteSite= null;
		try {
			URL remoteURL =
				new URL(SOURCE_FILE_SITE + "parsertests/site10.xml");
			DefaultSiteParser parser = new DefaultSiteParser(new SiteFileFactory());
			URL resolvedURL = URLEncoder.encode(remoteURL);		
			remoteSite = parser.parse(resolvedURL.openStream());
			remoteSite.resolve(remoteURL, null);
		} catch (SAXParseException e) {
			fail("Exception should not be thrown"+e.getMessage());			
		}
		FeatureReferenceModel[] models = remoteSite.getFeatureReferenceModels();
		assertEquals("Invalid versioned identifier",models[0].getFeatureIdentifier(),"org.eclipse.test.feature");
	}			
	
}