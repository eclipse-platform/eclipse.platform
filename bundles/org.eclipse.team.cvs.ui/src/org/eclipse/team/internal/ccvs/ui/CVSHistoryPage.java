/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
 *     Eugene Kuleshov <eu@md.pp.ru> - Bug 153932 [History] Custom hyperlink detectors for comments in History view
 *     Brock Janiczak (brockj@tpg.com.au) - Bug 181899 CVS History wrongly ordered
 *******************************************************************************/

package org.eclipse.team.internal.ccvs.ui;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.resources.mapping.ResourceChangeValidator;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.*;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.filehistory.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;
import org.eclipse.team.internal.ccvs.ui.actions.MoveRemoteTagAction;
import org.eclipse.team.internal.ccvs.ui.operations.*;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.*;
import org.eclipse.team.internal.ui.history.*;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.history.*;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

public class CVSHistoryPage extends HistoryPage implements IAdaptable, IHistoryCompareAdapter {
	
	/* private */ ICVSFile file;
	/* private */ ICVSFile previousFile;
	/* private */ IFileRevision currentFileRevision;
	private ITypedElement fileElement;
	
	// cached for efficiency
	/* private */ CVSFileHistory cvsFileHistory;
	/* private */IFileRevision[] entries;

	/* private */CVSHistoryTableProvider historyTableProvider;

	private Composite tagAndTextComposite;
	private Composite searchComposite;
	
	/* private */TreeViewer treeViewer;
	/* private */TextViewer textViewer;
	/* private */TableViewer tagViewer;

	/* private */CompareRevisionAction compareAction;
	/* private */OpenRevisionAction openAction;
	private OpenWithMenu openWithMenu;
	
	private CVSHistoryFilterAction  cvsHistoryFilter;
	private IAction toggleTextAction;
	private IAction toggleTextWrapAction;
	private IAction toggleListAction;
	private IAction toggleFilterAction;
	private IAction toggleSearchAction;
	private TextViewerAction copyAction;
	private TextViewerAction selectAllAction;
	private Action getContentsAction;
	private Action getRevisionAction;
	private Action refreshAction;
	/**
	 * Allows copying the tag name to the clipboard
	 */
	private TableViewerAction copyTagAction;
	
	private Action tagWithExistingAction;
	private Action localMode;
	private Action remoteMode;
	private Action remoteLocalMode;
	private Action groupByDateMode;
	private Action collapseAll;
	private Action compareModeAction;
	
	private SashForm sashForm;
	private SashForm innerSashForm;
	private SashForm searchSashForm;
	
	private Image branchImage;
	private Image versionImage;

	private IDialogSettings settings;

	protected IFileRevision currentSelection;

	/* private */RefreshCVSFileHistory  refreshCVSFileHistoryJob;

	/* private */boolean shutdown = false;

	/* private */boolean localFilteredOut = false;
	/* private */boolean remoteFilteredOut = false;
	
	private HistoryResourceListener resourceListener;
	
	//toggle constants for default click action
	private boolean compareMode = false;
	
	//filter constants
	public final static int REMOTE_LOCAL_MODE = 0;
	public final static int REMOTE_MODE = 1;
	public final static int LOCAL_MODE = 2;

	// page settings keys
	private final static String SASH_WEIGHTS = "SASH_WEIGHTS"; //$NON-NLS-1$
	private final static String INNER_SASH_WEIGHTS = "INNER_SASH_WEIGHTS"; //$NON-NLS-1$
	private final static String SASH_WEIGHTS_SEPARATOR = ";"; //$NON-NLS-1$
	private final static String SORT_ORDER_KEY = "SORT_ORDER"; //$NON-NLS-1$

	// page settings section name
	private static final String CVS_HISTORY_PAGE_SECTION = CVSHistoryPage.class.getName();

	//current filter mode
	private int currentFilerMode = 0;
	
	//text field used for search
	private Text searchField;
	
	//current tag list sort order.
	private boolean sortTagsAscending;

	// listener registered on the book this page is contained
	private DisposeListener disposeListener;

	//grouping on
	private boolean groupingOn;
	private CVSHistoryFilter historyFilter;
	private CVSHistorySearchFilter searchFilter;
	private RevisionAnnotationController rulerSelectionListener;
	
	private int refreshRequest = 0;
	private DateFormat dateTimeFormat;
	private String description;
	
	public CVSHistoryPage(Object object) {
		this.file = getCVSFile(object);

		IDialogSettings viewsSettings = CVSUIPlugin.getPlugin()
				.getDialogSettings();
		settings = viewsSettings.getSection(CVS_HISTORY_PAGE_SECTION);
		if (settings == null) {
			settings = viewsSettings.addNewSection(CVS_HISTORY_PAGE_SECTION);
		}
		sortTagsAscending= settings.get(SORT_ORDER_KEY) == null || settings.getBoolean(SORT_ORDER_KEY);
	}

	/**
	 * Action to sort the tag list ascending or descending.
	 */
	final class SortTagsAction extends Action {
		
		private boolean sortAscending;
		
		public SortTagsAction(boolean sortAscending) {
			super(
				sortAscending
					? CVSUIMessages.CVSHistoryPage_SortTagsAscendingAction
					: CVSUIMessages.CVSHistoryPage_SortTagsDescendingAction,
				IAction.AS_RADIO_BUTTON);
			setChecked(sortAscending == sortTagsAscending);
			this.sortAscending = sortAscending;
		}

		@Override
		public void run() {
			sortTagsAscending = sortAscending;
			tagViewer.refresh();
		}
	}


	@Override
	public void createControl(Composite parent) {
		initializeImages();
		
		sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		treeViewer = createTree(sashForm);
		
		tagAndTextComposite = new Composite(sashForm, SWT.NONE);
		tagAndTextComposite.setLayout(new FillLayout());
		innerSashForm = new SashForm(tagAndTextComposite, SWT.HORIZONTAL);
		tagViewer = createTagTable(innerSashForm);
		textViewer = createText(innerSashForm);
		
		searchComposite = new Composite(sashForm, SWT.NONE);
		searchComposite.setLayout(new FillLayout());
		searchSashForm = new SashForm(searchComposite, SWT.HORIZONTAL);
		//Find field
		searchField = new Text(searchSashForm, SWT.SEARCH);
		searchField.setMessage(CVSUIMessages.CVSHistoryPage_EnterSearchTerm);
		final SearchHistoryTable searchHistoryTable = new SearchHistoryTable();
		searchField.addModifyListener(e -> Display.getDefault().timerExec(1000, searchHistoryTable));

		contributeActions();
		setViewerVisibility();

		int[] weights = loadSashWeights(SASH_WEIGHTS);
		sashForm.setWeights(weights.length == 3 ? weights : new int[] { 65, 20,
				15 });
		int[] innerWeights = loadSashWeights(INNER_SASH_WEIGHTS);
		innerSashForm.setWeights(innerWeights.length == 2 ? innerWeights
				: new int[] { 50, 50 });

		IHistoryPageSite parentSite = getHistoryPageSite();
		if (parentSite != null && parentSite instanceof DialogHistoryPageSite && treeViewer != null)
			parentSite.setSelectionProvider(treeViewer);
		
		resourceListener = new HistoryResourceListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(sashForm, IHelpContextIds.RESOURCE_HISTORY_VIEW);

		disposeListener = e -> saveState();
		parent.addDisposeListener(disposeListener);
	}

	private void saveState() {
		saveSashWeights(SASH_WEIGHTS, sashForm.getWeights());
		saveSashWeights(INNER_SASH_WEIGHTS, innerSashForm.getWeights());
		historyTableProvider.saveColumnLayout();
		settings.put(SORT_ORDER_KEY, sortTagsAscending);
	}

	private int[] loadSashWeights(String key) {
		String value = settings.get(key);
		if (value == null) {
			return new int[0];
		}
		String weigths[] = value.split(SASH_WEIGHTS_SEPARATOR);
		int result[] = new int[weigths.length];
		for (int i = 0; i < weigths.length; i++) {
			try {
				result[i] = Integer.parseInt(weigths[i]);
			} catch (NumberFormatException e) {
				return new int[0];
			}
		}
		return result;
	}

	private void saveSashWeights(String key, int[] weights) {
		StringBuffer value = new StringBuffer();
		for (int i = 0; i < weights.length; i++) {
			value = value.append(weights[i] + SASH_WEIGHTS_SEPARATOR);
		}
		settings.put(key, value.toString());
	}

	private TextViewer createText(SashForm parent) {
	SourceViewer result = new SourceViewer(parent, null, null, true, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY);
	result.getTextWidget().setIndent(2);
	result.configure(new TextSourceViewerConfiguration(EditorsUI.getPreferenceStore()) {
	@Override
	protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
		return Collections.singletonMap("org.eclipse.ui.DefaultTextEditor", //$NON-NLS-1$
			new IAdaptable() {
			@Override
			public <T> T getAdapter(Class<T> adapter) {
				if(adapter==IFile.class && getInput() instanceof IFile) {
					return adapter.cast(getInput());
				} else if(adapter==IFileHistory.class && getInput() instanceof IFileHistory) {
					return adapter.cast(getInput());
				}
				return Platform.getAdapterManager().getAdapter(CVSHistoryPage.this, adapter);
			}
			});
	}
	});

