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
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

public class ReviewPage	extends BannerPage {

	private Label label;
	private ArrayList jobs;
	private Label counterLabel;
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
	private Button filterOlderVersionCheck;
	private ContainmentFilter filter = new ContainmentFilter();
	private LatestVersionFilter olderVersionFilter = new LatestVersionFilter();
	private UpdateSearchRequest searchRequest;
	private int LABEL_ORDER = 1;
	private int VERSION_ORDER = 1;
	private int PROVIDER_ORDER = 1;
    private ContainerCheckedTreeViewer treeViewer;
    private boolean initialized;
    private boolean isUpdateSearch;
    
    class TreeContentProvider extends DefaultContentProvider implements
            ITreeContentProvider {

        public Object[] getElements(Object parent) {
            return getSites();
        }

        public Object[] getChildren(final Object parent) {
            if (parent instanceof SiteBookmark) {
                SiteBookmark bookmark = (SiteBookmark) parent;
                bookmark.getSite(null); // triggers catalog creation
                Object[] children = bookmark.getCatalog(true,null);
                ArrayList nonEmptyCategories = new ArrayList(children.length);
                for (int i=0; i<children.length; i++)
                    if (hasChildren(children[i]))
                        nonEmptyCategories.add(children[i]);
                return nonEmptyCategories.toArray();
            } else if (parent instanceof SiteCategory) {
                SiteCategory category = (SiteCategory)parent;
                //return category.getChildren();
                Object[] children = category.getChildren();
                ArrayList list = new ArrayList(children.length);
                for (int i=0; i<children.length; i++) {
                    if (children[i] instanceof FeatureReferenceAdapter) {
                        try {
                            IInstallFeatureOperation job = findJob((FeatureReferenceAdapter)children[i]);
                            if (job != null)
                                list.add(job);
                        } catch (CoreException e) {
                            UpdateCore.log(e.getStatus());
                        }
                    }
                }
                return list.toArray();
            }
            return new Object[0];
        }

        public Object getParent(Object element) {
            if (element instanceof SiteCategory)
                return ((SiteCategory) element).getBookmark();
            if (element instanceof IInstallFeatureOperation) {
                IFeature f = ((IInstallFeatureOperation)element).getFeature();
                ISiteFeatureReference fr = f.getSite().getFeatureReference(f);
                ICategory[] categories = fr.getCategories();
//                if (categories != null && categories.length > 0)
//                    return categories[0];
                SiteBookmark[] sites = (SiteBookmark[])((ITreeContentProvider)treeViewer.getContentProvider()).getElements(null);
                for (int i=0; i<sites.length; i++) {
                    if (sites[i].getSite(false, null) != f.getSite())
                        continue;
                    Object[] children = sites[i].getCatalog(true, null);
                    for (int j = 0; j<children.length; j++) {
                        if (!(children[j] instanceof SiteCategory))
                            continue;
                        for (int c=0; c < categories.length; c++)
                            if (categories[c].getName().equals(((SiteCategory)children[j]).getName()))
                                return children[j];
                    }
                }
            }

            return null;
        }

        public boolean hasChildren(Object element) {
            return (element instanceof SiteBookmark || (
                    element instanceof SiteCategory && getChildren(element).length > 0));
        }

        private SiteBookmark[] getSites() {
            if (searchRequest == null)
                return new SiteBookmark[0];
            else if (searchRequest.getScope().getSearchSites() == null ||
                searchRequest.getScope().getSearchSites().length == 0) {
                // this is an update search, so see if there are any jobs first,
                // and get their sites
                if (jobs != null) {
                    ArrayList sitesList = new ArrayList(jobs.size());
                    for (int i = 0; i < jobs.size(); i++) {
                        IInstallFeatureOperation op = (IInstallFeatureOperation) jobs
                                .get(i);
                        // we need a label for the site, so try to get it from the old
                        // feature update url
                        String label = null;
                        IFeature[] existingFeatures = UpdateUtils
                                .getInstalledFeatures(op.getFeature(), true);
                        if (existingFeatures != null
                                && existingFeatures.length > 0) {
                            IURLEntry entry = op.getFeature()
                                    .getUpdateSiteEntry();
                            label = entry.getAnnotation();
                        }
                        if (label == null)
                            label = op.getFeature().getSite().getURL().toExternalForm();
                                    
                        SiteBookmark bookmark = new SiteBookmark(label,
                                op.getFeature().getSite().getURL(), false);
                        if (sitesList.contains(bookmark))
                            continue;
                        else
                            sitesList.add(bookmark);
                        
                    }
                    if (!sitesList.isEmpty())
                        return (SiteBookmark[]) sitesList
                                .toArray(new SiteBookmark[sitesList.size()]);
                }
                return new SiteBookmark[0];
            } else {
                // search for features
                IUpdateSearchSite[] sites = searchRequest.getScope().getSearchSites();
                SiteBookmark[] siteBookmarks = new SiteBookmark[sites.length];
                for (int i = 0; i < sites.length; i++)
                    siteBookmarks[i] = new SiteBookmark(sites[i].getLabel(),
                            sites[i].getURL(), false);
                return siteBookmarks;
            }
        }
    }

