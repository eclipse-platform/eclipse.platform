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
 * ICriteriaDefinition represents the criteria definition of one plug-in.
 * It contains criterion definitions, each of them is criterion id and its
 * display name, and criterion values id and their display names.
 *
 * @since 3.5
 */

public interface ICriteriaDefinition extends IUAElement{

	/**
	 * Obtains the criterion definitions contained in the definition file.
	 *
	 * @return Array of ICriterionDefinition
	 */
	ICriterionDefinition[] getCriterionDefinitions();
}
