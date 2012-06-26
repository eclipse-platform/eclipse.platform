/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 191783, NullPointerException in FeatureDownloader
 *******************************************************************************/
package org.eclipse.update.core.model;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.update.core.Site;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;

/**
 * Feature reference model object.
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
 * @see org.eclipse.update.core.FeatureReference
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class FeatureReferenceModel extends ModelObject {

	private String type;
	private URL url;
	private String urlString;
	private String featureId;
	private String featureVersion;
	private SiteModel site;
	private String label;
	private String localizedLabel;

	// performance
	private URL bundleURL;
	private URL base;
	private boolean resolved = false;
	private String os;
	private String ws;
	private String nl;
	private String arch;
	private String patch;	

	/**
	 * Creates an uninitialized feature reference model object.
	 * 
	 * @since 2.0
	 */
	public FeatureReferenceModel() {
		super();
	}

	/**
	 * Constructor FeatureReferenceModel.
	 * @param ref
	 */
	public FeatureReferenceModel(FeatureReferenceModel ref) {
		setFeatureIdentifier(ref.getFeatureIdentifier());
		setFeatureVersion(ref.getFeatureVersion());
		setType(ref.getType());
		setSiteModel(ref.getSiteModel());
		setLabel(ref.getLabel());
		setWS(ref.getWS());
		setOS(ref.getOS());
		setArch(ref.getOSArch());
		setNL(ref.getNL());
	}

	/**
	 * Compares 2 feature reference models for equality
	 *  
	 * @param object feature reference model to compare with
	 * @return <code>true</code> if the two models are equal, 
	 * <code>false</code> otherwise
	 * @since 2.0 
	 */
	public boolean equals(Object object) {

		if (object == null)
			return false;
		if (getURL() == null)
			return false;

		if (!(object instanceof FeatureReferenceModel))
			return false;

		FeatureReferenceModel f = (FeatureReferenceModel) object;

		return UpdateManagerUtils.sameURL(getURL(), f.getURL());
	}

	/**
	 * Returns the referenced feature type.
	 * 
	 * @return feature type, or <code>null</code> representing the default
	 * feature type for the site
	 * @since 2.0
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the site model for the reference.
	 * 
	 * @return site model
	 * @since 2.0
	 */
	public SiteModel getSiteModel() {
		return site;
	}

	/**
	 * Returns the unresolved URL string for the reference.
	 *
	 * @return url string
	 * @since 2.0
	 */
	public String getURLString() {
		return urlString;
	}

	/**
	 * Returns the resolved URL for the feature reference.
	 * 
	 * @return url string
	 * @since 2.0
	 */
	public URL getURL() {
		delayedResolve();
		return url;
	}

	/**
	 * Returns the feature identifier as a string
	 * 
	 * @see org.eclipse.update.core.IFeatureReference#getVersionedIdentifier()
	 * @return feature identifier
	 * @since 2.0
	 */
	public String getFeatureIdentifier() {
		return featureId;
	}

	/**
	 * Returns the feature version as a string
	 * 
	 * @see org.eclipse.update.core.IFeatureReference#getVersionedIdentifier()
	 * @return feature version 
	 * @since 2.0
	 */
	public String getFeatureVersion() {
		return featureVersion;
	}

	/**
	 * Sets the referenced feature type.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param type referenced feature type
	 * @since 2.0
	 */
	public void setType(String type) {
		assertIsWriteable();
		this.type = type;
	}

	/**
	 * Sets the site for the referenced.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param site site for the reference
	 * @since 2.0
	 */
	public void setSiteModel(SiteModel site) {
		assertIsWriteable();
		this.site = site;
	}

	/**
	 * Sets the unresolved URL for the feature reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param urlString unresolved URL string
	 * @since 2.0
	 */
	public void setURLString(String urlString) {
		assertIsWriteable();
		this.urlString = urlString;
		this.url = null;
	}

	/**
	 * Sets the feature identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureId feature identifier
	 * @since 2.0
	 */
	public void setFeatureIdentifier(String featureId) {
		assertIsWriteable();
		this.featureId = featureId;
	}

	/**
	 * Sets the feature version.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureVersion feature version
	 * @since 2.0
	 */
	public void setFeatureVersion(String featureVersion) {
		assertIsWriteable();
		this.featureVersion = featureVersion;
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
	public void resolve(URL base,URL bundleURL) throws MalformedURLException {
		this.base = base;
		this.bundleURL = bundleURL;
	}

	private void delayedResolve() {

		// PERF: delay resolution
		if (resolved)
			return;

		// resolve local elements
		localizedLabel = resolveNLString(bundleURL, label);
		try {
			url = resolveURL(base, bundleURL, urlString);
			resolved = true;
		} catch (MalformedURLException e){
			UpdateCore.warn("",e); //$NON-NLS-1$
		}
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getClass().toString() + " :"); //$NON-NLS-1$
		buffer.append(" at "); //$NON-NLS-1$
		if (url != null)
			buffer.append(url.toExternalForm());
		return buffer.toString();
	}

	/**
	 * @see org.eclipse.update.core.model.ModelObject#getPropertyName()
	 */
	protected String getPropertyName() {
		return Site.SITE_FILE;
	}
	
	/**
	 * Retrieve the displayable label for the feature reference. If the model
	 * object has been resolved, the label is localized.
	 *
	 * @return displayable label, or <code>null</code>.
	 * @since 2.0
	 */
	public String getLabel() {
		delayedResolve();
		if (localizedLabel != null)
			return localizedLabel;
		else
			return label;
	}

	/**
	 * Retrieve the non-localized displayable label for the feature reference.
	 *
	 * @return non-localized displayable label, or <code>null</code>.
	 * @since 2.0
	 */
	public String getLabelNonLocalized() {
		return label;
	}

	/**
	 * Sets the label.
	 * @param label The label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Get optional operating system specification as a comma-separated string.
	 *
	 * @return the operating system specification string, or <code>null</code>.
	 * @since 2.1
	 */
	public String getOS() {
		return os;
	}


	/**
	 * Get optional windowing system specification as a comma-separated string.
	 *
	 * @return the windowing system specification string, or <code>null</code>.
	 * @since 2.1
	 */
	public String getWS() {
		return ws;
	}


	/**
	 * Get optional system architecture specification as a comma-separated string.
	 *
	 * @return the system architecture specification string, or <code>null</code>.
	 * @since 2.1
	 */
	public String getOSArch() {
		return arch;
	}


	/**
	 * Get optional locale specification as a comma-separated string.
	 *
	 * @return the locale specification string, or <code>null</code>.
	 * @since 2.1
	 */
	public String getNL() {
		return nl;
	}

	/**
	 * Sets the operating system specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param os operating system specification as a comma-separated list
	 * @since 2.1
	 */
	public void setOS(String os) {
		assertIsWriteable();
		this.os = os;
	}


	/**
	 * Sets the windowing system specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param ws windowing system specification as a comma-separated list
	 * @since 2.1
	 */
	public void setWS(String ws) {
		assertIsWriteable();
		this.ws = ws;
	}


	/**
	 * Sets the locale specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param nl locale specification as a comma-separated list
	 * @since 2.1
	 */
	public void setNL(String nl) {
		assertIsWriteable();
		this.nl = nl;
	}


	/**
	 * Sets the system architecture specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param arch system architecture specification as a comma-separated list
	 * @since 2.1
	 */
	public void setArch(String arch) {
		assertIsWriteable();
		this.arch = arch;
	}

	/**
	 * Returns the patch mode.
	 */
	public String getPatch() {
		return patch;
	}


	/**
	 * Sets the patch mode.
	 */
	public void setPatch(String patch) {
		this.patch = patch;
	}

}
