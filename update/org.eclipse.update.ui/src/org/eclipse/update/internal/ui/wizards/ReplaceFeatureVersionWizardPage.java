/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;

import java.lang.reflect.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.operations.*;


public class ReplaceFeatureVersionWizardPage extends WizardPage {
	
	private IFeature currentFeature;
	private IFeature[] features;
	private TableViewer tableViewer;

	public ReplaceFeatureVersionWizardPage(IFeature currentFeature, IFeature[] features) {
		super("SwapFeature"); //$NON-NLS-1$
		setTitle(UpdateUI.getString("SwapFeatureWizardPage.title")); //$NON-NLS-1$
		setDescription(UpdateUI.getString("SwapFeatureWizardPage.desc")); //$NON-NLS-1$
		this.currentFeature = currentFeature;
		this.features = features;
	}

	public void createControl(Composite parent) {
		Composite tableContainer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		tableContainer.setLayout(layout);

		Label label = new Label(tableContainer, SWT.NONE);
		label.setText(UpdateUI.getString("SwapFeatureWizardPage.label")); //$NON-NLS-1$

		Table table = new Table(tableContainer, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		tableViewer = new TableViewer(table);
		tableViewer.setLabelProvider(new LabelProvider() {
			public Image getImage(Object element) {
				UpdateLabelProvider provider =
					UpdateUI.getDefault().getLabelProvider();
				return provider.get(UpdateUIImages.DESC_UNCONF_FEATURE_OBJ, 0);
			}
			public String getText(Object element) {
				IFeature feature = (IFeature) element;
				return feature.getLabel() + " " + feature.getVersionedIdentifier().getVersion().toString(); //$NON-NLS-1$
			}
		});
		
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object element) {
				return features;
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		
		tableViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				PluginVersionIdentifier v1 = ((IFeature)e1).getVersionedIdentifier().getVersion();
				PluginVersionIdentifier v2 = ((IFeature)e2).getVersionedIdentifier().getVersion();
				return v1.isGreaterOrEqualTo(v2) ? -1 : 1;
			}
		});
		
		tableViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				String version =
					((IFeature) element).getVersionedIdentifier().getVersion().toString();
				return !version.equals(
					currentFeature.getVersionedIdentifier().getVersion().toString());
			}
		});

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ssel = (IStructuredSelection)tableViewer.getSelection();
				if (ssel == null)
					return;
				IFeature chosenFeature = (IFeature)ssel.getFirstElement();
				IStatus validationStatus =
					OperationsManager.getValidator().validatePendingReplaceVersion(currentFeature, chosenFeature);
				setPageComplete(validationStatus == null || validationStatus.getCode() == IStatus.WARNING);
		
				if (validationStatus == null) {
					setErrorMessage(null);
				} else if (validationStatus.getCode() == IStatus.WARNING) {
					setErrorMessage(null);
					setMessage(validationStatus.getMessage(), IMessageProvider.WARNING);
				} else {
					setErrorMessage(validationStatus.getMessage());
				}
			}
		});
		
		tableViewer.setInput(currentFeature);
		tableViewer.getTable().select(0);
		setControl(tableContainer);
		
		Dialog.applyDialogFont(tableContainer);
	}

	public boolean performFinish() {
		IStructuredSelection ssel = (IStructuredSelection)tableViewer.getSelection();
		IFeature chosenFeature = (IFeature)ssel.getFirstElement();
		
		return swap(currentFeature, chosenFeature);
	}
	
	private boolean swap(final IFeature currentFeature, final IFeature anotherFeature) {
//		IStatus status =
//			OperationsManager.getValidator().validatePendingReplaceVersion(currentFeature, anotherFeature);
//		if (status != null) {
//			ErrorDialog.openError(
//				UpdateUI.getActiveWorkbenchShell(),
//				null,
//				null,
//				status);
//			return false;
//		}

		IRunnableWithProgress operation = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException {
				IOperation revertOperation =
					OperationsManager
						.getOperationFactory()
						.createReplaceFeatureVersionOperation(currentFeature, anotherFeature);
				try {
					boolean restartNeeded = revertOperation.execute(monitor, null);
					UpdateUI.requestRestart(restartNeeded);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
			return true;
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			if (targetException instanceof InstallAbortedException) {
				return true;
			} else {
				UpdateUI.logException(e);
			}
			return false;
		} catch (InterruptedException e) {
			return false;
		}
	}

}
