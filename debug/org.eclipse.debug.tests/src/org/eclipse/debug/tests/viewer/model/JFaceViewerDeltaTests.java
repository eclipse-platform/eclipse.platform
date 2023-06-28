/*******************************************************************************
 * Copyright (c) 2009, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * @since 3.6
 */
public class JFaceViewerDeltaTests extends DeltaTests {

	@Override
	protected IInternalTreeModelViewer createViewer(Display display, Shell shell) {
		return new TreeModelViewer(fShell, SWT.VIRTUAL, new PresentationContext("TestViewer")); //$NON-NLS-1$
	}

	/**
	 * TODO: remove this method when bug 292322 gets fixed in TreeViewer
	 */
	@Override
	@Test
	public void testBug292322() {
	}
}
