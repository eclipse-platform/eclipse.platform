/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.unittest.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Defines constants which are used to refer to values in the plugin's
 * preference store.
 */
public class UnitTestPreferencesConstants {
	/**
	 * Boolean preference controlling whether the failure stack should be filtered.
	 */
	public static final String DO_FILTER_STACK = UnitTestPlugin.PLUGIN_ID + ".do_filter_stack"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the Unit Test view should be shown on
	 * errors only.
	 */
	public static final String SHOW_ON_ERROR_ONLY = UnitTestPlugin.PLUGIN_ID + ".show_on_error"; //$NON-NLS-1$

	/**
	 * Maximum number of remembered test runs.
	 */
	public static final String MAX_TEST_RUNS = UnitTestPlugin.PLUGIN_ID + ".max_test_runs"; //$NON-NLS-1$

	private UnitTestPreferencesConstants() {
		// no instance
	}

	/**
	 * Serializes the array of strings into one comma-separated string.
	 *
	 * @param list array of strings
	 * @return a single string composed of the given list
	 */
	public static String serializeList(String[] list) {
		if (list == null)
			return ""; //$NON-NLS-1$

		return String.join(String.valueOf(','), list);
	}

	/**
	 * Parses the comma-separated string into an array of strings.
	 *
	 * @param listString a comma-separated string
	 * @return an array of strings
	 */
	public static String[] parseList(String listString) {
		List<String> list = new ArrayList<>(10);
		StringTokenizer tokenizer = new StringTokenizer(listString, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens())
			list.add(tokenizer.nextToken());
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Indicates if a filter patterns are to be applied on a stacktrace/error
	 * messages
	 *
	 * @return <code>true</code> in case the stacktrace is to be filtered, otherwise
	 *         - <code>false</code>
	 */
	public static boolean getFilterStack() {
		return Platform.getPreferencesService().getBoolean(UnitTestPlugin.PLUGIN_ID, DO_FILTER_STACK, false, null);
	}

	/**
	 * Sets up a value for the DO_FILTER_STACK preference
	 *
	 * @param filter boolean indicating if a stacktrace is to be filtered
	 */
	public static void setFilterStack(boolean filter) {
		InstanceScope.INSTANCE.getNode(UnitTestPlugin.PLUGIN_ID).putBoolean(DO_FILTER_STACK, filter);
	}
}
