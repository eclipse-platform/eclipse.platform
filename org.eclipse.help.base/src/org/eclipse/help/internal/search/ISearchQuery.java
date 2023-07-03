/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.help.internal.search;

import java.util.Collection;

public interface ISearchQuery {
	/**
	 * Obtains names of fields in addition to default field
	 */
	public Collection<String> getFieldNames();
	/**
	 * Obtains search word (user query)
	 */
	public String getSearchWord();
	/**
	 * @return true if search only in specified fields, not the default field
	 */
	public boolean isFieldSearch();
	/**
	 * Obtains locale
	 */
	public String getLocale();
}
