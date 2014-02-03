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
package org.eclipse.update.core;

import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;

/**
 * Convenience implementation of plug-in entry.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.IPluginEntry
 * @see org.eclipse.update.core.model.PluginEntryModel
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class PluginEntry extends PluginEntryModel implements IPluginEntry {
	
	// PERF: new instance variable
	private VersionedIdentifier versionId;

	/**
	 * Plug-in entry default constructor
	 */
	public PluginEntry() {
		super();
	}

	/**
	 * Returns the identifier of this plugin entry
	 * 
	 * @see IPluginEntry#getVersionedIdentifier()
	 * @since 2.0
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
				UpdateCore.warn("Unable to create versioned identifier:" + id + ":" + ver); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		versionId = new VersionedIdentifier("",null); //$NON-NLS-1$
		return versionId;
	}

	/**
	 * Sets the identifier of this plugin entry. 
	 * 
	 * @see IPluginEntry#setVersionedIdentifier(VersionedIdentifier)
	 * @since 2.0
	 */
	public void setVersionedIdentifier(VersionedIdentifier identifier) {
		setPluginIdentifier(identifier.getIdentifier());
		setPluginVersion(identifier.getVersion().toString());
	}	

	/**
	 * Compares two plugin entries for equality
	 * 
	 * @param object plugin entry object to compare with
	 * @return <code>true</code> if the two entries are equal, 
	 * <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean equals(Object object) {
		if (!(object instanceof IPluginEntry))
			return false;
		IPluginEntry e = (IPluginEntry) object;
		return getVersionedIdentifier().equals(e.getVersionedIdentifier());
	}

}
