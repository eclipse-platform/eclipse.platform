/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.wizards;

import java.net.URL;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.widgets.ScrolledFormText;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIImages;
import org.eclipse.update.internal.ui.UpdateUIMessages;
import org.eclipse.update.internal.ui.model.DiscoveryFolder;
import org.eclipse.update.internal.ui.model.SiteBookmark;
import org.eclipse.update.internal.ui.model.SiteCategory;
import org.eclipse.update.internal.ui.model.UpdateModel;
import org.eclipse.update.internal.ui.parts.SWTUtil;
import org.eclipse.update.operations.IUpdateModelChangedListener;
import org.eclipse.update.search.EnvironmentFilter;
import org.eclipse.update.search.UpdateSearchRequest;
import org.eclipse.update.search.UpdateSearchScope;

public class SitePage extends BannerPage implements ISearchProvider {

	class SitesLabelProvider extends LabelProvider {

		public Image getImage(Object obj) {
			if (obj instanceof SiteBookmark)
				return UpdateUI.getDefault().getLabelProvider().get(
					UpdateUIImages.DESC_SITE_OBJ);
			return super.getImage(obj);
		}

		public String getText(Object obj) {
			if (obj instanceof SiteBookmark) {
				return ((SiteBookmark) obj).getLabel();
			}
			return super.getText(obj);
		}
	}

	class ModelListener implements IUpdateModelChangedListener {
		public void objectChanged(Object object, String property) {
			viewer.refresh();
			checkItems();
		}

		public void objectsAdded(Object parent, Object[] children) {
            viewer.refresh();
			checkItems();
		}

		public void objectsRemoved(Object parent, Object[] children) {
			viewer.refresh();
			checkItems();
		}
	}

	private static DiscoveryFolder discoveryFolder = new DiscoveryFolder();
	private CheckboxTableViewer viewer;
	private ScrolledFormText descLabel;
	private Button addSiteButton;
	private Button addLocalButton;
	private Button addLocalZippedButton;
	private Button editButton;
	private Button removeButton;
	private Button exportButton;
	private Button importButton;
	private Button envFilterCheck;
	private Button automaticallySelectMirrorsCheckbox;
	private EnvironmentFilter envFilter;
	private UpdateSearchRequest searchRequest;
	private ModelListener modelListener;
	
	private boolean automaticallySelectMirrors = true;

	public SitePage(UpdateSearchRequest searchRequest) {
		super("SitePage"); //$NON-NLS-1$
        this.searchRequest = searchRequest;
		setTitle(UpdateUIMessages.SitePage_title); 
		setDescription(UpdateUIMessages.SitePage_desc); 
		UpdateUI.getDefault().getLabelProvider().connect(this);
    	envFilter = new EnvironmentFilter();

		modelListener = new ModelListener();
		UpdateUI.getDefault().getUpdateModel().addUpdateModelChangedListener(
			modelListener);
	}

	private void toggleEnvFilter(boolean add) {
		if (add)
			searchRequest.addFilter(envFilter);
		else
			searchRequest.removeFilter(envFilter);
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		UpdateUI
			.getDefault()
			.getUpdateModel()
			.removeUpdateModelChangedListener(
			modelListener);
		super.dispose();
	}

