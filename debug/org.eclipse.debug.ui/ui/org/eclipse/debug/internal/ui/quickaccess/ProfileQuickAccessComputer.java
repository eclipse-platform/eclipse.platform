/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.quickaccess;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;

public class ProfileQuickAccessComputer extends AbstractLaunchQuickAccessComputer {

	public ProfileQuickAccessComputer() {
		super(DebugPlugin.getDefault().getLaunchManager().getLaunchMode(ILaunchManager.PROFILE_MODE));
	}
}
