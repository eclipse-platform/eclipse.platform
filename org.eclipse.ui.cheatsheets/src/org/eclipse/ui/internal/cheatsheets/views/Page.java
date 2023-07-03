/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.views;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.cheatsheets.CheatSheetStopWatch;

public abstract class Page {
	protected final static int HORZ_SCROLL_INCREMENT = 20;

	protected final static int VERT_SCROLL_INCREMENT = 20;

	// Colors
	protected Color backgroundColor;

	protected FormToolkit toolkit;

	protected ScrolledForm form;

	public Page() {
	}

	public Control getControl() {
		return form;
	}

	public void createPart(Composite parent) {
		init(parent.getDisplay());
		CheatSheetStopWatch.startStopWatch("Page.createInfoArea()"); //$NON-NLS-1$
		CheatSheetStopWatch
				.printLapTime(
						"Page.createInfoArea()", "Time in Page.createInfoArea() after new FormToolkit(): "); //$NON-NLS-1$ //$NON-NLS-2$
		form = toolkit.createScrolledForm(parent);
		form.setData("novarrows", Boolean.TRUE); //$NON-NLS-1$
		form.setText(ViewUtilities.escapeForLabel(getTitle()));
		form.setDelayedReflow(true);
		CheatSheetStopWatch
				.printLapTime(
						"Page.createInfoArea()", "Time in Page.createInfoArea() after createScrolledForm(): "); //$NON-NLS-1$ //$NON-NLS-2$
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 10;
		form.setLayoutData(gd);
		CheatSheetStopWatch
				.printLapTime(
						"Page.createInfoArea()", "Time in Page.createInfoArea() after setLayoutData(): "); //$NON-NLS-1$ //$NON-NLS-2$
		TableWrapLayout layout = new TableWrapLayout();
		CheatSheetStopWatch
				.printLapTime(
						"Page.createInfoArea()", "Time in Page.createInfoArea() after new FormTableWrapLayout(): "); //$NON-NLS-1$ //$NON-NLS-2$
		layout.numColumns = 2;
		// DG - added changes to make the check icon use less space
		// and to compensate for the fix in section layout
		// computation that makes it shorter for 3 pixels.
		layout.leftMargin = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 3;
		form.getBody().setLayout(layout);

		CheatSheetStopWatch
				.printLapTime(
						"Page.createInfoArea()", "Time in Page.createInfoArea() end of method: "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void dispose() {
		if (form != null) {
			form.dispose();
		}

		if (toolkit != null) {
			toolkit.dispose();
		}
		form = null;
		toolkit = null;
	}

	protected void init(Display display) {
		toolkit = new FormToolkit(display);
		backgroundColor = toolkit.getColors().getBackground();
	}

	protected abstract String getTitle();

	public abstract void initialized();

}
