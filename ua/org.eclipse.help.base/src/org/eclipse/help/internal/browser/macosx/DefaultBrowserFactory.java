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
package org.eclipse.help.internal.browser.macosx;

import org.eclipse.help.browser.*;

public class DefaultBrowserFactory implements IBrowserFactory {

	public DefaultBrowserFactory() {
		super();
	}

	@Override
	public boolean isAvailable() {
		return System.getProperty("os.name").equals("Mac OS X"); //$NON-NLS-1$ //$NON-NLS-2$
		/*
		 * we assume that every Mac OS X has an "/usr/bin/osascript" so we don't
		 * test any further
		 */
	}

	@Override
	public IBrowser createBrowser() {
		return DefaultBrowserAdapter.getInstance();
	}
}
