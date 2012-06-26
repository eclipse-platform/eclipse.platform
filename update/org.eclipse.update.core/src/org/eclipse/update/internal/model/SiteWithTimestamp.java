/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.update.internal.model;

import java.util.Date;

import org.eclipse.update.core.Site;

/**
 * This is a wrapper class for Site class that adds timestamp
 * 
 *
 */
public class SiteWithTimestamp extends Site implements ITimestamp {

	private Date timestamp;
	
	/*private Site site;
	
	public SiteWithTimestamp( Site site) {
		this.site = site;
	}*/
	
	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
		
	}
/*
	public boolean isReadOnly() {
		return site.isReadOnly();
	}

	public Object getAdapter(Class adapter) {		
		return site.getAdapter(adapter);
	}

	public void addPluginEntry(IPluginEntry pluginEntry) {		
		site.addPluginEntry(pluginEntry);
	}

	public IFeature createFeature(String type, URL url) throws CoreException {	
		return site.createFeature(type, url);
	}

	public IFeature createFeature(String type, URL url, IProgressMonitor monitor) throws CoreException {		
		return site.createFeature(type, url, monitor);
	}

	public IArchiveReference[] getArchives() {		
		return site.getArchives();
	}

	public ICategory[] getCategories() {		
		return site.getCategories();
	}

	public ICategory getCategory(String key) {		
		return site.getCategory(key);
	}

	public IConfiguredSite getCurrentConfiguredSite() {	
		return site.getCurrentConfiguredSite();
	}

	public String getDefaultPackagedFeatureType() {	
		return site.getDefaultPackagedFeatureType();
	}

	public IURLEntry getDescription() {	
		return site.getDescription();
	}

	public long getDownloadSizeFor(IFeature feature) {	
		return site.getDownloadSizeFor(feature);
	}

	public ISiteFeatureReference getFeatureReference(IFeature feature) {	
		return site.getFeatureReference(feature);
	}

	public ISiteFeatureReference[] getFeatureReferences() {	
		return site.getFeatureReferences();
	}

	public long getInstallSizeFor(IFeature feature) {	
		return site.getInstallSizeFor(feature);
	}

	public IURLEntry[] getMirrorSiteEntries() {	
		return site.getMirrorSiteEntries();
	}

	public IPluginEntry[] getPluginEntries() {	
		return site.getPluginEntries();
	}

	public IPluginEntry[] getPluginEntriesOnlyReferencedBy(IFeature feature) throws CoreException {	
		return site.getPluginEntriesOnlyReferencedBy(feature);
	}

	public int getPluginEntryCount() {		
		return site.getPluginEntryCount();
	}

	public ISiteFeatureReference[] getRawFeatureReferences() {	
		return site.getRawFeatureReferences();
	}

	public ISiteContentProvider getSiteContentProvider() throws CoreException {	
		return site.getSiteContentProvider();
	}

	public URL getURL() {	
		return site.getURL();
	}

	public IFeatureReference install(IFeature sourceFeature, IFeatureReference[] optionalFeatures, IFeatureContentConsumer parentContentConsumer, IVerifier parentVerifier, IVerificationListener verificationListener, IProgressMonitor progress) throws CoreException {	
		return site.install(sourceFeature, optionalFeatures, parentContentConsumer,
				parentVerifier, verificationListener, progress);
	}

	public IFeatureReference install(IFeature sourceFeature, IFeatureReference[] optionalFeatures, IVerificationListener verificationListener, IProgressMonitor progress) throws InstallAbortedException, CoreException {		
		return site.install(sourceFeature, optionalFeatures, verificationListener,
				progress);
	}

	public IFeatureReference install(IFeature sourceFeature, IVerificationListener verificationListener, IProgressMonitor progress) throws InstallAbortedException, CoreException {		
		return site.install(sourceFeature, verificationListener, progress);
	}

	public void remove(IFeature feature, IProgressMonitor progress) throws CoreException {		
		site.remove(feature, progress);
	}

	public void setSiteContentProvider(ISiteContentProvider siteContentProvider) {		
		site.setSiteContentProvider(siteContentProvider);
	}

	public void addArchiveReferenceModel(ArchiveReferenceModel archiveReference) {		
		site.addArchiveReferenceModel(archiveReference);
	}

	public void addCategoryModel(CategoryModel category) {		
		site.addCategoryModel(category);
	}

	public void addFeatureReferenceModel(SiteFeatureReferenceModel featureReference) {		
		site.addFeatureReferenceModel(featureReference);
	}

	public void addMirrorModel(URLEntryModel mirror) {		
		site.addMirrorModel(mirror);
	}

	public ArchiveReferenceModel[] getArchiveReferenceModels() {		
		return site.getArchiveReferenceModels();
	}

	public CategoryModel[] getCategoryModels() {	
		return site.getCategoryModels();
	}

	public ConfiguredSiteModel getConfiguredSiteModel() {	
		return site.getConfiguredSiteModel();
	}

	public URLEntryModel getDescriptionModel() {	
		return site.getDescriptionModel();
	}

	public SiteFeatureReferenceModel[] getFeatureReferenceModels() {		
		return site.getFeatureReferenceModels();
	}

	public URL getLocationURL() {		
		return site.getLocationURL();
	}

	public String getLocationURLString() {		
		return site.getLocationURLString();
	}

	public URLEntryModel[] getMirrorSiteEntryModels() {	
		return site.getMirrorSiteEntryModels();
	}

	public String getType() {	
		return site.getType();
	}

	public void markReadOnly() {	
		site.markReadOnly();
	}

	public void removeArchiveReferenceModel(ArchiveReferenceModel archiveReference) {	
		site.removeArchiveReferenceModel(archiveReference);
	}

	public void removeCategoryModel(CategoryModel category) {	
		site.removeCategoryModel(category);
	}

	public void removeFeatureReferenceModel(FeatureReferenceModel featureReference) {	
		site.removeFeatureReferenceModel(featureReference);
	}

	public void removeMirror(URLEntryModel mirror) {		
		site.removeMirror(mirror);
	}

	public void resolve(URL base, URL bundleURL) throws MalformedURLException {		
		site.resolve(base, bundleURL);
	}

	public void setArchiveReferenceModels(ArchiveReferenceModel[] archiveReferences) {		
		site.setArchiveReferenceModels(archiveReferences);
	}

	public void setCategoryModels(CategoryModel[] categories) {		
		site.setCategoryModels(categories);
	}

	public void setConfiguredSiteModel(ConfiguredSiteModel configuredSiteModel) {		
		site.setConfiguredSiteModel(configuredSiteModel);
	}

	public void setDescriptionModel(URLEntryModel description) {		
		site.setDescriptionModel(description);
	}

	public void setFeatureReferenceModels(FeatureReferenceModel[] featureReferences) {		
		site.setFeatureReferenceModels(featureReferences);
	}

	public void setLocationURLString(String locationURLString) {		
		site.setLocationURLString(locationURLString);
	}

	public void setMirrorSiteEntryModels(URLEntryModel[] mirrors) {		
		site.setMirrorSiteEntryModels(mirrors);
	}

	public void setMirrorsURLString(String mirrorsURL) {		
		site.setMirrorsURLString(mirrorsURL);
	}

	public void setType(String type) {		
		site.setType(type);
	}
*/
}
