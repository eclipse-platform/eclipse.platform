/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.preferences;


import java.util.List;

import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class ICButtons implements SelectionListener{

	private HelpContentPreferencePage page;

	private Button addIC;
	private Button editIC;
	private Button removeIC;
	private Button moveUp;
	private Button moveDown;
	private Button testIC;
	private Button enableIC;

	private boolean enabled = true;

	public ICButtons(Composite parent,HelpContentPreferencePage page)
	{
		this.page = page;

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		container.setFont(parent.getFont());

		addIC = createPushButton(container, Messages.HelpContentBlock_addICTitle);
		editIC = createPushButton(container, Messages.HelpContentBlock_editICTitle);
		removeIC = createPushButton(container, Messages.HelpContentBlock_removeICTitle);
		testIC = createPushButton(container, Messages.HelpContentBlock_testConnectionTitle);

		String enableTitle = Messages.HelpContentBlock_3.length() > Messages.HelpContentBlock_4.length() ?
				Messages.HelpContentBlock_3 : Messages.HelpContentBlock_4;
		enableIC = createPushButton(container, enableTitle);
		moveUp = createPushButton(container, Messages.HelpContentBlock_upTitle);
		moveDown = createPushButton(container, Messages.HelpContentBlock_downTitle);

		page.getTable().getTable().addSelectionListener(this);


		updateButtonStates();
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled  = enabled;
		updateButtonStates();
	}

	public Button createPushButton(Composite parent, String buttonText) {
		Button button = new Button(parent, SWT.PUSH);
		button.setFont(parent.getFont());
		button.setText(buttonText);
		button.addSelectionListener(this);
		page.setButtonLayoutData(button);
		return button;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() instanceof Button)
		{
			if (e.getSource()==addIC)
				addIC();
			else if (e.getSource()==editIC)
				editIC();
			else if (e.getSource()==removeIC)
				removeIC();
			else if (e.getSource()==testIC)
				testIC();
			else if (e.getSource()==enableIC)
				enableIC();
			else if (e.getSource()==moveUp)
				move(-1);
			else if (e.getSource()==moveDown)
				move(1);
		}
		else if (e.getSource() instanceof Table)
			updateButtonStates();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		if (e.getSource() instanceof Table)
			editIC();
	}

	public void addIC()
	{
		ICDialog dialog = new ICDialog(page.getShell());
		if (dialog.open() == Window.OK) {
			page.getTable().addIC(dialog.getIC());
			updateButtonStates();
		}
	}

	public void editIC()
	{
		IStructuredSelection selection = (IStructuredSelection)page.getTable().getSelection();
		IC ic = (IC)selection.getFirstElement();
		if (ic==null)
			return;

		ICDialog dialog = new ICDialog(page.getShell(),ic);

		if (dialog.open() == Window.OK) {
			page.getTable().editIC(dialog.getIC());
			updateButtonStates();
		}
	}

	public void removeIC()
	{
		IStructuredSelection selection = (IStructuredSelection)page.getTable().getSelection();
		List<IC> ics = selection.toList();
		String remove = ""; //$NON-NLS-1$

		for (int i=0;i<ics.size();i++)
		{
			remove+="\n"+ics.get(i); //$NON-NLS-1$
		}

		boolean shouldRemove =
			  MessageDialog.openQuestion(
				page.getShell(),
				NLS.bind(Messages.HelpContentBlock_rmvTitle ,""), //$NON-NLS-1$
				NLS.bind(Messages.HelpContentBlock_rmvLabel ,remove));

		if (shouldRemove)
		{
			for (int i=0;i<ics.size();i++)
				page.getTable().removeIC(ics.get(i));
			updateButtonStates();
		}
	}

	public void testIC()
	{
		IStructuredSelection selection = (IStructuredSelection)page.getTable().getSelection();
		IC ic = (IC)selection.getFirstElement();
		if (ic==null)
			return;

		ICDialog dialog = new ICDialog(page.getShell(),ic,true);

		if (dialog.open() == Window.OK) {
			page.getTable().editIC(dialog.getIC());
			updateButtonStates();
		}


/*		boolean connected = TestConnectionUtility.testConnection(ic.getHost(),
					ic.getPort()+"", ic.getPath(),ic.getProtocol());
		TestICDialog dialog = new TestICDialog(page.getShell(),ic);
		dialog.setConnectionStatus(connected);
		dialog.open();
*/
	}

	public void enableIC()
	{
		int indexes[] = page.getTable().getTable().getSelectionIndices();
		IStructuredSelection selection = (IStructuredSelection)page.getTable().getSelection();
		List<IC> ics = selection.toList();

		boolean enable = enableIC.getText().equals(Messages.HelpContentBlock_4);

		for (int i=0;i<ics.size();i++)
		{
			ics.get(i).setEnabled(enable);
			page.getTable().getTableViewer().replace(ics.get(i), indexes[i]);
		}
		page.getTable().refresh();

		updateButtonStates();
	}

	public void move(int offset)
	{
		int index = page.getTable().getTable().getSelectionIndices()[0];

		List<IC> ics = page.getTable().getICs();
		IC x = ics.get(index);
		IC y = ics.get(index + offset);

		ics.set(index+offset,x);
		ics.set(index,y);


		page.getTable().getTableViewer().getContentProvider().inputChanged(
				page.getTable().getTableViewer(), null, ics);

		page.getTable().getTableViewer().replace(x,index+offset);
		page.getTable().getTableViewer().replace(y,index);
		page.getTable().refresh();

		page.getTable().getTable().deselectAll();
		page.getTable().getTable().select(index+offset);
		updateButtonStates();

	}

	public void updateButtonStates()
	{
		if (!enabled)
		{
			editIC.setEnabled(false);
			testIC.setEnabled(false);
			moveUp.setEnabled(false);
			moveDown.setEnabled(false);
			enableIC.setEnabled(false);
			addIC.setEnabled(false);
			removeIC.setEnabled(false);
			return;
		}

		addIC.setEnabled(true);

		IC firstIC = (IC)(((IStructuredSelection)page.getTable().getSelection()).getFirstElement());
		if (firstIC!=null)
		{
			int index = page.getTable().getTable().getSelectionIndices()[0];
			enableIC.setText(firstIC.isEnabled() ? Messages.HelpContentBlock_3 : Messages.HelpContentBlock_4);

			if (page.getTable().getTable().getSelectionIndices().length==1)
			{
				editIC.setEnabled(true);
				testIC.setEnabled(true);
				moveUp.setEnabled(index!=0);
				moveDown.setEnabled(index!=page.getTable().getICs().size()-1);
			}
			else
			{
				editIC.setEnabled(false);
				testIC.setEnabled(false);
				moveUp.setEnabled(false);
				moveDown.setEnabled(false);
			}
			removeIC.setEnabled(true);
			enableIC.setEnabled(true);
		}
		else
		{
			editIC.setEnabled(false);
			testIC.setEnabled(false);
			moveUp.setEnabled(false);
			moveDown.setEnabled(false);
			removeIC.setEnabled(false);
			enableIC.setEnabled(false);
		}
	}
}
