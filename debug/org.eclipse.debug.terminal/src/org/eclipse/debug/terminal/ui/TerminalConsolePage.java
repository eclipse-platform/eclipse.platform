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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.eclipse.cdt.utils.spawner.Spawner;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IBinaryStreamMonitor;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.control.TerminalViewControlFactory;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;

class TerminalConsolePage implements IPageBookViewPage, IAdaptable {

	private Spawner process;
	private IPageSite site;
	private ITerminalViewControl viewer;
	private Composite composite;
	private IStreamsProxy streamsProxy;

	public TerminalConsolePage(Spawner spawner, IStreamsProxy streamsProxy) {
		this.process = spawner;
		this.streamsProxy = streamsProxy;
	}

	@Override
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());
		viewer = TerminalViewControlFactory.makeControl(new ConsoleTerminalListener(), composite,
				new ITerminalConnector[] {}, true);
		viewer.setConnector(new ConsoleConnector(process));
		viewer.setCharset(Charset.defaultCharset());
		viewer.clearTerminal();
		viewer.connectTerminal();
		if (viewer instanceof ITerminalControl ctrl) {
			ctrl.setConnectOnEnterIfClosed(false);
			ctrl.setVT100LineWrapping(true);
			IStreamMonitor streamMonitor = streamsProxy.getOutputStreamMonitor();
			if (streamMonitor instanceof IBinaryStreamMonitor bin) {
				OutputStream outputStream = ctrl.getRemoteToTerminalOutputStream();
				bin.addBinaryListener((data, monitor) -> {
					try {
						outputStream.write(data);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				});
			}
		}
	}

	@Override
	public void dispose() {
		viewer.disposeTerminal();
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
