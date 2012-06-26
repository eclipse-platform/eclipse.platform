/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core.model;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Install handler entry model object.
 * An object which represents the definition of a custom install handler
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.InstallHandlerEntry
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class InstallHandlerEntryModel extends ModelObject {

	private String urlString;
	private URL url;
	private String library;
	private String name;

	/**
	 * Creates a uninitialized install handler entry model object.
	 * 
	 * @since 2.0
	 */
	public InstallHandlerEntryModel() {
		super();
	}

	/**
	 * Returns the URL string used for browser-triggered installation handling.
	 *
	 * @return url string or <code>null</code>
	 * @since 2.0
	 */
	public String getURLString() {
		return urlString;
	}

	/**
	 * Returns the resolved URL used for browser-triggered installation handling.
	 * 
	 * @return url, or <code>null</code>
	 * @since 2.0
	 */
	public URL getURL() {
		return url;
	}

	/**
	 * Returns the name of the custom installer library.
	 *
	 * @return library path, or <code>null</code>
	 * @since 2.0
	 */
	public String getLibrary() {
		return library;
	}

	/**
	 * Returns the name of the custom installer.
	 *
	 * @return handler name, or <code>null</code>
	 * @since 2.0
	 */
	public String getHandlerName() {
		return name;
	}

	/**
	 * Sets URL string used for browser-triggered installation handling.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param urlString trigget page URL string, may be <code>null</code>.
	 * @since 2.0
	 */
	public void setURLString(String urlString) {
		assertIsWriteable();
		this.urlString = urlString;
		this.url = null;
	}

	/**
	 * Sets the custom install handler library name.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param library name, may be <code>null</code>.
	 * @since 2.0
	 */
	public void setLibrary(String library) {
		assertIsWriteable();
		this.library = library;
	}

	/**
	 * Sets the name of the custom install handler.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param name name of the install handler, may be <code>null</code>.
	 * @since 2.0
	 */
	public void setHandlerName(String name) {
		assertIsWriteable();
		this.name = name;
	}

	/**
	 * Resolve the model object.
	 * Any URL strings in the model are resolved relative to the 
	 * base URL argument. Any translatable strings in the model that are
	 * specified as translation keys are localized using the supplied 
	 * resource bundle.
	 * 
	 * @param base URL
	 * @param bundleURL resource bundle URL
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	public void resolve(URL base,URL bundleURL)
		throws MalformedURLException {
		// resolve local elements
		url = resolveURL(base,bundleURL, urlString);
	}
}
