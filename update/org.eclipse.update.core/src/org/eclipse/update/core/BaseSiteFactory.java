package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;

/**
 * Base implementation of a site factory.
 * The factory is responsible for constructing the correct
 * concrete implementation of the model objects for each particular
 * site type. This class creates model objects that correspond
 * to the concrete implementation classes provided in this package.
 * The actual site creation method is subclass responsibility.
 * <p>
 * This class must be subclassed by clients.
 * </p>
 * @see org.eclipse.update.core.ISiteFactory
 * @see org.eclipse.update.core.model.SiteModelFactory
 * @since 2.0
 */
public abstract class BaseSiteFactory extends SiteModelFactory implements ISiteFactory {

	// 
	private URLConnection connection = null;
	private IOException exception = null;

	/**
	 * Create site. Implementation of this method must be provided by 
	 * subclass
	 * 
	 * @see ISiteFactory#createSite(URL)
	 * @since 2.0
	 */
	public abstract ISite createSite(URL url) throws CoreException, InvalidSiteTypeException;

	/**
	 * Helper method to access resouce bundle for site. The default 
	 * implementation attempts to load the appropriately localized 
	 * site.properties file.
	 * 
	 * @param url base URL used to load the resource bundle.
	 * @return resource bundle, or <code>null</code>.
	 * @since 2.0
	 */
	protected ResourceBundle getResourceBundle(URL url) {
		ResourceBundle bundle = null;

		try {
			url = UpdateManagerUtils.asDirectoryURL(url);
			ClassLoader l = new URLClassLoader(new URL[] { url }, null);
			bundle = ResourceBundle.getBundle(Site.SITE_FILE, Locale.getDefault(), l);
		} catch (MissingResourceException e) {
			UpdateManagerPlugin.warn(e.getLocalizedMessage() + ":" + url.toExternalForm()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			UpdateManagerPlugin.warn(Policy.bind("BaseSiteFactory.CannotRetriveParentDirectory", url.toExternalForm()));  //$NON-NLS-1$
		}

		return bundle;
	}

	/**
	 * Create a concrete implementation of site model.
	 * 
	 * @see Site
	 * @return site model
	 * @since 2.0
	 */
	public SiteModel createSiteMapModel() {
		return new Site();
	}


	/**
	 * Create a concrete implementation of feature reference model.
	 * 
	 * @see FeatureReference
	 * @return feature reference model
	 * @since 2.0
	 */
	public SiteFeatureReferenceModel createFeatureReferenceModel() {
		return new SiteFeatureReference();
	}

	/**
	 * Create a concrete implementation of archive reference model.
	 * 
	 * @see ArchiveReference
	 * @return archive reference model
	 * @since 2.0
	 */
	public ArchiveReferenceModel createArchiveReferenceModel() {
		return new ArchiveReference();
	}


	/**
	 * Create a concrete implementation of annotated URL model.
	 * 
	 * @see URLEntry
	 * @return annotated URL model
	 * @since 2.0
	 */
	public URLEntryModel createURLEntryModel() {
		return new URLEntry();
	}


	/**
	 * Create a concrete implementation of category model.
	 * 
	 * @see Category
	 * @return category model
	 * @since 2.0
	 */
	public CategoryModel createSiteCategoryModel() {
		return new Category();
	}

	/**
	 * Open a stream on a URL.
	 * manages a time out if the connection is locked or fails
	 * 
	 * @param resolvedURL
	 * @return InputStream
	 */
	protected InputStream openStream(URL resolvedURL)  throws IOException {
		Response response = UpdateManagerPlugin.getPlugin().get(resolvedURL);
		return response.getInputStream();
	}

}