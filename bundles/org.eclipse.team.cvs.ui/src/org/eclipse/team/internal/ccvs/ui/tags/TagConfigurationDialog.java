/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.tags;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.model.RemoteContentProvider;
import org.eclipse.team.internal.ccvs.ui.repo.NewDateTagAction;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.tags.TagSourceWorkbenchAdapter.ProjectElementComparator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Allows configuration of the CVS tags that are shown within the workbench.
 */
public class TagConfigurationDialog extends TrayDialog {
	
	// show the resource contained within the roots
	private TreeViewer cvsResourceTree;
	
	// shows the tags found on the selected resources
	private CheckboxTableViewer cvsTagTree;
	
	// shows the defined tags for the given root
	private TreeViewer cvsDefinedTagsTree;
	
	// remember the root element in the defined tags tree
	private TagSourceWorkbenchAdapter cvsDefinedTagsRootElement;
	
	// list of auto-refresh files
	private org.eclipse.swt.widgets.List autoRefreshFileList;
	
	// enable selecting auto-refresh files
	private boolean allowSettingAutoRefreshFiles = true;
	
	// preference keys
	private final String ALLOWREFRESH_WIDTH_KEY = "AllowRefreshWidth"; //$NON-NLS-1$
	private final String ALLOWREFRESH_HEIGHT_KEY = "AllowRefreshHeight"; //$NON-NLS-1$
	private final String NOREFRESH_WIDTH_KEY = "NoRefreshWidth"; //$NON-NLS-1$
	private final String NOREFRESH_HEIGHT_KEY = "NoRefreshHeight"; //$NON-NLS-1$

	// buttons
	private Button addSelectedTagsButton;
	private Button addSelectedFilesButton;
	private Button removeFileButton;
	private Button removeTagButton;
	
	// dialogs settings that are persistent between workbench sessions
	private IDialogSettings settings;

	private final TagSource tagSource;

	private final TagSourceWrapper wrappedTagSource;
	
