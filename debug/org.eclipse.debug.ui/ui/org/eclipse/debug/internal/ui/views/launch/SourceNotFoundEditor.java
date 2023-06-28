/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.launch;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.part.EditorPart;

/**
 * Editor used when no source if found for a stack frame.
 *
 * @since 2.1
 */
public class SourceNotFoundEditor extends EditorPart implements IReusableEditor {

	/**
	 * Text widget used for this editor
	 */
	private Text fText;

	/**
	 * @see org.eclipse.ui.IEditorPart#doSave(IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	/**
	 * @see org.eclipse.ui.IEditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
	}

	/**
	 * @see org.eclipse.ui.IEditorPart#init(IEditorSite, IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) {
			setSite(site);
			setInput(input);
	}

	/**
	 * @see org.eclipse.ui.IEditorPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		return false;
	}

	/**
	 * @see org.eclipse.ui.IEditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		fText = new Text(parent,SWT.MULTI|SWT.READ_ONLY|SWT.WRAP);
		fText.setForeground(JFaceColors.getErrorText(fText.getDisplay()));
		fText.setBackground(fText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		if (getEditorInput() != null) {
			setInput(getEditorInput());
		}
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		if (fText != null) {
			fText.setFocus();
		}
	}

	/**
	 * @see IReusableEditor#setInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void setInput(IEditorInput input) {
		super.setInput(input);
		setPartName(input.getName());
		if (fText != null) {
			fText.setText(input.getToolTipText());
		}
	}

}
