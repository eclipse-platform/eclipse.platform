/*******************************************************************************
 *  Copyright (c) 2004, 2018 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.internal.preferences.legacy.PreferenceForwarder;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.jupiter.api.Test;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Test suite for API class org.eclipse.core.runtime.Preferences
 * @deprecated This class tests intentionally tests deprecated functionality, so tag
 * added to hide deprecation reference warnings.
 */
@Deprecated
@SuppressWarnings("restriction")
public class PreferenceForwarderTest {

	@Deprecated
	static class Tracer implements Preferences.IPropertyChangeListener {
		@Deprecated
		public StringBuilder log = new StringBuilder();

		private String typeCode(Object value) {
			if (value == null)
			 {
				return ""; //$NON-NLS-1$
			}
			if (value instanceof Boolean)
			 {
				return "B"; //$NON-NLS-1$
			}
			if (value instanceof Integer)
			 {
				return "I"; //$NON-NLS-1$
			}
			if (value instanceof Long)
			 {
				return "L"; //$NON-NLS-1$
			}
			if (value instanceof Float)
			 {
				return "F"; //$NON-NLS-1$
			}
			if (value instanceof Double)
			 {
				return "D"; //$NON-NLS-1$
			}
			if (value instanceof String)
			 {
				return "S"; //$NON-NLS-1$
			}
			fail();
			return null;
		}

		@Deprecated
		@Override
		public void propertyChange(Preferences.PropertyChangeEvent event) {
			log.append('[');
			log.append(event.getProperty());
			log.append(':');
			log.append(typeCode(event.getOldValue()));
			log.append(event.getOldValue() == null ? "null" : event.getOldValue()); //$NON-NLS-1$
			log.append("->"); //$NON-NLS-1$
			log.append(typeCode(event.getNewValue()));
			log.append(event.getNewValue() == null ? "null" : event.getNewValue()); //$NON-NLS-1$
			log.append(']');
		}
	}

	@Deprecated
	@Test
	public void testConstants() {
		// make sure that the preference store constants are defined properly
		assertEquals(false, Preferences.BOOLEAN_DEFAULT_DEFAULT);
		assertEquals(0, Preferences.INT_DEFAULT_DEFAULT);
		assertEquals(0L, Preferences.LONG_DEFAULT_DEFAULT);
		assertEquals(0.0f, Preferences.FLOAT_DEFAULT_DEFAULT, 0.0f);
		assertEquals(0.0, Preferences.DOUBLE_DEFAULT_DEFAULT, 0.0);
		assertEquals("", Preferences.STRING_DEFAULT_DEFAULT);
	}

