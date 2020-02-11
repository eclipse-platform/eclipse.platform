/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ssh2;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

public class Policy {
	// debug constants
	public static boolean DEBUG = false;

	static final DebugOptionsListener DEBUG_OPTIONS_LISTENER = new DebugOptionsListener() {
		@Override
		public void optionsChanged(DebugOptions options) {
			DEBUG = options.getBooleanOption(CVSSSH2Plugin.ID + "/debug", false); //$NON-NLS-1$
		}
	};
}
