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

import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.internal.core.UpdateCore;

/**
 * Annotated URL model object.
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
 * @see org.eclipse.update.core.URLEntry
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class URLEntryModel extends ModelObject {
	
	private String annotation;
	private String localizedAnnotation;
	private String urlString;
	private URL url;
	
	private int type = IURLEntry.UPDATE_SITE;
	
	//performance
	private URL bundleURL;
	private URL base;
	private boolean resolved=false;
	
	/**
	 * Creates a uninitialized annotated URL model object.
	 * 
	 * @since 2.0
	 */
	public URLEntryModel() {
		super();
	}
		
	/**
	 * Returns the url annotation. If the model object has been resolved, 
	 * the annotation is localized.
	 * 
	 * @return url annotation, or <code>null</code>.
	 * @since 2.0
	 */
	public String getAnnotation() {
		delayedResolve();
		if (localizedAnnotation != null)
			return localizedAnnotation;
		else
			return annotation;
	}
		
	/**
	 * returns the non-localized url annotation.
	 * 
	 * @return non-localized url annotation, or <code>null</code>.
	 * @since 2.0
	 */
	public String getAnnotationNonLocalized() {
		return annotation;
	}

	/**
	 * Returns the unresolved url string.
	 *
	 * @return url string, or <code>null</code>
	 * @since 2.0
	 */
	public String getURLString() {
		delayedResolve();
		return urlString;
	}
	
	/**
	 * Returns the resolved URL.
	 * 
	 * @return url, or <code>null</code>
	 * @since 2.0
	 */
	public URL getURL() {
		delayedResolve();
		return url;
	}
	
	/**
	 * Sets the annotation.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param annotation annotation
	 * @since 2.0
	 */	
	public void setAnnotation(String annotation) {
		assertIsWriteable();
		this.annotation = annotation;
		this.localizedAnnotation = null;
	}
	
	/**
	 * Sets the url string
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param urlString url string
	 * @since 2.0
	 */	
	public void setURLString(String urlString) {
		assertIsWriteable();
		this.urlString = urlString;
		this.url = null;
	}
	
	/**
	 * Resolve the model object.
	 * Any URL strings in the model are resolved relative to the 
	 * base URL argument. Any translatable strings in the model that are
	 * specified as translation keys are localized using the supplied 
	 * resource bundle.
	 * 
	 * @param base URL
	 * @param bundleURL  resource bundle url
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	public void resolve(URL base, URL bundleURL) throws MalformedURLException {
		this.base = base;
		this.bundleURL = bundleURL;
	}


	private void delayedResolve() {
		
		//PERF: delay resolution
		if (resolved)return;
		
		resolved= true;
		// resolve local elements
		localizedAnnotation = resolveNLString(bundleURL, annotation);
		try {
			url = resolveURL(base,bundleURL, urlString);
		} catch (MalformedURLException e){
			UpdateCore.warn("",e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the specified type.
	 * 
	 * @since 2.1
	 */
	public int getType() {
		return type;
	}

	/**
	 * Method setType.
	 * @param i
	 */
	public void setType(int i) {
		type = i;
	}
}
