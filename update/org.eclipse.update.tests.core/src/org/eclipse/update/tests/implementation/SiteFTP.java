package org.eclipse.update.tests.implementation;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.core.*;

public class SiteFTP implements ISite {

	private URL url;
	public SiteFTP(URL url){
		this.url = url;
	}

	/*
	 * @see ISite#getFeatureReferences()
	 */
	public IFeatureReference[] getFeatureReferences() {
		return null;
	}

	/*
	 * @see ISite#install(IFeature, FeatureVerification verifier,IProgressMonitor)
	 */
	public IFeatureReference install(IFeature feature,IVerificationListener verificationListener, IProgressMonitor monitor) throws CoreException {
		return null;
	}

	/*
	 * @see ISite#remove(IFeature, IProgressMonitor)
	 */
	public void remove(IFeature feature, IProgressMonitor monitor) throws CoreException {
	}

	/*
	 * @see ISite#addSiteChangedListener(IConfiguredSiteChangedListener)
	 */
	public void addSiteChangedListener(IConfiguredSiteChangedListener listener) {
	}

	/*
	 * @see ISite#removeSiteChangedListener(IConfiguredSiteChangedListener)
	 */
	public void removeSiteChangedListener(IConfiguredSiteChangedListener listener) {
	}

	/*
	 * @see ISite#getURL()
	 */
	public URL getURL() {
		return url;
	}

	/*
	 * @see ISite#getType()
	 */
	public String getType() {
		return "org.eclipse.update.tests.ftp";
	}

	/*
	 * @see ISite#getCategories()
	 */
	public ICategory[] getCategories() {
		return null;
	}

	/*
	 * @see ISite#getArchives()
	 */
	public IArchiveReference[] getArchives() {
		return null;
	}
 
	/*
	 * @see ISite#addCategory(ICategory)
	 */
	public void addCategory(ICategory category) {
	}

	/*
	 * @see ISite#save()
	 */
	public void save() throws CoreException {
	}

	/*
	 * @see IPluginContainer#getPluginEntries()
	 */
	public IPluginEntry[] getPluginEntries() {
		return null;
	}

	/*
	 * @see IPluginContainer#getPluginEntryCount()
	 */
	public int getPluginEntryCount() {
		return 0;
	}

	/*
	 * @see ISite#getDownloadSizeFor(IFeature)
	 */
	public long getDownloadSizeFor(IFeature feature) {
		return 0;
	}

	/*
	 * @see ISite#getInstallSizeFor(IFeature)
	 */
	public long getInstallSizeFor(IFeature feature) {
		return 0;
	}


	/*
	 * @see IPluginContainer#store(IPluginEntry, String, InputStream)
	 */
	public void store(IPluginEntry entry, String name, InputStream inStream) throws CoreException {
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	/*
	 * @see IPluginContainer#remove(IPluginEntry)
	 */
	public void remove(IPluginEntry entry) throws CoreException {
	}

	/*
	 * @see ISite#setSiteContentConsumer(ISiteContentConsumer)
	 */
	public void setSiteContentConsumer(ISiteContentConsumer contentConsumer) {
	}

	/*
	 * @see ISite#setSiteContentProvider(ISiteContentProvider)
	 */
	public void setSiteContentProvider(ISiteContentProvider siteContentProvider) {
	}

	/*
	 * @see ISite#getSiteContentProvider()
	 */
	public ISiteContentProvider getSiteContentProvider() {
		return null;
	}

	/*
	 * @see ISite#getDefaultPackagedFeatureType()
	 */
	public String getDefaultPackagedFeatureType() {
		return null;
	}

	/*
	 * @see ISite#store(IFeature, String, InputStream, IProgressMonitor)
	 */
	public void store(IFeature feature, String name, InputStream inStream, IProgressMonitor monitor) throws CoreException {
	}


	/*
	 * @see IPluginContainer#remove(IPluginEntry, IProgressMonitor)
	 */
	public void remove(IPluginEntry entry, IProgressMonitor monitor) throws CoreException {
	}

	/*
	 * @see ISite#getCategory(String)
	 */
	public ICategory getCategory(String key) {
		return null;
	}

	/*
	 * @see ISite#createSiteContentConsumer(IFeature)
	 */
	public ISiteContentConsumer createSiteContentConsumer(IFeature feature) throws CoreException {
		return null;
	}

	/*
	 * @see ISite#getFeatureReference(IFeature)
	 */
	public IFeatureReference getFeatureReference(IFeature feature) {
		return null;
	}

	/*
	 * @see ISite#getDescription()
	 */
	public IURLEntry getDescription() {
		return null;
	}

	/*
	 * @see ISite#getPluginEntriesOnlyReferencedBy(IFeature)
	 */
	public IPluginEntry[] getPluginEntriesOnlyReferencedBy(IFeature feature) throws CoreException {
		return null;
	}

	/**
	 * @see ISite#addPluginEntry(IPluginEntry)
	 */
	public void addPluginEntry(IPluginEntry pluginEntry) {
	}

	/**
	 * @see org.eclipse.update.core.ISite#install(IFeature, IFeatureReference[], IVerificationListener, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature feature, IFeatureReference[] optionalfeatures, IVerificationListener verificationListener, IProgressMonitor monitor) throws InstallAbortedException, CoreException {
		return null;
	}

	/**
	 * @see org.eclipse.update.core.ISite#getConfiguredSite()
	 */
	public IConfiguredSite getCurrentConfiguredSite() {
		return null;
	}

	/**
	 * @see org.eclipse.update.core.ISite#createFeature(String, URL)
	 */
	public IFeature createFeature(String type, URL url) throws CoreException {
		return null;
	}

}
