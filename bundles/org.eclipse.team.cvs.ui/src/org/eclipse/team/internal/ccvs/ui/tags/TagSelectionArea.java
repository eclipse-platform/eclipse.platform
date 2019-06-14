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
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 182267 "Add Date..." button shouldn't be visible in merge wizard
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.*;
import java.util.List;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.repo.NewDateTagAction;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.tags.TagSourceWorkbenchAdapter.ProjectElementComparator;
import org.eclipse.team.internal.ui.PixelConverter;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.internal.ui.dialogs.DialogArea;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.PageBook;

/**
 * A dialog area that displays a list of tags for selection and supports
 * filtering of the displayed tags.
 */
public class TagSelectionArea extends DialogArea {
	
	private static int COLUMN_TRIM = "carbon".equals(SWT.getPlatform()) ? 24 : 3; //$NON-NLS-1$
	
	private static int ICON_WIDTH = 40; 
	
	/*
	 * Please see bug 184660
	 */
	private static final int SAFETY_MARGIN = 50;
	
	/*
	 * Property constant which identifies the selected tag or
	 * null if no tag is selected
	 */
	public static final String SELECTED_TAG = "selectedTag"; //$NON-NLS-1$
	
	/*
	 * Property constant which indicates that a tag has been selected in such 
	 * a way as to indicate that this is the desired tag (e.g double-click)
	 */
	public static final String OPEN_SELECTED_TAG = "openSelectedTag";  //$NON-NLS-1$
	
	/*
	 * Constants used to configure which tags are shown
	 */
	public static final int INCLUDE_HEAD_TAG = TagSourceWorkbenchAdapter.INCLUDE_HEAD_TAG;
	public static final int INCLUDE_BASE_TAG = TagSourceWorkbenchAdapter.INCLUDE_BASE_TAG;
	public static final int INCLUDE_BRANCHES = TagSourceWorkbenchAdapter.INCLUDE_BRANCHES;
	public static final int INCLUDE_VERSIONS = TagSourceWorkbenchAdapter.INCLUDE_VERSIONS;
	public static final int INCLUDE_DATES = TagSourceWorkbenchAdapter.INCLUDE_DATES;
	public static final int INCLUDE_ALL_TAGS = TagSourceWorkbenchAdapter.INCLUDE_ALL_TAGS;
	
	private String tagAreaLabel;
	private final int includeFlags;
	private CVSTag selection;
	private String helpContext;
	private Text filterText;
	private TagSource tagSource;
	private final Shell shell;
	private TagRefreshButtonArea tagRefreshArea;
	private final TagSource.ITagSourceChangeListener listener = source -> {
		Shell shell = getShell();
		if (!shell.isDisposed()) {
			shell.getDisplay().syncExec(() -> refresh());
		}
	};
	private final DisposeListener disposeListener = e -> {
		if (tagSource != null)
			tagSource.removeListener(listener);
	};

	private PageBook switcher;
	private TreeViewer tagTree;
	private TableViewer tagTable;
	private boolean treeVisible = true;
	private boolean includeFilterInputArea = true;
	private String filterPattern = ""; //$NON-NLS-1$

	private IRunnableContext context;
	
	public TagSelectionArea(Shell shell, TagSource tagSource, int includeFlags, String helpContext) {
		this.shell = shell;
		this.includeFlags = includeFlags;
		this.helpContext = helpContext;
		this.tagSource = tagSource;
		setSelection(null);
	}

