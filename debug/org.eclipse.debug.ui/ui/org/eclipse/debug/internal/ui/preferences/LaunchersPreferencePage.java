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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DefaultLabelProvider;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationPresentationManager;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchViewerComparator;

/**
 * This class provides a preference page for selecting and changing preferred launch delegates for those of them
 * that have conflicting delegates.
 *
 * Delegates are considered to be conflicting if they are for the same launch configuration type, and apply to the same
 * mode sets.
 *
 * @since 3.3
 */
public class LaunchersPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Class to collect and persist attributes to sufficiently describe a duplicate launch delegate
	 */
	static class DuplicateDelegate {
		private ILaunchConfigurationType fType = null;
		private ILaunchDelegate[] fDelegates = null;
		private Set<String> fModes = null;

		public DuplicateDelegate(ILaunchConfigurationType type, ILaunchDelegate[] delegates, Set<String> modes) {
			fModes = modes;
			fType = type;
			fDelegates = delegates;
		}

		public ILaunchConfigurationType getType() {
			return fType;
		}
		public ILaunchDelegate[] getDelegates() {
			return fDelegates;
		}

		public Set<String> getModeSet() {
			return fModes;
		}
	}

	/**
	 * label provider to extend the default one, provides labels to both the tree and table of this page
	 */
	class LabelProvider extends DefaultLabelProvider {
		@Override
		public String getText(Object element) {
			if(element instanceof ILaunchConfigurationType) {
				return super.getText(element);
			}
			else if(element instanceof DuplicateDelegate) {
				DuplicateDelegate dd = (DuplicateDelegate) element;
				return LaunchConfigurationPresentationManager.getDefault().getLaunchModeNames(dd.getModeSet()).toString();
			}
			else if(element instanceof ILaunchDelegate){
				return ((ILaunchDelegate) element).getName();
			}
			return element.toString();
		}
	}

	/**
	 * This class is used to provide content to the tree
	 */
	class TreeProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof ILaunchConfigurationType) {
				ILaunchConfigurationType type = (ILaunchConfigurationType) parentElement;
				Set<DuplicateDelegate> dupes = fDuplicates.get(type);
				if(dupes != null) {
					return dupes.toArray();
				}
				return null;
			}
			return null;
		}
		@Override
		public boolean hasChildren(Object element) {
			return element instanceof ILaunchConfigurationType;
		}
		@Override
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof Map) {
				return ((Map<?, ?>) inputElement).keySet().toArray();
			}
			return null;
		}
		@Override
		public Object getParent(Object element) {return null;}
	}

	private TreeViewer fTreeViewer = null;
	private CheckboxTableViewer fTableViewer = null;
	private Map<ILaunchConfigurationType, Set<DuplicateDelegate>> fDuplicates = null;
	private Map<DuplicateDelegate, ILaunchDelegate> fDupeSelections = null;
	private boolean fDirty = false;
	private Label fDescription = null;

	/**
	 * Constructor
	 */
	public LaunchersPreferencePage() {
		setTitle(DebugPreferencesMessages.LaunchDelegatesPreferencePage_0);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IDebugHelpContextIds.LAUNCH_DELEGATES_PREFERENCE_PAGE);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
		SWTFactory.createWrapLabel(comp, DebugPreferencesMessages.LaunchDelegatesPreferencePage_1, 2, 300);

		boolean enabled = fDuplicates.size() > 0;
		if(!enabled) {
			SWTFactory.createVerticalSpacer(comp, 1);
			SWTFactory.createWrapLabel(comp, DebugPreferencesMessages.LaunchersPreferencePage_0, 2, 300);
		}

		SWTFactory.createVerticalSpacer(comp, 1);
	//tree
		Composite comp1 = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_VERTICAL);
		SWTFactory.createLabel(comp1, DebugPreferencesMessages.LaunchDelegatesPreferencePage_2, 1);
		Tree tree = new Tree(comp1, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE);
		tree.setEnabled(enabled);
		tree.setFont(parent.getFont());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = false;
		tree.setLayoutData(gd);
		fTreeViewer = new TreeViewer(tree);
		fTreeViewer.setComparator(new WorkbenchViewerComparator());
		fTreeViewer.setContentProvider(new TreeProvider());
		fTreeViewer.setLabelProvider(new LabelProvider());
		fTreeViewer.setInput(fDuplicates);
		fTreeViewer.expandToLevel(2);
		fTreeViewer.addSelectionChangedListener(event -> {
			Object obj = event.getStructuredSelection().getFirstElement();
			if(obj instanceof DuplicateDelegate) {
				fTableViewer.setAllChecked(false);
				DuplicateDelegate dd = (DuplicateDelegate) obj;
				fTableViewer.setInput(dd.getDelegates());
				fTableViewer.setSelection(null);
				obj = fDupeSelections.get(dd);
				if(obj != null) {
					fTableViewer.setChecked(obj, true);
					fTableViewer.setSelection(new StructuredSelection(obj));
				}
			}
			else {
				fTableViewer.setInput(null);
			}
		});

	//table
		Composite comp2 = SWTFactory.createComposite(comp, comp.getFont(), 1, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(comp2, DebugPreferencesMessages.LaunchDelegatesPreferencePage_3, 1);
		Table table = new Table(comp2, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CHECK | SWT.SINGLE);
		table.setEnabled(enabled);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setFont(parent.getFont());
		fTableViewer = new CheckboxTableViewer(table);
		fTableViewer.setComparator(new WorkbenchViewerComparator());
		fTableViewer.setLabelProvider(new LabelProvider());
		fTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		fTableViewer.addSelectionChangedListener(event -> {
			IStructuredSelection ss = event.getStructuredSelection();
			if(ss != null && !ss.isEmpty()) {
				ILaunchDelegate delegate = (ILaunchDelegate)ss.getFirstElement();
				fDescription.setText(delegate.getDescription());
			}
			else {
				fDescription.setText(IInternalDebugCoreConstants.EMPTY_STRING);
			}
		});
		fTableViewer.addCheckStateListener(event -> {
			fDirty = true;
			Object element = event.getElement();
			boolean checked = event.getChecked();
			//always set checked, this way users cannot 'undo' a change to selecting a preferred delegate
			//The story for this is that on startup if there are dupes, the user is prompted to pick a delegate, after that they cannot
			//return to a state of not being able to launch something, but can pick a different delegate
			fTableViewer.setCheckedElements(new Object[] {element});
			//set the selection to be the checked element
			//https://bugs.eclipse.org/bugs/show_bug.cgi?id=233233
			fTableViewer.setSelection(new StructuredSelection(element));
			//persist the selection
			Object obj = fTreeViewer.getStructuredSelection().getFirstElement();
			if(obj instanceof DuplicateDelegate) {
				fDupeSelections.remove(obj);
				if(checked) {
					fDupeSelections.put((DuplicateDelegate) obj, (ILaunchDelegate) element);
				}
			}
		});
		Group group = SWTFactory.createGroup(comp, DebugPreferencesMessages.LaunchDelegatesPreferencePage_4, 1, 2, GridData.FILL_BOTH);
		fDescription = SWTFactory.createWrapLabel(group, "", 1); //$NON-NLS-1$
		return comp;
	}

	@Override
	public boolean performOk() {
		if(fDirty && fDupeSelections != null && fDupeSelections.size() > 0) {
			fDirty = false;
			DuplicateDelegate dd = null;
			ILaunchDelegate delegate = null;
			for (Iterator<DuplicateDelegate> iter = fDupeSelections.keySet().iterator(); iter.hasNext();) {
				dd = iter.next();
				delegate = fDupeSelections.get(dd);
				try {
					dd.getType().setPreferredDelegate(dd.getModeSet(), delegate);
				}
				catch (CoreException e) {DebugUIPlugin.log(e);}
			}
		}
		return super.performOk();
	}

	@Override
	public void init(IWorkbench workbench) {
		//init a listing of duplicate delegates arranged by type
		try {
			setPreferenceStore(DebugUIPlugin.getDefault().getPreferenceStore());
			LaunchManager lm = (LaunchManager) DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType[] types = lm.getLaunchConfigurationTypes();
			fDuplicates = new HashMap<>();
			fDupeSelections = new HashMap<>();
			ILaunchDelegate[] delegates = null;
			Set<Set<String>> modes = null;
			Set<String> modeset = null;
			Set<DuplicateDelegate> tmp = null;
			ILaunchDelegate prefdelegate = null;
			DuplicateDelegate dd = null;
			for (ILaunchConfigurationType type : types) {
				modes = type.getSupportedModeCombinations();
				for (Iterator<Set<String>> iter = modes.iterator(); iter.hasNext();) {
					modeset = iter.next();
					delegates = type.getDelegates(modeset);
					if (delegates.length > 1) {
						tmp = fDuplicates.get(type);
						if(tmp == null) {
							tmp = new HashSet<>();
						}
						dd = new DuplicateDelegate(type, delegates, modeset);
						tmp.add(dd);
						fDuplicates.put(type, tmp);
						prefdelegate = type.getPreferredDelegate(modeset);
						if(prefdelegate != null) {
							fDupeSelections.put(dd, prefdelegate);
						}
					}
				}
			}
		}
		catch(CoreException e) {DebugUIPlugin.log(e);}
	}

}
