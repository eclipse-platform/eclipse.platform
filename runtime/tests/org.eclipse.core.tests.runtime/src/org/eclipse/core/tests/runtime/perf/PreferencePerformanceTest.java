/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.perf;

import java.util.ArrayList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.internal.preferences.TestScope;
import org.eclipse.core.tests.runtime.RuntimeTest;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PreferencePerformanceTest extends RuntimeTest {
	private static final int INNER_LOOP = 10000;
	private static final int KEYS_PER_NODE = 1000;

	public PreferencePerformanceTest() {
		super();
	}

	public PreferencePerformanceTest(String testName) {
		super(testName);
	}

	/*
	 * Return a 2 dimensional String array with the first element being the keys
	 * and the second being the values. All the keys will have the given prefix.
	 */
	private String[][] getCommonPrefixKeys(int size, String prefix) {
		ArrayList<String> keyList = new ArrayList<>();
		ArrayList<String> valueList = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			keyList.add(prefix + '.' + i + getUniqueString());
			valueList.add(Integer.toString(i));
		}
		String[][] result = new String[2][];
		result[0] = keyList.toArray(new String[keyList.size()]);
		result[1] = valueList.toArray(new String[valueList.size()]);
		return result;
	}

	private IEclipsePreferences getScopeRoot() {
		return (IEclipsePreferences) Platform.getPreferencesService().getRootNode().node(TestScope.SCOPE);
	}

	/*
	 * Return a 2 dimensional String array with the first element being the keys
	 * and the second being the values. The keys will be integers in sequential order.
	 */
	private String[][] getSequentialKeys(int size) {
		ArrayList<String> keyList = new ArrayList<>();
		ArrayList<String> valueList = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			keyList.add(Integer.toString(i));
			valueList.add(Integer.toString(i));
		}
		String[][] result = new String[2][];
		result[0] = keyList.toArray(new String[keyList.size()]);
		result[1] = valueList.toArray(new String[valueList.size()]);
		return result;
	}

	/*
	 * Return a 2 dimensional String array with the first element being the keys
	 * and the second being the values. All the keys will have a unique prefix.
	 */
	private String[][] getUniqueKeys(int size) {
		ArrayList<String> keyList = new ArrayList<>();
		ArrayList<String> valueList = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			keyList.add(i + getUniqueString());
			valueList.add(Integer.toString(i));
		}
		String[][] result = new String[2][];
		result[0] = keyList.toArray(new String[keyList.size()]);
		result[1] = valueList.toArray(new String[valueList.size()]);
		return result;
	}

	/**
	 * Time how long it takes to retrieve KEYS_PER_NODE keys with a common prefix.
	 * This is a good finger print test because preference keys typically have a common
	 * prefix (org.eclipse.component.keyName).
	 */
	public void testGetStringCommonPrefixKeys() {
		// setup
		final String qualifier = getUniqueString();
		String[][] kvp = getCommonPrefixKeys(KEYS_PER_NODE, qualifier);
		final String[] keys = kvp[0];
		final String[] values = kvp[1];

		// run the test
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			Preferences prefs;

			// set the values outside the timed loop
			@Override
			protected void setUp() {
				prefs = getScopeRoot().node(qualifier);
				for (int i = 0; i < keys.length; i++) {
					prefs.put(keys[i], values[i]);
				}
			}

			//  clean-up
			@Override
			protected void tearDown() {
				try {
					prefs.removeNode();
				} catch (BackingStoreException e) {
					fail("0.99", e);
				}
			}

			// test retrieval
			@Override
			protected void test() {
				for (String key : keys) {
					prefs.get(key, null);
				}
			}
		};
		runner.setFingerprintName("Retrieve preference values");
		runner.run(this, 10, INNER_LOOP);
	}

	/*
	 * Time how long it takes to get KEYS_PER_NODE keys that aren't there.
	 * Fill the node up with KEYS_PER_NODE key/value pairs so it has some data
	 */
	public void testGetStringMisses() {
		// setup
		final String qualifier = getUniqueString();
		String[][] kvp = getUniqueKeys(KEYS_PER_NODE);
		final String[] keys = kvp[0];
		final String[] values = kvp[1];
		final String[] missingKeys = getUniqueKeys(KEYS_PER_NODE)[0];

		// run the test
		new PerformanceTestRunner() {
			Preferences prefs;

			// set the values outside the timed loop
			@Override
			protected void setUp() {
				prefs = getScopeRoot().node(qualifier);
				for (int i = 0; i < keys.length; i++) {
					prefs.put(keys[i], values[i]);
				}
			}

			// clean-up
			@Override
			protected void tearDown() {
				try {
					prefs.removeNode();
				} catch (BackingStoreException e) {
					fail("0.99", e);
				}
			}

			// how long to get the values?
			@Override
			protected void test() {
				for (int i = 0; i < keys.length; i++) {
					prefs.get(missingKeys[i], null);
				}
			}
		}.run(this, 10, INNER_LOOP);
	}

	/*
	 * Time how long it takes to retrieve KEYS_PER_NODE keys which are constructed
	 * from sequential integers.
	 */
	public void testGetStringSequentialKeys() {
		// setup
		final String qualifier = getUniqueString();
		String[][] kvp = getSequentialKeys(KEYS_PER_NODE);
		final String[] keys = kvp[0];
		final String[] values = kvp[1];

		// run the test
		new PerformanceTestRunner() {
			Preferences prefs;

			// set the values outside the timed loop
			@Override
			protected void setUp() {
				prefs = getScopeRoot().node(qualifier);
				for (int i = 0; i < keys.length; i++) {
					prefs.put(keys[i], values[i]);
				}
			}

			// clean-up
			@Override
			protected void tearDown() {
				try {
					prefs.removeNode();
				} catch (BackingStoreException e) {
					fail("0.99", e);
				}
			}

			// how long to get the values?
			@Override
			protected void test() {
				for (String key : keys) {
					prefs.get(key, null);
				}
			}
		}.run(this, 10, INNER_LOOP);
	}

	/*
	 * Time how long it takes to get KEYS_PER_NODE keys that are unique.
	 */
	public void testGetStringUniqueKeys() {
		// setup
		final String qualifier = getUniqueString();
		String[][] kvp = getUniqueKeys(KEYS_PER_NODE);
		final String[] keys = kvp[0];
		final String[] values = kvp[1];

		// run the test
		new PerformanceTestRunner() {
			Preferences prefs;

			// set the values outside the timed loop
			@Override
			protected void setUp() {
				prefs = getScopeRoot().node(qualifier);
				for (int i = 0; i < keys.length; i++) {
					prefs.put(keys[i], values[i]);
				}
			}

			// clean-up
			@Override
			protected void tearDown() {
				try {
					prefs.removeNode();
				} catch (BackingStoreException e) {
					fail("0.99", e);
				}
			}

			// how long to get the values?
			@Override
			protected void test() {
				for (String key : keys) {
					prefs.get(key, null);
				}
			}
		}.run(this, 10, INNER_LOOP);
	}

	/*
	 * Time how long it takes to put KEYS_PER_NODE keys into a preference node.
	 */
	public void testPutStringKeys() {

		// setup outside the timed block
		final String qualifier = getUniqueString();
		final ArrayList<String> keyList = new ArrayList<>();
		final ArrayList<String> valueList = new ArrayList<>();
		for (int i = 0; i < KEYS_PER_NODE; i++) {
			keyList.add(getUniqueString() + i);
			valueList.add(Integer.toString(i));
		}
		final String[] keys = keyList.toArray(new String[keyList.size()]);
		final String[] values = valueList.toArray(new String[valueList.size()]);

		// run the test
		new PerformanceTestRunner() {
			Preferences prefs;

			@Override
			protected void setUp() {
				prefs = getScopeRoot().node(qualifier);
			}

			// clean-up
			@Override
			protected void tearDown() {
				try {
					prefs.removeNode();
				} catch (BackingStoreException e) {
					fail("0.99", e);
				}
			}

			// how long to set the values?
			@Override
			protected void test() {
				for (int i = 0; i < keys.length; i++) {
					prefs.put(keys[i], values[i]);
				}
			}
		}.run(this, 10, INNER_LOOP);
	}

	/*
	 * Add KEYS_PER_NODE keys to a preference node and then remove them one at a time.
	 */
	public void testRemoveStringKeys() {

		// gather the key/value pairs before so we don't time it
		final String qualifier = getUniqueString();
		final ArrayList<String> keyList = new ArrayList<>();
		final ArrayList<String> valueList = new ArrayList<>();
		for (int i = 0; i < KEYS_PER_NODE; i++) {
			keyList.add(getUniqueString() + i);
			valueList.add(Integer.toString(i));
		}
		final String[] keys = keyList.toArray(new String[keyList.size()]);
		final String[] values = valueList.toArray(new String[valueList.size()]);

		// run the performance test
		new PerformanceTestRunner() {
			Preferences prefs;

			// fill the node with values each run
			@Override
			protected void setUp() {
				prefs = getScopeRoot().node(qualifier);
				for (int i = 0; i < keys.length; i++) {
					prefs.put(keys[i], values[i]);
				}
			}

			// clean-up at the end of each run
			@Override
			protected void tearDown() {
				try {
					prefs.removeNode();
				} catch (BackingStoreException e) {
					fail("0.99", e);
				}
			}

			// can only run this once because there is only so many keys you can remove

			// the test is how long it takes to remove all the values
			@Override
			protected void test() {
				for (String key : keys) {
					prefs.remove(key);
				}
			}
		}.run(this, 50, 1);
	}
}