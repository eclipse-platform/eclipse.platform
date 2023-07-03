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
package org.eclipse.tips.core;

import java.net.URL;

/**
 * Decoration of {@link Tip} that enables URL content.
 *
 */
public interface IUrlTip {

	/**
	 * Return the string representation of an {@link URL} with the primary goal to
	 * be rendered by the tip manager. Implementations of Tip may also use the URL
	 * to aid the rendering (e.g. by providing other data than HTML, e.g. a text
	 * file).
	 *
	 * @return the string representation of URL to the (remote) content
	 *
	 */
	String getURL();
}