/*******************************************************************************
 * Copyright (c) 2014, 2015 Markus Alexander Kuppe and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 *   Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 ******************************************************************************/
package org.eclipse.e4.core.internal.tests.di.extensions;

import static org.junit.Assert.assertNotNull;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.internal.tests.CoreTestsActivator;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class InjectionOSGiHandlerTest {

	public static class TestHandler {

		private BundleContext ctx;

		@Execute
		public void execute(@OSGiBundle BundleContext ctx) {
			this.ctx = ctx;
		}

		public BundleContext getCtx() {
			return ctx;
		}
	}

	@Test
	public void testInjectBCinExecute() {

		final BundleContext bundleContext = CoreTestsActivator
				.getDefault().getBundleContext();
		final IEclipseContext localContext = EclipseContextFactory
				.getServiceContext(bundleContext);

		TestHandler handler = new TestHandler();
		ContextInjectionFactory.invoke(handler, Execute.class, localContext);

		assertNotNull(handler.getCtx());
	}

}
