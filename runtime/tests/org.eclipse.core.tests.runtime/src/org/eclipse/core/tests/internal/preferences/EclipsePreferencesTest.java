/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.preferences;

import java.io.*;
import java.util.*;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.internal.preferences.TestHelper;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.core.tests.runtime.RuntimeTest;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Test suite for API class org.eclipse.core.runtime.Preferences
 */
@SuppressWarnings("restriction")
public class EclipsePreferencesTest extends RuntimeTest {

	static class NodeTracer implements IEclipsePreferences.INodeChangeListener {
		StringBuilder log = new StringBuilder();

		@Override
		public void added(IEclipsePreferences.NodeChangeEvent event) {
			log.append("[A:");
			log.append(event.getParent().absolutePath());
			log.append(',');
			log.append(event.getChild().absolutePath());
			log.append(']');
		}

		@Override
		public void removed(IEclipsePreferences.NodeChangeEvent event) {
			log.append("[R:");
			log.append(event.getParent().absolutePath());
			log.append(',');
			log.append(event.getChild().absolutePath());
			log.append(']');
		}
	}

	static class PreferenceTracer implements IEclipsePreferences.IPreferenceChangeListener {
		public StringBuilder log = new StringBuilder();

		private String typeCode(Object value) {
			if (value == null) {
				return "";
			}
			if (value instanceof Boolean) {
				return "B";
			}
			if (value instanceof Integer) {
				return "I";
			}
			if (value instanceof Long) {
				return "L";
			}
			if (value instanceof Float) {
				return "F";
			}
			if (value instanceof Double) {
				return "D";
			}
			if (value instanceof String) {
				return "S";
			}
			if (value instanceof byte[]) {
				return "b";
			}
			assertTrue("0.0: " + value, false);
			return null;
		}

		@Override
		public void preferenceChange(IEclipsePreferences.PreferenceChangeEvent event) {
			log.append("[");
			log.append(event.getKey());
			log.append(":");
			log.append(typeCode(event.getOldValue()));
			log.append(event.getOldValue() == null ? "null" : event.getOldValue());
			log.append("->");
			log.append(typeCode(event.getNewValue()));
			log.append(event.getNewValue() == null ? "null" : event.getNewValue());
			log.append("]");
		}
	}

	public EclipsePreferencesTest(String name) {
		super(name);
	}

	private IEclipsePreferences getScopeRoot() {
		return (IEclipsePreferences) Platform.getPreferencesService().getRootNode().node(TestScope.SCOPE);
	}

