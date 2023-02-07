/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.service;

import org.eclipse.help.internal.webapp.servlet.ControlServlet;

/**
 * Controls Eclipse help application from standalone application. This
 * service do not allow remote clients to control Eclipse help application.
 *
 * <p>Extends the {@link org.eclipse.help.internal.webapp.servlet.ControlServlet}
 * servlet.
 *
 * @param command		- specifies the control command. Accepts the following parameters:
 * 						displayHelp | displayHelpWindow | shutdown
 * 						| install | update | enable | disable | uninstall
 * 						| search | listFeatures | addSite | removeSite
 * 						| apply
 *
 * @version	$Version$
 *
 **/
public class ControlService extends ControlServlet {

	private static final long serialVersionUID = 1L;

}
