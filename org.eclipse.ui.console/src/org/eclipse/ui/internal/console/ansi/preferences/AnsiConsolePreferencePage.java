/*******************************************************************************
 * Copyright (c) 2012-2022 Mihai Nita and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.console.ansi.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.console.ansi.AnsiMessages;
import org.eclipse.ui.internal.console.ansi.commands.EnableDisableHandler;
import org.eclipse.ui.internal.console.ansi.utils.AnsiConsoleColorPalette;

public class AnsiConsolePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public AnsiConsolePreferencePage() {
		super(GRID);
		setPreferenceStore(ConsolePlugin.getDefault().getPreferenceStore());
		setDescription(AnsiMessages.PreferencePage_Title);
	}

	@Override
	public void createFieldEditors() {
		final Composite parent = getFieldEditorParent();

		addField(new BooleanFieldEditor(AnsiConsolePreferenceConstants.PREF_ANSI_CONSOLE_ENABLED,
				AnsiMessages.PreferencePage_PluginEnabled, parent));

		addField(new BooleanFieldEditor(AnsiConsolePreferenceConstants.PREF_WINDOWS_MAPPING,
				AnsiMessages.PreferencePage_Option_WindowsMapping, parent));

		addField(new BooleanFieldEditor(AnsiConsolePreferenceConstants.PREF_SHOW_ESCAPES,
				AnsiMessages.PreferencePage_Option_ShowEscapes, parent));

		addField(new BooleanFieldEditor(AnsiConsolePreferenceConstants.PREF_KEEP_STDERR_COLOR,
				AnsiMessages.PreferencePage_Option_HonorStderr, parent));

		addField(new BooleanFieldEditor(AnsiConsolePreferenceConstants.PREF_PUT_RTF_IN_CLIPBOARD,
				AnsiMessages.PreferencePage_Option_RtfInClipboard, parent));

		addField(new ComboFieldEditor(AnsiConsolePreferenceConstants.PREF_COLOR_PALETTE,
						AnsiMessages.PreferencePage_Option_ColorPaletteSectionTitle,
				new String[][] {
						{ AnsiMessages.PreferencePage_Option_ColorPaletteVGA, AnsiConsoleColorPalette.PALETTE_VGA },
						{ AnsiMessages.PreferencePage_Option_ColorPaletteWinXP, AnsiConsoleColorPalette.PALETTE_WINXP },
						{ AnsiMessages.PreferencePage_Option_ColorPaletteWin10, AnsiConsoleColorPalette.PALETTE_WIN10 },
						{ AnsiMessages.PreferencePage_Option_ColorPaletteMacOS, AnsiConsoleColorPalette.PALETTE_MAC },
						{ AnsiMessages.PreferencePage_Option_ColorPalettePutty, AnsiConsoleColorPalette.PALETTE_PUTTY },
						{ AnsiMessages.PreferencePage_Option_ColorPaletteXterm, AnsiConsoleColorPalette.PALETTE_XTERM },
						{ AnsiMessages.PreferencePage_Option_ColorPaletteMirc, AnsiConsoleColorPalette.PALETTE_MIRC },
						{ AnsiMessages.PreferencePage_Option_ColorPaletteUbuntu, AnsiConsoleColorPalette.PALETTE_UBUNTU }
				},
				parent));
	}

	@Override
	public void init(IWorkbench workbench) {
		// Nothing to do, but we are forced to implement it for IWorkbenchPreferencePage
	}

	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
		try {
			handlerService.executeCommand(EnableDisableHandler.COMMAND_ID, new Event());
		} catch (@SuppressWarnings("unused") Exception ex) {
			System.out.println("AnsiConsole: Command '" + EnableDisableHandler.COMMAND_ID + "' not found"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
}
