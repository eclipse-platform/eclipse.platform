/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.actions.IMenuContributor;
import org.eclipse.ui.internal.cheatsheets.composite.explorer.RestartAllAction;
import org.eclipse.ui.internal.cheatsheets.composite.explorer.TreeExplorerMenu;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetSaveHelper;
import org.eclipse.ui.internal.cheatsheets.composite.model.EditableTask;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ICompositeCheatsheetTags;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader.TaskExplorerNode;
import org.eclipse.ui.internal.cheatsheets.state.ICheatSheetStateManager;
import org.eclipse.ui.internal.cheatsheets.views.Page;
import org.eclipse.ui.internal.cheatsheets.views.ViewUtilities;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskEditor;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskExplorer;
import org.eclipse.ui.part.PageBook;

/**
 * A page which represents a composite cheat sheet
 */

public class CompositeCheatSheetPage extends Page implements ISelectionChangedListener, IMenuContributor {

	public static final String REVIEW_TAG = "__review__"; //$NON-NLS-1$
	public static final String END_REVIEW_TAG = "__endReview__"; //$NON-NLS-1$
	public static final String GOTO_TASK_TAG = "__goto__"; //$NON-NLS-1$
	public static final String START_HREF = "__start__"; //$NON-NLS-1$
	public static final String SKIP_HREF = "__skip__"; //$NON-NLS-1$

	private ManagedForm mform;
	private PageBook explorerContainer;
	private PageBook taskEditorContainer;
	private CompositeCheatSheetModel model;
	private TaskExplorer currentExplorer;
	private DescriptionPanel descriptionPanel;

	private CompositeCheatSheetSaveHelper saveHelper;

	private ICompositeCheatSheetTask selectedTask;
	private boolean initialized = false;

	public CompositeCheatSheetPage(CompositeCheatSheetModel model, ICheatSheetStateManager stateManager) {
		this.model = model;
		saveHelper = new CompositeCheatSheetSaveHelper(stateManager);
	}

