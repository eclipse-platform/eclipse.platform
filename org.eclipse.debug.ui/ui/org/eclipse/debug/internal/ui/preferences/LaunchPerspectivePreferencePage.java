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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 490755
 *******************************************************************************/
package org.eclipse.debug.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.LaunchDelegate;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchCategoryFilter;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationPresentationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.PerspectiveManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.ActivityManagerEvent;
import org.eclipse.ui.activities.IActivityManagerListener;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.model.WorkbenchViewerComparator;

/**
 * The preference page for selecting and changing launch perspectives
 *
 * @since 3.3
 */
public class LaunchPerspectivePreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IActivityManagerListener {

	/**
	 * Represents a perspective delta for a given type, delegate and mode set combination.
	 */
	final class PerspectiveChange {
		private ILaunchConfigurationType fType = null;
		private ILaunchDelegate fDelegate = null;
		private Set<String> fModes = null;
		private String fPid = null;

		public PerspectiveChange(ILaunchConfigurationType type, ILaunchDelegate delegate, Set<String> modes, String perspectiveid) {
			fType = type;
			fDelegate = delegate;
			fModes = modes;
			fPid = perspectiveid;
		}

		public ILaunchConfigurationType getType() {return fType;}
		public ILaunchDelegate getDelegate() {return fDelegate;}
		public String getPerspectiveId() {return fPid;}

		public Set<String> getModes() {
			return fModes;
		}
		@Override
		public boolean equals(Object o) {
			if(o instanceof PerspectiveChange) {
				PerspectiveChange change = (PerspectiveChange) o;
				return change.getDelegate() == fDelegate &&
						change.getType().equals(fType) &&
						change.getModes().equals(fModes);
			}
			return super.equals(o);
		}

		@Override
		public int hashCode() {
			return (fDelegate != null ? fDelegate.hashCode() : 0) + fType.hashCode() + fModes.hashCode();
		}
	}

	/**
	 * Implementation to expose use of getFilteredChildren method
	 */
	final class PerspectivesTreeViewer extends TreeViewer {
		public PerspectivesTreeViewer(Tree tree) {
			super(tree);
		}
		@Override
		public Object[] getFilteredChildren(Object o) {return super.getFilteredChildren(o);}
	}

