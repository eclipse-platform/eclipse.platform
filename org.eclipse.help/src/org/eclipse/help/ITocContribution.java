/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.help;

/**
 * Represents either a complete or partial table of contents, as well as
 * its metadata.
 *
 * @since 3.3
 */
public interface ITocContribution {

	/**
	 * Returns the contribution's category id. Contributions with the same
	 * category id will be grouped together.
	 *
	 * @return the contribution's category id.
	 */
	public String getCategoryId();

	/**
	 * Returns the symbolic name of the bundle that made this contribution,
	 * e.g. "org.eclipse.help"
	 *
	 * @return the contributor id, e.g. "org.eclipse.help"
	 */
	public String getContributorId();

	/**
	 * Returns the hrefs for any additional documents that are not in this TOC
	 * but are associated with it, and should be indexed for searching.
	 *
	 * @return any extra documents associated with the TOC
	 */
	public String[] getExtraDocuments();

	/**
	 * Returns a unique identifier for this contribution.
	 *
	 * @return the contribution's unique identifier
	 */
	public String getId();

	/**
	 * Returns the locale for this contribution.
	 *
	 * @return the contribution's locale
	 */
	public String getLocale();

	/**
	 * Returns the path to the anchor in another toc into which this
	 * one should be linked into.
	 *
	 * @return the link-to path
	 */
	public String getLinkTo();

	/**
	 * Returns the table of contents data for this contribution.
	 *
	 * @return the toc data for this contribution
	 */
	public IToc getToc();

	/**
	 * Returns whether or not this is a top-level contribution (a book).
	 *
	 * @return whether the contribution is top-level book
	 */
	public boolean isPrimary();
}
