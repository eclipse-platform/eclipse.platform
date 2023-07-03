/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A search engine that is a participant in the help search. All engines
 * selected by the user to run are executed as background jobs using the same
 * search expression. Engine is expected to accept the expression and the scope
 * object. The results should be passed to the result collector object, and the
 * search progress should be tracked by the progress monitor.
 * <p>
 * Search engines that have API access to the search server can provide a list
 * of individual search results. Search engines for which only the URL is known
 * are expected to return one search result containing a URL that is sufficient
 * to open a web browser and see the search results there.
 * <p>
 * Search engine must be cancelable.
 * <p>This interface is intended to be implemented by clients.
 *
 * @since 3.1
 */

public interface ISearchEngine {
	/**
	 *
	 * @param query
	 *            the search expression
	 * @param scope
	 *            the engine-specific scope object that is used to narrow the
	 *            search or <code>null</code> if the default scope is assumed.
	 * @param collector
	 *            the search result collector handles results as they arrive
	 * @param monitor
	 *            progress monitor to track the search progress
	 * @throws CoreException
	 *             when there are problems in the engine
	 */
	void run(String query, ISearchScope scope,
			ISearchEngineResultCollector collector, IProgressMonitor monitor)
			throws CoreException;
}
