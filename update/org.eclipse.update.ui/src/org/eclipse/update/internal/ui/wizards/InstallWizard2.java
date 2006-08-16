/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.IIncludedFeatureReference;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteFeatureReference;
import org.eclipse.update.core.IVerificationListener;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.core.FeatureDownloadException;
import org.eclipse.update.internal.core.LiteFeature;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.operations.DuplicateConflictsValidator;
import org.eclipse.update.internal.operations.InstallOperation;
import org.eclipse.update.internal.operations.UpdateUtils;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIImages;
import org.eclipse.update.internal.ui.UpdateUIMessages;
import org.eclipse.update.internal.ui.security.JarVerificationService;
import org.eclipse.update.operations.IBatchOperation;
import org.eclipse.update.operations.IFeatureOperation;
import org.eclipse.update.operations.IInstallFeatureOperation;
import org.eclipse.update.operations.IOperation;
import org.eclipse.update.operations.IOperationListener;
import org.eclipse.update.operations.OperationsManager;
import org.eclipse.update.search.UpdateSearchRequest;


public class InstallWizard2
	extends Wizard
	implements IOperationListener, ISearchProvider {
	private ReviewPage reviewPage;
	private LicensePage licensePage;
	private OptionalFeaturesPage optionalFeaturesPage;
	private TargetPage targetPage;
	private IInstallConfiguration config;
	private int installCount = 0;
	private UpdateSearchRequest searchRequest;
	private ArrayList jobs;
	private boolean needsRestart;
	private static boolean isRunning;
	private IBatchOperation installOperation;
	private Job job;
	public static final Object jobFamily = new Object();
	private IJobChangeListener jobListener;
    private boolean isUpdate;

	public InstallWizard2(UpdateSearchRequest searchRequest, IInstallFeatureOperation[] jobs, boolean isUpdate) {
		this (searchRequest, new ArrayList(Arrays.asList(jobs)), isUpdate);
	}

	public InstallWizard2(UpdateSearchRequest searchRequest, ArrayList jobs, boolean isUpdate) {
		this.isUpdate = isUpdate;
        this.searchRequest = searchRequest;
        this.jobs = jobs;
        isRunning = true;
        setDialogSettings(UpdateUI.getDefault().getDialogSettings());
        setDefaultPageImageDescriptor(UpdateUIImages.DESC_UPDATE_WIZ);
        setForcePreviousAndNextButtons(true);
        setNeedsProgressMonitor(true);
        setWindowTitle(UpdateUIMessages.InstallWizard_wtitle); 
	}
    
	public int getInstallCount() {
		return installCount;
	}
	public boolean isRestartNeeded() {
		return installCount > 0 && needsRestart; // or == selectedJobs.length
	}

	public boolean performCancel() {
		isRunning = false;
		if (targetPage != null)
			targetPage.removeAddedSites();
		return super.performCancel();
	}
	/**
	 * @see Wizard#performFinish()
	 */
	public boolean performFinish() {
		IInstallFeatureOperation[] selectedJobs = reviewPage.getSelectedJobs();
		
		// Check for duplication conflicts
		ArrayList conflicts = DuplicateConflictsValidator.computeDuplicateConflicts(selectedJobs, config);
		if (conflicts != null) {
			DuplicateConflictsDialog dialog = new DuplicateConflictsDialog(getShell(), conflicts);
			if (dialog.open() != 0)
				return false;
		}
		
		if (Platform.getJobManager().find(jobFamily).length > 0) {
			// another update/install job is running, need to wait to finish or cancel old job
			boolean proceed = MessageDialog.openQuestion(
					UpdateUI.getActiveWorkbenchShell(),
					UpdateUIMessages.InstallWizard_anotherJobTitle,
					UpdateUIMessages.InstallWizard_anotherJob); 
			if (!proceed)
				return false; // cancel this job, and let the old one go on
		}
		
		// set the install operation
		installOperation = getBatchInstallOperation(selectedJobs);
		if (installOperation != null)
			launchInBackground();
		return true;
	}

	public void addPages() {
		reviewPage = new ReviewPage(isUpdate, searchRequest, jobs);
		addPage(reviewPage);

		try {
//			config = UpdateUtils.createInstallConfiguration();
			config = SiteManager.getLocalSite().getCurrentConfiguration();
		} catch (CoreException e) {
			UpdateUI.logException(e);
		}

		licensePage = new LicensePage(true);
		addPage(licensePage);
		optionalFeaturesPage = new OptionalFeaturesPage(config);
		addPage(optionalFeaturesPage);
		targetPage = new TargetPage(config);
		addPage(targetPage);
	}


	private boolean isPageRequired(IWizardPage page) {
		if (page == null)
			return false;
			
		if (page.equals(licensePage)) {
			return OperationsManager.hasSelectedJobsWithLicenses(
				reviewPage.getSelectedJobs());
		}
		if (page.equals(optionalFeaturesPage)) {
			return OperationsManager.hasSelectedJobsWithOptionalFeatures(
				reviewPage.getSelectedJobs());
		}
		if (page.equals(targetPage)) {
			return reviewPage.getSelectedJobs().length > 0;
		}
		return true;
	}

	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage[] pages = getPages();
		boolean start = false;
		IWizardPage nextPage = null;

		if (page.equals(reviewPage)) {
			updateDynamicPages();
		}

		for (int i = 0; i < pages.length; i++) {
			if (pages[i].equals(page)) {
				start = true;
			} else if (start) {
				if (isPageRequired(pages[i])) {
					nextPage = pages[i];
					break;
				}
			}
		}
		return nextPage;
	}

	private void updateDynamicPages() {
		if (licensePage != null) {
			IInstallFeatureOperation[] licenseJobs =
				OperationsManager.getSelectedJobsWithLicenses(
					reviewPage.getSelectedJobs());
			licensePage.setJobs(licenseJobs);
		}
		if (optionalFeaturesPage != null) {
			IInstallFeatureOperation[] optionalJobs =
				OperationsManager.getSelectedJobsWithOptionalFeatures(
					reviewPage.getSelectedJobs());
			optionalFeaturesPage.setJobs(optionalJobs);
		}
		if (targetPage != null) {
			IInstallFeatureOperation[] installJobs =
				reviewPage.getSelectedJobs();
			targetPage.setJobs(installJobs);
		}
	}

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return page.equals(targetPage) && page.isPageComplete();
	}

	private void preserveOriginatingURLs(
		IFeature feature,
		IFeatureReference[] optionalFeatures) {
		// walk the hierarchy and preserve the originating URL
		// for all the optional features that are not chosen to
		// be installed.
		URL url = feature.getSite().getURL();
		try {
			IIncludedFeatureReference[] irefs =
				feature.getIncludedFeatureReferences();
			for (int i = 0; i < irefs.length; i++) {
				IIncludedFeatureReference iref = irefs[i];
				boolean preserve = false;
				if (iref.isOptional()) {
					boolean onTheList = false;
					for (int j = 0; j < optionalFeatures.length; j++) {
						if (optionalFeatures[j].equals(iref)) {
							//was on the list
							onTheList = true;
							break;
						}
					}
					if (!onTheList)
						preserve = true;
				}
				if (preserve) {
					try {
						String id =
							iref.getVersionedIdentifier().getIdentifier();
						UpdateUI.setOriginatingURL(id, url);
					} catch (CoreException e) {
						// Silently ignore
					}
				} else {
					try {
						IFeature ifeature = iref.getFeature(null);
						preserveOriginatingURLs(ifeature, optionalFeatures);
					} catch (CoreException e) {
						// Silently ignore
					}
				}
			}
		} catch (CoreException e) {
			// Silently ignore
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.operations.IOperationListener#afterExecute(org.eclipse.update.operations.IOperation)
	 */
	public boolean afterExecute(IOperation operation, Object data) {
		if (!(operation instanceof IInstallFeatureOperation))
			return true;
		IInstallFeatureOperation job = (IInstallFeatureOperation) operation;
		IFeature oldFeature = job.getOldFeature();
		if (oldFeature == null && job.getOptionalFeatures() != null)
			preserveOriginatingURLs(
				job.getFeature(),
				job.getOptionalFeatures());

		installCount++;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.operations.IOperationListener#beforeExecute(org.eclipse.update.operations.IOperation)
	 */
	public boolean beforeExecute(IOperation operation, Object data) {
		return true;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.update.internal.ui.wizards.ISearchProvider2#getSearchRequest()
	 */
	public UpdateSearchRequest getSearchRequest() {
		return searchRequest;
	}

	public static synchronized boolean isRunning() {
		return isRunning || Platform.getJobManager().find(jobFamily).length > 0;
	}
	
	private IBatchOperation getBatchInstallOperation(final IInstallFeatureOperation[] selectedJobs) {
		final IVerificationListener verificationListener =new JarVerificationService(
				//InstallWizard.this.getShell());
				UpdateUI.getActiveWorkbenchShell());
		
		// setup jobs with the correct environment
		IInstallFeatureOperation[] operations =	new IInstallFeatureOperation[selectedJobs.length];
		for (int i = 0; i < selectedJobs.length; i++) {
			IInstallFeatureOperation job = selectedJobs[i];
			IFeature[] unconfiguredOptionalFeatures = null;
			IFeatureReference[] optionalFeatures = null;
			if (UpdateUtils.hasOptionalFeatures(job.getFeature())) {
				optionalFeatures = optionalFeaturesPage.getCheckedOptionalFeatures(job);
				unconfiguredOptionalFeatures = optionalFeaturesPage.getUnconfiguredOptionalFeatures(job, job.getTargetSite());
			}
			IInstallFeatureOperation op =
				OperationsManager
					.getOperationFactory()
					.createInstallOperation(
					job.getTargetSite(),
					job.getFeature(),
					optionalFeatures,
					unconfiguredOptionalFeatures,
					verificationListener);
			operations[i] = op;
		}
		return OperationsManager.getOperationFactory().createBatchInstallOperation(operations);
	}



	private void launchInBackground() {
		// Downloads the feature content in the background.
		// The job listener will then install the feature when download is finished.
		
		// TODO: should we cancel existing jobs?
		if (jobListener != null)
			Platform.getJobManager().removeJobChangeListener(jobListener);
		if (job != null)
			Platform.getJobManager().cancel(job);
		jobListener = new UpdateJobChangeListener();
		Platform.getJobManager().addJobChangeListener(jobListener);
		
		job = new Job(UpdateUIMessages.InstallWizard_jobName) { 
			public IStatus run(IProgressMonitor monitor) {
				if (download(monitor))
					return Status.OK_STATUS;
				else {
					isRunning = false;
					return Status.CANCEL_STATUS;
				}
			}
			public boolean belongsTo(Object family) {
				return InstallWizard2.jobFamily == family;
			}
		};

		job.setUser(true);
		job.setPriority(Job.INTERACTIVE);
//		if (wait) {
//			progressService.showInDialog(workbench.getActiveWorkbenchWindow().getShell(), job); 
//		}
		job.schedule();
	}
	
	private boolean install(IProgressMonitor monitor) {
		// Installs the (already downloaded) features and prompts for restart
		try {
			needsRestart = installOperation.execute(monitor, InstallWizard2.this);
			UpdateUI.getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
					UpdateUI.requestRestart(InstallWizard2.this.isRestartNeeded());
				}
			});
		} catch (final InvocationTargetException e) {
			final Throwable targetException = e.getTargetException();
			if (!(targetException instanceof InstallAbortedException)){
				UpdateUI.getStandardDisplay().syncExec(new Runnable() {
					public void run() {
						UpdateUI.logException(targetException);
					}
				});
			}
			return false;
		} catch (CoreException e) {
			return false;
		} finally {
			isRunning = false;
		}
		return true;
	}
	
	private boolean download(final IProgressMonitor monitor) {
		// Downloads the feature content.
		// This method is called from a background job.
		// If download fails, the user is prompted to retry.
		try {
			IFeatureOperation[] ops = installOperation.getOperations();
			monitor.beginTask(UpdateUIMessages.InstallWizard_download, 4 * ops.length);
			for (int i = 0; i < ops.length; i++) {
				IInstallFeatureOperation op = (IInstallFeatureOperation)ops[i];
				
				try {
					monitor.worked(1);
					if (op.getFeature() instanceof LiteFeature) {
						ISiteFeatureReference featureReference = getFeatureReference(op.getFeature());
						IFeature feature = featureReference.getFeature(null);
						if (op instanceof InstallOperation) {
							((InstallOperation)op).setFeature(feature);
						}
					}				
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 3);
					UpdateUtils.downloadFeatureContent(op.getTargetSite(), op.getFeature(), op.getOptionalFeatures(), subMonitor);
				} catch (final CoreException e) {
					if(e instanceof FeatureDownloadException){
						boolean retry = retryDownload((FeatureDownloadException)e);
						if (retry) {
							// redownload current feature
							i--;
							continue;
						}
					} else {
						UpdateCore.log(e);
						if ( !monitor.isCanceled()) {
							Display.getDefault().syncExec( new Runnable () {
								public void run() {
									IStatus status = new Status( IStatus.ERROR, UpdateUI.getPluginId(), IStatus.OK, UpdateUIMessages.InstallWizard2_updateOperationHasFailed, null);
									ErrorDialog.openError(null, null, null, status);
								
								}							
							});
						}
					}
					return false;
				}
			}
			return true;
		} finally {
			monitor.done();
		}
	}
	
	private boolean retryDownload(final FeatureDownloadException e) {

		final boolean retry[] = new boolean[1];
		UpdateUI.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
				retry[0] =	MessageDialog.openQuestion(
					UpdateUI.getActiveWorkbenchShell(),
					UpdateUIMessages.InstallWizard_retryTitle, 
					e.getMessage()+"\n" //$NON-NLS-1$
						+ UpdateUIMessages.InstallWizard_retry); 
			}
		});
		return retry[0];
	}
    

    private class UpdateJobChangeListener extends JobChangeAdapter {
        public void done(final IJobChangeEvent event) {
            // the job listener is triggered when the download job is done, and it proceeds
            // with the actual install
            if (event.getJob() == InstallWizard2.this.job && event.getResult() == Status.OK_STATUS) {
                Platform.getJobManager().removeJobChangeListener(this);
                Platform.getJobManager().cancel(job);
                
                Job installJob = new Job(UpdateUIMessages.InstallWizard_jobName) { 
        			public IStatus run(IProgressMonitor monitor) {
        				//install(monitor);
        				if (install(monitor)) {
        					return Status.OK_STATUS;
        				} else {
        					isRunning = false;
        					return Status.CANCEL_STATUS;
        				}
        			}
        			public boolean belongsTo(Object family) {
        				return InstallWizard2.jobFamily == family;
        			}
        		};

        		installJob.setUser(true);
        		installJob.setPriority(Job.INTERACTIVE);
//        		if (wait) {
//        			progressService.showInDialog(workbench.getActiveWorkbenchWindow().getShell(), job); 
//        		}
        		installJob.schedule();
                
                /*final IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
                UpdateUI.getStandardDisplay().asyncExec(new Runnable() {
                    public void run() {
                        try {
                            progressService.busyCursorWhile( new IRunnableWithProgress() {
                                public void run(final IProgressMonitor monitor){
                                    install(monitor);
                                }
                            });
                        } catch (InvocationTargetException e) {
                            UpdateUI.logException(e);
                        } catch (InterruptedException e) {
                            UpdateUI.logException(e, false);
                        }
                    }
                }); */
            } else if (event.getJob() == InstallWizard2.this.job && event.getResult() != Status.OK_STATUS) {
                isRunning = false;
                Platform.getJobManager().removeJobChangeListener(this);
                Platform.getJobManager().cancel(job);
                UpdateUI.getStandardDisplay().syncExec(new Runnable() {
                    public void run() {
                        UpdateUI.log(event.getResult(), true);
                    }
                });
            }
        }
    }
    
	
	public ISiteFeatureReference getFeatureReference(IFeature feature) {

		ISite site = feature.getSite();
		ISiteFeatureReference[] references = site.getFeatureReferences();
		ISiteFeatureReference currentReference = null;
		for (int i = 0; i < references.length; i++) {
			currentReference = references[i];
			try {
				if (feature.getVersionedIdentifier().equals(currentReference.getVersionedIdentifier()))
					return currentReference;
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		UpdateCore.warn("Feature " + feature + " not found on site" + site.getURL()); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

}
