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

	class JobsContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return jobs;
		}
	}

	class SitesContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {

		public Object[] getElements(Object parent) {
			return config.getConfiguredSites();
		}
	}

	class JobsLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
			
		public Image getColumnImage(Object obj, int col) {
			UpdateLabelProvider provider = UpdateUI.getDefault().getLabelProvider();

			IInstallFeatureOperation job = (IInstallFeatureOperation) obj;
			ImageDescriptor base =
				job.getFeature().isPatch()
					? UpdateUIImages.DESC_EFIX_OBJ
					: UpdateUIImages.DESC_FEATURE_OBJ;
			int flags = 0;
			if (job.getTargetSite() == null)
				flags = UpdateLabelProvider.F_ERROR;
			return provider.get(base, flags);
		}

		public String getColumnText(Object obj, int col) {
			if (col == 0) {
				IFeature feature = ((IInstallFeatureOperation) obj).getFeature();
				return feature.getLabel()
					+ " " //$NON-NLS-1$
					+ feature.getVersionedIdentifier().getVersion().toString();
			}
			return null;
		}
	}
	
	class SitesLabelProvider
	extends LabelProvider
	implements ITableLabelProvider {
		
		public Image getColumnImage(Object obj, int col) {
			UpdateLabelProvider provider = UpdateUI.getDefault().getLabelProvider();
			return provider.getLocalSiteImage((IConfiguredSite) obj);
		}
	
		public String getColumnText(Object obj, int col) {
			if (col == 0) {
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
			
			// set the site as target for all jobs without a target
			for (int i=0; jobs != null && i<jobs.length; i++)
				if (jobs[i].getTargetSite() == null && getSiteVisibility(csite, jobs[i])) {
					jobs[i].setTargetSite(csite);
				}

			jobViewer.refresh();
			
			siteViewer.setSelection(new StructuredSelection(csite));
			siteViewer.getControl().setFocus();
		}

		public void installSiteRemoved(IConfiguredSite csite) {
			siteViewer.remove(csite);
			if (added != null)
				added.remove(csite);
			
			// remove the target site for all jobs that use it
			// set the site as target for all jobs without a target
			boolean refreshJobs = false;
			for (int i=0; jobs != null && i<jobs.length; i++)
				if (jobs[i].getTargetSite() == csite) {
					jobs[i].setTargetSite(null);
					refreshJobs = true;
				}
				
			pageChanged();
			
			jobViewer.refresh();
			if (refreshJobs) {
				jobViewer.getControl().setFocus();
			} else
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
	}

	public void setJobs(IInstallFeatureOperation[] jobs) {
		this.jobs = jobs;
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
		jobViewer.setLabelProvider(new JobsLabelProvider());

		jobViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				IInstallFeatureOperation job = (IInstallFeatureOperation) selection.getFirstElement();
				if (job != null) {
					siteViewer.setInput(job);
					IConfiguredSite affinitySite = UpdateUtils.getAffinitySite(config, job.getFeature());
					addButton.setEnabled(affinitySite == null);
					if (job.getTargetSite() != null) {
						siteViewer.setSelection(new StructuredSelection(job.getTargetSite()));
					}
				}
			}
		});
	}

	private void createSiteViewer(Composite parent) {
		siteViewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		siteViewer.getTable().setLayoutData(gd);
		siteViewer.setContentProvider(new SitesContentProvider());
		siteViewer.setLabelProvider(new SitesLabelProvider());
		siteViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object obj) {
				IInstallFeatureOperation job = (IInstallFeatureOperation) siteViewer.getInput();
				return getSiteVisibility((IConfiguredSite) obj, job);
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
			initializeDefaultTargetSites();
			jobViewer.setInput(jobs);
			if (jobViewer.getSelection().isEmpty() && jobs.length > 0)
				jobViewer.setSelection(new StructuredSelection(jobs[0]));
		}
		
		super.setVisible(visible);
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
			if (site != null)
				job.setTargetSite(site);
			pageChanged();
		}
		updateStatus(site);
	}

	private void addTargetLocation() {
		DirectoryDialog dd = new DirectoryDialog(getContainer().getShell());
		dd.setMessage(UpdateUI.getString("InstallWizard.TargetPage.location.message")); //$NON-NLS-1$
		String path = dd.open();
		if (path != null) {
			addConfiguredSite(getContainer().getShell(), config, new File(path));
		}
	}
	
	private void removeSelection() throws CoreException {
		IStructuredSelection selection = (IStructuredSelection) siteViewer.getSelection();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			IConfiguredSite targetSite = (IConfiguredSite) iter.next();
			config.removeConfiguredSite(targetSite);
		}
	}

	private IConfiguredSite addConfiguredSite(
		Shell shell,
		IInstallConfiguration config,
		File file) {
		try {
			IConfiguredSite csite = config.createConfiguredSite(file);
			IStatus status = csite.verifyUpdatableStatus();
			if (status.isOK())
				config.addConfiguredSite(csite);
			else 
				throw new CoreException(status);
			
			return csite;
		} catch (CoreException e) {
			String title = UpdateUI.getString("InstallWizard.TargetPage.location.error.title"); //$NON-NLS-1$
			ErrorDialog.openError(shell, title, null, e.getStatus());
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
			if (site.equals(jobs[i].getTargetSite())) {
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
		for (int i=0; jobs!=null && i<jobs.length; i++) {
			if (jobs[i].getTargetSite() == null) {
				empty = true;
				break;
			}
		
			IFeature feature = jobs[i].getFeature();
			if (feature.isPatch()) {
				// Patches must go together with the features
				// they are patching.
				
				// Check current jobs
				IInstallFeatureOperation patchedFeatureJob = findPatchedFeature(feature);
				if (patchedFeatureJob != null
					&& patchedFeatureJob.getTargetSite() != null
					&& !jobs[i].getTargetSite().equals(patchedFeatureJob.getTargetSite())) {
					String msg = UpdateUI.getFormattedMessage(
						"InstallWizard.TargetPage.patchError", //$NON-NLS-1$
						new String[] {
							feature.getLabel(),
							patchedFeatureJob.getFeature().getLabel()});
					setErrorMessage(msg);
					setPageComplete(false);
					return;
				}
				// Check insalled features
				IFeature patchedFeature = UpdateUtils.getPatchedFeature(feature);
				if (patchedFeature != null  
					&& !jobs[i].getTargetSite().equals(feature.getSite().getCurrentConfiguredSite())) {
					String msg = UpdateUI.getFormattedMessage(
							"InstallWizard.TargetPage.patchError", //$NON-NLS-1$
							new String[] {
								feature.getLabel(),
								patchedFeature.getLabel()});
					setErrorMessage(msg);
					setPageComplete(false);
					return;
				}
			}
		}
		verifyNotEmpty(empty);
	}
	
	void removeAddedSites() {
		if (added != null) {
			while (!added.isEmpty()) {
				Iterator it = added.iterator(); 
				if (it.hasNext())
					config.removeConfiguredSite((IConfiguredSite) it.next());
			}
		}
			
	}
	
	private  boolean getSiteVisibility(IConfiguredSite site, IInstallFeatureOperation job) {
		// Do not allow installing into a non-updateable site
		if (!site.isUpdatable())
			return false;
		
		// If affinity site is known, only it should be shown
		IConfiguredSite affinitySite = UpdateUtils.getAffinitySite(config, job.getFeature());
		if (affinitySite != null) {
			// Must compare referenced sites because
			// configured sites themselves may come from 
			// different configurations
			return site.getSite().equals(affinitySite.getSite());
		}

		// Allow installing into any site that is updateable and there is no affinity specified
		return true;
	}
	
	private void initializeDefaultTargetSites() {
		for (int i = 0; i < jobs.length; i++) {
			if (jobs[i].getTargetSite() != null)
				continue;
			
			IConfiguredSite affinitySite =	UpdateUtils.getAffinitySite(config, jobs[i].getFeature());
			if (affinitySite != null) {
				jobs[i].setTargetSite(affinitySite);
				continue;
			}

			IConfiguredSite defaultSite = UpdateUtils.getDefaultTargetSite(config, jobs[i], false);
			if (defaultSite != null) {
				jobs[i].setTargetSite(defaultSite);
				continue;
			}

			jobs[i].setTargetSite(getFirstTargetSite(jobs[i]));

		}
	}
	

	private IConfiguredSite getFirstTargetSite(IInstallFeatureOperation job) {
		IConfiguredSite[] sites = config.getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			IConfiguredSite csite = sites[i];
			if (getSiteVisibility(csite, job)) 
				return csite;
		}
		return null;
	}
	
	public IInstallFeatureOperation findPatchedFeature(IFeature patch) {
		for (int i=0; i<jobs.length; i++) {
			IFeature target = jobs[i].getFeature();
			if (!target.equals(patch) && UpdateUtils.isPatch(target, patch))
				return jobs[i];
		}
		return null;
	}
}
