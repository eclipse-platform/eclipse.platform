/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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

import java.util.Arrays;

import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.debug.internal.core.commands.DebugCommandRequest;

/**
 * Boolean collector that collects boolean results from a number of voters.
 * Request is cancelled when one voter votes false.
 *
 * @since 3.3
 *
 */
public class UpdateActionsRequest extends DebugCommandRequest implements IEnabledStateRequest {

	private IEnabledTarget[] fActions;
	private boolean fEnabled = false;

	public UpdateActionsRequest(Object[] elements, IEnabledTarget[] actions) {
		super(elements);
		fActions = actions;
	}

	@Override
	public synchronized void setEnabled(boolean result) {
		fEnabled = result;
	}

	@Override
	public synchronized void done() {
		if (!isCanceled()) {
			for (IEnabledTarget action : fActions) {
				action.setEnabled(fEnabled);
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " on " + fActions.length //$NON-NLS-1$
				+ " actions from " //$NON-NLS-1$
				+ Arrays.toString(getElements());
	}

}
