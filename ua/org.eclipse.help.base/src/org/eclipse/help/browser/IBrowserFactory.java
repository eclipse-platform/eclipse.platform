/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.browser;
/**
 * Implementators of <code>org.eclipse.help.base.browser</code> extension
 * points must provide implementation of this interface.
 *
 * @since 2.1
 */
public interface IBrowserFactory {
	/**
	 * Checks whether the factory can work on the user system.
	 *
	 * @return false if the factory cannot work on this system; for example the
	 *         required native browser required by browser adapters that it
	 *         creates is not installed.
	 */
	public boolean isAvailable();
	/**
	 * Obtains a new instance of a web browser.
	 *
	 * @return instance of IBrowser
	 */
	public IBrowser createBrowser();
}
