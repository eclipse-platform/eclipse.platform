/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.part.EditorPart;

/**
 * An editor that is deliberately not an {@link org.eclipse.ui.texteditor.ITextEditor}.
 * Registered for the {@code nontexteditortest} extension so tests can open a file
 * whose default editor cannot display a unified diff.
 */
public class NonTextTestEditor extends EditorPart {

	public static final String ID = "org.eclipse.compare.tests.nonTextEditor"; //$NON-NLS-1$

	public static final String EXTENSION = "nontexteditortest"; //$NON-NLS-1$

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing to save
	}

	@Override
	public void doSaveAs() {
		// saving as is not allowed
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		new Label(parent, SWT.NONE).setText("not a text editor"); //$NON-NLS-1$
	}

	@Override
	public void setFocus() {
		// no focusable content
	}
}
