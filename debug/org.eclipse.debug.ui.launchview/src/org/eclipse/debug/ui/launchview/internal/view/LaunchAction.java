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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.ui.launchview.internal.LaunchViewBundleInfo;
import org.eclipse.debug.ui.launchview.internal.LaunchViewMessages;
import org.eclipse.debug.ui.launchview.services.ILaunchObject;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;

public class LaunchAction {

	private static final Map<String, String> COMMON_MODE_ICONS;

	static {
		COMMON_MODE_ICONS = new TreeMap<>();
		COMMON_MODE_ICONS.put("run", "icons/run_exc.png"); //$NON-NLS-1$//$NON-NLS-2$
		COMMON_MODE_ICONS.put("debug", "icons/debug_exc.png"); //$NON-NLS-1$ //$NON-NLS-2$
		COMMON_MODE_ICONS.put("profile", "icons/profile_exc.png"); //$NON-NLS-1$ //$NON-NLS-2$
		COMMON_MODE_ICONS.put("coverage", "icons/coverage.png"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private final ILaunchMode mode;
	private final LaunchViewImpl view;

	public LaunchAction(ILaunchMode mode, LaunchViewImpl view) {
		this.mode = mode;
		this.view = view;
	}

	public MMenuItem asMMenuItem() {
		MDirectMenuItem item = MMenuFactory.INSTANCE.createDirectMenuItem();
		item.setLabel(mode.getLabel());
		item.setObject(this);

		if (COMMON_MODE_ICONS.containsKey(mode.getIdentifier())) {
			item.setIconURI("platform:/plugin/" + LaunchViewBundleInfo.PLUGIN_ID + "/" + COMMON_MODE_ICONS.get(mode.getIdentifier())); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return item;
	}

	@CanExecute
	public boolean isEnabled() {
		Set<ILaunchObject> elements = view.get();
		return !elements.isEmpty() && elements.stream().allMatch(m -> {
			try {
				return m.getType().getDelegates(Collections.singleton(mode.getIdentifier())).length > 0;
			} catch (CoreException e) {
				ILog.of(this.getClass()).warn(LaunchViewMessages.LaunchAction_FailedFetchLaunchDelegates, e);
				return false;
			}
		});
	}

	@Execute
	public void run() {
		Set<ILaunchObject> objects = view.get();
		for (ILaunchObject m : objects) {
			m.launch(mode);

			if (objects.size() > 1) {
				// PDE has a nasty bug. If launching too fast, it tries to
				// overwrite the last configurations platform.xml
				try {
					Thread.sleep(2_000);
				} catch (InterruptedException ie) {
					// ignored
				}
			}
		}
	}

}
