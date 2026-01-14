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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.ui.console.AbstractConsole;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.plugin.AbstractUIPlugin;

class TerminalConsole extends AbstractConsole {

	public final static String TYPE = "terminalConsole"; //$NON-NLS-1$
	private final ITerminalConnector terminalConnector;

	TerminalConsole(ITerminalConnector terminalConnector) {
		super("Terminal", TYPE, getImageDescriptorFromBundle(), true);
		this.terminalConnector = terminalConnector;
	}

	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		return new TerminalConsolePage(terminalConnector, null);
	}

	private static ImageDescriptor getImageDescriptorFromBundle() {
		return AbstractUIPlugin.imageDescriptorFromPlugin(
			"org.eclipse.debug.terminal", //$NON-NLS-1$
			"icons/console_view.svg" //$NON-NLS-1$
		);
	}

}
