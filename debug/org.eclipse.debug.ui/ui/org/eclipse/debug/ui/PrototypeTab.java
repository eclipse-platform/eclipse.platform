/*******************************************************************************
 * Copyright (c) 2017,2022 Obeo and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *     IBM Corporation - Bug fixes
 *******************************************************************************/
package org.eclipse.debug.ui;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Launch configuration tab used to specify the prototype associated with a
 * launch configuration, and also listed attributes from prototype shared with
 * the launch configuration.
 * <p>
 * Clients may call {@link #setHelpContextId(String)} on this tab prior to
 * control creation to alter the default context help associated with this tab.
 * </p>
 * <p>
 * Clients may instantiate this class only if the associated launch
 * configuration type allows prototypes.
 * </p>
 *
 * @see ILaunchConfigurationType#supportsPrototypes()
 *
 * @since 3.13
 * @noextend This class is not intended to be subclassed by clients.
 */
public class PrototypeTab extends AbstractLaunchConfigurationTab {

	private static final String PROTOTYPE_TAB_ID = "org.eclipse.debug.ui.prototypeTab"; //$NON-NLS-1$

	private static final String ATTRIBUTE = LaunchConfigurationsMessages.PrototypeTab_Atrribute_label;

	private static final String MODIFIED = LaunchConfigurationsMessages.PrototypeTab_Modified_label;

	private static final String PROTOTYPE_VALUE = LaunchConfigurationsMessages.PrototypeTab_Property_Value_label;

	private ILaunchConfiguration fSelectedLaunchConfiguration;

	private ILaunchConfiguration fAppliedPrototype;

	private Composite fPrototypeComposite;

	private Text fPrototypeText;

	private ControlDecoration fPrototypeTextDecoration;

	private Button fLinkPrototypeButton;

	private Button fUnlinkPrototypeButton;

	private Button fResetPrototypeButton;

	private CheckboxTreeViewer fAttributesTreeViewer;

	/**
	 * Constructs a new tab with default context help.
	 */
	public PrototypeTab() {
		setHelpContextId(IDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_PROTOTYPE_TAB);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());

		createConfigPrototypeComponent(comp);
		createPrototypeExplanationsComponent(comp);
		createPrototypeAttributesTreeComponent(comp);
	}

	/**
	 * Creates the config prototype location component
	 *
	 * @param parent the parent composite to add this component to
	 */
	private void createConfigPrototypeComponent(Composite parent) {
		fPrototypeComposite = new Composite(parent, SWT.NONE);
		fPrototypeComposite.setLayout(new GridLayout(5, false));
		fPrototypeComposite.setFont(parent.getFont());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		fPrototypeComposite.setLayoutData(gd);
		SWTFactory.createLabel(fPrototypeComposite, LaunchConfigurationsMessages.PrototypeTab_Label_2, 1);
		fPrototypeText = SWTFactory.createSingleText(fPrototypeComposite, 1);
		fPrototypeText.setEditable(false);
		fPrototypeTextDecoration = new ControlDecoration(fPrototypeText, SWT.TOP | SWT.LEFT);
		FieldDecoration errorDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		fPrototypeTextDecoration.setDescriptionText(LaunchConfigurationsMessages.PrototypeTab_Select_Prototype_Error_7);
		fPrototypeTextDecoration.setImage(errorDecoration.getImage());
		fLinkPrototypeButton = SWTFactory.createPushButton(fPrototypeComposite, LaunchConfigurationsMessages.PrototypeTab_Link_Button_Label_3, null);
		fLinkPrototypeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleLinkPrototypeButtonSelected();
			}
		});
		fUnlinkPrototypeButton = SWTFactory.createPushButton(fPrototypeComposite, LaunchConfigurationsMessages.PrototypeTab_Unlink_Button_Label_4, null);
		fUnlinkPrototypeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleUnlinkPrototypeButtonSelected();
			}
		});
		fResetPrototypeButton = SWTFactory.createPushButton(fPrototypeComposite, LaunchConfigurationsMessages.PrototypeTab_Reset_Button_Label_8, null);
		fResetPrototypeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleResetPrototypeButtonSelected();
			}
		});
	}

	/**
	 * Creates the prototype explanations component
	 *
	 * @param parent the parent composite to add this component to
	 */
	private void createPrototypeExplanationsComponent(Composite parent) {
		SWTFactory.createLabel(parent, LaunchConfigurationsMessages.PrototypeTab_Explanation_Label_10, 1);
		SWTFactory.createLabel(parent, LaunchConfigurationsMessages.PrototypeTab_Explanation_Label_11, 1);
	}

	/**
	 * Creates the prototype attributes tree component
	 *
	 * @param parent the parent composite to add this one to
	 */
	private void createPrototypeAttributesTreeComponent(Composite parent) {
		Composite attributesPrototypeTableComposite = new Composite(parent, SWT.NONE);
		attributesPrototypeTableComposite.setLayout(new GridLayout(1, false));
		attributesPrototypeTableComposite.setFont(parent.getFont());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		attributesPrototypeTableComposite.setLayoutData(gd);

		fAttributesTreeViewer = createCheckboxTreeViewer(attributesPrototypeTableComposite);
	}

	/**
	 * Update prototype text widget decorator.
	 */
	private void updateProductDecorator() {
		if (fAppliedPrototype != null && !fAppliedPrototype.exists()) {
			fPrototypeTextDecoration.show();
		} else {
			fPrototypeTextDecoration.hide();
		}
	}

	/**
	 * Handles the config prototype apply button being selected
	 */
	private void handleLinkPrototypeButtonSelected() {
		String currentSelectedPrototypeString = fPrototypeText.getText();
		DecoratingLabelProvider labelProvider = new DecoratingLabelProvider(DebugUITools.newDebugModelPresentation(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
		ElementListSelectionDialog selectPrototypeDialog = new ElementListSelectionDialog(getShell(), labelProvider);
		try {
			ILaunchConfigurationType type = fSelectedLaunchConfiguration.getType();
			ILaunchConfiguration[] prototypes = type.getPrototypes();
			selectPrototypeDialog.setElements(prototypes);
			selectPrototypeDialog.setMultipleSelection(false);
			selectPrototypeDialog.setTitle(LaunchConfigurationsMessages.PrototypeTab_Select_Message_5);
			selectPrototypeDialog.setEmptySelectionMessage(LaunchConfigurationsMessages.PrototypeTab_Select_Empty_Message_6);
			selectPrototypeDialog.setInitialSelections(getPrototype(prototypes, currentSelectedPrototypeString));
			int open = selectPrototypeDialog.open();
			if (open == Window.OK) {
				Object selectedPrototype = selectPrototypeDialog.getFirstResult();
				if (selectedPrototype instanceof ILaunchConfiguration) {
					fAppliedPrototype = (ILaunchConfiguration) selectedPrototype;
					ILaunchConfigurationWorkingCopy workingCopy = getWorkingCopy();
					workingCopy.setPrototype(fAppliedPrototype, true);
					fPrototypeText.setText(((ILaunchConfiguration) selectedPrototype).getName());
					fAttributesTreeViewer.setInput(fAppliedPrototype);
					fUnlinkPrototypeButton.setEnabled(true);
					fResetPrototypeButton.setEnabled(true);
					updateColumnsWidth();
					updateProductDecorator();
					setDirty(true);
					updateLaunchConfigurationDialog();
					reinitTabs(workingCopy);
				}
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e.getStatus());
		}
	}

	/**
	 * Handles the config prototype unapply button being selected
	 */
	private void handleUnlinkPrototypeButtonSelected() {
		try {
			fAppliedPrototype = null;
			ILaunchConfigurationWorkingCopy workingCopy = getWorkingCopy();
			workingCopy.setPrototype(null, false);
			fPrototypeText.setText(""); //$NON-NLS-1$
			fAttributesTreeViewer.setInput(null);
			fUnlinkPrototypeButton.setEnabled(false);
			fResetPrototypeButton.setEnabled(false);
			updateProductDecorator();
			setDirty(true);
			updateLaunchConfigurationDialog();
			reinitTabs(workingCopy);
		} catch (CoreException e) {
			DebugUIPlugin.log(e.getStatus());
		}
	}

	/**
	 * Handles the config prototype reset button being selected
	 */
	private void handleResetPrototypeButtonSelected() {
		try {
			ILaunchConfigurationWorkingCopy workingCopy = getWorkingCopy();
			workingCopy.setPrototype(fAppliedPrototype, true);
			fAttributesTreeViewer.refresh();
			setDirty(true);
			updateLaunchConfigurationDialog();
			reinitTabs(workingCopy);
		} catch (CoreException e) {
			DebugUIPlugin.log(e.getStatus());
		}
	}

	/**
	 * Get the working copy of the launch configuration associated with this
	 * tab.
	 *
	 * @return an {@link ILaunchConfigurationWorkingCopy}.
	 * @throws CoreException
	 */
	private ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
		ILaunchConfigurationWorkingCopy workingCopy;
		if (fSelectedLaunchConfiguration instanceof ILaunchConfigurationWorkingCopy) {
			workingCopy = (ILaunchConfigurationWorkingCopy) fSelectedLaunchConfiguration;
		} else {
			workingCopy = fSelectedLaunchConfiguration.getWorkingCopy();
		}
		return workingCopy;
	}

	/**
	 * Reinit the tabs of the launch configuration dialog.
	 *
	 * @param launchConfiguration the {@link LaunchConfiguration}.
	 */
	private void reinitTabs(ILaunchConfiguration launchConfiguration) {
		ILaunchConfigurationTab[] tabs = getLaunchConfigurationDialog().getTabs();
		for (ILaunchConfigurationTab configTab : tabs) {
			configTab.initializeFrom(launchConfiguration);
		}
	}

	/**
	 * Get the prototype with the given name in the given prototypes list.
	 *
	 * @param prototypes the prototypes list
	 * @param name the prototype to get
	 * @return the prototype with the given name in the given prototypes list.
	 */
	private Object[] getPrototype(ILaunchConfiguration[] prototypes, String name) {
		for (ILaunchConfiguration prototype : prototypes) {
			if (name.equals(prototype.getName())) {
				return new Object[] { prototype };
			}
		}
		return new Object[0];
	}

	/**
	 * Creates the checkbox tree viewer that contains attributes.
	 *
	 * @param parent the parent composite to add this one to
	 * @return a {@link CheckboxTreeViewer}
	 */
	private CheckboxTreeViewer createCheckboxTreeViewer(Composite parent) {
		Tree tree = new Tree(parent, SWT.MULTI | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		tree.setLayoutData(gd);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		CheckboxTreeViewer treeViewer = new CheckboxTreeViewer(tree);

		treeViewer.setContentProvider(new PrototypeAttributesContentProvider());
		treeViewer.setCheckStateProvider(new AttributesTreeCheckStateProvider());
		treeViewer.addCheckStateListener(new AttributesTreeCheckStateListener());

		MenuManager menuMgr = new MenuManager();
		Menu contextMenu = menuMgr.createContextMenu(tree);
		menuMgr.addMenuListener(new ResetMenuListener());
		menuMgr.setRemoveAllWhenShown(true);
		tree.setMenu(contextMenu);

		return treeViewer;
	}

	/**
	 * Add columns to the given {@link CheckboxTreeViewer}.
	 *
	 * @param treeViewer the given {@link CheckboxTreeViewer}.
	 */
	private void addColumnsToTreeViewer(CheckboxTreeViewer treeViewer) {
		for (TreeColumn treeColumn : treeViewer.getTree().getColumns()) {
			treeColumn.dispose();
		}
		TreeViewerColumn columnLabel = new TreeViewerColumn(treeViewer, SWT.LEFT, 0);
		columnLabel.setLabelProvider(new ColumnAttributeLabelProvider());
		columnLabel.getColumn().setText(ATTRIBUTE);
		if (fSelectedLaunchConfiguration != null && !fSelectedLaunchConfiguration.isPrototype()) {
			TreeViewerColumn columnModified = new TreeViewerColumn(treeViewer, SWT.CENTER, 1);
			columnModified.setLabelProvider(new ColumnModifiedLabelProvider());
			columnModified.getColumn().setText(MODIFIED);
		}
		TreeViewerColumn columnValue = new TreeViewerColumn(treeViewer, SWT.LEFT, fSelectedLaunchConfiguration != null && !fSelectedLaunchConfiguration.isPrototype() ? 2 : 1);
		columnValue.setLabelProvider(new ColumnValueLabelProvider());
		columnValue.getColumn().setText(PROTOTYPE_VALUE);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		initialize(configuration);
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		initialize(workingCopy);
	}

	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
	}

	/**
	 * Initialize this tab with the given configuration.
	 *
	 * @param configuration the given configuration
	 * @see PrototypeTab#activated(ILaunchConfigurationWorkingCopy)
	 * @see PrototypeTab#initializeFrom(ILaunchConfiguration)
	 */
	private void initialize(ILaunchConfiguration configuration) {
		try {
			if (configuration.isPrototype()) {
				fPrototypeComposite.setVisible(false);
				GridData gridData = (GridData) fPrototypeComposite.getLayoutData();
				gridData.exclude = true;
				fAppliedPrototype = configuration;
				fSelectedLaunchConfiguration = configuration;
				addColumnsToTreeViewer(fAttributesTreeViewer);
				fAttributesTreeViewer.setInput(fAppliedPrototype);
				fAttributesTreeViewer.getTree().setEnabled(true);
			} else {
				fPrototypeComposite.setVisible(true);
				GridData gridData = (GridData) fPrototypeComposite.getLayoutData();
				gridData.exclude = false;
				fSelectedLaunchConfiguration = configuration;
				fAppliedPrototype = configuration.getPrototype();
				addColumnsToTreeViewer(fAttributesTreeViewer);
				fAttributesTreeViewer.getTree().setEnabled(false);
				if (fAppliedPrototype == null) {
					fPrototypeText.setText(""); //$NON-NLS-1$
					fUnlinkPrototypeButton.setEnabled(false);
					fResetPrototypeButton.setEnabled(false);
					fAttributesTreeViewer.setInput(null);
				} else if (!fAppliedPrototype.exists()) {
					fPrototypeText.setText(fAppliedPrototype.getName());
					fUnlinkPrototypeButton.setEnabled(true);
					fResetPrototypeButton.setEnabled(true);
					fAttributesTreeViewer.setInput(null);
				} else {
					fPrototypeText.setText(fAppliedPrototype.getName());
					fUnlinkPrototypeButton.setEnabled(true);
					fResetPrototypeButton.setEnabled(true);
					fAttributesTreeViewer.setInput(fAppliedPrototype);
				}
			}
			updateProductDecorator();
			updateColumnsWidth();
		} catch (CoreException e) {
			DebugUIPlugin.log(e.getStatus());
		}
	}

	/**
	 * Set columns tree widths dynamically
	 */
	private void updateColumnsWidth() {
		getShell().getDisplay().asyncExec(() -> {
			Tree tree = fAttributesTreeViewer.getTree();
			if (tree != null && !tree.isDisposed()) {
				TreeColumn[] columns = tree.getColumns();
				for (TreeColumn treeColumn : columns) {
					treeColumn.pack();
				}
			}
		});
	}

	@Override
	public boolean isValid(ILaunchConfiguration config) {
		setMessage(null);
		setErrorMessage(null);

		if (fAppliedPrototype != null && !fAppliedPrototype.exists()) {
			setErrorMessage(LaunchConfigurationsMessages.PrototypeTab_Select_Prototype_Error_7);
			return false;
		}
		return !config.isPrototype();
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
	}

	@Override
	public void postApply() {
		super.postApply();
		ILaunchConfigurationDialog launchConfigurationDialog = getLaunchConfigurationDialog();
		if (launchConfigurationDialog instanceof LaunchConfigurationsDialog) {
			((LaunchConfigurationsDialog) launchConfigurationDialog).refreshLaunchConfigurationView();
		}
	}

	@Override
	public String getName() {
		return LaunchConfigurationsMessages.PrototypeTab_Prototype_1;
	}

	@Override
	public Image getImage() {
		return DebugUITools.getImage(IInternalDebugUIConstants.IMG_OBJS_PROTO_TAB);
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 *
	 */
	@Override
	public String getId() {
		return PROTOTYPE_TAB_ID;
	}

	@Override
	public boolean canSave() {
		return true;
	}

	/**
	 * Check if an attribute from the selected launch configuration is different
	 * from its prototype.
	 *
	 * @param element the given attribute.
	 */
	private boolean isAttributeModified(Entry<String, Object> element) {
		try {
			if (fSelectedLaunchConfiguration != null && !fSelectedLaunchConfiguration.isPrototype()) {
				return fSelectedLaunchConfiguration.isAttributeModified(element.getKey());
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e.getStatus());
		}
		return false;
	}

	/**
	 * Content provider for the prototype attributes table
	 */
	private class PrototypeAttributesContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ILaunchConfiguration) {
				try {
					Map<String, Object> attributes = ((ILaunchConfiguration) inputElement).getAttributes();
					return attributes.entrySet().toArray();
				} catch (CoreException e) {
					DebugUIPlugin.log(e.getStatus());
				}
			}
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	/**
	 * Label provider for the prototype attributes tree column "Edited"
	 */
	private class ColumnModifiedLabelProvider extends ColumnLabelProvider {
		@SuppressWarnings("unchecked")
		@Override
		public String getText(Object element) {
			if (fSelectedLaunchConfiguration != null && !fSelectedLaunchConfiguration.isPrototype()) {
				if (element instanceof Entry && isAttributeModified((Entry<String, Object>) element)) {
					return "true"; //$NON-NLS-1$
				}
			}
			return "false"; //$NON-NLS-1$
		}
	}

	/**
	 * Label provider for the prototype attributes tree column "Attribute"
	 */
	@SuppressWarnings({ "unchecked" })
	private class ColumnAttributeLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			String key = ((Entry<String, Object>) element).getKey();
			ILaunchConfigurationTab[] tabs = getLaunchConfigurationDialog().getTabs();
			for (ILaunchConfigurationTab tab : tabs) {
				if (tab instanceof AbstractLaunchConfigurationTab) {
					String attributeLabel = ((AbstractLaunchConfigurationTab) tab).getAttributeLabel(key);
					if (attributeLabel != null) {
						return attributeLabel;
					}
				}
			}
			return key;
		}
	}

	/**
	 * Label provider for the prototype attributes tree column "Value"
	 */
	@SuppressWarnings({ "unchecked" })
	private class ColumnValueLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			Object value = ((Entry<String, Object>) element).getValue();
			if (value instanceof Boolean) {
				return ((Boolean) value).toString();
			} else if (value instanceof String) {
				return (String) value;
			} else if (value instanceof Integer) {
				return value.toString();
			} else if (value instanceof List) {
				return "[" + ((List<String>) value).stream().collect(Collectors.joining(", ")) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} else if (value instanceof Set) {
				return "[" + ((Set<String>) value).stream().collect(Collectors.joining(", ")) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} else if (value instanceof Map) {
				return "[" + ((Map<String, String>) value).entrySet().stream().map(i -> "[" + i.getKey() + ", " + i.getValue() + "]").collect(Collectors.joining(", ")) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			}
			return super.getText(element);
		}
	}

	/**
	 * Check state provider for attributes tree.
	 */
	@SuppressWarnings("unchecked")
	private class AttributesTreeCheckStateProvider implements ICheckStateProvider {
		@Override
		public boolean isGrayed(Object element) {
			return false;
		}

		@Override
		public boolean isChecked(Object element) {
			try {
				if (element instanceof Entry) {
					return fAppliedPrototype.getPrototypeVisibleAttributes().contains(((Entry<String, Object>) element).getKey());
				}
			} catch (CoreException e) {
				DebugUIPlugin.log(e.getStatus());
			}
			return false;
		}
	}

	/**
	 * Check state listener for attributes tree.
	 */
	@SuppressWarnings("unchecked")
	private class AttributesTreeCheckStateListener implements ICheckStateListener {
		@Override
		public void checkStateChanged(CheckStateChangedEvent event) {
			try {
				Object data = event.getElement();
				if (data instanceof Entry) {
					fAppliedPrototype.setPrototypeAttributeVisibility(((Entry<String, Object>) data).getKey(), event.getChecked());
					setDirty(true);
					updateLaunchConfigurationDialog();
				}
			} catch (CoreException e) {
				DebugUIPlugin.log(e.getStatus());
			}
		}
	}

	/**
	 * Menu Listener for attributes table.
	 */
	private class ResetMenuListener implements IMenuListener {

		@Override
		public void menuAboutToShow(IMenuManager manager) {
			if (fSelectedLaunchConfiguration != null && !fSelectedLaunchConfiguration.isPrototype()) {
				manager.add(new ResetAction());
			}
		}
	}

	/**
	 * Reset attribute menu action.
	 */
	@SuppressWarnings("unchecked")
	private class ResetAction extends Action {

		@Override
		public String getText() {
			return LaunchConfigurationsMessages.PrototypeTab_Reset_Menu_Action_9;
		}

		@Override
		public void run() {
			IStructuredSelection selection = fAttributesTreeViewer.getStructuredSelection();
			try {
				ILaunchConfigurationWorkingCopy workingCopy = getWorkingCopy();
				for (Object element : selection.toList()) {
					if (element instanceof Entry) {
						String key = ((Entry<String, Object>) element).getKey();
						if (fAppliedPrototype.getPrototypeVisibleAttributes().contains(key)) {
							Object prototypeValue = fAppliedPrototype.getAttributes().get(key);
							workingCopy.setAttribute(key, prototypeValue);
						}
					}
				}
				setDirty(true);
				updateLaunchConfigurationDialog();
				reinitTabs(workingCopy);
			} catch (CoreException e) {
				DebugUIPlugin.log(e.getStatus());
			}
		}
	}
}
