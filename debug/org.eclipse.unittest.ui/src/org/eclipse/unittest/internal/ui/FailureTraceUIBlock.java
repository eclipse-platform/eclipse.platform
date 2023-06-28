/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.unittest.internal.UnitTestPreferencesConstants;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.ui.ITestViewSupport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.core.text.StringMatcher;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.OpenStrategy;

/**
 * A pane that shows a stack trace of a failed test.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class FailureTraceUIBlock implements IMenuListener {

	private static final int MAX_LABEL_LENGTH = 256;
	private Table fTable;
	private TestRunnerViewPart fTestRunner;
	private String fInputTrace;
	private final Clipboard fClipboard;
	private TestElement fFailure;
	private CompareResultsAction fCompareAction;
	private final FailureTableDisplay fFailureTableDisplay;
	private ShowStackTraceInConsoleViewAction fShowTraceInConsoleAction;

	/**
	 * Constructs a {@link FailureTraceUIBlock} object
	 *
	 * @param parent     a parent composite
	 * @param clipboard  a {@link Clipboard} instance
	 * @param testRunner a Test Runner view part
	 * @param toolBar    a {@link ToolBar} instance
	 */
	public FailureTraceUIBlock(Composite parent, Clipboard clipboard, TestRunnerViewPart testRunner, ToolBar toolBar) {
		Assert.isNotNull(clipboard);

		// fill the failure trace viewer toolbar
		ToolBarManager failureToolBarmanager = new ToolBarManager(toolBar);
		fShowTraceInConsoleAction = new ShowStackTraceInConsoleViewAction();
		fShowTraceInConsoleAction.setDelegate(null);
		fShowTraceInConsoleAction.setEnabled(false);
		failureToolBarmanager.add(fShowTraceInConsoleAction);
		failureToolBarmanager.add(new EnableStackFilterAction(this));
		fCompareAction = new CompareResultsAction(this);
		fCompareAction.setEnabled(false);
		failureToolBarmanager.add(fCompareAction);
		failureToolBarmanager.update(true);
		fTable = new Table(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		fTestRunner = testRunner;
		fClipboard = clipboard;

		OpenStrategy handler = new OpenStrategy(fTable);
		handler.addOpenListener(e -> {
			if (fTable.getSelectionIndex() == 0 && fFailure.getFailureTrace() != null
					&& fFailure.getFailureTrace().isComparisonFailure()) {
				fCompareAction.run();
			}
			if (fTable.getSelection().length != 0) {
				IAction a = createOpenEditorAction(getSelectedText());
				if (a != null)
					a.run();
			}
		});

		initMenu();

		fFailureTableDisplay = new FailureTableDisplay(fTable);
	}

	private void initMenu() {
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this);
		Menu menu = menuMgr.createContextMenu(fTable);
		fTable.setMenu(menu);
	}

	@Override
	public void menuAboutToShow(IMenuManager manager) {
		if (fTable.getSelectionCount() > 0) {
			IAction a = createOpenEditorAction(getSelectedText());
			if (a != null)
				manager.add(a);
			manager.add(new UnitTestCopyAction(FailureTraceUIBlock.this, fClipboard));
		}
		// fix for bug 68058
		if (fFailure != null && fFailure.getFailureTrace() != null
				&& fFailure.getFailureTrace().isComparisonFailure()) {
			manager.add(fCompareAction);
		}
	}

	/**
	 * Returns the current trace
	 *
	 * @return a current trace or <code>null</code>
	 */
	public String getTrace() {
		return fInputTrace;
	}

	private String getSelectedText() {
		return fTable.getSelection()[0].getText();
	}

	private IAction createOpenEditorAction(String traceLine) {
		return fFailure.getTestRunSession().getTestViewSupport()
				.createOpenEditorAction(fTestRunner.getSite().getShell(), fFailure, traceLine);
	}

	/**
	 * Returns the composite used to present the trace
	 *
	 * @return The composite
	 */
	public Composite getComposite() {
		return fTable;
	}

	/**
	 * Refresh the table from the trace.
	 */
	public void refresh() {
		updateTable(fInputTrace);
	}

	/**
	 * Shows a TestFailure
	 *
	 * @param test the failed test
	 */
	public void showFailure(TestElement test) {
		fFailure = test;
		String trace = ""; //$NON-NLS-1$
		updateActions(test);
		updateEnablement(test);
		if (test != null && test.getFailureTrace() != null) {
			trace = test.getFailureTrace().getTrace();
		}
		if (Objects.equals(fInputTrace, trace)) {
			return;
		}
		fInputTrace = trace;
		updateTable(trace);
	}

	private void updateActions(TestElement test) {
		ITestViewSupport testViewSupport = test != null ? test.getTestRunSession().getTestViewSupport() : null;
		fShowTraceInConsoleAction.setDelegate(testViewSupport != null && test.getFailureTrace() != null
				? testViewSupport.createShowStackTraceInConsoleViewActionDelegate(test)
				: null);
	}

	private void updateEnablement(TestElement test) {
		boolean enableCompare = test != null && test.getFailureTrace() != null
				&& test.getFailureTrace().isComparisonFailure();
		fCompareAction.setEnabled(enableCompare);
		if (enableCompare) {
			fCompareAction.updateOpenDialog(test);
		}

		boolean enableShowTraceInConsole = test != null && test.getFailureTrace() != null;
		fShowTraceInConsoleAction.setEnabled(enableShowTraceInConsole);
	}

	private void updateTable(String trace) {
		if (trace == null || trace.trim().isEmpty()) {
			clear();
			return;
		}
		trace = trace.trim();
		fTable.setRedraw(false);
		fTable.removeAll();
		new TextualTrace(trace, getFilterPatterns()).display(fFailureTableDisplay, MAX_LABEL_LENGTH);
		fTable.setRedraw(true);
	}

	private Collection<StringMatcher> getFilterPatterns() {
		if (UnitTestPreferencesConstants.getFilterStack())
			return getFilterPatterns(fFailure.getTestRunSession());
		return Collections.emptySet();
	}

	/**
	 * Returns an array of Filter patterns for Stacktraces/Error messages
	 *
	 * @param session a {@link ITestRunSession} to ask the filter pattern for
	 * @return an array of filter patterns
	 */
	public Collection<StringMatcher> getFilterPatterns(ITestRunSession session) {
		if (session == null) {
			return Collections.emptySet();
		}
		ITestViewSupport viewSupport = ((TestRunSession) session).getTestViewSupport();
		if (viewSupport != null) {
			Collection<StringMatcher> res = viewSupport.getTraceExclusionFilterPatterns();
			if (res != null) {
				return res;
			}
		}
		return Collections.emptySet();
	}

	/**
	 * Shows other information than a stack trace.
	 *
	 * @param text the informational message to be shown
	 */
	public void setInformation(String text) {
		clear();
		TableItem tableItem = fFailureTableDisplay.newTableItem();
		tableItem.setText(text);
	}

	/**
	 * Clears the non-stack trace info
	 */
	public void clear() {
		fTable.removeAll();
		fInputTrace = null;
	}

	/**
	 * Returns a failed test element
	 *
	 * @return a failed test element
	 */
	public TestElement getFailedTest() {
		return fFailure;
	}

	/**
	 * Returns a shell object
	 *
	 * @return a shell object
	 */
	public Shell getShell() {
		return fTable.getShell();
	}

	/**
	 * Disposes the Failure trace UI Block
	 */
	public void dispose() {
		// Nothing to dispose
	}
}