    class TreeLabelProvider extends SharedLabelProvider {

        public Image getImage(Object obj) {
            if (obj instanceof SiteBookmark)
                return UpdateUI.getDefault().getLabelProvider().get(
                        UpdateUIImages.DESC_SITE_OBJ);
            if (obj instanceof SiteCategory)
                return UpdateUI.getDefault().getLabelProvider().get(
                        UpdateUIImages.DESC_CATEGORY_OBJ);
            if (obj instanceof IInstallFeatureOperation) {
                IFeature feature = ((IInstallFeatureOperation) obj).getFeature();
                boolean patch = feature.isPatch();
                
                boolean problematic=problematicFeatures.contains(feature);
                
                if (patch) {
                    return get(UpdateUIImages.DESC_EFIX_OBJ, problematic? F_ERROR : 0);
                } else {
                    return get(UpdateUIImages.DESC_FEATURE_OBJ, problematic? F_ERROR : 0);
                }
            }
            return super.getImage(obj);

        }

        public String getText(Object obj) {
            if (obj instanceof SiteBookmark) 
                return ((SiteBookmark) obj).getLabel();
            if (obj instanceof SiteCategory)
                return ((SiteCategory)obj).getName();
            if (obj instanceof IInstallFeatureOperation) {
                IInstallFeatureOperation job = (IInstallFeatureOperation) obj;
                IFeature feature = job.getFeature();
                return feature.getLabel() + " " + feature
                            .getVersionedIdentifier()
                            .getVersion()
                            .toString();
            }
            return super.getText(obj);
        }
    }

    class ModelListener implements IUpdateModelChangedListener {
        public void objectChanged(Object object, String property) {
            treeViewer.refresh();
            checkItems();
        }

        public void objectsAdded(Object parent, Object[] children) {
            treeViewer.refresh();
            checkItems();
        }

        public void objectsRemoved(Object parent, Object[] children) {
            treeViewer.refresh();
            checkItems();
        }
        
        private void checkItems() {
            TreeItem[] items = treeViewer.getTree().getItems();
            for (int i = 0; i < items.length; i++) {
                SiteBookmark bookmark = (SiteBookmark) items[i].getData();
                treeViewer.setChecked(bookmark, bookmark.isSelected());
                String[] ignoredCats = bookmark.getIgnoredCategories();
                treeViewer.setGrayed(bookmark, ignoredCats.length > 0
                        && bookmark.isSelected());
            }
        }
    }

