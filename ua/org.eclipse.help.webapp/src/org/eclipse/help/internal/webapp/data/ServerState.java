/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.webapp.data;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.base.BaseHelpSystem;

public class ServerState {

	/**
	 * Function called when index.jsp is loaded, the main purpose here is to detect the
	 * situation where the Webapp is run without being launched on the built in server
	 * in which case we set the mode to infocenter.
	 */
	public static void webappStarted(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
			BaseHelpSystem.checkMode();
	}

}