	public void testRemove() {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final String value = "hello";
		final String defaultValue = null;

		// nothing there so expect the default
		assertEquals("1.1", defaultValue, prefs.get(key, defaultValue));
		// set a value and ensure it exists
		prefs.put(key, value);
		assertEquals("1.2", value, prefs.get(key, defaultValue));

		// remove the node and then try to remove the key
		try {
			prefs.removeNode();
		} catch (BackingStoreException e) {
			fail("2.1", e);
		}
		try {
			assertFalse("2.5", prefs.nodeExists(""));
		} catch (BackingStoreException e) {
			fail("2.2", e);
		}
		try {
			prefs.remove(key);
			fail("2.99");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testString() {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final String defaultValue = null;
		final String[] values = {"", "hello", " x ", "\n"};

		try {

			// nothing there so expect the default
			assertEquals("1.1", defaultValue, prefs.get(key, defaultValue));

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				String v1 = values[i];
				String v2 = values[i] + "x";
				prefs.put(key, v1);
				assertEquals("1.2." + i, v1, prefs.get(key, defaultValue));
				prefs.put(key, v2);
				assertEquals("1.3." + i, v2, prefs.get(key, defaultValue));
				prefs.remove(key);
				assertEquals("1.4." + i, defaultValue, prefs.get(key, defaultValue));
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.get(null, defaultValue);
				fail("1.5.0");
			} catch (NullPointerException e) {
				// expected
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.put(null, defaultValue);
				fail("1.5.1");
			} catch (NullPointerException e) {
				// expected
			}

			// spec'd to throw a NPE if value is null
			try {
				prefs.put(key, null);
				fail("1.5.2");
			} catch (NullPointerException e) {
				// expected
			}
		} finally {
			// clean-up
			try {
				prefs.removeNode();
			} catch (BackingStoreException e) {
				fail("0.99", e);
			}
		}

		// spec'd to throw IllegalStateException if node has been removed
		try {
			prefs.get(key, defaultValue);
			fail("1.6");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testLong() {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final long defaultValue = 42L;
		final long[] values = {-12345L, 0L, 12345L, Long.MAX_VALUE, Long.MIN_VALUE};

		try {

			// nothing there so expect the default
			assertEquals("1.1", defaultValue, prefs.getLong(key, defaultValue));

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				long v1 = values[i];
				long v2 = 54L;
				prefs.putLong(key, v1);
				assertEquals("1.2." + i, v1, prefs.getLong(key, defaultValue));
				prefs.putLong(key, v2);
				assertEquals("1.3." + i, v2, prefs.getLong(key, defaultValue));
				prefs.remove(key);
				assertEquals("1.4." + i, defaultValue, prefs.getLong(key, defaultValue));
			}

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals("1.5", stringValue, prefs.get(key, null));
			assertEquals("1.6", defaultValue, prefs.getLong(key, defaultValue));

			// spec'd to throw a NPE if key is null
			try {
				prefs.getLong(null, defaultValue);
				fail("2.0");
			} catch (NullPointerException e) {
				// expected
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.putLong(null, defaultValue);
				fail("2.1");
			} catch (NullPointerException e) {
				// expected
			}
		} finally {
			// clean-up
			try {
				prefs.removeNode();
			} catch (BackingStoreException e) {
				fail("0.99", e);
			}
		}

		// spec'd to throw IllegalStateException if node has been removed
		try {
			prefs.getLong(key, defaultValue);
			fail("3.0");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testBoolean() {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final boolean defaultValue = false;

		try {

			// nothing there so expect the default
			assertEquals("1.1", defaultValue, prefs.getBoolean(key, defaultValue));

			prefs.putBoolean(key, true);
			assertEquals("1.2", true, prefs.getBoolean(key, defaultValue));
			prefs.putBoolean(key, false);
			assertEquals("1.3", false, prefs.getBoolean(key, defaultValue));
			prefs.remove(key);
			assertEquals("1.4", defaultValue, prefs.getBoolean(key, defaultValue));

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals("1.5", stringValue, prefs.get(key, null));
			assertEquals("1.6", defaultValue, prefs.getBoolean(key, defaultValue));

			// spec'd to throw a NPE if key is null
			try {
				prefs.getBoolean(null, defaultValue);
				fail("2.0");
			} catch (NullPointerException e) {
				// expected
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.putBoolean(null, defaultValue);
				fail("2.1");
			} catch (NullPointerException e) {
				// expected
			}
		} finally {
			// clean-up
			try {
				prefs.removeNode();
			} catch (BackingStoreException e) {
				fail("0.99", e);
			}
		}

		// spec'd to throw IllegalStateException if node has been removed
		try {
			prefs.getBoolean(key, defaultValue);
			fail("3.0");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	private byte[][] getByteValues() {
		ArrayList<byte[]> result = new ArrayList<>();
		result.add(new byte[0]);
		result.add(new byte[] {127});
		result.add(new byte[] {-128});
		result.add(new byte[] {0});
		result.add(new byte[] {5});
		result.add(new byte[] {-23});
		return result.toArray(new byte[result.size()][]);
	}

	public void testBytes() {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final byte[] defaultValue = new byte[] {42};
		final byte[][] values = getByteValues();

		try {

			// nothing there so expect the default
			assertEquals("1.1", defaultValue, prefs.getByteArray(key, defaultValue));

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				byte[] v1 = values[i];
				byte[] v2 = new byte[] {54};
				prefs.putByteArray(key, v1);
				assertEquals("1.2." + i, v1, prefs.getByteArray(key, defaultValue));
				prefs.putByteArray(key, v2);
				assertEquals("1.3." + i, v2, prefs.getByteArray(key, defaultValue));
				prefs.remove(key);
				assertEquals("1.4." + i, defaultValue, prefs.getByteArray(key, defaultValue));
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.getByteArray(null, defaultValue);
				fail("2.0");
			} catch (NullPointerException e) {
				// expected
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.putByteArray(null, defaultValue);
				fail("2.1");
			} catch (NullPointerException e) {
				// expected
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.putByteArray(key, null);
				fail("2.2");
			} catch (NullPointerException e) {
				// expected
			}
		} finally {
			// clean-up
			try {
				prefs.removeNode();
			} catch (BackingStoreException e) {
				fail("0.99", e);
			}
		}

		// spec'd to throw IllegalStateException if node has been removed
		try {
			prefs.getByteArray(key, defaultValue);
			fail("3.0");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testFloat() {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final float defaultValue = 42f;
		final float[] values = {-12345f, 0f, 12345f, Float.MAX_VALUE, Float.MIN_VALUE};
		final float tol = 1.0e-20f;

		try {

			// nothing there so expect the default
			assertEquals("1.1", defaultValue, prefs.getFloat(key, defaultValue), tol);

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				float v1 = values[i];
				float v2 = 54f;
				prefs.putFloat(key, v1);
				assertEquals("1.2." + i, v1, prefs.getFloat(key, defaultValue), tol);
				prefs.putFloat(key, v2);
				assertEquals("1.3." + i, v2, prefs.getFloat(key, defaultValue), tol);
				prefs.remove(key);
				assertEquals("1.4." + i, defaultValue, prefs.getFloat(key, defaultValue), tol);
			}

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals("1.5", stringValue, prefs.get(key, null));
			assertEquals("1.6", defaultValue, prefs.getFloat(key, defaultValue), tol);

			// spec'd to throw a NPE if key is null
			try {
				prefs.getFloat(null, defaultValue);
				fail("2.0");
			} catch (NullPointerException e) {
				// expected
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.putFloat(null, defaultValue);
				fail("2.1");
			} catch (NullPointerException e) {
				// expected
			}
		} finally {
			// clean-up
			try {
				prefs.removeNode();
			} catch (BackingStoreException e) {
				fail("0.99", e);
			}
		}

		// spec'd to throw IllegalStateException if node has been removed
		try {
			prefs.getFloat(key, defaultValue);
			fail("3.0");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testFlushDeadlock() {

		final IEclipsePreferences parent = InstanceScope.INSTANCE.getNode(PI_RUNTIME_TESTS);
		final Preferences child = parent.node("testFlushDeadlock");
		class FlushJob extends Job {
			private Preferences node;

			FlushJob(Preferences node) {
				super("testFlushDeadlock");
				this.node = node;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					node.flush();
				} catch (BackingStoreException e) {
					return new Status(IStatus.ERROR, PI_RUNTIME_TESTS, "unexpected flush failure", e);
				}
				return Status.OK_STATUS;
			}

		}
		//make sure node is dirty
		child.putBoolean("testFlushDeadlock", true);
		//flush the parent of the load level, and the child
		Job flushParent = new FlushJob(parent);
		Job flushChild = new FlushJob(child);
		flushParent.schedule();
		flushChild.schedule();

		try {
			flushParent.join();
			flushChild.join();
		} catch (InterruptedException e) {
			fail("4.99", e);
		}
	}

	public void testDouble() {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final double defaultValue = 42.0;
		final double[] values = {0.0, 1002.5, -201788.55, Double.MAX_VALUE, Double.MIN_VALUE};
		final double tol = 1.0e-20;

		try {

			// nothing there so expect the default
			assertEquals("1.1", defaultValue, prefs.getDouble(key, defaultValue), tol);

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				double v1 = values[i];
				double v2 = 54.0;
				prefs.putDouble(key, v1);
				assertEquals("1.2." + i, v1, prefs.getDouble(key, defaultValue), tol);
				prefs.putDouble(key, v2);
				assertEquals("1.3." + i, v2, prefs.getDouble(key, defaultValue), tol);
				prefs.remove(key);
				assertEquals("1.4." + i, defaultValue, prefs.getDouble(key, defaultValue), tol);
			}

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals("1.5", stringValue, prefs.get(key, null));
			assertEquals("1.6", defaultValue, prefs.getDouble(key, defaultValue), tol);

			// spec'd to throw a NPE if key is null
			try {
				prefs.getDouble(null, defaultValue);
				fail("2.0");
			} catch (NullPointerException e) {
				// expected
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.putDouble(null, defaultValue);
				fail("2.1");
			} catch (NullPointerException e) {
				// expected
			}
		} finally {
			// clean-up
			try {
				prefs.removeNode();
			} catch (BackingStoreException e) {
				fail("0.99", e);
			}
		}

		// spec'd to throw IllegalStateException if node has been removed
		try {
			prefs.getDouble(key, defaultValue);
			fail("3.0");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testInt() {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final int defaultValue = 42;
		final int[] values = {0, 1002, -201788, Integer.MAX_VALUE, Integer.MIN_VALUE};

		try {

			// nothing there so expect the default
			assertEquals("1.1", defaultValue, prefs.getInt(key, defaultValue));

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				int v1 = values[i];
				int v2 = 54;
				prefs.putInt(key, v1);
				assertEquals("1.2." + i, v1, prefs.getInt(key, defaultValue));
				prefs.putInt(key, v2);
				assertEquals("1.3." + i, v2, prefs.getInt(key, defaultValue));
				prefs.remove(key);
				assertEquals("1.4." + i, defaultValue, prefs.getInt(key, defaultValue));
			}

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals("1.5", stringValue, prefs.get(key, null));
			assertEquals("1.6", defaultValue, prefs.getInt(key, defaultValue));

			// spec'd to throw a NPE if key is null
			try {
				prefs.getInt(null, defaultValue);
				fail("2.0");
			} catch (NullPointerException e) {
				// expected
			}

			// spec'd to throw a NPE if key is null
			try {
				prefs.putInt(null, defaultValue);
				fail("2.1");
			} catch (NullPointerException e) {
				// expected
			}
		} finally {
			// clean-up
			try {
				prefs.removeNode();
			} catch (BackingStoreException e) {
				fail("0.99", e);
			}
		}

		// spec'd to throw IllegalStateException if node has been removed
		try {
			prefs.getInt(key, defaultValue);
			fail("3.0");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	public void testRemoveNode() {
		Preferences root = getScopeRoot();
		ArrayList<Preferences> list = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			list.add(root.node(getUniqueString()));
		}

		// all exist
		for (Iterator<Preferences> i = list.iterator(); i.hasNext();) {
			Preferences node = i.next();
			try {
				assertTrue("1." + i, node.nodeExists(""));
			} catch (BackingStoreException e) {
				fail("1.99." + i, e);
			}
		}

		// remove each
		for (Iterator<Preferences> i = list.iterator(); i.hasNext();) {
			Preferences node = i.next();
			try {
				node.removeNode();
				assertTrue("2." + i, !node.nodeExists(""));
			} catch (BackingStoreException e) {
				fail("2.99." + i, e);
			}
		}
	}

	/*
	 * Test for bug 367366.
	 * TODO re-enable when the bug is fixed
	 */
	public void _testRemoveDeletesFile() {
		Preferences node = InstanceScope.INSTANCE.getNode("foo");
		Preferences parent = node.parent();
		node.put("a", "b");
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("0.99", e);
		}
		File file = TestHelper.getInstanceBaseLocation().append(".settings").append("foo.prefs").toFile();

		assertTrue("1.0", file.exists());
		try {
			node.removeNode();
		} catch (BackingStoreException e) {
			fail("1.99", e);
		}
		try {
			parent.flush();
		} catch (BackingStoreException e) {
			fail("2.99", e);
		}
		// ensure file was deleted
		assertFalse("3.0", file.exists());
	}

	public void testName() {
		Preferences node = Platform.getPreferencesService().getRootNode();

		assertEquals("1.0", "", node.name());
		node = node.node(TestScope.SCOPE);
		assertEquals("2.0", TestScope.SCOPE, node.name());
		node = node.node("foo");
		assertEquals("3.0", "foo", node.name());
	}

	public void testNode() {
		Preferences node = Platform.getPreferencesService().getRootNode();

		// root node
		assertNotNull("1.0", node);
		assertEquals("1.1", "", node.name());
		assertEquals("1.2", "/", node.absolutePath());
		// Bug 57150 [runtime] prefs: root.node("/") should return root
		assertEquals("1.3", node, node.node("/"));

		// scope root
		node = node.node(TestScope.SCOPE);
		assertNotNull("2.0", node);
		assertEquals("2.1", TestScope.SCOPE, node.name());
		assertEquals("2.2", "/" + TestScope.SCOPE, node.absolutePath());

		// child
		String name = getUniqueString();
		node = node.node(name);
		assertNotNull("3.0", node);
		assertEquals("3.1", name, node.name());
		assertEquals("3.2", "/" + TestScope.SCOPE + "/" + name, node.absolutePath());
	}

	public void testParent() {
		// parent of the root is null
		assertNull("1.0", Platform.getPreferencesService().getRootNode().parent());

		// parent of the scope root is the root
		Preferences node = Platform.getPreferencesService().getRootNode().node(TestScope.SCOPE);
		Preferences parent = node.parent();
		assertEquals("2.0", "/", parent.absolutePath());

		// parent of a child is the scope root
		node = getScopeRoot().node(getUniqueString());
		parent = node.parent();
		assertEquals("2.0", "/" + TestScope.SCOPE, parent.absolutePath());
	}

	public void testKeys() {
		String[] keys = new String[] {"foo", "bar", "quux"};
		Preferences node = getScopeRoot().node(getUniqueString());

		// ensure nothing exists to begin with
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			assertNull("1.0." + i, node.get(key, null));
		}

		// set all keys
		for (String key : keys) {
			node.put(key, getUniqueString());
		}

		// get the key list
		try {
			String[] result = node.keys();
			assertEquals("2.0", keys, result, false);
		} catch (BackingStoreException e) {
			fail("0.99", e);
		}
	}

	private void assertEquals(String message, byte[] one, byte[] two) {
		if (one == null && two == null) {
			return;
		}
		if (one == two) {
			return;
		}
		assertNotNull(message + ".1", one);
		assertNotNull(message + ".2", two);
		assertEquals(message + ".3", one.length, two.length);
		for (int i = 0; i < one.length; i++) {
			assertEquals(message + ".4." + i, one[i], two[i]);
		}
	}

	public void testChildrenNames() {
		String[] childrenNames = new String[] {"foo", "bar", "quux"};
		Preferences node = getScopeRoot().node(getUniqueString());
		String[] result = null;

		// no children to start
		try {
			result = node.childrenNames();
		} catch (BackingStoreException e) {
			fail("1.0", e);
		}
		assertEquals("1.1", 0, result.length);

		// add children
		for (String childrenName : childrenNames) {
			node.node(childrenName);
		}
		try {
			result = node.childrenNames();
		} catch (BackingStoreException e) {
			fail("2.0", e);
		}
		assertEquals("2.1", childrenNames, result, false);

	}

	public void testNodeExists() {
		Preferences parent = null;
		Preferences node = Platform.getPreferencesService().getRootNode();
		String[] childrenNames = new String[] {"foo", "bar", "quux"};
		String fake = "fake";

		// check the root node
		try {
			assertTrue("1.0", node.nodeExists(""));
			assertTrue("1.1", !node.nodeExists(fake));
		} catch (BackingStoreException e) {
			fail("1.99", e);
		}

		// check the scope root
		parent = node;
		node = getScopeRoot();
		try {
			assertTrue("2.0", parent.nodeExists(node.name()));
			assertTrue("2.1", node.nodeExists(""));
			assertTrue("2.2", !parent.nodeExists(fake));
			assertTrue("2.3", !node.nodeExists(fake));
		} catch (BackingStoreException e) {
			fail("2.99", e);
		}

		// check a child
		parent = node;
		node = parent.node(getUniqueString());
		try {
			assertTrue("3.0", parent.nodeExists(node.name()));
			assertTrue("3.1", node.nodeExists(""));
			assertTrue("3.2", !parent.nodeExists(fake));
			assertTrue("3.3", !node.nodeExists(fake));
		} catch (BackingStoreException e) {
			fail("3.99", e);
		}

		// create some more children and check
		parent = node;
		Preferences[] nodes = new Preferences[childrenNames.length];
		for (int i = 0; i < childrenNames.length; i++) {
			nodes[i] = parent.node(childrenNames[i]);
		}
		for (String childrenName : childrenNames) {
			try {
				assertTrue("4.0", parent.nodeExists(childrenName));
				assertTrue("4.1", !parent.nodeExists(fake));
			} catch (BackingStoreException e) {
				fail("4.99", e);
			}
		}
		for (Preferences preferenceNode : nodes) {
			try {
				assertTrue("4.2", preferenceNode.nodeExists(""));
			} catch (BackingStoreException e) {
				fail("4.100", e);
			}
		}

		// remove children and check
		for (Preferences n : nodes) {
			try {
				n.removeNode();
				assertTrue("5.1", !parent.nodeExists(n.name()));
				assertTrue("5.2", !n.nodeExists(""));
			} catch (BackingStoreException e) {
				fail("5.99", e);
			}
		}
	}

	public void testClear() {
		Preferences node = getScopeRoot().node(getUniqueString());
		String[] keys = new String[] {"foo", "bar", "quux"};
		String[] values = new String[] {getUniqueString(), getUniqueString(), getUniqueString()};

		// none to start with
		try {
			assertEquals("1.0", 0, node.keys().length);
		} catch (BackingStoreException e) {
			fail("1.99", e);
		}

		// fill the node up with values
		try {
			for (int i = 0; i < keys.length; i++) {
				node.put(keys[i], values[i]);
			}
			assertEquals("2.0", keys.length, node.keys().length);
			assertEquals("2.1", keys, node.keys(), false);
		} catch (BackingStoreException e) {
			fail("2.99", e);
		}

		// clear the values and check
		try {
			node.clear();
			assertEquals("3.0", 0, node.keys().length);
			for (int i = 0; i < keys.length; i++) {
				assertNull("3.1." + i, node.get(keys[i], null));
			}
		} catch (BackingStoreException e) {
			fail("3.99", e);
		}
	}

	public void testAbsolutePath() {
		IPath expected = Path.ROOT;
		Preferences node = Platform.getPreferencesService().getRootNode();

		// root node
		assertEquals("1.0", expected.toString(), node.absolutePath());

		// scope root
		expected = expected.append(TestScope.SCOPE);
		node = node.node(TestScope.SCOPE);
		assertEquals("2.0", expected.toString(), node.absolutePath());

		// another child
		String name = getUniqueString();
		expected = expected.append(name);
		node = node.node(name);
		assertEquals("3.0", expected.toString(), node.absolutePath());
	}

	public void testAccept() {
		IEclipsePreferences scopeRoot = getScopeRoot();
		ArrayList<String> expected = new ArrayList<>();
		final ArrayList<String> actual = new ArrayList<>();

		IPreferenceNodeVisitor visitor = node -> {
			actual.add(node.absolutePath());
			return true;
		};

		// just the scope root
		try {
			scopeRoot.accept(visitor);
		} catch (BackingStoreException e) {
			fail("0.99", e);
		}
		expected.add(scopeRoot.absolutePath());
		assertEquals("0.1", expected.toArray(new String[0]), actual.toArray(new String[0]), false);

		Set<String> children = new HashSet<>();
		children.add(getUniqueString());
		children.add(getUniqueString());
		children.add(getUniqueString());

		// visit some children nodes
		actual.clear();
		expected.clear();
		expected.add(scopeRoot.absolutePath());
		for (String s : children) {
			expected.add(scopeRoot.absolutePath() + '/' + s);
			scopeRoot.node(s);
		}
		try {
			scopeRoot.accept(visitor);
		} catch (BackingStoreException e) {
			fail("1.99", e);
		}
		assertEquals("1.0", expected.toArray(new String[0]), actual.toArray(new String[0]), false);
	}

	public void testPreferenceChangeListeners() {
		IEclipsePreferences node = getScopeRoot();
		PreferenceTracer tracer = new PreferenceTracer();
		node.addPreferenceChangeListener(tracer);

		String key = "foo";

		// initial state
		assertEquals("0.0", "", tracer.log.toString());

		// add preference (string value)
		node.put(key, "bar");
		String string = node.get(key, null);
		assertNotNull("1.0", string);
		assertEquals("1.1", "bar", string);
		assertEquals("1.2", "[foo:null->Sbar]", tracer.log.toString());

		// change its value
		tracer.log.setLength(0);
		node.put(key, "quux");
		string = node.get(key, null);
		assertNotNull("2.0", string);
		assertEquals("2.1", "quux", string);
		assertEquals("2.2", "[foo:Sbar->Squux]", tracer.log.toString());

		// change its type - should have no effect (events are strings)
		tracer.log.setLength(0);
		node.putInt(key, 123);
		int i = node.getInt(key, 0);
		assertEquals("3.0", 123, i);
		assertEquals("3.1", "[foo:Squux->S123]", tracer.log.toString());

		node.put(key, "aaa");
		tracer.log.setLength(0);
		node.remove(key);
		assertNull("4.0", node.get(key, null));
		assertEquals("4.1", "[foo:Saaa->null]", tracer.log.toString());

		// TODO finish these
	}

	public void testNodeChangeListeners() {
		IEclipsePreferences root = getScopeRoot();
		NodeTracer tracer = new NodeTracer();
		root.addNodeChangeListener(tracer);

		// initial state
		assertEquals("0.0", "", tracer.log.toString());

		// add a child
		String name = getUniqueString();
		IPath parent = new Path(root.absolutePath());
		IPath child = parent.append(name);
		Preferences node = root.node(name);
		assertEquals("1.0", "[A:" + parent + ',' + child + ']', tracer.log.toString());

		// remove the child
		tracer.log.setLength(0);
		try {
			node.removeNode();
			assertEquals("2.0", "[R:" + parent + ',' + child + ']', tracer.log.toString());
		} catch (BackingStoreException e) {
			fail("2.99", e);
		}

		// remove the listener and make sure we don't get any changes
		root.removeNodeChangeListener(tracer);
		tracer.log.setLength(0);
		root.node(name);
		assertEquals("3.0", "", tracer.log.toString());
	}

	@Override
	protected void tearDown() throws Exception {
		Preferences node = getScopeRoot();
		node.removeNode();
	}

	/*
	 * Bug 60590 - Flush on dirty child settings node fails if parent clean.
	 *
	 * After changing a preference value, we call #makeDirty which does a
	 * recursive call marking itself dirty as well as all its parents. As a short
	 * circuit, if a parent was already dirty then it stopped the recursion.
	 *
	 * Unfortuanatly the #makeClean method only marks the load level as
	 * clean and not all children since it doesn't know which child triggered
	 * the dirtiness.
	 *
	 * Changed the makeDirty call to mark all parent nodes as dirty.
	 */
	public void test_60590() {
		IEclipsePreferences root = Platform.getPreferencesService().getRootNode();
		String one = getUniqueString();
		String two = getUniqueString();
		String threeA = getUniqueString();
		String threeB = getUniqueString();
		String key = "key";
		String value = "value";
		Preferences node = root.node(TestScope.SCOPE).node(one).node(two).node(threeA);
		node.put(key, value);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.99", e);
		}
		node = root.node(TestScope.SCOPE).node(one).node(two).node(threeB);
		node.put(key, value);
		Preferences current = node;
		int count = 0;
		while (current != null && current instanceof EclipsePreferences && current.parent() != null && new Path(current.absolutePath()).segment(0).equals(TestScope.SCOPE)) {
			assertTrue("1.0." + current.absolutePath(), ((EclipsePreferences) current).isDirty());
			count++;
			current = current.parent();
		}
		assertEquals("2.0." + count, 4, count);
	}

	/*
	 * Bug 342709 - [prefs] Don't write date/timestamp comment in preferences file
	 */
	public void test_342709() {
		// set some prefs
		IEclipsePreferences root = Platform.getPreferencesService().getRootNode();
		String one = getUniqueString();
		String two = getUniqueString();
		String three = getUniqueString();
		String key = "key";
		String value = "value";
		Preferences node = root.node(TestScope2.SCOPE).node(one).node(two).node(three);
		node.put(key, value);

		// save the prefs to disk
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.99", e);
		}

		assertTrue("2.0", node instanceof TestScope2);

		// read the file outside of the pref mechanism
		IPath location = ((TestScope2) node).getLocation();
		Collection<String> lines = null;
		try {
			lines = read(location);
		} catch (IOException e) {
			fail("4.99", e);
		}

		// ensure there is no comment or timestamp in the file
		for (String line : lines) {
			assertFalse("3." + line, line.startsWith("#"));
		}
	}

	public static Collection<String> read(IPath location) throws IOException {
		Collection<String> result = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new FileReader(location.toFile()));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				result.add(line);
			}
		} finally {
			reader.close();
		}
		return result;
	}

	/*
	 * Bug 55410 - [runtime] prefs: keys and valid chars
	 */
	public void test_55410() {
		String[] keys = new String[] {"my/key", "my:key", "my/long:key"};
		String[] paths = new String[] {"my/path", "my:path"};
		Preferences node = Platform.getPreferencesService().getRootNode().node(TestScope.SCOPE).node(getUniqueString());

		// test keys
		for (String key : keys) {
			String value = getUniqueString();
			node.put(key, value);
			assertEquals("1.0." + key, value, node.get(key, null));
		}

		// test paths
		String root = node.absolutePath();
		for (String path : paths) {
			String expected = root + IPath.SEPARATOR + path;
			String actual = node.node(path).absolutePath();
			assertEquals("2.0." + path, expected, actual);
		}
	}

	public void testFileFormat() {
		class Info {

			String path;
			String key;
			String encoded;

			Info(String path, String key, String encoded) {
				this.path = path;
				this.key = key;
				this.encoded = encoded;
			}
		}

		List<Info> list = new ArrayList<>();
		list.add(new Info("", "a", "a"));
		list.add(new Info("", "/a", "///a"));
		list.add(new Info("a", "b", "a/b"));
		list.add(new Info("a/b", "c/d", "a/b//c/d"));
		list.add(new Info("", "a//b", "//a//b"));
		list.add(new Info("a/b", "c", "a/b/c"));
		list.add(new Info("a/b", "c//d", "a/b//c//d"));

		Preferences node = new TestScope().getNode(getUniqueString());
		for (int i = 0; i < list.size(); i++) {
			Info info = list.get(i);
			node.node(info.path).put(info.key, Integer.toString(i));
		}

		assertTrue("0.8", node instanceof EclipsePreferences);

		Properties properties = null;
		try {
			properties = TestHelper.convertToProperties((EclipsePreferences) node, "");
		} catch (BackingStoreException e) {
			fail("1.0", e);
		}

		for (Object object : properties.keySet()) {
			String key = (String) object;
			String value = properties.getProperty(key);
			try {
				Info info = list.get(Integer.parseInt(value));
				assertNotNull("2.0", info);
				assertEquals("2.1." + key, info.encoded, key);
			} catch (NumberFormatException e) {
				fail("2.99." + value, e);
			}
		}
	}

	private Properties loadProperties(IPath location) {
		Properties result = new Properties();
		if (!location.toFile().exists()) {
			return result;
		}
		InputStream input = null;
		try {
			input = new FileInputStream(location.toFile());
			result.load(input);
		} catch (IOException e) {
			fail("loadProperties", e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return result;
	}

	public void testEncodePath() {
		class Item {
			String path, key, expected;

			Item(String path, String key, String expected) {
				super();
				this.path = path;
				this.key = key;
				this.expected = expected;
			}
		}

		ArrayList<Item> list = new ArrayList<>();
		list.add(new Item(null, "a", "a"));
		list.add(new Item(null, "/a", "///a"));
		list.add(new Item("a", "b", "a/b"));
		list.add(new Item("a/b", "c/d", "a/b//c/d"));
		list.add(new Item("a", "b//c", "a//b//c"));
		list.add(new Item("repositories", "cvs://dev.eclipse.org:25/cvsroot", "repositories//cvs://dev.eclipse.org:25/cvsroot"));
		list.add(new Item("repositories:cvs", "dev.eclipse.org:25", "repositories:cvs/dev.eclipse.org:25"));

		for (Iterator<Item> i = list.iterator(); i.hasNext();) {
			Item item = i.next();
			assertEquals("a" + i + item.expected, item.expected, EclipsePreferences.encodePath(item.path, item.key));
			String[] result = EclipsePreferences.decodePath(item.expected);
			assertEquals("b" + i + item.path, item.path, result[0]);
			assertEquals("c" + i + item.key, item.key, result[1]);
		}
	}

	public void testGetSegment() {
		String[][] data = new String[][] {new String[] {"instance", "/instance/foo", "0"}, //
				new String[] {"instance", "instance/foo", "0"}, //
				new String[] {"instance", "instance", "0"}, //
				new String[] {"instance", "instance", "0"}, //
				new String[] {"foo", "/instance/foo", "1"}, //
				new String[] {"foo", "instance/foo", "1"}, //
				new String[] {"foo", "/instance/foo/", "1"}, //
				new String[] {"foo", "instance/foo/", "1"}, //
				new String[] {"foo", "/instance/foo/bar", "1"}, //
				new String[] {null, "/instance", "1"}, //
				new String[] {null, "instance", "1"}, //
				new String[] {null, "instance/", "1"}, //
		};
		for (int i = 0; i < data.length; i++) {
			String[] line = data[i];
			assertEquals("1.0." + i + ':' + line[1] + " (" + line[2] + ')', line[0], EclipsePreferences.getSegment(line[1], Integer.parseInt(line[2])));
		}
	}

	public void testGetSegmentCount() {
		String[][] data = new String[][] {new String[] {"/instance/foo", "2"}, //
				new String[] {"instance/foo", "2"}, //
				new String[] {"/instance/foo/", "2"}, //
				new String[] {"/instance", "1"}, //
				new String[] {"instance", "1"}, //
				new String[] {"/instance/", "1"}, //
				new String[] {"instance/", "1"}, //
		};
		for (String[] line : data) {
			assertEquals("1.0:" + line[0], Integer.parseInt(line[1]), EclipsePreferences.getSegmentCount(line[0]));
		}
	}

	public void test_68897() {
		File file = getRandomLocation().toFile();
		IPreferencesService service = Platform.getPreferencesService();

		IEclipsePreferences rootPreferences = service.getRootNode();
		Preferences pref = rootPreferences.node("/favorite");

		Preferences child = pref.node("my");
		child.put("file", "my.txt");
		try {
			child.flush();
			pref.flush();
			rootPreferences.flush();
		} catch (BackingStoreException e) {
			fail("0.0", e);
		}
		try {
			service.exportPreferences(rootPreferences, new FileOutputStream(file), (String[]) null);
		} catch (FileNotFoundException e) {
			fail("1.0", e);
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			IExportedPreferences epref = service.readPreferences(new FileInputStream(file));
			service.applyPreferences(epref);
		} catch (FileNotFoundException e) {
			fail("2.0", e);
		} catch (CoreException e) {
			fail("2.1", e);
		} catch (Exception e) {
			fail("2.2", e);
		}
	}

	public String TEST_NODE_PATH = "test.node.path";
	public String TEST_PREF_KEY = "test.pref.key";

	public void testGetByteArray() {
		final byte[] testArray = new byte[] {10, 13, 15, 20};
		IScopeContext defaultScope = DefaultScope.INSTANCE;
		defaultScope.getNode(TEST_NODE_PATH).putByteArray(TEST_PREF_KEY, testArray);
		final byte[] returnArray = Platform.getPreferencesService().getByteArray(TEST_NODE_PATH, TEST_PREF_KEY, new byte[] {}, null);
		assertEquals("1.0 Wrong size", testArray.length, returnArray.length);
		for (int i = 0; i < testArray.length; i++) {
			assertEquals("2.0 Wrong value at: " + i, testArray[i], returnArray[i]);
		}
	}

	/*
	 * Some tests to handle user-defined node storage.
	 */
	public void testNode3() {
		IPreferencesService service = Platform.getPreferencesService();
		IEclipsePreferences rootPreferences = service.getRootNode();
		Preferences node = rootPreferences.node("test3");

		// check that we have the expected children
		try {
			File file = RuntimeTestsPlugin.getTestData("testData/preferences/test3");
			Collection<String> expectedChildren = Arrays.asList(file.list());
			String[] children = node.childrenNames();
			for (String child : children) {
				assertTrue("1.1." + child, expectedChildren.contains(child));
			}
		} catch (BackingStoreException e) {
			fail("1.99", e);
		}

		// check the child has the expected values
		Preferences child = node.node("foo");
		try {
			assertEquals("2.0", 2, child.keys().length);
		} catch (BackingStoreException e) {
			fail("2.09", e);
		}
		assertEquals("2.1", "value1", child.get("key1", null));
		assertEquals("2.2", "value2", child.get("key2", null));

		// set a new value, flush (which saves the file) and check the file contents
		child.put("key8", "value8");
		try {
			child.flush();
		} catch (BackingStoreException e) {
			fail("3.99", e);
		}
		String prop = System.getProperty("equinox.preference.test.TestNodeStorage3,root");
		assertNotNull("3.1", prop);
		File rootFile = new File(prop);
		File childFile = new File(rootFile, "foo");
		assertTrue("3.2", childFile.exists());
		Properties contents = loadProperties(new Path(childFile.getAbsolutePath()));
		assertEquals("3.3", "value8", contents.getProperty("key8", null));

		// delete the node (which should remove the file)
		try {
			child.removeNode();
		} catch (BackingStoreException e) {
			fail("4.99", e);
		}
		assertFalse("4.1", childFile.exists());
	}
}
