package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IAdaptable;

/**
 * Non-plug-in entry defines an arbitrary non-plug-in data file packaged
 * as part of a feature. Non-plug-in entries are not interpreted by the
 * platform (other than being downloaded as part of an install action).
 * They require a custom install handler to be specified as part of the
 * feature. Note, that defining a non-plug-in entry does not necessarily
 * indicate the non-plug-in file is packaged together with any other
 * feature files. The actual packaging details are determined by the
 * feature content provider for the feature.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.NonPluginEntry
 * @see org.eclipse.update.core.FeatureContentProvider
 * @since 2.0
 */
public interface INonPluginEntry extends IPlatformEnvironment, IAdaptable {

	/** 
	 * Returns the identifier of this data entry. 
	 * 
	 * @return data entry identifier
	 * @since 2.0 
	 */
	public String getIdentifier();

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
}