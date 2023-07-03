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
 * ICriterionDefinition represents a single criterion definition of the help content.
 * It includes criterion id and its related values id, and also their display names.
 *
 * @since 3.5
 */
public interface ICriterionDefinition extends IUAElement{

	/**
	 * Returns the id that this criterion definition is associated with
	 *
	 * @return the id
	 */
	public String getId();

	/**
	 * Obtains the display name associated with this criterion definition.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * Obtains the criterion value definitions contained in the criterion.
	 *
	 * @return array of ICriterionValueDefinition
	 */
	public ICriterionValueDefinition[] getCriterionValueDefinitions();
}
