/*******************************************************************************
 * Copyright (c) 2026 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.MessagePage;

/**
 * Dummy console page showing mock number and counting the numbers its
 * control was shown in the console view.
 */
final class ConsoleMock implements IConsole {
	MessagePage page;
	final AtomicInteger showCalled;
	final AtomicInteger pageShownCalled;
	final AtomicInteger pageHiddenCalled;
	final int number;
	final static AtomicInteger allShownConsoles = new AtomicInteger();

	public ConsoleMock(int number) {
		this.number = number;
		showCalled = new AtomicInteger();
		pageShownCalled = new AtomicInteger();
		pageHiddenCalled = new AtomicInteger();
	}

	@Override
	public void pageShown() {
		pageShownCalled.incrementAndGet();
	}

	@Override
	public void pageHidden() {
		pageHiddenCalled.incrementAndGet();
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
	}

	@Override
	public String getType() {
		return null;
	}

	@Override
	public String getName() {
		return toString();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
	}

	/**
	 * Just a page showing the mock console name
	 */
	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		page = new MessagePage() {
			@Override
			public void createControl(Composite parent) {
				super.createControl(parent);
				// This listener is get called if the page is really shown
				// in the console view
				getControl().addListener(SWT.Show, event -> {
					int count = showCalled.incrementAndGet();
					if (count == 1) {
						count = allShownConsoles.incrementAndGet();
						System.out.println("Shown: " + ConsoleMock.this + ", overall: " + count); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
			}

		};
		page.setMessage(toString());
		return page;
	}

	@Override
	public String toString() {
		return "mock #" + number; //$NON-NLS-1$
	}
}