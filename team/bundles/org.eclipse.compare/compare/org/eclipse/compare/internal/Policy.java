/*******************************************************************************
 * Copyright (c) 2015, 2017 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stefan Xenos <sxenos@gmail.com> (Google) - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.osgi.service.debug.DebugOptionsListener;

/**
 * Policy implements NLS convenience methods for the plugin and
 * makes progress monitor policy decisions
 */
public class Policy {
	// Debug flags
	public static boolean debugContentMergeViewer = false;

	static final DebugOptionsListener DEBUG_OPTIONS_LISTENER = options -> {
		boolean DEBUG = options.getBooleanOption(CompareUIPlugin.PLUGIN_ID + "/debug", false); //$NON-NLS-1$
		debugContentMergeViewer = DEBUG && options.getBooleanOption(CompareUIPlugin.PLUGIN_ID + "/content_merge_viewer", false); //$NON-NLS-1$
	};
}
