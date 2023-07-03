/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

/**
 * A group of tasks within a composite cheatsheet. Each taskGroup will have
 * children. It does not have an editor and its state is determined from the
 * state of its children.
 * * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */

public interface ITaskGroup extends ICompositeCheatSheetTask {

	/**
	 * A task kind of <b>set</b> indicates that this task is complete when
	 * all subtasks have been completed.
	 */
	public static final String SET = "set"; //$NON-NLS-1$

	/**
	 * A task kind of <b>set</b> indicates that this task is complete when
	 * all subtasks have been completed. The subtasks must be completed in
	 * order.
	 */
	public static final String SEQUENCE = "sequence"; //$NON-NLS-1$

	/**
	 * A task kind of <b>choice</b> indicates that this task is complete when
	 * any of its children has been completed.
	 */
	public static final String CHOICE = "choice"; //$NON-NLS-1$

}
