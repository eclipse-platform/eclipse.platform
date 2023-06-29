/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model;

import org.eclipse.debug.internal.ui.viewers.model.ViewerStateTracker.IElementMementoCollector;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;

/**
 * Request for element memento.
 *
 * @since 3.3
 */
class ElementMementoRequest extends MementoUpdate implements IElementMementoRequest {

	private IElementMementoCollector fManager;
	private ModelDelta fDelta;

	/**
	 * @param provider the content provider to use for the update
	 * @param viewerInput the current input
	 * @param collector Collector to report the result to
	 * @param element the element to update
	 * @param elementPath the path of the element to update
	 * @param memento Memento to encode result into
	 * @param delta Delta to write the result comparison into.
	 */
	public ElementMementoRequest(TreeModelContentProvider provider, Object viewerInput, IElementMementoCollector collector, Object element, TreePath elementPath, IMemento memento, ModelDelta delta) {
		super(provider, viewerInput, provider.getPresentationContext(), element, elementPath, memento);
		fManager = collector;
		fDelta = delta;
	}

	@Override
	public void done() {
		ITreeModelViewer viewer = getContentProvider().getViewer();
		if (viewer == null) {
			return; // disposed
		}
		Display display = viewer.getDisplay();
		if (display != null) {
			if (display.getThread() == Thread.currentThread()) {
				doComplete();
			} else {
				display.asyncExec(this::doComplete);
			}
		}
	}

	private void doComplete() {
		if (getContentProvider().isDisposed()) {
			return;
		}

		if (!isCanceled() && (getStatus() == null || getStatus().isOK())) {
			// replace the element with a memento
			fDelta.setElement(getMemento());
		}
		fManager.requestComplete(ElementMementoRequest.this);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("IElementMementoRequest: "); //$NON-NLS-1$
		buf.append(getElement());
		return buf.toString();
	}
}
