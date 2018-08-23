/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.history;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An adaptation for a history page for displaying comparisons
 * triggered by history page selections.
 *
 * @since 3.2
 * @see HistoryPageSaveablePart
 * @see IHistoryPage
 */
public interface IHistoryCompareAdapter {

	/**
	 * Returns an ICompareInput for the passed in object.
	 * @param object the object
	 * @return returns an ICompareInput
	 */
	public ICompareInput getCompareInput(Object object);

	/**
	 * Prepare the compare input for display. Clients can perform
	 * any long running preparations and assign labels to the
	 * compare configuration.
	 * @param input the compare input
	 * @param configuration the compare configuration
	 * @param monitor a progress monitor
	 */
	public void prepareInput(ICompareInput input, CompareConfiguration configuration, IProgressMonitor monitor);

}
