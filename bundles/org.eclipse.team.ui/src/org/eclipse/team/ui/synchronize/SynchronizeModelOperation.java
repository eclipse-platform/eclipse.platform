/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A specialized team operation that operates on
 * {@link org.eclipse.team.ui.synchronize.ISynchronizeModelElement}elements. If
 * the operation is run in the background the elements the operation is created
 * with will be updated to show that they are busy while the operation is
 * running and will be marked un-busy after the operation completes.
 *
 * @see SyncInfoSet
 * @see SynchronizeModelAction
 * @since 3.0
 */
public abstract class SynchronizeModelOperation extends TeamOperation {

	private IDiffElement[] elements;

	/*
	 * Helper method for extracting the part safely from a configuration
	 */
	private static IWorkbenchPart getPart(ISynchronizePageConfiguration configuration) {
		if (configuration != null) {
			ISynchronizePageSite site = configuration.getSite();
			if (site != null) {
				return site.getPart();
			}
		}
		return null;
	}

	/*
	 * Helper method for extracting the runnable context safely from a configuration
	 */
	private static IRunnableContext getRunnableContext(ISynchronizePageConfiguration configuration) {
		if (configuration != null) {
			return configuration.getRunnableContext();
		}
		return null;
	}

	/**
	 * Create an operation that will operate on the given diff elements.
	 *
	 * @param configuration the participant configuration in which this
	 * operation is run
	 * @param elements the model elements this operation will run with
	 */
	protected SynchronizeModelOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(getPart(configuration), getRunnableContext(configuration));
		this.elements = elements;
	}

	/**
	 * Returns a sync info set that contains the {@link SyncInfo}for the
	 * elements of this operations.
	 *
	 * @return the sync info set that contains the elements this operation is
	 * operating on.
	 */
	protected SyncInfoSet getSyncInfoSet() {
		return makeSyncInfoSetFromSelection(getSyncInfos());
	}

	@Override
	public void scheduled(IJobChangeEvent event) {
		super.scheduled(event);
		markBusy(elements, true);
	}

	@Override
	public void done(IJobChangeEvent event) {
		markBusy(elements, false);
		super.done(event);
	}

	private void markBusy(IDiffElement[] elements, boolean isBusy) {
		for (IDiffElement element : elements) {
			if (element instanceof ISynchronizeModelElement) {
				((ISynchronizeModelElement)element).setPropertyToRoot(ISynchronizeModelElement.BUSY_PROPERTY, isBusy);
			}
		}
	}

	/*
	 * Return the selected SyncInfo for which this action is enabled.
	 *
	 * @return the selected SyncInfo for which this action is enabled.
	 */
	private SyncInfo[] getSyncInfos() {
		List<SyncInfo> filtered = new ArrayList<>();
		for (IDiffElement e : elements) {
			if (e instanceof SyncInfoModelElement) {
				filtered.add(((SyncInfoModelElement)e).getSyncInfo());
			}
		}
		return filtered.toArray(new SyncInfo[filtered.size()]);
	}

	/*
	 * Return a sync info set that contains the given sync info
	 */
	private SyncInfoSet makeSyncInfoSetFromSelection(SyncInfo[] infos) {
		return new SyncInfoSet(infos);
	}
}
