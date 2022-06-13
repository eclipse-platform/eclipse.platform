/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.tests.di;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectorFactory;
import org.eclipse.e4.core.internal.contexts.ContextObjectSupplier;
import org.eclipse.e4.core.internal.di.FieldRequestor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RequestorTest {
	public Object field;
	public IEclipseContext context;

	@Test
	public void testHashCode() throws Exception {
		Field field = getClass().getField("field");
		assertNotNull(field);
		FieldRequestor requestor = new FieldRequestor(field,
				InjectorFactory.getDefault(),
				ContextObjectSupplier.getObjectSupplier(context,
						InjectorFactory.getDefault()), null, this, false);
		int hash = requestor.hashCode();
		requestor.getReference().clear();
		assertEquals(hash, requestor.hashCode());
	}

	@Before
	public void setUp() throws Exception {
		context = EclipseContextFactory.create("RequestorTest");
	}

	@After
	public void tearDown() throws Exception {
		context.dispose();
	}


}
