/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.internal.ui.views.DetailsView;
import org.eclipse.update.internal.ui.views.FeatureSorter;

public class NewUpdatesWizardPage extends BannerPage {
	// NL keys
	private static final String KEY_TITLE = "NewUpdatesWizard.MainPage.title";
	private static final String KEY_DESC = "NewUpdatesWizard.MainPage.desc";
	private static final String KEY_TABLE_LABEL =
		"NewUpdatesWizard.MainPage.tableLabel";
	private static final String KEY_SELECT_ALL =
		"NewUpdatesWizard.MainPage.selectAll";
	private static final String KEY_DESELECT_ALL =
		"NewUpdatesWizard.MainPage.deselectAll";
	private static final String KEY_MORE_INFO =
		"NewUpdatesWizard.MainPage.moreInfo";
	private static final String KEY_FEATURE_DESC =
		"NewUpdatesWizard.MainPage.featureDesc";
	private static final String KEY_COUNTER =
		"NewUpdatesWizard.MainPage.counter";
	private static final String KEY_C_FEATURE =
		"NewUpdatesWizard.MainPage.column.feature";
	private static final String KEY_C_PROVIDER =
		"NewUpdatesWizard.MainPage.column.provider";
	private static final String KEY_C_VERSION =
		"NewUpdatesWizard.MainPage.column.version";
	private static final String KEY_C_SIZE =
		"NewUpdatesWizard.MainPage.column.size";
	private static final String KEY_UNKNOWN_SIZE =
		"NewUpdatesWizard.MainPage.column.sizeUnknown";
	private static final String KEY_FILTER_CHECK =
		"NewUpdatesWizard.MainPage.filterCheck";
	private static final String KEY_DUPLICATE_WARNING =
		"NewUpdatesWizard.MainPage.duplicateWarning";
	private CheckboxTableViewer tableViewer;
	private IInstallConfiguration config;
	private PendingChange[] pendingChanges;
	private Label counterLabel;
	private Button filterCheck;
	private Button moreInfoButton;
	private Text descriptionArea;
	private ContainmentFilter filter = new ContainmentFilter();

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {

		/**
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object parent) {
			return pendingChanges;
		}
	}

	class TableLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		/**
		* @see ITableLabelProvider#getColumnImage(Object, int)
		*/
		public Image getColumnImage(Object obj, int col) {
			if (col == 0) {
				try {
					IFeature feature = ((IFeatureAdapter) obj).getFeature(null);
					boolean patch = feature.isPatch();
					return UpdateUI.getDefault().getLabelProvider().get(
						patch
							? UpdateUIImages.DESC_EFIX_OBJ
							: UpdateUIImages.DESC_FEATURE_OBJ);
				} catch (CoreException e) {
					return UpdateUI.getDefault().getLabelProvider().get(
						UpdateUIImages.DESC_FEATURE_OBJ,
						UpdateLabelProvider.F_ERROR);
				}
			} else
				return null;
		}

		/**
		 * @see ITableLabelProvider#getColumnText(Object, int)
		 */
		public String getColumnText(Object obj, int col) {
			if (obj instanceof IFeatureAdapter) {

				try {
					IFeature feature = ((IFeatureAdapter) obj).getFeature(null);

					switch (col) {
						case 0 :
							return feature.getLabel();
						case 1 :
							return feature
								.getVersionedIdentifier()
								.getVersion()
								.toString();
						case 2 :
							return feature.getProvider();
						case 3 :
							long size = feature.getDownloadSize();
							if (size == -1)
								return UpdateUI.getString(KEY_UNKNOWN_SIZE);
							else
								return feature.getDownloadSize() + "KB";
					}
				} catch (CoreException e) {
					UpdateUI.logException(e);
					return "??";
				}
			}
			return "";
		}
	}