	@Deprecated
	@Test
	public void testBasics() {

		Preferences ps = new PreferenceForwarder(getUniqueString());
		final String k1 = "key1";
		final String v1 = "1";
		final String v2 = "2";
		final String v3 = "3";

		// check that a random property in a newly created store
		// appearchs to have default-default values of whatever type asked for
		assertEquals(true, ps.isDefault(k1));
		assertEquals(Preferences.BOOLEAN_DEFAULT_DEFAULT, ps.getBoolean(k1));
		assertEquals(Preferences.INT_DEFAULT_DEFAULT, ps.getInt(k1));
		assertEquals(Preferences.LONG_DEFAULT_DEFAULT, ps.getLong(k1));
		assertEquals(Preferences.FLOAT_DEFAULT_DEFAULT, ps.getFloat(k1), 0.0f);
		assertEquals(Preferences.DOUBLE_DEFAULT_DEFAULT, ps.getDouble(k1), 0.0);
		assertEquals(Preferences.STRING_DEFAULT_DEFAULT, ps.getString(k1));

		assertEquals(Preferences.BOOLEAN_DEFAULT_DEFAULT, ps.getDefaultBoolean(k1));
		assertEquals(Preferences.INT_DEFAULT_DEFAULT, ps.getDefaultInt(k1));
		assertEquals(Preferences.LONG_DEFAULT_DEFAULT, ps.getDefaultLong(k1));
		assertEquals(Preferences.FLOAT_DEFAULT_DEFAULT, ps.getDefaultFloat(k1), 0.0f);
		assertEquals(Preferences.DOUBLE_DEFAULT_DEFAULT, ps.getDefaultDouble(k1), 0.0);
		assertEquals(Preferences.STRING_DEFAULT_DEFAULT, ps.getDefaultString(k1));

		// test set/getString
		// give it a value
		ps.setValue(k1, v1);
		assertFalse(ps.isDefault(k1));
		assertEquals(v1, ps.getString(k1));
		assertEquals(Preferences.STRING_DEFAULT_DEFAULT, ps.getDefaultString(k1));
		// change the value
		ps.setValue(k1, v2);
		assertFalse(ps.isDefault(k1));
		assertEquals(v2, ps.getString(k1));
		assertEquals(Preferences.STRING_DEFAULT_DEFAULT, ps.getDefaultString(k1));
		// change to same value as default
		ps.setValue(k1, ps.getDefaultString(k1));
		assertTrue(ps.isDefault(k1));
		assertEquals(ps.getDefaultString(k1), ps.getString(k1));
		assertEquals(Preferences.STRING_DEFAULT_DEFAULT, ps.getDefaultString(k1));
		// reset to default
		ps.setValue(k1, v2);
		ps.setToDefault(k1);
		assertTrue(ps.isDefault(k1));
		assertEquals(Preferences.STRING_DEFAULT_DEFAULT, ps.getString(k1));
		assertEquals(Preferences.STRING_DEFAULT_DEFAULT, ps.getDefaultString(k1));
		// change default
		ps.setDefault(k1, v1);
		assertTrue(ps.isDefault(k1));
		assertEquals(v1, ps.getString(k1));
		assertEquals(v1, ps.getDefaultString(k1));
		// set the value
		ps.setValue(k1, v2);
		assertFalse(ps.isDefault(k1));
		assertEquals(v2, ps.getString(k1));
		assertEquals(v1, ps.getDefaultString(k1));
		// change to same value as default
		ps.setValue(k1, ps.getDefaultString(k1));
		assertTrue(ps.isDefault(k1));
		assertEquals(ps.getDefaultString(k1), ps.getString(k1));
		assertEquals(v1, ps.getDefaultString(k1));
		// reset to default
		ps.setValue(k1, v2);
		ps.setToDefault(k1);
		assertTrue(ps.isDefault(k1));
		assertEquals(v1, ps.getString(k1));
		assertEquals(v1, ps.getDefaultString(k1));
		// change default
		ps.setDefault(k1, v3);
		assertTrue(ps.isDefault(k1));
		assertEquals(v3, ps.getString(k1));
		assertEquals(v3, ps.getDefaultString(k1));

	}

	@Deprecated
	@Test
	public void testBoolean() {

		Preferences ps = new PreferenceForwarder(getUniqueString());
		final String k1 = "key1";

		assertEquals(false, Preferences.BOOLEAN_DEFAULT_DEFAULT);
		assertEquals(Preferences.BOOLEAN_DEFAULT_DEFAULT, ps.getBoolean(k1));

		ps.setValue(k1, true);
		assertEquals(true, ps.getBoolean(k1));
		ps.setValue(k1, false);
		assertEquals(false, ps.getBoolean(k1));

		ps.setDefault(k1, true);
		assertEquals(true, ps.getDefaultBoolean(k1));
		ps.setDefault(k1, false);
		assertEquals(false, ps.getDefaultBoolean(k1));

	}

	@Deprecated
	@Test
	public void testInteger() {

		Preferences ps = new PreferenceForwarder(getUniqueString());
		final String k1 = "key1";
		final int[] values = {0, 1002, -201788, Integer.MAX_VALUE, Integer.MIN_VALUE};

		assertEquals(0, Preferences.INT_DEFAULT_DEFAULT);
		assertEquals(Preferences.INT_DEFAULT_DEFAULT, ps.getInt(k1));

		for (int v1 : values) {
			int v2 = v1 + 1;
			ps.setValue(k1, v1);
			assertEquals(v1, ps.getInt(k1));
			ps.setDefault(k1, v2);
			assertEquals(v2, ps.getDefaultInt(k1));
		}
	}

