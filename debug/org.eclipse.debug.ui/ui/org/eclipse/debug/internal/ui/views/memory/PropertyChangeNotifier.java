/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Fire properties changes events in ISafeRunnable to ensure that exceptions are
 * caught and handled.
 *
 * @since 3.1
 */
public class PropertyChangeNotifier implements ISafeRunnable {

	IPropertyChangeListener fListener;
	PropertyChangeEvent fEvt;

	public PropertyChangeNotifier(IPropertyChangeListener listener, PropertyChangeEvent evt) {
		fListener = listener;
		fEvt = evt;
	}

	@Override
	public void handleException(Throwable exception) {
		DebugUIPlugin.log(exception);
	}

	@Override
	public void run() throws Exception {
		fListener.propertyChange(fEvt);
	}
}
