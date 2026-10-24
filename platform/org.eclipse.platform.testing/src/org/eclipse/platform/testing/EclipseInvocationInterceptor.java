/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.platform.testing;

import java.lang.reflect.Method;

import org.eclipse.pde.api.tools.annotations.NoInstantiate;
import org.eclipse.pde.api.tools.annotations.NoReference;
import org.eclipse.ui.testing.TestableObject;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * An {@link InvocationInterceptor} that executes all tests inside the
 * {@link TestableObject#runTest(Runnable)} method.
 */
@NoInstantiate
public class EclipseInvocationInterceptor implements InvocationInterceptor {

	private final ServiceTracker<TestableObject, TestableObject> testableObjectTracker;
	private final TestableObject testableObject;

	/**
	 * Creates the extension, will be called by JUnit
	 */
	public EclipseInvocationInterceptor() {
		Bundle bundle = FrameworkUtil.getBundle(EclipseLauncherSessionListener.class);
		if (bundle == null) {
			throw new IllegalStateException("Not running inside an OSGi Framework");
		}
		BundleContext bundleContext = bundle.getBundleContext();
		if (bundleContext == null) {
			throw new IllegalStateException("Extension Bundle not started");
		}
		testableObjectTracker = new ServiceTracker<>(bundleContext, TestableObject.class, null);
		testableObjectTracker.open();
		testableObject = testableObjectTracker.getService();
		if (testableObject == null) {
			throw new IllegalStateException("Testable Object not found!");
		}
	}

	@Override
	@NoReference
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		Throwable[] throwable = new Throwable[1];
		TestableObject service = testableObjectTracker.getService();
		if (service != null) {
			service.runTest(() -> {
				try {
					invocation.proceed();
				} catch (Throwable e) {
					throwable[0] = e;
				}
			});
			Throwable t = throwable[0];
			if (t == null) {
				return;
			}
			throw t;
		}
	}

}
