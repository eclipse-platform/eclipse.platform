/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.provisional.cheatsheets;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * An editable task within a composite cheatsheet. An editable task is associated
 * with a task editor and the task editor is responsible for completing the task.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */

public interface IEditableTask extends ICompositeCheatSheetTask {

	/**
	 * Set the state of this task to COMPLETED
	 */
	public void complete();

	/**
	 * Gets a URL which can be used to open the content file for this
	 * task if the content file is specified by a path.
	 * @param path the path to the content file
	 * @throws MalformedURLException
	 * @return a URL to the content file
	 */
	public URL getInputUrl(String path) throws MalformedURLException;

}
