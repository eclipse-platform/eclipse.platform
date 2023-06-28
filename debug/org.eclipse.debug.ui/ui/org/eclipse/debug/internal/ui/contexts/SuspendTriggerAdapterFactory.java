/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.contexts;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.ui.contexts.ISuspendTrigger;

/**
 * @since 3.2
 */
public class SuspendTriggerAdapterFactory implements IAdapterFactory {

	private Map<Object, LaunchSuspendTrigger> fSuspendTriggers = new HashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType.equals(ISuspendTrigger.class)) {
			if (adaptableObject instanceof ILaunch) {
				LaunchSuspendTrigger trigger = fSuspendTriggers.get(adaptableObject);
				if (trigger == null) {
					trigger = new LaunchSuspendTrigger((ILaunch) adaptableObject, this);
					fSuspendTriggers.put(adaptableObject, trigger);
				}
				return (T) trigger;
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[]{ISuspendTrigger.class};
	}

	public synchronized void dispose(LaunchSuspendTrigger trigger) {
		fSuspendTriggers.remove(trigger.getLaunch());
	}

}
