package org.eclipse.update.core.model;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * Plug-in entry model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * @see org.eclipse.update.core.PluginEntry
 * @since 2.0
 */
public class PluginEntryModel extends ContentEntryModel {

	private String pluginId;
	private String pluginVersion;
	private boolean isFragment = false;
	
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
	 * @see Object#toString()
	 */
	public String toString() {
		String msg = (getPluginIdentifier()!=null)?getPluginIdentifier().toString():"";
		msg += getPluginVersion()!=null?" "+getPluginVersion().toString():"";
		msg += isFragment()?" fragment":" plugin";
		return msg;
	}

}