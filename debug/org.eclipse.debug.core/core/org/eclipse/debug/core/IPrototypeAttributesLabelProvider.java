/*******************************************************************************
 * Copyright (c) 2017 Obeo.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core;

/**
 * A prototype attributes label provider is contributed as an optional attribute
 * of a <code>launchConfigurationType</code> extension and is responsible for
 * displaying launch configurations prototype attributes of that type.
 *
 * @since 3.12
 */
public interface IPrototypeAttributesLabelProvider {

	/**
	 * Get a human readable label to associate to this attribute.
	 *
	 * @param attribute the given attribute.
	 * @return a human readable label of this attribute.
	 */
	String getAttributeLabel(String attribute);
}
