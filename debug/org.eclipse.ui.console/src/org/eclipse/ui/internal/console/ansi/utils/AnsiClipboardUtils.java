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
package org.eclipse.ui.internal.console.ansi.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.console.ansi.AnsiConsoleUtils;
import org.eclipse.ui.internal.console.ansi.preferences.AnsiConsolePreferenceUtils;

public class AnsiClipboardUtils {

	public static void textToClipboard(StyledText styledText, boolean removeEscapeSeq) {
		Clipboard clipboard = new Clipboard(Display.getDefault());

		clipboard.clearContents();
		styledText.copy(); // copy to clipboard using the default Eclipse behavior

		// If we don't want to remove the escape sequences the default copy is enough.
		if (!removeEscapeSeq) {
			clipboard.dispose();
			return;
		}

		List<Object> clipboardData = new ArrayList<>(3);
		List<Transfer> clipboardTransfers = new ArrayList<>(3);

		TextTransfer textTransfer = TextTransfer.getInstance();
		Object textData = clipboard.getContents(textTransfer);
		if (textData != null && textData instanceof String) {
			String plainText = AnsiConsoleUtils.ESCAPE_SEQUENCE_REGEX_TXT
					.matcher((String) textData)
					.replaceAll(""); //$NON-NLS-1$
			clipboardData.add(plainText);
			clipboardTransfers.add(textTransfer);
		}

		if (AnsiConsolePreferenceUtils.putRtfInClipboard()) {
			RTFTransfer rtfTransfer = RTFTransfer.getInstance();
			Object rtfData = clipboard.getContents(rtfTransfer);
			if (rtfData != null && rtfData instanceof String) {
				String rtfText = AnsiConsoleUtils.ESCAPE_SEQUENCE_REGEX_RTF
						.matcher((String) rtfData)
						.replaceAll(""); //$NON-NLS-1$
				// The Win version of MS Word, and Write, understand \chshdng and \chcbpat, but not \cb
				// The MacOS tools seem to understand \cb, but not \chshdng and \chcbpat
				// But using both seems to work fine, both systems just ignore the tags they don't understand.
				rtfText = AnsiConsoleUtils.ESCAPE_SEQUENCE_REGEX_RTF_FIX_SRC
						.matcher(rtfText)
						.replaceAll(AnsiConsoleUtils.ESCAPE_SEQUENCE_REGEX_RTF_FIX_TRG);
				clipboardData.add(rtfText);
				clipboardTransfers.add(rtfTransfer);
			}

			HTMLTransfer htmlTransfer = HTMLTransfer.getInstance();
			Object htmlData = clipboard.getContents(htmlTransfer);
			if (htmlData != null && htmlData instanceof String) {
				String htmlText = AnsiConsoleUtils.ESCAPE_SEQUENCE_REGEX_HTML
						.matcher((String) htmlData)
						.replaceAll(""); //$NON-NLS-1$
				clipboardData.add(htmlText);
				clipboardTransfers.add(htmlTransfer);
			}
		}

		clipboard.setContents(clipboardData.toArray(), clipboardTransfers.toArray(new Transfer[0]));

		clipboard.dispose();
	}
}
