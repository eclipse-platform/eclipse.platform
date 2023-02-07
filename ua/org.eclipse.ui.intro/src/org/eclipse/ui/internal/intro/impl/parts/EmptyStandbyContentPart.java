/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl.parts;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.internal.intro.impl.swt.PageStyleManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.config.IStandbyContentPart;



public class EmptyStandbyContentPart implements IStandbyContentPart {

	private Composite contentComposite;
	private Text contentText;

	@Override
	public void createPartControl(Composite parent, FormToolkit toolkit) {
		contentComposite = toolkit.createComposite(parent);
		contentComposite.setLayout(new GridLayout());
		// Util.highlight(contentComposite, SWT.COLOR_YELLOW);

		String text = Messages.EmptyStandbyContentPart_text;
		Label label = toolkit.createLabel(contentComposite, text, SWT.WRAP);
		label.setFont(PageStyleManager.getBannerFont());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);

		contentText = toolkit.createText(contentComposite, " ", SWT.MULTI //$NON-NLS-1$
				| SWT.WRAP);
		GridData textGd = new GridData(GridData.FILL_BOTH);
		contentText.setLayoutData(textGd);
	}

	@Override
	public Control getControl() {
		return contentComposite;
	}


	public void setMessage(String message) {
		if (message != null) {
			contentText.setText(message);
			contentComposite.layout();
		}
	}

	@Override
	public void init(IIntroPart introPart, IMemento memento) {
		// no-op
	}

	@Override
	public void setInput(Object input) {
		if (input != null)
			setMessage((String) input);
		else
			setMessage(""); //$NON-NLS-1$
	}

	@Override
	public void setFocus() {
		// no-op
	}

	@Override
	public void dispose() {
		// no-op
	}

	@Override
	public void saveState(IMemento memento) {
		// no-op
	}

}
