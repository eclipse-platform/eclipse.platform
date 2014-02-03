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

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;

/**
 * A search query.
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
public interface IUpdateSearchQuery {
/**
 * Returns an update site adapter that should be added to the scope
 * while running this query. Some search category may need to 
 * scan specific site adapter in addition to those specified in 
 * the search scope.
 * 
 * @return the query-specific site adapter or <samp>null</samp> if
 * not specified.
 * 
 */
	public IQueryUpdateSiteAdapter getQuerySearchSite();

/**
 * Executes the query. The implementors should scan the provided
 * update site (skipping certain categories if provided) and
 * pass the matches to the result collector. The query is also
 * responsible for scoping and moving the provided progress monitor.
 * 
 * @param site the update site to scan
 * @param categoriesToSkip an array of category names that need to be skipped or <samp>null</samp> if categories should not be taken into account.
 * @param filter a filter to apply before passing the match to collector
 * @param collector an object that is used for reporting search results
 * @param monitor a progress monitor to report search progress within the provided site
 */
	public void run(ISite site, String [] categoriesToSkip, IUpdateSearchFilter filter, IUpdateSearchResultCollector collector, IProgressMonitor monitor);
}
