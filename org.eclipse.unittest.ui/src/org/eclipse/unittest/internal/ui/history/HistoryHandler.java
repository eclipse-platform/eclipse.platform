/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.internal.ui.history;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.ui.TestRunnerViewPart;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * A handler for Show History command
 */
public class HistoryHandler extends AbstractHandler {

	/**
	 * An identifier of Show History command
	 */
	public static final String COMMAND_ID = UnitTestPlugin.PLUGIN_ID + ".history"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		IWorkbenchPage page = HandlerUtil.getActivePart(event).getSite().getPage();
		Set<TestRunSession> visibleSessions = Arrays.stream(page.getViewReferences()) //
				.map(ref -> ref.getPart(false)) //
				.filter(TestRunnerViewPart.class::isInstance) //
				.map(TestRunnerViewPart.class::cast) //
				.map(TestRunnerViewPart::getCurrentTestRunSession) //
				.filter(Objects::nonNull) //
				.collect(Collectors.toSet());
		SelectionDialog historyDialog = new HistoryDialog(shell, visibleSessions);
		historyDialog.setBlockOnOpen(true);
		if (historyDialog.open() == IDialogConstants.OK_ID) {
			HistoryItem item = (HistoryItem) historyDialog.getResult()[0];
			try {
				TestRunnerViewPart part = findCurrentPartOrOpenNew(HandlerUtil.getActivePart(event));
				part.setActiveTestRunSession(item.reloadTestRunSession());
			} catch (CoreException e) {
				UnitTestPlugin.log(e);
			}
		}
		return null;
	}

	private TestRunnerViewPart findCurrentPartOrOpenNew(IWorkbenchPart part) throws PartInitException {
		if (part instanceof TestRunnerViewPart) {
			return (TestRunnerViewPart) part;
		}
		return (TestRunnerViewPart) part.getSite().getPage().showView(TestRunnerViewPart.NAME);
	}

}
