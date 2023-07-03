/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.ILiveHelpAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class ShowCapabilitiesPreferenceAction implements ILiveHelpAction {
	private boolean narrow;

	@Override
	public void setInitializationString(String data) {
		if (data!=null && data.equals("narrow")) //$NON-NLS-1$
			narrow=true;
	}

	@Override
	public void run() {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			Shell windowShell = null;
			if (!narrow) {
				Shell[] shells = display.getShells();
				for (int i = 0; i < shells.length; i++) {
					Object data = shells[i].getData();
					if (data != null && data instanceof IWorkbenchWindow) {
						windowShell = shells[i];
						break;
					}
				}
			}
			if (windowShell != null) {
				windowShell.forceActive();
				if (Platform.getWS().equals(Platform.WS_WIN32)) {
					// feature in Windows. Without this code,
					// the window will only flash in the launch bar.
					windowShell.setVisible(false);
					windowShell.setMinimized(true);
					windowShell.setVisible(true);
					windowShell.setMinimized(false);
				}
			}
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(windowShell,
					getCapabilityPageId(), null, null);
			dialog.open();
		});
	}

	private String getCapabilityPageId() {
		return "org.eclipse.sdk.capabilities"; //$NON-NLS-1$
	}
}