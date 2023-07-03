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
 * ICriterionValueDefinition represents one criterion value definition of one criterion.
 * It includes value id and its display name.
 *
 * @since 3.5
 */
public interface ICriterionValueDefinition extends IUAElement {

	/**
	 * Returns the id that this criterion value
	 *
	 * @return the id
	 */
	public String getId();

	/**
	 * Obtains the display name associated with this criterion value.
	 *
	 * @return the name
	 */
	public String getName();
}
