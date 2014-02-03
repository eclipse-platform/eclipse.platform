/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.mirror;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.update.core.ContentReference;
import org.eclipse.update.core.ICategory;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureContentProvider;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.INonPluginEntry;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteFeatureReference;
import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.core.IVerificationListener;
import org.eclipse.update.core.Site;
import org.eclipse.update.core.SiteFeatureReferenceModel;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.core.model.CategoryModel;
import org.eclipse.update.core.model.SiteModelFactory;
import org.eclipse.update.core.model.URLEntryModel;
import org.eclipse.update.internal.core.CoreExceptionWithRootCause;
import org.eclipse.update.internal.core.FatalIOException;
import org.eclipse.update.internal.core.FeaturePackagedContentProvider;
import org.eclipse.update.internal.core.ISiteContentConsumer;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.standalone.StandaloneUpdateApplication;

/**
 * Local mirror site.  Read/Write
 */
public class MirrorSite extends Site {
	private final static String INDENT = "   "; //$NON-NLS-1$
	private SiteModelFactory factory;
	/**
	 * plugin entries 
	 */
	private Collection downloadedPluginEntries = new ArrayList();
	private Collection downloadedFeatureReferenceModels = new ArrayList();
	private boolean ignoreNonPresentPlugins;
	public MirrorSite(SiteModelFactory factory) {
		this.factory = factory;
	}

	/**
	 * Mirrors the specified features and listed optional features on this site.
	 * @see ISite#install(IFeature, IVerificationListener, IProgressMonitor)
	 * @param mirrorSiteUrl external URL of the mirror site or null;
	 * if parameter is provided policy fragment will be generated
	 * @exception CoreException
	 */
	public void mirrorAndExpose(
		ISite remoteSite,
		ISiteFeatureReference[] sourceFeatureRefs,
		IFeatureReference[] optionalfeatures,
		String mirrorSiteUrl)
		throws CoreException {

		mirrorAndExposeFeatures(
			remoteSite,
			sourceFeatureRefs,
			optionalfeatures);

		System.out.println(
			"Installing features finished. Updating categories ..."); //$NON-NLS-1$
		updateCategories(remoteSite);
		System.out.println(
			"Updating categories finished. Updating site description ..."); //$NON-NLS-1$
		updateDescription(remoteSite);
		System.out.println(
			"Updating site description finished. Saving site.xml ..."); //$NON-NLS-1$
		save();
		if (mirrorSiteUrl != null) {
			generateUpdatePolicy(mirrorSiteUrl);
		}
	}
	private void mirrorAndExposeFeatures(
		ISite remoteSite,
		ISiteFeatureReference[] sourceFeatureRefs,
		IFeatureReference[] optionalfeatures)
		throws CoreException {

		// Features that failed will be retried once again
		Collection failedFeatures = new ArrayList();
		for (int i = 0; i < sourceFeatureRefs.length; i++) {
			try {
				IFeature sourceFeature =
					sourceFeatureRefs[i].getFeature(new NullProgressMonitor());
				SiteFeatureReferenceModel featureRef =
					mirrorFeature(
						remoteSite,
						sourceFeature,
						optionalfeatures,
						1);
				// Set categories of the new feature
				ICategory remoteCategories[] =
					sourceFeatureRefs[i].getCategories();
				for (int j = 0; j < remoteCategories.length; j++) {
					featureRef.addCategoryName(remoteCategories[j].getName());
				}

				addFeatureReferenceModel(remoteSite, featureRef);
			} catch (CoreException ce) {
				failedFeatures.add(sourceFeatureRefs[i]);
			}
		}

		// do we need to retry?
		if (failedFeatures.size() > 0) {
			sourceFeatureRefs =
				(ISiteFeatureReference[]) failedFeatures.toArray(
					new ISiteFeatureReference[failedFeatures.size()]);
		} else {
			return;
		}

		for (int i = 0; i < sourceFeatureRefs.length; i++) {
			IFeature sourceFeature =
				sourceFeatureRefs[i].getFeature(new NullProgressMonitor());
			SiteFeatureReferenceModel featureRef =
				mirrorFeature(remoteSite, sourceFeature, optionalfeatures, 1);
			// Set categories of the new feature
			ICategory remoteCategories[] = sourceFeatureRefs[i].getCategories();
			for (int j = 0; j < remoteCategories.length; j++) {
				featureRef.addCategoryName(remoteCategories[j].getName());
			}

			addFeatureReferenceModel(remoteSite, featureRef);
		}
	}

