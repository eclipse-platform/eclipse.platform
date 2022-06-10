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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.Test;

public class ParentContextDisposalTest {
	static class Target {
		int pc = 0;
		int pd = 0;

		@Inject
		@Named("o")
		Object o;

		@PostConstruct
		void pc() {
			pc++;
		}

		@PreDestroy
		void pd() {
			pd++;
		}
	}

	@Test
	public void testParentContextDisposal() {
		IEclipseContext context = EclipseContextFactory.create();
		IEclipseContext child = context.createChild();
		child.set("o", new Object());

		Target target = ContextInjectionFactory.make(Target.class, child);
		assertEquals(1, target.pc);

		context.dispose();
		assertEquals(1, target.pd);
	}
}
