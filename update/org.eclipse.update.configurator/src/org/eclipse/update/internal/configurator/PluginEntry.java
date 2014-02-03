/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import org.eclipse.osgi.util.NLS;


/**
 */
public class PluginEntry {

	private String pluginId;
	private String pluginVersion;
	private boolean isFragment = false;
	private VersionedIdentifier versionId;
	private String url;
	
	public PluginEntry() {
		super();
	}

	/**
	 * @return url relative to the site location: plugins/org.eclipse.foo/plugin.xml
	 * Note: to do: we should probably only use plugins/org.eclipse.foo/ in the future
	 */
	public String getURL() {
		return url;
	}
	
	/**
	 * url is relative to the site
	 */
	public void setURL(String url) {
		this.url = url;
	}

	/**
	 * Returns the plug-in identifier for this entry.
	 * 
	 * @return the plug-in identifier, or <code>null</code>
	 */
	public String getPluginIdentifier() {
		return pluginId;
	}

	/**
	 * Returns the plug-in version for this entry.
	 * 
	 * @return the plug-in version, or <code>null</code>
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
	 */
	public boolean isFragment() {
		return isFragment;
	}

	/**
	 * Sets the entry plug-in identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param pluginId the entry identifier.
	 */
	void setPluginIdentifier(String pluginId) {
		this.pluginId = pluginId;
	}

	/**
	 * Sets the entry plug-in version.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param pluginVersion the entry version.
	 */
	void setPluginVersion(String pluginVersion) {
		this.pluginVersion = pluginVersion;
	}

	/**
	 * Indicates whether this entry represents a fragment or plug-in.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param isFragment fragment setting
	 */
	public void isFragment(boolean isFragment) {
		this.isFragment = isFragment;
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


	/**
	 * Returns the identifier of this plugin entry
	 */
	public VersionedIdentifier getVersionedIdentifier() {
		if (versionId != null)
			return versionId;

		String id = getPluginIdentifier();
		String ver = getPluginVersion();
		if (id != null && ver != null) {
			try {
				versionId = new VersionedIdentifier(id, ver);
				return versionId;
			} catch (Exception e) {
				Utils.log(NLS.bind(Messages.PluginEntry_versionError, (new String[] { id, ver })));
			}
		}

		versionId = new VersionedIdentifier("",null); //$NON-NLS-1$
		return versionId;
	}

	/**
	 * Sets the identifier of this plugin entry. 
	 * 
	 */
	void setVersionedIdentifier(VersionedIdentifier identifier) {
		setPluginIdentifier(identifier.getIdentifier());
		setPluginVersion(identifier.getVersion().toString());
	}	

	/**
	 * Compares two plugin entries for equality
	 * 
	 * @param object plugin entry object to compare with
	 * @return <code>true</code> if the two entries are equal, 
	 * <code>false</code> otherwise
	 */
	public boolean equals(Object object) {
		if (!(object instanceof PluginEntry))
			return false;
		PluginEntry e = (PluginEntry) object;
		return getVersionedIdentifier().equals(e.getVersionedIdentifier());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getVersionedIdentifier().hashCode();
	}
}
