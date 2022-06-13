/*******************************************************************************
 * Copyright (c) 2015, 2016 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.contexts.inject;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Test;

/**
 * Tests for the type erasure in lists
 */
public class GenericsListTest {

	static public class TestNamedObject {
		public List<String> field;

		@Inject
		public void setList(List<String> value) {
			field = value;
		}

		public String combineIt() {
			StringBuilder stringBuilder = new StringBuilder();
			for (String string : field) {
				stringBuilder.append(string);
			}
			return stringBuilder.toString();
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testRawListInjection() {

		List list = new ArrayList();

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(List.class, list);

		TestNamedObject userObject = new TestNamedObject();
		ContextInjectionFactory.inject(userObject, context);

		// check field injection
		assertEquals(list, userObject.field);
	}

	@Test(expected = ClassCastException.class)
	public void testTypeErasure() {

		List<Integer> list = new ArrayList<>();
		list.add(1);

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(List.class, list);

		TestNamedObject userObject = new TestNamedObject();
		ContextInjectionFactory.inject(userObject, context);
		// check field injection, should be successful because
		// of the type erasure
		assertEquals(list, userObject.field);

		userObject.combineIt();
	}


}
