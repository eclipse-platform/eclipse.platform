/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;
 
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.model.ArchiveReferenceModel;
import org.eclipse.update.core.model.CategoryModel;
import org.eclipse.update.core.model.InvalidSiteTypeException;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.core.model.SiteModelFactory;
import org.eclipse.update.core.model.URLEntryModel;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.internal.core.connection.ConnectionFactory;
import org.eclipse.update.internal.core.connection.IResponse;

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
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.ISiteFactory
 * @see org.eclipse.update.core.model.SiteModelFactory
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public abstract class BaseSiteFactory extends SiteModelFactory implements ISiteFactory {


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
			UpdateCore.warn(e.getLocalizedMessage() + ":" + url.toExternalForm()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			UpdateCore.warn(NLS.bind(Messages.BaseSiteFactory_CannotRetriveParentDirectory, (new String[] { url.toExternalForm() })));
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
		IResponse response = ConnectionFactory.get(resolvedURL);
		return response.getInputStream();
	}

}
