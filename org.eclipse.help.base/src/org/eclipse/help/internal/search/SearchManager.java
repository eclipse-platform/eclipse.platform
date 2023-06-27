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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.help.internal.base.remote.RemoteHelp;
import org.eclipse.help.internal.base.remote.RemoteSearchManager;
import org.eclipse.help.internal.search.federated.FederatedSearchEntry;
import org.eclipse.help.internal.search.federated.FederatedSearchJob;
import org.eclipse.help.search.AbstractSearchProcessor;
import org.eclipse.help.search.ISearchResult;

/*
 * Manages both local and remote searching, as well as merging of results.
 */
public class SearchManager {

	private LocalSearchManager localManager = new LocalSearchManager();
	private RemoteSearchManager remoteManager = new RemoteSearchManager();

	private class SearchState {

		public IProgressMonitor localMonitor;
		public IProgressMonitor remoteMonitor;

		public ISearchQuery searchQuery;
		public BufferedSearchHitCollector bufferedCollector = new BufferedSearchHitCollector();

		public Job localSearchJob;
		public Job remoteSearchJob;

		public SearchState() {
			/*
			 * We use these jobs to perform the local and remote searches in parallel in the
			 * background.
			 */
			localSearchJob = new Job("localSearchJob") { //$NON-NLS-1$

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					localManager.search(searchQuery, bufferedCollector, localMonitor);
					return Status.OK_STATUS;
				}
			};
			remoteSearchJob = new Job("remoteSearchJob") { //$NON-NLS-1$

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					remoteManager.search(searchQuery, bufferedCollector, remoteMonitor);
					return Status.OK_STATUS;
				}
			};
			localSearchJob.setSystem(true);
			remoteSearchJob.setSystem(true);
		}
	}

	/*
	 * Constructs a new SearchManager.
	 */
	public SearchManager() {

	}

	/*
	 * Perform the given search both locally and remotely if configured.
	 */
	public void search(ISearchQuery searchQuery, ISearchHitCollector collector, IProgressMonitor pm)
			throws QueryTooComplexException {
		if (RemoteHelp.isEnabled()) {
			searchLocalAndRemote(searchQuery, collector, pm);
		}
		else {
			searchLocal(searchQuery, collector, pm);
		}
	}

	/*
	 * Perform the given search locally only.
	 */
	public void searchLocal(ISearchQuery searchQuery, ISearchHitCollector collector, IProgressMonitor pm)
			throws QueryTooComplexException {
		localManager.search(searchQuery, collector, pm);
	}

	/*
	 * Perform the given search both locally and remotely.
	 */
	public void searchLocalAndRemote(ISearchQuery searchQuery, ISearchHitCollector collector, IProgressMonitor pm)
			throws QueryTooComplexException {
		SearchState state = new SearchState();
		state.searchQuery = searchQuery;

		SubMonitor subMonitor = SubMonitor.convert(pm, 100);

		// allocate half of the progress bar for each
		state.localMonitor = subMonitor.split(50, SubMonitor.SUPPRESS_SUBTASK);
		state.remoteMonitor = subMonitor.split(50, SubMonitor.SUPPRESS_SUBTASK);

		// start both searches in parallel
		state.localSearchJob.schedule();
		state.remoteSearchJob.schedule();

		// wait until finished
		try {
			state.localSearchJob.join();
			state.remoteSearchJob.join();
		}
		catch (InterruptedException e) {
			String msg = "Unexpected InterruptedException while waiting for help search jobs to finish"; //$NON-NLS-1$
			ILog.of(getClass()).error(msg, e);
		}

		// results are in; send them off to the collector
		state.bufferedCollector.flush(collector);
		pm.done();
	}

	/**
	 * Performs the federated search.
	 */
	public void search(String expression, FederatedSearchEntry[] entries) {
		for (int i = 0; i < entries.length; i++) {
			FederatedSearchJob job = new FederatedSearchJob(expression, entries[i]);
			job.schedule();
		}
	}

	/*
	 * Returns the manager responsible for handling local searching.
	 */
	public LocalSearchManager getLocalSearchManager() {
		return localManager;
	}

	/*
	 * Returns the manager responsible for handling remote searching.
	 */
	public RemoteSearchManager getRemoteSearchManager() {
		return remoteManager;
	}

	/*
	 * Performs any necessary cleanup (workbench is shutting down).
	 */
	public void close() {
		localManager.close();
	}

	/*
	 * Gets the list of registered search processors
	 */
	public static AbstractSearchProcessor[] getSearchProcessors()
	{
		IConfigurationElement[] configs =
			Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.help.base.searchProcessor"); //$NON-NLS-1$

		ArrayList<Object> processors = new ArrayList<>();

		for (int c=0;c<configs.length;c++)
		{
			try {
				processors.add(
						configs[c].createExecutableExtension("class"));//$NON-NLS-1$
			} catch (CoreException e) {}
		}

		return processors.toArray(new AbstractSearchProcessor[processors.size()]);
	}

	/*
	 * Convert Lucene SearchHits to ISearchResults
	 */
	public static ISearchResult[] convertHitsToResults(SearchHit hits[]) {

		ISearchResult results[] = new ISearchResult[hits.length];
		for (int r=0;r<results.length;r++)
		{
			SearchResult result = new SearchResult();
			if (hits[r].getHref()!=null)
				result.setHref(hits[r].getHref());
			if (hits[r].getId()!=null)
				result.setId(hits[r].getId());
			if (hits[r].getParticipantId()!=null)
				result.setParticipantId(hits[r].getParticipantId());
			if (hits[r].getDescription()!=null)
				result.setDescription(hits[r].getDescription());
			if (hits[r].getLabel()!=null)
				result.setLabel(hits[r].getLabel());
			if (hits[r].getSummary()!=null)
				result.setSummary(hits[r].getSummary());
			if (hits[r].getToc()!=null)
				result.setToc(hits[r].getToc());
			if (hits[r].getIconURL()!=null)
				result.setIcon(hits[r].getIconURL());
			result.setScore(hits[r].getScore());
			result.setPotentialHit(hits[r].isPotentialHit());
			results[r] = result;
		}
		return results;
	}

	/*
	 * Convert ISearchResults to SearchHits
	 */
	public static SearchHit[] convertResultsToHits(ISearchResult[] results) {

		SearchHit hits[] = new SearchHit[results.length];
		for (int r=0;r<results.length;r++)
		{
			hits[r] = new SearchHit(
					results[r].getHref(),
					results[r].getLabel(),
					results[r].getSummary(),
					results[r].getScore(),
					results[r].getToc(),
					results[r].getId(),
					results[r].getParticipantId(),
					results[r].isPotentialHit());
		}
		return hits;
	}


	/*
	 * Buffers hits, and only sends them off to the wrapped collector
	 * when flush() is called.
	 */
	private static class BufferedSearchHitCollector implements ISearchHitCollector {
		private Set<SearchHit> allHits = new HashSet<>();
		private String wordsSearched = null;

		@Override
		public void addHits(List<SearchHit> hits, String wordsSearched) {
			if (wordsSearched != null) {
				this.wordsSearched = wordsSearched;
			}
			allHits.addAll(hits);
		}

		/*
		 * Send all the buffered hits to the underlying collector,
		 * and reset the buffers.
		 */
		public void flush(ISearchHitCollector collector) {
			// sort by score
			List<SearchHit> hitsList = new ArrayList<>(allHits);
			hitsList.sort(null);
			collector.addHits(hitsList, wordsSearched);
			allHits.clear();
			wordsSearched = null;
		}

		@Override
		public void addQTCException(QueryTooComplexException exception) throws QueryTooComplexException {
			throw exception;
		}
	}
}
