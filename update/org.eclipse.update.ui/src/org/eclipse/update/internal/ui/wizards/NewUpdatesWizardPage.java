package org.eclipse.update.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import java.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.viewers.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.core.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.configuration.*;
import org.eclipse.update.internal.ui.*;
import java.net.URL;
import java.io.*;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.jface.dialogs.MessageDialog;

public class NewUpdatesWizardPage extends BannerPage {
	// NL keys
	private static final String KEY_TITLE = "NewUpdatesWizard.MainPage.title";
	private static final String KEY_DESC = "NewUpdatesWizard.MainPage.desc";
	private static final String KEY_SELECT_ALL =
		"NewUpdatesWizard.MainPage.selectAll";
	private static final String KEY_DESELECT_ALL =
		"NewUpdatesWizard.MainPage.deselectAll";
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
	private Image featureImage;
	private PendingChange[] pendingChanges;
	private Label counterLabel;
	private Button filterCheck;
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
			if (col == 0)
				return featureImage;
			else
				return null;
		}

		/**
		 * @see ITableLabelProvider#getColumnText(Object, int)
		 */
		public String getColumnText(Object obj, int col) {
			if (obj instanceof IFeatureAdapter) {

				try {
					IFeature feature = ((IFeatureAdapter) obj).getFeature();

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
								return UpdateUIPlugin.getResourceString(
									KEY_UNKNOWN_SIZE);
							else
								return feature.getDownloadSize() + "KB";
					}
				} catch (CoreException e) {
					UpdateUIPlugin.logException(e);
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
			Object [] selected = tableViewer.getCheckedElements();
			for (int i = 0; i < selected.length; i++) {
				PendingChange candidate = (PendingChange)selected[i];
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
					IFeature ifeature = iref.getFeature();
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
		setTitle(UpdateUIPlugin.getResourceString(KEY_TITLE));
		setDescription(UpdateUIPlugin.getResourceString(KEY_DESC));
		this.config = config;
		this.pendingChanges = changes;
		featureImage = UpdateUIPluginImages.DESC_FEATURE_OBJ.createImage();
		setBannerVisible(false);
	}

	public void dispose() {
		if (featureImage != null) {
			featureImage.dispose();
			featureImage = null;
		}
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
		button.setText(UpdateUIPlugin.getResourceString(KEY_SELECT_ALL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll(true);
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);

		button = new Button(buttonContainer, SWT.PUSH);
		button.setText(UpdateUIPlugin.getResourceString(KEY_DESELECT_ALL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectAll(false);
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);

		tableViewer.setInput(UpdateUIPlugin.getDefault().getUpdateModel());
		tableViewer.setCheckedElements(pendingChanges);
		counterLabel = new Label(client, SWT.NULL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);
		filterCheck = new Button(client, SWT.CHECK);
		filterCheck.setText(UpdateUIPlugin.getResourceString(KEY_FILTER_CHECK));
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
			UpdateUIPlugin.getFormattedMessage(
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
			setMessage(
				UpdateUIPlugin.getResourceString(KEY_DUPLICATE_WARNING),
				WARNING);
	}

	private void createTableViewer(Composite parent) {
		tableViewer =
			CheckboxTableViewer.newCheckList(
				parent,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);

		TableColumn column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_FEATURE));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_VERSION));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_PROVIDER));

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUIPlugin.getResourceString(KEY_C_SIZE));

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