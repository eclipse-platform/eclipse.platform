/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareViewerPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

/**
 * Used whenever the input is null or no viewer can be found.
 */
public class NullViewer extends AbstractViewer {

	private Control fDummy;

	public NullViewer(Composite parent) {

		fDummy= new Tree(parent, SWT.NULL);

		CompareViewerPane.clearToolBar(parent);
	}

	@Override
	public Control getControl() {
		return fDummy;
	}
}
