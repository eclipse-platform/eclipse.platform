/*******************************************************************************
 *  Copyright (c) 2009, 2018 QNX Software Systems and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      QNX Software Systems - initial API and implementation
 *      Freescale Semiconductor
 *      SSI Schaefer
 *      Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *      IBM Corporation - Bug 517809
 *******************************************************************************/
package org.eclipse.debug.internal.ui.groups;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.groups.GroupLaunchConfigurationDelegate;
import org.eclipse.debug.internal.core.groups.GroupLaunchElement;
import org.eclipse.debug.internal.core.groups.GroupLaunchElement.GroupElementPostLaunchAction;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationFilteredTree;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupFilter;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * Dialog to select launch configuration(s)
 */
class GroupLaunchConfigurationSelectionDialog extends TitleAreaDialog implements ISelectionChangedListener {
	private static final String GROUP_TYPE_ID = "org.eclipse.debug.core.groups.GroupLaunchConfigurationType"; //$NON-NLS-1$

	private ISelection fSelection;
	private String mode;
	private GroupElementPostLaunchAction action = GroupElementPostLaunchAction.NONE;
	private Object actionParam;
	private boolean adoptIfRunning;
	private ViewerFilter emptyTypeFilter;
	private IStructuredSelection fInitialSelection;
	private Label fActionParamLabel;
	private Text fActionParamWidget; // in seconds
	private boolean fForEditing; // true if dialog was opened to edit an entry,
									// otherwise it was opened to add one
	private ILaunchConfigurationType groupType;
	private ILaunchConfiguration selfRef;

