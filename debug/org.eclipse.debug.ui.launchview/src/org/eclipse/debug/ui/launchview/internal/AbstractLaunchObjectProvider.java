/*******************************************************************************
 * Copyright (c) 2017, 2019 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.ui.launchview.services.ILaunchObjectProvider;

/**
 * Base class for {@link ILaunchObjectProvider} implementations which require
 * listeners to be notified on updates.
 */
public abstract class AbstractLaunchObjectProvider implements ILaunchObjectProvider {

	private final List<Runnable> updateListeners = new ArrayList<>();

	@Override
	public void addUpdateListener(Runnable r) {
		updateListeners.add(r);
	}

	@Override
	public void removeUpdateListener(Runnable r) {
		updateListeners.remove(r);
	}

	protected void fireUpdate() {
		// prevent multiple updates in short row somehow?
		updateListeners.forEach(Runnable::run);
	}

}
