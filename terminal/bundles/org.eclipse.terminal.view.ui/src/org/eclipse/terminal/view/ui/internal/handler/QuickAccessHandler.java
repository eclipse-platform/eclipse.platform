/*******************************************************************************
 * Copyright (c) 2014, 2018 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.handler;

import org.eclipse.e4.core.di.annotations.Execute;

/**
 * Quick access handler implementation.
 */
public class QuickAccessHandler {

	@Execute
	public void execute() {
		AbstractTriggerCommandHandler.triggerCommandStatic("org.eclipse.ui.window.quickAccess", null); //$NON-NLS-1$
	}

}
