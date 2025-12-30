/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.Preferences;
import org.junit.jupiter.api.Test;

/**
 * Test suite for API class org.eclipse.core.runtime.Preferences
 * @deprecated This class tests intentionally tests deprecated functionality, so tag
 * added to hide deprecation reference warnings.
 */
@Deprecated
public class PreferencesTest {

	@Deprecated
	static class Tracer implements Preferences.IPropertyChangeListener {
		@Deprecated
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
			fail();
			return null;
		}

		@Deprecated
		@Override
		public void propertyChange(Preferences.PropertyChangeEvent event) {
			log.append("[");
			log.append(event.getProperty());
			log.append(":");
			log.append(typeCode(event.getOldValue()));
			log.append(event.getOldValue() == null ? "null" : event.getOldValue());
			log.append("->");
			log.append(typeCode(event.getNewValue()));
			log.append(event.getNewValue() == null ? "null" : event.getNewValue());
			log.append("]");
		}
	}

	@Deprecated
	@Test
	public void testConstants() {
		// make sure that the preference store constants are defined properly
		assertEquals(Preferences.BOOLEAN_DEFAULT_DEFAULT, false);
		assertEquals(Preferences.INT_DEFAULT_DEFAULT, 0);
		assertEquals(Preferences.LONG_DEFAULT_DEFAULT, 0L);
		assertEquals(Preferences.FLOAT_DEFAULT_DEFAULT, 0.0f, 0.0f);
		assertEquals(Preferences.DOUBLE_DEFAULT_DEFAULT, 0.0, 0.0f);
		assertTrue(Preferences.STRING_DEFAULT_DEFAULT.isEmpty());
	}

	@Deprecated
	@Test
	public void testBasics() {

		Preferences ps = new Preferences();
		final String k1 = "key1";
		final String v1 = "1";
		final String v2 = "2";
		final String v3 = "3";

		// check that a random property in a newly created store
		// appearchs to have default-default values of whatever type asked for
		assertTrue(ps.isDefault(k1));
		assertEquals(ps.getBoolean(k1), Preferences.BOOLEAN_DEFAULT_DEFAULT);
		assertEquals(ps.getInt(k1), Preferences.INT_DEFAULT_DEFAULT);
		assertEquals(ps.getLong(k1), Preferences.LONG_DEFAULT_DEFAULT);
		assertEquals(ps.getFloat(k1), Preferences.FLOAT_DEFAULT_DEFAULT, 0.0f);
		assertEquals(ps.getDouble(k1), Preferences.DOUBLE_DEFAULT_DEFAULT, 0.0f);
		assertTrue(ps.getString(k1).equals(Preferences.STRING_DEFAULT_DEFAULT));

		assertEquals(ps.getDefaultBoolean(k1), Preferences.BOOLEAN_DEFAULT_DEFAULT);
		assertEquals(ps.getDefaultInt(k1), Preferences.INT_DEFAULT_DEFAULT);
		assertEquals(ps.getDefaultLong(k1), Preferences.LONG_DEFAULT_DEFAULT);
		assertEquals(ps.getDefaultFloat(k1), Preferences.FLOAT_DEFAULT_DEFAULT, 0.0f);
		assertEquals(ps.getDefaultDouble(k1), Preferences.DOUBLE_DEFAULT_DEFAULT, 0.0f);
		assertTrue(ps.getDefaultString(k1).equals(Preferences.STRING_DEFAULT_DEFAULT));

		// test set/getString
		// give it a value
		ps.setValue(k1, v1);
		assertFalse(ps.isDefault(k1));
		assertTrue(ps.getString(k1).equals(v1));
		assertTrue(ps.getDefaultString(k1).equals(Preferences.STRING_DEFAULT_DEFAULT));
		// change the value
		ps.setValue(k1, v2);
		assertFalse(ps.isDefault(k1));
		assertTrue(ps.getString(k1).equals(v2));
		assertTrue(ps.getDefaultString(k1).equals(Preferences.STRING_DEFAULT_DEFAULT));
		// change to same value as default
		ps.setValue(k1, ps.getDefaultString(k1));
		assertTrue(ps.isDefault(k1));
		assertTrue(ps.getString(k1).equals(ps.getDefaultString(k1)));
		assertTrue(ps.getDefaultString(k1).equals(Preferences.STRING_DEFAULT_DEFAULT));
		// reset to default
		ps.setValue(k1, v2);
		ps.setToDefault(k1);
		assertTrue(ps.isDefault(k1));
		assertTrue(ps.getString(k1).equals(Preferences.STRING_DEFAULT_DEFAULT));
		assertTrue(ps.getDefaultString(k1).equals(Preferences.STRING_DEFAULT_DEFAULT));
		// change default
		ps.setDefault(k1, v1);
		assertTrue(ps.isDefault(k1));
		assertTrue(ps.getString(k1).equals(v1));
		assertTrue(ps.getDefaultString(k1).equals(v1));
		// set the value
		ps.setValue(k1, v2);
		assertFalse(ps.isDefault(k1));
		assertTrue(ps.getString(k1).equals(v2));
		assertTrue(ps.getDefaultString(k1).equals(v1));
		// change to same value as default
		ps.setValue(k1, ps.getDefaultString(k1));
		assertTrue(ps.isDefault(k1));
		assertTrue(ps.getString(k1).equals(ps.getDefaultString(k1)));
		assertTrue(ps.getDefaultString(k1).equals(v1));
		// reset to default
		ps.setValue(k1, v2);
		ps.setToDefault(k1);
		assertTrue(ps.isDefault(k1));
		assertTrue(ps.getString(k1).equals(v1));
		assertTrue(ps.getDefaultString(k1).equals(v1));
		// change default
		ps.setDefault(k1, v3);
		assertTrue(ps.isDefault(k1));
		assertTrue(ps.getString(k1).equals(v3));
		assertTrue(ps.getDefaultString(k1).equals(v3));

	}

	@Deprecated
	@Test
	public void testBoolean() {

		Preferences ps = new Preferences();
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

		Preferences ps = new Preferences();
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

		Preferences ps = new Preferences();
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

		Preferences ps = new Preferences();
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

		Preferences ps = new Preferences();
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

		Preferences ps = new Preferences();
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

		Preferences ps = new Preferences();

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

		Preferences ps = new Preferences();

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

		// bug 51309 - if a default-default value is stored
		// as a default it is still a part of #contains
		assertFalse(ps.contains("c"));
		ps.setDefault("c", Preferences.STRING_DEFAULT_DEFAULT);
		assertTrue(ps.contains("c"));
	}

	@Deprecated
	@Test
	public void testDefaultPropertyNames() {

		Preferences ps = new Preferences();

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
	public void testListeners2() {

		final Preferences ps = new Preferences();
		final Tracer tracer = new Tracer();
		String key = "a";

		ps.addPropertyChangeListener(tracer);

		// go from a default value to a real value
		ps.setDefault(key, 1);
		ps.setValue(key, 2);
		assertEquals("[a:I1->I2]", tracer.log.toString());

		// real value to another real value
		tracer.log.setLength(0);
		ps.setValue(key, 3);
		assertEquals("[a:I2->I3]", tracer.log.toString());

		// back to the default
		tracer.log.setLength(0);
		ps.setToDefault(key);
		// TODO strings are reported because we don't know the type
		assertEquals("[a:S3->S1]", tracer.log.toString());

		// remove the default and then add a real value
		tracer.log.setLength(0);
		ps.setDefault(key, 0);
		ps.setValue(key, 2);
		assertEquals("[a:I0->I2]", tracer.log.toString());

		// then remove the value
		tracer.log.setLength(0);
		ps.setValue(key, ps.getDefaultInt(key));
		assertEquals("[a:I2->I0]", tracer.log.toString());
	}

	@Deprecated
	@Test
	public void testListeners() {

		final Preferences ps = new Preferences();

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
		assertEquals("[a:S->S1][a:S1->S2][a:S2->S][a:S->S3][a:S3->null]", tracer1.log.toString());

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
			@SuppressWarnings("deprecation")
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

		final Preferences ps = new Preferences();

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

		final Preferences ps2 = new Preferences();
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
		final Preferences ps3 = new Preferences();
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

		Preferences ps = new Preferences();

		// setValue dirties
		ps = new Preferences();
		assertEquals(false, ps.needsSaving());
		ps.setValue("b1", true);
		assertEquals(true, ps.needsSaving());

		ps = new Preferences();
		assertEquals(false, ps.needsSaving());
		ps.setValue("i1", 1);
		assertEquals(true, ps.needsSaving());

		ps = new Preferences();
		assertEquals(false, ps.needsSaving());
		ps.setValue("l1", 2L);
		assertEquals(true, ps.needsSaving());

		ps = new Preferences();
		assertEquals(false, ps.needsSaving());
		ps.setValue("f1", 1.0f);
		assertEquals(true, ps.needsSaving());

		ps = new Preferences();
		assertEquals(false, ps.needsSaving());
		ps.setValue("d1", 1.0);
		assertEquals(true, ps.needsSaving());

		ps = new Preferences();
		assertEquals(false, ps.needsSaving());
		ps.setValue("s1", "x");
		assertEquals(true, ps.needsSaving());

		// setToDefault does not dirty if value not set
		ps = new Preferences();
		assertEquals(false, ps.needsSaving());
		ps.setToDefault("any");
		assertEquals(false, ps.needsSaving());

		// setToDefault dirties if value was set
		ps = new Preferences();
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
		ps = new Preferences();
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

		ps = new Preferences();
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
			assertEquals(false, ps.needsSaving());
		}
	}

}
