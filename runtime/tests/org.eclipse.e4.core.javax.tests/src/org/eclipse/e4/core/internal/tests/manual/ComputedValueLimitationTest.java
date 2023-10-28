/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.manual;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Test;


/**
 * This is a demo of a scenario when computed values don't work. The basic idea here is that a
 * calculated value depends on something not stored in the context, it won't necessarily be updated.
 *
 * CalculatedValue = Function(ContextElement1, ..., ContextElement1N, ExtnernalFactor)
 *
 * In this scenario we deal with the Output = Function(arg1, ..., arg10, Time)
 *
 * We use a system timer here as an external input, but it can be pretty much anything not stored
 * directly in the context.
 */
public class ComputedValueLimitationTest {


	/**
	 * Used as an injection target
	 */
	public static class UserObject {

		private String txt;

		@Inject
		public void Computed(String txt) {
			this.txt = txt;
		}

		public String getComputed() {
			return txt;
		}
	}

	static public class ExtenralFactor {
		static public int useChild() {
			long time = System.currentTimeMillis();
			return ((int) time % 10); // this is incorrect but works for the example
		}
	}

	public static class CalcColor extends ContextFunction {

		@Override
		public Object compute(IEclipseContext context, String contextKey) {
			int useArg = ExtenralFactor.useChild();
			return context.get("arg" + useArg);
		}
	}

	public static class Time extends ContextFunction {
		@Override
		public Object compute(IEclipseContext context, String contextKey) {
			context.get(String.valueOf(System.currentTimeMillis()));
			return Long.valueOf(System.currentTimeMillis());
		}
	}

	@Test
	public synchronized void testInjection() {

		IEclipseContext context = EclipseContextFactory.create();
		for (int i = 0; i < 10; i++) {
			context.set("arg" + i, Integer.toString(i));
		}
		context.set("computed", new CalcColor());

		UserObject userObject = new UserObject();
		ContextInjectionFactory.inject(userObject, context);

		for (int i = 0; i < 20; i++) {
			int before = ExtenralFactor.useChild();
			String actual = userObject.getComputed();
			int after = ExtenralFactor.useChild();
			System.out.println("[" + before + "] actual: " + actual + " [" + after + "]");
			try {
				wait(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testVolatileFunction() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set("time", new Time());
		long time = ((Long) context.get("time")).longValue();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			//
		}
		long newTime = ((Long) context.get("time")).longValue();
		assertTrue(time != newTime);
	}

}
