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
package org.eclipse.team.ui;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * This adapter provides default implementations for methods on {@link ISaveableWorkbenchPart} and
 * {@link IWorkbenchPart}.
 * <p>
 * Classes that want to implement a saveable part can simply implement the methods that
 * they need while accepting the provided defaults for most of the methods.
 * </p>
 * @see SaveablePartDialog
 * @since 3.0
 * @deprecated Clients should use a subclass of {@link CompareEditorInput}
 *      and {@link CompareUI#openCompareDialog(org.eclipse.compare.CompareEditorInput)}
 */
@Deprecated
public abstract class SaveablePartAdapter implements ISaveableWorkbenchPart {

	@Deprecated
	@Override
	public void doSaveAs() {
	}

	@Deprecated
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Deprecated
	@Override
	public boolean isSaveOnCloseNeeded() {
		return false;
	}

	@Deprecated
	@Override
	public void addPropertyListener(IPropertyListener listener) {
	}

	@Deprecated
	@Override
	public void dispose() {
	}

	@Deprecated
	@Override
	public IWorkbenchPartSite getSite() {
		return null;
	}

	@Deprecated
	@Override
	public String getTitleToolTip() {
		return null;
	}

	@Deprecated
	@Override
	public void removePropertyListener(IPropertyListener listener) {
	}

	@Deprecated
	@Override
	public void setFocus() {
	}

	@Deprecated
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}
