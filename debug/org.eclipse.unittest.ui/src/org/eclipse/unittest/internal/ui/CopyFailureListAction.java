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

import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.PlatformUI;

/**
 * Copies the names of the methods that failed and their traces to the
 * clipboard.
 */
public class CopyFailureListAction extends Action {

	private final Clipboard fClipboard;
	private final TestRunnerViewPart fRunner;

	/**
	 * Constructs a copy failure list action object
	 *
	 * @param runner    a test runner view part object
	 * @param clipboard a clipboard object
	 */
	public CopyFailureListAction(TestRunnerViewPart runner, Clipboard clipboard) {
		super(Messages.CopyFailureList_action_label);
		fRunner = runner;
		fClipboard = clipboard;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IUnitTestHelpContextIds.COPYFAILURELIST_ACTION);
	}

	@Override
	public void run() {
		TextTransfer plainTextTransfer = TextTransfer.getInstance();

		try {
			fClipboard.setContents(new String[] { getAllFailureTraces() }, new Transfer[] { plainTextTransfer });
		} catch (SWTError e) {
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			if (MessageDialog.openQuestion(fRunner.getSite().getShell(), Messages.CopyFailureList_problem,
					Messages.CopyFailureList_clipboard_busy))
				run();
		}
	}

	/**
	 * Returns the failure trace lines as a string
	 *
	 * @return a failure traces string
	 */
	public String getAllFailureTraces() {
		StringBuilder buf = new StringBuilder();
		String lineDelim = System.getProperty("line.separator", "\n"); //$NON-NLS-1$//$NON-NLS-2$
		for (TestElement failure : fRunner.getCurrentTestRunSession().getAllFailedTestElements()) {
			buf.append(failure.getTestName()).append(lineDelim);
			FailureTrace failureTrace = failure.getFailureTrace();
			String trace = failureTrace != null ? failureTrace.getTrace() : null;
			if (trace != null) {
				int start = 0;
				while (start < trace.length()) {
					int idx = trace.indexOf('\n', start);
					if (idx != -1) {
						String line = trace.substring(start, idx);
						buf.append(line).append(lineDelim);
						start = idx + 1;
					} else {
						start = Integer.MAX_VALUE;
					}
				}
			}
		}
		return buf.toString();
	}

}
