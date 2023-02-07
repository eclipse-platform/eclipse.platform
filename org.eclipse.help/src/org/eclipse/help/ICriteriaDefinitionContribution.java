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
 * Represents criteria definition contribution
 *
 * @since 3.5
 */

public interface ICriteriaDefinitionContribution {

	/**
	 * Returns a unique identifier for this criteria definition.
	 *
	 * @return the contribution's unique identifier
	 */
	public String getId();

	/**
	 * Returns this contributions criteria definition.
	 *
	 * @return the criteria definition data for this contribution
	 */
	public ICriteriaDefinition getCriteriaDefinition();

	/**
	 * Returns the locale for this contribution.
	 *
	 * @return the contribution's locale
	 */
	public String getLocale();
}
