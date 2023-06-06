/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.registry.SynchronizeWizardDescription;
import org.eclipse.team.internal.ui.synchronize.SynchronizeManager;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Page that allows the user to select a set of resources that are managed
 * by a synchronize participant.
 *
 * Remembers last participant
 *
 * @since 3.0
 */
public class GlobalRefreshWizardSelectionPage extends WizardPage implements IDoubleClickListener, ISelectionChangedListener {

	private final static String DEFAULT_SELECTION= TeamUIPlugin.ID + "GlobalRefreshWizardSelectionPage.default_selection"; //$NON-NLS-1$

	private TableViewer fViewer;
	private IWizard wizard;
	private List<Image> createdImages;

	class MyContentProvider extends BaseWorkbenchContentProvider {
		@Override
		public Object[] getChildren(Object element) {
			if(element instanceof SynchronizeManager) {
				SynchronizeManager manager = (SynchronizeManager)element;
				return manager.getWizardDescriptors();
			}
			return super.getChildren(element);
		}
	}

	class MyLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if(element instanceof SynchronizeWizardDescription) {
				SynchronizeWizardDescription descriptor = (SynchronizeWizardDescription)element;
				return descriptor.getName();
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if(element instanceof SynchronizeWizardDescription) {
				SynchronizeWizardDescription descriptor = (SynchronizeWizardDescription)element;
				ImageDescriptor d = descriptor.getImageDescriptor();
				if(createdImages == null) {
					createdImages = new ArrayList<>(3);
				}
				Image image = d.createImage();
				createdImages.add(image);
				return image;
			}
			return null;
		}
	}

	public GlobalRefreshWizardSelectionPage() {
		super(TeamUIMessages.GlobalRefreshParticipantSelectionPage_0);
		setDescription(TeamUIMessages.GlobalRefreshParticipantSelectionPage_1);
		setTitle(TeamUIMessages.GlobalRefreshParticipantSelectionPage_2);
	}

	@Override
	public void dispose() {
		if (createdImages != null) {
			for (Image image : createdImages) {
				image.dispose();
			}
		}
	}

	/**
	 * Save the page settings into the dialog settings
	 */
	public void savePageSettings() {
		if (fViewer.getControl().isDisposed())
			return;

		final IStructuredSelection selection= fViewer.getStructuredSelection();
		final Object selected= selection.getFirstElement();
		if (!(selected instanceof SynchronizeWizardDescription))
			return;
		getDialogSettings().put(DEFAULT_SELECTION, ((SynchronizeWizardDescription)selected).getId());
	}

	@Override
	public void createControl(Composite parent2) {
		Composite top = new Composite(parent2, SWT.NULL);
		top.setLayout(new GridLayout());
		setControl(top);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(top, IHelpContextIds.REFRESH_WIZARD_SELECTION_PAGE);

		Label l = new Label(top, SWT.NULL);
		l.setText(TeamUIMessages.GlobalRefreshParticipantSelectionPage_3);
		fViewer = new TableViewer(top, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		fViewer.getControl().setLayoutData(data);
		fViewer.setContentProvider(new MyContentProvider());
		fViewer.addDoubleClickListener(this);
		fViewer.setLabelProvider(new MyLabelProvider());
		fViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
		fViewer.setInput(TeamUI.getSynchronizeManager());
		fViewer.addSelectionChangedListener(this);

		Object toSelect= getDefaultSelection();
		if (toSelect == null) {
			toSelect= fViewer.getElementAt(0);
		}
		if (toSelect != null) {
			fViewer.setSelection(new StructuredSelection(toSelect), true);
		}
		fViewer.getTable().setFocus();
		Dialog.applyDialogFont(parent2);
	}

	private SynchronizeWizardDescription getDefaultSelection() {

		if (!(TeamUI.getSynchronizeManager() instanceof SynchronizeManager))
			return null;

		final String defaultSelection= getDialogSettings().get(DEFAULT_SELECTION);
		if (defaultSelection == null)
			return null;

		final SynchronizeManager syncManager= (SynchronizeManager)TeamUI.getSynchronizeManager();
		final SynchronizeWizardDescription [] wizards= syncManager.getWizardDescriptors();
		for (SynchronizeWizardDescription w : wizards) {
			if (defaultSelection.equals(w.getId())) {
				return w;
			}
		}
		return null;
	}

	@Override
	public void doubleClick(DoubleClickEvent event) {
		selectionChanged(
			new SelectionChangedEvent(
				event.getViewer(),
				event.getViewer().getSelection()));
		getContainer().showPage(getNextPage());
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		// Initialize the wizard so we can tell whether to enable the
		// Next button
		ISelection selection = event.getSelection();
		if (selection == null || !(selection instanceof IStructuredSelection)) {
			wizard = null;
			setPageComplete(false);
			return;
		}
		IStructuredSelection ss = (IStructuredSelection) selection;
		if (ss.size() != 1) {
			wizard = null;
			setPageComplete(false);
			return;
		}
		SynchronizeWizardDescription selectedDescriptor = (SynchronizeWizardDescription)ss.getFirstElement();
		try {
			wizard = selectedDescriptor.createWizard();
			wizard.addPages();
			// Ask the container to update button enablement
			setPageComplete(true);
			setDescription(selectedDescriptor.getDescription());
		} catch (CoreException e) {
			Utils.handle(e);
			setPageComplete(false);
		}
	}

	public IWizard getSelectedWizard() {
		return this.wizard;
	}

	@Override
	public IWizardPage getNextPage() {
		if (wizard == null) return null;
		return wizard.getStartingPage();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fViewer.getTable().setFocus();
		}
	}
}
