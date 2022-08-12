/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.team.core.RepositoryProvider;

public class RepositoryProviderManager implements IRepositoryProviderListener {

	private static RepositoryProviderManager instance;
	private ListenerList<IRepositoryProviderListener> listeners = new ListenerList<>();

	public static synchronized RepositoryProviderManager getInstance() {
		if (instance == null) {
			instance = new RepositoryProviderManager();
		}
		return instance;
	}

	@Override
	public void providerMapped(RepositoryProvider provider) {
		Object[] allListeners = listeners.getListeners();
		for (Object l : allListeners) {
			IRepositoryProviderListener listener = (IRepositoryProviderListener) l;
			listener.providerMapped(provider);
		}
	}

	@Override
	public void providerUnmapped(IProject project) {
		Object[] allListeners = listeners.getListeners();
		for (Object l : allListeners) {
			IRepositoryProviderListener listener = (IRepositoryProviderListener) l;
			listener.providerUnmapped(project);
		}
	}

	public void addListener(IRepositoryProviderListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IRepositoryProviderListener listener) {
		listeners.remove(listener);
	}


}
