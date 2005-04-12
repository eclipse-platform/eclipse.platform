/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;


public class MirrorsDialog extends Dialog {
	private ISiteWithMirrors site;
	private String siteName;
	private IURLEntry[] mirrors;
	private List mirrorsList;
	private IURLEntry mirrorSelected;
	private Button okButton;
	/**
	 * @param parentShell
	 */
	public MirrorsDialog(Shell parentShell, ISiteWithMirrors site, String siteName) {
		super(parentShell);
		this.site = site;
		this.siteName = siteName;
		try {
			this.mirrors = site.getMirrorSiteEntries();
		} catch (CoreException e) {
			// ignore, as the mirrors have already been queried earlier, so we don't expect error here
		}
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(
				parent,
				IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL,
				true);
		createButton(
			parent,
			IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL,
			false);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		layout.verticalSpacing = 10;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.widthHint = 450;
		data.heightHint = 300;
		composite.setLayoutData(data);
		
		Text text = new Text(composite, SWT.WRAP );
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		text.setLayoutData(data);
		text.setText(NLS.bind("MirrorsDialog.text", siteName));
		text.setBackground(parent.getBackground());
		text.setEditable(false);
		// the text should not receive focus
		text.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				mirrorsList.setFocus();
			}
		});
		
		mirrorsList = new List(composite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		mirrorsList.setLayoutData(data);
		
		mirrorsList.add(siteName);
		for (int i=0; i<mirrors.length; i++)
			mirrorsList.add(mirrors[i].getAnnotation());

		mirrorsList.select(0);
		
		Dialog.applyDialogFont(composite);
		return composite;
	}
		
	protected void okPressed() {
		int i = mirrorsList.getSelectionIndex();
		// first entry is the site itself
		if (i > 0)
			mirrorSelected = mirrors[i-1];
		super.okPressed();
	}
	
	public IURLEntry getMirror() {
		return mirrorSelected;
	}

   protected void configureShell(Shell shell) {
       super.configureShell(shell);
       shell.setText(UpdateUIMessages.MirrorsDialog_title);
    }
}
