package org.eclipse.update.core.model;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.update.core.IIncludedFeatureReference;
import org.eclipse.update.core.VersionedIdentifier;

/**
 * Feature model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * @see org.eclipse.update.core.Feature
 * @since 2.0
 */
public class FeatureModel extends ModelObject {

	private String featureId;
	private String featureVersion;
	private String label;
	private String localizedLabel;
	private String provider;
	private String localizedProvider;
	private String imageURLString;
	private URL imageURL;
	private String os;
	private String ws;
	private String nl;
	private String arch;
	private boolean isPrimary = false;
	private String application;
	private String affinity;
	private InstallHandlerEntryModel installHandler;
	private URLEntryModel description;
	private URLEntryModel copyright;
	private URLEntryModel license;
	private URLEntryModel updateSiteInfo;
	private List /*of InfoModel*/
	discoverySiteInfo;
	private List /*of ImportModel*/
	imports;
	private List /*of PluginEntryModel*/
	pluginEntries;
	private List /*of IncludedFeatureReferenceModel */	
	featureIncludes;
	private List /*of NonPluginEntryModel*/
	nonPluginEntries;
	private List /*of ContentGroupModel*/
	groupEntries;
	
	/**
	 * Creates an uninitialized feature object.
	 * 
	 * @since 2.0
	 */
	public FeatureModel() {
		super();
	}

	/**
	 * Compares 2 feature models for equality
	 *  
	 * @param obj feature model to compare with
	 * @return <code>true</code> if the two models are equal, 
	 * <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof FeatureModel))
			return false;
		FeatureModel model = (FeatureModel) obj;

		return (
			featureId.toLowerCase().equals(model.getFeatureIdentifier())
				&& featureVersion.toLowerCase().equals(model.getFeatureVersion()));
	}

	/**
	 * Returns the feature identifier as a string
	 * 
	 * @see org.eclipse.update.core.IFeature#getVersionedIdentifier()
	 * @return feature identifier
	 * @since 2.0
	 */
	public String getFeatureIdentifier() {
		return featureId;
	}

	/**
	 * Returns the feature version as a string
	 * 
	 * @see org.eclipse.update.core.IFeature#getVersionedIdentifier()
	 * @return feature version 
	 * @since 2.0
	 */
	public String getFeatureVersion() {
		return featureVersion;
	}

	/**
	 * Retrieve the displayable label for the feature. If the model
	 * object has been resolved, the label is localized.
	 * 
	 * @return displayable label, or <code>null</code>.
	 * @since 2.0
	 */
	public String getLabel() {
		if (localizedLabel != null)
			return localizedLabel;
		else
			return label;
	}

	/**
	 * Retrieve the non-localized displayable label for the feature.
	 * 
	 * @return non-localized displayable label, or <code>null</code>.
	 * @since 2.0
	 */
	public String getLabelNonLocalized() {
		return label;
	}

	/**
	 * Retrieve the displayable label for the feature provider. If the model
	 * object has been resolved, the label is localized.
	 * 
	 * @return displayable label, or <code>null</code>.
	 * @since 2.0
	 */
	public String getProvider() {
		if (localizedProvider != null)
			return localizedProvider;
		else
			return provider;
	}

	/**
	 * Retrieve the non-localized displayable label for the feature provider.
	 * 
	 * @return non-localized displayable label, or <code>null</code>.
	 * @since 2.0
	 */
	public String getProviderNonLocalized() {
		return provider;
	}

	/**
	 * Returns the unresolved URL string for the feature image.
	 *
	 * @return url string, or <code>null</code>
	 * @since 2.0
	 */
	public String getImageURLString() {
		return imageURLString;
	}

	/**
	 * Returns the resolved URL for the image.
	 * 
	 * @return url, or <code>null</code>
	 * @since 2.0
	 */
	public URL getImageURL() {
		return imageURL;
	}

	/**
	 * Get optional operating system specification as a comma-separated string.
	 * 
	 * @see org.eclipse.core.boot.BootLoader 
	 * @return the operating system specification string, or <code>null</code>.
	 * @since 2.0
	 */
	public String getOS() {
		return os;
	}

