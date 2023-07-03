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
 * Represents either a complete or partial keyword index, as well as its
 * metadata.
 *
 * @since 3.3
 */
public interface IIndexContribution {

	/**
	 * Returns a unique identifier for this contribution.
	 *
	 * @return the contribution's unique identifier
	 */
	public String getId();

	/**
	 * Returns this contributions index.
	 *
	 * @return the index data for this contribution
	 */
	public IIndex getIndex();

	/**
	 * Returns the locale for this contribution.
	 *
	 * @return the contribution's locale
	 */
	public String getLocale();
}
