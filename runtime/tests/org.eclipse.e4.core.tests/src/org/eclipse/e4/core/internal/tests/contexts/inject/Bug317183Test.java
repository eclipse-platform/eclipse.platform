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

import static org.junit.Assert.assertTrue;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.junit.Test;

public class Bug317183Test {

	@Test
	public void testX() {
		IEclipseContext appContext = EclipseContextFactory.create();
		IEclipseContext windowContext = appContext.createChild();
		IEclipseContext partContextA = windowContext.createChild();
		IEclipseContext partContextB = windowContext.createChild();

		partContextA.activateBranch();

		RunAndTrackImpl impl = new RunAndTrackImpl();
		windowContext.runAndTrack(impl);

		impl.called = false;

		partContextA.dispose();
		partContextB.activate();
		assertTrue(impl.called); // this fails
	}

	@Test
	public void testY() {
		IEclipseContext appContext = EclipseContextFactory.create();
		IEclipseContext windowContext = appContext.createChild();
		IEclipseContext partContextA = windowContext.createChild();
		IEclipseContext partContextB = windowContext.createChild();
		IEclipseContext partContextC = windowContext.createChild();

		partContextA.activateBranch();

		RunAndTrackImpl impl = new RunAndTrackImpl();
		windowContext.runAndTrack(impl);

		partContextB.activate();
		partContextA.dispose();

		impl.called = false;

		partContextC.activate();
		assertTrue(impl.called); // this fails
	}

	static class RunAndTrackImpl extends RunAndTrack {

		boolean called = false;

		@Override
		public boolean changed(IEclipseContext context) {
			context.getActiveLeaf();
			called = true;
			return true;
		}

	}

}

