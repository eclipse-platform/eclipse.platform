/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.doc.internal.dialogs;

import java.util.ArrayList;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

public class SelectTocDialog extends Dialog {

	private int[] selectedTocs = {};
	private List tocList;
	private Toc[] tocs;
	private Button existButton, loadButton, followLinksButton;
	public static final int PAGES_EXIST = 1;
	public static final int LOAD_PAGES = 2;
	public static final int FOLLOW_LINKS = 3;
	public static final int NO_SELECTION = 3;
	private int buttonState;

	public SelectTocDialog(Shell parentShell) {
		super(parentShell);
		String locale = Platform.getNL();
		tocs = HelpPlugin.getTocManager().getTocs(locale);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Check Table of Contents");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite inner = new Composite(parent, SWT.NULL);
		inner.setLayout(new GridLayout());
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
		Label selectLabel = new Label(inner, SWT.NULL);
		selectLabel.setText("Select a TOC to check");
		tocList = new List(inner, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		for (Toc toc : tocs) {
			tocList.add(toc.getLabel());
		}
		tocList.setSelection(0);
		GridData tocData = new GridData(GridData.FILL_BOTH);
		tocData.widthHint = 300;
		tocData.heightHint = 300;
		tocList.setLayoutData(tocData);
		existButton = new Button(inner, SWT.RADIO);
		existButton.setText("Check files in TOC exist");
		loadButton = new Button(inner, SWT.RADIO);
		loadButton.setText("Check files in TOC can load including css/images/javascript");
		followLinksButton = new Button(inner, SWT.RADIO);
		followLinksButton.setText("Check files in TOC can load and check hyperlinks");
		existButton.setSelection(true);
		return inner;
	}

	@Override
	protected void okPressed() {
		selectedTocs = tocList.getSelectionIndices();
		if (existButton.getSelection()) {
			buttonState = PAGES_EXIST;
		} else if (loadButton.getSelection()) {
			buttonState = LOAD_PAGES;
		} else if (followLinksButton.getSelection()) {
			buttonState = FOLLOW_LINKS;
		} else {
			buttonState = NO_SELECTION;
		}
		super.okPressed();
	}

	public Toc[] getTocsToCheck() {
		ArrayList<Toc> selected = new ArrayList<>();
		for (int selectedToc : selectedTocs) {
			selected.add(tocs[selectedToc]);
		}
		Toc[] tocsToCheck = selected.toArray(new Toc[0]) ;
		return tocsToCheck;
	}

	public int getTestKind() {
		return buttonState;
	}

}
