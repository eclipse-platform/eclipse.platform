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
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.internal.core.*;

/**
 * Versioned Identifier. This is a utility class combining an identification
 * string with a version.
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p> 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.core.runtime.PluginVersionIdentifier
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class VersionedIdentifier {
	private String id;
	private PluginVersionIdentifier version;
	private static final String SEPARATOR = "_"; //$NON-NLS-1$

	/**
	 * Construct a versioned identifier from an identifier and a string
	 * representation of a version
	 * 
	 * @see org.eclipse.core.runtime.PluginVersionIdentifier#toString()
	 * @param id identifier string
	 * @param versionName string representation of version
	 * @since 2.0
	 */
	public VersionedIdentifier(String id, String versionName) {
		if (id == null	|| (id = id.trim()).equals("")) //$NON-NLS-1$
			throw new IllegalArgumentException(
				NLS.bind(Messages.VersionedIdentifier_IdOrVersionNull, (new String[] { id, versionName })));
		this.id = id;
		// 15707
		if (versionName != null){
			// if (PluginVersionIdentifier.validateVersionIdentifier(versionName).isOk())
			try {
				this.version = new PluginVersionIdentifier(versionName);
			} catch (RuntimeException e){
				UpdateCore.warn("Invalid Version:"+versionName,e); //$NON-NLS-1$
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
		return id.equals("") ? "" : id + SEPARATOR + version.toString(); //$NON-NLS-1$ //$NON-NLS-2$
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