	class ContainmentFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object child) {
            if (child instanceof IInstallFeatureOperation)
                return !isContained((IInstallFeatureOperation) child);
            else
                return true;
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
					throw Utilities.newCoreException(NLS.bind("InstallWizard.ReviewPage.cycle", feature.getVersionedIdentifier().toString()), null); //$NON-NLS-1$
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

	class LatestVersionFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object child) {
            if (child instanceof IInstallFeatureOperation)
                return isLatestVersion((IInstallFeatureOperation) child);
            else
                return true;
		}
		private boolean isLatestVersion(IInstallFeatureOperation job) {
			IFeature feature = job.getFeature();
			for (int i = 0; i < jobs.size(); i++) {
				IInstallFeatureOperation candidateJob = (IInstallFeatureOperation) jobs.get(i);
				if (candidateJob.equals(job))
					continue;
				IFeature candidate = candidateJob.getFeature();
				if (feature.getSite() != job.getFeature().getSite())
					continue;
				if (!feature.getVersionedIdentifier().getIdentifier().equals(candidate.getVersionedIdentifier().getIdentifier()))
					continue;
				if (!feature.getVersionedIdentifier().getVersion().isGreaterThan(candidate.getVersionedIdentifier().getVersion()))
					return false;
			}
			return true;
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
	public ReviewPage(boolean isUpdateSearch, UpdateSearchRequest searchRequest, ArrayList jobs) {
		super("Review"); //$NON-NLS-1$
        this.isUpdateSearch = isUpdateSearch;
        this.jobs = jobs;
        if (this.jobs==null) this.jobs = new ArrayList();
        this.searchRequest = searchRequest;
        
		setTitle(UpdateUIMessages.InstallWizard_ReviewPage_title); 
		setDescription(UpdateUIMessages.InstallWizard_ReviewPage_desc); 
		UpdateUI.getDefault().getLabelProvider().connect(this);
		setBannerVisible(false);
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
		String filterFeatures = UpdateUIMessages.InstallWizard_ReviewPage_filterFeatures; 
		String filterPatches = UpdateUIMessages.InstallWizard_ReviewPage_filterPatches; 

		if (isUpdateSearch && filterText.equals(filterFeatures))
			filterCheck.setText(filterPatches);
		else if ( !isUpdateSearch && filterText.equals(filterPatches))
			filterCheck.setText(filterFeatures);
		
		if (visible && !initialized) {
            initialized = true;
//			jobs.clear();

//			setDescription(UpdateUI.getString("InstallWizard.ReviewPage.searching")); //$NON-NLS-1$;
//			label.setText(UpdateUI.getString("")); //$NON-NLS-1$

			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
//					searchRunner.runSearch();
					performPostSearchProcessing();
				}
			});
		}
	}

	private void performPostSearchProcessing() {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				if (treeViewer != null) {
//                    treeViewer.refresh();
//                    treeViewer.getTree().layout(true);
					if (isUpdateSearch) {
						selectTrueUpdates();
					}
				}
				pageChanged();
				
				int totalCount = jobs != null ? jobs.size(): 0;
				if(totalCount >0) {
					setDescription(UpdateUIMessages.InstallWizard_ReviewPage_desc); 
					label.setText(UpdateUIMessages.InstallWizard_ReviewPage_label); 
				} else {
					if (isUpdateSearch)
						setDescription(UpdateUIMessages.InstallWizard_ReviewPage_zeroUpdates); 
					else
						setDescription(UpdateUIMessages.InstallWizard_ReviewPage_zeroFeatures); 
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
		treeViewer.setCheckedElements(trueUpdates.toArray()); 
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
		label.setText(UpdateUIMessages.InstallWizard_ReviewPage_label); 
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

        createTreeViewer(client);

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

//		Button button = new Button(buttonContainer, SWT.PUSH);
//		button.setText(UpdateUI.getString("InstallWizard.ReviewPage.selectAll")); //$NON-NLS-1$
//		gd =
//			new GridData(
//				GridData.HORIZONTAL_ALIGN_FILL
//					| GridData.VERTICAL_ALIGN_BEGINNING);
//		button.setLayoutData(gd);
//		SWTUtil.setButtonDimensionHint(button);
//		button.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				handleSelectAll(true);
//			}
//		});

		Button button = new Button(buttonContainer, SWT.PUSH);
		button.setText(UpdateUIMessages.InstallWizard_ReviewPage_deselectAll); 
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDeselectAll();
			}
		});

		moreInfoButton = new Button(buttonContainer, SWT.PUSH);
		moreInfoButton.setText(UpdateUIMessages.InstallWizard_ReviewPage_moreInfo); 
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
		propertiesButton.setText(UpdateUIMessages.InstallWizard_ReviewPage_properties); 
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
		statusButton.setText(UpdateUIMessages.InstallWizard_ReviewPage_showStatus); 
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

		filterOlderVersionCheck = new Button(client, SWT.CHECK);
		filterOlderVersionCheck.setText(UpdateUIMessages.InstallWizard_ReviewPage_filterOlderFeatures); 
		filterOlderVersionCheck.setSelection(true);
