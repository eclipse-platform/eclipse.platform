package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IAdaptable;

/**
 * Plug-in entry defines a packaging reference from a feature to a plug-in.
 * It indicates that the referenced plug-in is to be considered as
 * part of the feature. Note, that this does not necessarily indicate
 * that the plug-in files are packaged together with any other
 * feature files. The actual packaging details are determined by the
 * feature content provider for the feature.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.PluginEntry
 * @see org.eclipse.update.core.FeatureContentProvider
 * @since 2.0
 */
public interface IPluginEntry extends IPlatformEnvironment,IAdaptable {

	/** 
	 * Returns the identifier of this plugin entry
	 * 
	 * @return plugin entry identifier
	 * @since 2.0 
	 */
	public VersionedIdentifier getVersionedIdentifier();

	/**
	 * Returns an indication whethyer this entry represents a fragment.
	 * 
	 * @return <code>true</code> if the entry represents a plug-in fragment, 
	 * <code>false</code> if the entry represents a plug-in
	 * @since 2.0 
	 */
	public boolean isFragment();

	/**
	 * Returns the download size of the entry, if it can be determined.
	 * 
	 * @see org.eclipse.update.core.model.ContentEntryModel#UNKNOWN_SIZE
	 * @return download size of the feature in KiloBytes, or an indication 
	 * the size could not be determined
	 * @since 2.0 
	 */
	public long getDownloadSize();

	/**
	 * Returns the install size of the feature, if it can be determined.
	 * 
	 * @see org.eclipse.update.core.model.ContentEntryModel#UNKNOWN_SIZE
	 * @return install size of the feature in KiloBytes, or an indication 
	 * the size could not be determined
	 * @since 2.0 
	 */
	public long getInstallSize();

	/** 
	 * Sets the identifier of this plugin entry. 
	 * This is typically performed as part of the plug-in entry creation
	 * operation. Once set, the identifier should not be reset.
	 * 
	 * @param identifier plugin entry identifier
	 * @since 2.0 
	 */
	public void setVersionedIdentifier(VersionedIdentifier identifier);

}