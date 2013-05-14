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

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.compare.CompareConfiguration;

/**
 * A factory object for creating a <code>Viewer</code>s from a descriptor.
 * <p>
 * It is used when registering a viewer for a specific type
 * in <code>CompareUIPlugin.registerContentViewerDescriptor</code> and
 * in <code>CompareUIPlugin.registerStructureViewerDescriptor</code>.
 *
 * @see org.eclipse.compare.structuremergeviewer.IStructureCreator
 * @see CompareUIPlugin
 */
public interface IViewerDescriptor {

	/**
	 * Creates a new viewer from this descriptor under the given STW parent control.
	 * If the current viewer has the same type as a new viewer
	 * the implementation of this method is free to return the current viewer instead.
	 *
	 * @param currentViewer the current viewer which is going to be replaced with a new viewer.
	 * @param parent the SWT parent control under which the new viewer has to be created.
	 * @param config a compare configuration the new viewer might be interested in.
	 * @return a new viewer or the current viewer.
	 */
	Viewer createViewer(Viewer currentViewer, Composite parent, CompareConfiguration config);
}
