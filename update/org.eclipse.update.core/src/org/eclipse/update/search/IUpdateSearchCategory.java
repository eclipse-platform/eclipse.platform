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

/**
 * This interface is used to encapsulate a particular Update
 * search pattern. Each search category is free to scan
 * sites using a specific algorithm. Search category must
 * have a unique ID. The actual search is performed in
 * search queries. A category can provide one or more
 * queries to run during the search.
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
public interface IUpdateSearchCategory {
/**
 * Returns the unique identifier of this search category.
 */
	public String getId();
/**
 * Accepts the identifier assigned to this category during
 * the registry reading.
 */
	public void setId(String id);

/**
 * Returns an array of update search queries that need to 
 * be run during the search.
 * @return an arry of update search queries
 */	
	IUpdateSearchQuery [] getQueries();
}
