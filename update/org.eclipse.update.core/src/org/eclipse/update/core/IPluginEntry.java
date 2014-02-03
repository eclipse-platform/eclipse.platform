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

import org.eclipse.core.runtime.*;

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
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.PluginEntry
 * @see org.eclipse.update.core.FeatureContentProvider
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
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
