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
package org.eclipse.compare.internal;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.texteditor.IUpdate;


public abstract class MergeViewerAction extends Action implements IUpdate {

	private boolean fMutable;
	private boolean fSelection;
	private boolean fContent;

	public MergeViewerAction(boolean mutable, boolean selection, boolean content) {
		fMutable= mutable;
		fSelection= selection;
		fContent= content;
	}

	public boolean isSelectionDependent() {
		return fSelection;
	}

	public boolean isContentDependent() {
		return fContent;
	}

	public boolean isEditableDependent() {
		return fMutable;
	}

	@Override
	public void update() {
		// empty default implementation
	}
}
