/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.internal.ui.viewers.provisional.IChildrenRequestMonitor;

/**
 * Implementation for <code>IChildrenRequestMonitor</code>. Collects
 * children from an asynchronous tree content adapter.
 * <p>
 * Not intended to be subclassed or instantiated by clients. For use
 * speficially with <code>AsynchronousTreeViewer</code>.
 * </p>
 * @since 3.2
 */
class ChildrenRequestMonitor extends AsynchronousRequestMonitor implements IChildrenRequestMonitor {

	private boolean fFirstUpdate = true;

	/**
	 * Collection of children retrieved
	 */
	private List<Object> fChildren = new ArrayList<>();

	/**
	 * Constucts a monitor to retrieve and update the children of the given
	 * node.
	 *
	 * @param parent parent to retrieve children for
	 * @param model model being updated
	 */
	ChildrenRequestMonitor(ModelNode parent, AsynchronousModel model) {
		super(parent, model);
	}

	@Override
	public void addChild(Object child) {
		synchronized (fChildren) {
			fChildren.add(child);
		}

		scheduleViewerUpdate(250);
	}

	@Override
	public void addChildren(Object[] children) {
		synchronized (fChildren) {
			Collections.addAll(fChildren, children);
		}

		scheduleViewerUpdate(0);
	}

	@Override
	protected boolean contains(AsynchronousRequestMonitor update) {
		return (update instanceof ChildrenRequestMonitor) && contains(update.getNode());
	}

	@Override
	protected void performUpdate() {
		synchronized (fChildren) {
			if (fFirstUpdate) {
				getModel().setChildren(getNode(), fChildren);
				fFirstUpdate = false;
			} else {
				for (Object child : fChildren) {
					getModel().add(getNode(), child);
				}
			}
			fChildren.clear();
		}
	}

}
