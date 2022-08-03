/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import org.eclipse.core.runtime.Plugin;

/**
 * The main plug-in class to be used in the desktop.
 *
 * @since 3.1
 */
public class CompareTestPlugin extends Plugin {

	private static CompareTestPlugin fgPlugin;

	public CompareTestPlugin() {
		fgPlugin= this;
	}

	public static CompareTestPlugin getDefault() {
		return fgPlugin;
	}
}
