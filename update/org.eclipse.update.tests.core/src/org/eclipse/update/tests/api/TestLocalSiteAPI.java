package org.eclipse.update.tests.api;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.URL;

import org.eclipse.update.core.*;
import org.eclipse.update.core.ICategory;
import org.eclipse.update.core.Category;
import org.eclipse.update.core.model.CategoryModel;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.core.model.*;
import org.eclipse.update.core.FeatureReference;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestLocalSiteAPI extends UpdateManagerTestCase {
	
	/**
	 * Test the getFeatures()
	 */
	public TestLocalSiteAPI(String arg0) {
		super(arg0);
	}
	
	public void testFileParsing() throws Exception {

		ISite site = SiteManager.getSite(new URL(SOURCE_FILE_SITE,"siteTestLocalSite/"));
		IArchiveReference[] info = site.getArchives();
		
		if (info.length<=0){
			fail("no plugins in site: "+site.getURL().toExternalForm());
		}
		 
		boolean found1 = false;
		boolean found2 = false;
		String name1= "plugins/org.eclipse.update.core.tests.feature1.plugin2_5.0.0.jar";
		String name2 = "plugins/org.eclipse.update.core.tests.feature1.plugin2_5.0.1.jar";
		
		for (int i =0; i<info.length; i++){
			if (info[i].getPath().equals(name1)) found1 = true;
			if (info[i].getPath().equals(name2)) found2 = true;
		}
		
		if (!found1 || !found2){
			fail("Cannot find plugin : org.eclipse.update.core.tests.feature1.plugin2 version 5.0.0 and 5.0.1 on the site, by parsing file system");
		}
		

	}
	
	
	public void testCategories() throws Exception {

		ISite site = SiteManager.getSite(SOURCE_FILE_SITE);
		
		ICategory category = new Category("category","Label of category");
		
		((SiteModel)site).addCategoryModel((CategoryModel)category);
		
		ICategory[] categories = site.getCategories();
		boolean found = false;
		for (int index = 0; index < categories.length; index++) {
			ICategory element = categories[index];
			if (element.getName().equals("category")) found = true;
		}
		if (!found) fail("cannot find category 'category' in site");

		ISiteFeatureReference ref = new SiteFeatureReference();
		ref.setSite(site);
		ref.addCategory(category);		
		categories = ref.getCategories();
		found = false;
		for (int index = 0; index < categories.length; index++) {
			ICategory element = categories[index];
			if (element.getName().equals("category")) found = true;
		}
		if (!found) fail("cannot find category 'category' in feature");
		
			}

}