//		tableViewer.addFilter(olderVersionFilter);
		filterOlderVersionCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (filterOlderVersionCheck.getSelection())
					treeViewer.addFilter(olderVersionFilter);
				else 
					treeViewer.removeFilter(olderVersionFilter);
				
				pageChanged();
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		filterOlderVersionCheck.setLayoutData(gd);
		
		filterCheck = new Button(client, SWT.CHECK);
		filterCheck.setText(UpdateUIMessages.InstallWizard_ReviewPage_filterFeatures); 
		filterCheck.setSelection(false);
		//tableViewer.addFilter(filter);
		filterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (filterCheck.getSelection()) {
					// make sure model is local
					if (downloadIncludedFeatures()) {
						treeViewer.addFilter(filter);
					} else {
						filterCheck.setSelection(false);
					}
				} else {
					treeViewer.removeFilter(filter);
				}
				pageChanged();
			}
		});
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		filterCheck.setLayoutData(gd);
		
		pageChanged();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(client, "org.eclipse.update.ui.MultiReviewPage2"); //$NON-NLS-1$

		Dialog.applyDialogFont(parent);

		return client;
	}

    private void createTreeViewer(Composite parent) {
        SashForm sform = new SashForm(parent, SWT.VERTICAL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 250;
        gd.heightHint =100;
        sform.setLayoutData(gd);
        
        treeViewer = new ContainerCheckedTreeViewer(sform, SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.BORDER);
        treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        treeViewer.setContentProvider(new TreeContentProvider());
        treeViewer.setLabelProvider(new TreeLabelProvider());
        treeViewer.setInput(UpdateUI.getDefault().getUpdateModel());

        treeViewer
            .addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent e) {
                handleSelectionChanged((IStructuredSelection) e.getSelection());
            }
        });
        
        treeViewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                validateSelection();
                Object site = getSite(event.getElement());
                ArrayList descendants = new ArrayList();
                collectDescendants(site, descendants);
                Object[] nodes = new Object[descendants.size()];
                for (int i=0; i<nodes.length; i++)
                    nodes[i] = descendants.get(i);
                treeViewer.update(nodes, null);
            }
        });

