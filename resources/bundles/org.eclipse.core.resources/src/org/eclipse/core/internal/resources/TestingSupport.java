/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.core.internal.resources;

import java.util.Properties;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * Provides special internal access to the workspace resource implementation.
 * This class is to be used for testing purposes only.
 *
 * @since 2.0
 */
public class TestingSupport {
	/**
	 * Returns the save manager's master table.
	 */
	public static Properties getMasterTable() {
		return ((Workspace) ResourcesPlugin.getWorkspace()).getSaveManager().getMasterTable();
	}

	/**
	 * Blocks the calling thread until background snapshot completes.
	 * @since 3.0
	 */
	public static void waitForSnapshot() {
		try {
			((Workspace) ResourcesPlugin.getWorkspace()).getSaveManager().snapshotJob.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException("Interrupted while waiting for snapshot"); //$NON-NLS-1$
		}
	}

	/*
	 * Class cannot be instantiated.
	 */
	private TestingSupport() {
		// not allowed
	}
}