	@Deprecated
	@Test
	public void testLong() {

		Preferences ps = new PreferenceForwarder(getUniqueString());
		final String k1 = "key1";
		final long[] values = {0L, 1002L, -201788L, Long.MAX_VALUE, Long.MIN_VALUE};

		assertEquals(0L, Preferences.LONG_DEFAULT_DEFAULT);
		assertEquals(Preferences.LONG_DEFAULT_DEFAULT, ps.getLong(k1));

		for (long v1 : values) {
			long v2 = v1 + 1;
			ps.setValue(k1, v1);
			assertEquals(v1, ps.getLong(k1));
			ps.setDefault(k1, v2);
			assertEquals(v2, ps.getDefaultLong(k1));
		}
	}

	@Deprecated
	@Test
	public void testFloat() {

		Preferences ps = new PreferenceForwarder(getUniqueString());
		final String k1 = "key1";
		final float[] values = {0.0f, 1002.5f, -201788.55f, Float.MAX_VALUE, Float.MIN_VALUE};
		final float tol = 1.0e-20f;

		assertEquals(0.0f, Preferences.FLOAT_DEFAULT_DEFAULT, tol);
		assertEquals(Preferences.FLOAT_DEFAULT_DEFAULT, ps.getFloat(k1), tol);

		for (float v1 : values) {
			float v2 = v1 + 1.0f;
			ps.setValue(k1, v1);
			assertEquals(v1, ps.getFloat(k1), tol);
			ps.setDefault(k1, v2);
			assertEquals(v2, ps.getDefaultFloat(k1), tol);
		}

		assertThrows(IllegalArgumentException.class, () -> ps.setValue(k1, Float.NaN));
	}

	@Deprecated
	@Test
	public void testDouble() {

		Preferences ps = new PreferenceForwarder(getUniqueString());
		final String k1 = "key1";
		final double[] values = {0.0, 1002.5, -201788.55, Double.MAX_VALUE, Double.MIN_VALUE};
		final double tol = 1.0e-20;

		assertEquals(0.0, Preferences.DOUBLE_DEFAULT_DEFAULT, tol);
		assertEquals(Preferences.DOUBLE_DEFAULT_DEFAULT, ps.getDouble(k1), tol);

		for (double v1 : values) {
			double v2 = v1 + 1.0;
			ps.setValue(k1, v1);
			assertEquals(v1, ps.getDouble(k1), tol);
			ps.setDefault(k1, v2);
			assertEquals(v2, ps.getDefaultDouble(k1), tol);
		}

		assertThrows(IllegalArgumentException.class, () -> ps.setValue(k1, Float.NaN));
	}

	@Deprecated
	@Test
	public void testString() {

		Preferences ps = new PreferenceForwarder(getUniqueString());
		final String k1 = "key1";
		final String[] values = {"", "hello", " x ", "\n"};

		assertEquals("", Preferences.STRING_DEFAULT_DEFAULT);
		assertEquals(ps.getString(k1), Preferences.STRING_DEFAULT_DEFAULT);

		for (String v1 : values) {
			String v2 = v1 + "x";
			ps.setValue(k1, v1);
			assertEquals(v1, ps.getString(k1));
			ps.setDefault(k1, v2);
			assertEquals(v2, ps.getDefaultString(k1));
		}
	}

	@Deprecated
	@Test
	public void testPropertyNames() {

		Preferences ps = new PreferenceForwarder(getUniqueString());

		// there are no properties initially
		assertThat(ps.propertyNames()).isEmpty();

		String[] keys = {"a", "b", "c", "d"};

		// setting defaults does not add name to set
		for (String key : keys) {
			ps.setDefault(key, "default");
		}
		assertThat(ps.propertyNames()).isEmpty();

		// setting real values does add name to set
		for (String key : keys) {
			ps.setValue(key, "actual");
		}
		assertThat(ps.propertyNames()).hasSameSizeAs(keys);

		Set<String> s1 = new HashSet<>(Arrays.asList(keys));
		Set<String> s2 = new HashSet<>(Arrays.asList(ps.propertyNames()));
		assertEquals(s1, s2);

		// setting to default does remove name from set
		for (String key : keys) {
			ps.setToDefault(key);
			Set<String> s = new HashSet<>(Arrays.asList(ps.propertyNames()));
			assertFalse(s.contains(key));
		}
		assertThat(ps.propertyNames()).isEmpty();
	}

