/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.performance;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractIndexProvider;
import org.eclipse.help.AbstractTocProvider;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.index.IndexManager;
import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.help.internal.toc.TocFile;
import org.eclipse.help.internal.toc.TocFileProvider;
import org.eclipse.help.internal.toc.TocManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

public class OpenHelpTest extends PerformanceTestCase {

	private AbstractTocProvider[] tocProviders;
	private AbstractIndexProvider[] indexProviders;
	private Shell shell;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TocManager tocManager = HelpPlugin.getTocManager();
		tocProviders = tocManager.getTocProviders();
		tocManager.setTocProviders(new AbstractTocProvider[] { new TestTocFileProvider() });
		tocManager.clearCache();

		IndexManager indexManager = HelpPlugin.getIndexManager();
		indexProviders = indexManager.getIndexProviders();
		indexManager.setIndexProviders(new AbstractIndexProvider[0]);
		indexManager.clearCache();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		TocManager tocManager = HelpPlugin.getTocManager();
		tocManager.setTocProviders(tocProviders);
		tocManager.clearCache();

		IndexManager indexManager = HelpPlugin.getIndexManager();
		indexManager.setIndexProviders(indexProviders);
		indexManager.clearCache();
	}

	public void testOpenHelp() throws Exception {
		tagAsGlobalSummary("Open help", Dimension.ELAPSED_PROCESS);

		// warm-up
		for (int i=0;i<3;++i) {
			openHelp();
			closeHelp();
		}

		// run the tests
		for (int i=0;i<50;++i) {
			startMeasuring();
			openHelp();
			stopMeasuring();
			closeHelp();
		}

		commitMeasurements();
		assertPerformance();
	}

	private void openHelp() throws Exception {
		// start the webapp
		BaseHelpSystem.ensureWebappRunning();

		// open a browser
		Shell parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		shell = new Shell(parent);
		shell.setLayout(new FillLayout());
		shell.setSize(parent.getSize());
		Browser browser = new Browser(shell, SWT.NONE);
		shell.open();

		// open help url
		final boolean[] done = new boolean[] { false };
		final String url = "http://" + WebappManager.getHost() + ":" + WebappManager.getPort() + "/help/index.jsp";
		browser.addLocationListener(new LocationAdapter() {
			@Override
			public void changed(LocationEvent event) {
				if (url.equals(event.location)) {
					done[0] = true;
				}
			}
		});
		browser.setUrl(url);

		// wait until the browser finishes loading
		Display display = Display.getDefault();
		while (!done[0]) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		flush();
	}

	private void closeHelp() throws Exception {
		if (shell != null) {
			shell.dispose();
			shell = null;
		}
	}

	private static void flush() {
		Display display = Display.getCurrent();
		while (display.readAndDispatch()) {
		}
	}

	private static class TestTocFileProvider extends TocFileProvider {
		@Override
		protected TocFile[] getTocFiles(String locale) {
			String id = FrameworkUtil.getBundle(getClass()).getSymbolicName();
			String nl = Platform.getNL();
			return new TocFile[] {
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.isv/toc.xml", true, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Guide.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Porting.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Questions.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Reference.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Samples.xml", false, nl, null, null),

				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.user/toc.xml", true, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.user/topics_Concepts.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.user/topics_GettingStarted.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.user/topics_Reference.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.jdt.doc.user/topics_Tasks.xml", false, nl, null, null),

				new TocFile(id, "data/help/performance/org.eclipse.pde.doc.user/toc.xml", true, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.pde.doc.user/topics_Reference.xml", false, nl, null, null),

				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.isv/toc.xml", true, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.isv/topics_Guide.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.isv/topics_Porting.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.isv/topics_Questions.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.isv/topics_Reference.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.isv/topics_Samples.xml", false, nl, null, null),

				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.user/toc.xml", true, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.user/topics_Concepts.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.user/topics_GettingStarted.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.user/topics_Reference.xml", false, nl, null, null),
				new TocFile(id, "data/help/performance/org.eclipse.platform.doc.user/topics_Tasks.xml", false, nl, null, null),
			};
		}
	}
}