	@Override
	public void createPart(Composite parent) {
		init(parent.getDisplay());
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
		form = toolkit.createScrolledForm(parent);
		form.setLayoutData(new GridData(GridData.FILL_BOTH));
		FormColors colors = toolkit.getColors();
		/*
		colors.initializeSectionToolBarColors();
		Color gbg = colors.getColor(FormColors.TB_GBG);
		Color bg = colors.getBackground();
		form.getForm().setTextBackground(new Color[]{bg, gbg}, new int [] {100}, true);
		form.getForm().setSeparatorColor(colors.getColor(FormColors.TB_BORDER));
		form.getForm().setSeparatorVisible(true);
		*/
		toolkit.decorateFormHeading(form.getForm());
		mform = new ManagedForm(toolkit, form);
		GridLayout glayout = new GridLayout();
		glayout.marginHeight = 0;
		glayout.marginWidth = 0;
		form.getBody().setLayout(glayout);
		final SashForm sash = new SashForm(form.getBody(), SWT.NULL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 10;
		gd.heightHint = 10;
		sash.setLayoutData(gd);
		sash.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Point size = sash.getSize();
				if (size.x>size.y)
					sash.setOrientation(SWT.HORIZONTAL);
				else
					sash.setOrientation(SWT.VERTICAL);
				updateSashPanelMargins(sash);
			}
		});
		sash.setBackground(colors.getColor(IFormColors.TB_BG));

		Composite explorerPanel = new Composite(sash, SWT.NULL);
		explorerPanel.setBackground(colors.getColor(IFormColors.TB_BORDER));
		GridLayout playout = new GridLayout();
		playout.marginWidth = 0;
		playout.marginTop = 2;
		playout.marginBottom = 0;
		explorerPanel.setLayout(playout);
		toolkit.adapt(explorerPanel);
		explorerContainer = new PageBook(explorerPanel, SWT.NULL);
		explorerContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		Composite editorPanel = new Composite(sash, SWT.NULL);
		playout = new GridLayout();
		playout.marginWidth = 0;
		playout.marginHeight = 0;
		editorPanel.setLayout(playout);
		editorPanel.setBackground(colors.getColor(IFormColors.TB_BORDER));
		taskEditorContainer = new PageBook(editorPanel, SWT.NULL);
		toolkit.adapt(taskEditorContainer);
		taskEditorContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	private void updateSashPanelMargins(SashForm sash) {
		Control [] children = sash.getChildren();
		int orientation = sash.getOrientation();
		// update task explorer panel
		GridLayout layout = (GridLayout)((Composite)children[0]).getLayout();
		if (orientation==SWT.HORIZONTAL) {
			layout.marginBottom = 0;
			layout.marginRight = 1;
		}
		else {
			layout.marginBottom = 1;
			layout.marginRight = 0;
		}
		// update task editor panel
		layout = (GridLayout)((Composite)children[1]).getLayout();
		if (orientation==SWT.HORIZONTAL) {
			layout.marginTop = 0;
			layout.marginLeft = 1;
		}
		else {
			layout.marginTop = 1;
			layout.marginLeft = 0;
		}
		((Composite)children[0]).layout();
		((Composite)children[1]).layout();
	}

	@Override
	public void dispose() {
		mform.dispose();
		super.dispose();
	}

	private void setInputModel(CompositeCheatSheetModel model, Map<String, String> layout) {
		this.model = model;
		mform.getForm().setText(ViewUtilities.escapeForLabel(model.getName()));
		String explorerId = model.getTaskExplorerId();
		setCurrentExplorerFromId(explorerId);
		String selectedTaskId = layout.get(ICompositeCheatsheetTags.SELECTED_TASK);
		ICompositeCheatSheetTask selectedTask= null;
		if (selectedTaskId != null) {
			selectedTask = model.getDependencies().getTask(selectedTaskId);
			if (selectedTask != null)  {
				currentExplorer.setSelection(new StructuredSelection(selectedTask), true);
			}
		}
		if (selectedTask != null) {
			updateSelectedTask(selectedTask);
		} else {
			updateSelectedTask(model.getRootTask());
		}
		model.addObserver((o, arg) -> {
			ICompositeCheatSheetTask task = (ICompositeCheatSheetTask)arg;
			if (currentExplorer!=null)
				currentExplorer.taskUpdated(task);
			updateTask(task);
		});
	}

	private void setCurrentExplorerFromId(String explorerId) {
		TaskExplorer explorer = getTaskExplorer(explorerId);
		if (explorer!=null) {
			explorer.setCompositeCheatSheet(this.model);
			explorer.setFocus();
			setExplorerVisible(explorerId);
		}
	}

	private void setExplorerVisible(String id) {
		Control [] excontrols = explorerContainer.getChildren();
		TaskExplorer explorer=null;
		for (Control excontrol : excontrols) {
			explorer = (TaskExplorer)excontrol.getData(ICompositeCheatsheetTags.EXPLORER);
			if (explorer.getId().equals(id)) {
				explorerContainer.showPage(excontrol);
				setCurrentExplorer(explorer);
				return;
			}
		}
	}

	private void setCurrentExplorer(TaskExplorer explorer) {
		if (currentExplorer!=null) {
			currentExplorer.getSelectionProvider().removeSelectionChangedListener(this);
		}
		currentExplorer = explorer;
		currentExplorer.getSelectionProvider().addSelectionChangedListener(this);
		updateForSelection(currentExplorer.getSelectionProvider().getSelection());
	}

	private TaskExplorer getTaskExplorer(String id) {
		Control [] excontrols = explorerContainer.getChildren();
		TaskExplorer explorer=null;
		for (Control excontrol : excontrols) {
			Object data = excontrol.getData(ICompositeCheatsheetTags.EXPLORER);
			if (data instanceof TaskExplorer) {
				explorer = (TaskExplorer)data;
				if (explorer.getId().equals(id)) {
					return explorer;
				}
			}
		}

		// Load the explorer from an extension point

		TaskExplorerManager explorerManager = TaskExplorerManager.getInstance();
		explorer = explorerManager.getExplorer(id);
		if (explorer != null) {
			explorer.createControl(explorerContainer, mform.getToolkit());
			explorer.getControl().setData(ICompositeCheatsheetTags.EXPLORER, explorer);
			new TreeExplorerMenu(explorer);
		}
		return explorer;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		updateForSelection(event.getStructuredSelection());
	}

	private void updateForSelection(ISelection selection) {
		Object selectedElement = ((IStructuredSelection)selection).getFirstElement();
		if (selectedElement instanceof ICompositeCheatSheetTask) {
			updateSelectedTask((ICompositeCheatSheetTask)selectedElement);
		}
	}

	private void updateSelectedTask(ICompositeCheatSheetTask task) {
		selectedTask = task;
		updateTask(selectedTask);
	}

	/*
	 * Update can be called as a result of a selection  change or a state change
	 * If this is not the selected task wait till it is selected to display it
	 */
	private void updateTask(ICompositeCheatSheetTask task) {
		if (task==null || task != selectedTask) return;
		if ( task instanceof EditableTask) {
			EditableTask editable = (EditableTask)task;
			if (editable.getState() == ICompositeCheatSheetTask.IN_PROGRESS) {
				showEditor(editable);
				return;
			} else if (editable.isUnderReview()) {
				if (editable.getState() == ICompositeCheatSheetTask.COMPLETED) {
					showEditor(editable);
				} else {
					endReview(editable);
				}
				return;
			}
		}
		showDescription(task);
	}

	public void saveState() {
		Map<String, String> layout = new HashMap<>();
		if (selectedTask != null) {
			layout.put(ICompositeCheatsheetTags.SELECTED_TASK, selectedTask.getId());
		}
		saveHelper.saveCompositeState(model, layout);
	}

	private void showDescription(final ICompositeCheatSheetTask task) {
		if (descriptionPanel==null) {
			createDescriptionPanel();
		}
		descriptionPanel.showDescription(task);
		setCurrentEditor(descriptionPanel.getControl());
	}

	private void createDescriptionPanel() {
		descriptionPanel = new DescriptionPanel(mform, taskEditorContainer);
		descriptionPanel.addHyperlinkListener(new DescriptionLinkListener());
	}

	/*
	 * Ensure that if this task is visible and in a runnable state that it has been started
	 */
	private void showEditor(EditableTask task) {
		if (task == selectedTask) {
			TaskEditor editor = getTaskEditor(task);
			if (editor!= null) {
				if (!task.isEditorInitialized()) {
					task.setInput(model.getTaskMemento(task.getId()));
				}
				setCurrentEditor(editor.getControl().getParent());
			}
		}
	}

	private void addHyperlink(StringBuilder buf, String href, String imageRef, String message) {
		buf.append("<p><a href=\""); //$NON-NLS-1$
		buf.append(href);
		buf.append("\">"); //$NON-NLS-1$
		buf.append("<img href=\""); //$NON-NLS-1$
		buf.append(imageRef);
		buf.append("\"/> "); //$NON-NLS-1$
		buf.append(message);
		buf.append("</a></p>"); //$NON-NLS-1$
	}

	private void reviewTask(EditableTask task) {
		TaskEditor taskEditor = getTaskEditor(task);
		Composite container = taskEditor.getControl().getParent();
		Composite separator = toolkit.createCompositeSeparator(container);
		GridData data = new GridData();
		data.heightHint = 1;
		data.horizontalAlignment = SWT.FILL;
		separator.setLayoutData(data);
		FormText text = toolkit.createFormText(container, false);
		text.setImage(DescriptionPanel.REVIEW_IMAGE, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.COMPOSITE_TASK_REVIEW));
		text.addHyperlinkListener(getEndReviewListener());
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		StringBuilder buf = new StringBuilder();
		buf.append("<form>"); //$NON-NLS-1$
		addHyperlink(buf, END_REVIEW_TAG + task.getId(), DescriptionPanel.REVIEW_IMAGE, Messages.COMPOSITE_PAGE_END_REVIEW );
		buf.append("</form>"); //$NON-NLS-1$
		text.setText(buf.toString(), true, false);
		task.setUnderReview(true);
		container.layout(true);
		showEditor(task);
	}

	private void endReview(EditableTask task) {
		TaskEditor taskEditor = getTaskEditor(task);
		Control editorControl = taskEditor.getControl();
		Composite container = editorControl.getParent();
		Control[] children = container.getChildren();
		for (int i = children.length -2; i < children.length; i++) {
			children[i].dispose();
		}
		task.setUnderReview(false);
		showDescription(task);
		container.layout();
	}

	private void setCurrentEditor(Control c) {
		taskEditorContainer.showPage(c);
	}

	/**
	 * Class which responds to hyperlink events originating from the
	 * description panel.
	 */
	private final class DescriptionLinkListener extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			String ref = (String)e.getHref();
			if (ref.equals(START_HREF)) {
				Object data = descriptionPanel.getControl().getData(ICompositeCheatsheetTags.TASK);
				if (data instanceof EditableTask) {
					EditableTask task = (EditableTask)data;
					task.setStarted();
				}
			}
			if (ref.equals(SKIP_HREF)) {
				Object data = descriptionPanel.getControl().getData(ICompositeCheatsheetTags.TASK);
				if (data instanceof AbstractTask) {
					AbstractTask task = (AbstractTask)data;
					task.setState(ICompositeCheatSheetTask.SKIPPED);
				}
			}
			if (ref.equals(REVIEW_TAG)) {
				Object data = descriptionPanel.getControl().getData(ICompositeCheatsheetTags.TASK);
				if (data instanceof EditableTask) {
					reviewTask((EditableTask) data);
				}
			}
			if (ref.startsWith(GOTO_TASK_TAG)) {
				String next = ref.substring(GOTO_TASK_TAG.length());
				AbstractTask nextTask =
					model.getDependencies().getTask(next);
				currentExplorer.setSelection
					(new StructuredSelection(nextTask), true);
			}
		}
	}

	/**
	 * Class which responds to hyperlink events originating from the
	 * end review panel
	 */
	private final class EndReviewListener extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			String ref = (String)e.getHref();
			if (ref.startsWith(END_REVIEW_TAG)) {
				String next = ref.substring(END_REVIEW_TAG.length());
				AbstractTask task =
					model.getDependencies().getTask(next);
				endReview((EditableTask)task);
			}
		}
	}

	private EndReviewListener endReviewListener;

	private EndReviewListener getEndReviewListener() {
		if (endReviewListener == null) {
			endReviewListener = new EndReviewListener();
		}
		return endReviewListener;
	}


	/*
	 * Get the task editor for this task. If no editor exists create one
	 */
	private TaskEditor getTaskEditor(EditableTask editable) {
		if (editable.getEditor() == null) {
			// Create a new editor using the extension point data
			TaskEditor editor = TaskEditorManager.getInstance().getEditor(editable.getKind());
			if (editor != null) {
				Composite editorPanel = new Composite(taskEditorContainer, SWT.NULL);
				GridLayout layout = new GridLayout();
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				editorPanel.setLayout(layout);
				mform.getToolkit().adapt(editorPanel);
				editor.createControl(editorPanel, mform.getToolkit());
				editable.setEditor(editor);
				GridData gridData = new GridData(GridData.FILL_BOTH);
				editor.getControl().setLayoutData(gridData);
			}
		}
		return editable.getEditor();
	}

	@Override
	public Control getControl() {
		return form;
	}

	@Override
	protected String getTitle() {
		return model.getName();
	}

	@Override
	public void initialized() {
		// Open the model
		model.setSaveHelper(saveHelper);
		Map<String, String> layout = new HashMap<>();
		model.loadState(layout);
		setInputModel(model, layout);
		initialized  = true;
	}

	@Override
	public int contributeToViewMenu(Menu menu, int index) {
		if (!initialized) {
			return index;
		}
		index = contributeExplorerMenuItem(menu, index);
		return contributeRestartMenuItem(menu, index);
	}

	private int contributeRestartMenuItem(Menu menu, int index) {
		MenuItem item = new MenuItem(menu, SWT.PUSH, index++);
		item.setText(Messages.RESTART_ALL_MENU);
		item.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.COMPOSITE_RESTART_ALL));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (model != null) {
					if (RestartAllAction.confirmRestart()) {
						restart(null);
					}
				}
			}
		});
		return index;
	}

	private int contributeExplorerMenuItem(Menu menu, int index) {
		String[] explorerIds = CheatSheetRegistryReader.getInstance().getExplorerIds();
		if (explorerIds.length == 1) {
			return index;  // no other explorer to chosse from
		}
		MenuItem menuItem = new MenuItem(menu, SWT.CASCADE, index++);

		menuItem.setText(Messages.EXPLORER_PULLDOWN_MENU);

		Menu subMenu = new Menu(menu);
		menuItem.setMenu(subMenu);

		for (final String id : explorerIds) {
			TaskExplorerNode node = CheatSheetRegistryReader.getInstance().findTaskExplorer(id);
			boolean isCurrentExplorer = id.equals(currentExplorer.getId());
			int style = isCurrentExplorer ? SWT.RADIO: SWT.PUSH;
			MenuItem item = new MenuItem(subMenu, style);
			item.setText(node.getName());
			item.setSelection(isCurrentExplorer);
			item.setImage(TaskExplorerManager.getInstance().getImage(id));
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setCurrentExplorerFromId(id);
				}
			});
		}

		return index;
	}

	public void restart(Map<String, String> cheatSheetData) {
		model.resetAllTasks(cheatSheetData);
		currentExplorer.setSelection
			(new StructuredSelection(model.getRootTask()), true);

	}

}
