/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.llm;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Simple JFace dialog to configure the URL and model name of a {@link LlmModel}.
 * Retrieve the result with {@link #getModel()} after {@link #open()} returns
 * {@link org.eclipse.jface.window.Window#OK}.
 */
public class LlmConfigurationDialog extends Dialog {

	private String url;
	private String model;
	private Text urlText;
	private Text modelText;
	private LlmModel result;

	public LlmConfigurationDialog(Shell parent, LlmModel initial) {
		super(parent);
		this.url = initial != null ? initial.url() : ""; //$NON-NLS-1$
		this.model = initial != null ? initial.model() : ""; //$NON-NLS-1$
	}

	public LlmModel getModel() {
		return result;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("LLM Configuration"); //$NON-NLS-1$
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite c = new Composite(area, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(GridData.FILL_BOTH));

		new Label(c, SWT.NONE).setText("URL:"); //$NON-NLS-1$
		urlText = new Text(c, SWT.BORDER);
		urlText.setText(url);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 320;
		urlText.setLayoutData(gd);

		new Label(c, SWT.NONE).setText("Model:"); //$NON-NLS-1$
		modelText = new Text(c, SWT.BORDER);
		modelText.setText(model);
		modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return area;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			result = new LlmModel(urlText.getText().trim(), modelText.getText().trim());
		}
		super.buttonPressed(buttonId);
	}
}
