package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.update.internal.core.Policy;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

/**
 * Versioned Identifier. This is a utility class combining an identification
 * string with a version.
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.Version
 * @since 2.0
 */
public class VersionedIdentifier {
	private String id;
	private PluginVersionIdentifier version;
	private static final String SEPARATOR = "_"; //$NON-NLS-1$

	/**
	 * Construct a versioned identifier from an identifier and a string
	 * representation of a version
	 * 
	 * @see Version#toString()
	 * @param id identifier string
	 * @param versionName string representation of version
	 * @since 2.0
	 */
	public VersionedIdentifier(String id, String versionName) {
		if (id == null	|| (id = id.trim()).equals("")) //$NON-NLS-1$
			throw new IllegalArgumentException(
				Policy.bind("VersionedIdentifier.IdOrVersionNull", id, versionName));
		//$NON-NLS-1$
		this.id = id;
		// 15707
		if (versionName != null){
			// if (PluginVersionIdentifier.validateVersionIdentifier(versionName).isOk())
			try {
				this.version = new PluginVersionIdentifier(versionName);
			} catch (RuntimeException e){
				UpdateManagerPlugin.warn("Invalid Version:"+versionName,e);
			}
		}
		if (this.version==null)
			version = new PluginVersionIdentifier(0, 0, 0);
	}

	/**
	 * Returns the identifier
	 * 
	 * @return identifier
	 * @since 2.0
	 */
	public String getIdentifier() {
		return id;
	}

	/**
	 * Returns the version
	 * 
	 * @return version
	 * @since 2.0
	 */
	public PluginVersionIdentifier getVersion() {
		return version;
	}

	/**
	 * Returns a string representation of the versioned identifier.
	 * 
	 * @return string representation of versioned identifier. The resulting 
	 * string is <id>_<version>, where <id> is the identifier and 
	 * <version> is the string representation of the version
	 * @since 2.0
	 */
	public String toString() {
		return id.equals("") ? "" : id + SEPARATOR + version.toString();
		//$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Compares two versioned identifiers for equality
	 * 
	 * @param obj other versioned identifier to compare to
	 * @return <code>true</code> if the two objects are equal, 
	 * <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean equals(Object obj) {
//System.out.println("Compare :"+this+" and "+obj);		
		
		if (!(obj instanceof VersionedIdentifier))
			return false;
		VersionedIdentifier vid = (VersionedIdentifier) obj;
		if (!this.id.equals(vid.id))
			return false;
		return this.version.equals(vid.version);
	}

	/**
	 * Returns a computed hashcode for the versioned identifier.
	 * 
	 * @return hash code
	 * @since 2.0
	 */
	public int hashCode() {
		return toString().hashCode();
	}

}