/*******************************************************************************
 * Copyright (c) 2021 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel- initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;

/**
 * A support class for the marker tests.
 */
public class MarkersNumberOfDeltasChangeListener implements IResourceChangeListener {

	private int number = 0;

	/**
	 * Returns the number of resource changed calls.
	 */
	public int numberOfChanges() {
		return number;
	}


	public void reset() {
		number = 0;
	}
	/**
	 * Notification from the workspace.  Extract the marker changes.
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		number++;
	}

}
