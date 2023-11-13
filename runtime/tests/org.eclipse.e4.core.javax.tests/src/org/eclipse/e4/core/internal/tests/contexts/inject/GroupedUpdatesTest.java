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
package org.eclipse.e4.core.internal.tests.contexts.inject;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.GroupUpdates;
import org.junit.Test;

public class GroupedUpdatesTest {


	static class InjectTarget {
		public int countMain = 0;
		public int countSecondary = 0;

		public String s1;
		public String s2;

		public void resetCounters() {
			countMain = 0;
			countSecondary = 0;
		}
	}


	static class InjectTargetImmediate extends InjectTarget {
		@Inject
		void setInfo(@Named("string1") String s, @Named("string2") String s2) {
			countMain++;
			this.s1 = s;
			this.s2 = s2;
		}
		@Inject
		void setInfoSecondary(@Named("string2") String s2) {
			countSecondary++;
			this.s2 = s2;
		}
	}

	static class InjectTargetWait extends InjectTarget {
		@Inject @GroupUpdates
		void setInfo(@Named("string1") String s, @Named("string2") String s2) {
			countMain++;
			this.s1 = s;
			this.s2 = s2;
		}

		@Inject @GroupUpdates
		void setInfoSecondary(@Named("string2") String s2) {
			countSecondary++;
			this.s2 = s2;
		}
	}

	@Test
	public void testNoGrouping() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set("string1", "x");
		context.set("string2", "y");

		InjectTarget target = new InjectTargetImmediate();
		ContextInjectionFactory.inject(target, context);
		assertEquals(1, target.countMain);

		target.resetCounters();

		// I want these two sets to be one transaction
		context.set("string1", "a");
		context.set("string2", "b");
		assertEquals(2, target.countMain);
		assertEquals(1, target.countSecondary);
		context.processWaiting();
		assertEquals(2, target.countMain);
		assertEquals(1, target.countSecondary);
	}

	@Test
	public void testGrouping() {
		final IEclipseContext context = EclipseContextFactory.create();
		context.set("string1", "x");
		context.set("string2", "y");

		InjectTarget target = new InjectTargetWait();
		ContextInjectionFactory.inject(target, context);
		assertEquals(1, target.countMain);

		target.resetCounters();

		context.set("string1", "a");
		context.set("string2", "b");

		assertEquals(0, target.countMain);
		assertEquals(0, target.countSecondary);

		context.processWaiting();

		assertEquals(1, target.countMain);
		assertEquals(1, target.countSecondary);

		// do it again to make sure we properly cleared waiting updates
		context.processWaiting();
		assertEquals(1, target.countMain);
		assertEquals(1, target.countSecondary);

		// now with 3 updates
		target.resetCounters();
		context.set("string1", "x");
		context.set("string2", "y");
		context.set("string2", "z");
		context.set("string1", "delta");

		assertEquals(0, target.countMain);
		assertEquals(0, target.countSecondary);

		context.processWaiting();

		assertEquals(1, target.countMain);
		assertEquals(1, target.countSecondary);

		assertEquals(target.s1, "delta");
		assertEquals(target.s2, "z");
	}
}
