/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.debug.core.commands.ISuspendHandler;
import org.eclipse.debug.ui.actions.DebugCommandHandler;

/**
 * Default handler for command.  It ensures that the keyboard accelerator works even
 * if the menu action set is not enabled.
 *
 * @since 3.8
 */
public class SuspendCommandHandler extends DebugCommandHandler {

	@Override
	protected Class<ISuspendHandler> getCommandType() {
		return ISuspendHandler.class;
	}

}
