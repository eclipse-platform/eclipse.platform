/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

import java.util.Iterator;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.jface.viewers.*;

/**
 * This class provides a decorator for ISelectionProviders that use
 * TableViewer as their viewer and PluginStats as the basis for their data
 * model. The only affected method is <code>getSelection()</code>, which will
 * return a string that closely resembles the table view of this object.
 *
 */
public class TableSelectionProviderDecorator implements ISelectionProvider {

	/** The decorated selection provider. */
	private ISelectionProvider selectionProvider;

	/**
	 * Constructs a <code>TableSelectionProviderDecorator</code> having
	 * the given selection provider as its decorated object.
	 *
	 * @param selectionProvider the selection provider to be decorated
	 */
	public TableSelectionProviderDecorator(ISelectionProvider selectionProvider) {
		this.selectionProvider = selectionProvider;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.addSelectionChangedListener(listener);
	}

	/**
	 * Returns the current selection for this provider. If the selection is a
	 * structured selection made of <code>PluginStats</code> elements, this method
	 * will return a structured selection of strings that resemble the table view
	 * of this data.
	 *
	 * @return the current selection, printed in table view format
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	@Override
	public ISelection getSelection() {
		// gets the original selection object
		ISelection selection = selectionProvider.getSelection();

		// in these cases the original selection will be returned
		if (selection == null || selection.isEmpty() || !(selection instanceof IStructuredSelection))
			return selection;

		// constructs a list with the selected elements
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;

		StringBuilder copyText = new StringBuilder();
		copyText.append(headerPluginStats());
		copyText.append('\n');
		for (Iterator<?> selectionIter = structuredSelection.iterator(); selectionIter.hasNext();) {
			Object obj = selectionIter.next();
			if (obj instanceof PerformanceStats) {
				copyText.append(prettyPluginStats((PerformanceStats) obj));
				copyText.append('\n');
			}
		}
		return new StructuredSelection(copyText);

	}

	private String headerPluginStats() {
		String retString = ""; //$NON-NLS-1$
		retString += Messages.stats_eventHeader + "\t"; //$NON-NLS-1$
		retString += Messages.stats_blameHeader + "\t"; //$NON-NLS-1$
		retString += Messages.stats_contextHeader + "\t"; //$NON-NLS-1$
		retString += Messages.stats_countHeader + "\t"; //$NON-NLS-1$
		retString += Messages.stats_timeHeader + "\t"; //$NON-NLS-1$
		return retString;
	}

	private String prettyPluginStats(PerformanceStats stats) {
		String retString = ""; //$NON-NLS-1$
		retString += stats.getEvent() + "\t\t"; //$NON-NLS-1$
		retString += stats.getBlameString() + "\t\t"; //$NON-NLS-1$
		retString += stats.getContext() + "\t\t"; //$NON-NLS-1$
		retString += stats.getRunCount() + "\t"; //$NON-NLS-1$
		retString += stats.getRunningTime() + "\t"; //$NON-NLS-1$
		return retString;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.removeSelectionChangedListener(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		selectionProvider.setSelection(selection);
	}

}
