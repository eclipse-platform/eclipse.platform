/*******************************************************************************
 * Copyright (c) 2005, 2015 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation - 122967 [Help] Remote help system
 *******************************************************************************/
package org.eclipse.help;

/**
 * IIndexEntry represents a single entry of the help index. It includes
 * a keyword and related references into help content.
 *
 * @since 3.2
 */
public interface IIndexEntry extends IUAElement {

	/**
	 * Returns the keyword that this entry is associated with
	 *
	 * @return the keyword
	 */
	public String getKeyword();

	/**
	 * Obtains topics assosiated with this index entry (i.e. keyword).
	 *
	 * @return array of ITopic
	 */
	public ITopic[] getTopics();

	/**
	 * Obtains the index subentries contained in the entry.
	 *
	 * @return the index subentries
	 */
	public IIndexEntry[] getSubentries();
}
