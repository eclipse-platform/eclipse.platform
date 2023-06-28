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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.graphics.Image;


/**
 * @since 3.2
 *
 */
class LabelResult extends LabelRequestMonitor implements ILabelResult {

	public LabelResult(ModelNode node, AsynchronousModel model) {
		super(node, model);
	}

	@Override
	protected synchronized void scheduleViewerUpdate(long ms) {
		notifyAll();
	}

	@Override
	public synchronized boolean isDone() {
		return super.isDone();
	}

	@Override
	public Object getElement() {
		return getNode().getElement();
	}

	@Override
	public Image[] getImages() {
		return getModel().getViewer().getImages(getImageDescriptors());
	}

	@Override
	public String[] getLabels() {
		return super.getLabels();
	}

	@Override
	public TreePath getTreePath() {
		return getNode().getTreePath();
	}

	@Override
	public int getDepth() {
		int level = 0;
		ModelNode node = getNode().getParentNode();
		while (node != null) {
			node = node.getParentNode();
			level++;
		}
		return level;
	}


}
