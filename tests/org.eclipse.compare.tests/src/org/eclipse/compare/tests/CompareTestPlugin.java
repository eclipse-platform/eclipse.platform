/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