	/**
	 * Install the specified feature and listed optional features on this site.
	 * @see ISite#install(IFeature, IVerificationListener, IProgressMonitor)
	 * @exception CoreException
	 */
	private SiteFeatureReferenceModel mirrorFeature(
		ISite remoteSite,
		IFeature sourceFeature,
		IFeatureReference[] optionalfeatures,
		int indent)
		throws CoreException {
		String tab = ""; //$NON-NLS-1$
		for (int i = 0; i < indent; i++)
			tab += " "; //$NON-NLS-1$
		System.out.println(
			tab
				+ "Mirroring feature " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		SiteFeatureReferenceModel existingFeatures[] =
			getDownloadedFeatureReferenceModels();
		for (int e = 0; e < existingFeatures.length; e++) {
			if (existingFeatures[e]
				.getVersionedIdentifier()
				.equals(sourceFeature.getVersionedIdentifier())) {
				System.out.println(
					tab
						+ "Feature " //$NON-NLS-1$
						+ sourceFeature.getVersionedIdentifier()
						+ " already exists.  Skipping downloading."); //$NON-NLS-1$
				return existingFeatures[e];
			}
		}

		final IFeatureContentProvider provider =
			sourceFeature.getFeatureContentProvider();
		
		// TODO: passing command options could be made more general in future, so this 
		// cast is not needed. 
		if (provider instanceof FeaturePackagedContentProvider) {
			((FeaturePackagedContentProvider) provider).setContinueOnError(ignoreNonPresentPlugins);
		}
		
		System.out.println(
			tab
				+ "Getting plugin entries for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		final IPluginEntry[] sourceFeaturePluginEntries =
			sourceFeature.getRawPluginEntries();

		// determine list of plugins to install
		// find the intersection between the plugin entries already contained
		// on the target site, and plugin entries packaged in source feature

		IPluginEntry[] pluginsToInstall =
			UpdateManagerUtils.diff(
				sourceFeaturePluginEntries,
				getDownloadedPluginEntries());

		System.out.println(
			tab
				+ "Getting non plugin entries for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		final INonPluginEntry[] nonPluginsToInstall =
			sourceFeature.getRawNonPluginEntries();

		System.out.println(
			tab
				+ "Getting included features for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		IFeatureReference[] children =
			sourceFeature.getRawIncludedFeatureReferences();
		if (optionalfeatures != null) {
			children =
				UpdateManagerUtils.optionalChildrenToInstall(
					children,
					optionalfeatures);
		}

		System.out.println(
			tab
				+ "Downloading feature archives for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		// download feature archives
		provider.getFeatureEntryArchiveReferences(null);

		System.out.println(
			tab
				+ "Downloading plug-in archives for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		// download plugin archives
		for (int i = 0; i < pluginsToInstall.length; i++) {
			try {
				provider.getPluginEntryArchiveReferences(pluginsToInstall[i], null);
			} catch (CoreException ce) {
				if ( ignoreNonPresentPlugins && 
						(ce instanceof CoreExceptionWithRootCause) &&
						(((CoreExceptionWithRootCause)ce).getRootException() != null) && 
						(((CoreExceptionWithRootCause)ce).getRootException() instanceof FatalIOException) ) {
					System.out.println("Could not mirror plug-in " + pluginsToInstall[i].getVersionedIdentifier().toString() + ". It does not exist on the given site");  //$NON-NLS-1$//$NON-NLS-2$
				} else {
					throw ce;
				}
			}
		}

		System.out.println(
			tab
				+ "Downloading non plug-in archives for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		// download non-plugin archives
		for (int i = 0; i < nonPluginsToInstall.length; i++) {
			provider.getNonPluginEntryArchiveReferences(
				nonPluginsToInstall[i],
				null);
		}

		System.out.println(
			tab
				+ "Installing child features for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		// install child features first
		for (int i = 0; i < children.length; i++) {
			IFeature childFeature = children[i].getFeature(null);
			mirrorFeature(
				remoteSite,
				childFeature,
				optionalfeatures,
				indent + 1);
		}

		System.out.println(
			tab
				+ "Storing plug-in archives for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		// store plugins' archives
		for (int i = 0; i < pluginsToInstall.length; i++) {
			try {
				ContentReference[] references = provider.getPluginEntryArchiveReferences( pluginsToInstall[i], null);
				storePluginArchive(references[0]);
				addDownloadedPluginEntry(pluginsToInstall[i]);
			} catch (CoreException ce) {
				if ( ignoreNonPresentPlugins && 
						(ce instanceof CoreExceptionWithRootCause) &&
						(((CoreExceptionWithRootCause)ce).getRootException() != null) && 
						(((CoreExceptionWithRootCause)ce).getRootException() instanceof FatalIOException) ) {
					System.out.println("Could not write plug-in " + pluginsToInstall[i].getVersionedIdentifier().toString() + ". It does not exist on the given site"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					//System.out.println("ignoreNonPresentPlugins:"+ignoreNonPresentPlugins); //$NON-NLS-1$
					throw ce;
				}
			}
		}

		System.out.println(
			tab
				+ "Storing non plug-in archives for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		// store non plugins' archives
		for (int i = 0; i < nonPluginsToInstall.length; i++) {
			ContentReference[] references =
				provider.getNonPluginEntryArchiveReferences(
					nonPluginsToInstall[i],
					null);
			for (int r = 0; r < references.length; r++) {
				storeNonPluginArchive(
					sourceFeature.getVersionedIdentifier(),
					references[r]);
			}
		}

		System.out.println(
			tab
				+ "Storing feature archives for " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " ..."); //$NON-NLS-1$
		// store feature archive
		ContentReference[] references =
			provider.getFeatureEntryArchiveReferences(null);
		storeFeatureArchive(references[0]);

		System.out.println(
			tab
				+ "Adding feature " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " to model ..."); //$NON-NLS-1$

		// add feature model to site model
		SiteFeatureReferenceModel featureRef =
			factory.createFeatureReferenceModel();
		featureRef.setSiteModel(this);
		//featureRef.setURLString(featureURL.toExternalForm());
		featureRef.setType(ISite.DEFAULT_PACKAGED_FEATURE_TYPE);
		featureRef.setFeatureIdentifier(
			sourceFeature.getVersionedIdentifier().getIdentifier());
		featureRef.setFeatureVersion(
			sourceFeature.getVersionedIdentifier().getVersion().toString());
		addDownloadedFeatureReferenceModel(featureRef);

		System.out.println(
			tab
				+ "Mirroring feature " //$NON-NLS-1$
				+ sourceFeature.getVersionedIdentifier()
				+ " finished."); //$NON-NLS-1$
		return featureRef;

	}
	/**
	 * Adds a feature reference model to this site,
	 * and exposes in site.xml if remote site exposes given feature.
	 */
	public void addFeatureReferenceModel(
		ISite remoteSite,
		SiteFeatureReferenceModel featureReference) {
		// check if remote site exposes this feature
		ISiteFeatureReference remoteFeatures[] =
			remoteSite.getRawFeatureReferences();
		for (int i = 0; i < remoteFeatures.length; i++) {
			ISiteFeatureReference remoteFeatureRef = remoteFeatures[i];
			try {
				if (remoteFeatureRef
					.getVersionedIdentifier()
					.equals(featureReference.getVersionedIdentifier())) {
					addFeatureReferenceModel(featureReference);
				}
			} catch (CoreException ce) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(ce);
			}
		}
		save();
		System.out.println(
			"Feature " //$NON-NLS-1$
				+ featureReference.getVersionedIdentifier()
				+ " added to site.xml."); //$NON-NLS-1$
	}
	/**
	 * Adds feature model to site model, removing old feature
	 */
	public void addFeatureReferenceModel(SiteFeatureReferenceModel featureReference) {
		SiteFeatureReferenceModel[] existingModels =
			getFeatureReferenceModels();
		for (int j = 0; j < existingModels.length; j++) {
			if (existingModels[j]
				.getVersionedIdentifier()
				.equals(featureReference.getVersionedIdentifier())) {
				super.removeFeatureReferenceModel(existingModels[j]);
			}
		}
		super.addFeatureReferenceModel(featureReference);
	}

	/**
	 * @see ISiteContentConsumer#store(ContentReference, IProgressMonitor)
	 */
	private void storeFeatureArchive(ContentReference contentReference)
		throws CoreException {
		InputStream inStream = null;
		String featurePath = null;

		try {
			URL newURL =
				new URL(
					this.getURL(),
					Site.DEFAULT_INSTALLED_FEATURE_PATH
						+ contentReference.getIdentifier()
						+ ".jar"); //$NON-NLS-1$
			featurePath = newURL.getFile();
			inStream = contentReference.getInputStream();
			UpdateManagerUtils.copyToLocal(inStream, featurePath, null);
		} catch (IOException e) {
			throw Utilities.newCoreException(
				"Error occurred while creating "+ featurePath+" file.", //$NON-NLS-1$ //$NON-NLS-2$
				e);
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
				}
			}
		}

	}
	/**
	* @see ISiteContentConsumer#store(ContentReference, IProgressMonitor)
	*/
	private void storePluginArchive(ContentReference contentReference)
		throws CoreException {

		InputStream inStream = null;
		String pluginPath = null;
		try {
			URL newURL = new URL(getURL(), contentReference.getIdentifier());
			pluginPath = newURL.getFile();
			inStream = contentReference.getInputStream();
			// added null check here,  since contentReference can, in theory, return null for input stream. 
			if (inStream != null) {
				UpdateManagerUtils.copyToLocal(inStream, pluginPath, null);
			}
		} catch (IOException e) {
			throw Utilities.newCoreException(
			"Error occurred while creating "+ pluginPath+" file.", //$NON-NLS-1$ //$NON-NLS-2$
				e);
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void storeNonPluginArchive(
		VersionedIdentifier featureVersionedIdentifier,
		ContentReference contentReference)
		throws CoreException {

		InputStream inStream = null;
		File nonPluginArchivePath = null;
		try {
			URL newDirURL =
				new URL(
					getURL(),
					Site.DEFAULT_INSTALLED_FEATURE_PATH
						+ "/" //$NON-NLS-1$
						+ featureVersionedIdentifier);
			File dir = new File(newDirURL.getFile());
			dir.mkdirs();
			inStream = contentReference.getInputStream();
			nonPluginArchivePath =
				new File(dir, contentReference.getIdentifier());
			UpdateManagerUtils.copyToLocal(
				inStream,
				nonPluginArchivePath.getAbsolutePath(),
				null);
		} catch (IOException e) {
			throw Utilities.newCoreException(
			"Error occurred while creating "+ nonPluginArchivePath.getAbsolutePath()+" file." //$NON-NLS-1$ //$NON-NLS-2$
				,e);
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void save() {
		FileOutputStream fos = null;
		try {
			URL siteURL = new URL(this.getURL(), "site.xml"); //$NON-NLS-1$
			fos = new FileOutputStream(new File(siteURL.getFile()));
			OutputStreamWriter outWriter = new OutputStreamWriter(fos, "UTF-8"); //$NON-NLS-1$
			PrintWriter writer = new PrintWriter(outWriter);
			save(writer);
			writer.flush();
		} catch (IOException ioe) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(
				Utilities.newCoreException(
					"Site XML could not be saved.", //$NON-NLS-1$
					ioe));
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ioe2) {
				}
			}
		}
	}
	private void save(PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		//writer.println("<!DOCTYPE site SYSTEM \"dtd/site.dtd\">");
		writeSite("", writer); //$NON-NLS-1$
	}

	private void writeSite(String indent, PrintWriter writer) {
		writer.print(indent + "<site"); //$NON-NLS-1$
		String indent2 = indent + INDENT;
		// default type
		//writeIfDefined(indenta, writer, "type", getType());
		// stored relative to site.xml
		//writeIfDefined(indenta, writer, "url", getURL());
		writer.println(">"); //$NON-NLS-1$
		URLEntryModel description = getDescriptionModel();
		if (description != null) {
			writer.println();
			writeDescription(indent2, writer, description);
			writer.println();
		}
		writeFeatures(indent2, writer);
		writeCategories(indent2, writer);
		writer.println(indent + "</site>"); //$NON-NLS-1$
	}
	private void writeFeatures(String indent, PrintWriter writer) {
		SiteFeatureReferenceModel[] featureReferenceModels =
			getFeatureReferenceModels();
		for (int i = 0; i < featureReferenceModels.length; i++) {
			writer.print(indent);
			writer.print("<feature"); //$NON-NLS-1$
			writer.print(
				" url=\"features/" //$NON-NLS-1$
					+ featureReferenceModels[i].getFeatureIdentifier()
					+ "_" //$NON-NLS-1$
					+ featureReferenceModels[i].getFeatureVersion()
					+ ".jar\""); //$NON-NLS-1$
			writer.print(
				" id=\"" //$NON-NLS-1$
					+ featureReferenceModels[i].getFeatureIdentifier()
					+ "\""); //$NON-NLS-1$
			writer.print(
				" version=\"" //$NON-NLS-1$
					+ featureReferenceModels[i].getFeatureVersion()
					+ "\""); //$NON-NLS-1$
			writer.println(">"); //$NON-NLS-1$

			String[] categoryNames =
				featureReferenceModels[i].getCategoryNames();
			for (int cn = 0; cn < categoryNames.length; cn++) {
				writer.print(indent + INDENT);
				writer.println(
					"<category name=\"" + categoryNames[cn] + "\" />"); //$NON-NLS-1$ //$NON-NLS-2$

			}

			writer.print(indent);
			writer.println("</feature>"); //$NON-NLS-1$
			writer.println();
		}
	}
	private void writeCategories(String indent, PrintWriter writer) {
		CategoryModel[] categoryModels = getCategoryModels();
		if (categoryModels.length <= 0) {
			return;
		}
		for (int i = 0; i < categoryModels.length; i++) {
			writer.print(indent);
			writer.print("<category-def"); //$NON-NLS-1$
			writer.print(
				" name=\"" //$NON-NLS-1$
					+ categoryModels[i].getName()
					+ "\" label=\"" //$NON-NLS-1$
					+ categoryModels[i].getLabel()
					+ "\""); //$NON-NLS-1$
			writer.println(">"); //$NON-NLS-1$
			if (categoryModels[i].getDescriptionModel() != null) {
				writeDescription(
						indent + INDENT,
						writer,
						categoryModels[i].getDescriptionModel());
			}
			writer.print(indent);
			writer.println("</category-def>"); //$NON-NLS-1$
			writer.println();
		}
	}
	private void writeDescription(
		String indent,
		PrintWriter writer,
		URLEntryModel urlEntryModel) {
		String url = urlEntryModel.getURLString();
		String text = urlEntryModel.getAnnotationNonLocalized();
		writer.print(indent);
		writer.print("<description"); //$NON-NLS-1$
		if (url != null)
			writer.print(" url=\"" + url + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (text == null || text.length() <= 0) {
			writer.println(" />"); //$NON-NLS-1$
		} else {
			writer.println(">"); //$NON-NLS-1$
			if (text != null) {
				writer.println(
					indent + INDENT + UpdateManagerUtils.xmlSafe(text));
			}
			writer.println(indent + "</description>"); //$NON-NLS-1$
		}
	}
	/**
	 * Adds a plugin entry 
	 * Either from parsing the file system or 
	 * installing a feature
	 * 
	 * We cannot figure out the list of plugins by reading the Site.xml as
	 * the archives tag are optionals
	 */
	public void addDownloadedPluginEntry(IPluginEntry pluginEntry) {
		downloadedPluginEntries.add(pluginEntry);
	}

	private IPluginEntry[] getDownloadedPluginEntries() {
		return (IPluginEntry[]) downloadedPluginEntries.toArray(
			new IPluginEntry[downloadedPluginEntries.size()]);
	}
	/**
	 * Adds a plugin entry 
	 * Either from parsing the file system or 
	 * installing a feature
	 * 
	 * We cannot figure out the list of plugins by reading the Site.xml as
	 * the archives tag are optionals
	 */
	public void addDownloadedFeatureReferenceModel(SiteFeatureReferenceModel featureModel) {
		downloadedFeatureReferenceModels.add(featureModel);
	}

	private SiteFeatureReferenceModel[] getDownloadedFeatureReferenceModels() {
		return (
			SiteFeatureReferenceModel[]) downloadedFeatureReferenceModels
				.toArray(
			new SiteFeatureReferenceModel[downloadedFeatureReferenceModels
				.size()]);
	}
	/**
	 * Checks if mirror site contains a feature with given ID and version
	 * @param featureRefModel
	 * @return true if such feature exists
	 */
	/*private boolean contains(SiteFeatureReferenceModel featureRefModel) {
		ISiteFeatureReference featureRefs[] = getRawFeatureReferences();
		for (int i = 0; i < featureRefs.length; i++) {
			try {
				if (featureRefs[i]
					.getVersionedIdentifier()
					.equals(featureRefModel.getVersionedIdentifier())) {
					return true;
				}
			} catch (CoreException ce) {
				ce.printStackTrace();
			}
		}
		return false;
	}*/

	/**
	 * Updates description of this site
	 * from description of the remote site.
	 */
	private void updateDescription(ISite remoteSite) {
		IURLEntry urlEntry = remoteSite.getDescription();
		if (urlEntry != null) {
			URLEntryModel newUrlEntryModel = new URLEntryModel();
			URL url = urlEntry.getURL();
			newUrlEntryModel.setAnnotation(urlEntry.getAnnotation());
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=136249
			// URL is not required, so might be null
			// The null case is (already) handled correctly in
			// writeDescription
			if (url != null) {
				newUrlEntryModel.setURLString(url.toExternalForm());
			}
			this.setDescriptionModel(newUrlEntryModel);
		}
	}
	/**
	 * Updates all categories used by features on this site
	 * from categories defined on remote site.
	 * Categories not defined on remote site are unchanged.
	 */
	private void updateCategories(ISite remoteSite) {
		// collect name of categories used on this site
		Set usedCategoryNames = new HashSet();
		SiteFeatureReferenceModel featureRefModels[] =
			getFeatureReferenceModels();
		for (int f = 0; f < featureRefModels.length; f++) {
			String[] featureCategoryNames =
				featureRefModels[f].getCategoryNames();

			for (int c = 0; c < featureCategoryNames.length; c++) {
				usedCategoryNames.add(featureCategoryNames[c]);
			}
		}

		Collection newCategoryModels = new ArrayList();
		for (Iterator it = usedCategoryNames.iterator(); it.hasNext();) {
			String name = (String) it.next();
			ICategory remoteCategory = remoteSite.getCategory(name);
			if (remoteCategory == null) {
				// remote site does not define this category
				CategoryModel oldCategory = null;
				try {
					oldCategory = (CategoryModel) getCategory(name);
				} catch (NullPointerException npe) {
					// cannot reproduce npe anymore
				}
				if (oldCategory != null) {
					newCategoryModels.add(oldCategory);
				}
			} else {
				newCategoryModels.add(remoteCategory);
			}

		}
		setCategoryModels(
			(CategoryModel[]) newCategoryModels.toArray(
				new CategoryModel[newCategoryModels.size()]));

	}
	private void generateUpdatePolicy(String url) {
		FileOutputStream fos = null;
		try {
			URL siteURL = new URL(this.getURL(), "policy.xml"); //$NON-NLS-1$
			fos = new FileOutputStream(new File(siteURL.getFile()));
			OutputStreamWriter outWriter = new OutputStreamWriter(fos, "UTF-8"); //$NON-NLS-1$
			PrintWriter writer = new PrintWriter(outWriter);

			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			writer.println("<update-policy>"); //$NON-NLS-1$

			writer.println(
				"<!-- You can paste the following fragment, containing url-map elements, into another policy file. -->"); //$NON-NLS-1$
			writeUrlMaps(writer, url);
			writer.println("<!-- End of fragment with url-map elements. -->"); //$NON-NLS-1$

			writer.println("</update-policy>"); //$NON-NLS-1$

			writer.flush();
		} catch (IOException ioe) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(
				Utilities.newCoreException(
					"policy.xml could not be saved", //$NON-NLS-1$
					ioe));
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ioe2) {
				}
			}
		}
	}
	private void writeUrlMaps(PrintWriter writer, String url) {
		SiteFeatureReferenceModel[] featureReferenceModels =
			getFeatureReferenceModels();
		for (int i = 0; i < featureReferenceModels.length; i++) {
			writer.print("\t"); //$NON-NLS-1$
			writer.print("<url-map"); //$NON-NLS-1$
			writer.print(
				" pattern=\"" //$NON-NLS-1$
					+ featureReferenceModels[i].getFeatureIdentifier()
					+ "\""); //$NON-NLS-1$
			writer.print(" url=\"" + url + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println(" />"); //$NON-NLS-1$
		}
	}

	public void setIgnoreNonPresentPlugins(boolean ignoreNonPresentPlugins) {
		this.ignoreNonPresentPlugins = ignoreNonPresentPlugins;
		
	}
}
