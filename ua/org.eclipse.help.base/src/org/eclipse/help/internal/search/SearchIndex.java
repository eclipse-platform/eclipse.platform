/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Holger Voormann - fix for bug 426785 (http://eclip.se/426785)
 *     Alexander Kurtakov - Bug 460787
 *     Sopot Cela - Bug 466829
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.search;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.util.HelpProperties;
import org.eclipse.help.internal.protocols.HelpURLConnection;
import org.eclipse.help.internal.protocols.HelpURLStreamHandler;
import org.eclipse.help.internal.toc.TocFileProvider;
import org.eclipse.help.internal.toc.TocManager;
import org.eclipse.help.internal.util.ResourceLocator;
import org.eclipse.help.search.IHelpSearchIndex;
import org.eclipse.help.search.ISearchDocument;
import org.eclipse.help.search.SearchParticipant;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Text search index. Documents added to this index can than be searched against a search query.
 */
public class SearchIndex implements IHelpSearchIndex {

	private IndexReader ir;

	private IndexWriter iw;

	private final File indexDir;

	private Directory luceneDirectory;

	private final String locale;

	private final String relativePath;

	private final TocManager tocManager;

	private final AnalyzerDescriptor analyzerDescriptor;

	private PluginVersionInfo docPlugins;

	// table of all document names, used during indexing batches
	private HelpProperties indexedDocs;

	public static final String INDEXED_CONTRIBUTION_INFO_FILE = "indexed_contributions"; //$NON-NLS-1$

	public static final String INDEXED_DOCS_FILE = "indexed_docs"; //$NON-NLS-1$

	public static final String DEPENDENCIES_VERSION_FILENAME = "indexed_dependencies"; //$NON-NLS-1$

	public static final String DEPENDENCIES_KEY_LUCENE = "lucene"; //$NON-NLS-1$

	public static final String DEPENDENCIES_KEY_ANALYZER = "analyzer"; //$NON-NLS-1$

	private static final String LUCENE_BUNDLE_ID = "org.apache.lucene.core"; //$NON-NLS-1$

	private static final String FIELD_NAME = "name"; //$NON-NLS-1$

	private static final String FIELD_INDEX_ID = "index_path"; //$NON-NLS-1$

	private final File inconsistencyFile;

	private final HTMLSearchParticipant htmlSearchParticipant;

	private IndexSearcher searcher;

	private final Object searcherCreateLock = new Object();

	private HelpProperties dependencies;

	private volatile boolean closed = false;

	// Collection of searches occuring now
	private final Collection<Thread> searches = new ArrayList<>();

	private FileLock lock;
	private RandomAccessFile raf =  null;

	/**
	 * Constructor.
	 *
	 * @param locale
	 *            the locale this index uses
	 * @param analyzerDesc
	 *            the analyzer used to index
	 */
	public SearchIndex(String locale, AnalyzerDescriptor analyzerDesc, TocManager tocManager) {
		this(new File(HelpBasePlugin.getConfigurationDirectory(), "index/" + locale), //$NON-NLS-1$
				locale, analyzerDesc, tocManager, null);
	}

	/**
	 * Alternative constructor that provides index directory.
	 *
	 * @param indexDir
	 * @param locale
	 * @param analyzerDesc
	 * @param tocManager
	 * @since 3.1
	 */

	public SearchIndex(File indexDir, String locale, AnalyzerDescriptor analyzerDesc, TocManager tocManager,
			String relativePath) {
		this.locale = locale;
		this.analyzerDescriptor = analyzerDesc;
		this.tocManager = tocManager;
		this.indexDir = indexDir;

		this.relativePath = relativePath;
		inconsistencyFile = new File(indexDir.getParentFile(), locale + ".inconsistent"); //$NON-NLS-1$
		htmlSearchParticipant = new HTMLSearchParticipant(indexDir.getAbsolutePath());
		try {
			luceneDirectory = new NIOFSDirectory(indexDir.toPath());
		} catch (IOException e) {
		}
		if (!exists()) {
			try {
				if (tryLock()) {
					// don't block or unzip when another instance is indexing
					try {
						unzipProductIndex();
					} finally {
						releaseLock();
					}
				}
			} catch (OverlappingFileLockException ofle) {
				// another thread in this process is unzipping
				// should never be here - one index instance per locale exists
				// in vm
			}
		}

		try (DirectoryReader reader = DirectoryReader.open(luceneDirectory)) {
		} catch (IndexFormatTooOldException | IndexNotFoundException | IllegalArgumentException e) {
			deleteDir(indexDir);
			indexDir.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void deleteDir(File indexDir) {
		File[] files = indexDir.listFiles();
		if(files == null) {
			files = new File[0];
		}
		for (File file : files) {
			if (file.isDirectory())
				deleteDir(file);
			file.delete();
		}
	}

	/**
	 * Indexes one document from a stream. Index has to be open and close outside of this method
	 *
	 * @param name
	 *            the document identifier (could be a URL)
	 * @param url
	 *            the URL of the document
	 * @return IStatus
	 */
	public IStatus addDocument(String name, URL url) {
		try {
			Document doc = new Document();
			doc.add(new StringField(FIELD_NAME, name, Field.Store.YES));
			addExtraFields(doc);
			String pluginId = LocalSearchManager.getPluginId(name);
			if (relativePath != null) {
				doc.add(new StringField(FIELD_INDEX_ID, relativePath, Field.Store.YES));
			}
			// check for the explicit search participant.
			SearchParticipant participant = null;
			HelpURLConnection urlc = new HelpURLConnection(url);
			String id = urlc.getValue("id"); //$NON-NLS-1$
			String pid = urlc.getValue("participantId"); //$NON-NLS-1$
			if (pid != null)
				participant = BaseHelpSystem.getLocalSearchManager().getGlobalParticipant(pid);
			// check for file extension-based search participant;
			if (participant == null)
				participant = BaseHelpSystem.getLocalSearchManager().getParticipant(pluginId, name);
			if (participant != null) {
				IStatus status = participant.addDocument(this, pluginId, name, url, id, new LuceneSearchDocument(doc));
				if (status.getSeverity() == IStatus.OK) {
					String filters = doc.get("filters"); //$NON-NLS-1$
					indexedDocs.put(name, filters != null ? filters : "0"); //$NON-NLS-1$
					if (id != null)
						doc.add(new StoredField("id", id)); //$NON-NLS-1$
					if (pid != null)
						doc.add(new StoredField("participantId", pid)); //$NON-NLS-1$
					iw.addDocument(doc);
				}
				return status;
			}
			// default to html
			IStatus status = htmlSearchParticipant.addDocument(this, pluginId, name, url, id, new LuceneSearchDocument(doc));
			if (status.getSeverity() == IStatus.OK) {
				String filters = doc.get("filters"); //$NON-NLS-1$
				indexedDocs.put(name, filters != null ? filters : "0"); //$NON-NLS-1$
				iw.addDocument(doc);
			}
			return status;
		} catch (IOException e) {
			return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR,
					"IO exception occurred while adding document " + name //$NON-NLS-1$
							+ " to index " + indexDir.getAbsolutePath() + ".", //$NON-NLS-1$ //$NON-NLS-2$
					e);
		}
		catch (Exception e) {
			return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR,
					"An unexpected internal error occurred while adding document " //$NON-NLS-1$
							+ name + " to index " + indexDir.getAbsolutePath() //$NON-NLS-1$
							+ ".", e); //$NON-NLS-1$
		}
	}

	/**
	 * Add any extra fields that need to be added to this document. Subclasses
	 * should override to add more fields.
	 *
	 * @param doc the document to add fields to
	 */
	protected void addExtraFields(Document doc) {
	}

	/**
	 * Starts additions. To be called before adding documents.
	 */
	@SuppressWarnings("resource")
	public synchronized boolean beginAddBatch(boolean firstOperation) {
		try {
			if (iw != null) {
				iw.close();
			}
			boolean create = false;
			if (!indexDir.exists() || !isLuceneCompatible() || !isAnalyzerCompatible()
					|| inconsistencyFile.exists() && firstOperation) {
				create = true;
				indexDir.mkdirs();
				if (!indexDir.exists())
					return false; // unable to setup index directory
			}
			indexedDocs = new HelpProperties(INDEXED_DOCS_FILE, indexDir);
			indexedDocs.restore();
			setInconsistent(true);
			LimitTokenCountAnalyzer analyzer = new LimitTokenCountAnalyzer(analyzerDescriptor.getAnalyzer(), 1000000);
			IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
			writerConfig.setOpenMode(create ? OpenMode.CREATE : OpenMode.APPEND);
			LogMergePolicy mergePolicy = new LogByteSizeMergePolicy();
			mergePolicy.setMergeFactor(20);
			writerConfig.setMergePolicy(mergePolicy);
			iw = new IndexWriter(luceneDirectory, writerConfig);
			return true;
		} catch (IOException e) {
			ILog.of(getClass()).error("Exception occurred in search indexing at beginAddBatch.", e); //$NON-NLS-1$
			return false;
		}
	}

	/**
	 * Starts deletions. To be called before deleting documents.
	 */
	@SuppressWarnings("resource")
	public synchronized boolean beginDeleteBatch() {
		try {
			if (iw != null) {
				iw.close();
			}
			indexedDocs = new HelpProperties(INDEXED_DOCS_FILE, indexDir);
			indexedDocs.restore();
			setInconsistent(true);
			iw = new IndexWriter(luceneDirectory, new IndexWriterConfig(analyzerDescriptor.getAnalyzer()));
			return true;
		} catch (IOException e) {
			ILog.of(getClass()).error("Exception occurred in search indexing at beginDeleteBatch.", e); //$NON-NLS-1$
			return false;
		}
	}

	/**
	 * Starts deletions. To be called before deleting documents.
	 */
	public synchronized boolean beginRemoveDuplicatesBatch() {
		try {
			if (ir != null) {
				ir.close();
			}
			ir = DirectoryReader.open(luceneDirectory);
			if (iw == null) {
				return beginDeleteBatch();
			}
			return true;
		} catch (IOException e) {
			ILog.of(getClass()).error("Exception occurred in search indexing at beginDeleteBatch.", e); //$NON-NLS-1$
			return false;
		}
	}

	/**
	 * Deletes a single document from the index.
	 *
	 * @param name -
	 *            document name
	 * @return IStatus
	 */
	public IStatus removeDocument(String name) {
		Term term = new Term(FIELD_NAME, name);
		try {
			iw.deleteDocuments(term);
			indexedDocs.remove(name);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR,
					"IO exception occurred while removing document " + name //$NON-NLS-1$
							+ " from index " + indexDir.getAbsolutePath() + ".", //$NON-NLS-1$ //$NON-NLS-2$
					e);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Finish additions. To be called after adding documents.
	 */
	public synchronized boolean endAddBatch(boolean optimize, boolean lastOperation) {
		try {
			if (iw == null)
				return false;
			if (optimize)
				iw.forceMerge(1, true);
			iw.close();
			iw = null;
			// save the update info:
			// - all the docs
			// - plugins (and their version) that were indexed
			getDocPlugins().save();
			saveDependencies();
			if (lastOperation) {
				indexedDocs.save();
				indexedDocs = null;
				setInconsistent(false);
			}

			/*
			 * The searcher's index reader has it's stuff in memory so it won't
			 * know about this change. Close it so that it gets reloaded next search.
			 */
			if (searcher != null) {
				searcher.getIndexReader().close();
				searcher = null;
			}
			return true;
		} catch (IOException e) {
			ILog.of(getClass()).error("Exception occurred in search indexing at endAddBatch.", e); //$NON-NLS-1$
			return false;
		}
	}

	/**
	 * Finish deletions. To be called after deleting documents.
	 */
	public synchronized boolean endDeleteBatch() {
		try {
			if (iw == null)
				return false;
			iw.close();
			iw = null;
			// save the update info:
			// - all the docs
			// - plugins (and their version) that were indexed
			indexedDocs.save();
			indexedDocs = null;
			getDocPlugins().save();
			saveDependencies();

			/*
			 * The searcher's index reader has it's stuff in memory so it won't
			 * know about this change. Close it so that it gets reloaded next search.
			 */
			if (searcher != null) {
				searcher.getIndexReader().close();
				searcher = null;
			}
			return true;
		} catch (IOException e) {
			ILog.of(getClass()).error("Exception occurred in search indexing at endDeleteBatch.", e); //$NON-NLS-1$
			return false;
		}
	}

	/**
	 * Finish deletions. To be called after deleting documents.
	 */
	public synchronized boolean endRemoveDuplicatesBatch() {
		try {
			if (ir == null)
				return false;
			ir.close();
			ir = null;
			iw.close();
			iw = null;
			// save the update info:
			// - all the docs
			// - plugins (and their version) that were indexed
			indexedDocs.save();
			indexedDocs = null;
			getDocPlugins().save();
			saveDependencies();
			setInconsistent(false);
			return true;
		} catch (IOException e) {
			ILog.of(getClass()).error("Exception occurred in search indexing at endDeleteBatch.", e); //$NON-NLS-1$
			return false;
		}
	}

	/**
	 *
	 * @param pluginIndexes
	 * @param monitor
	 * @return Map. Keys are /pluginid/href of all merged Docs. Values are null for
	 *         added document, or String[] of indexIds with duplicates of the
	 *         document
	 */
	public Map<String, String[]> merge(PluginIndex[] pluginIndexes, IProgressMonitor monitor) {
		ArrayList<NIOFSDirectory> dirList = new ArrayList<>(pluginIndexes.length);
		Map<String, String[]> mergedDocs = new HashMap<>();
		// Create directories to merge and calculate all documents added
		// and which are duplicates (to delete later)
		for (PluginIndex pluginIndex : pluginIndexes) {
			List<String> indexIds = pluginIndex.getIDs();
			List<String> indexPaths = pluginIndex.getPaths();
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}

			for (int i = 0; i < indexPaths.size(); i++) {
				String indexId = indexIds.get(i);
				String indexPath = indexPaths.get(i);
				try {
					// can't use try-with-resources as 'dir' needs to stay open
					@SuppressWarnings("resource")
					NIOFSDirectory dir = new NIOFSDirectory(new File(indexPath).toPath());
					dirList.add(dir);
				} catch (IOException ioe) {
					ILog.of(getClass()).error(
							"Help search indexing directory could not be created for directory " + indexPath, ioe); //$NON-NLS-1$
					continue;
				}

				HelpProperties prebuiltDocs = new HelpProperties(INDEXED_DOCS_FILE, new File(indexPath));
				prebuiltDocs.restore();
				Set<?> prebuiltHrefs = prebuiltDocs.keySet();
				for (Iterator<?> it = prebuiltHrefs.iterator(); it.hasNext();) {
					String href = (String) it.next();
					if (i == 0) {
						// optimization for first prebuilt index of a plug-in
						mergedDocs.put(href, null);
					} else {
						if (mergedDocs.containsKey(href)) {
							// this is duplicate
							String[] dups = mergedDocs.get(href);
							if (dups == null) {
								// first duplicate
								mergedDocs.put(href, new String[] { indexId });
							} else {
								// next duplicate
								String[] newDups = new String[dups.length + 1];
								System.arraycopy(dups, 0, newDups, 0, dups.length);
								newDups[dups.length] = indexId;
								mergedDocs.put(href, newDups);
							}
						} else {
							// document does not exist in more specific indexes
							// for this plugin
							mergedDocs.put(href, null);
						}

					}
				}
			}
		}
		// perform actual merging
		for (String doc : mergedDocs.keySet()) {
			indexedDocs.put(doc, "0"); //$NON-NLS-1$
		}
		Directory[] luceneDirs = dirList.toArray(new Directory[dirList.size()]);
		try {
			iw.addIndexes(luceneDirs);
			iw.forceMerge(1, true);
		} catch (IOException ioe) {
			ILog.of(getClass()).error("Merging search indexes failed.", ioe); //$NON-NLS-1$
			return new HashMap<>();
		}
		return mergedDocs;
	}

	@SuppressWarnings("resource")
	public IStatus removeDuplicates(String name, String[] index_paths) {

		try (DirectoryReader ar = DirectoryReader.open(luceneDirectory)) {
			PostingsEnum hrefDocs = null;
			PostingsEnum indexDocs = null;
			Term hrefTerm = new Term(FIELD_NAME, name);
			for (String index_path : index_paths) {
				Term indexTerm = new Term(FIELD_INDEX_ID, index_path);
				List<LeafReaderContext> leaves = ar.leaves();
				for (LeafReaderContext c : leaves) {
					indexDocs = c.reader().postings(indexTerm);
					hrefDocs = c.reader().postings(hrefTerm);
					removeDocuments(hrefDocs, indexDocs);
				}
			}
		} catch (IOException ioe) {
			return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR,
					"IO exception occurred while removing duplicates of document " + name //$NON-NLS-1$
							+ " from index " + indexDir.getAbsolutePath() + ".", //$NON-NLS-1$ //$NON-NLS-2$
					ioe);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Removes documents containing term1 and term2
	 *
	 * @param doc1
	 * @param docs2
	 * @throws IOException
	 */
	private void removeDocuments(PostingsEnum doc1, PostingsEnum docs2) throws IOException {
		if (doc1.nextDoc() == DocIdSetIterator.NO_MORE_DOCS) {
			return;
		}
		if (docs2.nextDoc() == DocIdSetIterator.NO_MORE_DOCS) {
			return;
		}
		while (true) {
			if (doc1.docID() < docs2.docID()) {
				if (doc1.advance(docs2.docID()) == DocIdSetIterator.NO_MORE_DOCS
						&& doc1.nextDoc() == DocIdSetIterator.NO_MORE_DOCS) {
					return;
				}
			} else if (doc1.docID() > docs2.docID() && docs2.advance(doc1.docID()) == DocIdSetIterator.NO_MORE_DOCS
					&& doc1.nextDoc() == DocIdSetIterator.NO_MORE_DOCS) {
				return;
			}
			if (doc1.docID() == docs2.docID()) {
				iw.tryDeleteDocument(ir, doc1.docID());
				if (doc1.nextDoc() == DocIdSetIterator.NO_MORE_DOCS) {
					return;
				}
				if (docs2.nextDoc() == DocIdSetIterator.NO_MORE_DOCS) {
					return;
				}
			}
		}
	}

	/**
	 * Checks if index exists and is usable.
	 *
	 * @return true if index exists
	 */
	public boolean exists() {
		return indexDir.exists() && !isInconsistent();
		// assume index exists if directory does
	}

	/**
	 * Performs a query search on this index
	 */
	public void search(ISearchQuery searchQuery, ISearchHitCollector collector)
			throws QueryTooComplexException {
		try {
			if (closed)
				return;
			registerSearch(Thread.currentThread());
			if (closed)
				return;
			QueryBuilder queryBuilder = new QueryBuilder(searchQuery.getSearchWord(), analyzerDescriptor);
			Query luceneQuery = queryBuilder.getLuceneQuery(searchQuery.getFieldNames(), searchQuery
					.isFieldSearch());
			if (HelpPlugin.DEBUG_SEARCH) {
				System.out.println("Search Query: " + luceneQuery.toString()); //$NON-NLS-1$
			}
			String highlightTerms = queryBuilder.gethighlightTerms();
			if (luceneQuery != null) {
				if (searcher == null) {
					openSearcher();
				}
				TopDocs topDocs = searcher.search(luceneQuery, 1000);
				collector.addHits(LocalSearchManager.asList(topDocs, searcher), highlightTerms);
			}
		} catch (IndexSearcher.TooManyClauses tmc) {
			collector.addQTCException(new QueryTooComplexException());
		} catch (QueryTooComplexException qe) {
			collector.addQTCException(qe);
		} catch (Exception e) {
			ILog.of(getClass()).error(
					"Exception occurred performing search for: " //$NON-NLS-1$
					+ searchQuery.getSearchWord() + ".", e); //$NON-NLS-1$
		} finally {
			unregisterSearch(Thread.currentThread());
		}
	}

	@Override
	public String getLocale() {
		return locale;
	}

	/**
	 * Returns the list of all the plugins in this session that have declared a help contribution.
	 */
	public PluginVersionInfo getDocPlugins() {
		if (docPlugins == null) {
			Set<String> totalIds = new HashSet<>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint extensionPoint = registry.getExtensionPoint(TocFileProvider.EXTENSION_POINT_ID_TOC);
			IExtension[] extensions = extensionPoint.getExtensions();
			for (IExtension extension : extensions) {
				try {
					totalIds.add(extension.getContributor().getName());
				}
				catch (InvalidRegistryObjectException e) {
					// ignore this extension and move on
				}
			}
			Collection<String> additionalPluginIds = BaseHelpSystem.getLocalSearchManager()
					.getPluginsWithSearchParticipants();
			totalIds.addAll(additionalPluginIds);
			docPlugins = new PluginVersionInfo(INDEXED_CONTRIBUTION_INFO_FILE, totalIds, indexDir, !exists());
		}
		return docPlugins;
	}

	/**
	 * Sets the list of all plug-ns in this session. This method is used for external indexer.
	 *
	 * @param docPlugins
	 */
	public void setDocPlugins(PluginVersionInfo docPlugins) {
		this.docPlugins = docPlugins;
	}

	/**
	 * We use HelpProperties, but a list would suffice. We only need the key values.
	 *
	 * @return HelpProperties, keys are URLs of indexed documents
	 */
	public HelpProperties getIndexedDocs() {
		HelpProperties indexedDocs = new HelpProperties(INDEXED_DOCS_FILE, indexDir);
		if (exists())
			indexedDocs.restore();
		return indexedDocs;
	}

	/**
	 * Gets properties with versions of Lucene plugin and Analyzer used in existing index
	 */
	private HelpProperties getDependencies() {
		if (dependencies == null) {
			dependencies = new HelpProperties(DEPENDENCIES_VERSION_FILENAME, indexDir);
			dependencies.restore();
		}
		return dependencies;
	}

	private boolean isLuceneCompatible() {
		String usedLuceneVersion = getDependencies().getProperty(DEPENDENCIES_KEY_LUCENE);
		return isLuceneCompatible(usedLuceneVersion);
	}

	/**
	 * Determines whether an index can be read by the Lucene bundle
	 * @param indexVersionString The version of an Index directory
	 * @return
	 */
	public boolean isLuceneCompatible(String indexVersionString) {
		if (indexVersionString==null) return false;
		org.apache.lucene.util.Version nativeLuceneVersion = org.apache.lucene.util.Version.LATEST;
		Version luceneVersion = new Version(nativeLuceneVersion.toString());
		Version indexVersion = new Version(indexVersionString);
		Version luceneMajMin = new Version(nativeLuceneVersion.major, nativeLuceneVersion.minor, 0);
		if (indexVersion.compareTo(luceneMajMin) < 0) {
			// index is older than Lucene Maj.Min.0
			return false;
		}
		if ( luceneVersion.compareTo(indexVersion) >= 0 ) {
			// Lucene bundle is newer than the index
			return true;
		}
		return luceneVersion.getMajor() == indexVersion.getMajor()
				&& luceneVersion.getMinor() == indexVersion.getMinor()
				&& luceneVersion.getMicro() == indexVersion.getMicro();
	}

	private boolean isAnalyzerCompatible() {
		String usedAnalyzer = getDependencies().getProperty(DEPENDENCIES_KEY_ANALYZER);
		return isAnalyzerCompatible(usedAnalyzer);
	}

	public boolean isAnalyzerCompatible(String analyzerId) {
		if (analyzerId == null) {
			analyzerId = ""; //$NON-NLS-1$
		}
		return analyzerDescriptor.isCompatible(analyzerId);
	}

	/**
	 * Saves Lucene version and analyzer identifier to a file.
	 */
	private void saveDependencies() {
		getDependencies().put(DEPENDENCIES_KEY_ANALYZER, analyzerDescriptor.getId());
		Bundle luceneBundle = Platform.getBundle(LUCENE_BUNDLE_ID);
		if (luceneBundle != null) {
			String luceneBundleVersion = "" //$NON-NLS-1$
					+ luceneBundle.getHeaders().get(Constants.BUNDLE_VERSION);
			getDependencies().put(DEPENDENCIES_KEY_LUCENE, luceneBundleVersion);
		} else {
			getDependencies().put(DEPENDENCIES_KEY_LUCENE, ""); //$NON-NLS-1$
		}
		getDependencies().save();
	}

	/**
	 * @return Returns true if index has been left in inconsistent state If analyzer has changed to
	 *         incompatible one, index is treated as inconsistent as well.
	 */
	public boolean isInconsistent() {
		if (inconsistencyFile.exists()) {
			return true;
		}
		return !isLuceneCompatible() || !isAnalyzerCompatible();
	}

	/**
	 * Writes or deletes inconsistency flag file
	 */
	public void setInconsistent(boolean inconsistent) {
		if (inconsistent) {
			try (FileOutputStream fos = new FileOutputStream(inconsistencyFile)) {
				// parent directory already created by beginAddBatch on new
				// index
			} catch (IOException ioe) {
			}
		} else
			inconsistencyFile.delete();
	}

	@SuppressWarnings("resource")
	public void openSearcher() throws IOException {
		synchronized (searcherCreateLock) {
			if (searcher == null) {
				searcher = new IndexSearcher(DirectoryReader.open(luceneDirectory));
			}
		}
	}

	/**
	 * Closes IndexReader used by Searcher. Should be called on platform shutdown, or when TOCs have
	 * changed when no more reading from this index is to be performed.
	 */
	public void close() {
		closed = true;

		// wait for all searches to finish
		while (true) {
			synchronized (searches) {
				if (searches.isEmpty()) {
					if (searcher != null) {
						try {
							searcher.getIndexReader().close();
						} catch (IOException ioe) {
						}
					}
					break;
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException ie) {
			}
		}
	}

	/**
	 * Finds and unzips prebuild index specified in preferences
	 */
	private void unzipProductIndex() {
		String indexPluginId = Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID, "productIndex", null, null); //$NON-NLS-1$
		if (indexPluginId == null || indexPluginId.length() <= 0) {
			return;
		}
		InputStream zipIn = ResourceLocator.openFromPlugin(indexPluginId, "doc_index.zip", getLocale()); //$NON-NLS-1$
		if (zipIn == null) {
			return;
		}
		setInconsistent(true);
		cleanOldIndex();
		byte[] buf = new byte[8192];
		File destDir = indexDir;

		try (ZipInputStream zis = new ZipInputStream(zipIn)) {
			ZipEntry zEntry;
			while ((zEntry = zis.getNextEntry()) != null) {
				String name = zEntry.getName();
				File file = new File(destDir, name);
				if (!file.toPath().normalize().startsWith(destDir.toPath().normalize())) {
					throw new RuntimeException("Bad zip entry: " + name); //$NON-NLS-1$
				}
				// if it is empty directory, create it
				if (zEntry.isDirectory()) {
					file.mkdirs();
					continue;
				}
				// if it is a file, extract it
				String filePath = name;
				int lastSeparator = filePath.lastIndexOf("/"); //$NON-NLS-1$
				String fileDir = ""; //$NON-NLS-1$
				if (lastSeparator >= 0) {
					fileDir = filePath.substring(0, lastSeparator);
				}
				// create directory for a file
				new File(destDir, fileDir).mkdirs();
				// write file
				File outFile = new File(destDir, filePath);
				try (FileOutputStream fos = new FileOutputStream(outFile)) {
					int n = 0;
					while ((n = zis.read(buf)) >= 0) {
						fos.write(buf, 0, n);
					}
				}
			}
			setInconsistent(false);
		} catch (IOException ioe) {
		} finally {
			try {
				zipIn.close();
			} catch (IOException ioe) {
			}
		}
	}

	/**
	 * Cleans any old index and Lucene lock files by initializing a new index.
	 */
	private void cleanOldIndex() {
		try (LimitTokenCountAnalyzer analyzer = new LimitTokenCountAnalyzer(analyzerDescriptor.getAnalyzer(), 10000);
				IndexWriter cleaner = new IndexWriter(luceneDirectory,
						new IndexWriterConfig(analyzer)
								.setOpenMode(OpenMode.CREATE))) {

		} catch (IOException ioe) {
		}
	}

	/**
	 * Returns true when the index must be updated.
	 */
	public synchronized boolean needsUpdating() {
		if (!exists()) {
			return true;
		}
		return getDocPlugins().detectChange();
	}

	/**
	 * @return Returns the tocManager.
	 */
	public TocManager getTocManager() {
		return tocManager;
	}

	private void registerSearch(Thread t) {
		synchronized (searches) {
			searches.add(t);
		}
	}

	private void unregisterSearch(Thread t) {
		synchronized (searches) {
			searches.remove(t);
		}
	}

	/**
	 * @return Returns the closed.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * @return true if lock obtained for this Eclipse instance
	 * @throws OverlappingFileLockException
	 *             if lock already obtained
	 */
	@SuppressWarnings("resource")
	public synchronized boolean tryLock() throws OverlappingFileLockException {
		if ("none".equals(System.getProperty("osgi.locking"))) {  //$NON-NLS-1$//$NON-NLS-2$
			return true; // Act as if lock succeeded
		}
		if (lock != null) {
			throw new OverlappingFileLockException();
		}
		File lockFile = getLockFile();
		lockFile.getParentFile().mkdirs();
		try {
			raf = new RandomAccessFile(lockFile, "rw"); //$NON-NLS-1$
			FileLock l = raf.getChannel().tryLock();
			if (l != null) {
				// The RandomAccessFile raf cannot be closed yet because closing it will release the
				// lock. It will be closed when the lock is released.
				lock = l;
				return true;
			}
			logLockFailure(null);
		} catch (IOException ioe) {
			lock = null;
			logLockFailure(ioe);
		}
		if ( raf != null ) {
			try {
				raf.close();
			} catch (IOException e) {
			}
			raf = null;
		}
		return false;
	}

	private static boolean errorReported = false;

	private void logLockFailure(IOException ioe) {
		if (!errorReported) {
			ILog.of(getClass()).error("Unable to Lock Help Search Index", ioe); //$NON-NLS-1$
			errorReported = true;
		}
	}

	private File getLockFile() {
		return new File(indexDir.getParentFile(), locale + ".lock"); //$NON-NLS-1$
	}

	/**
	 * Deletes the lock file. The lock must be released prior to this call.
	 *
	 * @return <code>true</code> if the file has been deleted, <code>false</code> otherwise.
	 */

	public synchronized boolean deleteLockFile() {
		if (lock != null)
			return false;
		File lockFile = getLockFile();
		if (lockFile.exists())
			return lockFile.delete();
		return true;
	}

	public synchronized void releaseLock() {
		if (lock != null) {
			try {
				lock.channel().close();
			} catch (IOException ioe) {
			}
			lock = null;
		}
		if (raf != null ) {
			try {
				raf.close();
			} catch (IOException ioe) {
			}
			raf = null;
		}
	}

	public static String getIndexableHref(String url) {
		String fileName = url.toLowerCase(Locale.ENGLISH);
		if (fileName.endsWith(".htm") //$NON-NLS-1$
				|| fileName.endsWith(".html") //$NON-NLS-1$
				|| fileName.endsWith(".xhtml") //$NON-NLS-1$
				|| fileName.endsWith(".xml") //$NON-NLS-1$
				|| fileName.endsWith(".txt")) { //$NON-NLS-1$
			// indexable
		} else if (fileName.contains(".htm#") //$NON-NLS-1$
				|| fileName.contains(".html#") //$NON-NLS-1$
				|| fileName.contains(".xhtml#") //$NON-NLS-1$
				|| fileName.contains(".xml#")) { //$NON-NLS-1$
			url = url.substring(0, url.lastIndexOf('#'));
			// its a fragment, index whole document
		} else {
			// try search participants
			return BaseHelpSystem.getLocalSearchManager().isIndexable(url) ? url : null;
		}
		return url;
	}

	/**
	 * Checks if document is indexable, and creates a URL to obtain contents.
	 *
	 * @param locale
	 * @param url
	 *            specified in the navigation
	 * @return URL to obtain document content or null
	 */
	public static URL getIndexableURL(String locale, String url) {
		return getIndexableURL(locale, url, null, null);
	}

	/**
	 * Checks if document is indexable, and creates a URL to obtain contents.
	 *
	 * @param locale
	 * @param url
	 * @param participantId
	 *            the search participant or <code>null</code> specified in the navigation
	 * @return URL to obtain document content or null
	 */
	public static URL getIndexableURL(String locale, String url, String id, String participantId) {
		if (participantId == null)
			url = getIndexableHref(url);
		if (url == null)
			return null;

		try {
			StringBuilder query = new StringBuilder();
			query.append("?"); //$NON-NLS-1$
			query.append("lang=" + locale); //$NON-NLS-1$
			if (id != null)
				query.append("&id=" + id); //$NON-NLS-1$
			if (participantId != null)
				query.append("&participantId=" + participantId); //$NON-NLS-1$
			return new URL("localhelp", //$NON-NLS-1$
					null, -1, url + query.toString(), HelpURLStreamHandler.getDefault());

		} catch (MalformedURLException mue) {
			return null;
		}
	}

	public IStatus addDocument(String pluginId, String name, URL url, String id, Document doc) {
		// try a registered participant for the file format
		SearchParticipant participant = BaseHelpSystem.getLocalSearchManager()
				.getParticipant(pluginId, name);
		if (participant != null) {
			try {
				return participant.addDocument(this, pluginId, name, url, id, new LuceneSearchDocument(doc));
			}
			catch (Throwable t) {
				return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR,
						"Error while adding document to search participant (addDocument()): " //$NON-NLS-1$
						+ name + ", " + url + "for participant " + participant.getClass().getName(), t); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		// default to html
		return htmlSearchParticipant.addDocument(this, pluginId, name, url, id, new LuceneSearchDocument(doc));
	}

	@Override
	public IStatus addSearchableDocument(String pluginId, String name, URL url, String id, ISearchDocument doc) {
		// In the help system the only class that implements ISearchDocument is LuceneSearchDocument
		LuceneSearchDocument luceneDoc = (LuceneSearchDocument)doc;
		return addDocument(pluginId, name, url, id, luceneDoc.getDocument());
	}
}
