/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.jface.action.IAction;

/**
 * Step over action delegate.
 *
 * @since 3.3
 */
public class StepOverCommandActionDelegate extends DebugCommandActionDelegate {

	public StepOverCommandActionDelegate() {
		super();
		setAction(new StepOverCommandAction());
	}

	@Override
	public void init(IAction action) {
		super.init(action);
	}


}
