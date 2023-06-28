/*****************************************************************
 * Copyright (c) 2009, 2013 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 238956)
 *     Wind River Systems - ongoing enhancements and bug fixing
 *     IBM Corporation - bug fixing
 *****************************************************************/
package org.eclipse.debug.internal.ui.viewers.update;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.elements.adapters.DefaultBreakpointsViewInput;
import org.eclipse.debug.internal.ui.model.elements.BreakpointManagerContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.ViewerAdapterService;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Breakpoint manager model proxy.
 *
 * @since 3.6
 */
public class BreakpointManagerProxy extends AbstractModelProxy {
	/**
	 * The breakpoint manager content provider for this model proxy
	 */
	final private BreakpointManagerContentProvider fProvider;

	/**
	 * The breakpoint manager input for this model proxy
	 */
	final private DefaultBreakpointsViewInput fInput;

	/**
	 * Job to fire posted deltas.
	 */
	private Job fFireModelChangedJob;

	/**
	 * Object used for describing a posted delta.
	 */
	private static class DeltaInfo {
		final boolean fSelect;
		final IModelDelta fDelta;

		DeltaInfo(boolean selectDelta, IModelDelta delta) {
			fSelect = selectDelta;
			fDelta = delta;
		}
	}

	/**
	 * List of posted deltas ready to be fired.
	 */
	private List<DeltaInfo> fPendingDeltas = new LinkedList<>();


	/**
	 * Constructor.
	 *
	 * @param input the breakpoint manager input
	 * @param context the presentation context for this model proxy
	 */
	public BreakpointManagerProxy(Object input, IPresentationContext context) {
		super();

		DefaultBreakpointsViewInput bpmInput = null;
		BreakpointManagerContentProvider bpmProvider = null;
		if (input instanceof DefaultBreakpointsViewInput) {
			bpmInput = (DefaultBreakpointsViewInput) input;

			// cache the required data and pass to the provider when this model is installed
			IElementContentProvider provider = ViewerAdapterService.getContentProvider(input);
			if (provider instanceof BreakpointManagerContentProvider) {
				bpmProvider = (BreakpointManagerContentProvider) provider;
			}
		}
		fInput = bpmInput;
		fProvider = bpmProvider;
	}

	@Override
	public void installed(Viewer viewer) {
		super.installed(viewer);
		if (fProvider != null) {
			fProvider.registerModelProxy(fInput, this);
		}
	}

	@Override
	public void dispose() {
		fProvider.unregisterModelProxy(fInput, this);
		synchronized(this) {
			if (fFireModelChangedJob != null) {
				fFireModelChangedJob.cancel();
				fFireModelChangedJob = null;
			}
			fPendingDeltas.clear();
		}

		super.dispose();
	}

	/**
	 * Posts a given delta to be fired by the proxy.  Posting a delta places it
	 * in a queue which is later emptied by a job that fires the deltas.
	 * <p>
	 * If the delta is used only to select a breakpiont and does not change the
	 * viewer content, the caller should set the <code>select</code> parameter
	 * to <code>true</code>.  When a select delta is added to the delta queue,
	 * any previous select deltas are removed.
	 *
	 * @param delta Delta to be posted to the viewer.
	 * @param select Flag indicating that the delta is only to change the
	 * viewer selection.
	 */
	public synchronized void postModelChanged(IModelDelta delta, boolean select) {
		// Check for proxy being disposed.
		if (isDisposed()) {
			return;
		}
		// Check for viewer being disposed.
		Widget viewerControl = getViewer().getControl();
		if (viewerControl == null) {
			return;
		}

		// If we are processing a select delta, remove the previous select delta.
		if (select) {
			for (Iterator<DeltaInfo> itr = fPendingDeltas.iterator(); itr.hasNext();) {
				if ( itr.next().fSelect ) {
					itr.remove();
				}
			}
		}
		fPendingDeltas.add(new DeltaInfo(select, delta));

		if (fFireModelChangedJob == null) {
			fFireModelChangedJob = new WorkbenchJob(viewerControl.getDisplay(), "Select Breakpoint Job") { //$NON-NLS-1$
				{
					setSystem(true);
				}

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					Object[] deltas;
					synchronized(BreakpointManagerProxy.this) {
						deltas = fPendingDeltas.toArray();
						fPendingDeltas.clear();
						fFireModelChangedJob = null;
					}
					for (Object pendingDelta : deltas) {
						fireModelChanged( ((DeltaInfo)pendingDelta).fDelta );
					}
					return Status.OK_STATUS;
				}
			};
			fFireModelChangedJob.schedule();
		}
	}

}