//      treeViewer.addFilter(new ViewerFilter() {
//          public boolean select(
//              Viewer viewer,
//              Object parentElement,
//              Object element) {
//              if (element instanceof SiteBookmark)
//                  return !((SiteBookmark) element).isWebBookmark();
//              return true;
//          }
//      });
        
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
    
    
//    private void handleSiteChecked(SiteBookmark bookmark, boolean checked) {
//
//        bookmark.setSelected(checked);
//        if (checked)
//            bookmark.setIgnoredCategories(new String[0]);
//            
//        if (checked || bookmark.isSiteConnected())
//            treeViewer.setSubtreeChecked(bookmark, checked);
//   
//        treeViewer.setGrayed(bookmark, false);
//    }
//    
//    private void handleCategoryChecked(SiteCategory category, boolean checked) {
//        SiteBookmark bookmark = category.getBookmark();
//
//        ArrayList array = new ArrayList();
//
//        if (bookmark.isSelected()) {
//            String[] ignored = bookmark.getIgnoredCategories();
//            for (int i = 0; i < ignored.length; i++)
//                array.add(ignored[i]);
//        } else {
//            Object[] categs = bookmark.getCatalog(true, null);
//            for (int i = 0; i < categs.length; i++)
//                array.add(((SiteCategory) categs[i]).getFullName());
//        }
//
//        if (checked) {
//            array.remove(category.getFullName());
//        } else {
//            array.add(category.getFullName());
//        }
//
//        bookmark.setIgnoredCategories((String[]) array.toArray(new String[array
//                .size()]));
//
//        Object[] children = ((TreeContentProvider) treeViewer
//                .getContentProvider()).getChildren(category.getBookmark());
//        treeViewer.setChecked(bookmark, array.size() < children.length);
//        bookmark.setSelected(array.size() < children.length);
//        
//        if (checked)
//            treeViewer.setSubtreeChecked(bookmark, checked);
//        
//        treeViewer.setGrayed(bookmark, array.size() > 0
//                && array.size() < children.length);
//    }
//
//    private void handleFeatureChecked(IInstallFeatureOperation job, boolean checked) {
//        treeViewer.setGrayed(job, false);
//    }
    
    private void handleSelectionChanged(IStructuredSelection ssel) {
        boolean enable = false;
        Object item = ssel.getFirstElement();
        String description = null;
        if (item instanceof SiteBookmark) {
            enable = !((SiteBookmark) item).isReadOnly();
            description = ((SiteBookmark)item).getDescription();
        } else if (item instanceof SiteCategory) {
            IURLEntry descEntry = ((SiteCategory)item).getCategory().getDescription();
            if (descEntry != null)
                description = descEntry.getAnnotation();
        } else if (item instanceof IInstallFeatureOperation) {
            jobSelected(ssel);
//            IURLEntry descEntry = ((IInstallFeatureOperation)item).getFeature().getDescription();
//            if (descEntry != null)
//                description = descEntry.getAnnotation();
            return;
        }

        if (description == null)
            description = ""; //$NON-NLS-1$
        descLabel.setText(UpdateManagerUtils.getWritableXMLString(description));
        propertiesButton.setEnabled(false);
        moreInfoButton.setEnabled(false);
    }

	
	private void fillContextMenu(IMenuManager manager) {
		if (treeViewer.getSelection().isEmpty()) return;
		Action action = new Action(UpdateUIMessages.InstallWizard_ReviewPage_prop) { 
			public void run() {
				handleProperties();
			}
		};
		manager.add(action);
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
		descLabel.setText(UpdateManagerUtils.getWritableXMLString(desc));
		propertiesButton.setEnabled(feature != null);
		moreInfoButton.setEnabled(job != null && getMoreInfoURL(job) != null);
	}
	
	private void pageChanged() {
		Object[] checked = getSelectedJobs();
		int totalCount = jobs.size();
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
		treeViewer.update(jobs.toArray(), null);
		statusButton.setEnabled(validationStatus != null);
        treeViewer.refresh();
	}

	private void updateItemCount(int checkedCount, int totalCount) {
		if (checkedCount == -1) {
			Object[] checkedElements = getSelectedJobs();
			checkedCount = checkedElements.length;
		}
		if (totalCount == -1) {
			totalCount = jobs.size();
		}
		String total = "" + totalCount; //$NON-NLS-1$
		String selected = "" + checkedCount; //$NON-NLS-1$
		counterLabel.setText(
			NLS.bind("InstallWizard.ReviewPage.counter", (new String[] { selected, total })));
		counterLabel.getParent().layout();
	}