	@Deprecated
	@Test
	public void testContains() {

		Preferences ps = new PreferenceForwarder(getUniqueString());

		// there are no properties initially
		assertEquals(false, ps.contains("a"));

		// setting defaults adds name
		ps.setDefault("a", "default");
		assertEquals(true, ps.contains("a"));

		// setting value adds name
		assertEquals(false, ps.contains("b"));
		ps.setValue("b", "any");
		assertEquals(true, ps.contains("b"));

		// setting value does not remove entry already there
		ps.setValue("a", "any");
		assertEquals(true, ps.contains("a"));
		assertEquals(true, ps.contains("b"));

		// setting to default removes name from set unless there is a default too
		ps.setToDefault("b");
		assertEquals(false, ps.contains("b"));
		ps.setToDefault("a");
		assertEquals(true, ps.contains("a"));

		// test bug 62586
		// fail gracefully in PreferenceForwarder.contains(null)
		assertFalse(ps.contains(null));
	}

	@Deprecated
	@Test
	public void testDefaultPropertyNames() {

		Preferences ps = new PreferenceForwarder(getUniqueString());

		// there are no default properties initially
		assertThat(ps.defaultPropertyNames()).isEmpty();

		String[] keys = {"a", "b", "c", "d"};

		// setting actual values does not add name to set
		for (String key : keys) {
			ps.setValue(key, "actual");
		}
		assertThat(ps.defaultPropertyNames()).isEmpty();

		// setting defaults does add name to set
		for (String key : keys) {
			ps.setDefault(key, "default");
		}
		assertThat(ps.defaultPropertyNames()).hasSameSizeAs(keys);

		Set<String> s1 = new HashSet<>(Arrays.asList(keys));
		Set<String> s2 = new HashSet<>(Arrays.asList(ps.defaultPropertyNames()));
		assertEquals(s1, s2);

		// setting to default does not remove name from set
		for (String key : keys) {
			ps.setToDefault(key);
			Set<String> s = new HashSet<>(Arrays.asList(ps.defaultPropertyNames()));
			assertTrue(s.contains(key));
		}
		assertThat(ps.defaultPropertyNames()).hasSameSizeAs(keys);

		// setting to default-default does not remove name from set either
		for (String key : keys) {
			ps.setDefault(key, Preferences.STRING_DEFAULT_DEFAULT);
			Set<String> s = new HashSet<>(Arrays.asList(ps.defaultPropertyNames()));
			assertTrue(s.contains(key));

			ps.setDefault(key, Preferences.BOOLEAN_DEFAULT_DEFAULT);
			s = new HashSet<>(Arrays.asList(ps.defaultPropertyNames()));
			assertTrue(s.contains(key));

			ps.setDefault(key, Preferences.INT_DEFAULT_DEFAULT);
			s = new HashSet<>(Arrays.asList(ps.defaultPropertyNames()));
			assertTrue(s.contains(key));

			ps.setDefault(key, Preferences.LONG_DEFAULT_DEFAULT);
			s = new HashSet<>(Arrays.asList(ps.defaultPropertyNames()));
			assertTrue(s.contains(key));

			ps.setDefault(key, Preferences.FLOAT_DEFAULT_DEFAULT);
			s = new HashSet<>(Arrays.asList(ps.defaultPropertyNames()));
			assertTrue(s.contains(key));

			ps.setDefault(key, Preferences.DOUBLE_DEFAULT_DEFAULT);
			s = new HashSet<>(Arrays.asList(ps.defaultPropertyNames()));
			assertTrue(s.contains(key));
		}
		assertThat(ps.defaultPropertyNames()).hasSameSizeAs(keys);
	}

