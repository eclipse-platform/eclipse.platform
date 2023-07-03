/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.json;

import org.eclipse.tips.core.Tip;

/**
 * Decorator of {@link Tip} to mark that this it is constructed from a Json
 * structure.
 *
 */
public interface IJsonTip {

	/**
	 * Returns the JsonObject that describes this Tip.
	 *
	 * @return the JsonObject describing the tip.
	 */
	String getJsonObject();
}