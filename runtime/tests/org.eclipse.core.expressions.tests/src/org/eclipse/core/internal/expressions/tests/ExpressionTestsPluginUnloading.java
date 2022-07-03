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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExpressionTestsPluginUnloading {

	@Rule
	public TestName name = new TestName();

	@Test
	public void test01PluginStopping() throws Exception {
		Bundle bundle= getBundle("com.ibm.icu");

		int state = bundle.getState();
		if (state != Bundle.STARTING && state != Bundle.ACTIVE) {
			fail("Unexpected bundle state: " + stateToStgring(state) + " for bundle " + bundle);
		}

		doTestInstanceofICUDecimalFormat(bundle);
		assertEquals(Bundle.ACTIVE, bundle.getState());

		bundle.stop();
		assertEquals(Bundle.RESOLVED, bundle.getState());

		bundle.start();
		assertEquals(Bundle.ACTIVE, bundle.getState());

		doTestInstanceofICUDecimalFormat(bundle);
	}

	@Test
	public void test02MultipleClassloaders() throws Exception {
		Bundle expr= getBundle("org.eclipse.core.expressions.tests");
		Bundle icu= getBundle("com.ibm.icu");

		Class<?> exprClass = expr.loadClass("com.ibm.icu.text.DecimalFormat");
		Class<?> icuClass = icu.loadClass("com.ibm.icu.text.DecimalFormat");
		assertNotSame(exprClass, icuClass);

		Object exprObj = exprClass.getDeclaredConstructor().newInstance();
		Object icuObj = icuClass.getDeclaredConstructor().newInstance();

		assertInstanceOf(exprObj, "java.lang.Runnable", "java.lang.String");
		assertInstanceOf(exprObj, "java.lang.Object", "java.io.Serializable");

		assertInstanceOf(icuObj, "java.io.Serializable", "java.lang.String");
		assertInstanceOf(icuObj, "java.text.Format", "java.lang.Runnable");
	}

	static String stateToStgring(int state) {
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
				"ExpressionTestsPluginUnloading#" + name.getMethodName() + "() - " + clazz.getName() + ": "
						+ clazz.hashCode());
		System.out.println(
				"ExpressionTestsPluginUnloading#" + name.getMethodName() + "() - ClassLoader: "
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
