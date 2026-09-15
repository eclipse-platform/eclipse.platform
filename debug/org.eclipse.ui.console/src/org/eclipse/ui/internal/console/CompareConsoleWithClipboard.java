/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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
package org.eclipse.ui.internal.console;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.TextConsoleViewer;

public class CompareConsoleWithClipboard extends Action {

	private final TextConsoleViewer console;
	public CompareConsoleWithClipboard(TextConsoleViewer consoleView) {
		super(ConsoleMessages.CompareConsoleWithClipboard_0);
		this.console = consoleView;
		setToolTipText(ConsoleMessages.CompareConsoleWithClipboard_6);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IConsoleHelpContextIds.CONSOLE_COMPARE_ACTION);
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		ISelection selection = console.getSelection();
		if (selection instanceof ITextSelection textSelection) {
			String selectedText = textSelection.getText();
			if (selectedText.isEmpty()) {
				selectedText = console.getDocument().get();
			}
			compareWithClipboard(ConsoleMessages.CompareConsoleWithClipboard_1, selectedText);
		}
	}

	private static void compareWithClipboard(String leftLabel, String content) {
		Object clipboardContents = getClipboard();
		if (!(clipboardContents instanceof String clipboardText)) {
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), ConsoleMessages.CompareConsoleWithClipboard_2,
					ConsoleMessages.CompareConsoleWithClipboard_3);
			return;
		}

		class StringTypedElement implements ITypedElement, IEncodedStreamContentAccessor {
			private final String name;
			private final String text;

			StringTypedElement(String name, String text) {
				this.name = name;
				this.text = text;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Image getImage() {
				return null;
			}

			@Override
			public String getType() {
				return ITypedElement.TEXT_TYPE;
			}

			@Override
			public String getCharset() {
				return StandardCharsets.UTF_8.name();
			}

			@Override
			public InputStream getContents() {
				return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
			}
		}

		CompareConfiguration configuration = new CompareConfiguration();
		configuration.setLeftLabel(leftLabel);
		configuration.setRightLabel(ConsoleMessages.CompareConsoleWithClipboard_4);
		configuration.setLeftEditable(false);
		configuration.setRightEditable(false);

		CompareEditorInput input = new CompareEditorInput(configuration) {
			@Override
			protected Object prepareInput(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				return new DiffNode(new StringTypedElement(leftLabel, content),
						new StringTypedElement(ConsoleMessages.CompareConsoleWithClipboard_4, clipboardText));
			}
		};

		input.setTitle(ConsoleMessages.CompareConsoleWithClipboard_5);
		CompareUI.openCompareEditor(input);
	}

	private static Object getClipboard() {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			return clipboard.getContents(TextTransfer.getInstance());
		} finally {
			clipboard.dispose();
		}
	}

	public static boolean isAvailable() {
		return Platform.getBundle("org.eclipse.compare") != null; //$NON-NLS-1$
	}

	@Override
	public boolean isEnabled() {
		return console.getDocument().getLength() > 0;
	}
}

