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
 * An extension of ISite that supports mirrors.
 * The regular update site contains features (optionally grouped by categories), while
 * a mirrored site can define zero or more updates sites (mirrors) with the same content.
 * This allows users to pick their own update site, for performance purposes.
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface ISiteWithMirrors extends ISite {

	/**
	 * Returns an array of mirror sites that contain the same features/plugins.
	 * @return array of mirror sites, or empty array
	 * @throws CoreException
	 * @since 3.1
	 */
	IURLEntry[] getMirrorSiteEntries() throws CoreException;
}
