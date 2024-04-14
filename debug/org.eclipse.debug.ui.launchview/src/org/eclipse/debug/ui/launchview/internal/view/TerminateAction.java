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

public class TerminateAction {

	private final LaunchViewImpl view;

	public TerminateAction(LaunchViewImpl view) {
		this.view = view;
	}

	public MMenuItem asMMenuItem() {
		MDirectMenuItem item = MMenuFactory.INSTANCE.createDirectMenuItem();
		item.setLabel(LaunchViewMessages.TerminateAction_Terminate);
		item.setEnabled(isEnabled());
		item.setObject(this);

		item.setIconURI("platform:/plugin/" + LaunchViewBundleInfo.PLUGIN_ID + "/icons/terminate_co.png"); //$NON-NLS-1$ //$NON-NLS-2$

		return item;
	}

	@CanExecute
	public boolean isEnabled() {
		Set<ILaunchObject> elements = view.get();
		return !elements.isEmpty() && elements.stream().allMatch(ILaunchObject::canTerminate);
	}

	@Execute
	public void run() {
		view.get().forEach(ILaunchObject::terminate);
	}

}
