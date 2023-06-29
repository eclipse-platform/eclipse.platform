/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.unittest.model.ITestElement;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Copies a test failure stack trace to the clipboard.
 */
public class UnitTestCopyAction extends SelectionListenerAction {
	private FailureTraceUIBlock fView;

	private final Clipboard fClipboard;

	private ITestElement fTestElement;

	/**
	 * Constructs a Unit Test Copy action
	 *
	 * @param view      a {@link FailureTraceUIBlock} object
	 * @param clipboard a {@link Clipboard} object
	 */
	public UnitTestCopyAction(FailureTraceUIBlock view, Clipboard clipboard) {
		super(Messages.CopyTrace_action_label);
		Assert.isNotNull(clipboard);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IUnitTestHelpContextIds.COPYTRACE_ACTION);
		fView = view;
		fClipboard = clipboard;
	}

	@Override
	public void run() {
		String trace = fView.getTrace();
		String source = null;
		if (trace != null) {
			source = convertLineTerminators(trace);
		} else if (fTestElement != null) {
			source = fTestElement.getTestName();
		}
		if (source == null || source.length() == 0)
			return;

		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		try {
			fClipboard.setContents(new String[] { convertLineTerminators(source) },
					new Transfer[] { plainTextTransfer });
		} catch (SWTError e) {
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			if (MessageDialog.openQuestion(fView.getComposite().getShell(), Messages.CopyTraceAction_problem,
					Messages.CopyTraceAction_clipboard_busy))
				run();
		}
	}

	/**
	 * Handles a test selection
	 *
	 * @param test an {@link ITestElement} object
	 */
	public void handleTestSelected(ITestElement test) {
		fTestElement = test;
	}

	private String convertLineTerminators(String in) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		StringReader stringReader = new StringReader(in);
		BufferedReader bufferedReader = new BufferedReader(stringReader);
		String line;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				printWriter.println(line);
			}
		} catch (IOException e) {
			return in; // return the trace unfiltered
		}
		return stringWriter.toString();
	}
}
