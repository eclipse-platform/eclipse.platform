/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

public class ReviewPage
	extends BannerPage
	implements IUpdateSearchResultCollectorFromMirror {

	private Label label;
	private ArrayList jobs;
	private Label counterLabel;
	private CheckboxTableViewer tableViewer;
	private IStatus validationStatus;
	private Collection problematicFeatures = new HashSet();
	// feature that was recently selected or null
	private IFeature newlySelectedFeature;
	// 
	private FeatureStatus lastDisplayedStatus;
	private PropertyDialogAction propertiesAction;
	private ScrolledFormText descLabel;
	private Button statusButton;
	private Button moreInfoButton;
	private Button propertiesButton;
	private Button filterCheck;
	private ContainmentFilter filter = new ContainmentFilter();
	private SearchRunner searchRunner;
	private int LABEL_ORDER = 1;
	private int VERSION_ORDER = 1;
	private int PROVIDER_ORDER = 1;
	class JobsContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return jobs.toArray();
		}
	}

	class JobsLabelProvider
		extends SharedLabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int column) {
			IInstallFeatureOperation job = (IInstallFeatureOperation) obj;
			IFeature feature = job.getFeature();

			String text = null;
			switch (column) {
				case 0 :
					text = feature.getLabel();
					break;
				case 1 :
					text = feature
						.getVersionedIdentifier()
						.getVersion()
						.toString();
					break;
				case 2 :
					text = feature.getProvider();
					break;
			}
			if (text == null)
				text = ""; //$NON-NLS-1$
			return text;
		}
		public Image getColumnImage(Object obj, int column) {
			if (column == 0) {
				IFeature feature = ((IInstallFeatureOperation) obj).getFeature();
				boolean patch = feature.isPatch();
				
				boolean problematic=problematicFeatures.contains(feature);
				
				if (patch) {
					return get(UpdateUIImages.DESC_EFIX_OBJ, problematic? F_ERROR : 0);
				} else {
					return get(UpdateUIImages.DESC_FEATURE_OBJ, problematic? F_ERROR : 0);
				}
			}
			return null;
		}
	}

	class ContainmentFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object child) {
			return !isContained((IInstallFeatureOperation) child);
		}
		private boolean isContained(IInstallFeatureOperation job) {
			VersionedIdentifier vid = job.getFeature().getVersionedIdentifier();

			for (int i = 0; i < jobs.size(); i++) {
				IInstallFeatureOperation candidate = (IInstallFeatureOperation) jobs.get(i);
				if (candidate.equals(job))
					continue;
				IFeature feature = candidate.getFeature();
				if (includes(feature, vid,null))
					return true;
			}
			return false;
		}
		private boolean includes(IFeature feature, VersionedIdentifier vid, ArrayList cycleCandidates) {
			try {
				if (cycleCandidates == null)
					cycleCandidates = new ArrayList();
				if (cycleCandidates.contains(feature))
					throw Utilities.newCoreException(UpdateUI.getFormattedMessage("InstallWizard.ReviewPage.cycle", feature.getVersionedIdentifier().toString()), null); //$NON-NLS-1$
				else
					cycleCandidates.add(feature);
				IFeatureReference[] irefs =
					feature.getIncludedFeatureReferences();
				for (int i = 0; i < irefs.length; i++) {
					IFeatureReference iref = irefs[i];
					IFeature ifeature = iref.getFeature(null);
					VersionedIdentifier ivid =
						ifeature.getVersionedIdentifier();
					if (ivid.equals(vid))
						return true;
					if (includes(ifeature, vid, cycleCandidates))
						return true;
				}
				return false;
			} catch (CoreException e) {
				return false;
			} finally {
				// after this feature has been DFS-ed, it is no longer a cycle candidate
				cycleCandidates.remove(feature);
			}
		}
	}

	class FeaturePropertyDialogAction extends PropertyDialogAction {
		private IStructuredSelection selection;

		public FeaturePropertyDialogAction(
			Shell shell,
			ISelectionProvider provider) {
			super(shell, provider);
		}

		public IStructuredSelection getStructuredSelection() {
			return selection;
		}

		public void selectionChanged(IStructuredSelection selection) {
			this.selection = selection;
		}

	}
	/**
	 * Constructor for ReviewPage2
	 */
	public ReviewPage(SearchRunner searchRunner, ArrayList jobs) {
		super("Review"); //$NON-NLS-1$
		setTitle(UpdateUI.getString("InstallWizard.ReviewPage.title")); //$NON-NLS-1$
		setDescription(UpdateUI.getString("InstallWizard.ReviewPage.desc")); //$NON-NLS-1$
		UpdateUI.getDefault().getLabelProvider().connect(this);
		this.searchRunner = searchRunner;
		setBannerVisible(false);
		this.jobs = jobs;
		if (this.jobs==null) this.jobs = new ArrayList();
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		
		// when searching for updates, only nested patches can be shown.
		// when searching for features, features and patches can be shown
		String filterText = filterCheck.getText();
		String filterFeatures = UpdateUI.getString("InstallWizard.ReviewPage.filterFeatures"); //$NON-NLS-1$
		String filterPatches = UpdateUI.getString("InstallWizard.ReviewPage.filterPatches"); //$NON-NLS-1$
		boolean isUpdateSearch = searchRunner.getSearchProvider() instanceof ModeSelectionPage;
		if (isUpdateSearch && filterText.equals(filterFeatures))
			filterCheck.setText(filterPatches);
		else if ( !isUpdateSearch && filterText.equals(filterPatches))
			filterCheck.setText(filterFeatures);
		
		if (visible && searchRunner.isNewSearchNeeded()) {
			jobs.clear();

			setDescription(UpdateUI.getString("InstallWizard.ReviewPage.searching")); //$NON-NLS-1$;
			label.setText(UpdateUI.getString("")); //$NON-NLS-1$

			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					searchRunner.runSearch();
					performPostSearchProcessing();
				}
			});
		}
	}

	private void performPostSearchProcessing() {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				if (tableViewer != null) {
					tableViewer.refresh();
					tableViewer.getTable().layout(true);
					if (searchRunner.getSearchProvider() instanceof ModeSelectionPage) {
						selectTrueUpdates();
					}
				}
				pageChanged();
				
				int totalCount = tableViewer.getTable().getItemCount();
				if(totalCount >0) {
					setDescription(UpdateUI.getString("InstallWizard.ReviewPage.desc")); //$NON-NLS-1$;
					label.setText(UpdateUI.getString("InstallWizard.ReviewPage.label")); //$NON-NLS-1$
				} else {
					boolean isUpdateSearch = searchRunner.getSearchProvider() instanceof ModeSelectionPage;
					if (isUpdateSearch)
						setDescription(UpdateUI.getString("InstallWizard.ReviewPage.zeroUpdates")); //$NON-NLS-1$
					else
						setDescription(UpdateUI.getString("InstallWizard.ReviewPage.zeroFeatures")); //$NON-NLS-1$
					label.setText("");
				}

			}
		});
	}
	
	private void selectTrueUpdates() {
		ArrayList trueUpdates = new ArrayList();
		for (int i=0; i<jobs.size(); i++) {
			IInstallFeatureOperation job = (IInstallFeatureOperation)jobs.get(i);
			if (!UpdateUtils.isPatch(job.getFeature()))
				trueUpdates.add(job);
		}
		tableViewer.setCheckedElements(trueUpdates.toArray()); 
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
		label = new Label(client, SWT.NULL);
		label.setText(UpdateUI.getString("InstallWizard.ReviewPage.label")); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		createTable(client);

		Composite comp = new Composite(client, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
				
		Composite buttonContainer = new Composite(comp, SWT.NULL);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0; //30?
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		Button button = new Button(buttonContainer, SWT.PUSH);
		button.setText(UpdateUI.getString("InstallWizard.ReviewPage.selectAll")); //$NON-NLS-1$
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(true);
			}
		});

		button = new Button(buttonContainer, SWT.PUSH);
		button.setText(UpdateUI.getString("InstallWizard.ReviewPage.deselectAll")); //$NON-NLS-1$
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(false);
			}
		});

		moreInfoButton = new Button(buttonContainer, SWT.PUSH);
		moreInfoButton.setText(UpdateUI.getString("InstallWizard.ReviewPage.moreInfo")); //$NON-NLS-1$
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		moreInfoButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(moreInfoButton);
		moreInfoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleMoreInfo();
			}
		});
		moreInfoButton.setEnabled(false);
		
		
		propertiesButton = new Button(buttonContainer, SWT.PUSH);
		propertiesButton.setText(UpdateUI.getString("InstallWizard.ReviewPage.properties")); //$NON-NLS-1$
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		propertiesButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(propertiesButton);
		propertiesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleProperties();
			}
		});
		propertiesButton.setEnabled(false);

		statusButton = new Button(buttonContainer, SWT.PUSH);
		statusButton.setText(UpdateUI.getString("InstallWizard.ReviewPage.showStatus")); //$NON-NLS-1$
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		statusButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(statusButton);
		statusButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showStatus();
			}
		});

		//new Label(client, SWT.NULL);

		counterLabel = new Label(client, SWT.NULL);
		gd = new GridData();
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);

		filterCheck = new Button(client, SWT.CHECK);
		filterCheck.setText(UpdateUI.getString("InstallWizard.ReviewPage.filterFeatures")); //$NON-NLS-1$
		filterCheck.setSelection(false);
		//tableViewer.addFilter(filter);
		filterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (filterCheck.getSelection()) {
					// make sure model is local
					if (downloadIncludedFeatures()) {
						tableViewer.addFilter(filter);
					} else {
						filterCheck.setSelection(false);
					}
				} else {
					tableViewer.removeFilter(filter);
				}
				pageChanged();
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		filterCheck.setLayoutData(gd);
		pageChanged();

		WorkbenchHelp.setHelp(client, "org.eclipse.update.ui.MultiReviewPage2"); //$NON-NLS-1$

		Dialog.applyDialogFont(parent);

		return client;
	}

	private void createTable(Composite parent) {
		SashForm sform = new SashForm(parent, SWT.VERTICAL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 250;
		gd.heightHint =100;
		sform.setLayoutData(gd);

		TableLayoutComposite composite= new TableLayoutComposite(sform, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		tableViewer =
			CheckboxTableViewer.newCheckList(
					composite,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		Table table = tableViewer.getTable();

		table.setHeaderVisible(true);

		TableColumn column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUI.getString("InstallWizard.ReviewPage.feature")); //$NON-NLS-1$
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				LABEL_ORDER *= -1;
				tableViewer.setSorter(
					new FeatureSorter(
						FeatureSorter.FEATURE_LABEL,
						LABEL_ORDER,
						VERSION_ORDER,
						PROVIDER_ORDER));
			}
		});

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUI.getString("InstallWizard.ReviewPage.version")); //$NON-NLS-1$
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				VERSION_ORDER *= -1;
				tableViewer.setSorter(
					new FeatureSorter(
						FeatureSorter.FEATURE_VERSION,
						LABEL_ORDER,
						VERSION_ORDER,
						PROVIDER_ORDER));
			}
		});

		column = new TableColumn(table, SWT.NULL);
		column.setText(UpdateUI.getString("InstallWizard.ReviewPage.provider")); //$NON-NLS-1$
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PROVIDER_ORDER *= -1;
				tableViewer.setSorter(
					new FeatureSorter(
						FeatureSorter.FEATURE_PROVIDER,
						LABEL_ORDER,
						VERSION_ORDER,
						PROVIDER_ORDER));
			}
		});

		//TableLayout layout = new TableLayout();
		composite.addColumnData(new ColumnWeightData(5, 275, true));
		composite.addColumnData(new ColumnWeightData(1, 80, true));
		composite.addColumnData(new ColumnWeightData(5, 90, true));

		//layout.addColumnData(new ColumnPixelData(225, true));
		//layout.addColumnData(new ColumnPixelData(80, true));
		//layout.addColumnData(new ColumnPixelData(140, true));

		//table.setLayout(layout);

		tableViewer.setContentProvider(new JobsContentProvider());
		tableViewer.setLabelProvider(new JobsLabelProvider());
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				newlySelectedFeature = null;
				if(event.getChecked()){
					// save checked feeature, so its error can be shown if any
					Object checked = event.getElement();
					if(checked instanceof IInstallFeatureOperation){
						newlySelectedFeature= ((IInstallFeatureOperation)checked).getFeature();
					}
				}
				tableViewer
					.getControl()
					.getDisplay()
					.asyncExec(new Runnable() {
					public void run() {
						pageChanged();
					}
				});
			}
		});

		tableViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				jobSelected((IStructuredSelection) e.getSelection());
			}
		});

		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleProperties();
			}
		});
		tableViewer.setSorter(new FeatureSorter());
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		table.setMenu(menuMgr.createContextMenu(table));
		
		tableViewer.setInput(UpdateUI.getDefault().getUpdateModel());
		tableViewer.setAllChecked(true);
		
		descLabel = new ScrolledFormText(sform, true);
		descLabel.setText("");
		descLabel.setBackground(parent.getBackground());
		HyperlinkSettings settings = new HyperlinkSettings(parent.getDisplay());
		descLabel.getFormText().setHyperlinkSettings(settings);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;
		descLabel.setLayoutData(gd);
		
		sform.setWeights(new int[] {10, 2});
	}
	
	private void fillContextMenu(IMenuManager manager) {
		if (tableViewer.getSelection().isEmpty()) return;
		Action action = new Action(UpdateUI.getString("InstallWizard.ReviewPage.prop")) { //$NON-NLS-1$
			public void run() {
				handleProperties();
			}
		};
		manager.add(action);
	}

	public void accept(final IFeature feature) {
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				IInstallFeatureOperation job = OperationsManager.getOperationFactory().createInstallOperation( null, feature,null, null, null);
				ViewerFilter[] filters = tableViewer.getFilters();
				boolean visible = true;

				for (int i = 0; i < filters.length; i++) {
					ViewerFilter filter = filters[i];
					if (!filter.select(tableViewer, null, job)) {
						visible = false;
						break;
					}
				}
				if (visible) {
					tableViewer.add(job);
					updateItemCount(0, -1);
				}
				jobs.add(job);
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.search.IUpdateSearchResultCollectorFromMirror#getMirror(org.eclipse.update.core.ISite, java.lang.String)
	 */
	public IURLEntry getMirror(ISiteWithMirrors site, String siteName) {
		try {
			IURLEntry[] mirrors = site.getMirrorSiteEntries();
			if (mirrors.length == 0)
				return null;
			else {
				// here we need to prompt the user
				final MirrorsDialog dialog = new MirrorsDialog(getShell(), site, siteName);
				getShell().getDisplay().syncExec(new Runnable() {
					public void run() {
						dialog.create();
						dialog.open();
					}
				});
				return dialog.getMirror();
			}
		} catch (CoreException e) {
			return null;
		}
	}
	private void jobSelected(IStructuredSelection selection) {
		IInstallFeatureOperation job = (IInstallFeatureOperation) selection.getFirstElement();
		IFeature feature = job != null ? job.getFeature() : null;
		IURLEntry descEntry = feature != null ? feature.getDescription() : null;
		String desc = null;
		if (descEntry != null)
			desc = descEntry.getAnnotation();
		if (desc == null)
			desc = ""; //$NON-NLS-1$
		descLabel.setText(desc);
		propertiesButton.setEnabled(feature != null);
		moreInfoButton.setEnabled(job != null && getMoreInfoURL(job) != null);
	}
	
	private void pageChanged() {
		Object[] checked = tableViewer.getCheckedElements();
		int totalCount = tableViewer.getTable().getItemCount();
		updateItemCount(checked.length, totalCount);
		if (checked.length > 0) {
			validateSelection();
		} else {
			lastDisplayedStatus = null;
			setErrorMessage(null);
			setPageComplete(false);
			validationStatus = null;
			problematicFeatures.clear();
		}
		tableViewer.update(jobs.toArray(), null);
		statusButton.setEnabled(validationStatus != null);
	}

	private void updateItemCount(int checkedCount, int totalCount) {
		if (checkedCount == -1) {
			Object[] checkedElements = tableViewer.getCheckedElements();
			checkedCount = checkedElements.length;
		}
		if (totalCount == -1) {
			totalCount = tableViewer.getTable().getItemCount();
		}
		String total = "" + totalCount; //$NON-NLS-1$
		String selected = "" + checkedCount; //$NON-NLS-1$
		counterLabel.setText(
			UpdateUI.getFormattedMessage(
				"InstallWizard.ReviewPage.counter", //$NON-NLS-1$
				new String[] { selected, total }));
		counterLabel.getParent().layout();
	}

	private void handleSelectAll(boolean select) {
		tableViewer.setAllChecked(select);
		tableViewer.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				pageChanged();
			}
		});
	}

	private void handleProperties() {
		final IStructuredSelection selection =
			(IStructuredSelection) tableViewer.getSelection();

		final IInstallFeatureOperation job =
			(IInstallFeatureOperation) selection.getFirstElement();
		if (propertiesAction == null) {
			propertiesAction =
				new FeaturePropertyDialogAction(getShell(), tableViewer);
		}

		BusyIndicator
			.showWhile(tableViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				SimpleFeatureAdapter adapter =
					new SimpleFeatureAdapter(job.getFeature());
				propertiesAction.selectionChanged(
					new StructuredSelection(adapter));
				propertiesAction.run();
			}
		});
	}

	private String getMoreInfoURL(IInstallFeatureOperation job) {
		IURLEntry desc = job.getFeature().getDescription();
		if (desc != null) {
			URL url = desc.getURL();
			return (url == null) ? null : url.toString();
		}
		return null;
	}

	private void handleMoreInfo() {
		IStructuredSelection selection =
			(IStructuredSelection) tableViewer.getSelection();
		final IInstallFeatureOperation selectedJob =
			(IInstallFeatureOperation) selection.getFirstElement();
		BusyIndicator
			.showWhile(tableViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				String urlName = getMoreInfoURL(selectedJob);
				UpdateUI.showURL(urlName);
			}
		});
	}

	public IInstallFeatureOperation[] getSelectedJobs() {
		Object[] selected = tableViewer.getCheckedElements();
		IInstallFeatureOperation[] result = new IInstallFeatureOperation[selected.length];
		System.arraycopy(selected, 0, result, 0, selected.length);
		return result;
	}

	public void validateSelection() {
		IInstallFeatureOperation[] jobs = getSelectedJobs();
		validationStatus =
			OperationsManager.getValidator().validatePendingChanges(jobs);
		problematicFeatures.clear();
		if (validationStatus != null) {
			IStatus[] status = validationStatus.getChildren();
			for (int i = 0; i < status.length; i++) {
				IStatus singleStatus = status[i];
				if(isSpecificStatus(singleStatus)){
					IFeature f = ((FeatureStatus) singleStatus).getFeature();
					problematicFeatures.add(f);				
				}
			}
		}

		setPageComplete(validationStatus == null || validationStatus.getCode() == IStatus.WARNING);
		
		updateWizardMessage();
	}

	private void showStatus() {
		if (validationStatus != null) {
			new StatusDialog().open();
		}

	}
	/**
	 * Check whether status is relevant to show for
	 * a specific feature or is a other problem
	 * @param status
	 * @return true if status is FeatureStatus with
	 * specified feature and certain error codes
	 */
	private boolean isSpecificStatus(IStatus status){
		if(!(status instanceof FeatureStatus)){
			return false;
		}
		if(status.getSeverity()!=IStatus.ERROR){
			return false;
		}
		FeatureStatus featureStatus = (FeatureStatus) status;
		if(featureStatus.getFeature()==null){
			return false;
		}
		return 0!= (featureStatus.getCode()
				& FeatureStatus.CODE_CYCLE
				+ FeatureStatus.CODE_ENVIRONMENT
				+ FeatureStatus.CODE_EXCLUSIVE
				+ FeatureStatus.CODE_OPTIONAL_CHILD
				+ FeatureStatus.CODE_PREREQ_FEATURE
				+ FeatureStatus.CODE_PREREQ_PLUGIN);
	}
	/**
	 * Update status in the wizard status area
	 */
	private void updateWizardMessage() {
		if (validationStatus == null) {
			lastDisplayedStatus=null;
			setErrorMessage(null);
		} else if (validationStatus.getCode() == IStatus.WARNING) {
			lastDisplayedStatus=null;
			setErrorMessage(null);
			setMessage(validationStatus.getMessage(), IMessageProvider.WARNING);
		} else {
			// 1.  Feature selected, creating a problem for it, show status for it
			if(newlySelectedFeature !=null){
				IStatus[] status = validationStatus.getChildren();
				for(int s =0; s< status.length; s++){
					if(isSpecificStatus(status[s])){
						FeatureStatus featureStatus = (FeatureStatus)status[s];
						if(newlySelectedFeature.equals(featureStatus.getFeature())){
							lastDisplayedStatus=featureStatus;
							setErrorMessage(featureStatus.getMessage());
							return;
						}
					}
				}
			}
			
			// 2.  show old status if possible (it is still valid)
			if(lastDisplayedStatus !=null){
				IStatus[] status = validationStatus.getChildren();
				for(int i=0; i<status.length; i++){
					if(lastDisplayedStatus.equals(status[i])){
						//lastDisplayedStatus=lastDisplayedStatus;
						//setErrorMessage(status[i].getMessage());
						return;
					}
				}
				lastDisplayedStatus = null;
			}
			
			// 3.  pick the first problem that is specific to some feature
			IStatus[] status = validationStatus.getChildren();
			for(int s =0; s< status.length; s++){
				if(isSpecificStatus(status[s])){
					lastDisplayedStatus = (FeatureStatus)status[s];
					setErrorMessage(status[s].getMessage());
					return;
				}
			}
				
			// 4.  display the first problem (no problems specify a feature)
			if(status.length>0){
				IStatus singleStatus=status[0];
				setErrorMessage(singleStatus.getMessage());
			}else{
			// 5. not multi or empty multi status
				setErrorMessage(UpdateUI.getString("InstallWizard.ReviewPage.invalid.long")); //$NON-NLS-1$
			}
		}
	}

	class StatusDialog extends ErrorDialog {
//		Button detailsButton;
		public StatusDialog() {
			super(UpdateUI.getActiveWorkbenchShell(), UpdateUI
					.getString("InstallWizard.ReviewPage.invalid.short"), null, //$NON-NLS-1$
					validationStatus, IStatus.OK | IStatus.INFO
							| IStatus.WARNING | IStatus.ERROR);
		}
//		protected Button createButton(
//				Composite parent,
//				int id,
//				String label,
//				boolean defaultButton) {
//			Button b = super.createButton(parent, id, label, defaultButton);
//			if(IDialogConstants.DETAILS_ID == id){
//				detailsButton = b;
//			}
//			return b;
//		}
		public void create() {
			super.create();
			buttonPressed(IDialogConstants.DETAILS_ID);
//			if(detailsButton!=null){
//				detailsButton.dispose();
//			}
		}
	}

	/**
	 * @return true, if completed, false if canceled by the user
	 */
	private boolean downloadIncludedFeatures() {
		try {
			Downloader downloader = new Downloader(jobs);
			getContainer().run(true, true, downloader);
			return !downloader.isCanceled();
		} catch (InvocationTargetException ite) {
		} catch (InterruptedException ie) {
		}
		return true;
	}
	/**
	 * Runnable to resolve included feature references.
	 */
	class Downloader implements IRunnableWithProgress {
		boolean canceled = false;
		/**
		 * List of IInstallFeatureOperation
		 */
		ArrayList operations;
		public Downloader(ArrayList installOperations) {
			operations = installOperations;
		}
		public boolean isCanceled() {
			return canceled;
		}
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			for (int i = 0; i < operations.size(); i++) {
				IInstallFeatureOperation candidate = (IInstallFeatureOperation) operations
						.get(i);
				IFeature feature = candidate.getFeature();
				try {
					IFeatureReference[] irefs = feature
							.getRawIncludedFeatureReferences();
					for (int f = 0; f < irefs.length; f++) {
						if (monitor.isCanceled()) {
							canceled = true;
							return;
						}
						IFeatureReference iref = irefs[f];
						IFeature ifeature = iref.getFeature(monitor);
					}
				} catch (CoreException e) {
				}
			}
			if (monitor.isCanceled()) {
				canceled = true;
			}
		}
	}
}
