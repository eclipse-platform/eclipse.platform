/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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

import org.eclipse.debug.core.commands.IResumeOthersHandler;
import org.eclipse.debug.ui.actions.DebugCommandHandler;

/**
 * Default handler for command. It ensures that the keyboard accelerator works
 * even if the menu action set is not enabled.
 *
 * @since 3.24
 */
public class ResumeOthersCommandHandler extends DebugCommandHandler {

	@Override
	protected Class<IResumeOthersHandler> getCommandType() {
		return IResumeOthersHandler.class;
	}

}
