/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.unittest.internal.launcher;

import org.eclipse.unittest.internal.ui.UITestRunListener;

import org.eclipse.core.runtime.ListenerList;

/**
 * Test View Support registry
 */
public class TestListenerRegistry {

	/**
	 * Returns a {@link TestListenerRegistry} object instance
	 *
	 * @return a {@link TestListenerRegistry} object
	 */
	public static TestListenerRegistry getDefault() {
		if (fgRegistry != null)
			return fgRegistry;

		fgRegistry = new TestListenerRegistry();
		return fgRegistry;
	}

	private static TestListenerRegistry fgRegistry;

	/**
	 * List storing the registered test run listeners
	 */
	private ListenerList<TestRunListener> fUnitTestRunListeners = new ListenerList<>();

	private TestListenerRegistry() {
	}

	/**
	 * @return a <code>ListenerList</code> of all <code>TestRunListener</code>s
	 */
	public ListenerList<TestRunListener> getUnitTestRunListeners() {
		loadUnitTestRunListeners();
		return fUnitTestRunListeners;
	}

	/**
	 * Initializes TestRun Listener extensions
	 */
	private synchronized void loadUnitTestRunListeners() {
		if (!fUnitTestRunListeners.isEmpty()) {
			return;
		}
		fUnitTestRunListeners.add(new UITestRunListener());
	}

}
