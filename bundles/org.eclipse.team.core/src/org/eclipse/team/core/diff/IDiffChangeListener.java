/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.diff;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Diff change listener that reports changes in an {@link IDiffTree}.
 * Clients may implement this interface.
 *
 * @see IDiffTree
 *
 * @since 3.2
 */
public interface IDiffChangeListener {

	/**
	 * The diff contained in the originating tree has changed.
	 * @param event the change event
	 * @param monitor a progress monitor
	 */
	void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor);

	/**
	 * The given property has changed for the given paths.
	 * @param tree the tree for which the property changed
	 * @param property the property
	 * @param paths the paths
	 */
	void propertyChanged(IDiffTree tree, int property, IPath[] paths);

}