//	private void handleSelectAll(boolean select) {
//		treeViewer.setAllChecked(select);
////		 make sure model is local (download using progress monitor from container)
//		downloadIncludedFeatures(); 
//			
//		treeViewer.getControl().getDisplay().asyncExec(new Runnable() {
//			public void run() {
//				pageChanged();
//			}
//		});
//	}

//  private void handleSelectAll(boolean select) {
//  treeViewer.setAllChecked(select);
////     make sure model is local (download using progress monitor from container)
//  downloadIncludedFeatures(); 
//      
//  treeViewer.getControl().getDisplay().asyncExec(new Runnable() {
//      public void run() {
//          pageChanged();
//      }
//  });
//}

   private void handleDeselectAll() {
        treeViewer.setCheckedElements(new Object[0]);
        // make sure model is local (download using progress monitor from
        // container)
//        downloadIncludedFeatures();

        treeViewer.getControl().getDisplay().asyncExec(new Runnable() {
            public void run() {
                pageChanged();
            }
        });
}
    
	private void handleProperties() {
		final IStructuredSelection selection =
			(IStructuredSelection) treeViewer.getSelection();

		final IInstallFeatureOperation job =
			(IInstallFeatureOperation) selection.getFirstElement();
		if (propertiesAction == null) {
			propertiesAction =
				new FeaturePropertyDialogAction(getShell(), treeViewer);
		}

		BusyIndicator
			.showWhile(treeViewer.getControl().getDisplay(), new Runnable() {
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
			(IStructuredSelection) treeViewer.getSelection();
		final IInstallFeatureOperation selectedJob =
			(IInstallFeatureOperation) selection.getFirstElement();
		BusyIndicator
			.showWhile(treeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				String urlName = getMoreInfoURL(selectedJob);
				UpdateUI.showURL(urlName);
			}
		});
	}

	public IInstallFeatureOperation[] getSelectedJobs() {      
        Object[] selected = treeViewer.getCheckedElements();
        ArrayList selectedJobs = new ArrayList(selected.length);
        for (int i=0; i<selected.length; i++)
            if (selected[i] instanceof IInstallFeatureOperation)
                selectedJobs.add(selected[i]);
        return (IInstallFeatureOperation[])selectedJobs.toArray(new IInstallFeatureOperation[selectedJobs.size()]);
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
				setErrorMessage(UpdateUIMessages.InstallWizard_ReviewPage_invalid_long); 
			}
		}
	}

	class StatusDialog extends ErrorDialog {
//		Button detailsButton;
		public StatusDialog() {
			super(getContainer().getShell(), UpdateUIMessages.InstallWizard_ReviewPage_invalid_short, null, //$NON-NLS-1$
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
    
    private IInstallFeatureOperation findJob(FeatureReferenceAdapter feature)
            throws CoreException {
        if (jobs == null)
            return null;
        for (int i = 0; i < jobs.size(); i++)
            if (((IInstallFeatureOperation) jobs.get(i)).getFeature()
                    .getVersionedIdentifier().equals(feature.getFeatureReference()
                    .getVersionedIdentifier()))
                return (IInstallFeatureOperation) jobs.get(i);

        return null;
    }
    
    private Object getSite(Object object) {
        ITreeContentProvider provider = (ITreeContentProvider)treeViewer.getContentProvider();
        while (object != null && !(object instanceof SiteBookmark)) {
            object = provider.getParent(object);
        }
        return object;
    }
    
    private void collectDescendants(Object root, ArrayList list) {
        ITreeContentProvider provider = (ITreeContentProvider)treeViewer.getContentProvider();
        Object[] children = provider.getChildren(root);
        if (children != null && children.length > 0)
            for (int i=0; i<children.length; i++) {
                list.add(children[i]);
                collectDescendants(children[i], list);
            }
    }
}
