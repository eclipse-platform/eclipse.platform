package org.eclipse.update.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.model.IncludedFeatureReferenceModel;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * This is a utility class representing the options of a nested feature.
 * Feature will include other features. This class will represent the options of the inclusion.
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.VersionedIdentifier
 * @since 2.0.1
 */
public class IncludedFeatureReference extends IncludedFeatureReferenceModel implements IIncludedFeatureReference {
	
	private IFeature feature;	 

	/**
	 * Construct a included feature reference
	 * 
	 * @since 2.1
	 */
	public IncludedFeatureReference() {
		super();
	}


	/**
	 * Construct a feature options 
	 * 
	 * @param name string representation of the feature
	 * @param isOptional <code>true</code> if the feature is optional, <code>false</code> otherwise.
	 * @param matchingRule the matching rule
	 * @param searchLocation the location to search for this feature's updates.
	 * @since 2.0.2
	 */
	public IncludedFeatureReference(IIncludedFeatureReference includedFeatureRef) {
		super((IncludedFeatureReferenceModel)includedFeatureRef);
	}

	/**
	 * Constructor IncludedFeatureReference.
	 * @param iFeatureReference
	 */
	public IncludedFeatureReference(IFeatureReference featureReference) {
		super(featureReference);
	}


	/**
	* Method matches.
	* @param identifier
	* @param id
	* @param options
	* @return boolean
	*/
	private boolean matches(VersionedIdentifier baseIdentifier, VersionedIdentifier id) {
		if (baseIdentifier == null || id == null)
			return false;
		if (!id.getIdentifier().equals(baseIdentifier.getIdentifier()))
			return false;

		switch (getMatch()) {
			case IImport.RULE_PERFECT :
				return id.getVersion().isPerfect(baseIdentifier.getVersion());
			case IImport.RULE_COMPATIBLE :
				return id.getVersion().isCompatibleWith(baseIdentifier.getVersion());
			case IImport.RULE_EQUIVALENT :
				return id.getVersion().isEquivalentTo(baseIdentifier.getVersion());
			case IImport.RULE_GREATER_OR_EQUAL :
				return id.getVersion().isGreaterOrEqualTo(baseIdentifier.getVersion());
		}
		UpdateManagerPlugin.warn("Unknown matching rule:" + getMatch());
		return false;
	}


	/*
	 * Method retrieveEnabledFeatures.
	 * @param site
	 */
	private IFeatureReference[] retrieveEnabledFeatures(ISite site) {
		IConfiguredSite configuredSite = site.getCurrentConfiguredSite();
		if (configuredSite == null)
			return new IFeatureReference[0];
		return configuredSite.getConfiguredFeatures();
	}
	
	/*
	 * Method isDisabled.
	 * @return boolean
	 */
	private boolean isDisabled() {
		/*IConfiguredSite cSite = getSite().getConfiguredSite();
		if (cSite==null) return false;
		IFeatureReference[] configured = cSite.getConfiguredFeatures();
		for (int i = 0; i < configured.length; i++) {
			if (this.equals(configured[i])) return false;
		}
		return true;*/
		// FIXME
		return false;
	}
	
	/**
	 * @see org.eclipse.update.core.IFeatureReference#getFeature(boolean)
	 */
	public IFeature getFeature(boolean perfectMatch,IConfiguredSite configuredSite) throws CoreException {

		if (configuredSite==null)
			configuredSite = getSite().getCurrentConfiguredSite();
		
		// if perfect match is asked or if the feature is disabled
		// we return the exact match 		
		if (perfectMatch || getMatch() == IImport.RULE_PERFECT || isDisabled()) {
			return getFeature(this);
		} else {
			if (feature == null) {
				// find best match
				IFeatureReference bestMatch = getBestMatch(configuredSite);
				feature = getFeature(bestMatch);
			}
			return feature;
		}
	}
	
	/*
	 * 
	 */
	private IFeature getFeature(IFeatureReference ref) throws CoreException {
		String type = getType();
		if (type == null || type.equals("")) { //$NON-NLS-1$
			// ask the Site for the default type 
			type = getSite().getDefaultPackagedFeatureType();
		}
		return getSite().createFeature(type, ref.getURL());
	}
	
	/*
	 * Method getBestMatch.
	 * @param enabledFeatures
	 * @param identifier
	 * @param options
	 * @return Object
	 */
	private IIncludedFeatureReference getBestMatch(IConfiguredSite configuredSite) throws CoreException {
		IncludedFeatureReference newRef = null;

		if (configuredSite==null) return this;
		IFeatureReference[] enabledFeatures = configuredSite.getConfiguredFeatures();

		// find the best feature based on match from enabled features
		for (int ref = 0; ref < enabledFeatures.length; ref++) {
			if (enabledFeatures[ref] != null) {
				VersionedIdentifier id = null;
				try {
					id = enabledFeatures[ref].getVersionedIdentifier();
				} catch (CoreException e) {
					UpdateManagerPlugin.warn(null, e);
				};
				if (matches(getVersionedIdentifier(), id)) {
					if (newRef == null || id.getVersion().isGreaterThan(newRef.getVersionedIdentifier().getVersion())) {
						newRef = new IncludedFeatureReference(enabledFeatures[ref]);
						newRef.setMatchingRule(getMatch());
						newRef.isOptional(isOptional());
						newRef.setName(getName());
					}
				}
			}
		}

		if (newRef != null)
			return newRef;
		else 
			return this;
	}			
	/**
	 * @see org.eclipse.update.core.IFeatureReference#getFeature()
	 */
	public IFeature getFeature() throws CoreException {
		return getFeature(false,null);
	}

	/**
	 * @see org.eclipse.update.core.IIncludedFeatureReference#matchesPlatform()
	 */
	public boolean matchesPlatform() {
		if (getWS()!=null && !SiteManager.getWS().equalsIgnoreCase(getWS()))
			return false;
		if (getOS() != null && !SiteManager.getOS().equalsIgnoreCase(getOS()))
			return false;
		if (getOSArch()!= null && !SiteManager.getOSArch().equalsIgnoreCase(getOSArch()))
			return false;
		return true;		
	}

}