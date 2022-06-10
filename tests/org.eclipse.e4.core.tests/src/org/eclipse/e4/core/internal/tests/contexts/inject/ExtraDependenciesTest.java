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
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.contexts.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Test;

public class ExtraDependenciesTest {

	static public class TestObject {

		public String string;
		public Integer integer;
		public String other;

		public boolean disposed = false;

		@Inject
		public void injectedMethod(@Named("arg1") String strValue, @Named("arg2") Integer intValue, IEclipseContext context) {
			string = strValue;
			integer = intValue;
			if (context == null) {
				other = null;
				return;
			}
			IEclipseContext otherContext = (IEclipseContext) context.get("otherContext");
			if (otherContext == null) {
				other = null;
			} else {
				other = (String) otherContext.get("arg3");
			}
		}

		@PreDestroy
		public void finita() {
			disposed = true;
		}
	}

	@Test
	public void testExtraDependencies() throws InvocationTargetException, InstantiationException {
		IEclipseContext context = EclipseContextFactory.create();
		context.set("arg1", "abc");
		context.set("arg2", Integer.valueOf (123));

		IEclipseContext otherContext = EclipseContextFactory.create();
		otherContext.set("arg3", "other");

		context.set("otherContext", otherContext);

		TestObject object = ContextInjectionFactory.make(TestObject.class, context);

		// check that initial values are properly injected
		assertEquals("abc", object.string);
		assertEquals(Integer.valueOf(123), object.integer);
		assertEquals("other", object.other);

		// modify argument value to cause update - bug 308650
		context.set("arg2", Integer.valueOf (789));

		// change the "other" value; should not be propagated
		otherContext.set("arg3", "wrong");
		assertEquals("other", object.other);

		// dispose the other context; should not cause disposal of the test object
		otherContext.dispose();
		assertEquals("other", object.other);
		assertFalse(object.disposed);

		// remove "other" context, should not be propagated
		context.remove("otherContext");
		assertEquals("other", object.other);

		// check that changes in the method arguments are propagated
		context.set("arg1", "xyz");
		context.set("arg2", Integer.valueOf (456));
		assertEquals("xyz", object.string);
		assertEquals(Integer.valueOf(456), object.integer);
		assertNull(object.other);

		// check that disposal of the injected context causes disposal of the injected object
		context.dispose();
		assertTrue(object.disposed);
		assertNotNull(object.string);
		assertNotNull(object.integer);
		assertNull(object.other);
	}

}
