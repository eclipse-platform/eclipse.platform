/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * An adapter for a "run to line" operation. The debug platform provides
 * a retargettable "run to line" action that debuggers may plug into
 * by providing an adapter (see <code>IAdaptable</code>) of this type.
 * This allows the platform to provide one command and key binding for
 * the "run to line" function to be shared by many debuggers.
 * <p>
 * When a part is activated, a retargettable action asks the part
 * for its <code>IRunToLineTarget</code> adapter. If one exists,
 * that adapter is delegated to to perform "run to line" operations when
 * the user invokes an associated action. If an adapter does not exist
 * for the part, the retargettable actions asks selected objects in the
 * active part for an adapter. Generally, a debug implementation will
 * provide breakpoint adapters for relevant editors and model objects.
 * </p>
 * <p>
 * Clients are intended to implement this interface and provide instances as
 * an adapter on applicable parts (for example, editors) that support the
 * operation.
 * </p>
 * @since 3.0
 */
public interface IRunToLineTarget {

	/**
	 * Perform a run to line operation on the given element that is
	 * currently selected and suspended in the Debug view. Implementations
	 * must honor the user preference of whether to skip breakpoints
	 * during the operation -
	 * see <code>IDebugUIConstants.PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE</code>.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection the selection on which the action has been invoked
	 * @param target suspended element to perform the "run to line" action on
	 * @throws CoreException if unable to perform the action
	 */
	void runToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target) throws CoreException;

	/**
	 * Returns whether a run to line operation can be performed on the given
	 * element that is currently selected and suspended in the Debug view.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection the selection on which the action has been invoked
	 * @param target suspended element to perform the "run to line" action on
	 * @return if a run to line operation can be performed on the given
	 * element that is currently selected and suspended in the Debug view
	 */
	boolean canRunToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target);
}