	/**
	 * Provides content for the configuration tree viewer
	 */
	static final class PerspectiveContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof ILaunchConfigurationType) {
				ILaunchConfigurationType type = (ILaunchConfigurationType) parentElement;
				return ((LaunchManager)DebugPlugin.getDefault().getLaunchManager()).getLaunchDelegates(type.getIdentifier());
			}
			return new Object[0];
		}
		@Override
		public Object[] getElements(Object inputElement) {
			return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationTypes();
		}
		@Override
		public boolean hasChildren(Object element) {return element instanceof ILaunchConfigurationType;}
		@Override
		public Object getParent(Object element) {return null;}
	}

	/**
	 * Panel container that is reused to present series of combo boxes to users for perspective selections
	 */
	class PerspectivesPanel {

		private Composite fMainComposite = null;
		private Label fMessage = null;

		public PerspectivesPanel(Composite parent, String heading) {
			createPanel(parent, heading);
		}

		protected void createPanel(Composite parent, String heading) {
			fMainComposite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
			SWTFactory.createWrapLabel(fMainComposite, heading, 2);
			fMessage = SWTFactory.createWrapLabel(fMainComposite, IInternalDebugCoreConstants.EMPTY_STRING, 2, 250);
		}

		public void setMessage(String msg) {
			fMessage.setText((msg == null ? IInternalDebugCoreConstants.EMPTY_STRING : msg));
		}

		public void refreshPanel(IStructuredSelection selection) {
			//get rid of any existing children, but leave the first two (the label for the control, and the message area)
			Control[] children = fMainComposite.getChildren();
			for(int i = 2; i < children.length; i++) {
				children[i].dispose();
			}
			if(fgCurrentWorkingContext == null) {
				fgCurrentWorkingContext = new HashSet<>();
			}
			fgCurrentWorkingContext.clear();
			if(!selection.isEmpty()) {
				createCombos(fMainComposite, selection.toArray());
				fMainComposite.layout();
			}
			else {
				SWTFactory.createWrapLabel(fMainComposite, DebugPreferencesMessages.LaunchPerspectivePreferencePage_0, 2, 275);
			}
			fMainComposite.layout();
		}
	}

	/**
	 * Widgets
	 */
	private RadioGroupFieldEditor fSwitchLaunch = null;
	private RadioGroupFieldEditor fSwitchSuspend = null;
	private Tree fTree = null;
	private PerspectivesTreeViewer fTreeViewer = null;
	private PerspectivesPanel fPerspectivesPanel = null;

	/**
	 * Caches
	 */
	private String[] fgPerspectiveLabels = null;
	private Map<String, String> fgPerspectiveIdMap = null;
	private HashSet<PerspectiveChange> fgChangeSet = null;
	private HashSet<Object> fgCurrentWorkingContext = null;

	/**
	 * A default selection listener to be reused by all combo boxes presenting perspective data
	 */
	private SelectionListener fSelectionListener = new SelectionListener() {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {}

		@Override
		public void widgetSelected(SelectionEvent e) {
			Object o = e.getSource();
			if(o instanceof Combo) {
				Combo combo = (Combo) o;
				LaunchDelegate delegate = null;
				ILaunchConfigurationType type = null;
				PerspectiveChange change = null;
				for (Iterator<Object> iter = fgCurrentWorkingContext.iterator(); iter.hasNext();) {
					o = iter.next();
					if(o instanceof ILaunchDelegate) {
						delegate = (LaunchDelegate) o;
						type = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(delegate.getLaunchConfigurationTypeId());
					}
					else if(o instanceof ILaunchConfigurationType) {
						delegate = null;
						type = (ILaunchConfigurationType) o;
					}
					@SuppressWarnings("unchecked")
					Set<String> modes = (Set<String>) combo.getData();
					change = findChange(type, delegate, modes);
					if(change == null) {
						change = new PerspectiveChange(type, delegate, modes, fgPerspectiveIdMap.get(combo.getText()));
						fgChangeSet.add(change);
					}
					else {
						change.fPid = fgPerspectiveIdMap.get(combo.getText());
					}
				}
			}
		}
	};

	/**
	 * Constructor
	 */
	public LaunchPerspectivePreferencePage() {}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getActivitySupport().getActivityManager().removeActivityManagerListener(this);
		fgPerspectiveIdMap.clear();
		fgPerspectiveIdMap = null;
		fgPerspectiveLabels = null;
		fgChangeSet.clear();
		fgChangeSet = null;
		if(fgCurrentWorkingContext != null) {
			fgCurrentWorkingContext.clear();
			fgCurrentWorkingContext = null;
		}
		super.dispose();
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDebugHelpContextIds.PERSPECTIVE_PREFERENCE_PAGE);
	}

	@Override
	protected Control createContents(Composite parent) {

		SWTFactory.createWrapLabel(parent, DebugPreferencesMessages.PerspectivePreferencePage_0, 2, 300);

		SWTFactory.createVerticalSpacer(parent, 1);

		fSwitchLaunch = new RadioGroupFieldEditor(
				IInternalDebugUIConstants.PREF_SWITCH_TO_PERSPECTIVE,
				DebugPreferencesMessages.LaunchingPreferencePage_11, 3,
				new String[][] {{DebugPreferencesMessages.LaunchingPreferencePage_12, MessageDialogWithToggle.ALWAYS },
							{ DebugPreferencesMessages.LaunchingPreferencePage_13, MessageDialogWithToggle.NEVER },
							{ DebugPreferencesMessages.LaunchingPreferencePage_14, MessageDialogWithToggle.PROMPT } },
							SWTFactory.createComposite(parent, 1, 2, GridData.FILL_HORIZONTAL),
							true);
		fSwitchLaunch.setPreferenceName(IInternalDebugUIConstants.PREF_SWITCH_TO_PERSPECTIVE);
		fSwitchLaunch.setPreferenceStore(getPreferenceStore());
		fSwitchSuspend = new RadioGroupFieldEditor(
				IInternalDebugUIConstants.PREF_SWITCH_PERSPECTIVE_ON_SUSPEND,
				DebugPreferencesMessages.DebugPreferencePage_21, 3,
				new String[][] {{ DebugPreferencesMessages.DebugPreferencePage_22, MessageDialogWithToggle.ALWAYS },
								{ DebugPreferencesMessages.DebugPreferencePage_23, MessageDialogWithToggle.NEVER },
								{ DebugPreferencesMessages.DebugPreferencePage_24, MessageDialogWithToggle.PROMPT } },
								SWTFactory.createComposite(parent, 1, 2, GridData.FILL_HORIZONTAL),
								true);
		fSwitchSuspend.setPreferenceName(IInternalDebugUIConstants.PREF_SWITCH_PERSPECTIVE_ON_SUSPEND);
		fSwitchSuspend.setPreferenceStore(getPreferenceStore());

		SWTFactory.createVerticalSpacer(parent, 1);
		SWTFactory.createWrapLabel(parent, DebugPreferencesMessages.PerspectivePreferencePage_5, 2, 300);
		Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 2, 1, GridData.FILL_BOTH, 0, 0);
		createTreeViewer(comp);
		fPerspectivesPanel = new PerspectivesPanel(comp, DebugPreferencesMessages.PerspectivePreferencePage_2);
		initializeControls();
		PlatformUI.getWorkbench().getActivitySupport().getActivityManager().addActivityManagerListener(this);
		Dialog.applyDialogFont(parent);
		return parent;
	}

	/**
	 * Creates the <code>Tree</code> and <code>TreeViewer</code> widgets
	 * @param parent the parent to add these components to
	 */
	protected void createTreeViewer(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_VERTICAL);
		SWTFactory.createWrapLabel(comp, DebugPreferencesMessages.PerspectivePreferencePage_1, 1);
		fTree = new Tree(comp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		gd.widthHint = 220;
		gd.heightHint = 250;
		fTree.setLayoutData(gd);
		fTreeViewer = new PerspectivesTreeViewer(fTree);
		fTreeViewer.addSelectionChangedListener(event -> fPerspectivesPanel.refreshPanel(event.getStructuredSelection()));
		fTreeViewer.addDoubleClickListener(event -> {
			IStructuredSelection ss = (IStructuredSelection) event.getSelection();
			if(!ss.isEmpty()) {
				Object obj = ss.getFirstElement();
				fTreeViewer.setExpandedState(obj, !fTreeViewer.getExpandedState(obj));
			}
		});
		fTreeViewer.setLabelProvider(DebugUITools.newDebugModelPresentation());
		fTreeViewer.setComparator(new WorkbenchViewerComparator());
		fTreeViewer.setContentProvider(new PerspectiveContentProvider());
		// filter external tool builders
		fTreeViewer.addFilter(new LaunchCategoryFilter(IInternalDebugUIConstants.ID_EXTERNAL_TOOL_BUILDER_LAUNCH_CATEGORY));
		fTreeViewer.setInput(DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationTypes());
	}

	/**
	 * Creates a set of combo boxes on a per-selection basis that display a listing of available perspectives to switch to
	 * @param parent the parent to add the created combo boxes to
	 * @param selection the selection in the tree viewer
	 */
	protected void createCombos(Composite parent, Object[] selection) {
		Set<Set<String>> modes = collectCommonModeSets(selection);
		if(modes.isEmpty()) {
			fPerspectivesPanel.setMessage(DebugPreferencesMessages.LaunchPerspectivePreferencePage_1);
			return;
		}
		fPerspectivesPanel.setMessage(IInternalDebugCoreConstants.EMPTY_STRING);
		List<String> fmodes = null;
		Combo combo = null;
		for (Set<String> smodes : modes) {
			fmodes = LaunchConfigurationPresentationManager.getDefault().getLaunchModeNames(smodes);
			if(!fmodes.isEmpty()) {
				//add the mode set and create a combo
				String modeString= fmodes.size() == 1 ? fmodes.get(0) : fmodes.toString();
				SWTFactory.createLabel(parent, modeString + ":", 1); //$NON-NLS-1$
				combo = SWTFactory.createCombo(parent, SWT.READ_ONLY, 1, fgPerspectiveLabels);
				if(combo == null) {
					continue;
				}
				String text = getComboSelection(smodes);
				if(text != null) {
					combo.setText(text);
				}
				combo.setData(smodes);
				combo.addSelectionListener(fSelectionListener);
				GridData gd = (GridData)combo.getLayoutData();
				gd.grabExcessHorizontalSpace = true;
			}
		}
	}

	/**
	 * Returns the text item to select for the current combo context given the current working set context
	 * @param modes the set of modes
	 * @return the text to select in the current combo / current working set context, or "None"
	 */
	private String getComboSelection(Set<String> modes) {
		String text = DebugPreferencesMessages.PerspectivePreferencePage_4;
		IStructuredSelection ss = fTreeViewer.getStructuredSelection();
		if(ss != null && !ss.isEmpty()) {
			Object o = null;
			Set<String> tmp = new HashSet<>();
			String id = null;
			ILaunchConfigurationType type = null;
			LaunchDelegate delegate = null;
			PerspectiveChange change = null;
			for (Iterator<?> iter = ss.iterator(); iter.hasNext();) {
				o = iter.next();
				if(o instanceof LaunchDelegate) {
					delegate = (LaunchDelegate) o;
					type = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(delegate.getLaunchConfigurationTypeId());
				}
				else if(o instanceof ILaunchConfigurationType) {
					type = (ILaunchConfigurationType) o;
				}
				change = findChange(type, delegate, modes);
				if(change != null) {
					id = change.getPerspectiveId();
				}
				else {
					id = DebugUIPlugin.getDefault().getPerspectiveManager().getLaunchPerspective(type, modes, delegate);
				}
				if(id == null) {
					id = IDebugUIConstants.PERSPECTIVE_NONE;
				}
				tmp.add(id);
			}
			if(tmp.size() == 1) {
				id = tmp.iterator().next();
				if(!IDebugUIConstants.PERSPECTIVE_NONE.equals(id)) {
					for (Entry<String, String> entry : fgPerspectiveIdMap.entrySet()) {
						if (id.equals(entry.getValue())) {
							return entry.getKey();
						}
					}
				}
			}
		}

		return text;
	}

	/**
	 * Traverses the current change set to find a matching change. Matching in this context considers only the
	 * type, delegate and mode set, we do not compare perspective ids, as they can change many times.
	 * @param type the type
	 * @param delegate the delegate, possibly <code>null</code>
	 * @param modes the current mode set
	 * @return the existing <code>PerspectiveChange</code> if there is one, <code>null</code> otherwise
	 */
	private PerspectiveChange findChange(ILaunchConfigurationType type, ILaunchDelegate delegate, Set<String> modes) {
		PerspectiveChange change = new PerspectiveChange(type, delegate, modes, null);
		for (PerspectiveChange ch : fgChangeSet) {
			if (change.equals(ch)) {
				return ch;
			}
		}
		return null;
	}

	/**
	 * Collects a list of mode sets that are common to the current selection
	 * context. It is possible that there are no mode sets in common.
	 *
	 * @param selection the current selection context
	 * @return a list of mode sets or an empty list, never <code>null</code>
	 */
	protected Set<Set<String>> collectCommonModeSets(Object[] selection) {

	//prep selection context, remove types from the equation
		HashSet<ILaunchDelegate> delegates = new HashSet<>();
		for (Object o : selection) {
			if(o instanceof ILaunchDelegate) {
				delegates.add((ILaunchDelegate) o);
			}
			else if(o instanceof ILaunchConfigurationType) {
				fgCurrentWorkingContext.add(o);
				for (Object kid : fTreeViewer.getFilteredChildren(o)) {
					delegates.add((ILaunchDelegate) kid);
				}
			}
		}
	//compare the listing of delegates to find common mode sets
		HashSet<Set<String>> common = new HashSet<>();
		HashSet<Set<String>> pruned = new HashSet<>();
		for (ILaunchDelegate delegate : delegates) {
			for (Set<String> fmodes : delegate.getModes()) {
				if (isCommonModeset(fmodes, delegates, pruned)) {
					common.add(fmodes);
					fgCurrentWorkingContext.add(delegate);
				}
			}
		}
		return common;
	}

	/**
	 * Returns if the specified mode set is common to the listing of delegates, at the same time adding any not common
	 * mode sets to a listing used to prune the search as we go along
	 * @param modeset the set to test for commonality
	 * @param delegates the listing to test against
	 * @param pruned the monotonic listing of pruned mode sets
	 * @return true if the specified mode set is common to all members of the specified listing of launch delegates, false otherwise
	 */
	private boolean isCommonModeset(Set<String> modeset, Set<ILaunchDelegate> delegates, Set<Set<String>> pruned) {
		if(!pruned.contains(modeset)) {
			boolean common = true;
			for (ILaunchDelegate delegate : delegates) {
				common &= delegate.getModes().contains(modeset);
			}
			if(!common) {
				pruned.add(modeset);
			}
			else {
				return true;
			}
		}
		return false;
	}

	/**
	 * Restores the widget state from the preference store, called after all of the widgets have been created and triggers
	 * a selection changed event from the tree viewer
	 */
	protected void initializeControls() {
		if(fTree.getItemCount() > 0) {
			TreeItem item = fTree.getItem(0);
			fTreeViewer.setSelection(new StructuredSelection(item.getData()));
			fTreeViewer.expandToLevel(item.getData(), 1);
		}
	//load the group selections
		fSwitchLaunch.load();
		fSwitchSuspend.load();
	}

	@Override
	protected void performDefaults() {
		fgChangeSet.clear();
		fSwitchLaunch.loadDefault();
		fSwitchSuspend.loadDefault();

		PerspectiveManager pm = DebugUIPlugin.getDefault().getPerspectiveManager();
		for (TreeItem item : fTree.getItems()) {
			ILaunchConfigurationType type = (ILaunchConfigurationType) item.getData();
			for (Set<String> modeset : type.getSupportedModeCombinations()) {
				fgChangeSet.add(new PerspectiveChange(type, null, modeset, pm.getDefaultLaunchPerspective(type, null, modeset)));
			}
			for (Object child : fTreeViewer.getFilteredChildren(type)) {
				ILaunchDelegate delegate = (ILaunchDelegate) child;
				for (Set<String> modeset : new HashSet<>(delegate.getModes())) {
					fgChangeSet.add(new PerspectiveChange(type, delegate, modeset, pm.getDefaultLaunchPerspective(type, delegate, modeset)));
				}
			}
		}
		if(fTree.getItemCount() > 0) {
			TreeItem item = fTree.getItem(0);
			fTreeViewer.setSelection(new StructuredSelection(item.getData()));
			fTreeViewer.expandToLevel(item.getData(), 1);
		}
		super.performDefaults();
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(DebugUIPlugin.getDefault().getPreferenceStore());
		fgChangeSet = new HashSet<>();
	//init the labels mapping and the list of labels
		fgPerspectiveIdMap = new HashMap<>();
		ArrayList<String> labels = new ArrayList<>();
		labels.add(DebugPreferencesMessages.PerspectivePreferencePage_4);
		IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
		String label = null;
		for (IPerspectiveDescriptor descriptor : registry.getPerspectives()) {
			if(!WorkbenchActivityHelper.filterItem(descriptor)) {
				label = descriptor.getLabel();
				labels.add(label);
				fgPerspectiveIdMap.put(label, descriptor.getId());
			}
		}
		fgPerspectiveLabels = labels.toArray(new String[labels.size()]);
	}

	@Override
	public void activityManagerChanged(ActivityManagerEvent activityManagerEvent) {
		if(!fTree.isDisposed()) {
			fTreeViewer.refresh();
		}
	}

	@Override
	public boolean performOk() {
		fSwitchLaunch.store();
		fSwitchSuspend.store();
		if(!fgChangeSet.isEmpty()) {
			PerspectiveManager mgr = DebugUIPlugin.getDefault().getPerspectiveManager();
			for (PerspectiveChange change : fgChangeSet) {
				mgr.setLaunchPerspective(change.getType(), change.getModes(), change.getDelegate(), change.getPerspectiveId());
			}
		}
		return super.performOk();
	}
}
