/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.criteria.CriterionResource;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.WorkingSet;
import org.eclipse.help.ui.IEngineDescriptor;
import org.eclipse.help.ui.RootScopePage;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.search.HelpCriteriaContentProvider.CriterionName;
import org.eclipse.help.ui.internal.search.HelpCriteriaContentProvider.CriterionValue;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Local Help participant in the federated search.
 */
public class LocalHelpPage extends RootScopePage {
	private Button searchAll;

	private Button searchSelected;

	private CheckboxTreeViewer contentTree;

	private ITreeContentProvider contentTreeContentProvider;

	private ILabelProvider contentTreeLabelProvider;

	private CheckboxTreeViewer criteriaTree;

	private ITreeContentProvider criteriaTreeContentProvider;

	private ILabelProvider criteriaTreeLabelProvider;

	//private boolean firstCheck;

	private WorkingSet workingSet;

	@Override
	public void init(IEngineDescriptor ed, String scopeSetName) {
		super.init(ed, scopeSetName);
		if (scopeSetName != null)
			workingSet = BaseHelpSystem.getWorkingSetManager().getWorkingSet(
					scopeSetName);
	}

	/**
	 * Default constructor.
	 */
	public LocalHelpPage() {
		//firstCheck = true;
	}

	@Override
	protected int createScopeContents(Composite parent) {
		Font font = parent.getFont();
		initializeDialogUnits(parent);

		searchAll = new Button(parent, SWT.RADIO);
		searchAll.setText(Messages.selectAll);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		searchAll.setLayoutData(gd);
		searchAll.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				contentTree.getTree().setEnabled(false);
				if (criteriaTree != null) {
					criteriaTree.getTree().setEnabled(false);
				}
			}
		});

		searchSelected = new Button(parent, SWT.RADIO);
		searchSelected.setText(Messages.selectWorkingSet);
		gd = new GridData();
		gd.horizontalSpan = 2;
		searchSelected.setLayoutData(gd);
		searchSelected.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				contentTree.getTree().setEnabled(true);
				if (criteriaTree != null) {
					criteriaTree.getTree().setEnabled(true);

				}
			}
		});

		if (workingSet == null)
			searchAll.setSelection(true);
		else
			searchSelected.setSelection(true);

		Label contentLabel = new Label(parent, SWT.WRAP);
		contentLabel.setFont(font);
		contentLabel.setText(Messages.WorkingSetContent);
		gd = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		gd.horizontalSpan = 2;
		contentLabel.setLayoutData(gd);

		createContentTree(parent, font);

		if (HelpPlugin.getCriteriaManager().isCriteriaEnabled()) {
			Label criteriaLabel = new Label(parent, SWT.WRAP);
			criteriaLabel.setFont(font);
			criteriaLabel.setText(Messages.WorkingSetCriteria);
			gd = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_CENTER);
			gd.horizontalSpan = 2;
			criteriaLabel.setLayoutData(gd);
			createCriteriaTree(parent, font);
		}

		initializeCheckedState();
		applyDialogFont(parent);

		// Set help for the page
		// WorkbenchHelp.setHelp(tree, "help_workingset_page");
		return 1;
	}

	protected void createContentTree(Composite parent, Font font) {
		GridData gd;
		contentTree = new CheckboxTreeViewer(parent, SWT.BORDER | SWT.H_SCROLL
				| SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		gd.heightHint = getTreeHeightHint();
		gd.horizontalSpan = 2;
		contentTree.getControl().setLayoutData(gd);
		contentTree.getControl().setFont(font);

		contentTreeContentProvider = new HelpWorkingSetTreeContentProvider();
		contentTree.setContentProvider(contentTreeContentProvider);

		contentTreeLabelProvider = new HelpWorkingSetElementLabelProvider();
		contentTree.setLabelProvider(contentTreeLabelProvider);

		contentTree.setUseHashlookup(true);

		contentTree.setInput(BaseHelpSystem.getWorkingSetManager().getRoot());

		contentTree.addCheckStateListener(
				event -> handleCheckStateChange(event, contentTree, contentTreeContentProvider));

		contentTree.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
			}

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				final Object element = event.getElement();
				if (!contentTree.getGrayed(element))
					BusyIndicator.showWhile(getShell().getDisplay(),
							() -> setSubtreeChecked(element, contentTree.getChecked(element), false,
									contentTree, contentTreeContentProvider));
			}
		});
		contentTree.getTree().setEnabled(workingSet != null);
	}

	protected int getTreeHeightHint() {
		if (HelpPlugin.getCriteriaManager().isCriteriaEnabled()) {
			return convertHeightInCharsToPixels(8);
		}
		return convertHeightInCharsToPixels(15);
	}


	protected void createCriteriaTree(Composite parent, Font font) {
		GridData gd;
		criteriaTree = new CheckboxTreeViewer(parent, SWT.BORDER | SWT.H_SCROLL
				| SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		gd.heightHint = getTreeHeightHint();
		gd.horizontalSpan = 2;
		criteriaTree.getControl().setLayoutData(gd);
		criteriaTree.getControl().setFont(font);

		criteriaTreeContentProvider = new HelpCriteriaContentProvider();
		criteriaTree.setContentProvider(criteriaTreeContentProvider);

		criteriaTreeLabelProvider = new HelpCriteriaLabelProvider();
		criteriaTree.setLabelProvider(criteriaTreeLabelProvider);

		criteriaTree.setUseHashlookup(true);

		criteriaTree.setInput(BaseHelpSystem.getWorkingSetManager().getCriterionIds());

		criteriaTree.addCheckStateListener(
				event -> handleCheckStateChange(event, criteriaTree, criteriaTreeContentProvider));

		criteriaTree.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
			}

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				final Object element = event.getElement();
				if (!criteriaTree.getGrayed(element))
					BusyIndicator.showWhile(getShell().getDisplay(),
							() -> setSubtreeChecked(element, criteriaTree.getChecked(element), false,
									criteriaTree, criteriaTreeContentProvider));
			}
		});
		criteriaTree.getTree().setEnabled(workingSet != null);
	}


	private void initializeCheckedState() {
		if (workingSet == null)
			return;

		BusyIndicator.showWhile(getShell().getDisplay(), () -> {
			initializeContentTree();
			if (HelpPlugin.getCriteriaManager().isCriteriaEnabled()) {
				initializeCriteriaTree();
			}
		});
	}

	boolean isExpandable(Object element, ITreeContentProvider contentProvider) {
		return contentProvider.hasChildren(element);
	}

	void updateParentState(Object child, boolean baseChildState,
						   CheckboxTreeViewer tree, ITreeContentProvider contentProvider) {
		if (child == null)
			return;

		Object parent = contentProvider.getParent(child);
		if (parent == null)
			return;

		boolean allSameState = true;
		Object[] children = null;
		children = contentProvider.getChildren(parent);

		for (int i = children.length - 1; i >= 0; i--) {
			if (tree.getChecked(children[i]) != baseChildState
					|| tree.getGrayed(children[i])) {
				allSameState = false;
				break;
			}
		}

		tree.setGrayed(parent, !allSameState);
		tree.setChecked(parent, !allSameState || baseChildState);

		updateParentState(parent, baseChildState, tree, contentProvider);
	}

	void setSubtreeChecked(Object parent, boolean state,
			boolean checkExpandedState,
			CheckboxTreeViewer tree, ITreeContentProvider contentProvider) {

		Object[] children = contentProvider.getChildren(parent);
		for (int i = children.length - 1; i >= 0; i--) {
			Object element = children[i];
			if (state) {
				tree.setChecked(element, true);
				tree.setGrayed(element, false);
			} else
				tree.setGrayChecked(element, false);
			if (isExpandable(element, contentProvider))
				setSubtreeChecked(element, state, checkExpandedState, tree, contentProvider);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void findCheckedElements(java.util.List checkedResources, Object parent,
			CheckboxTreeViewer tree, ITreeContentProvider contentProvider) {
		Object[] children = contentProvider.getChildren(parent);
		for (int i = 0; i < children.length; i++) {
			if (tree.getGrayed(children[i]))
				findCheckedElements(checkedResources, children[i], tree, contentProvider);
			else if (tree.getChecked(children[i]))
				checkedResources.add(children[i]);
		}
	}

	private CriterionResource[] findCheckedCriteria(Object parent,
			CheckboxTreeViewer tree, ITreeContentProvider contentProvider) {
		Object[] children = contentProvider.getChildren(parent);
		List<CriterionResource> resources = new ArrayList<>();
		for (int i = 0; i < children.length; i++) {
			// First level children are names
			CriterionName name = (CriterionName) children[i];
			CriterionResource resource = new CriterionResource(name.getId());
			Object[] grandChildren = contentProvider.getChildren(name);
			for (int j = 0; j < grandChildren.length; j++) {
				if (tree.getChecked(grandChildren[j])) {
					CriterionValue value = (CriterionValue) grandChildren[j];
					resource.addCriterionValue(value.getId());
				}
			}
			if (resource.getCriterionValues().size() > 0) {
				resources.add(resource);
			}
		}
		return resources.toArray(new CriterionResource[resources.size()]);
	}

	void handleCheckStateChange(final CheckStateChangedEvent event,
			final CheckboxTreeViewer tree, final ITreeContentProvider contentProvider) {
		BusyIndicator.showWhile(getShell().getDisplay(), () -> {
			Object element = event.getElement();
			boolean state = event.getChecked();
			tree.setGrayed(element, false);
			if (isExpandable(element, contentProvider))
				setSubtreeChecked(element, state, state, tree, contentProvider);
			// only check subtree if state is set to true

			updateParentState(element, state, tree, contentProvider);
			// validateInput();
		});
	}

	public WorkingSet getWorkingSet() {
		ArrayList<AdaptableHelpResource> elements = new ArrayList<>(10);
		CriterionResource[] criteria;
		if (!HelpPlugin.getCriteriaManager().isCriteriaEnabled()) {
			criteria = new CriterionResource[0];
		} else {
			criteria = findCheckedCriteria(
				criteriaTree.getInput(),
				criteriaTree,
				criteriaTreeContentProvider);
		}
		findCheckedElements(elements, contentTree.getInput(), contentTree, contentTreeContentProvider);
		if (workingSet == null) {
			workingSet = new WorkingSet(
					getScopeSetName(),
					elements.toArray(new AdaptableHelpResource[elements.size()]), criteria);
		} else {
			workingSet.setName(getScopeSetName());
			workingSet.setElements(elements.toArray(new AdaptableHelpResource[elements.size()]));
			workingSet.setCriteria(criteria);
		}
		return workingSet;
	}

	@Override
	public boolean performOk() {
		if (searchSelected.getSelection())
			BaseHelpSystem.getWorkingSetManager()
					.addWorkingSet(getWorkingSet());
		else
			BaseHelpSystem.getWorkingSetManager().removeWorkingSet(
					getWorkingSet());

		getPreferenceStore().setValue(
				getKey(LocalSearchScopeFactory.P_WORKING_SET),
				getScopeSetName());
		getPreferenceStore().setValue(
				getKey(LocalSearchScopeFactory.P_CAPABILITY_FILTERING),
				false);
		return super.performOk();
	}

	private String getKey(String key) {
		return getEngineDescriptor().getId() + "." + key; //$NON-NLS-1$
	}

	protected void initializeContentTree() {
		Object[] elements = workingSet.getElements();
		contentTree.setCheckedElements(elements);
		for (int i = 0; i < elements.length; i++) {
			Object element = elements[i];
			if (isExpandable(element, contentTreeContentProvider))
				setSubtreeChecked(element, true, true, contentTree, contentTreeContentProvider);
			updateParentState(element, true, contentTree, contentTreeContentProvider);
		}
	}

	protected void initializeCriteriaTree() {
		CriterionResource[] criteria = workingSet.getCriteria();
		criteriaTree.setCheckedElements(criteria);
		for (int i = 0; i < criteria.length; i++) {
			CriterionResource element = criteria[i];
			CriterionName name = new CriterionName(element.getCriterionName(), null);
			List<String> values = element.getCriterionValues();
			for (Iterator<String> iter = values.iterator(); iter.hasNext();) {
				String valueString = iter.next();
				CriterionValue value = new CriterionValue(valueString, name);
				criteriaTree.setChecked(value, true);
				updateParentState(value, true, criteriaTree, criteriaTreeContentProvider);
			}
		}
	}

	@Override
	protected void performDefaults() {
		searchAll.setSelection(true);
		searchSelected.setSelection(false);
		contentTree.setCheckedElements(new Object[0]);
		if (criteriaTree != null) {
			criteriaTree.setCheckedElements(new Object[0]);
		}
		super.performDefaults();
	}

	@Override
	protected Label createDescriptionLabel(Composite parent) {
		if ( getContainer() == null ) {
			return null;
		}
		return super.createDescriptionLabel(parent);
	}
}
