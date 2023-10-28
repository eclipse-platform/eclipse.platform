/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.tests.di;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.junit.Test;

/**
 * Test that we don't hold on to the values calculated during injection.
 * This test relies on VM performing garbage collection in System.gc().
 * The actual VM processing of GC requests is up to the implementation
 * so this test might not work on all VMs or might become invalid on
 * future VMs.
 */
public class InjectionResultLeakTest {

	static class PartConsumer {
		Object part;

		@Inject
		void setPart(@Optional @Named("testGC") Object part) {
			this.part = part;
		}
	}

	@Test
	public void testLeaks() {
		IEclipseContext context = EclipseContextFactory.create();
		WeakReference<?> ref;

		Object part = new Object();
		ref = new WeakReference<>(part);
		assertEquals(part, ref.get());

		context.set("testGC", part);

		PartConsumer consumer = ContextInjectionFactory.make(
				PartConsumer.class, context);
		assertEquals(part, consumer.part);

		part = null; // another "let's help GC" statement
		context.remove("testGC");

		assertNull(consumer.part);

		// gc a few times
		System.runFinalization();
		System.gc();
		System.runFinalization();
		System.gc();
		System.runFinalization();
		System.gc();
		System.runFinalization();
		System.gc();

		// partA should have been gc'd
		assertNull("The object should have been garbage collected", ref.get());
	}

}
