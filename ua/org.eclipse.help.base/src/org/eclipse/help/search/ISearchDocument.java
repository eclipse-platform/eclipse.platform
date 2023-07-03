/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

import java.io.Reader;

/**
 * Represents a document in the search index. This interface is used by
 * clients which implement the search participant extension point. While the
 * help system uses the Lucene classes internally this interface does not
 * import anything from Lucene and enables the API for adding search participants
 * to remain binary compatible whatever changes are made in Lucene.
 *
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.5
 */

public interface ISearchDocument {

	/**
	 * @param title the title which will be shown in the search results summary.
	 * This function should be called exactly once per document
	 */
	public void setTitle(String title);

	/**
	 * @param summary the summary which will be used when displaying search results.
	 * This function should be called exactly once per document
	 */
	public void setSummary(String summary);

	/**
	 * @param contents the text which will be used when performing a search. The search index
	 * will parse the contents and store the results in a database for rapid retrieval.
	 */
	public void addContents(String contents);

	/**
	 * Alternate way of adding contents which is closer to the API for the
	 * deprecated extension point org.eclipse.help.base.luceneSearchParticipants.
	 * This is provided for backward compatibility, most users will find it simpler
	 * to use <code>addContents(String)</code>
	 * @param contents a Reader object which provides the contents
	 * @param exactContents a second Reader object which contains the same same contents
	 */
	public void addContents(Reader contents, Reader exactContents);

	/**
	 * Allows search participant to indicate that a document will be processed
	 * at runtime and that parts of the contents may not be displayed to the
	 * user, causing a match in the search to be shown as a potential match.
	 * By default documents are considered to be unfiltered so this function
	 * need only be called to indicate that a document is filtered
	 */
	public void setHasFilters(boolean hasFilters);

}