	result.addSelectionChangedListener(event -> copyAction.update());
	result.setTextDoubleClickStrategy(
				new DefaultTextDoubleClickStrategy(),
				IDocument.DEFAULT_CONTENT_TYPE);
		result.activatePlugins();
		return result;
	}

	private TableViewer createTagTable(SashForm parent) {
		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		TableViewer result = new TableViewer(table);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100));
		table.setLayout(layout);
		result.setContentProvider(new SimpleContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement == null)
					return new Object[0];
				ITag[] tags = (ITag[]) inputElement;
				return tags;
			}
		});
		result.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				if (element == null)
					return null;
				ITag tag = (ITag) element;
				if (!(tag instanceof CVSTag))
					return null;
				
				switch (((CVSTag)tag).getType()) {
					case CVSTag.BRANCH:
					case CVSTag.HEAD:
						return branchImage;
					case CVSTag.VERSION:
						return versionImage;
				}
				return null;
			}

			@Override
			public String getText(Object element) {
				return ((ITag) element).getName();
			}
		});
		result.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (!(e1 instanceof ITag) || !(e2 instanceof ITag))
					return super.compare(viewer, e1, e2);
				CVSTag tag1 = (CVSTag) e1;
				CVSTag tag2 = (CVSTag) e2;
				int type1 = tag1.getType();
				int type2 = tag2.getType();
				if (type1 != type2) {
					return type2 - type1;
				}
				if (sortTagsAscending)
					return super.compare(viewer, tag1, tag2);
				else
					return super.compare(viewer, tag2, tag1);
			}
		});
		result.addSelectionChangedListener(event -> {
			copyTagAction.setEnabled(false);
			if (event.getSelection() instanceof StructuredSelection) {
				if (((StructuredSelection) event.getSelection()).getFirstElement() != null) {
					copyTagAction.setEnabled(true);
				}
			}
			
		});
		return result;
	}

	@Override
	public void setFocus() {
		sashForm.setFocus();
		if (refreshRequest != 0) {
			refresh(refreshRequest);
			refreshRequest = 0;
		}
	}

	protected void contributeActions() {
		CVSUIPlugin plugin = CVSUIPlugin.getPlugin();

		//Refresh
		refreshAction = new Action(CVSUIMessages.HistoryView_refreshLabel, plugin.getImageDescriptor(ICVSUIConstants.IMG_REFRESH_ENABLED)) {
			@Override
			public void run() {
				refresh();
			}
		};
		refreshAction.setToolTipText(CVSUIMessages.HistoryView_refresh);
		refreshAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_REFRESH_DISABLED));
		refreshAction.setHoverImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_REFRESH));

		//Local Mode
		final IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		localMode =  new Action(CVSUIMessages.CVSHistoryPage_LocalModeAction, plugin.getImageDescriptor(ICVSUIConstants.IMG_LOCALMODE)) {
			@Override
			public void run() {
				if (isChecked()){
					store.setValue(ICVSUIConstants.PREF_REVISION_MODE, LOCAL_MODE);
					updateFilterMode(LOCAL_MODE);
				} else
					setChecked(true);
			}
		};
		localMode.setToolTipText(CVSUIMessages.CVSHistoryPage_LocalModeTooltip);
		localMode.setDisabledImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_LOCALMODE_DISABLED));
		localMode.setHoverImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_LOCALMODE));

		//Remote Mode
		remoteMode =  new Action(CVSUIMessages.CVSHistoryPage_RemoteModeAction, plugin.getImageDescriptor(ICVSUIConstants.IMG_REMOTEMODE)) {
			@Override
			public void run() {
				if (isChecked()){
					store.setValue(ICVSUIConstants.PREF_REVISION_MODE, REMOTE_MODE);
					updateFilterMode(REMOTE_MODE);
				} else
					setChecked(true);
			}
		};
		remoteMode.setToolTipText(CVSUIMessages.CVSHistoryPage_RemoteModeTooltip);
		remoteMode.setDisabledImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_REMOTEMODE_DISABLED));
		remoteMode.setHoverImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_REMOTEMODE));
		
		//Remote + Local Mode
		remoteLocalMode =  new Action(CVSUIMessages.CVSHistoryPage_CombinedModeAction, plugin.getImageDescriptor(ICVSUIConstants.IMG_LOCALREMOTE_MODE)) {
			@Override
			public void run() {
				if (isChecked()){
					store.setValue(ICVSUIConstants.PREF_REVISION_MODE, REMOTE_LOCAL_MODE);
					updateFilterMode(REMOTE_LOCAL_MODE);
				} else
					setChecked(true);
			}
		};
		remoteLocalMode.setToolTipText(CVSUIMessages.CVSHistoryPage_CombinedModeTooltip);
		remoteLocalMode.setDisabledImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_LOCALREMOTE_MODE_DISABLED));
		remoteLocalMode.setHoverImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_LOCALREMOTE_MODE));
		
		//set the inital filter to both remote and local
		updateFilterMode(store.getInt(ICVSUIConstants.PREF_REVISION_MODE));
		
		//Group by Date
		groupByDateMode = new Action(CVSUIMessages.CVSHistoryPage_GroupByDate, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_DATES_CATEGORY)){
			@Override
			public void run() {
				groupingOn = !groupingOn;
				store.setValue(ICVSUIConstants.PREF_GROUPBYDATE_MODE, groupingOn);
				refreshHistory(false, false, CVSFileHistory.REFRESH_LOCAL | CVSFileHistory.REFRESH_REMOTE);
			}
		};
		groupingOn = store.getBoolean(ICVSUIConstants.PREF_GROUPBYDATE_MODE);
		groupByDateMode.setChecked(groupingOn);
		groupByDateMode.setToolTipText(CVSUIMessages.CVSHistoryPage_GroupByDate);
		groupByDateMode.setDisabledImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_DATES_CATEGORY));
		groupByDateMode.setHoverImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_DATES_CATEGORY));
		
		//Collapse All
		collapseAll =  new Action(CVSUIMessages.CVSHistoryPage_CollapseAllAction, plugin.getImageDescriptor(ICVSUIConstants.IMG_COLLAPSE_ALL)) {
			@Override
			public void run() {
				treeViewer.collapseAll();
			}
		};
		collapseAll.setToolTipText(CVSUIMessages.CVSHistoryPage_CollapseAllTooltip);
		collapseAll.setDisabledImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_COLLAPSE_ALL));
		collapseAll.setHoverImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_COLLAPSE_ALL));
		
		//Compare Mode Action
		compareModeAction = new Action(CVSUIMessages.CVSHistoryPage_CompareModeToggleAction,plugin.getImageDescriptor(ICVSUIConstants.IMG_COMPARE_VIEW)) {
			@Override
			public void run() {
				compareMode = !compareMode;
				compareModeAction.setChecked(compareMode);
			}
		};
		compareModeAction.setToolTipText(CVSUIMessages.CVSHistoryPage_CompareModeTooltip);
		compareModeAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_COMPARE_VIEW));
		compareModeAction.setHoverImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_COMPARE_VIEW));
		compareModeAction.setChecked(false);
		
		// Click Compare action
		compareAction = new CompareRevisionAction(CVSUIMessages.CVSHistoryPage_CompareRevisionAction, this);
		compareAction.setEnabled(!treeViewer.getSelection().isEmpty());
		treeViewer.getTree().addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				//update the current
				compareAction.setCurrentFileRevision(getCurrentFileRevision());
				compareAction.selectionChanged(treeViewer.getStructuredSelection());
			}
		});
		
		openAction = new OpenRevisionAction(CVSUIMessages.CVSHistoryPage_OpenAction, this);
		openAction.setEnabled(!treeViewer.getSelection().isEmpty());
		treeViewer.getTree().addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				openAction.selectionChanged(treeViewer.getStructuredSelection());
			}
		});
		
		// Add 'Open With...'  sub-menu
		openWithMenu = new OpenWithMenu(this);
		treeViewer.getTree().addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				openWithMenu.selectionChanged(treeViewer.getStructuredSelection());
			}
		});

		new OpenAndLinkWithEditorHelper(treeViewer) {
			@Override
			protected void open(ISelection selection, boolean activate) {
				if (getSite() != null && selection instanceof IStructuredSelection) {
					IStructuredSelection structuredSelection= (IStructuredSelection)selection;
					if (compareMode){
						StructuredSelection sel = new StructuredSelection(new Object[] {getCurrentFileRevision(), structuredSelection.getFirstElement()});
						compareAction.selectionChanged(sel);
						compareAction.run();
					} else {
						//Pass in the entire structured selection to allow for multiple editor openings
						openAction.selectionChanged(structuredSelection);
						openAction.run();
					}
				}
			}

			@Override
			protected void activate(ISelection selection) {
				int currentMode= OpenStrategy.getOpenMethod();
				try {
					OpenStrategy.setOpenMethod(OpenStrategy.DOUBLE_CLICK);
					open(selection, true);
				} finally {
					OpenStrategy.setOpenMethod(currentMode);
				}
			}

			@Override
			protected void linkToEditor(ISelection selection) {
				// XXX: Not yet implemented, see http://bugs.eclipse.org/324185
			}
		};

		getContentsAction = getContextMenuAction(CVSUIMessages.HistoryView_getContentsAction, true /* needs progress */, monitor -> {
			monitor.beginTask(null, 100);
			try {
				if(confirmOverwrite() && validateChange()) {
					IStorage currentStorage = currentSelection.getStorage(SubMonitor.convert(monitor, 50));
					InputStream in = currentStorage.getContents();
					((IFile)file.getIResource()).setContents(in, false, true, SubMonitor.convert(monitor, 50));
				}
			} catch (TeamException e) {
				throw new CoreException(e.getStatus());
			} finally {
				monitor.done();
			}
		});
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getContentsAction, IHelpContextIds.GET_FILE_CONTENTS_ACTION);

		getRevisionAction = getContextMenuAction(CVSUIMessages.HistoryView_getRevisionAction, true /* needs progress */, monitor -> {
			ICVSRemoteFile remoteFile = (ICVSRemoteFile) CVSWorkspaceRoot.getRemoteResourceFor(((CVSFileRevision) currentSelection).getCVSRemoteFile());
			try {
				if(confirmOverwrite() && validateChange()) {
					CVSTag revisionTag = new CVSTag(remoteFile.getRevision(), CVSTag.VERSION);
					
					if(CVSAction.checkForMixingTags(getHistoryPageSite().getShell(), new IResource[] {file.getIResource()}, revisionTag)) {
						new UpdateOperation(
								null,
								new IResource[] {file.getIResource()},
								new Command.LocalOption[] {Update.IGNORE_LOCAL_CHANGES},
								revisionTag)
									.run(monitor);
						
						Display.getDefault().asyncExec(() -> refresh());
					}
				}
			} catch (InvocationTargetException e1) {
				throw CVSException.wrapException(e1);
			} catch (InterruptedException e2) {
				// Cancelled by user
			}
		});
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getRevisionAction, IHelpContextIds.GET_FILE_REVISION_ACTION);

		// Override MoveRemoteTagAction to work for log entries
		final IActionDelegate tagActionDelegate = new MoveRemoteTagAction() {
			@Override
			protected ICVSResource[] getSelectedCVSResources() {
				ICVSResource[] resources = super.getSelectedCVSResources();
				if (resources == null || resources.length == 0) {
					ArrayList logEntrieFiles = null;
					IStructuredSelection selection = getSelection();
					if (!selection.isEmpty()) {
						logEntrieFiles = new ArrayList();
						Iterator elements = selection.iterator();
						while (elements.hasNext()) {
							Object next = elements.next();
							if (next instanceof CVSFileRevision) {
								logEntrieFiles.add(((CVSFileRevision)next).getCVSRemoteFile());
								continue;
							}
							if (next instanceof IAdaptable) {
								IAdaptable a = (IAdaptable) next;
								Object adapter = a.getAdapter(ICVSResource.class);
								if (adapter instanceof ICVSResource) {
									logEntrieFiles.add(((ILogEntry)adapter).getRemoteFile());
									continue;
								}
							}
						}
					}
					if (logEntrieFiles != null && !logEntrieFiles.isEmpty()) {
						return (ICVSResource[])logEntrieFiles.toArray(new ICVSResource[logEntrieFiles.size()]);
					}
				}
				return resources;
			}
			/*
			 * Override the creation of the tag operation in order to support
			 * the refresh of the view after the tag operation completes
			 */
			@Override
			protected ITagOperation createTagOperation() {
				return new TagInRepositoryOperation(getTargetPart(), getSelectedRemoteResources()) {
					@Override
					public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
						super.execute(monitor);
						Display.getDefault().asyncExec(() -> {
							if( ! wasCancelled()) {
								refresh();
							}
						});
					}
				};
			}
		};
		tagWithExistingAction = getContextMenuAction(CVSUIMessages.HistoryView_tagWithExistingAction, false /* no progress */, monitor -> {
			tagActionDelegate.selectionChanged(tagWithExistingAction, treeViewer.getSelection());
			tagActionDelegate.run(tagWithExistingAction);
		});
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getRevisionAction, IHelpContextIds.TAG_WITH_EXISTING_ACTION);
		
		// Toggle text visible action
		toggleTextAction = new Action(TeamUIMessages.GenericHistoryView_ShowCommentViewer) {
			@Override
			public void run() {
				setViewerVisibility();
				store.setValue(ICVSUIConstants.PREF_SHOW_COMMENTS, toggleTextAction.isChecked());
			}
		};
		toggleTextAction.setChecked(store.getBoolean(ICVSUIConstants.PREF_SHOW_COMMENTS));
		//PlatformUI.getWorkbench().getHelpSystem().setHelp(toggleTextAction, IHelpContextIds.SHOW_COMMENT_IN_HISTORY_ACTION);

		// Toggle wrap comments action
		toggleTextWrapAction = new Action(TeamUIMessages.GenericHistoryView_WrapComments) {
			@Override
			public void run() {
				setViewerVisibility();
				store.setValue(ICVSUIConstants.PREF_WRAP_COMMENTS, toggleTextWrapAction.isChecked());
			}
		};
		toggleTextWrapAction.setChecked(store.getBoolean(ICVSUIConstants.PREF_WRAP_COMMENTS));
		//PlatformUI.getWorkbench().getHelpSystem().setHelp(toggleTextWrapAction, IHelpContextIds.SHOW_TAGS_IN_HISTORY_ACTION);

		// Toggle list visible action
		toggleListAction = new Action(TeamUIMessages.GenericHistoryView_ShowTagViewer) {
			@Override
			public void run() {
				setViewerVisibility();
				store.setValue(ICVSUIConstants.PREF_SHOW_TAGS, toggleListAction.isChecked());
			}
		};
		toggleListAction.setChecked(store.getBoolean(ICVSUIConstants.PREF_SHOW_TAGS));
		//PlatformUI.getWorkbench().getHelpSystem().setHelp(toggleListAction, IHelpContextIds.SHOW_TAGS_IN_HISTORY_ACTION);

		//Toggle search field
		toggleSearchAction= new Action(CVSUIMessages.CVSHistoryPage_ShowSearchField) {
			@Override
			public void run() {
				setViewerVisibility();
				store.setValue(ICVSUIConstants.PREF_SHOW_SEARCH, toggleSearchAction.isChecked());
				if (!toggleSearchAction.isChecked()){
					if (searchFilter != null)
						treeViewer.removeFilter(searchFilter);
				} else {
					searchField.setMessage(CVSUIMessages.CVSHistoryPage_EnterSearchTerm);
					searchField.selectAll();
					searchField.setFocus();
				}
			}
		};
		toggleSearchAction.setChecked(store.getBoolean(ICVSUIConstants.PREF_SHOW_SEARCH));
		//PlatformUI.getWorkbench().getHelpSystem().setHelp(toggleListAction, IHelpContextIds.SHOW_TAGS_IN_HISTORY_ACTION);
		
		toggleFilterAction = new Action(CVSUIMessages.CVSHistoryPage_NoFilter){
			@Override
			public void run(){
				if (historyFilter != null)
					treeViewer.removeFilter(historyFilter);
					historyFilter = null;
					String old = CVSHistoryPage.this.description;
					CVSHistoryPage.this.description = null;
					CVSHistoryPage.this.firePropertyChange(CVSHistoryPage.this, P_NAME, old, getName());
					toggleFilterAction.setEnabled(false);
			}
		};
		toggleFilterAction.setEnabled(historyFilter != null);
		
		//Create the filter action
		cvsHistoryFilter = new CVSHistoryFilterAction(this);
		cvsHistoryFilter.setText(CVSUIMessages.CVSHistoryPage_FilterOn);
		cvsHistoryFilter.init(treeViewer);
		cvsHistoryFilter.setToolTipText(CVSUIMessages.CVSHistoryPage_FilterHistoryTooltip);
		cvsHistoryFilter.setImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_FILTER_HISTORY));
		
		//Contribute actions to popup menu
		MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(treeViewer.getTree());
		menuMgr.addMenuListener(menuMgr1 -> fillTableMenu(menuMgr1));
		menuMgr.setRemoveAllWhenShown(true);
		treeViewer.getTree().setMenu(menu);
		//Don't add the object contribution menu items if this page is hosted in a dialog
		IHistoryPageSite parentSite = getHistoryPageSite();
		if (!parentSite.isModal()) {
			IWorkbenchPart part = parentSite.getPart();
			if (part != null) {
				IWorkbenchPartSite workbenchPartSite = part.getSite();
				workbenchPartSite.registerContextMenu(menuMgr, treeViewer);
			}
			IPageSite pageSite = parentSite.getWorkbenchPageSite();
			if (pageSite != null) {
				IActionBars actionBars = pageSite.getActionBars();
				// Contribute toggle text visible to the toolbar drop-down
				IMenuManager actionBarsMenu = actionBars.getMenuManager();
				if (actionBarsMenu != null){
					actionBarsMenu.add(toggleTextWrapAction);
					actionBarsMenu.add(new Separator());
					actionBarsMenu.add(toggleTextAction);
					actionBarsMenu.add(toggleListAction);
					actionBarsMenu.add(new Separator());
					actionBarsMenu.add(toggleSearchAction);
					actionBarsMenu.add(new Separator());
					actionBarsMenu.add(cvsHistoryFilter);
					actionBarsMenu.add(toggleFilterAction);
				}
				// Create actions for the text editor
				copyAction = new TextViewerAction(textViewer, ITextOperationTarget.COPY);
				copyAction.setText(CVSUIMessages.HistoryView_copy);
				actionBars.setGlobalActionHandler(ITextEditorActionConstants.COPY, copyAction);
				
				selectAllAction = new TextViewerAction(textViewer, ITextOperationTarget.SELECT_ALL);
				selectAllAction.setText(CVSUIMessages.HistoryView_selectAll);
				actionBars.setGlobalActionHandler(ITextEditorActionConstants.SELECT_ALL, selectAllAction);
				
				copyTagAction = new TableViewerAction(tagViewer);
				copyTagAction.setText(CVSUIMessages.HistoryView_copy);
				copyTagAction.setEnabled(false);
				
				actionBars.updateActionBars();
			}
		}
		
		
		
		//Create the local tool bar
		IToolBarManager tbm = parentSite.getToolBarManager();
		if (tbm != null) {
			String fileNameQualifier = getFileNameQualifier();
			//Add groups
			tbm.add(new Separator(fileNameQualifier + "grouping"));	//$NON-NLS-1$
			tbm.appendToGroup(fileNameQualifier+"grouping", groupByDateMode); //$NON-NLS-1$
			tbm.add(new Separator(fileNameQualifier+"modes"));	//$NON-NLS-1$
			tbm.appendToGroup(fileNameQualifier+"modes", remoteLocalMode); //$NON-NLS-1$
			tbm.appendToGroup(fileNameQualifier+"modes", localMode); //$NON-NLS-1$
			tbm.appendToGroup(fileNameQualifier+"modes", remoteMode); //$NON-NLS-1$
			tbm.add(new Separator(fileNameQualifier+"collapse")); //$NON-NLS-1$
			tbm.appendToGroup(fileNameQualifier+"collapse", collapseAll); //$NON-NLS-1$
			if (!parentSite.isModal()) {
				//don't bother adding the compare mode toolbar button if in
				//a dialog; you can only compare from dialogs
				tbm.appendToGroup(fileNameQualifier+"collapse", compareModeAction);  //$NON-NLS-1$
			}
			tbm.update(false);
		}

		menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(menuMgr1 -> fillTextMenu(menuMgr1));
		StyledText text = textViewer.getTextWidget();
		menu = menuMgr.createContextMenu(text);
		text.setMenu(menu);
		
		menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(menuMgr1 -> fillTagMenu(menuMgr1));
		menu = menuMgr.createContextMenu(tagViewer.getControl());
		tagViewer.getControl().setMenu(menu);
	}

	private String getFileNameQualifier() {
		//Just append the current system time to generate a unique group name
		return Long.toString(System.currentTimeMillis());
	}

	private boolean isLocalHistoryFilteredOut() {
		return localFilteredOut;
	}
	
	private boolean isRemoteHistoryFilteredOut(){
		return remoteFilteredOut;
	}
	
	/* private */ void fillTableMenu(IMenuManager manager) {
		// file actions go first (view file)
		IHistoryPageSite parentSite = getHistoryPageSite();
		manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
		
		if (file != null && !parentSite.isModal()){
			manager.add(openAction);
			
			MenuManager openWithSubmenu = new MenuManager(
					CVSUIMessages.CVSHistoryPage_OpenWithMenu);
			openWithSubmenu.add(openWithMenu);
			manager.add(openWithSubmenu);
			
			manager.add(compareAction);
			manager.add(new Separator("openCompare")); //$NON-NLS-1$
		}
		if (file != null &&
			!(file instanceof RemoteFile)) {
			// Add the "Add to Workspace" action if 1 revision is selected.
			ISelection sel = treeViewer.getSelection();
			if (!sel.isEmpty()) {
				if (sel instanceof IStructuredSelection) {
					IStructuredSelection tempSelection = (IStructuredSelection) sel;
					if (tempSelection.size() == 1) {
						manager.add(getContentsAction);
						if (!(tempSelection.getFirstElement() instanceof LocalFileRevision)) {
							manager.add(getRevisionAction);
							manager.add(new Separator());
							if (!parentSite.isModal())
								manager.add(tagWithExistingAction);
						}
					}
				}
			}
		}
		
		if (!parentSite.isModal()){
			manager.add(new Separator("additions")); //$NON-NLS-1$
			manager.add(refreshAction);
			manager.add(new Separator("additions-end")); //$NON-NLS-1$
		}
	}

	private void fillTextMenu(IMenuManager manager) {
		manager.add(copyAction);
		manager.add(selectAllAction);
	}
	
	private void fillTagMenu(IMenuManager manager) {
		manager.add(copyTagAction);
		manager.add(new Separator());
		manager.add(new SortTagsAction(true));
		manager.add(new SortTagsAction(false));
	}

	/**
	 * Creates the group that displays lists of the available repositories and
	 * team streams.
	 * 
	 * @param the
	 *            parent composite to contain the group
	 * @return the group control
	 */
	protected TreeViewer createTree(Composite parent) {

		historyTableProvider = new CVSHistoryTableProvider();
		TreeViewer viewer = historyTableProvider.createTree(parent);

		viewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {

				// The entries of already been fetch so return them
				if (entries != null)
					return entries;
				
				if (!(inputElement instanceof IFileHistory) &&
					!(inputElement instanceof AbstractHistoryCategory[]))
					return new Object[0];

				if (inputElement instanceof AbstractHistoryCategory[]){
					return (AbstractHistoryCategory[]) inputElement;
				}
				
				final IFileHistory fileHistory = (IFileHistory) inputElement;
				entries = fileHistory.getFileRevisions();
				
				return entries;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				entries = null;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof AbstractHistoryCategory){
					return ((AbstractHistoryCategory) parentElement).getRevisions();
				}
				
				return null;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof AbstractHistoryCategory){
					IFileRevision[] revs = ((AbstractHistoryCategory) element).getRevisions();
					if (revs != null)
						return revs.length > 0;
				}
				return false;
			}
		});

		viewer.addSelectionChangedListener(event -> {
			ISelection selection = event.getSelection();
			if (selection == null || !(selection instanceof IStructuredSelection)) {
				textViewer.setDocument(new Document("")); //$NON-NLS-1$
				tagViewer.setInput(null);
				setStatusLineMessage(null);
				return;
			}
			IStructuredSelection ss = (IStructuredSelection)selection;
			if (ss.size() != 1) {
				textViewer.setDocument(new Document("")); //$NON-NLS-1$
				tagViewer.setInput(null);
				setStatusLineMessage(null);
				return;
			}
			Object o = ss.getFirstElement();
			if (o instanceof AbstractHistoryCategory){
				textViewer.setDocument(new Document("")); //$NON-NLS-1$
				tagViewer.setInput(null);
				setStatusLineMessage(null);
				return;
			}
			IFileRevision entry = (IFileRevision)o;
			textViewer.setDocument(new Document(entry.getComment()));
			tagViewer.setInput(entry.getTags());
			setStatusLineMessage(CVSHistoryTableProvider.getCommentAsSingleLine(entry));
		});

		return viewer;
	}

	private Action getContextMenuAction(String title, final boolean needsProgressDialog, final IWorkspaceRunnable action) {
		return new Action(title) {
			@Override
			public void run() {
				try {
					if (file == null) return;
					ISelection selection = treeViewer.getSelection();
					if (!(selection instanceof IStructuredSelection)) return;
					IStructuredSelection ss = (IStructuredSelection)selection;
					Object o = ss.getFirstElement();
					
					if (o instanceof AbstractHistoryCategory)
						return;
					
					currentSelection = (IFileRevision)o;
					if(needsProgressDialog) {
						PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> {
							try {
								action.run(monitor);
							} catch (CoreException e) {
								throw new InvocationTargetException(e);
							}
						});
					} else {
						try {
							action.run(null);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}
				} catch (InvocationTargetException e) {
					IHistoryPageSite parentSite = getHistoryPageSite();
					CVSUIPlugin.openError(parentSite.getShell(), null, null, e, CVSUIPlugin.LOG_NONTEAM_EXCEPTIONS);
				} catch (InterruptedException e) {
					// Do nothing
				}
			}
			
			@Override
			public boolean isEnabled() {
				ISelection selection = treeViewer.getSelection();
				if (!(selection instanceof IStructuredSelection)) return false;
				IStructuredSelection ss = (IStructuredSelection)selection;
				if(ss.size() != 1) return false;
				return true;
			}
		};
	}

	private boolean confirmOverwrite() {
		if (file!=null && file.getIResource().exists()) {
			try {
				if(file.isModified(null)) {
					String title = CVSUIMessages.HistoryView_overwriteTitle;
					String msg = CVSUIMessages.HistoryView_overwriteMsg;
					IHistoryPageSite parentSite = getHistoryPageSite();
					final MessageDialog dialog = new MessageDialog(parentSite.getShell(), title, null, msg, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
					final int[] result = new int[1];
					parentSite.getShell().getDisplay().syncExec(() -> result[0] = dialog.open());
					if (result[0] != 0) {
						// cancel
						return false;
					}
				}
			} catch(CVSException e) {
				CVSUIPlugin.log(e);
			}
		}
		return true;
	}
	
	private boolean validateChange(){
		if (file!=null && file.getIResource().exists()) {
			IResourceChangeDescriptionFactory factory = ResourceChangeValidator.getValidator().createDeltaFactory();
			factory.change((IFile) file.getIResource());
			return IDE.promptToConfirm(getHistoryPageSite().getShell(), CVSUIMessages.CVSHistoryPage_ValidateChangeTitle, NLS.bind(CVSUIMessages.CVSHistoryPage_ValidateChangeMessage, new String[]{file.getName()}), factory.getDelta(), new String[0], true /* syncExec */);
		}
		
		return false;
	}

	/*
	 * Refresh the view by refetching the log entries for the remote file
	 */
	@Override
	public void refresh() {
		refresh(CVSFileHistory.REFRESH_LOCAL | CVSFileHistory.REFRESH_REMOTE);
	}
	
	public void refresh(int refreshFlags) {
		printDebugInfo("CVSHistoryPage#refresh", (IFile)(previousFile != null ? previousFile.getIResource() : null), cvsFileHistory, new Throwable()); //$NON-NLS-1$
		//refetch revisions, not a select only job
		// TODO
		refreshHistory(true, false, refreshFlags);
	}

	private void refreshHistory(boolean refetch, boolean selectOnly, int refreshFlags) {
		if (refreshCVSFileHistoryJob.getState() != Job.NONE){
			RefreshCVSFileHistory oldJob = refreshCVSFileHistoryJob;
			oldJob.cancel();
			refreshCVSFileHistoryJob = new RefreshCVSFileHistory(this);
			refreshCVSFileHistoryJob.setLocalFileRevision(oldJob.localFileRevision);
			refreshCVSFileHistoryJob.setSelectLocal(oldJob.useLocalSelect);
			refetch = true;
			selectOnly = false;
			refreshFlags = CVSFileHistory.REFRESH_ALL;
			printDebugInfo("CVSHistoryPage#refreshHistory, cancel old job", (IFile)(previousFile != null ? previousFile.getIResource() : null), cvsFileHistory, null); //$NON-NLS-1$
		}
		refreshCVSFileHistoryJob.setFileHistory(cvsFileHistory);
		IResource resource = previousFile.getIResource();
		if (resource != null){
			IResource workspaceFile = ResourcesPlugin.getWorkspace().getRoot().findMember(resource.getFullPath());
			refreshCVSFileHistoryJob.setWorkspaceFile((IFile) workspaceFile);
		}
		//if we need to refetch it's not a select only job and vice versa
		refreshCVSFileHistoryJob.setSelectOnly(selectOnly);
		refreshCVSFileHistoryJob.setRefetchHistory(refetch);
		refreshCVSFileHistoryJob.setIncludeLocals(!isLocalHistoryFilteredOut());
		refreshCVSFileHistoryJob.setIncludeRemote(!isRemoteHistoryFilteredOut());
		refreshCVSFileHistoryJob.setGrouping(groupingOn);
		refreshCVSFileHistoryJob.setRefreshFlags(refreshFlags);
		IHistoryPageSite parentSite = getHistoryPageSite();
		printDebugInfo("CVSHistoryPage#refreshHistory, about to schedule RefreshCVSFileHistoryJob", (IFile)resource, cvsFileHistory, null); //$NON-NLS-1$
		Utils.schedule(refreshCVSFileHistoryJob, getWorkbenchSite(parentSite));
	}

	private IWorkbenchPartSite getWorkbenchSite(IHistoryPageSite parentSite) {
		IWorkbenchPart part = parentSite.getPart();
		if (part != null)
			return part.getSite();
		return null;
	}

	/**
	 * Select the revision in the receiver.
	 */
	public void selectRevision(String revision) {
		IFileRevision entry = null;
		entry = getFileRevision(revision);
	
		if (entry != null) {
			IStructuredSelection selection = new StructuredSelection(entry);
			treeViewer.getTree().setRedraw(false);
			treeViewer.setSelection(selection, true);
			treeViewer.getTree().setRedraw(true);
		} else {
			//nothing to select so clear selection
			treeViewer.getTree().deselectAll();
		}
	}
	
	private IFileRevision getFileRevision(String revision) {
		if (entries != null) {
			for (IFileRevision entry : entries) {
				if (entry.getContentIdentifier().equals(revision)) {
					return entry;
				}
			}
		} else if (cvsFileHistory != null) {
			return cvsFileHistory.getFileRevision(revision);
		}
		return null;
	}
	
	/**
	 * Select the local revision in the receiver. Local revisions are differentiated by their
	 * timestamps.
	 */
	public void selectLocalRevision(long timeStamp){
		IFileRevision entry = null;
		if (entries != null) {
			for (IFileRevision e : entries) {
				if (e.getTimestamp() == timeStamp) {
					entry = e;
					break;
				}
			}
		}else if (cvsFileHistory != null) {
			IFileRevision[] tempEntries = cvsFileHistory.getFileRevisions();
			for (IFileRevision tempEntry : tempEntries) {
				if (tempEntry.getTimestamp() == timeStamp) {
					entry = tempEntry;
					break;
				}
			}
		}
	
		if (entry != null) {
			IStructuredSelection selection = new StructuredSelection(entry);
			treeViewer.getTree().setRedraw(false);
			treeViewer.setSelection(selection, true);
			treeViewer.getTree().setRedraw(true);
		} else {
			//nothing to select so clear selection
			treeViewer.getTree().deselectAll();
		}
	}
	
	protected static ICVSFile getCVSFile(Object object) {
		// First, adapt to IResource and ensure mapped to CVS
		IResource resource = Adapters.adapt(object, IResource.class);
		if (resource instanceof IFile) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
			if (provider instanceof CVSTeamProvider)
				return CVSWorkspaceRoot.getCVSFileFor((IFile) resource);
			return null;
		}
		// Second, try ICVSFile
		ICVSFile remoteFile = Adapters.adapt(object, ICVSFile.class);
		if (remoteFile != null) {
			return remoteFile;
		}
		// Next, try ICVSResource
		ICVSResource remote = Adapters.adapt(object, ICVSResource.class);
		if (remote instanceof RemoteFile) {
			return (ICVSFile)remote;
		}
		// Next, try IResourceVariant
		IResourceVariant variant = Adapters.adapt(object, IResourceVariant.class);
		if (variant instanceof RemoteFile) {
			return (ICVSFile)remote;
		}
		// Finally, try IFileRevision
		IFileRevision revision = Adapters.adapt(object, IFileRevision.class);
		if (revision instanceof CVSFileRevision) {
			return ((CVSFileRevision)revision).getCVSRemoteFile();
		}
		
		return null;
	}

	/* private */void setViewerVisibility() {
		boolean showText = toggleTextAction.isChecked();
		boolean showList = toggleListAction.isChecked();
		boolean showSearch = toggleSearchAction.isChecked();
		
		//check to see if this page is being shown in a dialog, in which case
		//don't show the text and list panes
		IHistoryPageSite parentSite = getHistoryPageSite();
		if (parentSite.isModal()){
			showText = false;
			showList = false;
		}

		
		if (showText && showList && showSearch) {
			//tree + tag + text + search
			tagAndTextComposite.setVisible(true);
			searchComposite.setVisible(true);
			sashForm.setWeights(new int[] {60, 25, 15});
			sashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(null);
			searchSashForm.setMaximizedControl(null);
		} else if (showText && showSearch) {
			//tree + text + search
			tagAndTextComposite.setVisible(true);
			searchComposite.setVisible(true);
			sashForm.setWeights(new int[] {60, 25, 15});
			sashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(textViewer.getTextWidget());
			searchSashForm.setMaximizedControl(searchField);
		} else if (showList && showSearch) {
			//tree + tag + search
			tagAndTextComposite.setVisible(true);
			searchComposite.setVisible(true);
			sashForm.setWeights(new int[] {60, 25, 15});
			sashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(tagViewer.getTable());
			searchSashForm.setMaximizedControl(searchField);
		} else if (showSearch){
			//tree + search
			tagAndTextComposite.setVisible(false);
			searchComposite.setVisible(true);
			sashForm.setWeights(new int[] {85, 0, 15});
			sashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(null);
			searchSashForm.setMaximizedControl(searchField);
		} else if (showText && showList) {
			//tree + tag + text
			tagAndTextComposite.setVisible(true);
			searchComposite.setVisible(false);
			sashForm.setWeights(new int[] {70, 30, 0});
			sashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(null);
			searchSashForm.setMaximizedControl(searchField);
		} else if (showText) {
			//tree + text
			tagAndTextComposite.setVisible(true);
			searchComposite.setVisible(false);
			sashForm.setWeights(new int[] {70, 30, 0});
			sashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(textViewer.getTextWidget());
		} else if (showList) {
			//tree + tag
			tagAndTextComposite.setVisible(true);
			searchComposite.setVisible(false);
			sashForm.setWeights(new int[] {70, 30, 0});
			sashForm.setMaximizedControl(null);
			innerSashForm.setMaximizedControl(tagViewer.getTable());
		} else {
			//tree
			tagAndTextComposite.setVisible(false);
			searchComposite.setVisible(false);
			sashForm.setMaximizedControl(treeViewer.getControl());
		}

		boolean wrapText = toggleTextWrapAction.isChecked();
		textViewer.getTextWidget().setWordWrap(wrapText);
	}

	private void initializeImages() {
		CVSUIPlugin plugin = CVSUIPlugin.getPlugin();
		versionImage = plugin.getImageDescriptor(ICVSUIConstants.IMG_PROJECT_VERSION).createImage();
		branchImage = plugin.getImageDescriptor(ICVSUIConstants.IMG_TAG).createImage();
	}
	
	@Override
	public void dispose() {
		shutdown = true;
		
		
		if (!sashForm.isDisposed() && !innerSashForm.isDisposed()) {
			saveState(); // called when switching pages
			if (disposeListener != null){
				sashForm.getParent().removeDisposeListener(disposeListener);
				disposeListener = null;
			}
		}

		if (resourceListener != null){
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
			resourceListener = null;
		}
		
		if (branchImage != null) {
			branchImage.dispose();
			branchImage = null;
		}
		if (versionImage != null) {
			versionImage.dispose();
			versionImage = null;
		}
		if (rulerSelectionListener != null) {
			rulerSelectionListener.dispose();
			rulerSelectionListener= null;
		}
		
		//Cancel any incoming
		if (refreshCVSFileHistoryJob != null) {
			if (refreshCVSFileHistoryJob.getState() != Job.NONE) {
				refreshCVSFileHistoryJob.cancel();
			}
		}

		super.dispose();
	}

	public IFileRevision getCurrentFileRevision() {
		if (currentFileRevision != null)
			return currentFileRevision;

		if (file != null) {
			try {
				//Case 1 : file is remote
				if (file instanceof RemoteFile) {
					RemoteFile remote = (RemoteFile) file;
					currentFileRevision = cvsFileHistory.getFileRevision(remote.getContentIdentifier());
					//remote.getContents(monitor);
					//currentFileRevision = new CVSFileRevision(remote.getLogEntry(monitor));
					return currentFileRevision;
				}
				//Case 2 : file is local
				//if (file.isModified(monitor)) {
				//file has been modified locally
				IFile localFile = (IFile) file.getIResource();
				if (localFile != null) {
					//make sure that there's actually a resource associated with the file
					currentFileRevision = new LocalFileRevision(localFile);
				} else {
					//no local version exists
					if (file.getSyncInfo() != null) {
						currentFileRevision = cvsFileHistory.getFileRevision(file.getSyncInfo().getRevision());
					}
				}
				return currentFileRevision;
			} catch (CVSException e) {
			}
		}

		return null;
	}
	
	private final class CVSRevisionAnnotationController extends
			RevisionAnnotationController {
		public CVSRevisionAnnotationController(IWorkbenchPage page, IFile file) {
			super(page, file, treeViewer);
		}

		public CVSRevisionAnnotationController(IWorkbenchPage page, IStorageEditorInput editorInput) {
			super(page, editorInput, treeViewer);
		}

		@Override
		protected Object getHistoryEntry(Revision selected) {
			return CVSHistoryPage.this.getFileRevision(selected.getId());
		}
	}


	private final class SearchHistoryTable implements Runnable {
		@Override
		public void run() {
			String searchString = searchField.getText();
			if (searchString.isEmpty() ||
				searchString.equals(CVSUIMessages.CVSHistoryPage_EnterSearchTerm)) {
				if (searchFilter != null)
					treeViewer.removeFilter(searchFilter);
				return;
			}
			

			if (searchFilter != null)
				treeViewer.removeFilter(searchFilter);

			searchFilter = new CVSHistorySearchFilter(searchString);

			if (historyFilter != null)
				treeViewer.removeFilter(historyFilter);

			treeViewer.addFilter(searchFilter);
		}
	}


	private class RefreshCVSFileHistory extends Job {
		private final static int NUMBER_OF_CATEGORIES = 4;
		
		private CVSFileHistory fileHistory;
		private AbstractHistoryCategory[] categories;
		private boolean grouping;
		private Object[] elementsToExpand;
		private boolean revisionsFound;
		private IFile workspaceFile;
		private CVSHistoryPage page;
		private boolean selectOnly;
		private boolean useLocalSelect;
		
		private CVSLocalFileRevision localFileRevision;

		private int refreshFlags = CVSFileHistory.REFRESH_LOCAL | CVSFileHistory.REFRESH_REMOTE;
		
		public RefreshCVSFileHistory(CVSHistoryPage page) {
			super(CVSUIMessages.HistoryView_fetchHistoryJob);
			this.page = page;
		}
		
		public void setWorkspaceFile(IFile workspaceFile) {
			this.workspaceFile = workspaceFile;
		}

		public void setIncludeLocals(boolean flag) {
			if (fileHistory != null)
				fileHistory.includeLocalRevisions(flag);
		}
		
		public void setIncludeRemote(boolean flag){
			if (fileHistory != null)
				fileHistory.includeRemoteRevisions(flag);
		}

		public void setRefetchHistory(boolean refetch) {
			if (fileHistory != null)
				fileHistory.setRefetchRevisions(refetch);
		}

		public void setFileHistory(CVSFileHistory fileHistory) {
			this.fileHistory = fileHistory;
		}
	
		public void setGrouping (boolean value){
			this.grouping = value;
		}
		
		public void setSelectOnly(boolean select) {
			this.selectOnly = select;
		}

		public void setSelectLocal(boolean localSelect) {
			this.useLocalSelect = localSelect;
		}

		public void setLocalFileRevision(CVSLocalFileRevision localRev){
			this.localFileRevision = localRev;
		}
		
		@Override
		public IStatus run(IProgressMonitor monitor)  {
			final int cachedRefreshFlags = refreshFlags;
			final boolean cachedSelectOnly= selectOnly;
			IStatus status = Status.OK_STATUS;
			printDebugInfo("RefreshCVSFileHistory#run started for", workspaceFile, fileHistory, null); //$NON-NLS-1$
			if (fileHistory != null && !shutdown) {
				printDebugInfo("RefreshCVSFileHistory#run checkpoint 1", workspaceFile, fileHistory, null); //$NON-NLS-1$

				//If fileHistory terminates in a bad way, try to fetch the local
				//revisions only
				boolean localFetched = false;
				boolean needsUpdate = true;
				if (!fileHistory.isInitialized()
						&& fileHistory.isIncludeLocal()
						&& (cachedRefreshFlags & CVSFileHistory.REFRESH_REMOTE) > 0) {
					// If this is the first refresh, show the local history before hitting the server
					try {
						printDebugInfo("RefreshCVSFileHistory#run checkpoint 2", workspaceFile, fileHistory, null); //$NON-NLS-1$
						fileHistory.refresh(CVSFileHistory.REFRESH_LOCAL, monitor);
						updateTable(cachedSelectOnly);
						localFetched = true;
						needsUpdate = false;
					} catch (TeamException e) {
						// Ignore and try the full refresh
						printDebugInfo("RefreshCVSFileHistory#run encountered an exception(1)", workspaceFile, fileHistory, e); //$NON-NLS-1$
					}
				}
				try {
					printDebugInfo("RefreshCVSFileHistory#run checkpoint 3", workspaceFile, fileHistory, null); //$NON-NLS-1$
					fileHistory.refresh(cachedRefreshFlags, monitor);
					needsUpdate = true;
				} catch (OperationCanceledException ex) {
					printDebugInfo("RefreshCVSFileHistory#run OperationCanceledException", workspaceFile, fileHistory, ex); //$NON-NLS-1$
					throw ex;
				} catch (TeamException ex) {
					printDebugInfo("RefreshCVSFileHistory#run encountered an exception(2)", workspaceFile, fileHistory, ex); //$NON-NLS-1$
					if (!localFetched) {
						try {
							fileHistory.refresh(CVSFileHistory.REFRESH_LOCAL, monitor);
							needsUpdate = true;
						} catch (TeamException e) {
							// Ignore and allow the original exception to go through
							printDebugInfo("RefreshCVSFileHistory#run encountered an exception(3)", workspaceFile, fileHistory, e); //$NON-NLS-1$
						}
					}
					status = new CVSStatus(ex.getStatus().getSeverity(), ex.getStatus().getCode(), ex.getMessage(), ex);
				}
				if (needsUpdate) {
					printDebugInfo("RefreshCVSFileHistory#run checkpoint 4", workspaceFile, fileHistory, null); //$NON-NLS-1$
					updateTable(cachedSelectOnly);
				}
			}

			if (status != Status.OK_STATUS ) {
				this.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
				this.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
			}
			printDebugInfo("RefreshCVSFileHistory#run finished, status: " + status, workspaceFile, fileHistory, null); //$NON-NLS-1$
			return status;
		}

		private void updateTable(final boolean selectOnly) {
			if (grouping)
				revisionsFound = sortRevisions();
			
			Utils.asyncExec((Runnable) () -> {
				printDebugInfo("RefreshCVSFileHistory#updateTable, in asyncExec", workspaceFile, cvsFileHistory, null); //$NON-NLS-1$
				treeViewer.refresh();
				historyTableProvider.setFile(fileHistory, workspaceFile);
				//historyTableProvider.setWorkspaceFile(workspaceFile);
				if (!selectOnly){
					if (grouping) {
						mapExpandedElements(treeViewer.getExpandedElements());
						treeViewer.getTree().setLinesVisible(revisionsFound);
						treeViewer.getTree().setRedraw(false);
						printDebugInfo("RefreshCVSFileHistory#updateTable, setInput:grouping", workspaceFile, cvsFileHistory, null); //$NON-NLS-1$
						treeViewer.setInput(categories);
						//if user is switching modes and already has expanded elements
						//selected try to expand those, else expand all
						if (elementsToExpand.length > 0)
							treeViewer.setExpandedElements(elementsToExpand);
						else {
							treeViewer.expandAll();
							Object[] el = treeViewer.getExpandedElements();
							if (el != null && el.length > 0) {
								treeViewer.setSelection(new StructuredSelection(el[0]));
								treeViewer.getTree().deselectAll();
							}
						}
						treeViewer.getTree().setRedraw(true);
					} else {
						if (fileHistory.getFileRevisions().length > 0) {
							treeViewer.getTree().setLinesVisible(true);
							printDebugInfo("RefreshCVSFileHistory#updateTable, setInput:no grouping", workspaceFile, cvsFileHistory, null); //$NON-NLS-1$
							treeViewer.setInput(fileHistory);
						} else {
							categories = new AbstractHistoryCategory[] {getErrorMessage()};
							treeViewer.getTree().setLinesVisible(false);
							treeViewer.setInput(categories);
						}
					}
				}
				//Update the history (if it exists) to reflect the new
				//counts
				if (historyFilter != null){
					CVSHistoryFilter tempFilter = new CVSHistoryFilter(historyFilter.branchName, historyFilter.author, historyFilter.comment, historyFilter.fromDate, historyFilter.toDate, historyFilter.isOr);
					showFilter(tempFilter);
				}
				
				//Select the current file if we didn't have to refetch the history
				if (file != null){
					try {
						if (useLocalSelect){
							page.selectLocalRevision(localFileRevision.getTimestamp());
						} else {
							byte[] syncBytes = file.getSyncBytes();
							if (syncBytes != null) {
								String workspaceRevision = ResourceSyncInfo.getRevision(syncBytes);
								page.selectRevision(workspaceRevision);
							}
						}
					} catch (CVSException e){
					}
				}
			}, treeViewer);
		}

		private void mapExpandedElements(Object[] expandedElements) {
			//store the names of the currently expanded categories in a map
			HashMap elementMap = new HashMap();
			for (Object expandedElement : expandedElements) {
				elementMap.put(((DateHistoryCategory) expandedElement).getName(), null);
			}
			
			//Go through the new categories and keep track of the previously expanded ones
			ArrayList expandable = new ArrayList();
			for (AbstractHistoryCategory category : categories) {
				//check to see if this category is currently expanded
				if (elementMap.containsKey(category.getName())) {
					expandable.add(category);
				}
			}
			
			elementsToExpand = new Object[expandable.size()];
			elementsToExpand = expandable.toArray(new Object[expandable.size()]);
		}

		private boolean sortRevisions() {
			IFileRevision[] fileRevision = fileHistory.getFileRevisions();
			
			//Create the 4 categories
			DateHistoryCategory[] tempCategories = new DateHistoryCategory[NUMBER_OF_CATEGORIES];
			//Get a calendar instance initialized to the current time
			Calendar currentCal = Calendar.getInstance();
			tempCategories[0] = new DateHistoryCategory(CVSUIMessages.CVSHistoryPage_Today, currentCal, null);
			//Get yesterday
			Calendar yesterdayCal = Calendar.getInstance();
			yesterdayCal.roll(Calendar.DAY_OF_YEAR, -1);
			tempCategories[1] = new DateHistoryCategory(CVSUIMessages.CVSHistoryPage_Yesterday, yesterdayCal, null);
			//Get this month
			Calendar monthCal = Calendar.getInstance();
			monthCal.set(Calendar.DAY_OF_MONTH, 1);
			tempCategories[2] = new DateHistoryCategory(CVSUIMessages.CVSHistoryPage_ThisMonth, monthCal, yesterdayCal);
			//Everything before this month is previous
			tempCategories[3] = new DateHistoryCategory(CVSUIMessages.CVSHistoryPage_Previous, null, monthCal);
		
			ArrayList finalCategories = new ArrayList();
			for (int i = 0; i<NUMBER_OF_CATEGORIES; i++){
				tempCategories[i].collectFileRevisions(fileRevision, false);
				if (tempCategories[i].hasRevisions())
					finalCategories.add(tempCategories[i]);
			}
			
			//Assume that some revisions have been found
			boolean revisionsFound = true;
			
			if (finalCategories.isEmpty()){
				//no revisions found for the current mode, so add a message category
				finalCategories.add(getErrorMessage());
				revisionsFound = false;
			}
			
			categories = (AbstractHistoryCategory[])finalCategories.toArray(new AbstractHistoryCategory[finalCategories.size()]);
			return revisionsFound;
		}
		
		private MessageHistoryCategory getErrorMessage(){
			String message = ""; //$NON-NLS-1$
			switch(currentFilerMode){
				case LOCAL_MODE:
				message = CVSUIMessages.CVSHistoryPage_LocalModeTooltip;
				break;
				
				case REMOTE_MODE:
				message = CVSUIMessages.CVSHistoryPage_RemoteModeTooltip;
				break;
				
				case REMOTE_LOCAL_MODE:
				message = CVSUIMessages.CVSHistoryPage_NoRevisions;
				break;
			}
		
			MessageHistoryCategory messageCategory = new MessageHistoryCategory(NLS.bind(CVSUIMessages.CVSHistoryPage_NoRevisionsForMode, new String[] { message }));
			return messageCategory;
		}

		public void setRefreshFlags(int refreshFlags) {
			this.refreshFlags = refreshFlags;
		}

	
		
	}
	
	
	/**
	 * A default content provider to prevent subclasses from
	 * having to implement methods they don't need.
	 */
	private class SimpleContentProvider implements IStructuredContentProvider {

		/**
		 * SimpleContentProvider constructor.
		 */
		public SimpleContentProvider() {
			super();
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object element) {
			return new Object[0];
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private class HistoryResourceListener implements IResourceChangeListener {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta root = event.getDelta();
			//Safety check for non-managed files that are added with the CVSHistoryPage
			//in view
			if (file == null ||	file.getIResource() == null)
				return;
			
			IResourceDelta resourceDelta = root.findMember(((IFile)file.getIResource()).getFullPath());
			if (resourceDelta != null){
				String revision = getRevision();
				final boolean hasRevision = cvsFileHistory.getFileRevision(revision) != null;
				Display.getDefault().asyncExec(() -> {
					if (treeViewer.getControl().isDisposed())
						return;
					if (treeViewer.getControl().isVisible()) {
						if (hasRevision)
							refresh(CVSFileHistory.REFRESH_LOCAL);
						else
							refresh();
					} else {
						refreshRequest = hasRevision ? CVSFileHistory.REFRESH_LOCAL : CVSFileHistory.REFRESH_LOCAL | CVSFileHistory.REFRESH_REMOTE;
					}
				});
			}
		}

		private String getRevision() {
			try {
				byte[] syncBytes = file.getSyncBytes();
				if (syncBytes != null && !ResourceSyncInfo.isAddition(syncBytes)) {
					return ResourceSyncInfo.getRevision(syncBytes);
				}
			} catch (CVSException e) {
				// Ignore the errors
			}
			return null;
		}
	}
		
	@Override
	public Control getControl() {
		return sashForm;
	}

	@Override
	public boolean isValidInput(Object object) {
		return getCVSFile(object) != null;
	}

	@Override
	public String getName() {
		if (description != null)
			return description;
		if (file != null)
			return file.getName();
		
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * Returns the path of the file currently being shown in the CVS History Page.
	 * @return an IPath or <code>null</code> if the file is null
	 */
	public IPath getFilePath() {
		if (file != null)
			return file.getIResource().getFullPath();
		
		return null;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if(adapter == IHistoryCompareAdapter.class) {
			return adapter.cast(this);
		}
		return null;
	}

	@Override
	public ICompareInput getCompareInput(Object object) {
		if (object instanceof IFileRevision){
			IFileRevision selectedFileRevision = (IFileRevision)object;
			if (fileElement == null)
				fileElement = SaveableCompareEditorInput.createFileElement((IFile) file.getIResource());
			FileRevisionTypedElement right = new FileRevisionTypedElement(selectedFileRevision, getLocalEncoding());
			DiffNode node = new DiffNode(fileElement, right);
			return node;
		}
		return null;
	}

	private String getLocalEncoding() {
		IResource resource = file.getIResource();
		if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			try {
				return file.getCharset();
			} catch (CoreException e) {
				CVSUIPlugin.log(e);
			}
		}
		return null;
	}

	public void setClickAction(boolean compare) {
		//toggleCompareAction is going to switch the mode
		//so make sure that we're in the appropriate mode before
		compareMode = !compare;
		compareModeAction.run();
	}

	@Override
	public void prepareInput(ICompareInput input, CompareConfiguration configuration, IProgressMonitor monitor) {
		initLabels(input, configuration);
		// TODO: pre-fetch contents
	}
	
	private void initLabels(ICompareInput input, CompareConfiguration cc) {
		String leftLabel = getFileRevisionLabel(input.getLeft(), cc);
		cc.setLeftLabel(leftLabel);
		String rightLabel = getFileRevisionLabel(input.getRight(), cc);
		cc.setRightLabel(rightLabel);
	}

	private String getFileRevisionLabel(ITypedElement element, CompareConfiguration cc) {
		String label = null;

		if (element instanceof IEditableContent) {
			//current revision
			if (element instanceof IModificationDate) {
				IModificationDate md = (IModificationDate) element;
				Date dateFromLong = new Date(md.getModificationDate());
				label = NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_workspace, new Object[]{ element.getName(), getDateTimeFormat().format(dateFromLong)});
			} else {
				label = element.getName();
			}
			return label;

		} else if (element instanceof FileRevisionTypedElement) {
			Object fileObject = ((FileRevisionTypedElement) element).getFileRevision();

			if (fileObject instanceof LocalFileRevision) {
				try {
					IStorage storage = ((LocalFileRevision) fileObject).getStorage(new NullProgressMonitor());
					if (Adapters.adapt(storage, IFileState.class) != null) {
						//local revision
						label = NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_localRevision, new Object[]{element.getName(), ((FileRevisionTypedElement) element).getTimestamp()});
					}
				} catch (CoreException e) {
				}
			} else {
				label = NLS.bind(
						TeamUIMessages.CompareFileRevisionEditorInput_repository,
						new Object[]{
								element.getName(),
								((FileRevisionTypedElement) element).getContentIdentifier(),
								((FileRevisionTypedElement) element).getAuthor()
								});
			}
		}
		return label;
	}
	
	private synchronized DateFormat getDateTimeFormat() {
		if (dateTimeFormat == null)
			dateTimeFormat = DateFormat.getDateTimeInstance();
		return dateTimeFormat;
	}

	@Override
	public String getDescription() {
		try {
			if (file != null)
				return file.getRepositoryRelativePath();
		} catch (CVSException e) {
			// Ignore
		}
		return null;
	}

	@Override
	public boolean inputSet() {
		//reset currentFileRevision
		currentFileRevision = null;
		Object inputObj = getInput();
	
		ICVSFile cvsFile = getCVSFile(inputObj);
		if (cvsFile == null)
			return false;
		
		this.file = cvsFile;
		fileElement = null;

		// if this input is the same as the last, we don't need to cancel
		// the current job
		boolean needRefresh = checkPreviousInput();
		printDebugInfo("CVSHistoryPage#inputSet, needRefresh = " + needRefresh, (IFile)cvsFile.getIResource(), cvsFileHistory, null); //$NON-NLS-1$
		if (refreshCVSFileHistoryJob != null) {
			if (!needRefresh && refreshCVSFileHistoryJob.getState() != Job.NONE) {
				// let the old job finish
				printDebugInfo("CVSHistoryPage#inputSet, the old job is still running", (IFile)cvsFile.getIResource(), cvsFileHistory, null); //$NON-NLS-1$
				return true;
			} else {
				// cancel the old job and continue
				printDebugInfo("CVSHistoryPage#inputSet, cancel the old job", (IFile)cvsFile.getIResource(), cvsFileHistory, null); //$NON-NLS-1$
				refreshCVSFileHistoryJob.cancel();
			}
		}
		
		refreshCVSFileHistoryJob = new RefreshCVSFileHistory(this);
		
		//if the input is a local file revision, pass it to the refresh job to
		//allow the refresh job to use it to match the time stamp of the local
		//files displayed in the history page
		if (inputObj instanceof CVSLocalFileRevision){
			refreshCVSFileHistoryJob.setLocalFileRevision((CVSLocalFileRevision) inputObj);
		} else if (inputObj instanceof IFile) {
			refreshCVSFileHistoryJob.setLocalFileRevision(new CVSLocalFileRevision((IFile) inputObj));
		}
		//let the refresh job know which flavour of select to use (ie. select CVSFileRevisions
		//or CVSLocalFileRevision)
		refreshCVSFileHistoryJob.setSelectLocal(inputObj instanceof CVSLocalFileRevision || inputObj instanceof IFile);
		
		
		//If the file history doesn't need to be refreshed, we can just
		//use the previous input file history
		if (needRefresh){
			cvsFileHistory = new CVSFileHistory(cvsFile);
			//fetch both local and remote revisions the first time around
			cvsFileHistory.includeLocalRevisions(true);
			//blank current input only after we're sure that we have a file
			//to fetch history for
			this.treeViewer.setInput(null);
			
			linkWithEditor();
		}
		
		//always refresh the history if the input gets set - in which
		//case set the selectOnly to false
		refreshHistory(needRefresh, !needRefresh, CVSFileHistory.REFRESH_LOCAL | CVSFileHistory.REFRESH_REMOTE);
		
		if (toggleSearchAction!= null && toggleSearchAction.isChecked()){
			searchField.selectAll();
		}
		
		return true;
	}
	

	
	/**
	 * @param page the workbench page that view and editor are contained in
	 * @param editor the editor to link to the history view
	 * @param historyView the history view to link to the editor
	 */
	public void linkWithEditor() {
		if (rulerSelectionListener != null) {
			rulerSelectionListener.dispose();
			rulerSelectionListener= null;
		}

		if (!getHistoryPageSite().isModal()) {
			IResource resource = file.getIResource();
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				rulerSelectionListener= new CVSRevisionAnnotationController(getHistoryPageSite().getWorkbenchPageSite().getPage(), file);
			} else {
				Object input = getInput();
				if (input instanceof IStorageEditorInput) {
					IStorageEditorInput editorInput = (IStorageEditorInput) input;
					rulerSelectionListener= new CVSRevisionAnnotationController(getHistoryPageSite().getWorkbenchPageSite().getPage(), editorInput);
				}
			}
		}
	}
	
	/*
	 * Check to see if we need to refresh the input; if the previous file
	 * that was being shown
	 */
	private boolean checkPreviousInput() {
		
		if (previousFile != null){
			try {
				if (isSameRemote(file, previousFile)
					&& (isSameLocalFile(file, previousFile)
							|| (!isLocal(file) && isLocal(previousFile)))) {
					return false;
				}
			} catch (CVSException e) {
			}
		}
		
		//set previous file to current file
		previousFile = file;
		return true;
	}

	private boolean isLocal(ICVSFile file) {
		return file.getIResource() != null;
	}

	private boolean isSameLocalFile(ICVSFile file, ICVSFile previousFile) {
		IResource r1 = file.getIResource();
		IResource r2 = previousFile.getIResource();
		return r1 != null && r2 != null && r1.equals(r2);
	}

	private boolean isSameRemote(ICVSFile file, ICVSFile previousFile) throws CVSException {
		String path = file.getRepositoryRelativePath();
		String previousPath = previousFile.getRepositoryRelativePath();
		
		//Could be comparing two local files with no remotes
		if (path == null && previousPath == null)
			return true;
		
		return (path != null && previousPath != null && path.equals(previousPath) && isSameRepository(file.getParent(), previousFile.getParent()));
	}


	private boolean isSameRepository(ICVSFolder parent1, ICVSFolder parent2) {
		try {
			FolderSyncInfo info1 = parent1.getFolderSyncInfo();
			FolderSyncInfo info2 = parent2.getFolderSyncInfo();
			return (info1 != null && info2 != null && info1.getRemoteLocation().equals(info2.getRemoteLocation()));
		} catch (CVSException e) {
			// Ignore
		}
		return false;
	}

	private void updateFilterMode(int mode) {
		currentFilerMode=mode;
		switch(mode){
			case LOCAL_MODE:
				localFilteredOut = false;
				remoteFilteredOut = true;
				localMode.setChecked(true);
				remoteMode.setChecked(false);
				remoteLocalMode.setChecked(false);
				break;

			case REMOTE_MODE:
				localFilteredOut = true;
				remoteFilteredOut = false;
				localMode.setChecked(false);
				remoteMode.setChecked(true);
				remoteLocalMode.setChecked(false);
				break;

			case REMOTE_LOCAL_MODE:
				localFilteredOut = false;
				remoteFilteredOut = false;
				localMode.setChecked(false);
				remoteMode.setChecked(false);
				remoteLocalMode.setChecked(true);
				break;
		}

		//the refresh job gets created once the input is set
		//don't bother trying to refresh any history until the input has been set
		if (refreshCVSFileHistoryJob != null){
			//don't refetch, but not a select only job (ie. have to get the
			//existing revisions corresponding to the mode change)
			refreshHistory(false, false, CVSFileHistory.REFRESH_LOCAL | CVSFileHistory.REFRESH_REMOTE);
		}
	}

	public TreeViewer getTreeViewer() {
		return treeViewer;
	}

	public void showFilter(CVSHistoryFilter filter) {
		if (historyFilter != null)
			treeViewer.removeFilter(historyFilter);
		
		historyFilter = filter;
		int before = cvsFileHistory.getFileRevisions().length;
		treeViewer.addFilter(historyFilter);
		IHistoryPageSite historyPageSite =getHistoryPageSite();
		if (historyPageSite instanceof WorkbenchHistoryPageSite){
			IWorkbenchPart part = ((WorkbenchHistoryPageSite) historyPageSite).getPart();
			if (part instanceof GenericHistoryView){
				String revisions = NLS.bind(CVSUIMessages.CVSHistoryPage_FilterOnMessage, new Object[]{Integer.valueOf(historyFilter.getMatchCount()),Integer.valueOf(before)});
				String old = getName();
				description = NLS.bind(CVSUIMessages.CVSHistoryPage_FilterDescription, new Object[]{file.getName(), revisions});
				CVSHistoryPage.this.firePropertyChange(CVSHistoryPage.this, P_NAME, old, getName());
			}
		}
		toggleFilterAction.setEnabled(true);
	}
	
	/*
	 * Sets the filter mode for the page.
	 * param flag	LOCAL_MODE, REMOTE_MODE, REMOTE_LOCAL_MODE
	 */
	public void setMode(int flag){
		switch(flag){
			case LOCAL_MODE:
				localMode.setChecked(true);
				localMode.run();
				break;
			
			case REMOTE_MODE:
				remoteMode.setChecked(true);
				remoteMode.run();
				break;
				
			case REMOTE_LOCAL_MODE:
				remoteLocalMode.setChecked(true);
				remoteLocalMode.run();
				break;
		}
		
		//refetch revisions, not a select only job
		refreshHistory(true, false, CVSFileHistory.REFRESH_LOCAL | CVSFileHistory.REFRESH_REMOTE);
	}

	/**
	 * Save any changes that are buffered in the pages typed element.
	 * @param monitor a progress monitor.
	 * @throws CoreException
	 */
	public void saveChanges(IProgressMonitor monitor) throws CoreException {
		if (fileElement instanceof LocalResourceTypedElement) {
			LocalResourceTypedElement element = (LocalResourceTypedElement) fileElement;
			element.commit(monitor);
		}
	}

	private void setStatusLineMessage(String message) {
		IPageSite workbenchPageSite = getHistoryPageSite().getWorkbenchPageSite();
		if (workbenchPageSite != null) {
			workbenchPageSite.getActionBars().getStatusLineManager().setMessage(message);
		}
	}

	private static void printDebugInfo(String message, IFile file, CVSFileHistory history, Throwable t) {
		if (!Policy.DEBUG_HISTORY)
			return;
		String time= new SimpleDateFormat("m:ss.SSS").format(new Date(System.currentTimeMillis())); //$NON-NLS-1$
		String fileName= file != null ? file.getName() : "<workspaceFile == null>"; //$NON-NLS-1$
		String fileHistoryID= history != null ? history.toString() : "<fileHistory == null>"; //$NON-NLS-1$
		int i= fileHistoryID.indexOf('@');
		if (i != -1)
			fileHistoryID= fileHistoryID.substring(i);
		System.out.println(time + ": " + fileName + ", " + fileHistoryID + ": " + message); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (t != null)
			t.printStackTrace();
	}
}
