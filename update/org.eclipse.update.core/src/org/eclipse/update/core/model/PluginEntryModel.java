/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core.model;


/**
 * Plug-in entry model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.PluginEntry
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class PluginEntryModel extends ContentEntryModel {

	private String pluginId;
	private String pluginVersion;
	private boolean isFragment = false;
	private boolean unpack = true;
	
	/**
	 * Creates a uninitialized plug-in entry model object.
	 * 
	 * @since 2.0
	 */
	public PluginEntryModel() {
		super();
	}

	/**
	 * Compares two plug-in models for equality
	 * 
	 * @param obj other model to compare to
	 * @return <code>true</code> if the models are equal, <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof PluginEntryModel))
			return false;
		PluginEntryModel model = (PluginEntryModel) obj;
		
		return (
			(getPluginIdentifier().equals(model.getPluginIdentifier()))
				&& (getPluginVersion().equals(model.getPluginVersion()))
				&& (isFragment() == model.isFragment()));
	}

	/**
	 * Returns the plug-in identifier for this entry.
	 * 
	 * @return the plug-in identifier, or <code>null</code>
	 * @since 2.0 
	 */
	public String getPluginIdentifier() {
		return pluginId;
	}

	/**
	 * Returns the plug-in version for this entry.
	 * 
	 * @return the plug-in version, or <code>null</code>
	 * @since 2.0 
	 */
	public String getPluginVersion() {
		return pluginVersion;
	}

	/**
	 * Indicates whether the entry describes a full plug-in, or 
	 * a plug-in fragment.
	 * 
	 * @return <code>true</code> if the entry is a plug-in fragment, 
	 * <code>false</code> if the entry is a plug-in
	 * @since 2.0 
	 */
	public boolean isFragment() {
		return isFragment;
	}

	/**
	 * Sets the entry plug-in identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param pluginId the entry identifier.
	 * @since 2.0
	 */
	public void setPluginIdentifier(String pluginId) {
		assertIsWriteable();
		this.pluginId = pluginId;
	}

	/**
	 * Sets the entry plug-in version.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param pluginVersion the entry version.
	 * @since 2.0
	 */
	public void setPluginVersion(String pluginVersion) {
		assertIsWriteable();
		this.pluginVersion = pluginVersion;
	}

	/**
	 * Indicates whether this entry represents a fragment or plug-in.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param isFragment fragment setting
	 * @since 2.0
	 */
	public void isFragment(boolean isFragment) {
		assertIsWriteable();
		this.isFragment = isFragment;
	}

	/**
	 * @return Indicates whether plugin should be unpacked during installation
	 * or can run from a jar
	 * @since 3.0
	 */
	public boolean isUnpack() {
		// TODO this is a candidate for IPluginEntry API
		return unpack;
	}
	/**
	 * @param unpack Sets whether plugin should be unpacked during installation
	 * or can run from a jar
	 * @since 3.0
	 *
	 */
	public void setUnpack(boolean unpack) {
		// TODO this is a candidate for IPluginEntry API
		assertIsWriteable();
		this.unpack = unpack;
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		String msg = (getPluginIdentifier()!=null)?getPluginIdentifier().toString():""; //$NON-NLS-1$
		msg += getPluginVersion()!=null?" "+getPluginVersion().toString():""; //$NON-NLS-1$ //$NON-NLS-2$
		msg += isFragment()?" fragment":" plugin"; //$NON-NLS-1$ //$NON-NLS-2$
		return msg;
	}

}
