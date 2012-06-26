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
package org.eclipse.update.core;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;

/**
 * Feature content provider.
 * A feature content provider is an abstraction of each feature internal 
 * packaging structure. It allows the feature content to be accessed in
 * a standard way regardless of the internal packaging. All concrete feature
 * implementations need to implement a feature content provider.
 * <p>
 * There are two ways of looking at a feature content:
 * <ol>
 * <li>the "logical" view, which is a representation of the actual files that
 * make up the feature. These include any files that describe the feature
 * itself, files that are the actual implementation of referenced plug-ins,
 * and files that are the non-plug-in data files associated with the feature
 * <li>the "packaged" view, which is a set of related archive files that
 * contain the "logical" files.
 * </ol>
 * It is the responsibility of a feature content provider to manage the
 * mapping between the "packaged" and "logical" views.
 * </p>
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.FeatureContentProvider
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IFeatureContentProvider {

	/**
	 * Returns the feature url. 
	 * The exact interpretation of this URL is specific to each content
	 * provider. Typically, the URL is a reference to a file that can be 
	 * used directly, or indirectly, to determine the content of the feature.
	 * 
	 * @return feature url
	 * @since 2.0
	 */
	public URL getURL();

	/**
	 * Returns a content reference to the feature manifest. The feature manifest
	 * is an xml file, whose format is specified by the platform. Typically
	 * a feature will contain the manifest as one of the packaged files.
	 * For features that do not contain the manifest, or contain a manifest
	 * that does not follow the specified format, this method returns
	 * a reference to a computed manifest in the appropriate platform
	 * format.
	 * 
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return feature manifest reference, or <code>null</code> if the manifest cannot be found.
	 * @since 2.0
	 */
	public ContentReference getFeatureManifestReference(InstallMonitor monitor)
		throws CoreException;

	/**
	 * Returns an array of content references of all the "packaged"
	 * archives that make up this feature. 
	 * <p>
	 * The number of returned references is dependent on each feature 
	 * content provider (i.e is dependent on the packaging mechanism used
	 * by the particular feature type).
	 * </p>
	 * 
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return an array of references, or an empty array if no references
	 * are found
	 * @exception CoreException
	 * @since 2.0 
	 */
	public ContentReference[] getArchiveReferences(InstallMonitor monitor)
		throws CoreException;

	/**
	 * Returns an array of content references of the "packaged"
	 * archives that contain the feature descriptive information.
	 * <p>
	 * In general, the feature descriptive information should be packaged
	 * separately from the "bulk" of the actual feature content.
	 * The feature entry archive(s) must be downloaded from an update
	 * site in order to present information about the feature to the
	 * client. Consequently, keeping the number and size of the feature
	 * entry archive(s) to a minimum will speed up the responsiveness of the
	 * user interface.
	 * </p>
	 * <p>
	 * The number of returned references is dependent on each feature
	 * content provider (i.e is dependent on the packaging mechanism used
	 * by the particular feature type).
	 * </p>
	 * 
	 * @see IFeatureContentProvider#getFeatureEntryContentReferences(InstallMonitor)
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return an array of references, or an empty array if no references
	 * are found
	 * @exception CoreException
	 * @since 2.0 
	 */
	public ContentReference[] getFeatureEntryArchiveReferences(InstallMonitor monitor)
		throws CoreException;

	/**
	 * Returns an array of content references of the "packaged"
	 * archives that contain the files for the specified plug-in entry.
	 * <p>
	 * The number of returned references is dependent on each feature
	 * content provider (i.e is dependent on the packaging mechanism used
	 * by the particular feature type).
	 * </p>
	 * 
	 * @see IFeatureContentProvider#getPluginEntryContentReferences(IPluginEntry, InstallMonitor)
	 * @param pluginEntry plug-in entry
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return an array of references, or an empty array if no references
	 * are found
	 * @exception CoreException
	 * @since 2.0 
	 */
	public ContentReference[] getPluginEntryArchiveReferences(
		IPluginEntry pluginEntry,
		InstallMonitor monitor)
		throws CoreException;

	/**
	 * Returns an array of content references of the "packaged"
	 * archives that contain the files for the specified non-plug-in entry.
	 * <p>
	 * The number of returned references is dependent on each feature
	 * content provider (i.e is dependent on the packaging mechanism used
	 * by the particular feature type).
	 * </p>
	 * <p>
	 * Note, that the platform does not interpret non-plug-in entries in any 
	 * way, other that performing any required downloads. Non-plug-in entries
	 * are handled by custom install handlers that must be specified for
	 * the feature. Consequently, this interface does not make a distinction
	 * between the "logical" and "packaged" views for non-plug-in entries.
	 * The "packaged" view (returning references to the non-plug-in archives)
	 * is the only one supported. It is the responsibility of the custom install
	 * handler to understand the "logical" view of non-plug-in archives.
	 * </p>
	 * 
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return an array of references, or an empty array if no references
	 * are found
	 * @exception CoreException
	 * @since 2.0 
	 */
	public ContentReference[] getNonPluginEntryArchiveReferences(
		INonPluginEntry nonPluginEntry,
		InstallMonitor monitor)
		throws CoreException;

	/**
	 * Returns an array of content references to the feature definition files
	 * (i.e the "logical" view of the files defining the feature). These
	 * are the files required to present information about the feature to the
	 * client, and in general, should not contain references to plug-in and 
	 * non-plug-in files.
	 * 
	 * @see IFeatureContentProvider#getFeatureEntryArchiveReferences(InstallMonitor)
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return an array of ContentReference or an empty array if no references are found
	 * @exception CoreException when an error occurs
	 * @since 2.0 
	 */
	public ContentReference[] getFeatureEntryContentReferences(InstallMonitor monitor)
		throws CoreException;

	/**
	 * Returns an array of content references to the files implementing
	 * the specified plug-in. (i.e the "logical" view of the plug-in).
	 * 
	 * @see IFeatureContentProvider#getPluginEntryArchiveReferences(IPluginEntry, InstallMonitor)
	 * @param monitor progress monitor, can be <code>null</code>
	 * @return an array of ContentReference or an empty array if no references are found
	 * @exception CoreException
	 * @since 2.0 
	 */
	public ContentReference[] getPluginEntryContentReferences(
		IPluginEntry pluginEntry,
		InstallMonitor monitor)
		throws CoreException;

	/**
	 * Returns the total size of all archives required for the
	 * specified plug-in and non-plug-in entries (the "packaging" view).
	 * 
	 * @param pluginEntries an array of plug-in entries
	 * @param nonPluginEntries an array of non-plug-in entries
	 * @return total download size, or an indication that size could not be
	 * determined
	 * @see org.eclipse.update.core.model.ContentEntryModel#UNKNOWN_SIZE
	 * @since 2.0
	 */
	public long getDownloadSizeFor(
		IPluginEntry[] pluginEntries,
		INonPluginEntry[] nonPluginEntries);

	/**
	 * Returns the total size of all files required for the
	 * specified plug-in and non-plug-in entries (the "logical" view).
	 * 
	 * @param pluginEntries an array of plug-in entries
	 * @param nonPluginEntries an array of non-plug-in entries
	 * @return total download size, or an indication that size could not be
	 * determined
	 * @see org.eclipse.update.core.model.ContentEntryModel#UNKNOWN_SIZE
	 * @since 2.0
	 */
	public long getInstallSizeFor(
		IPluginEntry[] pluginEntries,
		INonPluginEntry[] nonPluginEntries);

	/**
	 * Returns the verifier for this feature.
	 * If provided, the verifier is called at various point during
	 * installation processing to verify downloaded archives. The
	 * type of verification provided is dependent on the content
	 * provider implementation.
	 * 
	 * @return verifier
	 * @exception CoreException
	 * @since 2.0
	 */
	public IVerifier getVerifier() throws CoreException;

	/**
	 * Returns the feature associated with this content provider.
	 * 
	 * @return feature for this content provider
	 * @since 2.0
	 */
	public IFeature getFeature();

	/**
	 * Sets the feature associated with this content provider.
	 * In general, this method should only be called as part of
	 * feature creation. Once set, the feature should not be reset.
	 * 
	 * @param feature feature for this content provider
	 * @since 2.0
	 */
	public void setFeature(IFeature feature);
}
