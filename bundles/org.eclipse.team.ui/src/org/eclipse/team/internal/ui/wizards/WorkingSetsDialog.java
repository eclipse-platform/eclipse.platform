/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;

import java.util.*;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.IWorkingSet;

public class WorkingSetsDialog extends TitleAreaDialog {

	protected TableViewer wsTableViewer;
	protected Text wsNameText;
	protected Image dlgTitleImage;

	private String selectedWorkingSet;

	public static final String resourceWorkingSetId = "org.eclipse.ui.resourceWorkingSetPage"; //$NON-NLS-1$

	public WorkingSetsDialog(Shell shell) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(TeamUIMessages.WorkingSetsDialog_Title);
		setMessage(TeamUIMessages.WorkingSetsDialog_Message);
		Composite workingSetsComposite = (Composite) super.createDialogArea(parent);
		workingSetsComposite = new Composite(workingSetsComposite, SWT.NONE);
		getShell().setText(TeamUIMessages.WorkingSetsDialog_TitleBar);

		final Composite group = new Composite(workingSetsComposite, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		group.setLayout(layout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Label label = new Label(group, SWT.NONE);
		label.setLayoutData(new GridData());
		label.setText(TeamUIMessages.WorkingSetsDialog_Label);

		wsNameText = new Text(group, SWT.BORDER);
		wsNameText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		workingSetsComposite.setLayout(layout);
		final GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		workingSetsComposite.setLayoutData(data);

		wsTableViewer = new TableViewer(workingSetsComposite, SWT.BORDER);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 450;
		gd.heightHint = 250;
		wsTableViewer.getTable().setLayoutData(gd);

		wsTableViewer.setContentProvider(new ArrayContentProvider());
		wsTableViewer.setLabelProvider(new WorkingSetLabelProvider());
		wsTableViewer.setInput(TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager().getWorkingSets());

		setupListeners();

		return parent;
	}

	@Override
	protected void okPressed() {
		selectedWorkingSet = wsNameText.getText();

		if (selectedWorkingSet.equals("")) { //$NON-NLS-1$
			setErrorMessage(TeamUIMessages.WorkingSetsDialog_ErrorMessage);
			return;
		}

		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}

	@Override
	public boolean close() {
		if (dlgTitleImage != null)
			dlgTitleImage.dispose();
		return super.close();
	}

	void setupListeners() {
		wsTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection s = (IStructuredSelection) event.getSelection();
				Object obj = s.getFirstElement();
				if (obj instanceof IWorkingSet)
					wsNameText.setText(((IWorkingSet) obj).getName());
			}
		});

		wsNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setErrorMessage(null);
			}
		});
	}

	class WorkingSetLabelProvider extends LabelProvider {
		  private Map icons;

		    public WorkingSetLabelProvider() {
		        icons = new Hashtable();
		    }

		    @Override
			public void dispose() {
		        Iterator iterator = icons.values().iterator();

		        while (iterator.hasNext()) {
		            Image icon = (Image) iterator.next();
		            icon.dispose();
		        }
		        super.dispose();
		    }

		    @Override
			public Image getImage(Object object) {
		        Assert.isTrue(object instanceof IWorkingSet);
		        IWorkingSet workingSet = (IWorkingSet) object;
		        ImageDescriptor imageDescriptor = workingSet.getImageDescriptor();

		        if (imageDescriptor == null) {
					return null;
				}

		        Image icon = (Image) icons.get(imageDescriptor);
		        if (icon == null) {
		            icon = imageDescriptor.createImage();
		            icons.put(imageDescriptor, icon);
		        }
		        return icon;
		    }

		    @Override
			public String getText(Object object) {
		        Assert.isTrue(object instanceof IWorkingSet);
		        IWorkingSet workingSet = (IWorkingSet) object;
		        return workingSet.getLabel();
		    }
		}

	public String getSelectedWorkingSet(){
		return selectedWorkingSet;
	}
}
