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

package org.eclipse.update.search;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.update.core.ISiteWithMirrors;
import org.eclipse.update.core.IURLEntry;

/**
 * Search results are collected by implementing this interface
 * and passing it to the search request. If the implementation is
 * visual, it is recommended that the match is shown as soon
 * as it is collected (rather than kept in a list and presented
 * at the end of the search). This interface should be implemented 
 * when you want to support collection of results from a mirror site,
 * otherwise you can just implement the IUpdateSearchResultsCollector.
  * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.1
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IUpdateSearchResultCollectorFromMirror extends
		IUpdateSearchResultCollector {

	/**
	 * Returns a mirror of the specified site. Normally, if the site defines some mirrors,
	 * this method can be implement so that it prompts the user to pick one of the mirrors.
	 * @param site the site to get the mirror for
	 * @param siteName the name of the site
	 * @return a mirror (url+label) for the specified site, or null if no mirror is needed
	 * @throws OperationCanceledException if the user chooses to cancel
	 * the prompt instead of choosing the mirror from the list.
	 */
	public IURLEntry getMirror(ISiteWithMirrors site, String siteName) throws OperationCanceledException;
}
