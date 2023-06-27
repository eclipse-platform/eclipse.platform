/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.doc.internal.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.internal.validation.TocValidator;
import org.eclipse.help.internal.validation.TocValidator.BrokenLink;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ua.tests.doc.internal.dialogs.SelectTocDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class CheckTocAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	public static List<BrokenLink> errors = new ArrayList<>();

	public static void showErrors() {
		if (errors.isEmpty()) {
			reportStatus("No errors detected in load");
		}
		for (int i = 0; i < errors.size(); i++) {
			BrokenLink link = errors.get(i);
			reportStatus("Invalid link in \"" + link.getTocID() + "\": " + link.getHref());
		}
	}

	private static void reportStatus(String errorMessage) {
		ILog.of(CheckTocAction.class).warn(errorMessage);
	}


	/**
	 * The constructor.
	 */
	public CheckTocAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run(IAction action) {
		SelectTocDialog dlg = new SelectTocDialog(window.getShell());
		dlg.open();
		if (dlg.getReturnCode() == Window.CANCEL) {
			return;
		}
		Toc[] tocsToCheck = dlg.getTocsToCheck();
		checkTocFilesExist(tocsToCheck);

	}

	public  void checkTocFilesExist(Toc[] tocsToCheck) {
		for (Toc toc : tocsToCheck) {
			String id = toc.getTocContribution().getId();
			reportStatus("Testing " + id);
			String[] href = { id };
			try {
				errors = TocValidator.validate(href);
			} catch (Exception e) {
				e.printStackTrace();
			}
			showErrors();
		}
	}



	/**
	 * Selection in the workbench has been changed. We
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	@Override
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}