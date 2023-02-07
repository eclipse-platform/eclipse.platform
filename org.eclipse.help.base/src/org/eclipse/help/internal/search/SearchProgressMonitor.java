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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;

/**
 * Progress monitor for search
 *
 * @since 2.0
 */
public class SearchProgressMonitor implements IProgressMonitor {

	// Progress monitors, indexed by locale
	protected static Map<String, SearchProgressMonitor> progressMonitors = new HashMap<>();

	// Dummy collector for triggering a progress monitor
	protected static ISearchHitCollector dummy_collector;

	private boolean started, done, canceled;

	private int totalWork = IProgressMonitor.UNKNOWN;

	private double currWork;

	static {
		dummy_collector = new ISearchHitCollector() {
			@Override
			public void addHits(List<SearchHit> hits, String s) {
			}

			@Override
			public void addQTCException(QueryTooComplexException exception) throws QueryTooComplexException {
				throw exception;
			}
		};
	}

	/**
	 * Constructor.
	 */
	public SearchProgressMonitor() {
		started = done = canceled = false;
	}

	@Override
	public void beginTask(String name, int totalWork) {
		this.totalWork = totalWork;
		this.started = true;
	}

	@Override
	public void done() {
		currWork = totalWork;
		this.done = true;
		this.started = true;
	}

	@Override
	public void setTaskName(String name) {
	}

	@Override
	public void subTask(String name) {
	}

	@Override
	public void worked(int work) {
		internalWorked(work);
	}

	@Override
	public void internalWorked(double work) {
		currWork += work;
		if (currWork > totalWork)
			currWork = totalWork;
		else if (currWork < 0)
			currWork = 0;
	}

	public int getPercentage() {
		if (done) {
			return 100;
		}
		if (totalWork == IProgressMonitor.UNKNOWN)
			return 0;
		if (currWork >= totalWork)
			return 100;
		return (int)(100 * currWork / totalWork);
	}

	/**
	 * Gets the isCancelled.
	 *
	 * @return Returns a boolean
	 */
	@Override
	public boolean isCanceled() {
		return canceled;
	}

	/**
	 * Sets the isStarted.
	 */
	public void started() {
		this.started = true;
	}

	/**
	 * Gets the isStarted.
	 *
	 * @return Returns a boolean
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 * Gets the isDone.
	 *
	 * @return Returns a boolean
	 */
	public boolean isDone() {
		return done;
	}

	/**
	 * Sets the isCanceled.
	 *
	 * @param canceled
	 *            The isCanceled to set
	 */
	@Override
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	/**
	 * Returns a progress monitor for specified query and locale
	 */
	public static synchronized SearchProgressMonitor getProgressMonitor(
			final String locale) {

		// return an existing progress monitor if there is one
		if (progressMonitors.get(locale) != null)
			return progressMonitors.get(locale);

		final SearchProgressMonitor pm = new SearchProgressMonitor();
		progressMonitors.put(locale, pm);

		// spawn a thread that will cause indexing if needed
		Thread indexer = new Thread(() -> {
			try {
				BaseHelpSystem.getSearchManager().search(new DummySearchQuery(locale), dummy_collector, pm);
			} catch (OperationCanceledException oce) {
				// operation cancelled
				// throw out the progress monitor
				progressMonitors.remove(locale);
			} catch (Exception e) {
				progressMonitors.remove(locale);
				if (HelpBasePlugin.getDefault() != null) {
					Platform.getLog(SearchProgressMonitor.class)
							.error(
							"Problem occurred during indexing of documentation.", e); //$NON-NLS-1$
				} else {
					// Plugin has been shut down
				}
			}
		});
		indexer.setName("HelpSearchIndexer"); //$NON-NLS-1$
		indexer.start();
		// give pm chance to start
		// this will avoid seing progress if there is no work to do
		while (!pm.isStarted()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException ie) {
			}
			if (progressMonitors.get(locale) == null)
				// operation got canceled
				break;
		}

		return pm;
	}

	static class DummySearchQuery implements ISearchQuery {
		private String l;

		DummySearchQuery(String loc) {
			l = loc;
		}

		/**
		 * Obtains names of fields in addition to default field
		 */
		@Override
		public Collection<String> getFieldNames() {
			return new ArrayList<>();
		}

		/**
		 * Obtains search word (user query)
		 */
		@Override
		public String getSearchWord() {
			return "dummy"; //$NON-NLS-1$
		}

		/**
		 * @return true if search only in specified fields, not the default
		 *         field
		 */
		@Override
		public boolean isFieldSearch() {
			return false;
		}

		/**
		 * Obtains locale
		 */
		@Override
		public String getLocale() {
			return l;
		}
	}

	public synchronized static void reinit(String locale) {
		progressMonitors.remove(locale);
	}

}
