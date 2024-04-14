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
package org.eclipse.debug.ui.launchview.internal.view;

import java.util.Set;

import org.eclipse.debug.ui.launchview.internal.LaunchViewBundleInfo;
import org.eclipse.debug.ui.launchview.internal.LaunchViewMessages;
import org.eclipse.debug.ui.launchview.services.ILaunchObject;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;

public class RelaunchAction {

	private final LaunchViewImpl view;

	public RelaunchAction(LaunchViewImpl view) {
		this.view = view;
	}

	public MMenuItem asMMenuItem() {
		MDirectMenuItem item = MMenuFactory.INSTANCE.createDirectMenuItem();
		item.setLabel(LaunchViewMessages.RelaunchAction_TerminateRelaunch);
		item.setEnabled(isEnabled());
		item.setObject(this);

		item.setIconURI("platform:/plugin/" + LaunchViewBundleInfo.PLUGIN_ID + "/icons/term_restart.png"); //$NON-NLS-1$ //$NON-NLS-2$

		return item;
	}

	@CanExecute
	public boolean isEnabled() {
		Set<ILaunchObject> elements = view.get();
		return !elements.isEmpty() && elements.stream().allMatch(ILaunchObject::canTerminate);
	}

	@Execute
	public void run() {
		view.get().stream().forEach(ILaunchObject::relaunch);
	}

}
