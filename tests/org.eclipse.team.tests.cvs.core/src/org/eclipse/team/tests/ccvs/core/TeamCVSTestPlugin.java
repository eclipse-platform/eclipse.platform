/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core;

import org.eclipse.core.runtime.Plugin;

public class TeamCVSTestPlugin extends Plugin {

	public static final boolean IS_UNSTABLE_TEST = true;

	private static TeamCVSTestPlugin fgPlugin;


	public TeamCVSTestPlugin() {
		fgPlugin = this;
	}

	public static TeamCVSTestPlugin getDefault() {
		return fgPlugin;
	}

}
