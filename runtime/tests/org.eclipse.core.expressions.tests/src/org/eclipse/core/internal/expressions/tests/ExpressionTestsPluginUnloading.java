/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.core.internal.expressions.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import org.eclipse.core.internal.expressions.Expressions;


/**
 * Tests for cache used in {@link Expressions#isInstanceOf(Object, String)}.
 * <p>
 * <b>WARNING:</b> These tests start, stop, and re-start the <code>com.ibm.icu</code> bundle.
 * Don't include these in another test suite!
 */
@SuppressWarnings("restriction")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ExpressionTestsPluginUnloading {

	private String name;

	@BeforeEach
	public void setupTestName(TestInfo testInfo) {
		name = testInfo.getDisplayName();
	}

	@Test
	public void test01PluginStopping() throws Exception {
		Bundle bundle= getBundle("com.ibm.icu");
		bundle.start();
		int state = bundle.getState();
		assertThat(state).withFailMessage("Unexpected bundle state: " + stateToString(state) + " for bundle " + bundle)
				.isEqualTo(Bundle.ACTIVE);

		doTestInstanceofICUDecimalFormat(bundle);
		assertThat(bundle.getState()).as("Instanceof with bundle-local class should load extra bundle")
				.isEqualTo(state);

		bundle.stop();
		assertThat(bundle.getState())
				.withFailMessage("Unexpected bundle state: " + stateToString(state) + " for bundle " + bundle)
				.isEqualTo(Bundle.RESOLVED);

		bundle.start();
		assertThat(bundle.getState())
				.withFailMessage("Unexpected bundle state: " + stateToString(state) + " for bundle " + bundle)
				.isEqualTo(Bundle.ACTIVE);

		doTestInstanceofICUDecimalFormat(bundle);
	}

	@Test
	public void test02MultipleClassloaders() throws Exception {
		Bundle expr= getBundle("org.eclipse.core.expressions.tests");
		Bundle icu= getBundle("com.ibm.icu");

		Class<?> exprClass = expr.loadClass("com.ibm.icu.text.DecimalFormat");
		Class<?> icuClass = icu.loadClass("com.ibm.icu.text.DecimalFormat");
		assertThat(exprClass).isNotSameAs(icuClass);

		Object exprObj = exprClass.getDeclaredConstructor().newInstance();
		Object icuObj = icuClass.getDeclaredConstructor().newInstance();

		assertInstanceOf(exprObj, "java.lang.Runnable", "java.lang.String");
		assertInstanceOf(exprObj, "java.lang.Object", "java.io.Serializable");

		assertInstanceOf(icuObj, "java.io.Serializable", "java.lang.String");
		assertInstanceOf(icuObj, "java.text.Format", "java.lang.Runnable");
	}

	static String stateToString(int state) {
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		}
		throw new IllegalStateException("Unknown state: " + state);
	}

	private void assertInstanceOf(Object obj, String isInstance, String isNotInstance) throws Exception {
		Class<?> clazz = obj.getClass();

		System.out.println(
				"ExpressionTestsPluginUnloading#" + name + "() - " + clazz.getName() + ": "
						+ clazz.hashCode());
		System.out.println(
				"ExpressionTestsPluginUnloading#" + name + "() - ClassLoader: "
						+ clazz.getClassLoader().hashCode());

		for (int i= 0; i < 2; i++) { // test twice, second time is cached:
			assertTrue(Expressions.isInstanceOf(obj, isInstance));
			assertFalse(Expressions.isInstanceOf(obj, isNotInstance));
		}
	}

	private void doTestInstanceofICUDecimalFormat(Bundle bundle) throws Exception {
		Class<?> clazz = bundle.loadClass("com.ibm.icu.text.DecimalFormat");
		Object decimalFormat = clazz.getDeclaredConstructor().newInstance();
		assertInstanceOf(decimalFormat, "com.ibm.icu.text.DecimalFormat", "java.text.NumberFormat");
	}

	private static Bundle getBundle(String bundleName) {
		BundleContext bundleContext = FrameworkUtil.getBundle(ExpressionTestsPluginUnloading.class).getBundleContext();
		Bundle[] bundles= bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			if (bundleName.equals(bundle.getSymbolicName())) {
				return bundle;
			}
		}
		fail("Could not find bundle: " + bundleName);
		return null;
	}
}