	class ContainmentFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object child) {
			return !isContained((PendingChange) child);
		}
		private boolean isContained(PendingChange job) {
			VersionedIdentifier vid = job.getFeature().getVersionedIdentifier();
			//Object[] selected = tableViewer.getCheckedElements();
			Object [] selected = pendingChanges;
			for (int i = 0; i < selected.length; i++) {
				PendingChange candidate = (PendingChange) selected[i];
				if (candidate.equals(job))
					continue;
				IFeature feature = candidate.getFeature();
				if (includes(feature, vid))
					return true;
			}
			return false;
		}
		private boolean includes(IFeature feature, VersionedIdentifier vid) {
			try {
				IFeatureReference[] irefs =
					feature.getIncludedFeatureReferences();
				for (int i = 0; i < irefs.length; i++) {
					IFeatureReference iref = irefs[i];
					IFeature ifeature = iref.getFeature(null);
					VersionedIdentifier ivid =
						ifeature.getVersionedIdentifier();
					if (ivid.equals(vid))
						return true;
					if (includes(ifeature, vid))
						return true;
				}
			} catch (CoreException e) {
			}
			return false;
		}
	}

	/**
	 * Constructor for ReviewPage
	 */
	public NewUpdatesWizardPage(
		PendingChange[] changes,
		IInstallConfiguration config) {
		super("Target");
		setTitle(UpdateUI.getString(KEY_TITLE));
		setDescription(UpdateUI.getString(KEY_DESC));
		this.config = config;
		this.pendingChanges = changes;
		UpdateUI.getDefault().getLabelProvider().connect(this);
		setBannerVisible(false);
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	/**
	 * @see DialogPage#createControl(Composite)
	 */
	public Control createContents(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		client.setLayout(layout);
		createTableViewer(client);
		Composite buttonContainer = new Composite(client, SWT.NULL);
		GridLayout blayout = new GridLayout();
		blayout.marginWidth = blayout.marginHeight = 0;
		buttonContainer.setLayout(blayout);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);

		Button button = new Button(buttonContainer, SWT.PUSH);
		button.setText(UpdateUI.getString(KEY_SELECT_ALL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll(true);
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);

		button = new Button(buttonContainer, SWT.PUSH);
		button.setText(UpdateUI.getString(KEY_DESELECT_ALL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll(false);
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);

		moreInfoButton = new Button(buttonContainer, SWT.PUSH);
		moreInfoButton.setText(UpdateUI.getString(KEY_MORE_INFO));
		moreInfoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doMoreInfo();
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		moreInfoButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(moreInfoButton);

		tableViewer.setInput(UpdateUI.getDefault().getUpdateModel());
		//tableViewer.setCheckedElements(pendingChanges);
		selectTrueUpdates();

		Label label = new Label(client, SWT.NULL);
		label.setText(UpdateUI.getString(KEY_FEATURE_DESC));
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		descriptionArea =
			new Text(client, SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.heightHint = 64;
		descriptionArea.setLayoutData(gd);

		new Label(client, SWT.NULL);

		counterLabel = new Label(client, SWT.NULL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);
		filterCheck = new Button(client, SWT.CHECK);
		filterCheck.setText(UpdateUI.getString(KEY_FILTER_CHECK));
		filterCheck.setSelection(true);
		tableViewer.addFilter(filter);
		filterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (filterCheck.getSelection())
					tableViewer.addFilter(filter);
				else
					tableViewer.removeFilter(filter);
				pageChanged();
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		filterCheck.setLayoutData(gd);

		pageChanged();
		WorkbenchHelp.setHelp(
			client,
			"org.eclipse.update.ui.NewUpdatesWizardPage");
		return client;
	}
	
	private void selectTrueUpdates() {
		ArrayList trueUpdates = new ArrayList();
		for (int i=0; i<pendingChanges.length; i++) {
			PendingChange job = pendingChanges[i];
			if (!UpdateUI.isPatch(job.getFeature()))
				trueUpdates.add(job);
		}
		tableViewer.setCheckedElements(trueUpdates.toArray()); 
	}

	private void selectAll(boolean state) {
		tableViewer.setAllChecked(state);
		pageChanged();
	}

	private void pageChanged() {
		Object[] checked = tableViewer.getCheckedElements();
		int totalCount = tableViewer.getTable().getItemCount();
		setPageComplete(checked.length > 0);
		String total = "" + totalCount;
		String selected = "" + checked.length;
		counterLabel.setText(
			UpdateUI.getFormattedMessage(
				KEY_COUNTER,
				new String[] { selected, total }));
		boolean duplicates = false;
		for (int i = 0; i < checked.length; i++) {
			PendingChange job = (PendingChange) checked[i];
			if (filter.isContained(job)) {
				duplicates = true;
				break;
			}
		}
		if (!duplicates)
			setMessage(null);
		else
			setMessage(UpdateUI.getString(KEY_DUPLICATE_WARNING), WARNING);
	}

	private void createTableViewer(Composite parent) {
		Label label = new Label(parent, SWT.NULL);
		label.setText(UpdateUI.getString(KEY_TABLE_LABEL));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		tableViewer =
			CheckboxTableViewer.newCheckList(
				parent,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		gd = new GridData(GridData.FILL_BOTH);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);

		TableColumn column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUI.getString(KEY_C_FEATURE));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUI.getString(KEY_C_VERSION));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUI.getString(KEY_C_PROVIDER));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUI.getString(KEY_C_SIZE));

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100, 200, true));
		layout.addColumnData(new ColumnWeightData(100, 50, true));
		layout.addColumnData(new ColumnWeightData(100, 100, true));
		layout.addColumnData(new ColumnWeightData(100, true));
		table.setLayout(layout);

		table.setLayoutData(gd);
		tableViewer.setContentProvider(new TableContentProvider());
		tableViewer.setLabelProvider(new TableLabelProvider());
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				pageChanged();
			}
		});
		tableViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				tableSelectionChanged((IStructuredSelection) e.getSelection());
			}
		});
		tableViewer.setSorter(new FeatureSorter() {
			public int category(Object obj) {
				PendingChange job = (PendingChange)obj;
				if (UpdateUI.isPatch(job.getFeature()))
					return 1;
				return 0;
			}
		});
	}

	private void tableSelectionChanged(IStructuredSelection selection) {
		PendingChange selectedJob = (PendingChange) selection.getFirstElement();
		IFeature feature =
			selectedJob != null ? selectedJob.getFeature() : null;
		IURLEntry descEntry = null;

		if (feature != null)
			descEntry = feature.getDescription();

		moreInfoButton.setEnabled(
			descEntry != null && descEntry.getURL() != null);
		String text = descEntry != null ? descEntry.getAnnotation() : null;
		descriptionArea.setText(text != null ? text : "");
	}

	private void doMoreInfo() {
		IStructuredSelection selection =
			(IStructuredSelection) tableViewer.getSelection();
		PendingChange selectedJob = (PendingChange) selection.getFirstElement();
		IFeature feature =
			selectedJob != null ? selectedJob.getFeature() : null;
		URL url = null;

		if (feature != null) {
			IURLEntry descEntry = feature.getDescription();
			if (descEntry != null)
				url = descEntry.getURL();
		}
		if (url != null)
			DetailsView.showURL(url.toString(), false);
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			tableViewer.getTable().setFocus();
		}
	}

	public PendingChange[] getSelectedJobs() {
		Object[] selected = tableViewer.getCheckedElements();
		PendingChange[] jobs = new PendingChange[selected.length];
		System.arraycopy(selected, 0, jobs, 0, selected.length);
		return jobs;
	}

	public PendingChange[] getSelectedJobsWithLicenses() {
		Object[] selected = tableViewer.getCheckedElements();
		ArrayList list = new ArrayList();
		for (int i = 0; i < selected.length; i++) {
			PendingChange job = (PendingChange) selected[i];
			if (UpdateModel.hasLicense(job))
				list.add(job);
		}
		return (PendingChange[]) list.toArray(new PendingChange[list.size()]);
	}

}
