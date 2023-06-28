/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
package org.eclipse.debug.examples.ui.pda.adapters;

import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.internal.ui.viewers.update.DebugEventHandler;
import org.eclipse.debug.internal.ui.viewers.update.DebugTargetEventHandler;
import org.eclipse.debug.internal.ui.viewers.update.DebugTargetProxy;

/**
 * @since 3.2
 *
 */
public class PDADebugTargetProxy extends DebugTargetProxy {

	/**
	 * @param target
	 */
	public PDADebugTargetProxy(IDebugTarget target) {
		super(target);
	}

	@Override
	protected DebugEventHandler[] createEventHandlers() {
		return new DebugEventHandler[] { new DebugTargetEventHandler(this), new PDAThreadEventHandler(this) };
	}

}
