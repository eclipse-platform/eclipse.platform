/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.debug.ui;

/**
 * Optional extension for an {@link IDebugModelPresentation}. Dynamically controls
 * whether labels are computed in a UI thread. The debug platform calls debug model
 * presentation label related methods that do not implement this interface in a
 * <b>non-UI</b> thread.
 * <p>
 * For example, some models may require at least one access in a UI thread to initialize
 * an image registry at which point they may be able to provide labels in a non-UI
 * thread.
 * </p>
 * <p>
 * Clients implementing a debug model presentation should also implement
 * this interface to control which thread labels are generated in.
 * </p>
 * @since 3.4
 */
public interface IDebugModelPresentationExtension extends IDebugModelPresentation {

	/**
	 * Returns whether the UI thread is required to retrieve a label (text, image, font,
	 * foreground, background, etc.), for the specified element. When <code>true</code> is
	 * returned, label related methods will be called in the UI thread, otherwise methods
	 * may be called in a non-UI thread.
	 *
	 * @param element the element a label is to be retrieved for
	 * @return whether label related methods should be called on the UI thread
	 */
	boolean requiresUIThread(Object element);
}
