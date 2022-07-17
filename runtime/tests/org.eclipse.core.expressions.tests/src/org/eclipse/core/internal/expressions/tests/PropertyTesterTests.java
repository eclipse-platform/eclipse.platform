/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.osgi.framework.Bundle;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.TestExpression;
import org.eclipse.core.internal.expressions.Property;
import org.eclipse.core.internal.expressions.TypeExtensionManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

@SuppressWarnings("restriction")
public class PropertyTesterTests extends TestCase {

	private A a;
	private B b;
	private I i;

	private static final TypeExtensionManager fgManager= new TypeExtensionManager("propertyTesters"); //$NON-NLS-1$

	// Needs additional local test plug-ins
	private static final boolean TEST_DYNAMIC_AND_ACTIVATION= false;

	public static Test suite() {
		return new TestSuite(PropertyTesterTests.class);
	}

	@Override
	protected void setUp() throws Exception {
		a= new A();
		b= new B();
		i= b;
	}

	public void testSimple() throws Exception {
		assertTrue(test(a, "simple", null,"simple")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(a, "simple", null,"simple")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testInherited() throws Exception {
		assertTrue(test(b, "simple", null, "simple")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(i, "simple", null, "simple")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(b, "simple", null, "simple")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(i, "simple", null, "simple")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testUnknown() throws Exception {
		try {
			test(a, "unknown", null, null); //$NON-NLS-1$
		} catch (CoreException e) {
			return;
		}
		assertTrue(false);
	}

	public void testOverridden() throws Exception {
		assertTrue(test(a, "overridden", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(b, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		A b_as_a= b;
		assertTrue(test(b_as_a, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(i, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(a, "overridden", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(b, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(b_as_a, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(i, "overridden", null, "B")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testOdering() throws Exception {
		assertTrue(test(b, "ordering", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		I other= new I() {};
		assertTrue(test(other, "ordering", null, "I")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(b, "ordering", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(test(other, "ordering", null, "I")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testChaining() throws Exception {
		assertTrue(test(a, "chaining", null, "A2")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(a, "chaining", null, "A2")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// This test is questionable. It depends on if core runtime can
	// guaratee any ordering in the plug-in registry.
	public void testChainOrdering() throws Exception {
		assertTrue(test(a, "chainOrdering", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
		// second pass to check if cache is populated correctly
		assertTrue(test(a, "chainOrdering", null, "A")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testWrongNameSpace() throws Exception {
		try {
			test(a, "differentNamespace", null, null); //$NON-NLS-1$
		} catch (CoreException e) {
			return;
		}
		assertTrue(false);
	}

	public void testDynamicPlugin() throws Exception {
		if (TEST_DYNAMIC_AND_ACTIVATION) {
			A receiver= new A();
			Property p= fgManager.getProperty(receiver, "org.eclipse.core.expressions.tests.dynamic", "testing"); //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue(!p.isInstantiated());
			Bundle bundle= Platform.getBundle("org.eclipse.core.expressions.tests.dynamic"); //$NON-NLS-1$
			bundle.start();
			p= fgManager.getProperty(receiver, "org.eclipse.core.expressions.tests.dynamic", "testing"); //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue(p.isInstantiated());
			bundle.stop();
			bundle.uninstall();
			boolean exception= false;
			try {
				p= fgManager.getProperty(receiver, "org.eclipse.core.expressions.tests.dynamic", "testing"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (CoreException | InvalidRegistryObjectException e) {
				// The uninstall events are sent out in a separate thread.
				// So the type extension registry might not be flushed even
				// though the bundle has already been uninstalled.
				exception= true;
			}
			assertTrue("Core exception not thrown", exception);
		}
	}

	public void testPluginActivation() throws Exception {
		if (TEST_DYNAMIC_AND_ACTIVATION) {
			Bundle bundle= Platform.getBundle("org.eclipse.core.expressions.tests.forceActivation"); //$NON-NLS-1$
			assertEquals(Bundle.STARTING, bundle.getState());

			A receiver= new A();
			TestExpression exp= new TestExpression("org.eclipse.core.expressions.tests.forceActivation", "testing", null, null, true);
			EvaluationContext context= new EvaluationContext(null, receiver);
			EvaluationResult result= exp.evaluate(context);
			assertEquals(EvaluationResult.NOT_LOADED, result);
			assertEquals(Bundle.STARTING, bundle.getState());
			Property p= TestExpression.testGetTypeExtensionManager().getProperty(receiver, "org.eclipse.core.expressions.tests.forceActivation", "testing", false); //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue(!p.isInstantiated());

			context.setAllowPluginActivation(true);
			exp.evaluate(context);
			assertEquals(Bundle.ACTIVE, bundle.getState());
			p= TestExpression.testGetTypeExtensionManager().getProperty(receiver, "org.eclipse.core.expressions.tests.forceActivation", "testing", false); //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue(p.isInstantiated());
		}
	}

	public void testPlatformTester() throws Exception {
		TestExpression exp = new TestExpression("org.eclipse.core.runtime",
				"bundleState",
				new Object[] { "org.eclipse.core.expressions" }, "ACTIVE", false);
		EvaluationContext context= new EvaluationContext(null, Platform.class);
		EvaluationResult result= exp.evaluate(context);
		assertEquals(EvaluationResult.TRUE, result);
	}

	public void testDifferentNameSpace() throws Exception {
		assertTrue(test("org.eclipse.core.internal.expressions.tests2", a, "differentNamespace", null, "A3"));		 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private boolean test(Object receiver, String property, Object[] args, Object expectedValue) throws CoreException {
		Property p= fgManager.getProperty(receiver, "org.eclipse.core.internal.expressions.tests", property); //$NON-NLS-1$
		assertTrue(p.isInstantiated());
		return p.test(receiver, args, expectedValue);
	}

	private boolean test(String namespace, Object receiver, String property, Object[] args, Object expectedValue) throws CoreException {
		Property p= fgManager.getProperty(receiver, namespace, property);
		assertTrue(p.isInstantiated());
		return p.test(receiver, args, expectedValue);
	}
}
