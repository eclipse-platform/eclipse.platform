/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.debug.ui.memory.IMemoryRendering;

/**
 * Represent a view tab in the Memory View or Memory Rendering View
 *
 * Refer to AbstractMemoryViewTab. This is an internal interface. This class is
 * not intended to be implemented by clients.
 *
 * @since 3.0
 */
public interface IMemoryViewTab {
	/**
	 * Remove the view tab.
	 */
	void dispose();

	/**
	 * @return if the view tab is disposed
	 */
	boolean isDisposed();

	/**
	 * @return enablement state of the view tab.
	 */
	boolean isEnabled();

	/**
	 * Sets the enablament state of the view tab.
	 *
	 * @param enabled
	 */
	void setEnabled(boolean enabled);

	/**
	 * Set view tab's label
	 *
	 * @param label
	 */
	void setTabLabel(String label);

	/**
	 * @return view tab's label, null if the label is not available
	 */
	String getTabLabel();

	/**
	 * @return the rendering of this view tab
	 */
	IMemoryRendering getRendering();
}
