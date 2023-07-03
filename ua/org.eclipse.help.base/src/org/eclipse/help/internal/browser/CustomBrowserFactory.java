/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.browser;

import org.eclipse.help.browser.*;

/**
 * Produces Custom Browser
 */
public class CustomBrowserFactory implements IBrowserFactory {

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public IBrowser createBrowser() {
		return new CustomBrowser();
	}

}
