/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.terminal.ui;

import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.ITerminalControl;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.control.TerminalViewControlFactory;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;

class TerminalConsolePage implements IPageBookViewPage, IAdaptable {

	private IPageSite site;
	private ITerminalViewControl viewer;
	private Composite composite;
	private final ITerminalConnector connector;
	private final Consumer<ITerminalControl> terminalControlHandler;

	public TerminalConsolePage(ITerminalConnector connector, Consumer<ITerminalControl> terminalControlHandler) {
		this.connector = connector;
		this.terminalControlHandler = terminalControlHandler;
	}

	@Override
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
		viewer = TerminalViewControlFactory.makeControl(new ConsoleTerminalListener(), composite,
				new ITerminalConnector[] {}, true);
		viewer.setConnector(connector);
		viewer.setCharset(Charset.defaultCharset());
		viewer.clearTerminal();
		viewer.connectTerminal();
		if (viewer instanceof ITerminalControl ctrl) {
			ctrl.setConnectOnEnterIfClosed(false);
			ctrl.setVT100LineWrapping(true);
			if (terminalControlHandler != null) {
				terminalControlHandler.accept(ctrl);
			}
		}
	}

	@Override
	public void dispose() {
		viewer.disposeTerminal();
		composite.dispose();
	}

	@Override
	public Control getControl() {
		return composite;
	}

	@Override
	public void setActionBars(IActionBars actionBars) {
	}

	@Override
	public void setFocus() {
		if (viewer != null) {
			viewer.setFocus();
		}
	}

	@Override
	public IPageSite getSite() {
		return site;
	}

	@Override
	public void init(IPageSite site) throws PartInitException {
		this.site = site;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

}