	@Deprecated
	@Test
	public void test55138() {
		final Preferences ps = new PreferenceForwarder(getUniqueString());

		final Tracer tracer1 = new Tracer();
		String key = "foo";

		// register one listener
		ps.addPropertyChangeListener(tracer1);
		assertEquals("", tracer1.log.toString());

		// boolean value
		boolean booleanDefault = true;
		boolean booleanValue = false;
		ps.setDefault(key, booleanDefault);
		assertEquals("", tracer1.log.toString());

		tracer1.log.setLength(0);
		ps.setValue(key, booleanValue);
		assertEquals("[foo:Btrue->Bfalse]", tracer1.log.toString());

		ps.setValue(key, booleanDefault);
		assertEquals("[foo:Btrue->Bfalse][foo:Bfalse->Btrue]", tracer1.log.toString());

		// int value
		int intDefault = 10;
		int intValue = 11;
		tracer1.log.setLength(0);
		ps.setDefault(key, intDefault);
		assertEquals("", tracer1.log.toString());

		ps.setValue(key, intValue);
		assertEquals("[foo:I10->I11]", tracer1.log.toString());

		ps.setValue(key, intDefault);
		assertEquals("[foo:I10->I11][foo:I11->I10]", tracer1.log.toString());

		// double value
		double doubleDefault = 10.0;
		double doubleValue = 11.0;
		tracer1.log.setLength(0);
		ps.setDefault(key, doubleDefault);
		assertEquals("", tracer1.log.toString());

		tracer1.log.setLength(0);
		ps.setValue(key, doubleValue);
		assertEquals("[foo:D10.0->D11.0]", tracer1.log.toString());

		ps.setValue(key, doubleDefault);
		assertEquals("[foo:D10.0->D11.0][foo:D11.0->D10.0]", tracer1.log.toString());

		// float value
		float floatDefault = 10.0f;
		float floatValue = 11.0f;
		tracer1.log.setLength(0);
		ps.setDefault(key, floatDefault);
		assertEquals("", tracer1.log.toString());

		tracer1.log.setLength(0);
		ps.setValue(key, floatValue);
		assertEquals("[foo:F10.0->F11.0]", tracer1.log.toString());

		ps.setValue(key, floatDefault);
		assertEquals("[foo:F10.0->F11.0][foo:F11.0->F10.0]", tracer1.log.toString());

		// long value
		long longDefault = 10L;
		long longValue = 11L;
		tracer1.log.setLength(0);
		ps.setDefault(key, longDefault);
		assertEquals("", tracer1.log.toString());

		tracer1.log.setLength(0);
		ps.setValue(key, longValue);
		assertEquals("[foo:L10->L11]", tracer1.log.toString());

		ps.setValue(key, longDefault);
		assertEquals("[foo:L10->L11][foo:L11->L10]", tracer1.log.toString());
	}

