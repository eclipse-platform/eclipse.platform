/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.debug.internal.ui.actions.breakpoints.Messages"; //$NON-NLS-1$
	public static String RetargetMethodBreakpointAction_0;
	public static String RetargetToggleBreakpointAction_0;
	public static String RetargetToggleLineBreakpointAction_0;
	public static String RetargetWatchpointAction_0;
	public static String BreakpointTypesContribution_0;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
