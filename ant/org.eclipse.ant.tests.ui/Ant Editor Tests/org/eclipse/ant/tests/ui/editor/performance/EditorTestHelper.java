/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.tests.ui.editor.performance;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * @since 3.1
 */
public class EditorTestHelper {

	public static IEditorPart openInEditor(IFile file, boolean runEventLoop) throws PartInitException {
		IEditorPart part = IDE.openEditor(getActivePage(), file);
		if (runEventLoop) {
			runEventQueue(part);
		}
		return part;
	}

	public static IEditorPart openInEditor(IFile file, String editorId, boolean runEventLoop) throws PartInitException {
		IEditorPart part = IDE.openEditor(getActivePage(), file, editorId);
		if (runEventLoop) {
			runEventQueue(part);
		}
		return part;
	}

	public static void closeAllEditors() {
		IWorkbenchPage page = getActivePage();
		if (page != null) {
			page.closeAllEditors(false);
		}
	}

	public static void runEventQueue() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			runEventQueue(window.getShell());
		}
	}

	public static void runEventQueue(IWorkbenchPart part) {
		runEventQueue(part.getSite().getShell());
	}

	public static void runEventQueue(Shell shell) {
		while (shell.getDisplay().readAndDispatch()) {
			// XXX do nothing, just spin the loop
		}
	}

	public static void runEventQueue(long minTime) {
		long nextCheck = System.currentTimeMillis() + minTime;
		while (System.currentTimeMillis() < nextCheck) {
			runEventQueue();
			sleep(1);
		}
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		return window != null ? window.getActivePage() : null;
	}

	public static void sleep(int intervalTime) {
		try {
			Thread.sleep(intervalTime);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
