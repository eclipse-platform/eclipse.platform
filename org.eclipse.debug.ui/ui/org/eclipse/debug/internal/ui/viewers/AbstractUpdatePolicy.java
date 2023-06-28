/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;


/**
 * An update policy updates a viewer based on deltas reported by a model proxy.
 *
 * @since 3.2
 *
 */
public abstract class AbstractUpdatePolicy {

	private AsynchronousViewer fViewer = null;

	public void init(AsynchronousViewer viewer) {
		fViewer = viewer;
	}

	/*
	 * @see org.eclipse.debug.ui.viewers.IUpdatePolicy#dispose()
	 */
	public synchronized void dispose() {
		fViewer = null;
	}

	/**
	 * Returns the viewer this policy is installed on or <code>null</code>
	 * if disposed.
	 *
	 * @return presentation to update
	 */
	public AsynchronousViewer getViewer() {
		return fViewer;
	}

	protected synchronized boolean isDisposed() {
		return fViewer == null;
	}

}
