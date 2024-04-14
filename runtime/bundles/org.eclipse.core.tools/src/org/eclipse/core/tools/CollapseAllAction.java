/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
package org.eclipse.core.tools;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * @since 3.0
 */
public class CollapseAllAction extends Action {

	private static final String label = "Collapse All"; //$NON-NLS-1$
	private TreeViewer viewer;

	public CollapseAllAction(TreeViewer viewer) {
		super(label);
		this.setToolTipText(label);
		this.viewer = viewer;
		this.setImageDescriptor(ImageDescriptor.createFromURLSupplier(true, () -> CollapseAllAction.class.getResource("/icons/collapseall.gif")));
	}

	@Override
	public void run() {
		viewer.collapseAll();
	}
}
