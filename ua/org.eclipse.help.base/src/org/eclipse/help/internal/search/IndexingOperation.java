/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.search;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.HelpBaseResources;
import org.eclipse.help.internal.base.util.HelpProperties;
import org.eclipse.help.internal.protocols.HelpURLConnection;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.internal.toc.TocFileProvider;
import org.eclipse.help.search.SearchParticipant;

/**
 * Indexing Operation represents a long operation, which performs indexing of
 * the group (Collection) of documents. It is used Internally by SlowIndex and
 * returned by its getIndexUpdateOperation() method.
 */
class IndexingOperation {

	private static final String ELEMENT_NAME_INDEX = "index"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_PATH = "path"; //$NON-NLS-1$

	private int numRemoved;
	private SearchIndex index = null;

	/**
	 * Construct indexing operation.
	 *
	 * @param ix
	 *            ISearchIndex already opened
	 */
	public IndexingOperation(SearchIndex ix) {
		this.index = ix;
	}

	private void checkCancelled(IProgressMonitor pm)
			throws OperationCanceledException {
		if (pm.isCanceled())
			throw new OperationCanceledException();
	}

	/**
	 * Executes indexing, given the progress monitor.
	 *
	 * @param pm
	 *            progres monitor to be used during this long operation for
	 *            reporting progress
	 * @throws OperationCanceledException
	 *             if indexing was cancelled
	 */
	protected void execute(IProgressMonitor pm)
			throws OperationCanceledException, IndexingException {
		checkCancelled(pm);
		Collection<URL> staleDocs = getRemovedDocuments(index);
		numRemoved = staleDocs.size();
		Collection<URL> newDocs = getAddedDocuments(index);
		int numAdded = newDocs.size();

		// if collection is empty, we may return right away
		// need to check if we have to do anything to the progress monitor
		if (numRemoved + numAdded <= 0) {
			pm.done();
			BaseHelpSystem.getLocalSearchManager().clearSearchParticipants();
			return;
		}
		SubMonitor subMonitor = SubMonitor.convert(pm, HelpBaseResources.UpdatingIndex, numRemoved + 10 * numAdded);

		// 1. remove all documents for plugins changed (including change in a
		// fragment)
		removeStaleDocuments(subMonitor.split(numRemoved), staleDocs);
		checkCancelled(pm);
		// 2. merge prebult plugin indexes and addjust
		addNewDocuments(subMonitor.split(10 * numAdded), newDocs, staleDocs.isEmpty());

		pm.done();
		BaseHelpSystem.getLocalSearchManager().clearSearchParticipants();
	}

	private Map<String, String[]> calculateNewToRemove(Collection<URL> newDocs, Map<String, String[]> prebuiltDocs) {
		// Calculate document that were in prebuilt indexes, but are not in
		// TOCs. (prebuiltDocs - newDocs)
		/*
		 * Map. Keys are /pluginid/href of docs to delete. Values are null to
		 * delete completely, or String[] of indexIds with duplicates of the
		 * document
		 */
		Map<String, String[]> docsToDelete = prebuiltDocs;
		ArrayList<String> prebuiltHrefs = new ArrayList<>(prebuiltDocs.keySet());
		for (String href : prebuiltHrefs) {
			URL u = SearchIndex.getIndexableURL(index.getLocale(), href);
			if (u == null) {
				// should never be here
				docsToDelete.put(href, null);
			}
			if (newDocs.contains(u)) {
				// delete duplicates only
				if (docsToDelete.get(href) != null) {
					// duplicates exist, leave map entry as is
				} else {
					// no duplicates, do not delete
					docsToDelete.remove(href);
				}
			} else {
				// document should not be indexed at all (TOC not built)
				// delete completely, not just duplicates
				docsToDelete.put(href, null);
			}
		}
		return docsToDelete;
	}

	/**
	 * Returns documents that must be deleted
	 */
	private Map<String, String[]> addNewDocuments(IProgressMonitor pm, Collection<URL> newDocs,
			boolean opened) throws IndexingException {
		Map<String, String[]> prebuiltDocs = mergeIndexes(pm, opened);
		checkCancelled(pm);
		Collection<URL> docsToIndex = calculateDocsToAdd(newDocs, prebuiltDocs);
		checkCancelled(pm);
		Map<String, String[]> docsToDelete = calculateNewToRemove(newDocs, prebuiltDocs);
		SubMonitor subMonitor = SubMonitor.convert(pm, 10 * docsToIndex.size() + docsToDelete.size());
		checkCancelled(pm);
		addDocuments(subMonitor.split(10 * docsToIndex.size()), docsToIndex, docsToDelete.isEmpty());
		checkCancelled(pm);
		removeNewDocuments(subMonitor.split(docsToDelete.size()), docsToDelete);
		pm.done();
		return docsToDelete;
	}

