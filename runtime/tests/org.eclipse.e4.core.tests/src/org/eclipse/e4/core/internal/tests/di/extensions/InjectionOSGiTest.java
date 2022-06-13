/*******************************************************************************
 * Copyright (c) 2013, 2015 Markus Alexander Kuppe and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 *   Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 ******************************************************************************/
package org.eclipse.e4.core.internal.tests.di.extensions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.internal.tests.CoreTestsActivator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class InjectionOSGiTest {

	// classed used as a user of the @OSGiBundle annotation
	static class InjectionTarget {

		private BundleContext ctx;

		@Inject
		public void setBundleContext(
				@OSGiBundle @Optional BundleContext ctx) {
			this.ctx = ctx;
		}

		public boolean hasContext() {
			return this.ctx != null;
		}

		public BundleContext getContext() {
			return this.ctx;
		}

		private Bundle b;

		@Inject
		public void setBundle(
				@OSGiBundle Bundle b) {
					this.b = b;
		}

		public Bundle getBundle() {
			return this.b;
		}

		@Inject
		public void setFoo(@OSGiBundle Object o) {
			// make sure we don't fail when incompatible type requested
		}
	}

	// classed used as a user of the @OSGiBundle annotation
	static class InjectionBundleTarget extends InjectionTarget {
	}

	private InjectionTarget target;
	private Bundle bundle;

	@After
	public void tearDown() throws Exception {
		bundle.start();

		final BundleContext bundleContext = CoreTestsActivator
				.getDefault().getBundleContext();
		final IEclipseContext localContext = EclipseContextFactory
				.getServiceContext(bundleContext);

		ContextInjectionFactory.uninject(target, localContext);
	}

	@Before
	public void setUp() throws Exception {
		final BundleContext bundleContext = CoreTestsActivator
				.getDefault().getBundleContext();
		bundle = bundleContext.getBundle();

		final IEclipseContext localContext = EclipseContextFactory
				.getServiceContext(bundleContext);

		target = ContextInjectionFactory.make(InjectionTarget.class,
				localContext);
	}

	@Test
	public void testInject() {
		assertTrue(target.hasContext());
	}

	@Test
	public void testUnInject() throws BundleException, InterruptedException {
		// inject
		assertTrue(target.hasContext());

		// Check also that the BundleContext instance has indeed changed
		final BundleContext firstContext = target
				.getContext();

		// uninject
		bundle.stop();
		assertFalse(target.hasContext());

		// re-inject
		bundle.start();
		assertTrue(target.hasContext());

		final BundleContext secondContext = target
				.getContext();
		assertNotSame(firstContext, secondContext);
	}

	@Test
	public void testBundleInject() throws BundleException {
		// inject
		assertNotNull(target.getBundle());

		// Contrary to the BC, the Bundle is available even for RESOLVED bundles
		bundle.stop();

		// not null but resolved _and_ still usable
		assertNotNull(target.getBundle());
		assertTrue(target.getBundle().getState() == Bundle.RESOLVED);
		assertNotNull(target.getBundle().getSymbolicName());

		assertNull(target.getContext());
	}
}
