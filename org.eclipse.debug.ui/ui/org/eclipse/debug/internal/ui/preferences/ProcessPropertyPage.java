package org.eclipse.debug.internal.ui.preferences;/**********************************************************************Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.This file is made available under the terms of the Common Public License v1.0which accompanies this distribution, and is available athttp://www.eclipse.org/legal/cpl-v10.html**********************************************************************/import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.core.model.IProcess;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Text;import org.eclipse.ui.dialogs.PropertyPage;public class ProcessPropertyPage extends PropertyPage {	/**	 * Constructor for ProcessPropertyPage	 */	public ProcessPropertyPage() {		super();	}	/**	 * @see PreferencePage#createContents(Composite)	 */	protected Control createContents(Composite ancestor) {		noDefaultAndApplyButton();				Composite parent= new Composite(ancestor, SWT.NULL);		GridLayout layout= new GridLayout();		layout.numColumns= 2;		parent.setLayout(layout);				Label l1= new Label(parent, SWT.NULL);		l1.setText(DebugPreferencesMessages.getString("ProcessPropertyPage.Command_Line__1")); //$NON-NLS-1$				GridData gd= new GridData();		gd.verticalAlignment= GridData.BEGINNING;		l1.setLayoutData(gd);				Text l2= new Text(parent, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);		gd= new GridData(GridData.FILL_HORIZONTAL);		gd.widthHint= convertWidthInCharsToPixels(80);		gd.heightHint= convertHeightInCharsToPixels(15);		l2.setLayoutData(gd);				initCommandLineLabel(l2);				return parent;	}		private void initCommandLineLabel(Text l) {		Object o= getElement();		if (o instanceof IDebugTarget)			o= ((IDebugTarget)o).getProcess();		if (o instanceof IProcess) {			IProcess process= (IProcess)o;			String cmdLine= process.getAttribute(IProcess.ATTR_CMDLINE);			if (cmdLine != null)				l.setText(cmdLine);		}	}}