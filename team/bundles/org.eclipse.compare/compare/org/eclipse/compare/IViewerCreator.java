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
package org.eclipse.compare;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

/**
 * A factory object for <code>Viewer</code>.
 * <p>
 * This interface is only required when creating a <code>Viewer</code> from a plugin.xml file.
 * Since <code>Viewer</code>s have no default constructor they cannot be
 * instantiated directly with <code>Class.forName</code>.
 */
public interface IViewerCreator {

	/**
	 * Creates a new viewer under the given SWT parent control.
	 *
	 * @param parent the SWT parent control under which to create the viewer's SWT control
	 * @param config a compare configuration the newly created viewer might want to use
	 * @return a new viewer
	 */
	Viewer createViewer(Composite parent, CompareConfiguration config);
}
