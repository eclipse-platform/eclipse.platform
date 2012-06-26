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

import org.eclipse.update.core.model.ImportModel;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;

/**
 * Convenience implementation of a plug-in dependency.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.IImport
 * @see org.eclipse.update.core.model.ImportModel
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class Import extends ImportModel implements IImport {

	//PERF: new instance variable
	private VersionedIdentifier versionId;

	/**
	 * Returns an identifier of the dependent plug-in.
	 * @see IImport#getVersionedIdentifier()
	 */
	public VersionedIdentifier getVersionedIdentifier() {
		if (versionId != null)
			return versionId;

		String id = getIdentifier();
		String ver = getVersion();
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
	 * Returns the matching rule for the dependency.
	 * @see IImport#getRule()
	 */
	public int getRule() {
		return UpdateManagerUtils.getMatchingRule(getMatchingRuleName());
	}
	
	/**
	 * Returns the matching rule for the dependency identifier.
	 * @see IImport#getIdRule()
	 */
	public int getIdRule() {
		return UpdateManagerUtils.getMatchingIdRule(getMatchingIdRuleName());
	}
	
	/**
	 * 
	 * @see org.eclipse.update.core.IImport#getKind()
	 */

	/**
	 * Returns the dependency kind
	 * @see org.eclipse.update.core.IImport#getKind()
	 */
	public int getKind() {
		return isFeatureImport()?KIND_FEATURE:KIND_PLUGIN;
	}

}
