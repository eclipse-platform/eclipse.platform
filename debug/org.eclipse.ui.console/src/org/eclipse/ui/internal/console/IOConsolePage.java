/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     vogella GmbH - Bug 287303 - [patch] Add Word Wrap action to Console View
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;

/**
 * A page for an IOConsole
 *
 * @since 3.1
 *
 */
public class IOConsolePage extends TextConsolePage {

	private ScrollLockAction fScrollLockAction;
	private WordWrapAction fWordWrapAction;

	private boolean fReadOnly;

	private IPropertyChangeListener fPropertyChangeListener;

	private IConsoleView fView;

	public IOConsolePage(TextConsole console, IConsoleView view) {
		super(console, view);
		fView = view;

		fPropertyChangeListener = event -> {
			String property = event.getProperty();
			if (property.equals(IConsoleConstants.P_CONSOLE_OUTPUT_COMPLETE)) {
				setReadOnly();
			}
		};
		console.addPropertyChangeListener(fPropertyChangeListener);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		if (fReadOnly) {
			IOConsoleViewer viewer = (IOConsoleViewer) getViewer();
			viewer.setReadOnly();
		}
	}

	@Override
	protected TextConsoleViewer createViewer(Composite parent) {
		return new IOConsoleViewer(parent, (TextConsole) getConsole(), fView);
	}

	public void setAutoScroll(boolean scroll) {
		IOConsoleViewer viewer = (IOConsoleViewer) getViewer();
		if (viewer != null) {
			viewer.setAutoScroll(scroll);
			fScrollLockAction.setChecked(!scroll);
		}
	}

	public boolean isAutoScroll() {
		IOConsoleViewer viewer = (IOConsoleViewer) getViewer();
		if (viewer != null) {
			return viewer.isAutoScroll();

		}
		return false;
	}
	public void setWordWrap(boolean wordwrap) {
		IOConsoleViewer viewer = (IOConsoleViewer) getViewer();
		if (viewer != null) {
			viewer.setWordWrap(wordwrap);
			fWordWrapAction.setChecked(wordwrap);
		}
	}

	/**
	 * Informs the viewer that it's text widget should not be editable.
	 */
	public void setReadOnly() {
		fReadOnly = true;
		IOConsoleViewer viewer = (IOConsoleViewer) getViewer();
		if (viewer != null) {
			viewer.setReadOnly();
		}
	}

	@Override
	protected void createActions() {
		super.createActions();
		fScrollLockAction = new ScrollLockAction(getConsoleView());
		setAutoScroll(!fScrollLockAction.isChecked());
		fWordWrapAction = new WordWrapAction(getConsoleView());
		setWordWrap(fWordWrapAction.isChecked());
	}

	@Override
	protected void contextMenuAboutToShow(IMenuManager menuManager) {
		super.contextMenuAboutToShow(menuManager);
		menuManager.add(fScrollLockAction);
		menuManager.add(fWordWrapAction);
		IOConsoleViewer viewer = (IOConsoleViewer) getViewer();
		if (!viewer.isReadOnly()) {
			menuManager.remove(ActionFactory.CUT.getId());
			menuManager.remove(ActionFactory.PASTE.getId());
		}
	}

	@Override
	protected void configureToolBar(IToolBarManager mgr) {
		super.configureToolBar(mgr);
		mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, fScrollLockAction);
		mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, fWordWrapAction);
	}

	@Override
	public void dispose() {
		if (fScrollLockAction != null) {
			fScrollLockAction.dispose();
			fScrollLockAction = null;
		}
		if (fWordWrapAction != null) {
			fWordWrapAction.dispose();
			fWordWrapAction = null;
		}
		fView = null;
		getConsole().removePropertyChangeListener(fPropertyChangeListener);
		super.dispose();
	}
}
