package org.eclipse.update.tests.parser;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.URL;

import org.eclipse.update.core.*;
import org.eclipse.update.core.SiteFeatureReference;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestCategories extends UpdateManagerTestCase {
	/**
	 * Constructor for Test1
	 */
	public TestCategories(String arg0) {
		super(arg0);
	}

	public void testCategories() throws Exception {

		URL remoteUrl = new URL(SOURCE_FILE_SITE + "xmls/site1/");
		ISite remoteSite = SiteManager.getSite(remoteUrl);

		ISiteFeatureReference[] feature = remoteSite.getFeatureReferences();
		//ICategory[] categories = remoteSite.getCategories();

		ICategory featureCategory = feature[0].getCategories()[0];

		assertEquals("UML tools", featureCategory.getLabel());

	}

	public void testOrderedCategories() throws Exception {

		URL remoteUrl = new URL(SOURCE_FILE_SITE + "xmls/site1/");
		ISite remoteSite = SiteManager.getSite(remoteUrl);

		ICategory[] categories = remoteSite.getCategories();
		for (int i = 0; i < categories.length; i++) {
			System.out.println("Cat ordered->" + categories[i].getName());
		}

		assertEquals("Eclipse tools", categories[1].getLabel());

	}

	public void testTranslatedCategories() throws Exception {

		ISite remoteSite = SiteManager.getSite(SOURCE_HTTP_SITE);

		ICategory[] categories = remoteSite.getCategories();

		assertEquals("Required Drivers", categories[0].getLabel());

	}
}