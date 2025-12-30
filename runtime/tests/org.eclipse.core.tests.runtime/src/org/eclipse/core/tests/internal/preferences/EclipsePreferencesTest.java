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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.internal.preferences.TestHelper;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Test suite for API class org.eclipse.core.runtime.Preferences
 */
@SuppressWarnings("restriction")
public class EclipsePreferencesTest {

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
			fail(value.toString());
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

	@AfterEach
	public void tearDown() throws Exception {
		Preferences node = getScopeRoot();
		node.removeNode();
	}

	private IEclipsePreferences getScopeRoot() {
		return (IEclipsePreferences) Platform.getPreferencesService().getRootNode().node(TestScope.SCOPE);
	}

	@Test
	public void testRemove() throws BackingStoreException {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final String value = "hello";
		final String defaultValue = null;

		// nothing there so expect the default
		assertEquals(defaultValue, prefs.get(key, defaultValue));
		// set a value and ensure it exists
		prefs.put(key, value);
		assertEquals(value, prefs.get(key, defaultValue));

		// remove the node and then try to remove the key
		prefs.removeNode();
		assertFalse(prefs.nodeExists(""));
		assertThrows(IllegalStateException.class, () -> prefs.remove(key));
	}

	@Test
	public void testString() throws BackingStoreException {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final String defaultValue = null;
		final String[] values = {"", "hello", " x ", "\n"};

		try {
			// nothing there so expect the default
			assertEquals(defaultValue, prefs.get(key, defaultValue));

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				String v1 = values[i];
				String v2 = values[i] + "x";
				prefs.put(key, v1);
				assertEquals(v1, prefs.get(key, defaultValue), i + "");
				prefs.put(key, v2);
				assertEquals(v2, prefs.get(key, defaultValue), i + "");
				prefs.remove(key);
				assertEquals(defaultValue, prefs.get(key, defaultValue), i + "");
			}

			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.get(null, defaultValue));
			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.put(null, defaultValue));
			// spec'd to throw a NPE if value is null
			assertThrows(NullPointerException.class, () -> prefs.put(key, null));
		} finally {
			// clean-up
			prefs.removeNode();
		}

		// spec'd to throw IllegalStateException if node has been removed
		assertThrows(IllegalStateException.class, () -> prefs.get(key, defaultValue));
	}

	@Test
	public void testLong() throws BackingStoreException {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final long defaultValue = 42L;
		final long[] values = {-12345L, 0L, 12345L, Long.MAX_VALUE, Long.MIN_VALUE};

		try {
			// nothing there so expect the default
			assertEquals(defaultValue, prefs.getLong(key, defaultValue));

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				long v1 = values[i];
				long v2 = 54L;
				prefs.putLong(key, v1);
				assertEquals(v1, prefs.getLong(key, defaultValue), i);
				prefs.putLong(key, v2);
				assertEquals(v2, prefs.getLong(key, defaultValue), i);
				prefs.remove(key);
				assertEquals(defaultValue, prefs.getLong(key, defaultValue), i);
			}

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals(stringValue, prefs.get(key, null));
			assertEquals(defaultValue, prefs.getLong(key, defaultValue));

			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.getLong(null, defaultValue));
			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.putLong(null, defaultValue));
		} finally {
			// clean-up
			prefs.removeNode();
		}

		// spec'd to throw IllegalStateException if node has been removed
		assertThrows(IllegalStateException.class, () -> prefs.getLong(key, defaultValue));
	}

	@Test
	public void testBoolean() throws BackingStoreException {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final boolean defaultValue = false;

		try {
			// nothing there so expect the default
			assertEquals(defaultValue, prefs.getBoolean(key, defaultValue));

			prefs.putBoolean(key, true);
			assertEquals(true, prefs.getBoolean(key, defaultValue));
			prefs.putBoolean(key, false);
			assertEquals(false, prefs.getBoolean(key, defaultValue));
			prefs.remove(key);
			assertEquals(defaultValue, prefs.getBoolean(key, defaultValue));

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals(stringValue, prefs.get(key, null));
			assertEquals(defaultValue, prefs.getBoolean(key, defaultValue));

			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.getBoolean(null, defaultValue));
			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.putBoolean(null, defaultValue));
		} finally {
			// clean-up
			prefs.removeNode();
		}

		// spec'd to throw IllegalStateException if node has been removed
		assertThrows(IllegalStateException.class, () -> prefs.getBoolean(key, defaultValue));
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

	@Test
	public void testBytes() throws BackingStoreException {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final byte[] defaultValue = new byte[] {42};
		final byte[][] values = getByteValues();

		try {
			// nothing there so expect the default
			assertThat(prefs.getByteArray(key, defaultValue)).containsExactly(defaultValue);

			// try for each value in the set
			for (byte[] value : values) {
				byte[] v1 = value;
				byte[] v2 = new byte[] {54};
				prefs.putByteArray(key, v1);
				assertThat(prefs.getByteArray(key, defaultValue)).as(value.toString()).containsExactly(v1);
				prefs.putByteArray(key, v2);
				assertThat(prefs.getByteArray(key, defaultValue)).as(value.toString()).containsExactly(v2);
				prefs.remove(key);
				assertThat(prefs.getByteArray(key, defaultValue)).as(value.toString()).containsExactly(defaultValue);
			}

			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.getByteArray(null, defaultValue));
			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.putByteArray(null, defaultValue));
			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.putByteArray(key, null));
		} finally {
			// clean-up
			prefs.removeNode();
		}

		// spec'd to throw IllegalStateException if node has been removed
		assertThrows(IllegalStateException.class, () -> prefs.getByteArray(key, defaultValue));
	}

	@Test
	public void testFloat() throws BackingStoreException {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final float defaultValue = 42f;
		final float[] values = {-12345f, 0f, 12345f, Float.MAX_VALUE, Float.MIN_VALUE};
		final float tol = 1.0e-20f;

		try {
			// nothing there so expect the default
			assertEquals(defaultValue, prefs.getFloat(key, defaultValue), tol);

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				float v1 = values[i];
				float v2 = 54f;
				prefs.putFloat(key, v1);
				assertEquals(v1, prefs.getFloat(key, defaultValue), tol, i + "");
				prefs.putFloat(key, v2);
				assertEquals(v2, prefs.getFloat(key, defaultValue), tol, i + "");
				prefs.remove(key);
				assertEquals(defaultValue, prefs.getFloat(key, defaultValue), tol, i + "");
			}

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals(stringValue, prefs.get(key, null));
			assertEquals(defaultValue, prefs.getFloat(key, defaultValue), tol);

			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.getFloat(null, defaultValue));
			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.putFloat(null, defaultValue));
		} finally {
			// clean-up
			prefs.removeNode();
		}

		// spec'd to throw IllegalStateException if node has been removed
		assertThrows(IllegalStateException.class, () -> prefs.getFloat(key, defaultValue));
	}

	@Test
	public void testFlushDeadlock() throws InterruptedException {
		String pluginId = RuntimeTestsPlugin.PI_RUNTIME_TESTS;
		final IEclipsePreferences parent = InstanceScope.INSTANCE.getNode(pluginId);
		final Preferences child = parent.node("testFlushDeadlock");
		class FlushJob extends Job {
			private final Preferences node;

			FlushJob(Preferences node) {
				super("testFlushDeadlock");
				this.node = node;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					node.flush();
				} catch (BackingStoreException e) {
					return new Status(IStatus.ERROR, pluginId, "unexpected flush failure", e);
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

		flushParent.join();
		flushChild.join();
	}

	@Test
	public void testDouble() throws BackingStoreException {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final double defaultValue = 42.0;
		final double[] values = {0.0, 1002.5, -201788.55, Double.MAX_VALUE, Double.MIN_VALUE};
		final double tol = 1.0e-20;

		try {

			// nothing there so expect the default
			assertEquals(defaultValue, prefs.getDouble(key, defaultValue), tol);

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				double v1 = values[i];
				double v2 = 54.0;
				prefs.putDouble(key, v1);
				assertEquals(v1, prefs.getDouble(key, defaultValue), tol, i + "");
				prefs.putDouble(key, v2);
				assertEquals(v2, prefs.getDouble(key, defaultValue), tol, i + "");
				prefs.remove(key);
				assertEquals(defaultValue, prefs.getDouble(key, defaultValue), tol, i + "");
			}

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals(stringValue, prefs.get(key, null));
			assertEquals(defaultValue, prefs.getDouble(key, defaultValue), tol);

			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.getDouble(null, defaultValue));
			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.putDouble(null, defaultValue));
		} finally {
			// clean-up
			prefs.removeNode();
		}

		// spec'd to throw IllegalStateException if node has been removed
		assertThrows(IllegalStateException.class, () -> prefs.getDouble(key, defaultValue));
	}

	@Test
	public void testInt() throws BackingStoreException {
		String qualifier = getUniqueString();
		Preferences prefs = getScopeRoot().node(qualifier);
		final String key = "key1";
		final int defaultValue = 42;
		final int[] values = {0, 1002, -201788, Integer.MAX_VALUE, Integer.MIN_VALUE};

		try {
			// nothing there so expect the default
			assertEquals(defaultValue, prefs.getInt(key, defaultValue));

			// try for each value in the set
			for (int i = 0; i < values.length; i++) {
				int v1 = values[i];
				int v2 = 54;
				prefs.putInt(key, v1);
				assertEquals(v1, prefs.getInt(key, defaultValue), i);
				prefs.putInt(key, v2);
				assertEquals(v2, prefs.getInt(key, defaultValue), i);
				prefs.remove(key);
				assertEquals(defaultValue, prefs.getInt(key, defaultValue), i);
			}

			String stringValue = "foo";
			prefs.put(key, stringValue);
			assertEquals(stringValue, prefs.get(key, null));
			assertEquals(defaultValue, prefs.getInt(key, defaultValue));

			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.getInt(null, defaultValue));
			// spec'd to throw a NPE if key is null
			assertThrows(NullPointerException.class, () -> prefs.putInt(null, defaultValue));
		} finally {
			// clean-up
			prefs.removeNode();
		}

		// spec'd to throw IllegalStateException if node has been removed
		assertThrows(IllegalStateException.class, () -> prefs.getInt(key, defaultValue));
	}

	@Test
	public void testRemoveNode() throws BackingStoreException {
		Preferences root = getScopeRoot();
		ArrayList<Preferences> list = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			list.add(root.node(getUniqueString()));
		}

		// all exist
		for (Iterator<Preferences> i = list.iterator(); i.hasNext();) {
			Preferences node = i.next();
			assertTrue(node.nodeExists(""), i + "");
		}

		// remove each
		for (Iterator<Preferences> i = list.iterator(); i.hasNext();) {
			Preferences node = i.next();
			node.removeNode();
			assertFalse(node.nodeExists(""), i + "");
		}
	}

	/*
	 * Test for bug 367366.
	 * TODO re-enable when the bug is fixed
	 */
	@Test
	@Disabled("see bug 367366")
	public void _testRemoveDeletesFile() throws BackingStoreException {
		Preferences node = InstanceScope.INSTANCE.getNode("foo");
		Preferences parent = node.parent();
		node.put("a", "b");
		node.flush();
		File file = TestHelper.getInstanceBaseLocation().append(".settings").append("foo.prefs").toFile();

		assertTrue(file.exists());
		node.removeNode();
		parent.flush();
		// ensure file was deleted
		assertFalse(file.exists());
	}

	@Test
	public void testName() {
		Preferences node = Platform.getPreferencesService().getRootNode();

		assertEquals("", node.name());
		node = node.node(TestScope.SCOPE);
		assertEquals(TestScope.SCOPE, node.name());
		node = node.node("foo");
		assertEquals("foo", node.name());
	}

	@Test
	public void testNode() {
		Preferences node = Platform.getPreferencesService().getRootNode();

		// root node
		assertNotNull(node);
		assertEquals("", node.name());
		assertEquals("/", node.absolutePath());
		// Bug 57150 [runtime] prefs: root.node("/") should return root
		assertEquals(node, node.node("/"));

		// scope root
		node = node.node(TestScope.SCOPE);
		assertNotNull(node);
		assertEquals(TestScope.SCOPE, node.name());
		assertEquals("/" + TestScope.SCOPE, node.absolutePath());

		// child
		String name = getUniqueString();
		node = node.node(name);
		assertNotNull(node);
		assertEquals(name, node.name());
		assertEquals("/" + TestScope.SCOPE + "/" + name, node.absolutePath());
	}

	@Test
	public void testParent() {
		// parent of the root is null
		assertNull(Platform.getPreferencesService().getRootNode().parent());

		// parent of the scope root is the root
		Preferences node = Platform.getPreferencesService().getRootNode().node(TestScope.SCOPE);
		Preferences parent = node.parent();
		assertEquals("/", parent.absolutePath());

		// parent of a child is the scope root
		node = getScopeRoot().node(getUniqueString());
		parent = node.parent();
		assertEquals("/" + TestScope.SCOPE, parent.absolutePath());
	}

	@Test
	public void testKeys() throws BackingStoreException {
		String[] keys = new String[] {"foo", "bar", "quux"};
		Preferences node = getScopeRoot().node(getUniqueString());

		// ensure nothing exists to begin with
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			assertNull(node.get(key, null), i + "");
		}

		// set all keys
		for (String key : keys) {
			node.put(key, getUniqueString());
		}

		// get the key list
		String[] result = node.keys();
		assertThat(keys).containsExactlyInAnyOrder(result);
	}

	@Test
	public void testChildrenNames() throws BackingStoreException {
		String[] childrenNames = new String[] {"foo", "bar", "quux"};
		Preferences node = getScopeRoot().node(getUniqueString());
		String[] result = null;

		// no children to start
		result = node.childrenNames();
		assertThat(result).isEmpty();

		// add children
		for (String childrenName : childrenNames) {
			node.node(childrenName);
		}
		result = node.childrenNames();
		assertThat(childrenNames).containsExactlyInAnyOrder(result);
	}

	@Test
	public void testNodeExists() throws BackingStoreException {
		Preferences parent = null;
		Preferences node = Platform.getPreferencesService().getRootNode();
		String[] childrenNames = new String[] {"foo", "bar", "quux"};
		String fake = "fake";

		// check the root node
		assertTrue(node.nodeExists(""));
		assertFalse(node.nodeExists(fake));

		// check the scope root
		parent = node;
		node = getScopeRoot();
		assertTrue(parent.nodeExists(node.name()));
		assertTrue(node.nodeExists(""));
		assertFalse(parent.nodeExists(fake));
		assertFalse(node.nodeExists(fake));

		// check a child
		parent = node;
		node = parent.node(getUniqueString());
		assertTrue(parent.nodeExists(node.name()));
		assertTrue(node.nodeExists(""));
		assertFalse(parent.nodeExists(fake));
		assertFalse(node.nodeExists(fake));

		// create some more children and check
		parent = node;
		Preferences[] nodes = new Preferences[childrenNames.length];
		for (int i = 0; i < childrenNames.length; i++) {
			nodes[i] = parent.node(childrenNames[i]);
		}
		for (String childrenName : childrenNames) {
			assertTrue(parent.nodeExists(childrenName));
			assertFalse(parent.nodeExists(fake));
		}
		for (Preferences preferenceNode : nodes) {
			assertTrue(preferenceNode.nodeExists(""));
		}

		// remove children and check
		for (Preferences n : nodes) {
			n.removeNode();
			assertFalse(parent.nodeExists(n.name()));
			assertFalse(n.nodeExists(""));
		}
	}

	@Test
	public void testClear() throws BackingStoreException {
		Preferences node = getScopeRoot().node(getUniqueString());
		String[] keys = new String[] {"foo", "bar", "quux"};
		String[] values = new String[] {getUniqueString(), getUniqueString(), getUniqueString()};

		// none to start with
		assertThat(node.keys()).isEmpty();

		// fill the node up with values
		for (int i = 0; i < keys.length; i++) {
			node.put(keys[i], values[i]);
		}
		assertThat(keys).containsExactlyInAnyOrder(node.keys());

		// clear the values and check
		node.clear();
		assertThat(node.keys()).isEmpty();
		for (int i = 0; i < keys.length; i++) {
			assertNull(node.get(keys[i], null), i + "");
		}
	}

	@Test
	public void testAbsolutePath() {
		IPath expected = IPath.ROOT;
		Preferences node = Platform.getPreferencesService().getRootNode();

		// root node
		assertEquals(expected.toString(), node.absolutePath());

		// scope root
		expected = expected.append(TestScope.SCOPE);
		node = node.node(TestScope.SCOPE);
		assertEquals(expected.toString(), node.absolutePath());

		// another child
		String name = getUniqueString();
		expected = expected.append(name);
		node = node.node(name);
		assertEquals(expected.toString(), node.absolutePath());
	}

	@Test
	public void testAccept() throws BackingStoreException {
		IEclipsePreferences scopeRoot = getScopeRoot();
		ArrayList<String> expected = new ArrayList<>();
		final ArrayList<String> actual = new ArrayList<>();

		IPreferenceNodeVisitor visitor = node -> {
			actual.add(node.absolutePath());
			return true;
		};

		// just the scope root
		scopeRoot.accept(visitor);
		expected.add(scopeRoot.absolutePath());
		assertThat(actual).containsExactlyInAnyOrderElementsOf(actual);

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
		scopeRoot.accept(visitor);
		assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
	}

	@Test
	public void testPreferenceChangeListeners() {
		IEclipsePreferences node = getScopeRoot();
		PreferenceTracer tracer = new PreferenceTracer();
		node.addPreferenceChangeListener(tracer);

		String key = "foo";

		// initial state
		assertEquals("", tracer.log.toString());

		// add preference (string value)
		node.put(key, "bar");
		String string = node.get(key, null);
		assertNotNull(string);
		assertEquals("bar", string);
		assertEquals("[foo:null->Sbar]", tracer.log.toString());

		// change its value
		tracer.log.setLength(0);
		node.put(key, "quux");
		string = node.get(key, null);
		assertNotNull(string);
		assertEquals("quux", string);
		assertEquals("[foo:Sbar->Squux]", tracer.log.toString());

		// change its type - should have no effect (events are strings)
		tracer.log.setLength(0);
		node.putInt(key, 123);
		int i = node.getInt(key, 0);
		assertEquals(123, i);
		assertEquals("[foo:Squux->S123]", tracer.log.toString());

		node.put(key, "aaa");
		tracer.log.setLength(0);
		node.remove(key);
		assertNull(node.get(key, null));
		assertEquals("[foo:Saaa->null]", tracer.log.toString());
	}

	@Test
	public void testNodeChangeListeners() throws BackingStoreException {
		IEclipsePreferences root = getScopeRoot();
		NodeTracer tracer = new NodeTracer();
		root.addNodeChangeListener(tracer);

		// initial state
		assertEquals("", tracer.log.toString());

		// add a child
		String name = getUniqueString();
		IPath parent = IPath.fromOSString(root.absolutePath());
		IPath child = parent.append(name);
		Preferences node = root.node(name);
		assertEquals("[A:" + parent + ',' + child + ']', tracer.log.toString());

		// remove the child
		tracer.log.setLength(0);
		node.removeNode();
		assertEquals("[R:" + parent + ',' + child + ']', tracer.log.toString());

		// remove the listener and make sure we don't get any changes
		root.removeNodeChangeListener(tracer);
		tracer.log.setLength(0);
		root.node(name);
		assertEquals("", tracer.log.toString());
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
	@Test
	public void test_60590() throws BackingStoreException {
		IEclipsePreferences root = Platform.getPreferencesService().getRootNode();
		String one = getUniqueString();
		String two = getUniqueString();
		String threeA = getUniqueString();
		String threeB = getUniqueString();
		String key = "key";
		String value = "value";
		Preferences node = root.node(TestScope.SCOPE).node(one).node(two).node(threeA);
		node.put(key, value);
		node.flush();
		node = root.node(TestScope.SCOPE).node(one).node(two).node(threeB);
		node.put(key, value);
		Preferences current = node;
		int count = 0;
		while (current != null && current instanceof EclipsePreferences && current.parent() != null && IPath.fromOSString(current.absolutePath()).segment(0).equals(TestScope.SCOPE)) {
			assertTrue(((EclipsePreferences) current).isDirty(), current.absolutePath());
			count++;
			current = current.parent();
		}
		assertEquals(4, count);
	}

	/*
	 * Bug 342709 - [prefs] Don't write date/timestamp comment in preferences file
	 */
	@Test
	public void test_342709() throws Exception {
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
		node.flush();

		assertTrue(node instanceof TestScope2);

		// read the file outside of the pref mechanism
		IPath location = ((TestScope2) node).getLocation();
		Collection<String> lines = null;
		lines = read(location);

		// ensure there is no comment or timestamp in the file
		for (String line : lines) {
			assertFalse(line.startsWith("#"), line);
		}
	}

	public static Collection<String> read(IPath location) throws IOException {
		Collection<String> result = new ArrayList<>();
		try (FileReader fileReader = new FileReader(location.toFile())) {
			try (BufferedReader reader = new BufferedReader(fileReader)) {
				String line;
				while ((line = reader.readLine()) != null) {
					result.add(line);
				}
			}
		}
		return result;
	}

	/*
	 * Bug 55410 - [runtime] prefs: keys and valid chars
	 */
	@Test
	public void test_55410() {
		String[] keys = new String[] {"my/key", "my:key", "my/long:key"};
		String[] paths = new String[] {"my/path", "my:path"};
		Preferences node = Platform.getPreferencesService().getRootNode().node(TestScope.SCOPE).node(getUniqueString());

		// test keys
		for (String key : keys) {
			String value = getUniqueString();
			node.put(key, value);
			assertEquals(value, node.get(key, null), key);
		}

		// test paths
		String root = node.absolutePath();
		for (String path : paths) {
			String expected = root + IPath.SEPARATOR + path;
			String actual = node.node(path).absolutePath();
			assertEquals(expected, actual, path);
		}
	}

	@Test
	public void testFileFormat() throws BackingStoreException {
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

		assertTrue(node instanceof EclipsePreferences);

		Properties properties = null;
		properties = TestHelper.convertToProperties((EclipsePreferences) node, "");

		for (Object object : properties.keySet()) {
			String key = (String) object;
			String value = properties.getProperty(key);
			Info info = list.get(Integer.parseInt(value));
			assertNotNull(info);
			assertEquals(info.encoded, key);
		}
	}

	private Properties loadProperties(IPath location) throws FileNotFoundException, IOException {
		Properties result = new Properties();
		if (!location.toFile().exists()) {
			return result;
		}
		try (InputStream input = new FileInputStream(location.toFile())) {
			result.load(input);
		}
		return result;
	}

	@Test
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
			assertEquals(item.expected, EclipsePreferences.encodePath(item.path, item.key), "a" + i + item.expected);
			String[] result = EclipsePreferences.decodePath(item.expected);
			assertEquals(item.path, result[0], "b" + i + item.path);
			assertEquals(item.key, result[1], "c" + i + item.key);
		}
	}

	@Test
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
			assertEquals(line[0], EclipsePreferences.getSegment(line[1], Integer.parseInt(line[2])),
					i + ':' + line[1] + " (" + line[2] + ')');
		}
	}

	@Test
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
			assertEquals(Integer.parseInt(line[1]), EclipsePreferences.getSegmentCount(line[0]), line[0]);
		}
	}

	@Test
	public void test_68897() throws Exception {
		Path file = FileSystemHelper.getRandomLocation().toPath();
		IPreferencesService service = Platform.getPreferencesService();

		IEclipsePreferences rootPreferences = service.getRootNode();
		Preferences pref = rootPreferences.node("/favorite");

		Preferences child = pref.node("my");
		child.put("file", "my.txt");
		child.flush();
		pref.flush();
		rootPreferences.flush();
		try (OutputStream outputStream = Files.newOutputStream(file)) {
			service.exportPreferences(rootPreferences, outputStream, (String[]) null);
		}
		try (InputStream inputStream = Files.newInputStream(file)) {
			IExportedPreferences epref = service.readPreferences(inputStream);
			service.applyPreferences(epref);
		}
	}

	public String TEST_NODE_PATH = "test.node.path";
	public String TEST_PREF_KEY = "test.pref.key";

	@Test
	public void testGetByteArray() {
		final byte[] testArray = new byte[] {10, 13, 15, 20};
		IScopeContext defaultScope = DefaultScope.INSTANCE;
		defaultScope.getNode(TEST_NODE_PATH).putByteArray(TEST_PREF_KEY, testArray);
		final byte[] returnArray = Platform.getPreferencesService().getByteArray(TEST_NODE_PATH, TEST_PREF_KEY, new byte[] {}, null);
		assertThat(returnArray).isEqualTo(testArray);
	}

	/*
	 * Some tests to handle user-defined node storage.
	 */
	@Test
	public void testNode3() throws Exception {
		IPreferencesService service = Platform.getPreferencesService();
		IEclipsePreferences rootPreferences = service.getRootNode();
		Preferences node = rootPreferences.node("test3");

		// check that we have the expected children
		File file = RuntimeTestsPlugin.getTestData("testData/preferences/test3");
		Collection<String> expectedChildren = Arrays.asList(file.list());
		String[] children = node.childrenNames();
		for (String child : children) {
			assertTrue(expectedChildren.contains(child), child);
		}

		// check the child has the expected values
		Preferences child = node.node("foo");
		assertThat(child.keys()).hasSize(2);
		assertEquals("value1", child.get("key1", null));
		assertEquals("value2", child.get("key2", null));

		// set a new value, flush (which saves the file) and check the file contents
		child.put("key8", "value8");
		child.flush();
		String prop = System.getProperty("equinox.preference.test.TestNodeStorage3,root");
		assertNotNull(prop);
		File rootFile = new File(prop);
		File childFile = new File(rootFile, "foo");
		assertTrue(childFile.exists());
		Properties contents = loadProperties(IPath.fromOSString(childFile.getAbsolutePath()));
		assertEquals("value8", contents.getProperty("key8", null));

		// delete the node (which should remove the file)
		child.removeNode();
		assertFalse(childFile.exists());
	}

	private static String getUniqueString() {
		return UUID.randomUUID().toString();
	}

}
