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
package org.eclipse.ui.internal.console.ansi.participants;

import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_CONCEAL_OFF;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_CONCEAL_ON;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_CROSSOUT_OFF;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_CROSSOUT_ON;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_FRAMED_OFF;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_FRAMED_ON;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_INTENSITY_BRIGHT;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_INTENSITY_FAINT;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_INTENSITY_NORMAL;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_ITALIC;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_ITALIC_OFF;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_NEGATIVE_OFF;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_NEGATIVE_ON;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_RESET;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_UNDERLINE;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_UNDERLINE_DOUBLE;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_ATTR_UNDERLINE_OFF;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_COLOR_BACKGROUND_FIRST;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_COLOR_BACKGROUND_LAST;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_COLOR_BACKGROUND_RESET;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_COLOR_FOREGROUND_FIRST;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_COLOR_FOREGROUND_LAST;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_COLOR_FOREGROUND_RESET;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_COLOR_INTENSITY_DELTA;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_HICOLOR_BACKGROUND;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_HICOLOR_BACKGROUND_FIRST;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_HICOLOR_BACKGROUND_LAST;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_HICOLOR_FOREGROUND;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_HICOLOR_FOREGROUND_FIRST;
import static org.eclipse.ui.internal.console.ansi.utils.AnsiCommands.COMMAND_HICOLOR_FOREGROUND_LAST;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Position;
import org.eclipse.swt.SWT;
import org.eclipse.ui.internal.console.ansi.AnsiConsoleUtils;
import org.eclipse.ui.internal.console.ansi.utils.AnsiConsoleAttributes;
import org.eclipse.ui.internal.console.ansi.utils.AnsiConsoleColorPalette;

public class AnsiPosition extends Position {
	public static final String POSITION_NAME = "ansi_color"; //$NON-NLS-1$

	private static final AnsiConsoleAttributes current = new AnsiConsoleAttributes();

	public final AnsiConsoleAttributes attributes;
	public final String text;

	public AnsiPosition(int offset, String text) {
		super(offset, text == null ? 0 : text.length());
		this.text = text == null ? "" : text; //$NON-NLS-1$
		this.attributes = updateAttributes();
	}

	@Override
	public String toString() {
		return String.format("AnsiPosition:{ offset:%d length:%d text:\"%s\" attr:\"%s\" }", offset, length, text, attributes); //$NON-NLS-1$
	}

	// Takes a string that looks like this: int [ ';' int] and returns a list of the integers
	private static List<Integer> parseSemicolonSeparatedIntList(String text) {
		final List<Integer> result = new ArrayList<>(10);
		int crtValue = 0;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch >= '0' && ch <= '9') {
				crtValue *= 10;
				crtValue += ch - '0';
			} else {
				result.add(crtValue);
				crtValue = 0;
			}
		}
		result.add(crtValue);
		return result;
	}

	private AnsiConsoleAttributes updateAttributes() {
		char code = text.charAt(text.length() - 1);
		if (code == AnsiConsoleUtils.ESCAPE_SGR) {
			String theEscape = text.substring(2, text.length() - 1);
			// Select Graphic Rendition (SGR) escape sequence
			interpretCommand(parseSemicolonSeparatedIntList(theEscape));
			return AnsiConsoleAttributes.from(current);
		}
		return null;
	}

	private static void interpretCommand(List<Integer> nCommands) {

		Iterator<Integer> iter = nCommands.iterator();
		while (iter.hasNext()) {
			int nCmd = iter.next();
			switch (nCmd) {
				case COMMAND_ATTR_RESET:             current.reset(); break;

				case COMMAND_ATTR_INTENSITY_BRIGHT:  current.bold = true; break;
				case COMMAND_ATTR_INTENSITY_FAINT: // Intentional fallthrough
				case COMMAND_ATTR_INTENSITY_NORMAL:  current.bold = false; break;

				case COMMAND_ATTR_ITALIC:            current.italic = true; break;
				case COMMAND_ATTR_ITALIC_OFF:        current.italic = false; break;

				case COMMAND_ATTR_UNDERLINE:         current.underline = SWT.UNDERLINE_SINGLE; break;
				case COMMAND_ATTR_UNDERLINE_DOUBLE:  current.underline = SWT.UNDERLINE_DOUBLE; break;
				case COMMAND_ATTR_UNDERLINE_OFF:     current.underline = AnsiConsoleAttributes.UNDERLINE_NONE; break;

				case COMMAND_ATTR_CROSSOUT_ON:       current.strike = true; break;
				case COMMAND_ATTR_CROSSOUT_OFF:      current.strike = false; break;

				case COMMAND_ATTR_NEGATIVE_ON:       current.invert = true; break;
				case COMMAND_ATTR_NEGATIVE_OFF:      current.invert = false; break;

				case COMMAND_ATTR_CONCEAL_ON:        current.conceal = true; break;
				case COMMAND_ATTR_CONCEAL_OFF:       current.conceal = false; break;

				case COMMAND_ATTR_FRAMED_ON:         current.framed = true; break;
				case COMMAND_ATTR_FRAMED_OFF:        current.framed = false; break;

				case COMMAND_COLOR_FOREGROUND_RESET: current.currentFgColor = null; break;
				case COMMAND_COLOR_BACKGROUND_RESET: current.currentBgColor = null; break;

				case COMMAND_HICOLOR_FOREGROUND:
				case COMMAND_HICOLOR_BACKGROUND: // {esc}[48;5;{color}m
					int color = -1;
					int nMustBe2or5 = iter.hasNext() ? iter.next() : -1;
					if (nMustBe2or5 == 5) { // 256 colors
						color = iter.hasNext() ? iter.next() : -1;
						if (!AnsiConsoleColorPalette.isValidIndex(color)) {
							color = -1;
						}
					} else if (nMustBe2or5 == 2) { // rgb colors
						int r = iter.hasNext() ? iter.next() : -1;
						int g = iter.hasNext() ? iter.next() : -1;
						int b = iter.hasNext() ? iter.next() : -1;
						color = AnsiConsoleColorPalette.hackRgb(r, g, b);
					}
					if (color != -1) {
						if (nCmd == COMMAND_HICOLOR_FOREGROUND) {
							current.currentFgColor = color;
						} else {
							current.currentBgColor = color;
						}
					}
					break;

				case -1: break; // do nothing

				default:
					if (nCmd >= COMMAND_COLOR_FOREGROUND_FIRST && nCmd <= COMMAND_COLOR_FOREGROUND_LAST) { // text color
						current.currentFgColor = nCmd - COMMAND_COLOR_FOREGROUND_FIRST;
					} else if (nCmd >= COMMAND_COLOR_BACKGROUND_FIRST && nCmd <= COMMAND_COLOR_BACKGROUND_LAST) { // background color
						current.currentBgColor = nCmd - COMMAND_COLOR_BACKGROUND_FIRST;
					} else if (nCmd >= COMMAND_HICOLOR_FOREGROUND_FIRST && nCmd <= COMMAND_HICOLOR_FOREGROUND_LAST) { // text color
						current.currentFgColor = nCmd - COMMAND_HICOLOR_FOREGROUND_FIRST + COMMAND_COLOR_INTENSITY_DELTA;
					} else if (nCmd >= COMMAND_HICOLOR_BACKGROUND_FIRST && nCmd <= COMMAND_HICOLOR_BACKGROUND_LAST) { // background color
						current.currentBgColor = nCmd - COMMAND_HICOLOR_BACKGROUND_FIRST + COMMAND_COLOR_INTENSITY_DELTA;
					}
			}
		}
	}

}
