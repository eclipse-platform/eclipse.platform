package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.core.*;

/**
 * Convenience implementation of a feature reference.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.IFeatureReference
 * @see org.eclipse.update.core.model.FeatureReferenceModel
 * @since 2.0
 */
public class FeatureReference extends FeatureReferenceModel implements IFeatureReference {

	private List categories;
	private VersionedIdentifier versionId;
 
	/**
	 * Feature reference default constructor
	 */
	public FeatureReference() {
		super();
	}

	/**
	 * Constructor FeatureReference.
	 * @param ref the reference to copy
	 */
	public FeatureReference(IFeatureReference ref) {
		super((FeatureReferenceModel) ref);
		try {
			setURL(ref.getURL());
		} catch (CoreException e) {
			UpdateManagerPlugin.warn("", e);
		}
	}

	/**
	 * Constructor FeatureReference.
	 * @param ref the reference to copy
	 */
	public FeatureReference(FeatureReferenceModel ref) {
		super(ref);
		try {
			setURL(ref.getURL());
		} catch (CoreException e) {
			UpdateManagerPlugin.warn("", e);
		}
	}
	
	/**
	 * Returns the feature this reference points to based on match and resolution
	 *  @return the feature on the Site
	 */
	public IFeature getFeature() throws CoreException {
		String type = getType();
		if (type == null || type.equals("")) { //$NON-NLS-1$
			// ask the Site for the default type 
			type = getSite().getDefaultPackagedFeatureType();
		}
		return getSite().createFeature(type, this.getURL());
	}

	/**
	 * Returns the update site for the referenced feature
	 * 
	 * @see IFeatureReference#getSite()
	 * @since 2.0 
	 */
	public ISite getSite() {
		return (ISite) getSiteModel();
	}
	
	/** 
	 * Sets the feature reference URL.
	 * This is typically performed as part of the feature reference creation
	 * operation. Once set, the url should not be reset.
	 * 
	 * @see IFeatureReference#setURL(URL)
	 * @since 2.0 
	 */
	public void setURL(URL url) throws CoreException {
		if (url != null) {
			setURLString(url.toExternalForm());
			try {
				resolve(url, null);
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(Policy.bind("FeatureReference.UnableToResolveURL", url.toExternalForm()), e);
				//$NON-NLS-1$
			}
		}
	}

	/**
	 * Associates a site with the feature reference.
	 * This is typically performed as part of the feature reference creation
	 * operation. Once set, the site should not be reset.
	 * 
	 * @see IFeatureReference#setSite(ISite)
	 * @since 2.0 
	 */
	public void setSite(ISite site) {
		setSiteModel((SiteModel) site);
	}

	/**
	* Returns the feature identifier.
	* 
	* @see IFeatureReference#getVersionedIdentifier()
	* @since 2.0
	*/
	public VersionedIdentifier getVersionedIdentifier() {

		if (versionId != null)
			return versionId;

		String id = getFeatureIdentifier();
		String ver = getFeatureVersion();
		if (id != null && ver != null) {
			try {
				versionId = new VersionedIdentifier(id, ver);
				return versionId;
			} catch (Exception e) {
				UpdateManagerPlugin.warn("Unable to create versioned identifier:" + id + ":" + ver);
			}
		}

		// we need the exact match or we may have an infinite loop
		versionId = new VersionedIdentifier(getURL().toExternalForm(),null);
		try {
			versionId = getFeature().getVersionedIdentifier();
		} catch (CoreException e){
			UpdateManagerPlugin.warn("",e);
		}
		return versionId;
	}
	
	
}