	/**
	 * Get optional windowing system specification as a comma-separated string.
	 * 
	 * @see org.eclipse.core.boot.BootLoader 
	 * @return the windowing system specification string, or <code>null</code>.
	 * @since 2.0
	 */
	public String getWS() {
		return ws;
	}

	/**
	 * Get optional system architecture specification as a comma-separated string.
	 * 
	 * @see org.eclipse.core.boot.BootLoader 
	 * @return the system architecture specification string, or <code>null</code>.
	 * @since 2.0
	 */
	public String getOSArch() {
		return arch;
	}

	/**
	 * Get optional locale specification as a comma-separated string.
	 * 
	 * @return the locale specification string, or <code>null</code>.
	 * @since 2.0
	 */
	public String getNL() {
		return nl;
	}

	/**
	 * Indicates whether the feature can be used as a primary feature.
	 * 
	 * @return <code>true</code> if this is a primary feature, 
	 * otherwise <code>false</code>
	 * @since 2.0
	 */
	public boolean isPrimary() {
		return isPrimary;
	}

	/**
	 * Returns an optional identifier for the feature application
	 * 
	 * @return application identifier, or <code>null</code>.
	 * @since 2.0
	 */
	public String getApplication() {
		return application;
	}

	/**
	 * Returns an optional identifier for the colocation affinity feature
	 * 
	 * @return feature identifier, or <code>null</code>.
	 * @since 2.0
	 */
	public String getAffinityFeature() {
		return affinity;
	}

	/**
	 * Returns and optional custom install handler entry.
	 * 
	 * @return install handler entry, or <code>null</code> if
	 * none was specified
	 * @since 2.0
	 */
	public InstallHandlerEntryModel getInstallHandlerModel() {
		return installHandler;
	}

	/**
	 * Returns the feature description.
	 * 
	 * @return feature rescription, or <code>null</code>.
	 * @since 2.0
	 */
	public URLEntryModel getDescriptionModel() {
		return description;
	}

	/**
	 * Returns the copyright information for the feature.
	 * 
	 * @return copyright information, or <code>null</code>.
	 * @since 2.0
	 */
	public URLEntryModel getCopyrightModel() {
		return copyright;
	}

	/**
	 * Returns the license information for the feature.
	 * 
	 * @return feature license, or <code>null</code>.
	 * @since 2.0
	 */
	public URLEntryModel getLicenseModel() {
		return license;
	}

	/**
	 * Returns an information entry referencing the location of the
	 * feature update site.
	 * 
	 * @return update site entry, or <code>null</code>.
	 * @since 2.0
	 */
	public URLEntryModel getUpdateSiteEntryModel() {
		return updateSiteInfo;
	}

	/**
	 * Return an array of information entries referencing locations of other
	 * update sites. 
	 * 
	 * @return an array of site entries, or an empty array.
	 * @since 2.0 
	 * @since 2.0
	 */
	public URLEntryModel[] getDiscoverySiteEntryModels() {
		if (discoverySiteInfo == null)
			return new URLEntryModel[0];

		return (URLEntryModel[]) discoverySiteInfo.toArray(
			arrayTypeFor(discoverySiteInfo));
	}

	/**
	 * Return a list of plug-in dependencies for this feature.
	 * 
	 * @return the list of required plug-in dependencies, or an empty array.
	 * @since 2.0
	 */
	public ImportModel[] getImportModels() {
		if (imports == null)
			return new ImportModel[0];

		return (ImportModel[]) imports.toArray(arrayTypeFor(imports));
	}

	/**
	 * Returns an array of plug-in entries referenced by this feature
	 * 
	 * @return an erray of plug-in entries, or an empty array.
	 * @since 2.0
	 */
	public PluginEntryModel[] getPluginEntryModels() {
		if (pluginEntries == null)
			return new PluginEntryModel[0];

		return (PluginEntryModel[]) pluginEntries.toArray(arrayTypeFor(pluginEntries));
	}

