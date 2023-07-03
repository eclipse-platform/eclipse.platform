/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ui.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tips.core.internal.TipManager;

/**
 * The dialog containing the tips.
 *
 */
@SuppressWarnings("restriction")
public class TipDialog extends Dialog {

	/**
	 * When passed as style, the default style will be used which is
	 * <p>
	 * (SWT.RESIZE | SWT.SHELL_TRIM)
	 */
	public static final int DEFAULT_STYLE = -1;
	private TipManager fTipManager;
	private TipComposite fTipComposite;
	private int fShellStyle;
	private IDialogSettings fDialogSettings;
	private ResourceManager resourceManager;

	public TipDialog(Shell parentShell, TipManager tipManager, int shellStyle, IDialogSettings dialogSettings) {
		super(parentShell);
		fTipManager = tipManager;
		fDialogSettings = dialogSettings;
		fShellStyle = (shellStyle == DEFAULT_STYLE) ? (SWT.RESIZE | SWT.SHELL_TRIM) : shellStyle;
		resourceManager = new LocalResourceManager(JFaceResources.getResources(), parentShell);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return fDialogSettings;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		fixLayout(parent);
		Composite area = (Composite) super.createDialogArea(parent);
		fixLayout(area);
		fTipComposite = new TipComposite(area, SWT.NONE);
		fixLayout(fTipComposite);
		getShell().setText(Messages.TipDialog_0);
		fTipComposite.addDisposeListener(event -> close());
		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite pParent) {
	}

	@Override
	protected Control createButtonBar(Composite pParent) {
		Control bar = super.createButtonBar(pParent);
		bar.setLayoutData(GridDataFactory.swtDefaults().hint(1, 1).create());
		return bar;
	}

	@Override
	protected int getShellStyle() {
		return fShellStyle;
	}

	private void fixLayout(Composite parent) {
		((GridLayout) parent.getLayout()).marginHeight = 0;
		((GridLayout) parent.getLayout()).marginBottom = 0;
		((GridLayout) parent.getLayout()).marginLeft = 0;
		((GridLayout) parent.getLayout()).marginRight = 0;
		((GridLayout) parent.getLayout()).marginWidth = 0;
		((GridLayout) parent.getLayout()).marginTop = 0;
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
	}

	@Override
	public int open() {
		setBlockOnOpen(false);
		int result = super.open();
		if (result == Window.OK) {
			fTipComposite.setTipManager(fTipManager);
		}
		return result;
	}

	@Override
	protected void configureShell(Shell pNewShell) {
		super.configureShell(pNewShell);
		Image pluginImage = DefaultTipManager.getImage("icons/lightbulb.png", resourceManager); //$NON-NLS-1$
		if (pluginImage != null) {
			pNewShell.setImage(pluginImage);
		}
	}
}