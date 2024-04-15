/*******************************************************************************
 * Copyright (c) 2020 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.breakpoint;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.Breakpoint;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * Test only implementation of IBreakpoint.
 */
public class TestBreakpoint extends Breakpoint {

	public static final String MODEL = "org.eclipse.debug.tests"; //$NON-NLS-1$
	public static final String TEXT_ATTRIBUTE = "org.eclipse.debug.tests.breakpoint.TestBreakpoint.text"; //$NON-NLS-1$

	public TestBreakpoint() {
		super();
	}

	TestBreakpoint(String text) {
		this(text, IBreakpoint.BREAKPOINT_MARKER);
	}

	TestBreakpoint(String text, final String markerType) {
		final IResource resource = ResourcesPlugin.getWorkspace().getRoot();
		IWorkspaceRunnable wr = monitor -> {
			// create the marker
			setMarker(resource.createMarker(markerType));
			ensureMarker().setAttribute(ID, getModelIdentifier());
			ensureMarker().setAttribute(TEXT_ATTRIBUTE, text);
		};
		try {
			ResourcesPlugin.getWorkspace().run(wr, null);
		} catch (CoreException e) {
			fail("Unexpected exception: " + e); //$NON-NLS-1$
		}

	}

	public String getText() {
		return getMarker().getAttribute(TEXT_ATTRIBUTE, null);
	}

	@Override
	public String getModelIdentifier() {
		return MODEL;
	}

	@Override
	public void setMarker(IMarker marker) throws CoreException {
		assertTrue(getMarker() == null && marker != null);
		super.setMarker(marker);
	}

}