	/**
	 * Returns an array of versioned identifier referenced by this feature
	 * 
	 * @return an array of versioned identifier, or an empty array.
	 * @deprecated use getFeatureIncludeIdentifier instead.
	 * @since 2.0
	 */
	public VersionedIdentifier[] getFeatureIncludeVersionedIdentifier() {
		if (featureIncludes == null)
			return new VersionedIdentifier[0];
			
		//
		Iterator iter = featureIncludes.iterator();
		VersionedIdentifier[] versionIncluded = new VersionedIdentifier[featureIncludes.size()];
		int index = 0;
		while (iter.hasNext()) {
			IncludedFeatureReferenceModel model = (IncludedFeatureReferenceModel)iter.next();
			versionIncluded[index]=model.getVersionedIdentifier();
			index++;
		}
		return versionIncluded;
	}

	/**
	 * Returns an array of included feature reference model referenced by this feature.
	 *
	 * @return an array of included feature reference model, or an empty array.
	 * @since 2.0
	 */
	public IIncludedFeatureReference[] getFeatureIncluded() {
		if (featureIncludes == null)
			return new IIncludedFeatureReference[0];	
		return (IIncludedFeatureReference[]) featureIncludes.toArray(
			arrayTypeFor(featureIncludes));
	}

	/**
	 * Returns an array of non-plug-in entries referenced by this feature
	 * 
	 * @return an erray of non-plug-in entries, or an empty array.
	 * @since 2.0
	 */
	public NonPluginEntryModel[] getNonPluginEntryModels() {
		if (nonPluginEntries == null)
			return new NonPluginEntryModel[0];

		return (NonPluginEntryModel[]) nonPluginEntries.toArray(
			arrayTypeFor(nonPluginEntries));
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
	 * Sets the feature displayable label.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param label displayable label
	 * @since 2.0
	 */
	public void setLabel(String label) {
		assertIsWriteable();
		this.label = label;
		this.localizedLabel = null;
	}

	/**
	 * Sets the feature provider displayable label.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param provider provider displayable label
	 * @since 2.0
	 */
	public void setProvider(String provider) {
		assertIsWriteable();
		this.provider = provider;
		this.localizedProvider = null;
	}

	/**
	 * Sets the unresolved URL for the feature image.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param imageURLString unresolved URL string
	 * @since 2.0
	 */
	public void setImageURLString(String imageURLString) {
		assertIsWriteable();
		this.imageURLString = imageURLString;
		this.imageURL = null;
	}

	/**
	 * Sets the operating system specification.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @see org.eclipse.core.boot.BootLoader
	 * @param os operating system specification as a comma-separated list
	 * @since 2.0
	 */
	public void setOS(String os) {
		assertIsWriteable();
		this.os = os;
	}

	/**
	 * Sets the windowing system specification.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @see org.eclipse.core.boot.BootLoader
	 * @param ws windowing system specification as a comma-separated list
	 * @since 2.0
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
	 * @since 2.0
	 */
	public void setNL(String nl) {
		assertIsWriteable();
		this.nl = nl;
	}

	/**
	 * Sets the system architecture specification.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @see org.eclipse.core.boot.BootLoader
	 * @param arch system architecture specification as a comma-separated list
	 * @since 2.0
	 */
	public void setArch(String arch) {
		assertIsWriteable();
		this.arch = arch;
	}

	/**
	 * Indicates whether this feature can act as a primary feature.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param isPrimary <code>true</code> if this feature can act as primary,
	 * <code>false</code> otherwise
	 * 
	 * @since 2.0
	 */
	public void isPrimary(boolean isPrimary) {
		assertIsWriteable();
		this.isPrimary = isPrimary;
	}

	/**
	 * Sets the feature application identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param application feature application identifier
	 * @since 2.0
	 */
	public void setApplication(String application) {
		assertIsWriteable();
		this.application = application;
	}

	/**
	 * Sets the identifier of the Feature this feature should be
	 * installed with.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param affinity the identifier of the Feature
	 * @since 2.0
	 */
	public void setAffinityFeature(String affinity) {
		assertIsWriteable();
		this.affinity = affinity;
	}


	/**
	 * Sets the custom install handler for the feature.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param installHandler install handler entry
	 * @since 2.0
	 */
	public void setInstallHandlerModel(InstallHandlerEntryModel installHandler) {
		assertIsWriteable();
		this.installHandler = installHandler;
	}

	/**
	 * Sets the feature description information.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param description feature description information
	 * @since 2.0
	 */
	public void setDescriptionModel(URLEntryModel description) {
		assertIsWriteable();
		this.description = description;
	}

	/**
	 * Sets the feature copyright information.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param copyright feature copyright information
	 * @since 2.0
	 */
	public void setCopyrightModel(URLEntryModel copyright) {
		assertIsWriteable();
		this.copyright = copyright;
	}

	/**
	 * Sets the feature license information.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param license feature license information
	 * @since 2.0
	 */
	public void setLicenseModel(URLEntryModel license) {
		assertIsWriteable();
		this.license = license;
	}

	/**
	 * Sets the feature update site reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param updateSiteInfo feature update site reference
	 * @since 2.0
	 */
	public void setUpdateSiteEntryModel(URLEntryModel updateSiteInfo) {
		assertIsWriteable();
		this.updateSiteInfo = updateSiteInfo;
	}

	/**
	 * Sets additional update site references.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param discoverySiteInfo additional update site references
	 * @since 2.0
	 */
	public void setDiscoverySiteEntryModels(URLEntryModel[] discoverySiteInfo) {
		assertIsWriteable();
		if (discoverySiteInfo == null)
			this.discoverySiteInfo = null;
		else
			this.discoverySiteInfo = new ArrayList(Arrays.asList(discoverySiteInfo));
	}

	/**
	 * Sets the feature plug-in dependency information.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param imports feature plug-in dependency information
	 * @since 2.0
	 */
	public void setImportModels(ImportModel[] imports) {
		assertIsWriteable();
		if (imports == null)
			this.imports = null;
		else
			this.imports = new ArrayList(Arrays.asList(imports));
	}

	/**
	 * Sets the feature plug-in references.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param pluginEntries feature plug-in references
	 * @since 2.0
	 */
	public void setPluginEntryModels(PluginEntryModel[] pluginEntries) {
		assertIsWriteable();
		if (pluginEntries == null)
			this.pluginEntries = null;
		else
			this.pluginEntries = new ArrayList(Arrays.asList(pluginEntries));
	}

	/**
	 * Sets the feature non-plug-in data references.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param nonPluginEntries feature non-plug-in data references
	 * @since 2.0
	 */
	public void setNonPluginEntryModels(NonPluginEntryModel[] nonPluginEntries) {
		assertIsWriteable();
		if (nonPluginEntries == null)
			this.nonPluginEntries = null;
		else
			this.nonPluginEntries = new ArrayList(Arrays.asList(nonPluginEntries));
	}

	/**
	 * Adds an additional update site reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param discoverySiteInfo update site reference
	 * @since 2.0
	 */
	public void addDiscoverySiteEntryModel(URLEntryModel discoverySiteInfo) {
		assertIsWriteable();
		if (this.discoverySiteInfo == null)
			this.discoverySiteInfo = new ArrayList();
		if (!this.discoverySiteInfo.contains(discoverySiteInfo))
			this.discoverySiteInfo.add(discoverySiteInfo);
	}

	/**
	 * Adds a plug-in dependency entry.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param importEntry plug-in dependency entry
	 * @since 2.0
	 */
	public void addImportModel(ImportModel importEntry) {
		assertIsWriteable();
		if (this.imports == null)
			this.imports = new ArrayList();
		if (!this.imports.contains(importEntry))
			this.imports.add(importEntry);
	}

	/**
	 * Adds a plug-in reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param pluginEntry plug-in reference
	 * @since 2.0
	 */
	public void addPluginEntryModel(PluginEntryModel pluginEntry) {
		assertIsWriteable();
		if (this.pluginEntries == null)
			this.pluginEntries = new ArrayList();
		if (!this.pluginEntries.contains(pluginEntry))
			this.pluginEntries.add(pluginEntry);
	}
	
	/**
	 * Adds a feature identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param identifier feature identifer
	 * @param options the options associated with the nested feature
	 * @since 2.1
	 */
	public void addIncludedFeatureReferenceModel(IncludedFeatureReferenceModel include){
		assertIsWriteable();
		if (this.featureIncludes==null)
			this.featureIncludes = new ArrayList();
		if (!this.featureIncludes.contains(include))
			this.featureIncludes.add(include);
	}
		
	/**
	 * Adds a non-plug-in data reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param nonPluginEntry non-plug-in data reference
	 * @since 2.0
	 */
	public void addNonPluginEntryModel(NonPluginEntryModel nonPluginEntry) {
		assertIsWriteable();
		if (this.nonPluginEntries == null)
			this.nonPluginEntries = new ArrayList();
		if (!this.nonPluginEntries.contains(nonPluginEntry))
			this.nonPluginEntries.add(nonPluginEntry);
	}

	/**
	 * Removes an update site reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param discoverySiteInfo update site reference
	 * @since 2.0
	 */
	public void removeDiscoverySiteEntryModel(URLEntryModel discoverySiteInfo) {
		assertIsWriteable();
		if (this.discoverySiteInfo != null)
			this.discoverySiteInfo.remove(discoverySiteInfo);
	}

	/**
	 * Removes a plug-in dependency entry.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param importEntry plug-in dependency entry
	 * @since 2.0
	 */
	public void removeImportModel(ImportModel importEntry) {
		assertIsWriteable();
		if (this.imports != null)
			this.imports.remove(importEntry);
	}

	/**
	 * Removes a plug-in reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param pluginEntry plug-in reference
	 * @since 2.0
	 */
	public void removePluginEntryModel(PluginEntryModel pluginEntry) {
		assertIsWriteable();
		if (this.pluginEntries != null)
			this.pluginEntries.remove(pluginEntry);
	}

	/**
	 * Removes a non-plug-in data reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param nonPluginEntry non-plug-in data reference
	 * @since 2.0
	 */
	public void removeNonPluginEntryModel(NonPluginEntryModel nonPluginEntry) {
		assertIsWriteable();
		if (this.nonPluginEntries != null)
			this.nonPluginEntries.remove(nonPluginEntry);
	}

	/**
	 * Marks the model object as read-only.
	 * 
	 * @since 2.0
	 */
	public void markReadOnly() {
		super.markReadOnly();
		markReferenceReadOnly(getDescriptionModel());
		markReferenceReadOnly(getCopyrightModel());
		markReferenceReadOnly(getLicenseModel());
		markReferenceReadOnly(getUpdateSiteEntryModel());
		markListReferenceReadOnly(getDiscoverySiteEntryModels());
		markListReferenceReadOnly(getImportModels());
		markListReferenceReadOnly(getPluginEntryModels());
		markListReferenceReadOnly(getNonPluginEntryModels());
	}

	/**
	 * Resolve the model object.
	 * Any URL strings in the model are resolved relative to the 
	 * base URL argument. Any translatable strings in the model that are
	 * specified as translation keys are localized using the supplied 
	 * resource bundle.
	 * 
	 * @param base URL
	 * @param bundle resource bundle
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	public void resolve(URL base, ResourceBundle bundle)
		throws MalformedURLException {
		// resolve local elements
		localizedLabel = resolveNLString(bundle, label);
		localizedProvider = resolveNLString(bundle, provider);
		imageURL = resolveURL(base, bundle, imageURLString);

		// delegate to references		
		resolveReference(getDescriptionModel(), base, bundle);
		resolveReference(getCopyrightModel(), base, bundle);
		resolveReference(getLicenseModel(), base, bundle);
		resolveReference(getUpdateSiteEntryModel(), null, bundle);
		resolveListReference(getDiscoverySiteEntryModels(), null, bundle);
		resolveListReference(getImportModels(), base, bundle);
		resolveListReference(getPluginEntryModels(), base, bundle);
		resolveListReference(getNonPluginEntryModels(), base, bundle);
	}
}