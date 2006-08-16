/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.IInstallConfigurationChangedListener;
import org.eclipse.update.configuration.LocalSystemInfo;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.ISite;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.operations.UpdateUtils;
import org.eclipse.update.internal.ui.UpdateLabelProvider;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIImages;
import org.eclipse.update.internal.ui.UpdateUIMessages;
import org.eclipse.update.internal.ui.parts.DefaultContentProvider;
import org.eclipse.update.internal.ui.parts.SWTUtil;
import org.eclipse.update.operations.IInstallFeatureOperation;

public class TargetPage extends BannerPage implements IDynamicPage {
	
	private static final int FEATURE_NAME_COLUMN = 0;
	private static final int FEATURE_VERSION_COLUMN = 1;
	private static final int FEATURE_SIZE_COLUMN = 2;
	private static final int INSTALLATION_DIRECTORY_COLUMN = 3;
	
	private float featureNameColumnProcetange = 0.25f;
	private float featureVersionColumnProcetange = 0.25f;
	private float featureSizeColumnProcetange = 0.15f;
	private float installationDirectoryColumnProcetange = 0.35f;
	
	private TableViewer jobViewer;
	private IInstallConfiguration config;
	private ConfigListener configListener;
	private Label requiredSpaceLabel;
	private Label availableSpaceLabel;
	private IInstallFeatureOperation[] jobs;
    //private IInstallFeatureOperation currentJob;
    private Label installLocation;
    private Button changeLocation;
    static HashSet added;

