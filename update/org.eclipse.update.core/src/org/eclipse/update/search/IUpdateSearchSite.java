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
package org.eclipse.update.search;

/**
 * This interface is used to represent sites that need to be
 * searched within the search scope. In addition to being an
 * update site adapter, it also returns an array of categories
 * within the site that need not be searched (skipping categories
 * makes the search faster because fewer features need to
 * be checked and potentially downloaded from the server).
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IUpdateSearchSite extends IUpdateSiteAdapter {
/**
 * Returns an array of categories that need not be searched
 * when scanning this site or <samp>null</samp> if all the
 * features must be tested.
 * @return an array of category names or <samp>null</samp> if
 * all the features must be tested.
 */
	String[] getCategoriesToSkip();
}
