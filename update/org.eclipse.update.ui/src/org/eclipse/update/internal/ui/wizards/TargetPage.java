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
import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.operations.*;

public class TargetPage extends BannerPage implements IDynamicPage {
	private TableViewer jobViewer;
	private TableViewer siteViewer;
	private IInstallConfiguration config;
	private ConfigListener configListener;
	private Label requiredSpaceLabel;
	private Label availableSpaceLabel;
	private IInstallFeatureOperation[] jobs;
	private Button addButton;
	private Button deleteButton;
	private HashSet added;
	private JobTargetSites targetSites;

	class JobsContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return jobs;
		}
	}

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {

		/**
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object parent) {
			return config.getConfiguredSites();
		}
	}

	class TableLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
			
		public Image getColumnImage(Object obj, int col) {
			UpdateLabelProvider provider = UpdateUI.getDefault().getLabelProvider();
			if (obj instanceof IConfiguredSite)
				return provider.getLocalSiteImage((IConfiguredSite) obj);
			if (obj instanceof IInstallFeatureOperation) {
				IInstallFeatureOperation job = (IInstallFeatureOperation) obj;
				ImageDescriptor base =
					job.getFeature().isPatch()
						? UpdateUIImages.DESC_EFIX_OBJ
						: UpdateUIImages.DESC_FEATURE_OBJ;
				int flags = 0;
				JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
				if (jobSite == null || jobSite.targetSite == null)
					flags = UpdateLabelProvider.F_ERROR;
				return provider.get(base, flags);
			}
			return null;
		}

		public String getColumnText(Object obj, int col) {
			if (obj instanceof IInstallFeatureOperation && col == 0) {
				IFeature feature = ((IInstallFeatureOperation) obj).getFeature();
				return feature.getLabel()
					+ " " //$NON-NLS-1$
					+ feature.getVersionedIdentifier().getVersion().toString();
			}
			if (obj instanceof IConfiguredSite && col == 0) {
				ISite site = ((IConfiguredSite) obj).getSite();
				return site.getURL().getFile();

			}
			return null;
		}
	}

	class ConfigListener implements IInstallConfigurationChangedListener {
		public void installSiteAdded(IConfiguredSite csite) {
			siteViewer.add(csite);
			if (added == null)
				added = new HashSet();
			added.add(csite);
			siteViewer.setSelection(new StructuredSelection(csite));
			siteViewer.getControl().setFocus();
		}

		public void installSiteRemoved(IConfiguredSite csite) {
			siteViewer.remove(csite);
			if (added != null)
				added.remove(csite);
			IInstallFeatureOperation job = (IInstallFeatureOperation) siteViewer.getInput();
			if (job != null) {
				JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
				IConfiguredSite defaultSite = targetSites.computeTargetSite(jobSite);
				if (defaultSite != null)
					siteViewer.setSelection(new StructuredSelection(defaultSite));
			}
			siteViewer.getControl().setFocus();
		}
	}

	/**
	 * Constructor for ReviewPage2
	 */
	public TargetPage(IInstallConfiguration config) {
		super("Target"); //$NON-NLS-1$
		setTitle(UpdateUI.getString("InstallWizard.TargetPage.title")); //$NON-NLS-1$
		setDescription(UpdateUI.getString("InstallWizard.TargetPage.desc")); //$NON-NLS-1$
		this.config = config;
		UpdateUI.getDefault().getLabelProvider().connect(this);
		configListener = new ConfigListener();
		targetSites = new JobTargetSites(config);
	}

	public void setJobs(IInstallFeatureOperation[] jobs) {
		this.jobs = jobs;
		targetSites.setJobs(jobs);
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		config.removeInstallConfigurationChangedListener(configListener);
		super.dispose();
	}

	public Control createContents(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = layout.marginHeight = 0;
		client.setLayout(layout);

		Label label = new Label(client, SWT.NULL);
		label.setText(UpdateUI.getString("InstallWizard.TargetPage.jobsLabel")); //$NON-NLS-1$

		label = new Label(client, SWT.NULL);
		label.setText(UpdateUI.getString("InstallWizard.TargetPage.siteLabel")); //$NON-NLS-1$

		new Label(client, SWT.NULL);

		createJobViewer(client);
		createSiteViewer(client);

		Composite buttonContainer = new Composite(client, SWT.NULL);
		GridLayout blayout = new GridLayout();
		blayout.marginWidth = blayout.marginHeight = 0;
		buttonContainer.setLayout(blayout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		addButton = new Button(buttonContainer, SWT.PUSH);
		addButton.setText(UpdateUI.getString("InstallWizard.TargetPage.new")); //$NON-NLS-1$
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addTargetLocation();
			}
		});
		addButton.setEnabled(false);
		addButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		SWTUtil.setButtonDimensionHint(addButton);
		
		deleteButton = new Button(buttonContainer, SWT.PUSH);
		deleteButton.setText(UpdateUI.getString("InstallWizard.TargetPage.delete")); //$NON-NLS-1$
		deleteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					removeSelection();
				}
				catch (CoreException ex) {
					UpdateUI.logException(ex);
				}
			}
		});
		deleteButton.setEnabled(false);
		deleteButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		SWTUtil.setButtonDimensionHint(deleteButton);		
		
				
		Composite status = new Composite(client, SWT.NULL);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		status.setLayoutData(gd);
		layout = new GridLayout();
		layout.numColumns = 2;
		status.setLayout(layout);
		label = new Label(status, SWT.NULL);
		label.setText(UpdateUI.getString("InstallWizard.TargetPage.requiredSpace")); //$NON-NLS-1$
		requiredSpaceLabel = new Label(status, SWT.NULL);
		requiredSpaceLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label = new Label(status, SWT.NULL);
		label.setText(UpdateUI.getString("InstallWizard.TargetPage.availableSpace")); //$NON-NLS-1$
		availableSpaceLabel = new Label(status, SWT.NULL);
		availableSpaceLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		WorkbenchHelp.setHelp(client, "org.eclipse.update.ui.MultiTargetPage2"); //$NON-NLS-1$
		
		Dialog.applyDialogFont(parent);
		
		return client;
	}

	private void createJobViewer(Composite parent) {
		jobViewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 150;
		jobViewer.getTable().setLayoutData(gd);
		jobViewer.setContentProvider(new JobsContentProvider());
		jobViewer.setLabelProvider(new TableLabelProvider());

		jobViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleJobsSelected((IStructuredSelection) event.getSelection());
			}
		});
	}

	private void handleJobsSelected(IStructuredSelection selection) {
		IInstallFeatureOperation job = (IInstallFeatureOperation) selection.getFirstElement();
		siteViewer.setInput(job);
		JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
		addButton.setEnabled(jobSite.affinitySite == null);
		if (jobSite.targetSite != null) {
			siteViewer.setSelection(new StructuredSelection(jobSite.targetSite));
		}
	}


	private void createSiteViewer(Composite parent) {
		siteViewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		siteViewer.getTable().setLayoutData(gd);
		siteViewer.setContentProvider(new TableContentProvider());
		siteViewer.setLabelProvider(new TableLabelProvider());
		siteViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object obj) {
				IInstallFeatureOperation job = (IInstallFeatureOperation) siteViewer.getInput();
				JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
				return targetSites.getSiteVisibility((IConfiguredSite) obj, jobSite);
			}
		});
		siteViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ssel = (IStructuredSelection) event.getSelection();
				selectTargetSite(ssel);
				updateDeleteButton(ssel);
			}
		});

		if (config != null)
			config.addInstallConfigurationChangedListener(configListener);
	}
	
	private void updateDeleteButton(IStructuredSelection selection) {
		boolean enable = added != null && !added.isEmpty();
		if (enable) {
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				if (!added.contains(iter.next())) {
					enable = false;
					break;
				}
			}
		}
		deleteButton.setEnabled(enable);
	}

	public void setVisible(boolean visible) {
		if (visible) {
			targetSites.computeDefaultTargetSites();
			jobViewer.setInput(jobs);
		}
		super.setVisible(visible);
		if (visible) {
			jobViewer.getTable().setFocus();
			if (jobs.length > 0)
				jobViewer.setSelection(new StructuredSelection(jobs[0]));
		}
	}

	private void verifyNotEmpty(boolean empty) {
		String errorMessage = null;
		if (empty)
			errorMessage = UpdateUI.getString("InstallWizard.TargetPage.location.empty"); //$NON-NLS-1$
		setErrorMessage(errorMessage);
		setPageComplete(!empty);
	}

	private void selectTargetSite(IStructuredSelection selection) {
		IConfiguredSite site = (IConfiguredSite) selection.getFirstElement();
		IInstallFeatureOperation job = (IInstallFeatureOperation) siteViewer.getInput();
		if (job != null) {
			JobTargetSite jobSite = (JobTargetSite) targetSites.get(job);
			jobSite.targetSite = site;
			pageChanged();
		}
		updateStatus(site);
	}

	private void addTargetLocation() {
		DirectoryDialog dd = new DirectoryDialog(getContainer().getShell());
		dd.setMessage(UpdateUI.getString("InstallWizard.TargetPage.location.message")); //$NON-NLS-1$
		String path = dd.open();
		if (path != null) {
			addConfiguredSite(getContainer().getShell(), config, new File(path), false);
		}
	}
	
	private void removeSelection() throws CoreException {
		IStructuredSelection selection = (IStructuredSelection) siteViewer.getSelection();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			config.removeConfiguredSite((IConfiguredSite) iter.next());
		}
	}

	public static IConfiguredSite addConfiguredSite(
		Shell shell,
		IInstallConfiguration config,
		File file,
		boolean linked) {
		try {
			IConfiguredSite csite = null;
			if (linked) {
				csite = config.createLinkedConfiguredSite(file);
				config.addConfiguredSite(csite);
			} else {
				if (!ensureUnique(file, config)) {
					String title = UpdateUI.getString("InstallWizard.TargetPage.location.error.title"); //$NON-NLS-1$
					String message =
						UpdateUI.getFormattedMessage("InstallWizard.TargetPage.location.exists", file.getPath()); //$NON-NLS-1$
					MessageDialog.openError(shell, title, message);
					return null;
				}
				csite = config.createConfiguredSite(file);
				IStatus status = csite.verifyUpdatableStatus();
				if (status.isOK())
					config.addConfiguredSite(csite);
				else {
					String title = UpdateUI.getString("InstallWizard.TargetPage.location.error.title"); //$NON-NLS-1$
					String message =
						UpdateUI.getFormattedMessage(
							"InstallWizard.TargetPage.location.error.message", //$NON-NLS-1$
							file.getPath());
					String message2 =
						UpdateUI.getFormattedMessage(
							"InstallWizard.TargetPage.location.error.reason", //$NON-NLS-1$
							status.getMessage());
					message += System.getProperty("line.separator") + message2; //$NON-NLS-1$
					ErrorDialog.openError(shell, title, message, status);
					return null;
				}
			}
			return csite;
		} catch (CoreException e) {
			UpdateUI.logException(e);
			return null;
		}
	}

	private void updateStatus(Object element) {
		if (element == null) {
			requiredSpaceLabel.setText(""); //$NON-NLS-1$
			availableSpaceLabel.setText(""); //$NON-NLS-1$
			return;
		}
		IConfiguredSite site = (IConfiguredSite) element;
		File file = new File(site.getSite().getURL().getFile());
		long available = LocalSystemInfo.getFreeSpace(file);
		long required = computeRequiredSizeFor(site);
		if (required == -1)
			requiredSpaceLabel.setText(UpdateUI.getString("InstallWizard.TargetPage.unknownSize")); //$NON-NLS-1$
		else
			requiredSpaceLabel.setText(
				UpdateUI.getFormattedMessage("InstallWizard.TargetPage.size", "" + required)); //$NON-NLS-1$ //$NON-NLS-2$

		if (available == LocalSystemInfo.SIZE_UNKNOWN)
			availableSpaceLabel.setText(UpdateUI.getString("InstallWizard.TargetPage.unknownSize")); //$NON-NLS-1$
		else
			availableSpaceLabel.setText(
				UpdateUI.getFormattedMessage("InstallWizard.TargetPage.size", "" + available)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private long computeRequiredSizeFor(IConfiguredSite site) {
		long totalSize = 0;
		for (int i = 0; i < jobs.length; i++) {
			JobTargetSite jobSite = (JobTargetSite) targetSites.get(jobs[i]);
			if (site.equals(jobSite.targetSite)) {
				long jobSize = site.getSite().getInstallSizeFor(jobs[i].getFeature());
				if (jobSize == -1)
					return -1;
				totalSize += jobSize;
			}
		}
		return totalSize;
	}

	private void pageChanged() {
		boolean empty = false;
		for (Iterator enum = targetSites.keySet().iterator(); enum.hasNext();) {
			JobTargetSite jobSite = (JobTargetSite) targetSites.get(enum.next());
			if (jobSite.targetSite == null) {
				empty = true;
				break;
			}
			IFeature feature = jobSite.job.getFeature();
			if (feature.isPatch()) {
				// Patches must go together with the features
				// they are patching.
				JobTargetSite patchedSite = targetSites.findPatchedFeature(feature);
				if (patchedSite != null
					&& jobSite.targetSite != null
					&& patchedSite.targetSite != null
					&& jobSite.targetSite.equals(patchedSite.targetSite) == false) {
					UpdateUI.getFormattedMessage(
						"IntallWizard.TargetPage.patchError", //$NON-NLS-1$
						new String[] {
							feature.getLabel(),
							patchedSite.job.getFeature().getLabel()});
					setPageComplete(false);
					return;
				}
			}
		}
		verifyNotEmpty(empty);
	}
	
	private static boolean ensureUnique(File file, IInstallConfiguration config) {
		IConfiguredSite[] sites = config.getConfiguredSites();
		URL fileURL;
		try {
			fileURL = new URL("file:" + file.getPath()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			return true;
		}
		for (int i = 0; i < sites.length; i++) {
			URL url = sites[i].getSite().getURL();
			if (UpdateManagerUtils.sameURL(fileURL, url))
				return false;
		}
		return true;
	}
	
	public IConfiguredSite getTargetSite(IInstallFeatureOperation job) {
		return targetSites.getTargetSite(job);
	}
	
	public JobTargetSite[] getTargetSites() {
		JobTargetSite[] sites = new JobTargetSite[jobs.length];
		for (int i = 0; i < jobs.length; i++) {
			JobTargetSite jobSite = (JobTargetSite) targetSites.get(jobs[i]);
			sites[i] = jobSite;
		}
		return sites;
	}
}
