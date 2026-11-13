/*******************************************************************************
 * Copyright (c) 2026 Sergii Zolotarov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sergii Zolotarov - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.LaunchAsChoices.Choice;
import org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Shows the "Run As" (or "Debug As") choices for the current editor or
 * selection in a dialog, and launches the selected one. The dialog selects the
 * last choice first. The launch mode comes from the handler definition, for
 * example
 * <code>defaultHandler="org.eclipse.debug.internal.ui.actions.LaunchAsHandler:debug"</code>.
 */
public class LaunchAsHandler extends AbstractHandler implements IExecutableExtension {

	private static final String SETTINGS_SECTION = "LaunchAsDialog"; //$NON-NLS-1$

	private String mode = ILaunchManager.RUN_MODE;

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
		if (data instanceof String text && !text.isBlank()) {
			mode = text.trim();
		}
	}

	@Override
	public Object execute(ExecutionEvent event) {
		Shell shell = HandlerUtil.getActiveShell(event);
		List<Choice> choices = LaunchAsChoices.of(SelectedResourceManager.getDefault().getCurrentSelection(), mode);
		ILaunchMode launchMode = DebugPlugin.getDefault().getLaunchManager().getLaunchMode(mode);
		String title = launchMode != null ? LegacyActionTools.removeMnemonics(launchMode.getLaunchAsLabel()) : mode;
		if (choices.isEmpty()) {
			MessageDialog.openInformation(shell, title, NLS.bind(ActionMessages.LaunchAsHandler_nothing, title));
			return null;
		}
		String button = launchMode != null ? LegacyActionTools.removeMnemonics(launchMode.getLabel()) : mode;
		IDialogSettings settings = DialogSettings.getOrCreateSection(DebugUIPlugin.getDefault().getDialogSettings(),
				SETTINGS_SECTION);
		LaunchAsDialog dialog = new LaunchAsDialog(shell, title, button, choices,
				lastChoice(choices, settings.get(mode)));
		if (dialog.open() == Window.OK && dialog.getResult() != null) {
			settings.put(mode, dialog.getResult().key());
			dialog.getResult().launch().run();
		}
		return null;
	}

	/**
	 * @return the choice with the given key, or <code>null</code> if there is
	 *         none. The dialog then selects the first choice.
	 */
	static Choice lastChoice(List<Choice> choices, String key) {
		if (key == null) {
			return null;
		}
		return choices.stream().filter(choice -> key.equals(choice.key())).findFirst().orElse(null);
	}
}
