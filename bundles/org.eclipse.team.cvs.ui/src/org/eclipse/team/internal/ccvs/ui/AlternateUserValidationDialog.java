/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class AlternateUserValidationDialog extends Dialog {
	String user;
	String password = ""; //$NON-NLS-1$
	List numXs = new ArrayList();
	Label icon1;
	Label icon2;
	Label icon3;
	Label icon4;
	Text passwordText;
	boolean inUpdate = false;
	
	Image[] images;
	
	public AlternateUserValidationDialog(Shell parentShell, String user) {
		super(parentShell);
		this.user = user;
		initializeImages();
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(CVSUIMessages.AlternateUserValidationDialog_Enter_Password_2); 
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		main.setLayout(layout);
		main.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite iconComposite = new Composite(main, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		iconComposite.setLayout(layout);
		iconComposite.setLayoutData(new GridData());
		
		icon1 = createLabel(iconComposite);
		icon2 = createLabel(iconComposite);
		icon3 = createLabel(iconComposite);
		icon4 = createLabel(iconComposite);
		
		Composite middleComposite = new Composite(main, SWT.NONE);
		middleComposite.setLayout(new GridLayout());
		middleComposite.setLayoutData(new GridData());
		
		Label l = new Label(middleComposite, SWT.NULL);
		l.setText(NLS.bind(CVSUIMessages.AlternateUserValidationDialog_message, new String[] { user })); 
		l.setLayoutData(new GridData());
		l = new Label(middleComposite, SWT.NULL);
		l.setText(""); //$NON-NLS-1$
		l.setLayoutData(new GridData());
		passwordText = new Text(middleComposite, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData();
		data.widthHint = 250;
		passwordText.setLayoutData(data);
		
		passwordText.addVerifyListener(e -> {
			if (inUpdate)
				return;
			e.doit = false;
			inUpdate = true;
			switch (e.character) {
			case 8: {
				// backspace pressed
				if (password.length() > 0) {
					password = password.substring(0, password.length() - 1);
					}
				// get rid of bogus Xs
				int numX = ((Integer) numXs.get(numXs.size() - 1)).intValue();
				numXs.remove(numXs.size() - 1);
				String oldText1 = passwordText.getText();
				String newText1 = oldText1.substring(0, oldText1.length() - numX);
				passwordText.setText(newText1);
				passwordText.setSelection(newText1.length());
				break;
			}
			default: {
				String oldText2 = passwordText.getText();
				String x = getXs();
				numXs.add(numXs.size(), Integer.valueOf(x.length()));
				String newText2 = oldText2 + x;
				passwordText.setText(newText2);
				passwordText.setSelection(newText2.length());
				password += e.character;
				}
			}
			inUpdate = false;
			updateImages();
		});
		/*passwordText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				switch (e.detail) {
					case SWT.TRAVERSE_ARROW_NEXT:
					case SWT.TRAVERSE_ARROW_PREVIOUS:
						e.detail = SWT.TRAVERSE_NONE;
						e.doit = false;
						break;
				}
			}
		});*/
		Composite buttonComposite = new Composite(main, SWT.NONE);
		buttonComposite.setLayout(new GridLayout());
		buttonComposite.setLayoutData(new GridData());
		Button b = new Button(buttonComposite, SWT.PUSH);
		b.setText(CVSUIMessages.AlternateUserValidationDialog_OK_6); 
		data = new GridData();
		data.widthHint = 70;
		b.setLayoutData(data);
		b.addListener(SWT.Selection, event -> okPressed());
		buttonComposite.getShell().setDefaultButton(b);
		b = new Button(buttonComposite, SWT.PUSH);
		b.setText(CVSUIMessages.AlternateUserValidationDialog_Cancel_7); 
		data = new GridData();
		data.widthHint = 70;
		b.setLayoutData(data);
		b.addListener(SWT.Selection, event -> cancelPressed());
		Dialog.applyDialogFont(parent);
		return main;
	}

	@Override
	public boolean close() {
		boolean result = super.close();
		if (images != null) {
			for (int i = 0; i < images.length; i++) {
				images[i].dispose();
				images[i] = null;
			}
			images = null;
		}
		return result;
	}
	public String getPassword() {
		return password;
	}
	
	Label createLabel(Composite parent) {
		Label result = new Label(parent, SWT.NULL);
		GridData data = new GridData();
		data.widthHint = 22;
		data.heightHint = 22;
		result.setLayoutData(data);
		result.setImage(getImage());
		return result;
	}
	Image getImage() {
		double random = Math.random();
		random *= 7; // Random number between 0.0 and 7.0
		long num = Math.round(random);
		return images[(int)num];
	}
	void initializeImages() {
		images = new Image[8];
		for (int i = 0; i < images.length; i++) {
			images[i] = CVSUIPlugin.getPlugin().getImageDescriptor("glyphs/glyph" + (i+1) + ".gif").createImage(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		FontData fd = new FontData();
		fd.setStyle(SWT.BOLD);
		fd.setHeight(10);
		// On Windows, set the font to Sans Serif for an authentic look
		if (System.getProperty("os.name").contains("Windows")) { //$NON-NLS-1$ //$NON-NLS-2$
			fd.setName("Microsoft Sans Serif"); //$NON-NLS-1$
		}
	}
	void updateImages() {
		icon1.setImage(getImage());
		icon2.setImage(getImage());
		icon3.setImage(getImage());
		icon4.setImage(getImage());
	}
	public void setUsername(String user) {
		this.user = user;
	}
	String getXs() {
		double random = Math.random();
		random *= 2;
		random += 2;
		long num = Math.round(random);
		// Random number between 2 and 4
		switch ((int)num) {
			case 2:
				return "XX"; //$NON-NLS-1$
			case 3:
				return "XXX"; //$NON-NLS-1$
			case 4:
				return "XXXX"; //$NON-NLS-1$
		}
		return "X"; //$NON-NLS-1$
	}
	@Override
	protected void cancelPressed() {
		password = null;
		super.cancelPressed();
	}
}
