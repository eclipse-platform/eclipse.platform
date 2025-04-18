/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
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
 *     Alexander Kurtakov - Bug 460787
 *******************************************************************************/

package org.eclipse.ua.tests.help.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.AnalyzerDescriptor;
import org.eclipse.help.internal.search.PluginIndex;
import org.eclipse.help.internal.search.QueryBuilder;
import org.eclipse.help.internal.search.SearchIndexWithIndexingProgress;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Verify that older versions of the index can be read by this
 * version of Eclipse.
 */
public class PrebuiltIndexCompatibility {

	/**
	 * Test index built with Lucene 8.4.1
	 */
	@Test
	public void test8_4_1_IndexUnreadable() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> checkReadable("data/help/searchindex/index841"));
	}

	@Test
	public void test8_4_1LuceneIncompatible() {
		checkLuceneCompatible("8.4.1", false);
	}

	@Test
	public void testLatestLuceneCompatible() {
		checkLuceneCompatible(org.apache.lucene.util.Version.LATEST.toString(), true);
	}

	@Test
	public void testPluginIndexEqualToItself() {
		PluginIndex index = createPluginIndex("data/help/searchindex/index841");
		assertEquals(index, index);
	}

	/**
	 * Verify that if the paths and plugins are the same two PluginIndex objects are equal
	 */
	@Test
	public void testPluginIndexEquality() {
		PluginIndex index1a = createPluginIndex("data/help/searchindex/index841");
		PluginIndex index1b = createPluginIndex("data/help/searchindex/index841");
		assertEquals(index1a, index1b);
	}

	/**
	 * Verify that if the paths and plugins are the same two PluginIndex objects are equal
	 */
	@Test
	public void testPluginIndexHash() {
		PluginIndex index1a = createPluginIndex("data/help/searchindex/index841");
		PluginIndex index1b = createPluginIndex("data/help/searchindex/index841");
		assertEquals(index1a.hashCode(), index1b.hashCode());
	}

	/**
	 * Verify that if the paths are different two PluginIndex objects are not equal
	 */
	@Test
	public void testPluginIndexInequality() {
		PluginIndex index1 = createPluginIndex("data/help/searchindex/index841");
		PluginIndex index2 = createPluginIndex("data/help/searchindex/index800");
		assertNotEquals(index1, index2);
	}

	/*
	 * Verifies that a prebuilt index can be searched
	 */
	private void checkReadable(String indexPath) throws IOException,
			CorruptIndexException {
		IPath path = IPath.fromOSString(indexPath);
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		URL url = FileLocator.find(bundle, path, null);
		URL resolved = FileLocator.resolve(url);
		if ("file".equals(resolved.getProtocol())) { //$NON-NLS-1$
			String filePath = resolved.getFile();
			QueryBuilder queryBuilder = new QueryBuilder("eclipse", new AnalyzerDescriptor("en-us"));
			Query luceneQuery = queryBuilder.getLuceneQuery(new ArrayList<>() , false);
			IndexSearcher searcher;
			try (Directory luceneDirectory = new NIOFSDirectory(new File(filePath).toPath())) {
				try (DirectoryReader luceneDirectoryReader = DirectoryReader.open(luceneDirectory)) {
					// This code never reached because the index is not
					// readable.
					searcher = new IndexSearcher(luceneDirectoryReader);
					TopDocs hits = searcher.search(luceneQuery, 500);
					assertTrue(hits.totalHits != null);
				}
			} catch (IndexFormatTooOldException ex) {
				// Lucene 10.x throws this more explicit exception whereas 9.x
				// throws IllegalArgumentException.
				// Note that in
				// org.eclipse.help.internal.search.SearchIndex.SearchIndex(File,
				// String, AnalyzerDescriptor, TocManager, String)
				// it catches all these:
				// catch (IndexFormatTooOldException | IndexNotFoundException |
				// IllegalArgumentException e)
				throw new IllegalArgumentException(ex);
			}
		} else {
			fail("Cannot resolve to file protocol");
		}
	}

	public PluginIndex createPluginIndex(String versionDirectory) {
		PluginIndex pluginIndex;
		SearchIndexWithIndexingProgress index = BaseHelpSystem.getLocalSearchManager().getIndex("en_us".toString());
		BaseHelpSystem.getLocalSearchManager().ensureIndexUpdated(
				new NullProgressMonitor(),
				index);
		pluginIndex = new PluginIndex("org.eclipse.ua.tests", "data/help/searchindex/" + versionDirectory, index);
		return pluginIndex;
	}

	/*
	 * Tests the isLuceneCompatible method in SearchIndex
	 */
	private void checkLuceneCompatible(String version, boolean expected) {
		SearchIndexWithIndexingProgress index = BaseHelpSystem.getLocalSearchManager().getIndex("en_us".toString());
		BaseHelpSystem.getLocalSearchManager().ensureIndexUpdated(
				new NullProgressMonitor(),
				index);
		assertEquals(expected, index.isLuceneCompatible(version));
	}

}
