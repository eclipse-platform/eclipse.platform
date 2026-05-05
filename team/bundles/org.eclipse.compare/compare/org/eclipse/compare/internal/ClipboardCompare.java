/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;


public class ClipboardCompare extends BaseCompareAction {

	@Override
	protected void run(ISelection selection) {
		IFile[] files = Utilities.getFiles(selection);
		Shell parentShell = CompareUIPlugin.getShell();
		for (IFile file : files) {
			try {
				ClipboardCompareProcess pro = new ClipboardCompareProcess();
				pro.processComparison(parentShell, file);
			} catch (Exception e) {
				MessageDialog.openError(parentShell, "Comparison Failed", e.getMessage()); //$NON-NLS-1$
			}
		}
	}

	@Override
	protected boolean isEnabled(ISelection selection) {
		return Utilities.getFiles(selection).length == 1 && ClipboardCompareProcess.getClipboard() != null;
	}
}