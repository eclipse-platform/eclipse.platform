package org.eclipse.update.examples.buildzip;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.update.core.*;
import org.eclipse.update.core.BaseSiteFactory;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.Site;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.InvalidSiteTypeException;
import org.eclipse.update.core.model.CategoryModel;

public class BuildZipSiteFactory extends BaseSiteFactory {

	/*
	 * @see ISiteFactory#createSite(URL)
	 */
	public ISite createSite(URL url) throws CoreException,InvalidSiteTypeException {

		Site site = null;

		// create site and add category
		site = (Site) createSiteMapModel();
		CategoryModel category = createSiteCategoryModel();
		category.setName("eclipse-builds");
		category.setLabel("Eclipse Builds");
		site.addCategoryModel(category);

		// set content provider
		BuildZipSiteContentProvider contentProvider = new BuildZipSiteContentProvider(url);
		site.setSiteContentProvider(contentProvider);

		// get all matching zip files on the site
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				if (file.getName().endsWith(".zip")) {
					try {
						ZipFile zip = new ZipFile(file);
						ZipEntry entry = zip.getEntry("eclipse/buildmanifest.properties");
						if (entry == null)
							return false;
						else
							return true;
					} catch (IOException e) {
						return false;
					}
				} else
					return false;
			}
		};
		File file = new File(URLDecoder.decode(url.getFile()));
		File[] zips = file.listFiles(filter);

		// create a reference for each matching zip
		SiteFeatureReferenceModel ref = null;
		for (int i = 0; zips != null && i < zips.length; i++) {
			ref = createFeatureReferenceModel();
			ref.setType("org.eclipse.update.examples.zip");
			ref.setSiteModel(site);
			ref.setURLString("file:" + zips[i].getAbsolutePath());
			ref.setCategoryNames(new String[] { "eclipse-builds" });
			site.addFeatureReferenceModel(ref);
		}
		try {
			site.resolve(url, null); // resolve any URLs relative to the site
		} catch (MalformedURLException e){
			throw Utilities.newCoreException("",e);
		}

		return site;
	}

}