	public GroupLaunchConfigurationSelectionDialog(Shell shell, String initMode, boolean forEditing, ILaunchConfiguration self) {
		super(shell);
		mode = initMode;
		fForEditing = forEditing;
		selfRef = self;
		setShellStyle(getShellStyle() | SWT.RESIZE);

		groupType = getLaunchManager().getLaunchConfigurationType(GROUP_TYPE_ID);
		emptyTypeFilter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				try {
					if (element instanceof ILaunchConfigurationType) {
						ILaunchConfigurationType type = (ILaunchConfigurationType) element;
						if (type.equals(groupType)) {
							// we're hiding ourselves. if we're the only group,
							// don't show the type.
							return getLaunchManager().getLaunchConfigurations(type, ILaunchConfiguration.CONFIGURATION).length > 1;
						}

						return getLaunchManager().getLaunchConfigurations(type, ILaunchConfiguration.CONFIGURATION).length > 0;
					} else if (element instanceof ILaunchConfiguration) {
						ILaunchConfiguration c = (ILaunchConfiguration) element;
						if (c.getName().equals(self.getName()) && c.getType().equals(groupType)) {
							return false;
						}

						if (c.getAttribute(IDebugUIConstants.ATTR_PRIVATE, false)) {
							return false;
						}

						return GroupLaunchConfigurationTabGroup.isValidLaunchReference(c);
					}
					return true;
				} catch (CoreException e) {
					return false;
				}
			}
		};
	}

	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	@Override
	protected Control createContents(Composite parent) {
		Control x = super.createContents(parent);
		validate();
		setErrorMessage(null);
		return x;
	}

	@Override
	protected Control createDialogArea(Composite parent2) {
		Composite comp = (Composite) super.createDialogArea(parent2);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(comp);

		// title bar
		getShell().setText(fForEditing ? DebugUIMessages.GroupLaunchConfigurationSelectionDialog_13 : DebugUIMessages.GroupLaunchConfigurationSelectionDialog_12);

		// dialog message area (not title bar)
		setTitle(fForEditing ? DebugUIMessages.GroupLaunchConfigurationSelectionDialog_15 : DebugUIMessages.GroupLaunchConfigurationSelectionDialog_14);

		Map<String, ILaunchGroup> modes = GroupLaunchConfigurationTabGroup.getModes();

		// the tree requires a non-null group. use inherit as dummy as this will
		// not cause filtering.
		ILaunchGroup launchGroup = modes.get(GroupLaunchElement.MODE_INHERIT);
		LaunchConfigurationFilteredTree fTree = new LaunchConfigurationFilteredTree(comp, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, new PatternFilter(), launchGroup, null);
		fTree.createViewControl();
		ViewerFilter[] filters = fTree.getViewer().getFilters();
		for (ViewerFilter viewerFilter : filters) {
			if (viewerFilter instanceof LaunchGroupFilter) {
				fTree.getViewer().removeFilter(viewerFilter);
			}
		}
		fTree.getViewer().addFilter(emptyTypeFilter);
		fTree.getViewer().addSelectionChangedListener(this);
		if (fInitialSelection != null) {
			fTree.getViewer().setSelection(fInitialSelection, true);
		}
		GridDataFactory.fillDefaults().grab(true, true).hint(convertWidthInCharsToPixels(100), convertHeightInCharsToPixels(15)).applyTo(fTree.getViewer().getControl());

		Composite additionalSettings = new Composite(comp, SWT.NONE);
		additionalSettings.setLayout(new GridLayout(4, false));
		additionalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createModeSelectionControl(modes, additionalSettings);
		createPostLaunchControl(additionalSettings);

		// skip the first cell and put the checkbox in the second one
		Composite c = new Composite(additionalSettings, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(c);
		GridDataFactory.fillDefaults().applyTo(c);

		Button chkAdopt = new Button(additionalSettings, SWT.CHECK);
		chkAdopt.setText(DebugUIMessages.GroupLaunchConfigurationSelectionDialog_adoptText);
		chkAdopt.setToolTipText(DebugUIMessages.GroupLaunchConfigurationSelectionDialog_adoptTooltip);
		chkAdopt.setSelection(adoptIfRunning);
		chkAdopt.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				adoptIfRunning = chkAdopt.getSelection();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(chkAdopt);

		return comp;
	}

	private void createModeSelectionControl(Map<String, ILaunchGroup> modes, Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		label.setText(DebugUIMessages.GroupLaunchConfigurationSelectionDialog_4);

		Map<String, String> labelToMode = new LinkedHashMap<>();
		modes.forEach((modeName, launchGrp) -> {
			String launchGrpLabel = DebugUIPlugin.removeAccelerators(launchGrp.getLabel());
			labelToMode.put(launchGrpLabel, modeName);
		});

		Combo cvMode = new Combo(comp, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().applyTo(cvMode);
		cvMode.setItems(labelToMode.keySet().toArray(new String[labelToMode.size()]));

		// initial selection to the current mode.
		int index = 0;
		for (String m : modes.keySet()) {
			if (m.equals(mode)) {
				cvMode.select(index);
				break;
			}
			index++;
		}

		cvMode.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mode = labelToMode.get(cvMode.getText());
				validate();
			}
		});

		// fill up the remaining two cells in the parent layout
		Composite c = new Composite(comp, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(c);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(c);
	}

	private void createPostLaunchControl(Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		label.setText(DebugUIMessages.GroupLaunchConfigurationSelectionDialog_8);
		Combo combo = new Combo(comp, SWT.READ_ONLY);
		combo.add(GroupElementPostLaunchAction.NONE.getDescription());
		combo.add(GroupElementPostLaunchAction.WAIT_FOR_TERMINATION.getDescription());
		combo.add(GroupElementPostLaunchAction.DELAY.getDescription());
		combo.add(GroupElementPostLaunchAction.OUTPUT_REGEXP.getDescription());
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String actionStr = ((Combo) e.widget).getText();
				action = GroupElementPostLaunchAction.valueOfDescription(actionStr);
				showHideDelayAmountWidgets();
				validate();
			}
		});
		combo.setText(action.getDescription());
		GridDataFactory.fillDefaults().applyTo(combo);

		fActionParamLabel = new Label(comp, SWT.NONE);
		fActionParamWidget = new Text(comp, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().applyTo(fActionParamWidget);
		fActionParamWidget.addModifyListener(e -> {
			String text = ((Text) e.widget).getText();
			if (action == GroupElementPostLaunchAction.DELAY) {
				try {
					actionParam = Integer.valueOf(text);
				} catch (NumberFormatException exc) {
					actionParam = null;
				}
			} else if (action == GroupElementPostLaunchAction.OUTPUT_REGEXP) {
				actionParam = text;
			}
			validate();
		});
		if (actionParam instanceof Integer) {
			fActionParamWidget.setText(((Integer) actionParam).toString());
		} else if (actionParam instanceof String) {
			fActionParamWidget.setText(actionParam.toString());
		}

		showHideDelayAmountWidgets();
	}

	private void showHideDelayAmountWidgets() {
		final boolean visible = (action == GroupElementPostLaunchAction.DELAY || action == GroupElementPostLaunchAction.OUTPUT_REGEXP);
		fActionParamLabel.setVisible(visible);
		fActionParamWidget.setVisible(visible);

		if (action == GroupElementPostLaunchAction.DELAY) {
			fActionParamLabel.setText(DebugUIMessages.GroupLaunchConfigurationSelectionDialog_9);
		} else if (action == GroupElementPostLaunchAction.OUTPUT_REGEXP) {
			fActionParamLabel.setText(DebugUIMessages.GroupLaunchConfigurationSelectionDialog_regexp);
		}

		fActionParamLabel.getParent().layout();
	}

	public ILaunchConfiguration[] getSelectedLaunchConfigurations() {
		List<ILaunchConfiguration> configs = new ArrayList<>();
		if (fSelection != null && !fSelection.isEmpty()) {
			for (Iterator<?> iter = ((IStructuredSelection) fSelection).iterator(); iter.hasNext();) {
				Object selection = iter.next();
				if (selection instanceof ILaunchConfiguration) {
					configs.add((ILaunchConfiguration) selection);
				}
			}
		}
		return configs.toArray(new ILaunchConfiguration[configs.size()]);
	}

	public String getMode() {
		return mode;
	}

	public GroupElementPostLaunchAction getAction() {
		return action;
	}

	public boolean getAdoptIfRunning() {
		return adoptIfRunning;
	}

	public Object getActionParam() {
		return actionParam;
	}

	public static GroupLaunchConfigurationSelectionDialog createDialog(Shell shell, String initMode, boolean forEditing, ILaunchConfiguration self) {
		return new GroupLaunchConfigurationSelectionDialog(shell, initMode, forEditing, self);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		fSelection = event.getSelection();
		validate();
	}

	protected void validate() {
		Button ok_button = getButton(IDialogConstants.OK_ID);
		boolean isValid = true;
		if (getSelectedLaunchConfigurations().length < 1) {
			setErrorMessage(DebugUIMessages.GroupLaunchConfigurationSelectionDialog_7);
			isValid = false;
		} else {
			setErrorMessage(null);
		}

		if (isValid) {
			if (fForEditing) {
				// must have only one selection
				if (getSelectedLaunchConfigurations().length > 1) {
					setErrorMessage(DebugUIMessages.GroupLaunchConfigurationSelectionDialog_11);
					isValid = false;
				}
			}
		}

		try {
			for (ILaunchConfiguration sel : getSelectedLaunchConfigurations()) {
				if (isValid && sel.getType().equals(groupType)) {
					// check whether there is a recursive reference to self
					isValid = !hasSelfRecursive(sel);
					setErrorMessage(isValid ? null : DebugUIMessages.GroupLaunchConfigurationSelectionDialog_0);
				}
				if (isValid && !GroupLaunchElement.MODE_INHERIT.equals(mode)) {
					if (!sel.supportsMode(mode)) {
						isValid = false;
					}
					setErrorMessage(isValid ? null : DebugUIMessages.GroupLaunchConfigurationSelectionDialog_1);
				}
				if (isValid && sel.isPrototype()) {
					isValid = false;
					setErrorMessage(DebugUIMessages.GroupLaunchConfigurationSelectionDialog_2);
				}
				if (!isValid) {
					break;
				}
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}


		if (isValid) {
			if (action == GroupElementPostLaunchAction.DELAY) {
				isValid = (actionParam instanceof Integer) && ((Integer) actionParam > 0);
				setErrorMessage(isValid ? null : DebugUIMessages.GroupLaunchConfigurationSelectionDialog_10);
			}

			if (action == GroupElementPostLaunchAction.OUTPUT_REGEXP) {
				isValid = actionParam instanceof String && !((String) actionParam).isEmpty();
				if (isValid) {
					try {
						Pattern.compile((String) actionParam);
					} catch (Exception e) {
						isValid = false;
					}
				}
				setErrorMessage(isValid ? null : DebugUIMessages.GroupLaunchConfigurationSelectionDialog_errorNoRegexp);
			}
		}

		if (ok_button != null) {
			ok_button.setEnabled(isValid);
		}
	}

	private boolean hasSelfRecursive(ILaunchConfiguration c) throws CoreException {
		if (c == null) {
			return false;
		}

		if (c.getType().equals(groupType)) {
			// it's a launch group
			if (c.getName().equals(selfRef.getName())) {
				return true;
			}

			// recurse to all elements of the group
			for (GroupLaunchElement e : GroupLaunchConfigurationDelegate.createLaunchElements(c)) {
				// if any of the contained configs is self
				if (hasSelfRecursive(e.data)) {
					return true;
				}
			}
		}

		return false;
	}

	public void setInitialSelection(GroupLaunchElement el) {
		action = el.action;
		actionParam = el.actionParam;
		adoptIfRunning = el.adoptIfRunning;
		fInitialSelection = new StructuredSelection(el.data);
		fSelection = fInitialSelection;
	}

}
