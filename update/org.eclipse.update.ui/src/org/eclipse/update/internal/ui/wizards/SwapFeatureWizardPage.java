package org.eclipse.update.internal.ui.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.internal.ui.UpdateLabelProvider;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIImages;


public class SwapFeatureWizardPage extends WizardPage {
	
	private IFeature currentFeature;
	private IFeature[] features;
	private TableViewer tableViewer;

	public SwapFeatureWizardPage(IFeature currentFeature, IFeature[] features) {
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
				return "v" + ((IFeature)element).getVersionedIdentifier().getVersion().toString(); //$NON-NLS-1$
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
				String v1 = ((IFeature)e1).getVersionedIdentifier().getVersion().toString();
				String v2 = ((IFeature)e2).getVersionedIdentifier().getVersion().toString();

				return v2.compareTo(v1);
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
				setPageComplete(true);
			}
		});
		
		tableViewer.setInput(currentFeature);
		tableViewer.getTable().select(0);
		setControl(tableContainer);
		
		Dialog.applyDialogFont(tableContainer);
	}

	public boolean performFinish() {
		//TODO Dejan to implement the actual swap
		//IStructuredSelection ssel = (IStructuredSelection)tableViewer.getSelection();
		//IFeature chosenFeature = (IFeature)ssel.getFirstElement();
		//swap(currentFeature, chosenFeature);
		return true;
	}

}
