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

import java.util.function.Function;

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractViewerModelTest extends AbstractDebugTest {

	Display fDisplay;
	Shell fShell;
	IInternalTreeModelViewer fViewer;
	TestModelUpdatesListener fListener;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fDisplay = PlatformUI.getWorkbench().getDisplay();
		fShell = new Shell(fDisplay);
		fShell.setMaximized(true);
		fShell.setLayout(new FillLayout());
		fViewer = createViewer(fDisplay, fShell);
		fListener = createListener(fViewer);
		fShell.open();
		TestUtil.processUIEvents();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		fListener.dispose();
		fViewer.getPresentationContext().dispose();

		// Close the shell and exit.
		fShell.close();
		TestUtil.processUIEvents();
		super.tearDown();
	}

	abstract protected IInternalTreeModelViewer createViewer(Display display, Shell shell);

	abstract protected TestModelUpdatesListener createListener(IInternalTreeModelViewer viewer);

	protected Function<AbstractDebugTest, String> createListenerErrorMessage() {
		return t -> "Listener not finished: " + fListener;
	}

}
