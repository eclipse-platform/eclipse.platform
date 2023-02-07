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
 * A directive indicating the criteria information of a Toc or Topic described in xml
 *
 * @since 3.5
 */

public interface ICriteria extends IUAElement {

	/**
	 * Returns the name of the criteria element, e.g.
	 * "Platform"
	 *
	 * @return the name of the criteria element
	 */
	public String getName();

	/**
	 * Returns the value of the criteria element, e.g.
	 * "AIX,Windows"
	 *
	 * @return the value of the criteria element
	 */
	public String getValue();
}