	class FileComparator extends ViewerComparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			boolean oneIsFile = e1 instanceof CVSFileElement;
			boolean twoIsFile = e2 instanceof CVSFileElement;
			if (oneIsFile != twoIsFile) {
				return oneIsFile ? 1 : -1;
			}
			return super.compare(viewer, e1, e2);
		}
	}
	
	/*
	 * Create a tag source that cahces the added and removed tags
	 * so that the changes can be propogated to the repository 
	 * manager when OK is pressed
	 */
	class TagSourceWrapper extends TagSource {

		private final TagSource tagSource;
		private final List<CVSTag> branches = new ArrayList<>();
		private final List<CVSTag> versions = new ArrayList<>();
		private final List<CVSTag> dates = new ArrayList<>();

		public TagSourceWrapper(TagSource tagSource) {
			this.tagSource = tagSource;
			branches.addAll(Arrays.asList(tagSource.getTags(CVSTag.BRANCH)));
			versions.addAll(Arrays.asList(tagSource.getTags(CVSTag.VERSION)));
			dates.addAll(Arrays.asList(tagSource.getTags(CVSTag.DATE)));
		}
		
		@Override
		public CVSTag[] getTags(int type) {
			if (type == CVSTag.HEAD || type == BASE) {
				return super.getTags(type);
			}
			List<CVSTag> list = getTagList(type);
			if (list != null)
				return list.toArray(new CVSTag[list.size()]);
			return tagSource.getTags(type);
		}

		private List<CVSTag> getTagList(int type) {
			switch (type) {
			case CVSTag.VERSION: 
				return versions;
			case CVSTag.BRANCH:
				return branches;
			case CVSTag.DATE:
				return dates;
		}
			return null;
		}
		
		@Override
		public CVSTag[] refresh(boolean bestEffort, IProgressMonitor monitor) throws TeamException {
			// The wrapper is never refreshed
			return new CVSTag[0];
		}

		@Override
		public ICVSRepositoryLocation getLocation() {
			return tagSource.getLocation();
		}

		@Override
		public String getShortDescription() {
			return tagSource.getShortDescription();
		}

		public void remove(CVSTag[] tags) {
			for (CVSTag tag : tags) {
				List list = getTagList(tag.getType());
				if (list != null)
					list.remove(tag);        
			}
		}

		public void add(CVSTag[] tags) {
			for (CVSTag tag : tags) {
				List<CVSTag> list = getTagList(tag.getType());
				if (list != null)
					list.add(tag);        
			}
		}

		public void removeAll() {
			versions.clear();
			branches.clear();
			dates.clear();
		}

		/**
		 * Remember the state that has been accumulated
		 * @param monitor
		 * @throws CVSException
		 */
		public void commit(IProgressMonitor monitor) throws CVSException {
			tagSource.commit(getTags(new int[] { CVSTag.VERSION, CVSTag.BRANCH, CVSTag.DATE }), true /* replace */, monitor);
		}

		@Override
		public void commit(CVSTag[] tags, boolean replace, IProgressMonitor monitor) throws CVSException {
			// Not invoked
		}

		@Override
		public ICVSResource[] getCVSResources() {
			return tagSource.getCVSResources();
		}
	}
	
	public TagConfigurationDialog(Shell shell, TagSource tagSource) {
		super(shell);
		this.tagSource = tagSource;
		wrappedTagSource = new TagSourceWrapper(tagSource);
		setShellStyle(SWT.CLOSE|SWT.RESIZE|SWT.APPLICATION_MODAL);
		allowSettingAutoRefreshFiles = getSingleFolder(tagSource, false) != null;
		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		this.settings = workbenchSettings.getSection("TagConfigurationDialog");//$NON-NLS-1$
		if (settings == null) {
			this.settings = workbenchSettings.addNewSection("TagConfigurationDialog");//$NON-NLS-1$
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(NLS.bind(CVSUIMessages.TagConfigurationDialog_1, new String[] { tagSource.getShortDescription() })); 
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite shell = new Composite(parent, SWT.NONE);
		GridData data = new GridData (GridData.FILL_BOTH);		
		shell.setLayoutData(data);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		gridLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);        
		shell.setLayout (gridLayout);
		
		Composite comp = new Composite(shell, SWT.NULL);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		comp.setLayout(gridLayout);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label cvsResourceTreeLabel = new Label(comp, SWT.NONE);
		cvsResourceTreeLabel.setText(CVSUIMessages.TagConfigurationDialog_5); 
		data = new GridData();
		data.horizontalSpan = 1;
		cvsResourceTreeLabel.setLayoutData(data);
	
		Tree tree = new Tree(comp, SWT.BORDER | SWT.MULTI);
		cvsResourceTree = new TreeViewer (tree);
		cvsResourceTree.setContentProvider(new RemoteContentProvider());
		cvsResourceTree.setLabelProvider(new WorkbenchLabelProvider());
		data = new GridData (GridData.FILL_BOTH);
		data.heightHint = 150;
		data.horizontalSpan = 1;
		cvsResourceTree.getTree().setLayoutData(data);
		cvsResourceTree.setComparator(new FileComparator());
		cvsResourceTree.setInput(TagSourceResourceAdapter.getViewerInput(tagSource));
		cvsResourceTree.addSelectionChangedListener(event -> {
			updateShownTags();
			updateEnablements();
		});

		comp = new Composite(shell, SWT.NULL);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		comp.setLayout(gridLayout);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
	
		Label cvsTagTreeLabel = new Label(comp, SWT.NONE);
		cvsTagTreeLabel.setText(CVSUIMessages.TagConfigurationDialog_6); 
		data = new GridData();
		data.horizontalSpan = 1;
		cvsTagTreeLabel.setLayoutData(data);
		
		final Table table = new Table(comp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 150;
		data.horizontalSpan = 1;
		table.setLayoutData(data);
		cvsTagTree = new CheckboxTableViewer(table);
		cvsTagTree.setContentProvider(new WorkbenchContentProvider());
		cvsTagTree.setLabelProvider(new WorkbenchLabelProvider());
		cvsTagTree.addSelectionChangedListener(event -> updateEnablements());
		
		Composite selectComp = new Composite(comp, SWT.NONE);
		GridLayout selectLayout = new GridLayout(2, true);
		selectLayout.marginHeight = selectLayout.marginWidth = 0;
		selectComp.setLayout(selectLayout);
		selectComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button selectAllButton = new Button(selectComp, SWT.PUSH);
		selectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectAllButton.setText(CVSUIMessages.ReleaseCommentDialog_selectAll); 
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int nItems = table.getItemCount();
				for (int j=0; j<nItems; j++)
					table.getItem(j).setChecked(true);
			}
		});
		Button deselectAllButton = new Button(selectComp, SWT.PUSH);
		deselectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		deselectAllButton.setText(CVSUIMessages.ReleaseCommentDialog_deselectAll); 
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int nItems = table.getItemCount();
				for (int j=0; j<nItems; j++)
					table.getItem(j).setChecked(false);
			}
		});
		
		cvsTagTree.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (!(e1 instanceof TagElement) || !(e2 instanceof TagElement)) return super.compare(viewer, e1, e2);
				CVSTag tag1 = ((TagElement)e1).getTag();
				CVSTag tag2 = ((TagElement)e2).getTag();
				int type1 = tag1.getType();
				int type2 = tag2.getType();
				if (type1 != type2) {
					return  type1 - type2;
				}
				// Sort in reverse order so larger numbered versions are at the top
				return -tag1.compareTo(tag2);
			}
		});
		
		Composite rememberedTags = new Composite(shell, SWT.NONE);
		data = new GridData (GridData.FILL_BOTH);		
		data.horizontalSpan = 2;
		rememberedTags.setLayoutData(data);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		rememberedTags.setLayout (gridLayout);
	
		Label rememberedTagsLabel = new Label (rememberedTags, SWT.NONE);
		rememberedTagsLabel.setText (CVSUIMessages.TagConfigurationDialog_7); 
		data = new GridData ();
		data.horizontalSpan = 2;
		rememberedTagsLabel.setLayoutData (data);
		
		tree = new Tree(rememberedTags, SWT.BORDER | SWT.MULTI);
		cvsDefinedTagsTree = new TreeViewer (tree);
		cvsDefinedTagsTree.setContentProvider(new WorkbenchContentProvider());
		cvsDefinedTagsTree.setLabelProvider(new WorkbenchLabelProvider());
		data = new GridData (GridData.FILL_BOTH);
		data.heightHint = 100;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		cvsDefinedTagsTree.getTree().setLayoutData(data);
		cvsDefinedTagsRootElement = new TagSourceWorkbenchAdapter(wrappedTagSource, TagSourceWorkbenchAdapter.INCLUDE_BRANCHES | TagSourceWorkbenchAdapter.INCLUDE_VERSIONS |TagSourceWorkbenchAdapter.INCLUDE_DATES);
		cvsDefinedTagsTree.setInput(cvsDefinedTagsRootElement);
		cvsDefinedTagsTree.addSelectionChangedListener(event -> updateEnablements());
		cvsDefinedTagsTree.setComparator(new ProjectElementComparator());
	
		Composite buttonComposite = new Composite(rememberedTags, SWT.NONE);
		data = new GridData ();
		data.verticalAlignment = GridData.BEGINNING;		
		buttonComposite.setLayoutData(data);
		gridLayout = new GridLayout();
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		buttonComposite.setLayout (gridLayout);
		
		addSelectedTagsButton = new Button (buttonComposite, SWT.PUSH);
		addSelectedTagsButton.setText (CVSUIMessages.TagConfigurationDialog_8); 
		data = getStandardButtonData(addSelectedTagsButton);
		data.horizontalAlignment = GridData.FILL;
		addSelectedTagsButton.setLayoutData(data);
		addSelectedTagsButton.addListener(SWT.Selection, event -> {
			rememberCheckedTags();
			updateShownTags();
			updateEnablements();
		});			
		Button addDatesButton = new Button(buttonComposite, SWT.PUSH);
		addDatesButton.setText(CVSUIMessages.TagConfigurationDialog_0); 
		data = getStandardButtonData(addDatesButton);
		data.horizontalAlignment = GridData.FILL;
		addDatesButton.setLayoutData(data);
		addDatesButton.addListener(SWT.Selection, event -> {
			CVSTag dateTag = NewDateTagAction.getDateTag(getShell(), tagSource.getLocation());
			addDateTagsSelected(dateTag);
			updateShownTags();
			updateEnablements();
		});
		removeTagButton = new Button (buttonComposite, SWT.PUSH);
		removeTagButton.setText (CVSUIMessages.TagConfigurationDialog_9); 
		data = getStandardButtonData(removeTagButton);
		data.horizontalAlignment = GridData.FILL;		
		removeTagButton.setLayoutData(data);
		removeTagButton.addListener(SWT.Selection, event -> {
			deleteSelected();
			updateShownTags();
			updateEnablements();
		});
			
		Button removeAllTags = new Button (buttonComposite, SWT.PUSH);
		removeAllTags.setText (CVSUIMessages.TagConfigurationDialog_10); 
		data = getStandardButtonData(removeAllTags);
		data.horizontalAlignment = GridData.FILL;		
		removeAllTags.setLayoutData(data);
		removeAllTags.addListener(SWT.Selection, event -> {
			removeAllKnownTags();
			updateShownTags();
			updateEnablements();
		});
		
		if(allowSettingAutoRefreshFiles) {
			Label explanation = new Label(rememberedTags, SWT.WRAP);
			explanation.setText(CVSUIMessages.TagConfigurationDialog_11); 
			data = new GridData ();
			data.horizontalSpan = 2;
			//data.widthHint = 300;
			explanation.setLayoutData(data);
			
			autoRefreshFileList = new org.eclipse.swt.widgets.List(rememberedTags, SWT.BORDER | SWT.MULTI);	 
			data = new GridData ();		
			data.heightHint = 45;
			data.horizontalAlignment = GridData.FILL;
			data.grabExcessHorizontalSpace = true;
			autoRefreshFileList.setLayoutData(data);		
			try {
				autoRefreshFileList.setItems(CVSUIPlugin.getPlugin().getRepositoryManager().getAutoRefreshFiles(getSingleFolder(tagSource, false)));
			} catch (CVSException e) {
				autoRefreshFileList.setItems(new String[0]);
				CVSUIPlugin.log(e);
			}
			autoRefreshFileList.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateEnablements();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					updateEnablements();
				}
			});
	
			Composite buttonComposite2 = new Composite(rememberedTags, SWT.NONE);
			data = new GridData ();
			data.verticalAlignment = GridData.BEGINNING;		
			buttonComposite2.setLayoutData(data);
			gridLayout = new GridLayout();
			gridLayout.marginHeight = 0;
			gridLayout.marginWidth = 0;
			buttonComposite2.setLayout (gridLayout);
	
			addSelectedFilesButton = new Button (buttonComposite2, SWT.PUSH);
			addSelectedFilesButton.setText (CVSUIMessages.TagConfigurationDialog_12); 
			data = getStandardButtonData(addSelectedFilesButton);
			data.horizontalAlignment = GridData.FILL;
			addSelectedFilesButton.setLayoutData(data);
			addSelectedFilesButton.addListener(SWT.Selection, event -> addSelectionToAutoRefreshList());			
				
			removeFileButton = new Button (buttonComposite2, SWT.PUSH);
			removeFileButton.setText (CVSUIMessages.TagConfigurationDialog_13); 
			data = getStandardButtonData(removeFileButton);
			data.horizontalAlignment = GridData.FILL;		
			removeFileButton.setLayoutData(data);
			removeFileButton.addListener(SWT.Selection, event -> {
				String[] selected = autoRefreshFileList.getSelection();
				for (String s : selected) {
					autoRefreshFileList.remove(s);
					autoRefreshFileList.setFocus();
				}
			});			
			PlatformUI.getWorkbench().getHelpSystem().setHelp(autoRefreshFileList, IHelpContextIds.TAG_CONFIGURATION_REFRESHLIST);
		}
			
		Label seperator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData (GridData.FILL_BOTH);		
		data.horizontalSpan = 2;
		seperator.setLayoutData(data);
	
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.TAG_CONFIGURATION_OVERVIEW);
	
		updateEnablements();
		Dialog.applyDialogFont(parent);
		return shell;
	}

	private void updateShownTags() {
		final CVSFileElement[] filesSelection = getSelectedFiles();
		final Set<CVSTag> tags = new HashSet<>();
		if(filesSelection.length!=0) {
			try {
				CVSUIPlugin.runWithProgress(getShell(), true /*cancelable*/, monitor -> {
					monitor.beginTask(CVSUIMessages.TagConfigurationDialog_22, filesSelection.length); 
					try {
						for (CVSFileElement f : filesSelection) {
							ICVSFile file = f.getCVSFile();
							tags.addAll(Arrays.asList(getTagsFor(file, Policy.subMonitorFor(monitor, 1))));
						}
					} catch (TeamException e) {
						// ignore the exception
					} finally {
						monitor.done();
					}
				});
			} catch (InterruptedException e) {
				// operation cancelled
			} catch (InvocationTargetException e) {
				// can't happen since we're ignoring all possible exceptions
			}
			cvsTagTree.getTable().removeAll();
			for (Iterator it = tags.iterator(); it.hasNext();) {
				CVSTag tag = (CVSTag) it.next();
				List<CVSTag> knownTags = new ArrayList<>();
				knownTags.addAll(Arrays.asList(wrappedTagSource.getTags(new int[] { CVSTag.VERSION, CVSTag.BRANCH, CVSTag.DATE })));
				if(!knownTags.contains(tag)) {
					TagElement tagElem = new TagElement(tag);
					cvsTagTree.add(tagElem);
					cvsTagTree.setChecked(tagElem, true);
				}
			}
		}
	}
	
	private CVSFileElement[] getSelectedFiles() {
		IStructuredSelection selection = cvsResourceTree.getStructuredSelection();
		if (!selection.isEmpty()) {
			final List<CVSFileElement> filesSelection = new ArrayList<>();
			Iterator it = selection.iterator();
			while(it.hasNext()) {
				Object o = it.next();
				if(o instanceof CVSFileElement) {
					filesSelection.add((CVSFileElement) o);
				}
			}
			return filesSelection.toArray(new CVSFileElement[filesSelection.size()]);
		}
		return new CVSFileElement[0];
	}
	
	private void addSelectionToAutoRefreshList() {
		IStructuredSelection selection = cvsResourceTree.getStructuredSelection();
		if (!selection.isEmpty()) {
			final List<CVSFileElement> filesSelection = new ArrayList<>();
			Iterator it = selection.iterator();
			while(it.hasNext()) {
				Object o = it.next();
				if(o instanceof CVSFileElement) {
					filesSelection.add((CVSFileElement) o);
				}
			}
			if(!filesSelection.isEmpty()) {
				for (it = filesSelection.iterator(); it.hasNext();) {
					try {
						ICVSFile file = ((CVSFileElement)it.next()).getCVSFile();
						ICVSFolder fileParent = file.getParent();
						String filePath = new Path(null, fileParent.getFolderSyncInfo().getRepository())
							.append(file.getRelativePath(fileParent)).toString();
						if(autoRefreshFileList.indexOf(filePath)==-1) {
							autoRefreshFileList.add(filePath);					
						}
					} catch(CVSException e) {
						CVSUIPlugin.openError(getShell(), null, null, e);
					}
				}
			}
		}
	}
	
	private CVSTag[] getTagsFor(ICVSFile file, IProgressMonitor monitor) throws TeamException {
		return SingleFileTagSource.fetchTagsFor(file, monitor);
	}
	
	private void rememberCheckedTags() {
		Object[] checked = cvsTagTree.getCheckedElements();
		List<CVSTag> tagsToAdd = new ArrayList<>();
		for (Object c : checked) {
			CVSTag tag = ((TagElement) c).getTag();
			tagsToAdd.add(tag);
		}
		if (!tagsToAdd.isEmpty()) {
			wrappedTagSource.add(tagsToAdd.toArray(new CVSTag[tagsToAdd.size()]));
			cvsDefinedTagsTree.refresh();
		}
	}
	
	private void deleteSelected() {
		IStructuredSelection selection = cvsDefinedTagsTree.getStructuredSelection();
		List<CVSTag> tagsToRemove = new ArrayList<>();
		if (!selection.isEmpty()) {
			Iterator it = selection.iterator();
			while(it.hasNext()) {
				Object o = it.next();
				if(o instanceof TagElement) {
					CVSTag tag = ((TagElement)o).getTag();
					tagsToRemove.add(tag);
				}
			}
		}
		if (!tagsToRemove.isEmpty()) {
			wrappedTagSource.remove(tagsToRemove.toArray(new CVSTag[tagsToRemove.size()]));
			cvsDefinedTagsTree.refresh();
			cvsDefinedTagsTree.getTree().setFocus();
		}
	}
	private void addDateTagsSelected(CVSTag tag){
		if(tag == null) return;
		List<CVSTag> knownTags = new ArrayList<>();
		knownTags.addAll(Arrays.asList(wrappedTagSource.getTags(CVSTag.DATE)));
		if(!knownTags.contains( tag)){
			wrappedTagSource.add(new CVSTag[] { tag });
			cvsDefinedTagsTree.refresh();
			cvsDefinedTagsTree.getTree().setFocus();
		}
	}
	private boolean isTagSelectedInKnownTagTree() {
		IStructuredSelection selection = cvsDefinedTagsTree.getStructuredSelection();
		if (!selection.isEmpty()) {
			Iterator it = selection.iterator();
			while(it.hasNext()) {
				Object o = it.next();
				if(o instanceof TagElement) {
					return true;		
				}
			}
		}
		return false;
	}

	private void removeAllKnownTags() {
		wrappedTagSource.removeAll();
		cvsDefinedTagsTree.refresh();
	}
	
	private void updateEnablements() {
		// add checked tags
		Object[] checked = cvsTagTree.getCheckedElements();
		addSelectedTagsButton.setEnabled(checked.length!=0?true:false);
		
		// Remove known tags
		removeTagButton.setEnabled(isTagSelectedInKnownTagTree()?true:false);
		
		if(allowSettingAutoRefreshFiles) {
			// add selected files
			addSelectedFilesButton.setEnabled(getSelectedFiles().length!=0?true:false);
			
			// remove auto refresh files
			removeFileButton.setEnabled(autoRefreshFileList.getSelection().length!=0?true:false);		
		}
	}
	
	@Override
	protected void okPressed() {
		try {
			// save auto refresh file names
			if(allowSettingAutoRefreshFiles) {
				RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();
				manager.setAutoRefreshFiles(getSingleFolder(tagSource, false), autoRefreshFileList.getItems());		
			}
			
			wrappedTagSource.commit(null);
			
			super.okPressed();
		} catch (CVSException e) {
			CVSUIPlugin.openError(getShell(), null, null, e);
		}
	}
	
	protected ICVSFolder getSingleFolder(TagSource tagSource, boolean bestEffort) {
		if (!bestEffort && tagSource instanceof MultiFolderTagSource)
			return null;
		if (tagSource instanceof SingleFolderTagSource)
			return ((SingleFolderTagSource)tagSource).getFolder();
		return null;
	}

	@Override
	protected Point getInitialSize() {
		int width, height;
		if(allowSettingAutoRefreshFiles) {
			try {
				height = settings.getInt(ALLOWREFRESH_HEIGHT_KEY);
				width = settings.getInt(ALLOWREFRESH_WIDTH_KEY);
			} catch(NumberFormatException e) {
				return super.getInitialSize();
			}
		} else {
			try {
				height = settings.getInt(NOREFRESH_HEIGHT_KEY);
				width = settings.getInt(NOREFRESH_WIDTH_KEY);
			} catch(NumberFormatException e) {
				return super.getInitialSize();
			}
		}
		return new Point(width, height);
	}
	
	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}
	
	private GridData getStandardButtonData(Button button) {
		GridData data = new GridData();
		data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		//don't crop labels with large font
		//int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		//data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		return data;
	}

	@Override
	public boolean close() {
		// Close the tray so we only remember the size without the tray
		if (getTray() != null)
			closeTray();
		Rectangle bounds = getShell().getBounds();
		if(allowSettingAutoRefreshFiles) {
			settings.put(ALLOWREFRESH_HEIGHT_KEY, bounds.height);
			settings.put(ALLOWREFRESH_WIDTH_KEY, bounds.width);
		} else {
			settings.put(NOREFRESH_HEIGHT_KEY, bounds.height);
			settings.put(NOREFRESH_WIDTH_KEY, bounds.width);
		}
		return super.close();
	}
}
