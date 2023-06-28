/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.internal.ui.history;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.model.ITestRunSessionListener;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.model.ITestRunSession;

import org.eclipse.core.runtime.CoreException;

/**
 * A test run sessions history object
 */
public class History implements ITestRunSessionListener {

	private static final String HISTORY_DIR_NAME = "history"; //$NON-NLS-1$

	/**
	 * An instance of test run sessions history object
	 */
	public static final History INSTANCE = new History();

	private History() {
	}

	private boolean wasRead = false;
	private List<HistoryItem> items = new ArrayList<>();

	/**
	 * Creates and returns a directory to store the History information
	 *
	 * @return the file corresponding to History directory
	 * @throws IllegalStateException in case of failed to create or find an existing
	 *                               directory
	 */
	public File getDirectory() throws IllegalStateException {
		File historyDir = UnitTestPlugin.getDefault().getStateLocation().append(HISTORY_DIR_NAME).toFile();
		if (!historyDir.isDirectory()) {
			historyDir.mkdir();
		}
		return historyDir;
	}

	/**
	 * Returns a list of history items
	 *
	 * @return a list of history items
	 */
	public List<HistoryItem> getHistory() {
		if (!wasRead) {
			Arrays.stream(getDirectory().listFiles()).map(HistoryItem::new).forEach(items::add);
			wasRead = true;
		}
		return Collections.unmodifiableList(items);
	}

	/**
	 * Clears the history
	 */
	public void clear() {
		for (HistoryItem item : items) {
			try {
				item.removeSwapFile();
			} catch (IOException e) {
				UnitTestPlugin.log(e);
			}
		}
		items.clear();
	}

	@Override
	public void sessionAdded(ITestRunSession testRunSession) {
		items.add(new HistoryItem((TestRunSession) testRunSession));
	}

	@Override
	public void sessionRemoved(ITestRunSession testRunSession) {
		items.stream().filter(item -> item.getCurrentTestRunSession().filter(testRunSession::equals).isPresent())
				.forEach(toRemove -> {
					try {
						toRemove.removeSwapFile();
					} catch (IOException e) {
						UnitTestPlugin.log(e);
					}
				});
	}

	/**
	 * Saves a test run session into history
	 *
	 * @param testRunSession a test run session object
	 */
	public void watch(TestRunSession testRunSession) {
		for (HistoryItem item : items) {
			if (testRunSession == null || item.getCurrentTestRunSession().filter(testRunSession::equals).isEmpty()) {
				try {
					item.swapOut();
				} catch (CoreException e) {
					UnitTestPlugin.log(e);
				}
			}
		}
	}

	/**
	 * Removes a history item
	 *
	 * @param selected a history item to remove
	 */
	public void remove(HistoryItem selected) {
		this.items.remove(selected);
		try {
			selected.removeSwapFile();
		} catch (IOException e) {
			UnitTestPlugin.log(e);
		}
	}

	/**
	 * adds a history item
	 *
	 * @param historyItem history item to add
	 */
	public void add(HistoryItem historyItem) {
		items.add(historyItem);
	}
}
