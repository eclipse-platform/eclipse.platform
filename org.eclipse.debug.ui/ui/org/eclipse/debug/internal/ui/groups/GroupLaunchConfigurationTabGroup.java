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
 *      IBM Corporation - Bug 517809
 *******************************************************************************/
package org.eclipse.debug.internal.ui.groups;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.groups.GroupLaunchConfigurationDelegate;
import org.eclipse.debug.internal.core.groups.GroupLaunchElement;
import org.eclipse.debug.internal.core.groups.GroupLaunchElement.GroupElementPostLaunchAction;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

/**
 * Tab group for Launch Group.
 *
 * @since 3.12
 */
public class GroupLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {
	static class ContentProvider implements IStructuredContentProvider, ITreeContentProvider {
		protected List<GroupLaunchElement> input;

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public void dispose() {
			input = null;
		}

		@Override
		@SuppressWarnings("unchecked") // nothing we can do about this
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput instanceof List<?>) {
				input = (List<GroupLaunchElement>) newInput;
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return (parentElement == input) ? input.toArray() : null;
		}

		@Override
		public Object getParent(Object element) {
			return (element == input) ? null : input;
		}

		@Override
		public boolean hasChildren(Object element) {
			return (element == input) ? (input.size() > 0) : false;
		}
	}
	static class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof GroupLaunchElement)) {
				return null;
			}
			if (columnIndex == 0) {
				GroupLaunchElement el = (GroupLaunchElement) element;
				if (el.data == null || !isValidLaunchReference(el.data)) {
					Image errorImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
					return errorImage;
				}

				try {
					String key = el.data.getType().getIdentifier();
					return DebugPluginImages.getImage(key);
				} catch (CoreException e) {
					Image errorImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
					return errorImage;
				}
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof GroupLaunchElement)) {
				return null;
			}
			GroupLaunchElement el = (GroupLaunchElement) element;

			// launch name
			if (columnIndex == 0) {
				try {
					return (el.data != null) ? el.data.getType().getName() + "::" + el.name : el.name; //$NON-NLS-1$
				} catch (CoreException e) {
					return el.name;
				}
			}

			// launch mode
			if (columnIndex == 1) {
				return getLabel(el.mode) + (el.adoptIfRunning ? DebugUIMessages.GroupLaunchConfigurationTabGroup_lblAdopt : ""); //$NON-NLS-1$
			}

			// launch post action
			if (columnIndex == 2) {
				GroupElementPostLaunchAction action = el.action;
				switch (action) {
					case NONE:
						return ""; //$NON-NLS-1$
					case WAIT_FOR_TERMINATION:
						return action.getDescription();
					case DELAY:
						final Object actionParam = el.actionParam;
						return NLS.bind(DebugUIMessages.GroupLaunchConfigurationTabGroup_13, actionParam instanceof Integer ? Integer.toString((Integer) actionParam) : "?"); //$NON-NLS-1$
					case OUTPUT_REGEXP:
						return NLS.bind(DebugUIMessages.GroupLaunchConfigurationTabGroup_0, el.actionParam);
					default:
						assert false : "new post launch action missing logic here"; //$NON-NLS-1$
						return ""; //$NON-NLS-1$
				}
			}
			return null;
		}
	}

	static class CheckStateProvider implements ICheckStateProvider {

		@Override
		public boolean isChecked(Object element) {
			if (element instanceof GroupLaunchElement) {
				return ((GroupLaunchElement)element).enabled;
			}
			return false;
		}

		@Override
		public boolean isGrayed(Object element) {
			return false;
		}
	}
	static abstract class ButtonComposite extends Composite implements SelectionListener {
		Button upButton;
		Button downButton;
		Button addButton;
		Button deleteButton;
		Button editButton;

		public ButtonComposite(Composite parent, int style) {
			super(parent, style);
			setLayout(new GridLayout());
			upButton = createPushButton(this, DebugUIMessages.GroupLaunchConfigurationTabGroup_1);
			downButton = createPushButton(this, DebugUIMessages.GroupLaunchConfigurationTabGroup_2);
			editButton = createPushButton(this, DebugUIMessages.GroupLaunchConfigurationTabGroup_3);
			addButton = createPushButton(this, DebugUIMessages.GroupLaunchConfigurationTabGroup_4);
			deleteButton = createPushButton(this, DebugUIMessages.GroupLaunchConfigurationTabGroup_5);
		}

		protected abstract void updateWidgetEnablement();

		/**
		 * Helper method to create a push button.
		 *
		 * @param parent
		 *            the parent control
		 * @param key
		 *            the resource name used to supply the button's label text
		 * @return Button
		 */
		protected Button createPushButton(Composite parent, String key) {
			Button button = SWTFactory.createPushButton(parent, key, null);
			button.addSelectionListener(this);
			return button;
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// nothing
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			Widget widget = e.widget;
			if (widget == upButton) {
				upPressed();
			} else if (widget == downButton) {
				downPressed();
			} else if (widget == addButton) {
				addPressed();
			} else if (widget == deleteButton) {
				deletePressed();
			} else if (widget == editButton) {
				editPressed();
			}
		}

		protected abstract void addPressed();

		protected abstract void editPressed();

		protected abstract void deletePressed();

		protected abstract void downPressed();

		protected abstract void upPressed();
	}
	static class GroupLaunchTab extends AbstractLaunchConfigurationTab {
		protected CheckboxTreeViewer treeViewer;
		protected List<GroupLaunchElement> input = new ArrayList<>();

		/**
		 * copy of the initial state of the configuration used for cycle
		 * checking. This is not updated when the user changes settings!
		 */
		private ILaunchConfiguration self;

		@Override
		public void createControl(Composite parent) {
			Composite comp = new Composite(parent, SWT.NONE);
			setControl(comp);
			//comp.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
			comp.setLayout(new GridLayout(2, false));
			treeViewer = new CheckboxTreeViewer(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
			Tree table = treeViewer.getTree();
			table.setFont(parent.getFont());
			treeViewer.setContentProvider(new ContentProvider());
			treeViewer.setLabelProvider(new LabelProvider());
			treeViewer.setCheckStateProvider(new CheckStateProvider());
			table.setHeaderVisible(true);
			table.setLayoutData(new GridData(GridData.FILL_BOTH));
			TreeColumn col1 = new TreeColumn(table, SWT.NONE);
			col1.setText(DebugUIMessages.GroupLaunchConfigurationTabGroup_6);
			col1.setWidth(300);
			TreeColumn col2 = new TreeColumn(table, SWT.NONE);
			col2.setText(DebugUIMessages.GroupLaunchConfigurationTabGroup_7);
			col2.setWidth(100);
			TreeColumn col3 = new TreeColumn(table, SWT.NONE);
			col3.setText(DebugUIMessages.GroupLaunchConfigurationTabGroup_12);
			col3.setWidth(100);

			treeViewer.setInput(input);
			final ButtonComposite buts = new ButtonComposite(comp, SWT.NONE) {
				@Override
				protected void addPressed() {
					GroupLaunchConfigurationSelectionDialog dialog =
						GroupLaunchConfigurationSelectionDialog.createDialog(
									treeViewer.getControl().getShell(), GroupLaunchElement.MODE_INHERIT, false, self);
					if (dialog.open() == Window.OK) {
						ILaunchConfiguration[] configs = dialog.getSelectedLaunchConfigurations();
						if (configs.length < 1) {
							return;
						}
						for (ILaunchConfiguration config : configs) {
							GroupLaunchElement el = new GroupLaunchElement();
							input.add(el);
							el.index = input.size() - 1;
							el.enabled = true;
							applyFromDialog(el, dialog, config);
							treeViewer.refresh(true);
							treeViewer.setChecked(el, el.enabled);
						}
						updateWidgetEnablement();
						updateLaunchConfigurationDialog();
					}
				}
				@Override
				protected void updateWidgetEnablement(){
					downButton.setEnabled(isDownEnabled());
					upButton.setEnabled(isUpEnabled());

					int selectionCount = getSelectionCount();
					editButton.setEnabled(selectionCount == 1);
					deleteButton.setEnabled(selectionCount > 0);
				}


				@Override
				protected void editPressed() {
					int index = getSingleSelectionIndex();
					if (index < 0) {
						return;
					}
					GroupLaunchElement el = input.get(index);
					GroupLaunchConfigurationSelectionDialog dialog =
						GroupLaunchConfigurationSelectionDialog.createDialog(
									treeViewer.getControl().getShell(), el.mode, true, self);
					if (isValidLaunchReference(el.data)) {
						dialog.setInitialSelection(el);
					}
					if (dialog.open() == Window.OK) {
						ILaunchConfiguration[] confs = dialog.getSelectedLaunchConfigurations();
						if (confs.length < 0) {
							return;
						}
						assert confs.length == 1 : "invocation of the dialog for editing an entry sholdn't allow OK to be hit if the user chooses multiple launch configs in the dialog"; //$NON-NLS-1$
						applyFromDialog(el, dialog, confs[0]);
						treeViewer.refresh(true);
						updateWidgetEnablement();
						updateLaunchConfigurationDialog();
					}
				}

				private void applyFromDialog(GroupLaunchElement el, GroupLaunchConfigurationSelectionDialog dialog, ILaunchConfiguration config) {
					el.name = config.getName();
					el.data = config;
					el.mode = dialog.getMode();
					el.action = dialog.getAction();
					el.adoptIfRunning = dialog.getAdoptIfRunning();
					el.actionParam = dialog.getActionParam();
				}

				@Override
				protected void deletePressed() {
					int[] indices = getMultiSelectionIndices();
					if (indices.length < 1) {
						return;
					}
					// need to delete from high to low
					for (int i = indices.length - 1; i >= 0; i--) {
						input.remove(indices[i]);
					}
					treeViewer.refresh(true);
					updateWidgetEnablement();
					updateLaunchConfigurationDialog();
				}

				/**
				 * @return the index of the selection if a single item is
				 *         selected. If zero or multiple are selected, -1 is
				 *         returned
				 */
				private int getSingleSelectionIndex() {
					StructuredSelection sel = (StructuredSelection) treeViewer.getSelection();
					if (sel.size() != 1) {
						return -1;
					}
					GroupLaunchElement el = ((GroupLaunchElement) sel
							.getFirstElement());
					return input.indexOf(el);
				}

				/**
				 * @return the indices of one or more selected items. Indices
				 *         are always returned in ascending order
				 */
				private int[] getMultiSelectionIndices() {
					StructuredSelection sel = (StructuredSelection) treeViewer.getSelection();
					List<Integer> indices = new ArrayList<>();

					for (Iterator<?> iter = sel.iterator(); iter.hasNext(); ) {
						GroupLaunchElement el = (GroupLaunchElement) iter.next();
						indices.add(input.indexOf(el));

					}
					int[] result = new int[indices.size()];
					for (int i = 0; i < result.length; i++) {
						result[i] = indices.get(i);
					}
					return result;
				}

				private int getSelectionCount() {
					return ((StructuredSelection)treeViewer.getSelection()).size();
				}


				@Override
				protected void downPressed() {
					if (!isDownEnabled()) {
						return;
					}
					int index = getSingleSelectionIndex();

					GroupLaunchElement x = input.get(index);
					input.set(index, input.get(index + 1));
					input.set(index + 1, x);
					treeViewer.refresh(true);
					updateWidgetEnablement();
					updateLaunchConfigurationDialog();
				}

				protected boolean isDownEnabled() {
					final int index = getSingleSelectionIndex();
					return (index >= 0) && (index != input.size() - 1);
				}

				protected boolean isUpEnabled(){
					return getSingleSelectionIndex() > 0;
				}

				@Override
				protected void upPressed() {
					if (!isUpEnabled()) {
						return;
					}
					int index = getSingleSelectionIndex();
					GroupLaunchElement x = input.get(index);
					input.set(index, input.get(index - 1));
					input.set(index - 1, x);
					treeViewer.refresh(true);
					updateWidgetEnablement();
					updateLaunchConfigurationDialog();
				}
			};
			treeViewer.addSelectionChangedListener(event -> buts.updateWidgetEnablement());

			treeViewer.getTree().addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					buts.editPressed();
				}
			});

			treeViewer.addCheckStateListener(event -> {
				((GroupLaunchElement)event.getElement()).enabled = event.getChecked();
				updateLaunchConfigurationDialog();
			});
			buts.updateWidgetEnablement();
			GridData layoutData = new GridData(GridData.GRAB_VERTICAL);
			layoutData.verticalAlignment = SWT.BEGINNING;
			buts.setLayoutData(layoutData);
		}

		@Override
		public String getName() {
			return DebugUIMessages.GroupLaunchConfigurationTabGroup_10;
		}

		@Override
		public Image getImage() {
			return DebugUITools.getImage(IInternalDebugUIConstants.IMG_OBJS_LAUNCH_GROUP);
		}

		@Override
		public void initializeFrom(ILaunchConfiguration configuration) {
			try {
				self = configuration.copy(configuration.getName());
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}

			// replace the input from previously shown launch configurations
			input = GroupLaunchConfigurationDelegate.createLaunchElements(configuration);
			if (treeViewer != null) {
				treeViewer.setInput(input);
			}
		}

		@Override
		public void performApply(ILaunchConfigurationWorkingCopy configuration) {
			GroupLaunchConfigurationDelegate.storeLaunchElements(configuration, input);
		}

		@Override
		public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
			// defaults is empty list
		}

		@Override
		public boolean isValid(ILaunchConfiguration launchConfig) {
			setMessage(null);
			setErrorMessage(null);
			int validLaunches = 0;
			// test if each launch is valid
			for (GroupLaunchElement element : input) {
				if (element.enabled) {
					if ( element.data == null) {
						// error referencing invalid launch
						setErrorMessage(MessageFormat.format(DebugUIMessages.GroupLaunchConfigurationTabGroup_14,
								element.name));
						return false;
					} else if (!isValidLaunchReference(element.data)) {
						// error referencing invalid launch
						setErrorMessage(MessageFormat.format(DebugUIMessages.GroupLaunchConfigurationTabGroup_15,
								element.name));
						return false;
					}
					validLaunches++;
				}
			}
			if (validLaunches < 1) {
				// must have at least one valid and enabled launch
				setErrorMessage(DebugUIMessages.GroupLaunchConfigurationTabGroup_16);
				return false;
			}
			return true;
		}
	}

	public GroupLaunchConfigurationTabGroup() {
		// nothing
	}

	/**
	 * Test if a launch configuration is a valid reference.
	 *
	 * @param config configuration reference
	 * @return <code>true</code> if it is a valid reference, <code>false</code>
	 *         if launch configuration should be filtered
	 */
	public static boolean isValidLaunchReference(ILaunchConfiguration config) {
		if (config == null) {
			return false;
		}
		return DebugUIPlugin.doLaunchConfigurationFiltering(config) && !WorkbenchActivityHelper.filterItem(config);
	}

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {//
				new GroupLaunchTab(), //
				new CommonTabLite() //
		};
		setTabs(tabs);
	}

	public static Map<String, ILaunchGroup> getModes() {
		if (modes == null) {
			initializeModes();
		}
		return modes;
	}

	private static Map<String, ILaunchGroup> modes;

	/**
	 * Required to satisfy the tree in mode inherit.
	 */
	private static final class InheritModeGroup implements ILaunchGroup {

		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		@Override
		public ImageDescriptor getBannerImageDescriptor() {
			return null;
		}

		@Override
		public String getLabel() {
			return DebugCoreMessages.GroupLaunchElement_inherit_launch_mode_label;
		}

		@Override
		public String getIdentifier() {
			return null;
		}

		@Override
		public String getCategory() {
			return null;
		}

		@Override
		public String getMode() {
			return null;
		}

		@Override
		public boolean isPublic() {
			return false;
		}

	}

	private static synchronized void initializeModes() {
		if (modes != null) {
			return;
		}
		modes = new LinkedHashMap<>();
		modes.put(GroupLaunchElement.MODE_INHERIT, new InheritModeGroup());
		Set<ILaunchGroup> sortedGroups = new TreeSet<>(Comparator.comparing(ILaunchGroup::getLabel));
		LaunchConfigurationManager mgr = DebugUIPlugin.getDefault().getLaunchConfigurationManager();
		sortedGroups.addAll(Arrays.asList(mgr.getLaunchGroups()));
		for (ILaunchGroup launchGroup : sortedGroups) {
			LaunchHistory history = mgr.getLaunchHistory(launchGroup.getIdentifier());
			if (history == null) {
				// mode currently not supported.
				continue;
			}

			String modeName = launchGroup.getMode();
			if (!modes.containsKey(modeName)) {
				modes.put(modeName, launchGroup);
			}
		}
	}

	public static String getLabel(String mode) {
		if (mode == null || mode.isEmpty())
		{
			return ""; //$NON-NLS-1$
		}
		if (modes == null) {
			initializeModes();
		}
		ILaunchGroup launchGrp = modes.get(mode);
		if (launchGrp == null) {
			return ""; //$NON-NLS-1$
		}
		return DebugUIPlugin.removeAccelerators(launchGrp.getLabel());
	}
}
