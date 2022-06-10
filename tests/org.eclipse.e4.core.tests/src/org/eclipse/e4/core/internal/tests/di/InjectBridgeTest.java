/*******************************************************************************
 * Copyright (c) 2018 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.core.internal.tests.di;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.junit.Test;

public class InjectBridgeTest {
	static class Base<T> {
		public void testInject(T value) {

		}

		public void testInvoke(T value) {

		}
	}

	static class Concrete extends Base<String> {
		@Inject
		@Override
		public void testInject(String value) {
		}

		@Override
		@Execute
		public void testInvoke(String value) {

		}
	}

	@Test
	public void testInjection() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set(String.class, "Value");

		ContextInjectionFactory.make(Concrete.class, context);
	}

	@Test(expected = InjectionException.class)
	public void testInjectionFail() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Object.class, "Value");

		ContextInjectionFactory.make(Concrete.class, context);
	}

	@Test
	public void testInvokation() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set(String.class, "Value");

		ContextInjectionFactory.invoke(new Concrete(), Execute.class, context);
	}

	@Test(expected = InjectionException.class)
	public void testInvokationFail() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Object.class, "Value");
		ContextInjectionFactory.invoke(new Concrete(), Execute.class, context);
	}
}
