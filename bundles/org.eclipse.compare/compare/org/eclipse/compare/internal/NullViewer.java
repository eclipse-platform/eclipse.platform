/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.eclipse.compare.CompareViewerPane;

/**
 * Used whenever the input is null or no viewer can be found.
 */
public class NullViewer extends AbstractViewer {

	private Control fDummy;

	public NullViewer(Composite parent) {

		fDummy= new Tree(parent, SWT.NULL);

		CompareViewerPane.clearToolBar(parent);
	}

	public Control getControl() {
		return fDummy;
	}
}