	@Deprecated
	@Test
	public void testListeners() {

		final Preferences ps = new PreferenceForwarder(getUniqueString());

		final Tracer tracer1 = new Tracer();
		final Tracer tracer2 = new Tracer();

		// register one listener
		ps.addPropertyChangeListener(tracer1);
		assertEquals("", tracer1.log.toString());

		// make sure it is notified in a type appropriate manner
		ps.setValue("a", "1");
		assertEquals("[a:S->S1]", tracer1.log.toString());

		ps.setToDefault("a");
		tracer1.log.setLength(0);
		ps.setValue("a", true);
		assertEquals("[a:Bfalse->Btrue]", tracer1.log.toString());

		ps.setToDefault("a");
		tracer1.log.setLength(0);
		ps.setValue("a", 100);
		assertEquals("[a:I0->I100]", tracer1.log.toString());

		ps.setToDefault("a");
		tracer1.log.setLength(0);
		ps.setValue("a", 100L);
		assertEquals("[a:L0->L100]", tracer1.log.toString());

		ps.setToDefault("a");
		tracer1.log.setLength(0);
		ps.setValue("a", 2.0f);
		assertEquals("[a:F0.0->F2.0]", tracer1.log.toString());

		ps.setToDefault("a");
		tracer1.log.setLength(0);
		ps.setValue("a", 2.0);
		assertEquals("[a:D0.0->D2.0]", tracer1.log.toString());

		// make sure it is notified of a series of events
		ps.setToDefault("a");
		tracer1.log.setLength(0);
		ps.setValue("a", "1");
		assertEquals("[a:S->S1]", tracer1.log.toString());

		ps.setValue("a", "2");
		assertEquals("[a:S->S1][a:S1->S2]", tracer1.log.toString());

		ps.setValue("a", ps.getDefaultString("a"));
		assertEquals("[a:S->S1][a:S1->S2][a:S2->S]", tracer1.log.toString());

		ps.setValue("a", "3");
		assertEquals("[a:S->S1][a:S1->S2][a:S2->S][a:S->S3]", tracer1.log.toString());

		ps.setToDefault("a");
		assertEquals("[a:S->S1][a:S1->S2][a:S2->S][a:S->S3][a:S3->S]", tracer1.log.toString());

		// change to same value - no one notified
		ps.setValue("a", "2");
		tracer1.log.setLength(0);
		assertEquals("", tracer1.log.toString());

		// register second listener
		ps.addPropertyChangeListener(tracer2);

		// make sure both are notified
		ps.setValue("a", "3");
		assertEquals("[a:S2->S3]", tracer1.log.toString());
		assertEquals("[a:S2->S3]", tracer2.log.toString());

		// deregister is honored
		ps.removePropertyChangeListener(tracer2);
		tracer1.log.setLength(0);
		tracer2.log.setLength(0);
		ps.setValue("a", "1");
		assertEquals("[a:S3->S1]", tracer1.log.toString());
		assertEquals("", tracer2.log.toString());

		// duplicate deregister is ignored
		ps.removePropertyChangeListener(tracer2);
		tracer1.log.setLength(0);
		tracer2.log.setLength(0);
		ps.setValue("a", "2");
		assertEquals("[a:S1->S2]", tracer1.log.toString());
		assertEquals("", tracer2.log.toString());

		// duplicate register is ignored
		ps.addPropertyChangeListener(tracer1);
		tracer1.log.setLength(0);
		ps.setValue("a", "1");
		assertEquals("[a:S2->S1]", tracer1.log.toString());

		// last deregister is honored
		ps.removePropertyChangeListener(tracer1);
		tracer1.log.setLength(0);
		ps.setValue("a", "4");
		assertEquals("", tracer1.log.toString());

		// adds 2 and removes 1 during during callback!
		class Trouble implements Preferences.IPropertyChangeListener {
			@Deprecated
			@Override
			public void propertyChange(Preferences.PropertyChangeEvent event) {
				ps.removePropertyChangeListener(tracer1);
				ps.addPropertyChangeListener(tracer2);
			}
		}

		ps.setValue("a", "0");
		ps.addPropertyChangeListener(tracer1);
		ps.addPropertyChangeListener(new Trouble());
		tracer1.log.setLength(0);
		tracer2.log.setLength(0);
		ps.setValue("a", "1");
		ps.setValue("a", "2");
		assertEquals("[a:S0->S1]", tracer1.log.toString());
		assertEquals("[a:S1->S2]", tracer2.log.toString());

	}

	@Deprecated
	@Test
	public void testLoadStore() throws IOException {

		final Preferences ps = new PreferenceForwarder(getUniqueString());

		ps.setValue("b1", true);
		ps.setValue("i1", 1);
		ps.setValue("l1", 2L);
		ps.setValue("f1", 1.0f);
		ps.setValue("d1", 1.0);
		ps.setValue("s1", "x");
		String[] keys = {"b1", "i1", "l1", "f1", "d1", "s1",};

		byte[] bytes;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ps.store(out, "test header");
			bytes = out.toByteArray();
		}

