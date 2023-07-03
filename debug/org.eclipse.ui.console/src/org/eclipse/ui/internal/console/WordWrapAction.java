/*******************************************************************************
 * Copyright (c) 2014, 2020 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gaetano Santoro - initial implementation
 *     Matthias Mailänder - rebase onto Mars
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 287303
 *     Christian Gabrisch <eclipse@cgabrisch.de> - Bug 491853
 *******************************************************************************/

package org.eclipse.ui.internal.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;

/**
 * Activates line breaks in the Console view so that the full log statement is
 * always visible
 */
public class WordWrapAction extends Action implements IPropertyChangeListener {

	private IConsoleView fConsoleView = null;

	public WordWrapAction(IConsoleView consoleView) {
		super(ConsoleMessages.WordWrapAction_0);
		fConsoleView = consoleView;

		setToolTipText(ConsoleMessages.WordWrapAction_1);
		setHoverImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_LCL_WRAP));
		setDisabledImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_DLCL_WRAP));
		setImageDescriptor(ConsolePluginImages.getImageDescriptor(IInternalConsoleConstants.IMG_ELCL_WRAP));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IConsoleHelpContextIds.CONSOLE_WORD_WRAP_ACTION);

		getPreferenceStore().addPropertyChangeListener(this);
		applyPreferences();
	}

	@Override
	public void run() {
		boolean enableWordWrap = isChecked();
		getPreferenceStore().setValue(IConsoleConstants.P_CONSOLE_WORD_WRAP, enableWordWrap);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (IConsoleConstants.P_CONSOLE_WORD_WRAP.equals(property)) {
			applyPreferences();
		}
	}

	public void dispose() {
		getPreferenceStore().removePropertyChangeListener(this);
		fConsoleView = null;
	}

	private void applyPreferences() {
		boolean enableWordWrap = getPreferenceStore().getBoolean(IConsoleConstants.P_CONSOLE_WORD_WRAP);
		setChecked(enableWordWrap);
		fConsoleView.setWordWrap(enableWordWrap);
	}

	private IPreferenceStore getPreferenceStore() {
		return ConsolePlugin.getDefault().getPreferenceStore();
	}
}
