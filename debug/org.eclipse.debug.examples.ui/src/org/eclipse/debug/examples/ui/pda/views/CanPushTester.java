/*******************************************************************************
 * Copyright (c) 2008, 2015 Wind River Systems and others.
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
package org.eclipse.debug.examples.ui.pda.views;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.examples.core.pda.model.PDADebugElement;
import org.eclipse.debug.examples.core.pda.model.PDAStackFrame;
import org.eclipse.debug.examples.core.pda.model.PDAThread;

/**
 * Property tester for use with standard expressions to determine whether
 * the given debug target can perform a push operation.
 */
public class CanPushTester extends PropertyTester {

	private static final String CAN_PUSH_PROPERTY = "canPush"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (CAN_PUSH_PROPERTY.equals(property)) {
			if (receiver instanceof IAdaptable) {
				PDADebugElement element = ((IAdaptable) receiver).getAdapter(PDADebugElement.class);
				PDAThread thread = null;
				if (element instanceof PDAThread) {
					thread = (PDAThread)element;
				} else if (element instanceof PDAStackFrame) {
					thread = (PDAThread)((PDAStackFrame)element).getThread();
				}

				if (thread != null) {
					return thread.canPushData();
				}
			}
		}
		return false;
	}
}