	/*
	 * (non-Javadoc) @see
	 * org.eclipse.update.internal.ui.wizards.BannerPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite client = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		client.setLayout(layout);

		Label label = new Label(client, SWT.NULL);
		label.setText(UpdateUIMessages.SitePage_label); 
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		createViewer(client);

		Composite buttonContainer = new Composite(client, SWT.NULL);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonContainer.setLayout(layout);

		addSiteButton = new Button(buttonContainer, SWT.PUSH);
		addSiteButton.setText(UpdateUIMessages.SitePage_addUpdateSite); 
		addSiteButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(addSiteButton);
		addSiteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddSite();
			}
		});

		addLocalButton = new Button(buttonContainer, SWT.PUSH);
		addLocalButton.setText(UpdateUIMessages.SitePage_addLocalSite); 
		addLocalButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(addLocalButton);
		addLocalButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddLocal();
			}
		});

		addLocalZippedButton = new Button(buttonContainer, SWT.PUSH);
		addLocalZippedButton.setText(UpdateUIMessages.SitePage_addLocalZippedSite); 
		addLocalZippedButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(addLocalZippedButton);
		addLocalZippedButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddLocalZipped();
			}
		});
		
		// separator
		new Label(buttonContainer, SWT.None);
		
		editButton = new Button(buttonContainer, SWT.PUSH);
		editButton.setText(UpdateUIMessages.SitePage_edit); 
		editButton.setEnabled(false);
		editButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(editButton);
		editButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEdit();
			}
		});

		removeButton = new Button(buttonContainer, SWT.PUSH);
		removeButton.setText(UpdateUIMessages.SitePage_remove); 
		removeButton.setEnabled(false);
		removeButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(removeButton);
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		
		// separator
		new Label(buttonContainer, SWT.None);
		
		importButton = new Button(buttonContainer, SWT.PUSH);
		importButton.setText(UpdateUIMessages.SitePage_import); 
		importButton.setLayoutData(
			new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(importButton);
		importButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleImport();
			}
		});
		
		exportButton = new Button(buttonContainer, SWT.PUSH);
		exportButton.setText(UpdateUIMessages.SitePage_export); 
		exportButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(exportButton);
		exportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleExport();
			}
		});

		descLabel = new ScrolledFormText(client, true);
		descLabel.setText(""); //$NON-NLS-1$
		descLabel.setBackground(parent.getBackground());
		HyperlinkSettings settings = new HyperlinkSettings(parent.getDisplay());
		descLabel.getFormText().setHyperlinkSettings(settings);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;
        gd.heightHint = 30;
		descLabel.setLayoutData(gd);
		
		envFilterCheck = new Button(client, SWT.CHECK);
		envFilterCheck.setText(UpdateUIMessages.SitePage_ignore); 
		envFilterCheck.setSelection(true);
		toggleEnvFilter(true);
		envFilterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toggleEnvFilter(envFilterCheck.getSelection());
			}
		});
		gd = new GridData();
		gd.horizontalSpan = 2;
        gd.verticalAlignment = SWT.BOTTOM;
		envFilterCheck.setLayoutData(gd);

		
		automaticallySelectMirrorsCheckbox = new Button(client, SWT.CHECK);
		automaticallySelectMirrorsCheckbox.setText(UpdateUIMessages.SitePage_automaticallySelectMirrors); 
		automaticallySelectMirrorsCheckbox.setSelection(true);
		automaticallySelectMirrorsCheckbox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				automaticallySelectMirrors = automaticallySelectMirrorsCheckbox.getSelection();
				UpdateCore.getPlugin().getPluginPreferences().setValue(UpdateCore.P_AUTOMATICALLY_CHOOSE_MIRROR, automaticallySelectMirrors);
			}
		});
		gd = new GridData();
		gd.horizontalSpan = 2;
        gd.verticalAlignment = SWT.BOTTOM;
        automaticallySelectMirrorsCheckbox.setLayoutData(gd);
		
		Dialog.applyDialogFont(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(client, "org.eclipse.update.ui.SitePage"); //$NON-NLS-1$

		return client;
	}

	private void createViewer(Composite parent) {
		viewer =
			CheckboxTableViewer.newCheckList(
				parent,
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object parent) {
				return getAllSiteBookmarks();
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}
		});
		viewer.setLabelProvider(new SitesLabelProvider());
		viewer.setInput(UpdateUI.getDefault().getUpdateModel());
		
		// bug # 83212
		viewer.setSorter( new ViewerSorter());
		
		
		initializeItems();

		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				Object element = e.getElement();
				if (element instanceof SiteBookmark)
					handleSiteChecked((SiteBookmark) element, e.getChecked());
			}
		});

		viewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged((IStructuredSelection) e.getSelection());
			}
		});

	}

	private void initializeItems() {
		checkItems();
		updateSearchRequest();
	}

	private void checkItems() {
		TableItem[] items = viewer.getTable().getItems();
		for (int i = 0; i < items.length; i++) {
			SiteBookmark bookmark = (SiteBookmark) items[i].getData();
			viewer.setChecked(bookmark, bookmark.isSelected());
		}
	}

	private void handleAddSite() {
		NewUpdateSiteDialog dialog = new NewUpdateSiteDialog(getShell(), getAllSiteBookmarks());
		dialog.create();
		dialog.getShell().setText(UpdateUIMessages.SitePage_new); 
		if (dialog.open() == NewUpdateSiteDialog.OK)
			updateSearchRequest();
	}

	private void handleAddLocal() {
		SiteBookmark siteBookmark = LocalSiteSelector.getLocaLSite(getShell(), this.getAllSiteBookmarks());
		if (siteBookmark != null) {
			if (handleNameEdit(siteBookmark) == EditSiteDialog.OK) {
				siteBookmark.setSelected(true);
				UpdateModel model = UpdateUI.getDefault().getUpdateModel();
				model.addBookmark(siteBookmark);
				model.saveBookmarks();
				updateSearchRequest();
			}
		}
		return;
	}

	private void handleAddLocalZipped() {
		SiteBookmark siteBookmark = LocalSiteSelector
				.getLocaLZippedSite(getShell(), this.getAllSiteBookmarks());
		if (siteBookmark != null) {
			if (handleNameEdit(siteBookmark) == EditSiteDialog.OK) {
				siteBookmark.setSelected(true);
				UpdateModel model = UpdateUI.getDefault().getUpdateModel();
				model.addBookmark(siteBookmark);
				model.saveBookmarks();
				updateSearchRequest();
			}
		}
		return;
	}

	private void handleRemove() {
		BusyIndicator
			.showWhile(viewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				UpdateModel updateModel =
					UpdateUI.getDefault().getUpdateModel();
				IStructuredSelection ssel =
					(IStructuredSelection) viewer.getSelection();
				SiteBookmark bookmark = (SiteBookmark) ssel.getFirstElement();
				String selName = bookmark.getLabel();
				boolean answer = MessageDialog
								.openQuestion(
										getShell(),
										UpdateUIMessages.SitePage_remove_location_conf_title, 
										UpdateUIMessages.SitePage_remove_location_conf
												+ " " + selName); //$NON-NLS-1$

				if (answer && !bookmark.isReadOnly()) {
					updateModel.removeBookmark(bookmark);
					updateSearchRequest();
				}
			}
		});
	}

	private void handleEdit() {
		IStructuredSelection ssel =
			(IStructuredSelection) viewer.getSelection();
		SiteBookmark bookmark = (SiteBookmark) ssel.getFirstElement();
		URL oldURL = bookmark.getURL();
		EditSiteDialog dialog = new EditSiteDialog(getShell(), bookmark, getAllSiteBookmarks());
		dialog.create();
		String title = bookmark.isLocal() ? UpdateUIMessages.SitePage_dialogEditLocal : UpdateUIMessages.SitePage_dialogEditUpdateSite; 
																																				  // //$NON-NLS-2$
		dialog.getShell().setText(title);
		if (dialog.open() == EditSiteDialog.OK ) {
			URL newURL = bookmark.getURL();
			if (!UpdateManagerUtils.sameURL(oldURL, newURL)) {
				UpdateModel model = UpdateUI.getDefault().getUpdateModel();
				model.fireObjectChanged(bookmark, null);
				updateSearchRequest();	
			}
		}
	}

	private void handleImport() {
		SiteBookmark[] siteBookmarks = SitesImportExport.getImportedBookmarks(getShell());
		if (siteBookmarks != null && siteBookmarks.length > 0) {
			UpdateModel model = UpdateUI.getDefault().getUpdateModel();
			SiteBookmark[] currentBookmarks = getAllSiteBookmarks();
			for (int i=0; i<siteBookmarks.length; i++) {
				boolean siteExists = false;
				for (int j=0; !siteExists && j<currentBookmarks.length; j++)
					if (currentBookmarks[j].getURL().equals(siteBookmarks[i].getURL()))
						siteExists = true;
				if (!siteExists)
					model.addBookmark(siteBookmarks[i]);
			}
			model.saveBookmarks();
			updateSearchRequest();
		}
		return;
	}
	
	private void handleExport() {
		SitesImportExport.exportBookmarks(getShell(), getAllSiteBookmarks());
	}
	
	private int handleNameEdit(SiteBookmark bookmark) {
		EditSiteDialog dialog = new EditSiteDialog(getShell(), bookmark, getAllSiteBookmarks(), true);
		dialog.create();
		String title = bookmark.isLocal() ? UpdateUIMessages.SitePage_dialogEditLocal : UpdateUIMessages.SitePage_dialogEditUpdateSite; 
		// //$NON-NLS-2$
		dialog.getShell().setText(title);
		int rc = dialog.open();
		return rc;
	}

	private void handleSiteChecked(SiteBookmark bookmark, boolean checked) {
		if (bookmark.isUnavailable()) {
			bookmark.setSelected(false);
			viewer.setChecked(bookmark, false);
			return;
		}
		
		bookmark.setSelected(checked);
		updateSearchRequest();
	}


	private void handleSelectionChanged(IStructuredSelection ssel) {
		boolean enable = false;
		Object item = ssel.getFirstElement();
		String description = null;
		if (item instanceof SiteBookmark) {
			enable = !((SiteBookmark) item).isReadOnly();
			description = ((SiteBookmark)item).getDescription();
		} else if (item instanceof SiteCategory) {
//			IURLEntry descEntry = ((SiteCategory)item).getCategory().getDescription();
//			if (descEntry != null)
//				description = descEntry.getAnnotation();
		}
		editButton.setEnabled(enable);
		removeButton.setEnabled(enable);

		if (description == null)
			description = ""; //$NON-NLS-1$
		descLabel.setText(UpdateManagerUtils.getWritableXMLString(description));
	}

	private void updateSearchRequest() {
		Object[] checked = viewer.getCheckedElements();

		UpdateSearchScope scope = new UpdateSearchScope();
		int nsites = 0;

		for (int i = 0; i < checked.length; i++) {
			if (checked[i] instanceof SiteBookmark) {
				SiteBookmark bookmark = (SiteBookmark) checked[i];
				scope.addSearchSite(
					bookmark.getLabel(),
					bookmark.getURL(),
					bookmark.getIgnoredCategories());
				nsites++;
			}
		}
		searchRequest.setScope(scope);
		setPageComplete(nsites > 0);
	}

	public UpdateSearchRequest getSearchRequest() {
		return searchRequest;
	}
    
	public void setVisible(boolean value) {
		super.setVisible(value);
		if (value) {
			// Reset all unavailable sites, so they can be tried again if the user wants it
			SiteBookmark[] bookmarks = getAllSiteBookmarks();
			for (int i=0; i<bookmarks.length; i++) {
				if (bookmarks[i].isUnavailable())
					bookmarks[i].setUnavailable(false);
			}
			automaticallySelectMirrors = UpdateCore.getPlugin().getPluginPreferences().getBoolean(UpdateCore.P_AUTOMATICALLY_CHOOSE_MIRROR);
			automaticallySelectMirrorsCheckbox.setSelection(automaticallySelectMirrors);
		}
	}
	
	private SiteBookmark[] getAllSiteBookmarks() {
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		Object[] bookmarks = model.getBookmarkLeafs();
		Object[] sitesToVisit =
			discoveryFolder.getChildren(discoveryFolder);
		SiteBookmark[] all = new SiteBookmark[bookmarks.length + sitesToVisit.length];
		System.arraycopy(bookmarks, 0, all, 0, bookmarks.length);
		System.arraycopy(
			sitesToVisit,
			0,
			all,
			bookmarks.length,
			sitesToVisit.length);
		return all;
	}

	public boolean isPageComplete() {
		return viewer.getCheckedElements().length != 0;
	}
}
