package org.eclipse.update.examples.freeform;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.core.BaseSiteFactory;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.Site;
import org.eclipse.update.core.model.InvalidSiteTypeException;
import org.eclipse.update.core.model.URLEntryModel;

public class FreeFormSiteFactory extends BaseSiteFactory {

	/*
	 * @see ISiteFactory#createSite(URL)
	 */
	public ISite createSite(URL url)
		throws CoreException, InvalidSiteTypeException {

		// Create site
		Site site = null;
		InputStream is = null;
		try {
			is = openStream(url);
			site = (Site) parseSite(is);

			URLEntryModel realSiteRef = site.getDescriptionModel();
			if (realSiteRef == null)
				throw Utilities.newCoreException(
					"Unable to obtain update site reference",
					null);
			String siteURLString = realSiteRef.getURLString();
			if (siteURLString == null)
				throw Utilities.newCoreException(
					"Unable to obtain update site reference",
					null);
			URL siteURL = new URL(siteURLString);
			FreeFormSiteContentProvider contentProvider =
				new FreeFormSiteContentProvider(siteURL);
			site.setSiteContentProvider(contentProvider);
			site.resolve(siteURL, null); // resolve any URLs relative to the site

		} catch (MalformedURLException e){
			throw Utilities.newCoreException("Unable to create URL",e);
		} catch (IOException e){
			throw Utilities.newCoreException("Unable to access URL",e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		return site;
	}

	/*
	 * @see SiteModelFactory#canParseSiteType(String)
	 */
	public boolean canParseSiteType(String type) {
		return type != null
			&& type.equals("org.eclipse.update.examples.site.freeform");
	}

}