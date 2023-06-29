/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.ui.externaltools.internal.menu;

import org.eclipse.debug.ui.actions.OpenLaunchDialogAction;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

/**
 * Opens the launch config dialog on the external tools launch group.
 */
public class OpenExternalToolsConfigurations extends OpenLaunchDialogAction {

	public OpenExternalToolsConfigurations() {
		super(IExternalToolConstants.ID_EXTERNAL_TOOLS_LAUNCH_GROUP);
	}
}