		final Preferences ps2 = new PreferenceForwarder(getUniqueString());
		try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
			ps2.load(in);
		}

		assertEquals(true, ps2.getBoolean("b1"));
		assertEquals(1, ps2.getInt("i1"));
		assertEquals(2L, ps2.getLong("l1"));
		assertEquals(1.0f, ps2.getFloat("f1"), 1e-20f);
		assertEquals(1.0, ps2.getDouble("d1"), 1e-20);
		assertEquals("x", ps2.getString("s1"));

		Set<String> s1 = new HashSet<>(Arrays.asList(keys));
		Set<String> s2 = new HashSet<>(Arrays.asList(ps2.propertyNames()));
		assertEquals(s1, s2);

		// load discards current values
		final Preferences ps3 = new PreferenceForwarder(getUniqueString());
		ps3.setValue("s1", "y");
		try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
			ps3.load(in);
		}
		assertEquals("x", ps3.getString("s1"));
		Set<String> k1 = new HashSet<>(Arrays.asList(keys));
		Set<String> k2 = new HashSet<>(Arrays.asList(ps3.propertyNames()));
		assertEquals(k1, k2);
	}

	@Deprecated
	@Test
	public void testNeedsSaving() throws IOException {

		Preferences ps = new PreferenceForwarder(getUniqueString());

		// setValue dirties
		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setValue("b1", true);
		assertEquals(true, ps.needsSaving());

		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setValue("i1", 1);
		assertEquals(true, ps.needsSaving());

		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setValue("l1", 2L);
		assertEquals(true, ps.needsSaving());

		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setValue("f1", 1.0f);
		assertEquals(true, ps.needsSaving());

		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setValue("d1", 1.0);
		assertEquals(true, ps.needsSaving());

		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setValue("s1", "x");
		assertEquals(true, ps.needsSaving());

		// setToDefault does not dirty if value not set
		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setToDefault("any");
		assertEquals(false, ps.needsSaving());

		// setToDefault dirties if value was set
		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setValue("any", "x");
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ps.store(out, "test header");
			try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
				ps.load(in);
			}
		}
		assertEquals(false, ps.needsSaving());
		ps.setToDefault("any");
		assertEquals(true, ps.needsSaving());

		// setDefault, getT, getDefaultT do not dirty
		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setDefault("b1", true);
		ps.getBoolean("b1");
		ps.getDefaultBoolean("b1");
		ps.setDefault("i1", 1);
		ps.getInt("i1");
		ps.getDefaultInt("i1");
		ps.setDefault("l1", 1L);
		ps.getLong("l1");
		ps.getDefaultLong("l1");
		ps.setDefault("f1", 1.0f);
		ps.getFloat("f1");
		ps.getDefaultFloat("f1");
		ps.setDefault("d1", 1.0);
		ps.getDouble("d1");
		ps.getDefaultDouble("d1");
		ps.setDefault("s1", "x");
		ps.getString("s1");
		ps.getDefaultString("s1");
		assertEquals(false, ps.needsSaving());

		ps = new PreferenceForwarder(getUniqueString());
		assertEquals(false, ps.needsSaving());
		ps.setValue("b1", true);
		assertEquals(true, ps.needsSaving());
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			// store makes not dirty
			ps.store(out, "test header");
			assertEquals(false, ps.needsSaving());

			// load comes in not dirty
			ps.setValue("b1", false);
			assertEquals(true, ps.needsSaving());
			try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
				ps.load(in);
			}
		}
		assertEquals(false, ps.needsSaving());
	}
	/*
	 * Regression test for bug 178815.
	 */
	@Deprecated
	@Test
	public void testListenerOnRemove() throws BackingStoreException {
		AtomicReference<IStatus> logStatus = new AtomicReference<>();
		// create a new log listener that will fail if anything is written
		ILogListener logListener = (status, plugin) -> logStatus.set(status);

		// set a preference value to get everything initialized
		String id = getUniqueString();
		Preferences ps = new PreferenceForwarder(id);
		ps.setValue("key", "value");

		// add a property change listener which will cause one to be
		// added at the preference node level
		IPropertyChangeListener listener = event -> {
		};
		ps.addPropertyChangeListener(listener);
		ps.setValue("key2", "value2");
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(id);

		// add our log listener and remove the node. nothing should be logged.
		RuntimeLog.addLogListener(logListener);
		try {
			node.removeNode();
			assertNull(logStatus.get(), "Log listener should have been called, but logging was called for status");
		} finally {
			RuntimeLog.removeLogListener(logListener);
		}
	}

	private static String getUniqueString() {
		return UUID.randomUUID().toString();
	}
}
