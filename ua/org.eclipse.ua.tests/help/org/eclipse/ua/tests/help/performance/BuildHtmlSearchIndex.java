/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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

import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractIndexProvider;
import org.eclipse.help.AbstractTocProvider;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.index.IndexManager;
import org.eclipse.help.internal.search.AnalyzerDescriptor;
import org.eclipse.help.internal.search.SearchIndex;
import org.eclipse.help.internal.search.SearchIndexWithIndexingProgress;
import org.eclipse.help.internal.toc.TocFile;
import org.eclipse.help.internal.toc.TocFileProvider;
import org.eclipse.help.internal.toc.TocManager;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.osgi.framework.FrameworkUtil;

public class BuildHtmlSearchIndex extends PerformanceTestCase {

	private AbstractTocProvider[] tocProviders;
	private AbstractIndexProvider[] indexProviders;
	private AnalyzerDescriptor analyzerDesc;

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
		analyzerDesc = new AnalyzerDescriptor("en-us");
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
		analyzerDesc = null;
		tocProviders = null;
		indexProviders = null;
	}

	public void testCreateHtmlSearchIndex() throws Exception {
		tagAsGlobalSummary("Create HTML Search Index", Dimension.ELAPSED_PROCESS);

		// warm-up
		for (int i=0;i<3;++i) {
			buildIndex();
		}

		// run the tests
		for (int i=0;i<100;++i) {
			startMeasuring();
			buildIndex();
			stopMeasuring();
		}

		commitMeasurements();
		assertPerformance();
	}

	private void buildIndex() {
		SearchIndexWithIndexingProgress index = new SearchIndexWithIndexingProgress("en-us", analyzerDesc, HelpPlugin
				.getTocManager());
		index.beginAddBatch(true);
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test7.html");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test8.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/search/test9.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_active_action.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_active.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_active_action.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_active_debug.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_active_invoke.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_command.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_command_authoring.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_files.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_manifest.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_nested.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_process.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_remote.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_toc.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_content_xhtml.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_context.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_context_dynamic.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_context_id.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_context_infopops.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_context_xml.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_menu.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_search.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_search_types.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_setup.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_setup_about.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_setup_help_data.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_setup_infocenter.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_setup_nav.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_setup_preferences.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_setup_preindex.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_setup_rcp.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_setup_standalone.htm");
		addHrefToIndex(index, "/org.eclipse.ua.tests/data/help/performance/search/ua_help_war.htm");
		index.endAddBatch(true, true);
		index.close();
	}


	private void addHrefToIndex(SearchIndexWithIndexingProgress index,
			String doc) {
		URL url = SearchIndex.getIndexableURL(index.getLocale(), doc);
		IStatus status = index.addDocument(url.getFile(), url);
		assertTrue(status.isOK());
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
