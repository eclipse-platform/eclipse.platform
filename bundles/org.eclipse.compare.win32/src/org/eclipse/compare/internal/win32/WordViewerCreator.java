/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
package org.eclipse.compare.internal.win32;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

/**
 * Factory registered with the <code>org.eclipse.compare.contentMergeViewers</code>
 * extension point to create a Word document comparison.
 */
public class WordViewerCreator implements IViewerCreator {

	@Override
	public Viewer createViewer(Composite parent, CompareConfiguration config) {
		return new WordMergeViewer(parent, config);
	}

	
}