	class JobsContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return jobs;
		}
	}

	class JobsLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
			
		public Image getColumnImage(Object obj, int col) {
			
			if (col > 0) {
				return null;
			}
			
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
			
			IFeature feature = ((IInstallFeatureOperation) obj).getFeature();
			ISite site = ((IInstallFeatureOperation)obj).getTargetSite().getSite();
			if (col == FEATURE_NAME_COLUMN) {				
				return feature.getLabel();
			} else if (col == FEATURE_VERSION_COLUMN) {
				return feature.getVersionedIdentifier().getVersion().toString();
			} else if (col == FEATURE_SIZE_COLUMN) {
				long requiredSpace = site.getDownloadSizeFor(feature) + site.getInstallSizeFor(feature);
				return getSizeString(requiredSpace);
			} else if (col == INSTALLATION_DIRECTORY_COLUMN) {
				return site.getURL().getFile().toString();
			}
			return null;
		}
	}
	
	class JobViewerSorter extends ViewerSorter {
		
		
		private static final int ASCENDING = 0;

		private static final int DESCENDING = 1;

		private int column;

		private int direction;

		public void doSort(int column) {

			if (column == this.column) {
				direction = 1 - direction;
			} else {
				this.column = column;
				direction = ASCENDING;
			}
		}

		/**
		 * Compares the object for sorting
		 */
		public int compare(Viewer viewer, Object o1, Object o2) {

			int rc = 0;
			
			IFeature feature1 = ((IInstallFeatureOperation) o1).getFeature();
			IFeature feature2 = ((IInstallFeatureOperation) o2).getFeature();

			String featureName1 = feature1.getLabel();
			String featureName2 = feature2.getLabel();
			
			String featureId1 = feature1.getVersionedIdentifier().getVersion().toString();
			String featureId2 = feature2.getVersionedIdentifier().getVersion().toString();

			String installationDirectory1 = ((IInstallFeatureOperation)o1).getTargetSite().getSite().getURL().getFile().toString();
			String installationDirectory2 = ((IInstallFeatureOperation)o2).getTargetSite().getSite().getURL().getFile().toString();

			switch (column) {
			case FEATURE_NAME_COLUMN:
				rc = collator.compare(featureName1, featureName2);
				break;
			case FEATURE_VERSION_COLUMN:
				rc = collator.compare(featureId1, featureId2);
				break;
			case INSTALLATION_DIRECTORY_COLUMN:
				rc = collator.compare(installationDirectory1, installationDirectory2);
				break;
			}

			// If descending order, flip the direction
			if (direction == DESCENDING)
				rc = -rc;

			return rc;
		}
	}
	
	class ConfigListener implements IInstallConfigurationChangedListener {
		public void installSiteAdded(IConfiguredSite csite) {
			
			// set the site as target for all jobs without a target
			for (int i=0; jobs != null && i<jobs.length; i++)
				if (jobs[i].getTargetSite() == null && getSiteVisibility(csite, jobs[i])) {
					jobs[i].setTargetSite(csite);
				}

			jobViewer.refresh();
		}

		public void installSiteRemoved(IConfiguredSite csite) {
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
			}
		}
	}

	/**
	 * Constructor for ReviewPage2
	 */
	public TargetPage(IInstallConfiguration config) {
		super("Target"); //$NON-NLS-1$
		setTitle(UpdateUIMessages.InstallWizard_TargetPage_title); 
		setDescription(UpdateUIMessages.InstallWizard_TargetPage_desc); 
		this.config = config;
		UpdateUI.getDefault().getLabelProvider().connect(this);
		configListener = new ConfigListener();
	}

	public void setJobs(IInstallFeatureOperation[] jobs) {
		this.jobs = jobs;
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
//		config.removeInstallConfigurationChangedListener(configListener);
		super.dispose();
	}

	public Control createContents(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = layout.marginHeight = 0;
		client.setLayout(layout);
		client.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(client, SWT.NULL);
		label.setText(UpdateUIMessages.InstallWizard_TargetPage_jobsLabel); 
		createJobViewer(client);

		label = new Label(client, SWT.NULL);
		label.setText(UpdateUIMessages.InstallWizard_TargetPage_location); 
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        label.setLayoutData(gd);

		installLocation = new Label(client, SWT.NULL);
        installLocation.setText("foo"); //$NON-NLS-1$
        gd = new GridData(GridData.FILL_HORIZONTAL);
        installLocation.setLayoutData(gd);
        
        changeLocation = new Button(client, SWT.PUSH);
        changeLocation.setText(UpdateUIMessages.InstallWizard_TargetPage_location_change); 
        changeLocation.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                changeLocationOfFeatures();
            }

			
        });
			
		Composite status = new Composite(client, SWT.NULL);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 3;
		status.setLayoutData(gd);
		layout = new GridLayout();
		layout.numColumns = 2;
		status.setLayout(layout);
		label = new Label(status, SWT.NULL);
		label.setText(UpdateUIMessages.InstallWizard_TargetPage_requiredSpace); 
		requiredSpaceLabel = new Label(status, SWT.NULL);
		requiredSpaceLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label = new Label(status, SWT.NULL);
		label.setText(UpdateUIMessages.InstallWizard_TargetPage_availableSpace); 
		availableSpaceLabel = new Label(status, SWT.NULL);
		availableSpaceLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        PlatformUI.getWorkbench().getHelpSystem().setHelp(client, "org.eclipse.update.ui.MultiTargetPage2"); //$NON-NLS-1$
		
		Dialog.applyDialogFont(parent);
		
		return client;
	}

	private void createJobViewer(Composite parent) {
		
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.RESIZE | SWT.FULL_SELECTION);
		
		TableColumn featureNameColumn = new TableColumn(table, SWT.LEFT, FEATURE_NAME_COLUMN);
		featureNameColumn.setText(UpdateUIMessages.TargetPage_FeatureNameColumn); 
		featureNameColumn.setWidth(75);
		
		TableColumn featureVersionColumn = new TableColumn(table, SWT.LEFT, FEATURE_VERSION_COLUMN);
		featureVersionColumn.setText(UpdateUIMessages.TargetPage_Feature_Version); 
		featureVersionColumn.setWidth(75);
		
		TableColumn featureSizeColumn = new TableColumn(table, SWT.LEFT, FEATURE_SIZE_COLUMN);
		featureSizeColumn.setText(UpdateUIMessages.TargetPage_Feature_Size); 
		featureSizeColumn.setWidth(75);
		
		TableColumn featureLocationColumn = new TableColumn(table, SWT.LEFT, INSTALLATION_DIRECTORY_COLUMN);
		featureLocationColumn.setText(UpdateUIMessages.TargetPage_InstallationDirectoryColumn); 
		featureLocationColumn.setWidth(75);
		jobViewer = new TableViewer(table);
		
		//jobViewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
		gd.widthHint = 150;
		jobViewer.getTable().setLayoutData(gd);
		jobViewer.setContentProvider(new JobsContentProvider());
		jobViewer.setLabelProvider(new JobsLabelProvider());
		jobViewer.setSorter(new JobViewerSorter());
		
		jobViewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent event) {
					changeLocationOfFeatures();				
				}
			}
		);
		
		table.addListener(SWT.MenuDetect, new Listener() {
		      public void handleEvent(Event event) {
		    	  Menu menu = new Menu (getShell(), SWT.POP_UP);
					MenuItem item = new MenuItem (menu, SWT.PUSH);
					item.setText(UpdateUIMessages.InstallWizard_TargetPage_location_change);
					item.addListener(SWT.Selection, new Listener () {
							public void handleEvent (Event e) {
								changeLocationOfFeatures();
							}
						}
					);
					menu.setLocation (event.x, event.y);
					menu.setVisible (true);
		      }
			}
		);
		featureNameColumn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					((JobViewerSorter) jobViewer.getSorter()).doSort(FEATURE_NAME_COLUMN);
					jobViewer.refresh();
				}
		    }
		);
		
		featureVersionColumn.addSelectionListener(new SelectionAdapter() {
		      	public void widgetSelected(SelectionEvent event) {
		      		((JobViewerSorter) jobViewer.getSorter()).doSort(FEATURE_VERSION_COLUMN);
		      		jobViewer.refresh();
		      	}
		    }
		);
		
		featureSizeColumn.addSelectionListener(new SelectionAdapter() {
		      	public void widgetSelected(SelectionEvent event) {
		      		((JobViewerSorter) jobViewer.getSorter()).doSort(FEATURE_SIZE_COLUMN);
		      		jobViewer.refresh();
		      	}
		    }
		);
		
		featureLocationColumn.addSelectionListener(new SelectionAdapter() {
		      	public void widgetSelected(SelectionEvent event) {
		      		((JobViewerSorter) jobViewer.getSorter()).doSort(INSTALLATION_DIRECTORY_COLUMN);
		      		jobViewer.refresh();
		      	}
		    }
		);
		
		table.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {


				Rectangle area = table.getClientArea();
				Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);

				int tableWidth = area.width - 3 * table.getBorderWidth();
				if (preferredSize.y > area.height + table.getHeaderHeight()){
					Point vBarSize = table.getVerticalBar().getSize();
					tableWidth -= vBarSize.x;
				}		
				
				TableColumn featureNameColumn = table.getColumn(FEATURE_NAME_COLUMN);
				TableColumn featureVersionColumn = table.getColumn(FEATURE_VERSION_COLUMN);
				TableColumn featureSizeColumn = table.getColumn(FEATURE_SIZE_COLUMN);
				TableColumn featureLocationColumn = table.getColumn(INSTALLATION_DIRECTORY_COLUMN);

				featureNameColumn.setWidth((int)(tableWidth * featureNameColumnProcetange));
				featureVersionColumn.setWidth((int)(tableWidth * featureVersionColumnProcetange));
				featureSizeColumn.setWidth((int)(tableWidth * featureSizeColumnProcetange));
				featureLocationColumn.setWidth(tableWidth - featureNameColumn.getWidth() - featureVersionColumn.getWidth() - featureSizeColumn.getWidth());		    	

				
				featureNameColumnProcetange = featureNameColumn.getWidth() / (float)tableWidth;
				featureVersionColumnProcetange = featureVersionColumn.getWidth() / (float)tableWidth;
				featureSizeColumnProcetange = featureSizeColumn.getWidth() / (float)tableWidth;
				installationDirectoryColumnProcetange = featureLocationColumn.getWidth() / (float)tableWidth;
			}
		});


		jobViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateStatus();

			}
		});
		
		table.setHeaderVisible(true);
	    table.setLinesVisible(true);
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
			errorMessage = UpdateUIMessages.InstallWizard_TargetPage_location_empty; 
		setErrorMessage(errorMessage);
		setPageComplete(!empty);
	}


	private void updateStatus() {
		
		IStructuredSelection selectedJobs = (IStructuredSelection)jobViewer.getSelection();
		IInstallFeatureOperation selectedJob = (IInstallFeatureOperation)selectedJobs.getFirstElement();
	
		requiredSpaceLabel.setText(""); //$NON-NLS-1$
		availableSpaceLabel.setText(""); //$NON-NLS-1$
		installLocation.setText(""); //$NON-NLS-1$
		
		if (selectedJob == null) {
			return;
		}
		
		IConfiguredSite site = selectedJob.getTargetSite(); /*(IConfiguredSite) element*/;
		File file = new File(site.getSite().getURL().getFile());
		long available = -1;
		long required = -1;
		if (areAllTargetSitesSame()) {
			available = LocalSystemInfo.getFreeSpace(file);
			required = computeRequiredSizeFor(site);
			//add the download size to space required to do operation since all plugins and nonplugin data will be downloaded first
			required += computeDownloadSizeFor(site); 
			installLocation.setText(new File(selectedJob.getTargetSite().getSite().getURL().getFile()).toString());
		}		
		
		if (available == LocalSystemInfo.SIZE_UNKNOWN)
			available = -1;
		
		requiredSpaceLabel.setText(getSizeString(required));
		availableSpaceLabel.setText(getSizeString(available));
		
		
		//if the available space was retireved from the OS and the required space is greater that the available space, do not let the user continue
		if(available != LocalSystemInfo.SIZE_UNKNOWN && required > available){
			this.setPageComplete(false);
			//TODO: set error message: "...not enough space..."
		}else
			this.setPageComplete(true);
	}
	
	private boolean areAllTargetSitesSame() {
		IStructuredSelection selectedJobs = (IStructuredSelection)jobViewer.getSelection();
		Iterator iterator = selectedJobs.iterator();
		URL site = null;
		if (iterator != null) {
			while(iterator.hasNext()) {
				IInstallFeatureOperation current = (IInstallFeatureOperation) iterator.next();				
				if (site != null) {
					if (!site.equals(current.getTargetSite().getSite().getURL())) {
						return false;
					}
				} else {
					site = current.getTargetSite().getSite().getURL();
				}
			}
			return true;
		}
		return false;
	}

	private String getSizeString(long size) {
		if (size <= 0) {
			return UpdateUIMessages.InstallWizard_TargetPage_unknownSize;
		} else {
			double order = 1024.0;
			double sizeInMB = size / order;
			double sizeInGB = size / order / order;
			if ( sizeInMB < 1) {
				return NLS.bind(UpdateUIMessages.InstallWizard_TargetPage_size_KB, "" + size); //$NON-NLS-1$
			} else {
				String pattern = "#.##"; //$NON-NLS-1$
				DecimalFormat formatter = new DecimalFormat(pattern);
				if (sizeInGB < 1) {					
					return NLS.bind(UpdateUIMessages.InstallWizard_TargetPage_size_MB, formatter.format(sizeInMB));
				} else {
					return NLS.bind(UpdateUIMessages.InstallWizard_TargetPage_size_GB, formatter.format(sizeInGB));
				}
			}
		}
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
	
	private long computeDownloadSizeFor(IConfiguredSite site) {
		long totalSize = 0;
		for (int i = 0; i < jobs.length; i++) {
			if (site.equals(jobs[i].getTargetSite())) {
				long jobSize = site.getSite().getDownloadSizeFor(jobs[i].getFeature());
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
					String msg = NLS.bind(UpdateUIMessages.InstallWizard_TargetPage_patchError, (new String[] {
                    feature.getLabel(),
                    patchedFeatureJob.getFeature().getLabel()}));
					setErrorMessage(msg);
					setPageComplete(false);
					return;
				}
				// Check installed features
				IFeature patchedFeature = UpdateUtils.getPatchedFeature(feature);
				if (patchedFeature != null  
					&& !jobs[i].getTargetSite().equals(patchedFeature.getSite().getCurrentConfiguredSite())) {
					String msg = NLS.bind(UpdateUIMessages.InstallWizard_TargetPage_patchError2, (new String[] {
                    feature.getLabel(),
                    patchedFeature.getLabel(),
                    patchedFeature.getSite().getCurrentConfiguredSite().getSite().getURL().getFile()}));
					setErrorMessage(msg);
					setPageComplete(false);
					return;
				}
			}
		}
		verifyNotEmpty(empty);
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
		
		// Co-locate updates with the old feature
		if (job.getOldFeature() != null) {
			IConfiguredSite oldSite = UpdateUtils.getSiteWithFeature(config, job.getOldFeature().getVersionedIdentifier().getIdentifier());
			return (site == oldSite);
		}

		// Allow installing into any site that is updateable and there is no affinity specified
		return true;
	}
	
	private void initializeDefaultTargetSites() {
		
		
		IConfiguredSite mostReceantlyUsedSite = getMostReceantlyUsedSite();
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
			
			if (mostReceantlyUsedSite != null) {
				jobs[i].setTargetSite(mostReceantlyUsedSite);
				continue;
			}

			jobs[i].setTargetSite(getFirstTargetSite(jobs[i]));

		}

	}
	
	private IConfiguredSite getMostReceantlyUsedSite() {
		IDialogSettings master = UpdateUI.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection(TargetSiteDialog.MOST_RECEANTLY_USED_SITE_URL);
		if (section == null) {
			return null;
		}
		String mostReceantlyUsedSiteURLString = section.get(TargetSiteDialog.MOST_RECEANTLY_USED_SITE_URL); 
		if (mostReceantlyUsedSiteURLString == null) {
			return null;
		}
		
		URL mostReceantlyUsedSiteURL = null;
		try {
			mostReceantlyUsedSiteURL = new URL(mostReceantlyUsedSiteURLString);
		} catch (MalformedURLException mue) {
			UpdateCore.log("Url format is wrong for the mostReceantlyUsedSiteURL in preferences", mue); //$NON-NLS-1$
			mue.printStackTrace();
			return null;
		}
		IConfiguredSite[] sites = config.getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			IConfiguredSite configuredSite = sites[i];
			if (mostReceantlyUsedSiteURL.equals(configuredSite.getSite().getURL())) {
				return configuredSite;
			}
		}
		return null;
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
  
    
    void removeAddedSites() {
        if (added != null) {
        	Iterator it = added.iterator(); 
            while (it.hasNext()) {
                 config.removeConfiguredSite((IConfiguredSite) it.next());
                // the config listener no longer removes the site so we need to cleanup the collection at the end
            }
            added.clear();
        }           
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizardPage#isPageComplete()
     */
    public boolean isPageComplete() {
        // check if all jobs have a target site set
        for (int i = 0; jobs != null && i < jobs.length; i++) {
            if (jobs[i].getTargetSite() == null)
                return false;
        }
        return super.isPageComplete();
    }

    /**
     * @param job
     */
/*    private void setTargetLocation(IInstallFeatureOperation job) {
        if (job != null && job.getTargetSite() != null) {
            installLocation.setText(new File(job.getTargetSite().getSite().getURL().getFile()).toString());
            updateStatus(job.getTargetSite());
        }
    }*/
    
    private IInstallFeatureOperation[] toJobArray(Iterator selectedJobs) {
		
    	if (selectedJobs == null) 
    		return new IInstallFeatureOperation[0];
    	
    	ArrayList result = new ArrayList();
    	
    	while (selectedJobs.hasNext()) {
    		result.add(selectedJobs.next());
    	}
    	
    	return (IInstallFeatureOperation[])result.toArray(new IInstallFeatureOperation[result.size()]);
	}

	private void changeLocationOfFeatures() {
		IStructuredSelection selection = (IStructuredSelection) jobViewer.getSelection();
		if (selection == null)
		    return;
		Iterator selectedJob = selection.iterator();
		if (selectedJob == null) 
		    return;
		
		TargetSiteDialog dialog = new TargetSiteDialog(getShell(), config, toJobArray(selection.iterator()), configListener);
		dialog.create();

		SWTUtil.setDialogSize(dialog, 400, 300);
		
		dialog.getShell().setText(UpdateUIMessages.SitePage_new); 
		if ( dialog.open() == Dialog.OK) {
			pageChanged();
			jobViewer.refresh();
			updateStatus();
		}
	}
}
