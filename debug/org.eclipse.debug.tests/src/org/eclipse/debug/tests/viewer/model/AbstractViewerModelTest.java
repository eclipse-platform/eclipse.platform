/*******************************************************************************
 *  Copyright (c) 2017 Andrey Loskutov and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

import java.util.function.Supplier;

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.tests.DebugTestExtension;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DebugTestExtension.class)
public abstract class AbstractViewerModelTest {

	Display fDisplay;
	Shell fShell;
	IInternalTreeModelViewer fViewer;
	TestModelUpdatesListener fListener;

	@BeforeEach
	public void setUp() throws Exception {
		fDisplay = PlatformUI.getWorkbench().getDisplay();
		fShell = new Shell(fDisplay);
		fShell.setMaximized(true);
		fShell.setLayout(new FillLayout());
		fViewer = createViewer(fDisplay, fShell);
		fListener = createListener(fViewer);
		fShell.open();
		TestUtil.processUIEvents();
	}

	@AfterEach
	public void tearDown() throws Exception {
		fListener.dispose();
		fViewer.getPresentationContext().dispose();

		// Close the shell and exit.
		fShell.close();
		TestUtil.processUIEvents();
	}

	abstract protected IInternalTreeModelViewer createViewer(Display display, Shell shell);

	abstract protected TestModelUpdatesListener createListener(IInternalTreeModelViewer viewer);

	protected Supplier<String> createListenerErrorMessage() {
		return () -> "Listener not finished: " + fListener;
	}

}
