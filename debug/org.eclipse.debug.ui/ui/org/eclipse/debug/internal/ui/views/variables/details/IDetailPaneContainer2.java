/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
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
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.variables.details;

import org.eclipse.jface.viewers.ISelectionProvider;

/**
 * A detail pane container which allows to set the selection provider of the detail pane.
 *
 * @since 3.5
 */
public interface IDetailPaneContainer2 extends IDetailPaneContainer {

	/**
	 * Set the selection provider of the detail pane.  Allows the container to
	 * forward the selection provider events to the container's workbench site.
	 *
	 * @param provider  the selection provider or <code>null</code>
	 */
	void setSelectionProvider(ISelectionProvider provider);
}
