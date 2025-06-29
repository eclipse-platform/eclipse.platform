/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 ******************************************************************************/

package org.eclipse.e4.core.internal.tests.contexts.performance;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.e4.core.internal.tests.CoreTestsActivator;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class ContextPerformanceTest {
	String displayName;

	IEclipseContext parentContext, context;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		displayName = testInfo.getDisplayName();
		parentContext = EclipseContextFactory.getServiceContext(CoreTestsActivator.getDefault().getBundleContext());
		context = parentContext.createChild(displayName);

		// add some values to the contexts
		for (int i = 0; i < 100; i++) {
			context.set("Value-" + i, Integer.valueOf(i));
		}
		// do some additional service lookups on non-existent keys
		for (int i = 0; i < 1000; i++) {
			context.get("NonExistentValue-" + i);
		}

		// lookup some OSGi services
		context.get(DebugOptions.class.getName());
		context.get(IAdapterManager.class.getName());
		context.get(IExtensionRegistry.class.getName());
		context.get(IPreferencesService.class.getName());
		context.get(Location.class.getName());
	}

	@Test
	public void testLookup() throws Exception {
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				context.get("something");
			}
		}.run(getClass(), displayName, 10, 600000);
	}

	@Test
	public void testLookupContextFunction() throws Exception {
		context.set("somefunction", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return "result";
			}
		});
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				context.get("somefunction");
			}
		}.run(getClass(), displayName, 10, 5000000);
	}

	@Test
	public void testSetContextFunction() throws Exception {
		context.set("somefunction", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return context.get("something");
			}
		});
		new PerformanceTestRunner() {
			int i = 0;

			@Override
			protected void test() {
				context.set("something", "value-" + i++);
			}
		}.run(getClass(), displayName, 10, 600000);
	}

	/**
	 * Tests setting a value in a context that a RAT is listening to. This test mimics what occurs
	 * when handlers change in e4. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=305038
	 */
	@Test
	public void testSetValueRunAndTrack() throws Exception {
		context.set("somefunction", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				// make sure this function has a large number of dependencies
				for (int i = 0; i < 1000; i++) {
					context.get("NonExistentValue-" + i);
				}
				return context.get("something");
			}
		});
		context.runAndTrack(new RunAndTrack() {
			@Override
			public boolean changed(IEclipseContext context) {
				context.get("somefunction");
				return true;
			}
		});
		new PerformanceTestRunner() {
			int i = 0;

			@Override
			protected void test() {
				context.set("something", "value-" + i++);
			}
		}.run(getClass(), displayName, 10, 400);
	}

}
