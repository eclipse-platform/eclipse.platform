package org.eclipse.update.tests.api;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.core.FeatureReference;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestNestedFeatureAPI extends UpdateManagerTestCase {

	/**
	 * Test the testFeatures()
	 */
	public TestNestedFeatureAPI(String arg0) {
		super(arg0);
	}

	/**
	 * the feature to test
	 */
	private IFeature getRootFeature() throws MalformedURLException, CoreException {
		ISite site = SiteManager.getSite(new URL(SOURCE_FILE_SITE,"nestedFeatureSiteTest/"));
		URL url =
			UpdateManagerUtils.getURL(
				site.getURL(),
					Site.DEFAULT_INSTALLED_FEATURE_PATH
					+ "rootfeature.jar",
				null);
		SiteFeatureReference ref = new SiteFeatureReference();
		ref.setSite(site);
		ref.setURL(url);
		ref.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
		IFeature feature = ref.getFeature();
		return feature;
	}

	/**
	 * the feature to test
	 */
	private IFeature getChildFeature()
		throws MalformedURLException, CoreException {

		ISite site = SiteManager.getSite(new URL(SOURCE_FILE_SITE,"nestedFeatureSiteTest/"));
		URL url =
			UpdateManagerUtils.getURL(
				site.getURL(),
					 Site.DEFAULT_INSTALLED_FEATURE_PATH
					+ "org.eclipse.update.core.tests.childrenfeature_2.0.0.jar",
				null);
		SiteFeatureReference ref = new SiteFeatureReference();
		ref.setSite(site);
		ref.setURL(url);
		ref.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
		IFeature feature = ref.getFeature();
		return feature;
	}

	/*
	 * 
	 */
	public void testNested() throws Exception {

		IFeature rootFeature = getRootFeature();
		IFeatureReference[] ref = rootFeature.getIncludedFeatureReferences();
		IFeature childFeature = getChildFeature();
		assertEquals(
			"Children feature are not equal",
			childFeature,
			ref[0].getFeature());
	}



	/**
	 * the feature to test
	 */
	private IFeature getRootFeature2() throws MalformedURLException, CoreException {
		ISite site = SiteManager.getSite(new URL(SOURCE_FILE_SITE,"nestedFeatureSiteTest2/"));
		URL url =
			UpdateManagerUtils.getURL(
				site.getURL(),
					Site.DEFAULT_INSTALLED_FEATURE_PATH
					+ "rootfeature.jar",
				null);
		SiteFeatureReference ref = new SiteFeatureReference();
		ref.setSite(site);
		ref.setURL(url);
		ref.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
		IFeature feature = ref.getFeature();
		return feature;
	}

	/**
	 * the feature to test
	 */
	private IFeature getChildFeature2()
		throws MalformedURLException, CoreException {

		ISite site = SiteManager.getSite(new URL(SOURCE_FILE_SITE,"nestedFeatureSiteTest2/"));
		URL url =
			UpdateManagerUtils.getURL(
				site.getURL(),
					 Site.DEFAULT_INSTALLED_FEATURE_PATH
					+ "childrenfeature.jar",
				null);
		SiteFeatureReference ref = new SiteFeatureReference();
		ref.setSite(site);
		ref.setURL(url);
		ref.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
		IFeature feature = ref.getFeature();
		return feature;
	}


	/*
	 * 
	 */
	public void testNested2() throws Exception {

		IFeature rootFeature = getRootFeature2();
		IFeatureReference[] ref = rootFeature.getIncludedFeatureReferences();
		IFeature childFeature = getChildFeature2();
		assertEquals(
			"Children feature are not equal",
			childFeature,
			ref[0].getFeature());
	}


	/**
	 * the feature to test
	 */
	private IFeature getRootFeature3() throws MalformedURLException, CoreException {
		ISite site = SiteManager.getSite(new URL(SOURCE_FILE_SITE,"nestedFeatureSiteTest3/"));
		URL url =
			UpdateManagerUtils.getURL(
				site.getURL(),
					Site.DEFAULT_INSTALLED_FEATURE_PATH
					+ "rootfeature.jar",
				null);
		SiteFeatureReference ref = new SiteFeatureReference();
		ref.setSite(site);
		ref.setURL(url);
		ref.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
		IFeature feature = ref.getFeature();
		return feature;
	}

	/**
	 * the feature to test
	 */
	private IFeature getChildFeature3()
		throws MalformedURLException, CoreException {

		ISite site = SiteManager.getSite(new URL(SOURCE_FILE_SITE,"nestedFeatureSiteTest3/"));
		URL url =
			UpdateManagerUtils.getURL(
				site.getURL(),
					 Site.DEFAULT_INSTALLED_FEATURE_PATH
					+ "childrenfeature.jar",
				null);
		SiteFeatureReference ref = new SiteFeatureReference();
		ref.setSite(site);
		ref.setURL(url);
		ref.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
		IFeature feature = ref.getFeature();
		return feature;
	}


	/*
	 * 
	 */
	public void testNested3() throws Exception {

		IFeature rootFeature = getRootFeature3();
		IFeatureReference[] ref = rootFeature.getIncludedFeatureReferences();
		IFeature childFeature = getChildFeature3();
		assertEquals(
			"Children feature are not equal",
			childFeature,
			ref[0].getFeature());
	}


}