	private Collection<URL> calculateDocsToAdd(Collection<URL> newDocs, Map<String, String[]> prebuiltDocs) {
		// Calculate documents that were not in prebuilt indexes, and still need
		// to be added
		// (newDocs minus prebuiltDocs)
		Collection<URL> docsToIndex = null;
		int newDocSize = newDocs.size();
		if (prebuiltDocs.size() > 0) {
			docsToIndex = new HashSet<>(newDocs);
			for (String href : prebuiltDocs.keySet()) {
				URL u = SearchIndex.getIndexableURL(index.getLocale(), href);
				if (u != null) {
					docsToIndex.remove(u);
				}
			}
		} else {
			docsToIndex = newDocs;
		}
		if (HelpPlugin.DEBUG_SEARCH) {
			System.out.println("Building search index-  new docs: " + newDocSize +  //$NON-NLS-1$
					", preindexed: " + prebuiltDocs.size() + ", remaining: " + docsToIndex.size()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return docsToIndex;
	}

	/**
	 * @param docsToDelete
	 *            Keys are /pluginid/href of all merged Docs. Values are null to
	 *            delete href, or String[] of indexIds to delete duplicates with
	 *            given index IDs
	 */
	private void removeNewDocuments(IProgressMonitor pm, Map<String, String[]> docsToDelete)
			throws IndexingException {
		if (docsToDelete.isEmpty()) {
			return;
		}
		pm = new LazyProgressMonitor(pm);
		pm.beginTask("", docsToDelete.size()); //$NON-NLS-1$
		checkCancelled(pm);
		Set<String> keysToDelete = docsToDelete.keySet();
		if (!index.beginRemoveDuplicatesBatch()) {
			throw new IndexingException();
		}
		MultiStatus multiStatus = null;
		for (String href : keysToDelete) {
			String[] indexIds = docsToDelete.get(href);
			if (indexIds == null) {
				// delete all copies
				index.removeDocument(href);
				continue;
			}
			IStatus status = index.removeDuplicates(href, indexIds);
			if (status.getCode() != IStatus.OK) {
				if (multiStatus == null) {
					multiStatus = new MultiStatus(
							HelpBasePlugin.PLUGIN_ID,
							IStatus.WARNING,
							"Some help documents could not removed from index.", //$NON-NLS-1$
							null);
				}
				multiStatus.add(status);
			}
			checkCancelled(pm);
			pm.worked(1);
			if (multiStatus != null) {
				Platform.getLog(getClass()).log(multiStatus);
			}
		}
		if (!index.endRemoveDuplicatesBatch()) {
			throw new IndexingException();
		}
		pm.done();
	}

	private void addDocuments(IProgressMonitor pm, Collection<URL> addedDocs,
			boolean lastOperation) throws IndexingException {
		pm = new LazyProgressMonitor(pm);
		// beginAddBatch()) called when processing prebuilt indexes
		pm.beginTask("", addedDocs.size()); //$NON-NLS-1$
		checkCancelled(pm);
		pm.subTask(HelpBaseResources.UpdatingIndex);
		MultiStatus multiStatus = null;
		for (URL doc : addedDocs) {
			IStatus status = index.addDocument(getName(doc), doc);
			if (status.getCode() != IStatus.OK) {
				if (multiStatus == null) {
					multiStatus = new MultiStatus(
							HelpBasePlugin.PLUGIN_ID,
							IStatus.ERROR,
							"Help documentation could not be indexed completely."); //$NON-NLS-1$
				}
				multiStatus.add(status);
			}
			checkCancelled(pm);
			pm.worked(1);
		}
		if (multiStatus != null) {
			Platform.getLog(getClass()).log(multiStatus);
		}
		pm.subTask(HelpBaseResources.Writing_index);
		if (!index.endAddBatch(addedDocs.size() > 0, lastOperation))
			throw new IndexingException();
		pm.done();
	}

	private void removeStaleDocuments(IProgressMonitor pm,
			Collection<URL> removedDocs) throws IndexingException {
		pm = new LazyProgressMonitor(pm);
		pm.beginTask("", removedDocs.size()); //$NON-NLS-1$
		pm.subTask(HelpBaseResources.Preparing_for_indexing);
		checkCancelled(pm);

		if (numRemoved > 0) {
			if (!index.beginDeleteBatch()) {
				throw new IndexingException();
			}
			checkCancelled(pm);
			pm.subTask(HelpBaseResources.UpdatingIndex);
			MultiStatus multiStatus = null;
			for (URL doc : removedDocs) {
				IStatus status = index.removeDocument(getName(doc));
				if (status.getCode() != IStatus.OK) {
					if (multiStatus == null) {
						multiStatus = new MultiStatus(
								HelpBasePlugin.PLUGIN_ID,
								IStatus.WARNING,
								"Uninstalled or updated help documents could not be removed from index.", //$NON-NLS-1$
								null);
					}
					multiStatus.add(status);
				}
				checkCancelled(pm);
				pm.worked(1);
			}
			if (multiStatus != null) {
				Platform.getLog(getClass()).log(multiStatus);
			}
			if (!index.endDeleteBatch()) {
				throw new IndexingException();
			}
		}
		pm.done();
	}

	/**
	 * Returns the document identifier. Currently we use the document file name
	 * as identifier.
	 */
	private String getName(URL doc) {
		String name = doc.getFile();
		// remove query string if any
		int i = name.indexOf('?');
		if (i != -1)
			name = name.substring(0, i);
		return name;
	}

	public static class IndexingException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Returns IDs of plugins which need docs added to index.
	 */
	private Collection<String> getAddedPlugins(SearchIndex index) {
		// Get the list of added plugins
		Collection<String> addedPlugins = index.getDocPlugins().getAdded();

		if (addedPlugins == null || addedPlugins.isEmpty())
			return new ArrayList<>(0);
		return addedPlugins;
	}

	/**
	 * Returns the documents to be added to index. The collection consists of
	 * the associated PluginURL objects.
	 */
	private Collection<URL> getAddedDocuments(SearchIndex index) {
		// Get the list of added plugins
		Collection<String> addedPlugins = getAddedPlugins(index);
		if (HelpPlugin.DEBUG_SEARCH) {
			traceAddedContributors(addedPlugins);
		}
		// get the list of all navigation urls.
		Set<String> urls = getAllDocuments(index.getLocale());
		Set<URL> addedDocs = new HashSet<>(urls.size());
		for (String doc : urls) {
			// Assume the url is /pluginID/path_to_topic.html
			if (doc.startsWith("//")) { //$NON-NLS-1$  Bug 225592
				doc = doc.substring(1);
			}
			int i = doc.indexOf('/', 1);
			String plugin = i == -1 ? "" : doc.substring(1, i); //$NON-NLS-1$
			if (!addedPlugins.contains(plugin)) {
				continue;
			}

			URL url = SearchIndex.getIndexableURL(index.getLocale(), doc);
			if (url != null) {
				addedDocs.add(url);
			}
		}
		//Add documents from global search participants
		SearchParticipant[] participants = BaseHelpSystem.getLocalSearchManager().getGlobalParticipants();
		for (SearchParticipant participant : participants) {
			String participantId;
			try {
				participantId = participant.getId();
			}
			catch (Throwable t) {
				// log the error and skip this participant
				Platform.getLog(getClass()).error(
						"Failed to get help search participant id for: " //$NON-NLS-1$
						+ participant.getClass().getName() + "; skipping this one.", t); //$NON-NLS-1$
				continue;
			}
			Set<String> set;
			try {
				set = participant.getAllDocuments(index.getLocale());
			}
			catch (Throwable t) {
				// log the error and skip this participant
				Platform.getLog(getClass())
						.error("Failed to retrieve documents from one of the help search participants: " //$NON-NLS-1$
						+ participant.getClass().getName() + "; skipping this one.", t); //$NON-NLS-1$
				continue;
			}

			for (String doc : set) {
				String id = null;
				int qloc = doc.indexOf('?');
				if (qloc!= -1) {
					String query = doc.substring(qloc+1);
					doc = doc.substring(0, qloc);
					HashMap<String, Object> arguments = new HashMap<>();
					HelpURLConnection.parseQuery(query, arguments);
					id = (String)arguments.get("id"); //$NON-NLS-1$
				}
				// Assume the url is /pluginID/path_to_topic.html
				int i = doc.indexOf('/', 1);
				String plugin = i == -1 ? "" : doc.substring(1, i); //$NON-NLS-1$
				if (!addedPlugins.contains(plugin)) {
					continue;
				}

				URL url = SearchIndex.getIndexableURL(index.getLocale(), doc, id, participantId);
				if (url != null) {
					addedDocs.add(url);
				}
			}
		}
		return addedDocs;
	}

	private void traceAddedContributors(Collection<String> addedContributors) {
		for (String id : addedContributors) {
			System.out.println("Updating search index for contributor :" + id); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the documents to be removed from index. The collection consists
	 * of the associated PluginURL objects.
	 */
	private Collection<URL> getRemovedDocuments(SearchIndex index) {
		// Get the list of removed plugins
		Collection<String> removedPlugins = index.getDocPlugins().getRemoved();
		if (removedPlugins == null || removedPlugins.isEmpty())
			return new ArrayList<>(0);
		// get the list of indexed docs. This is a hashtable (url, plugin)
		HelpProperties indexedDocs = index.getIndexedDocs();
		Set<URL> removedDocs = new HashSet<>(indexedDocs.size());
		for (Object name : indexedDocs.keySet()) {
			String doc = (String) name;
			// Assume the url is /pluginID/path_to_topic.html
			int i = doc.indexOf('/', 1);
			String plugin = i == -1 ? "" : doc.substring(1, i); //$NON-NLS-1$
			if (!removedPlugins.contains(plugin)) {
				continue;
			}

			URL url = SearchIndex.getIndexableURL(index.getLocale(), doc);
			if (url != null) {
				removedDocs.add(url);
			}
		}
		return removedDocs;
	}

	/**
	 * Adds the topic and its subtopics to the list of documents
	 */
	private void add(ITopic topic, Set<String> hrefs) {
		String href = topic.getHref();
		add(href, hrefs);
		ITopic[] subtopics = topic.getSubtopics();
		for (ITopic subtopic : subtopics)
			add(subtopic, hrefs);
	}

	private void add(String href, Set<String> hrefs) {
		if (href != null
				&& !href.isEmpty() && !href.startsWith("http://") && !href.startsWith("https://")) //$NON-NLS-1$ //$NON-NLS-2$
			hrefs.add(href);
	}

	/**
	 * Returns the collection of href's for all the help topics.
	 */
	private Set<String> getAllDocuments(String locale) {
		// Add documents from TOCs
		HashSet<String> hrefs = new HashSet<>();
		Toc[] tocs = index.getTocManager().getTocs(locale);
		for (Toc toc : tocs) {
			ITopic[] topics = toc.getTopics();
			for (ITopic topic : topics) {
				add(topic, hrefs);
			}
			ITocContribution contrib = toc.getTocContribution();
			String[] extraDocs = contrib.getExtraDocuments();
			for (String extraDoc : extraDocs) {
				add(extraDoc, hrefs);
			}
			ITopic tocDescriptionTopic = toc.getTopic(null);
			if (tocDescriptionTopic != null)
				add(tocDescriptionTopic, hrefs);
		}
		return hrefs;
	}

	/**
	 * Obtains PluginIndexes pointing to prebuilt indexes
	 *
	 * @param pluginIds
	 * @param locale
	 * @return
	 */
	private PrebuiltIndexes getIndexesToAdd(Collection<String> pluginIds) {
		PrebuiltIndexes indexes = new PrebuiltIndexes(index);

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(TocFileProvider.EXTENSION_POINT_ID_TOC);

		for (IConfigurationElement elem : elements) {
			try {
				if (elem.getName().equals(ELEMENT_NAME_INDEX)) {
					String pluginId = elem.getNamespaceIdentifier();
					if (pluginIds.contains(pluginId)) {
						String path = elem.getAttribute(ATTRIBUTE_NAME_PATH);
						if (path != null) {
							indexes.add(pluginId, path);
							if (HelpPlugin.DEBUG_SEARCH) {
								System.out.println("Search index for " + pluginId + " is prebuilt with path \"" + path + '"');  //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
						else {
							String msg = "Element \"index\" in extension of \"org.eclipse.help.toc\" must specify a \"path\" attribute (plug-in: " + pluginId + ")"; //$NON-NLS-1$ //$NON-NLS-2$
							Platform.getLog(getClass()).error(msg, null);
						}
					}
				}
			}
			catch (InvalidRegistryObjectException e) {
				// ignore this extension; move on
			}
		}
		return indexes;
	}

	private Map<String, String[]> mergeIndexes(IProgressMonitor monitor, boolean opened)
			throws IndexingException {
		Collection<String> addedPluginIds = getAddedPlugins(index);
		PrebuiltIndexes indexes = getIndexesToAdd(addedPluginIds);
		PluginIndex[] pluginIndexes = indexes.getIndexes();
		Map<String, String[]> mergedDocs = null;
		// Always perform add batch to ensure that index is created and saved
		// even if no new documents
		if (!index.beginAddBatch(opened)) {
			throw new IndexingException();
		}
		if (pluginIndexes.length > 0) {
			mergedDocs = index.merge(pluginIndexes, monitor);
		}

		if (mergedDocs == null) {
			return Collections.EMPTY_MAP;
		}
		return mergedDocs;
	}

}