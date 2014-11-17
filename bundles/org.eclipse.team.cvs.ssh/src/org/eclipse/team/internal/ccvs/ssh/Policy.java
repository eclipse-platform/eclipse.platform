/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ssh;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

public class Policy {
	// debug constants
	public static boolean DEBUG_SSH_PROTOCOL = false;

	static final DebugOptionsListener DEBUG_OPTIONS_LISTENER = new DebugOptionsListener() {
		public void optionsChanged(DebugOptions options) {
			boolean DEBUG = options.getBooleanOption(SSHPlugin.ID + "/debug", false); //$NON-NLS-1$
			DEBUG_SSH_PROTOCOL = DEBUG && options.getBooleanOption(SSHPlugin.ID + "/ssh_protocol", false); //$NON-NLS-1$
		}
	};
}