	@Override
	public void createArea(Composite parent) {
		initializeDialogUnits(parent);
		Dialog.applyDialogFont(parent);
		final PixelConverter converter= new PixelConverter(parent);
		
		// Create a composite for the entire area
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayoutData(SWTUtils.createHVFillGridData());
		composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_NONE));
		
		// Add F1 help
		if (helpContext != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, helpContext);
		}
		
		// Create the tree area and refresh buttons with the possibility to add stuff in between
		createTagDisplayArea(composite);	
		createCustomArea(composite);
		createRefreshButtons(composite);
		
		Dialog.applyDialogFont(parent);
		updateTagDisplay(true);
	}

	private void createTagDisplayArea(Composite parent) {
		Composite inner = createGrabbingComposite(parent, 1);
		if (isIncludeFilterInputArea()) {
			createFilterInput(inner);
			createWrappingLabel(inner, CVSUIMessages.TagSelectionArea_0, 1); 
		} else {
			createWrappingLabel(inner, NLS.bind(CVSUIMessages.TagSelectionArea_1, new String[] { getTagAreaLabel() }), 1);  
		}
		switcher = new PageBook(inner, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 0;
		gridData.widthHint = 0;
		switcher.setLayoutData(gridData);
		tagTree = createTree(switcher);
		tagTable = createTable(switcher);
	}

	private void createFilterInput(Composite inner) {
		createWrappingLabel(inner, NLS.bind(CVSUIMessages.TagSelectionArea_2, new String[] { getTagAreaLabel() }), 1); 
		filterText = createText(inner, 1);
		filterText.addModifyListener(e -> setFilter(filterText.getText()));
		filterText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN && e.stateMask == 0) {			
					tagTable.getControl().setFocus();
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {
				// Ignore
			}
		});
	}

	/**
	 * Return the label that should be used for the tag area.
	 * It should not have any trailing punctuations as the tag area
	 * may position it differently depending on whether the filter
	 * text input is included in the area.
	 * @return the tag area label
	 */
	public String getTagAreaLabel() {
		if (tagAreaLabel == null)
			tagAreaLabel = CVSUIMessages.TagSelectionArea_3; 
		return tagAreaLabel;
	}

	/**
	 * Set the label that should be used for the tag area.
	 * It should not have any trailing punctuations as the tag area
	 * may position it differently depending on whether the filter
	 * text input is included in the area.
	 * @param tagAreaLabel the tag area label
	 */
	public void setTagAreaLabel(String tagAreaLabel) {
		this.tagAreaLabel = tagAreaLabel;
	}
	
	/**
	 * Update the tag display to show the tags that match the
	 * include flags and the filter entered by the user.
	 */
	protected void updateTagDisplay(boolean firstTime) {
		String filter = getFilterString();
		if ((filter != null && filter.length() > 0) || isTableOnly()) {
			// Show the table and filter it accordingly
			try {
				switcher.setRedraw(false);
				treeVisible = false;
				switcher.showPage(tagTable.getControl());
				FilteredTagList list = (FilteredTagList)tagTable.getInput();
				list.setPattern(filter);
				tagTable.refresh();
				int maxWidth = getMaxWidth(list.getChildren(null));
				if (maxWidth > 0) {
					maxWidth = maxWidth + ICON_WIDTH + COLUMN_TRIM + SAFETY_MARGIN; /* space for the tag icon */
					tagTable.getTable().getColumn(0).setWidth(maxWidth);
				}
				if (filterText == null || filter == null || filter.length() == 0) {
					setSelection(selection);
				} else {
					// Only set the top selection if there is a filter from the filter text
					// of this area. This is done to avoid selection loops
					selectTopElement();
				}
			} finally {
				switcher.setRedraw(true);
			}
		} else {
			// Show the tree
			if (!isTreeVisible() || firstTime) {
				try {
					switcher.setRedraw(false);
					treeVisible = true;
					switcher.showPage(tagTree.getControl());
					tagTree.refresh();
					setSelection(selection);
				} finally {
					switcher.setRedraw(true);
				}
			}
		}
	}
	
	private int getMaxWidth(Object[] children) {
		PixelConverter converter = new PixelConverter(tagTable.getTable());
		int maxWidth = 0;
		for (Object object : children) {
			if (object instanceof TagElement) {
				TagElement tag = (TagElement) object;
				int width = tag.getTag().getName().length();
				if (width > maxWidth) {
					maxWidth = width;
				}
			}
		}
		return converter.convertWidthInCharsToPixels(maxWidth);
	}

	/**
	 * Return whether only a table should be used
	 * @return whether only a table should be used
	 */
	protected boolean isTableOnly() {
		return (includeFlags == INCLUDE_VERSIONS) || (includeFlags == INCLUDE_BRANCHES);
	}

	private String getFilterString() {
		return filterPattern;
	}

	/*
	 * Select the top element in the tag table
	 */
	private void selectTopElement() {
		if (tagTable.getTable().getItemCount() > 0) {
			TableItem item = tagTable.getTable().getItem(0);
			tagTable.getTable().setSelection(new TableItem[] { item });
			tagTable.setSelection(tagTable.getSelection());
		}   
	}

	private FilteredTagList createFilteredInput() {
		return new FilteredTagList(tagSource, TagSource.convertIncludeFlaqsToTagTypes(includeFlags));
	}
	
	private Text createText(Composite parent, int horizontalSpan) {
		Text text = new Text(parent, SWT.SEARCH);
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.widthHint= 0;
		text.setLayoutData(data);
		return text;
	}

	protected void createRefreshButtons(Composite parent) {
		tagSource.addListener(listener);
		parent.addDisposeListener(disposeListener);
		Listener listener = null;
		if ((includeFlags & TagSourceWorkbenchAdapter.INCLUDE_DATES) != 0) {
			listener = event -> {
				CVSTag dateTag = NewDateTagAction.getDateTag(getShell(), getLocation());
				addDateTag(dateTag);
			};
		}
		tagRefreshArea = new TagRefreshButtonArea(shell, tagSource, listener);
		if (context != null)
			tagRefreshArea.setRunnableContext(context);
		tagRefreshArea.createArea(parent);
	}

	protected void createTreeMenu(TreeViewer tagTree) {
		if ((includeFlags & TagSourceWorkbenchAdapter.INCLUDE_DATES) != 0) {
			// Create the popup menu
			MenuManager menuMgr = new MenuManager();
			Tree tree = tagTree.getTree();
			Menu menu = menuMgr.createContextMenu(tree);
			menuMgr.addMenuListener(manager -> addMenuItemActions(manager));
			menuMgr.setRemoveAllWhenShown(true);
			tree.setMenu(menu);
		}
	}

	/**
	 * Create aq custom area that is below the tag selection area but above the refresh busson group
	 * @param parent
	 */
	protected void createCustomArea(Composite parent) {
		// No default custom area
	}
	
	protected TreeViewer createTree(Composite parent) {
		Tree tree = new Tree(parent, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(data);
		TreeViewer result = new TreeViewer(tree);
		initialize(result);
		result.getControl().addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent event) {
				handleKeyPressed(event);
			}
			@Override
			public void keyReleased(KeyEvent event) {
				handleKeyReleased(event);
			}
		});
		result.setInput(createUnfilteredInput());
		createTreeMenu(result);
		return result;
	}

	protected TableViewer createTable(Composite parent) {
		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(data);
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100));
		table.setLayout(layout);
		new TableColumn(table, SWT.NONE);
		TableViewer viewer = new TableViewer(table);
		initialize(viewer);
		viewer.setInput(createFilteredInput());
		return viewer;

	}

	private void initialize(StructuredViewer viewer) {
		viewer.setContentProvider(new WorkbenchContentProvider());
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.setComparator(new ProjectElementComparator());
		viewer.addSelectionChangedListener(event -> handleSelectionChange());
		// select and close on double click
		// To do: use defaultselection instead of double click
		viewer.getControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				CVSTag tag = internalGetSelectedTag();
				if (tag != null) {
					firePropertyChangeChange(OPEN_SELECTED_TAG, null, tag);
				}
			}
		});
	}

	private Object createUnfilteredInput() {
		return TagSourceWorkbenchAdapter.createInput(tagSource, includeFlags);
	}

	public void handleKeyPressed(KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0) {			
			deleteDateTag();
		}
	}
	private void deleteDateTag() {
		TagElement[] selectedDateTagElements = getSelectedDateTagElement();
		if (selectedDateTagElements.length == 0) return;
		for (TagElement selectedDateTagElement : selectedDateTagElements) {
			RepositoryManager mgr = CVSUIPlugin.getPlugin().getRepositoryManager();
			CVSTag tag = selectedDateTagElement.getTag();
			if(tag.getType() == CVSTag.DATE){
				mgr.removeDateTag(getLocation(),tag);
			}				
		}
		tagTree.refresh();
		handleSelectionChange();
	}
	
	/**
	 * Returns the selected date tag elements
	 */
	private TagElement[] getSelectedDateTagElement() {
		ArrayList<Object> dateTagElements = null;
		IStructuredSelection selection = tagTree.getStructuredSelection();
		if (selection!=null && !selection.isEmpty()) {
			dateTagElements = new ArrayList<>();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = TeamAction.getAdapter(elements.next(), TagElement.class);
				if (next instanceof TagElement) {
					if(((TagElement)next).getTag().getType() == CVSTag.DATE){
						dateTagElements.add(next);
					}
				}
			}
		}
		if (dateTagElements != null && !dateTagElements.isEmpty()) {
			TagElement[] result = new TagElement[dateTagElements.size()];
			dateTagElements.toArray(result);
			return result;
		}
		return new TagElement[0];
	}
	private void addDateTag(CVSTag tag){
		if(tag == null) return;
		List<CVSTag> dateTags = new ArrayList<>();
		ICVSRepositoryLocation location = getLocation();
		dateTags.addAll(Arrays.asList(CVSUIPlugin.getPlugin().getRepositoryManager().getDateTags(location)));
		if(!dateTags.contains( tag)){
			CVSUIPlugin.getPlugin().getRepositoryManager().addDateTag(location, tag);
		}
		try {
			tagTree.getControl().setRedraw(false);
			tagTree.refresh();
			setSelection(tag);
		} finally {
			tagTree.getControl().setRedraw(true);
		}
		handleSelectionChange();
	}
	private void addMenuItemActions(IMenuManager manager) {
		manager.add(new Action(CVSUIMessages.TagSelectionDialog_0) { 
			@Override
			public void run() {
				CVSTag dateTag = NewDateTagAction.getDateTag(getShell(), getLocation());
				addDateTag(dateTag);
			}
		});
		if(getSelectedDateTagElement().length > 0){
			manager.add(new Action(CVSUIMessages.TagSelectionDialog_1) { 
				@Override
				public void run() {
					deleteDateTag();
				}
			});			
		}
	}

	protected void handleKeyReleased(KeyEvent event) {
	}
	
	/**
	 * handle a selection change event from the visible tag display
	 * (which could be either the table or the tree).
	 */
	protected void handleSelectionChange() {
		CVSTag newSelection = internalGetSelectedTag();
		if (selection != null && newSelection != null && selection.equals(newSelection)) {
			// the selection hasn't change so return
			return;
		}
		CVSTag oldSelection = selection;
		selection = newSelection;
		firePropertyChangeChange(SELECTED_TAG, oldSelection, selection);
	}
	
	private CVSTag internalGetSelectedTag() {
		IStructuredSelection selection;
		if (isTreeVisible()) {
			selection = tagTree.getStructuredSelection();
		} else {
			selection = tagTable.getStructuredSelection();
		}
		Object o = selection.getFirstElement();
		if (o instanceof TagElement)
			return ((TagElement)o).getTag();
		return null;
	}
	
	private boolean isTreeVisible() {
		return treeVisible;
	}

	private ICVSRepositoryLocation getLocation(){
		return tagSource.getLocation();
	}
	public CVSTag getSelection() {
		return selection;
	}
	public Shell getShell() {
		return shell;
	}

	/**
	 * Set the focus to the filter text widget
	 */
	public void setFocus() {
		if (filterText != null)
			filterText.setFocus();
		else if (switcher != null)
			switcher.setFocus();
			
		// Refresh in case tags were added since the last time the area had focus
		refresh();
	}

	/**
	 * Select the given tag
	 * @param selectedTag the tag to be selected
	 */
	public void setSelection(CVSTag selectedTag) {
		if (isTreeVisible())
			if (tagTree != null && !tagTree.getControl().isDisposed()) {
				// TODO: Hack to instantiate the model before revealing the selection
				tagTree.expandToLevel(2);
				tagTree.collapseAll();
				// Reveal the selection
				tagTree.reveal(new TagElement(selectedTag));
				tagTree.setSelection(new StructuredSelection(new TagElement(selectedTag)));
			}
		else
			if (tagTable != null && !tagTable.getControl().isDisposed()) {
				tagTable.setSelection(new StructuredSelection(new TagElement(selectedTag)));
			}
	}

	/**
	 * Refresh the state of the tag selection area
	 */
	public void refresh() {
		if (isTreeVisible()) {
			if (tagTree != null && !tagTree.getControl().isDisposed()) {
				tagTree.refresh();
			}
		} else {
			if (tagTable != null && !tagTable.getControl().isDisposed()) {
				tagTable.refresh();
			}
		}
	}
	
	public void refreshTagList() {
		tagRefreshArea.refresh(true);
	}

	/**
	 * Set the enablement state of the area
	 * @param enabled the enablement state
	 */
	public void setEnabled(boolean enabled) {
		if (filterText != null)
			filterText.setEnabled(enabled);
		tagTree.getControl().setEnabled(enabled);
		tagTable.getControl().setEnabled(enabled);
	}
	
	/**
	 * Set the tag source from which the displayed tags are determined
	 * @param tagSource the source of the tags being displayed
	 */
	public void setTagSource(TagSource tagSource) {
		if (this.tagSource != null) {
			this.tagSource.removeListener(listener);
		}
		this.tagSource = tagSource;
		this.tagSource.addListener(listener);
		tagRefreshArea.setTagSource(this.tagSource);
		setTreeAndTableInput();
	}

	private void setTreeAndTableInput() {
		if (tagTree != null) {
			tagTree.setInput(createUnfilteredInput());
		}
		if (tagTable != null) {
			tagTable.setInput(createFilteredInput());
		}
		
	}

	/**
	 * Set whether the input filter text is to be included in the tag selection area.
	 * If excluded, clientscan still set the filter text directly using
	 * <code>setFilter</code>.
	 * @param include whether filter text input should be included
	 */
	public void setIncludeFilterInputArea(boolean include) {
		includeFilterInputArea = include;
	}
	
	/**
	 * Return whether the input filter text is to be included in the tag selection area.
	 * If excluded, clientscan still set the filter text directly using
	 * <code>setFilter</code>.
	 * @return whether filter text input should be included
	 */
	public boolean isIncludeFilterInputArea() {
		return includeFilterInputArea;
	}

	/**
	 * Set the text used to filter the tag list.
	 * @param filter the filter pattern
	 */
	public void setFilter(String filter) {
		this.filterPattern = filter;
		updateTagDisplay(false);
	}
	
	public void setRunnableContext(IRunnableContext context) {
		this.context = context;
		if (tagRefreshArea != null)
			tagRefreshArea.setRunnableContext(context);
	}
}
