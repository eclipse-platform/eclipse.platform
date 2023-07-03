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

package org.eclipse.help;


/**
 * <code>AbstractCriteriaProvider</code> is a mechanism for assigning criteria to
 * <code>ITopic</code> and <code>IToc</code> elements independent of a table of contents
 * file. The criteria defined by this class are merged with those from the table of contents
 * or from other criteria providers by creating a union of defined criteria and
 * defined criteria values.
 * @since 3.5
 */
public abstract class AbstractCriteriaProvider {

	/**
	 * Gets criteria for a topic
	 * @param topic a topic from a table of contents or index
	 * @return an array of criteria which will be added to those already defined in
	 * the table of contents file
	 */
	public abstract ICriteria[] getCriteria(ITopic topic);

	/**
	 * Gets criteria for a table of contents
	 * @param toc a table of contents
	 * @return an array of criteria which will be added to those already defined in
	 * the table of contents file
	 */
	public abstract ICriteria[] getCriteria(IToc toc);

}
