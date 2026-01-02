/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ant.tests.ui.debug;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@SuppressWarnings("restriction")
class AntUIDebugTestExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
	private boolean wasAutomatedModeEnabled;
	private boolean wasIgnoreErrorEnabled;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		// set error dialog to non-blocking to avoid hanging the UI during test
		wasAutomatedModeEnabled = ErrorDialog.AUTOMATED_MODE;
		ErrorDialog.AUTOMATED_MODE = true;
		wasIgnoreErrorEnabled = SafeRunnable.getIgnoreErrors();
		SafeRunnable.setIgnoreErrors(true);
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		ErrorDialog.AUTOMATED_MODE = wasAutomatedModeEnabled;
		SafeRunnable.setIgnoreErrors(wasIgnoreErrorEnabled);
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		setPreferences();
		DebugUIPlugin.getStandardDisplay().syncExec(() -> {
			IWorkbench workbench = PlatformUI.getWorkbench();
			IPerspectiveDescriptor descriptor = workbench.getPerspectiveRegistry()
					.findPerspectiveWithId(IDebugUIConstants.ID_DEBUG_PERSPECTIVE);
			workbench.getActiveWorkbenchWindow().getActivePage().setPerspective(descriptor);
		});
	}

	private static void setPreferences() {
		IPreferenceStore debugUIPreferences = DebugUIPlugin.getDefault().getPreferenceStore();
		String property = System.getProperty("debug.workbenchActivation"); //$NON-NLS-1$
		boolean activate = property != null && property.equals("on"); //$NON-NLS-1$
		debugUIPreferences.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, activate);
		debugUIPreferences.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, activate);
		debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW, activate);
		debugUIPreferences.setValue(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH, activate);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		IPreferenceStore debugUIPreferences = DebugUIPlugin.getDefault().getPreferenceStore();
		debugUIPreferences.setToDefault(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR);
		debugUIPreferences.setToDefault(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT);
		debugUIPreferences.setToDefault(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW);
		debugUIPreferences.setToDefault(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH);
	}

}
