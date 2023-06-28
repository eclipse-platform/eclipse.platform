/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointOrganizer;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointOrganizerManager;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog which presents available breakpoint groupings to
 * the user and allows them to specify which they'd like
 * to use and in what order they should be applied.
 */
public class GroupBreakpointsByDialog extends TrayDialog {

	private BreakpointsView fView;

	// Table viewer that presents available containers
	private TableViewer fAvailableViewer;
	private AvailableOrganizersProvider fAvailableOrganizersProvider= new AvailableOrganizersProvider();

	// Tree viewer that presents selected containers
	private TreeViewer fSelectedViewer;
	private SelectedOrganizerProvider fSelectedOrganizersProvider= new SelectedOrganizerProvider();

	private List<Object> fResult = new ArrayList<>();

	private Button fAddButton;
	private Button fRemoveButton;
	private Button fMoveUpButton;
	private Button fMoveDownButton;

	/**
	 * Selection listener that listens to selection from all buttons in this
	 * dialog.
	 */
	private SelectionAdapter fSelectionListener= new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Object source= e.getSource();
			if (source == fAddButton) {
				handleAddPressed();
			} else if (source == fRemoveButton) {
				handleRemovePressed();
			} else if (source == fMoveUpButton) {
				handleMoveUpPressed();
			} else if (source == fMoveDownButton) {
				handleMoveDownPressed();
			}
		}
	};

	protected GroupBreakpointsByDialog(BreakpointsView view) {
		super(view.getSite().getShell());
		fView= view;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		ILabelProvider labelProvider= new BreakpointOrganzierLabelProvider();

		Composite parentComposite= (Composite) super.createDialogArea(parent);
		parentComposite.setFont(parent.getFont());
		Composite composite= new Composite(parentComposite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		GridData data= new GridData(GridData.FILL_BOTH);
		data.heightHint= 400;
		composite.setLayoutData(data);
		composite.setFont(parent.getFont());

		Label label= new Label(composite, SWT.WRAP);
		label.setText(BreakpointGroupMessages.GroupBreakpointsByDialog_0);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		label.setLayoutData(gridData);

		createAvailableViewer(composite, labelProvider);
		createButtons(composite);
		createSelectedViewer(composite, labelProvider);

		initializeContent();
		updateViewers();
		Dialog.applyDialogFont(parentComposite);
		return parentComposite;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getDialogArea(), IDebugHelpContextIds.GROUP_BREAKPOINTS_DIALOG);
		return contents;
	}

	/**
	 * Divides the available breakpoint container factories into the
	 * appropriate viewers ("available" or "selected").
	 */
	private void initializeContent() {
		IBreakpointOrganizer[] organizers= BreakpointOrganizerManager.getDefault().getOrganizers();
		for (IBreakpointOrganizer organizer : organizers) {
			fAvailableOrganizersProvider.addAvailable(organizer);
		}
		organizers = fView.getBreakpointOrganizers();
		if (organizers != null) {
			for (IBreakpointOrganizer organizer : organizers) {
				fSelectedOrganizersProvider.addSelected(organizer);
			}
		}
	}

	/**
	 * Creates and configured the viewer that shows the available (not currently selected)
	 * breakpoint container factories.
	 */
	private void createAvailableViewer(Composite parent, ILabelProvider labelProvider) {
		Composite availableComposite= new Composite(parent, SWT.NONE);
		availableComposite.setFont(parent.getFont());
		GridLayout layout = new GridLayout();
		layout.marginHeight=0;
		layout.marginWidth=0;
		availableComposite.setLayout(layout);
		GridData gridData= new GridData(GridData.FILL_BOTH);
		gridData.widthHint= 225;
		availableComposite.setLayoutData(gridData);

		Label label= new Label(availableComposite, SWT.WRAP);
		label.setText(BreakpointGroupMessages.GroupBreakpointsByDialog_1);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gridData);

		fAvailableViewer= new TableViewer(availableComposite);
		fAvailableViewer.setContentProvider(fAvailableOrganizersProvider);
		fAvailableViewer.setLabelProvider(labelProvider);
		fAvailableViewer.setInput(new Object());
		Table table = fAvailableViewer.getTable();
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setFont(parent.getFont());
		fAvailableViewer.addDoubleClickListener(event -> handleAddPressed());
		fAvailableViewer.addSelectionChangedListener(event -> updateAddButton());
	}

	/**
	 * Creates and configures the viewer that shows the currently selected
	 * breakpoint container factories.
	 */
	private void createSelectedViewer(Composite parent, ILabelProvider labelProvider) {
		Composite selectedComposite= new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight=0;
		layout.marginWidth=0;
		layout.numColumns= 2;
		selectedComposite.setLayout(layout);
		GridData gridData= new GridData(GridData.FILL_BOTH);
		gridData.widthHint= 225;
		selectedComposite.setLayoutData(gridData);
		selectedComposite.setFont(parent.getFont());

		Label label= new Label(selectedComposite, SWT.WRAP);
		label.setText(BreakpointGroupMessages.GroupBreakpointsByDialog_3);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData(gridData);

		fSelectedViewer= new TreeViewer(selectedComposite);
		fSelectedViewer.setContentProvider(fSelectedOrganizersProvider);
		fSelectedViewer.setLabelProvider(labelProvider);
		fSelectedViewer.setInput(new Object());
		Tree tree = fSelectedViewer.getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setFont(parent.getFont());
		fSelectedViewer.addDoubleClickListener(event -> handleRemovePressed());
		fSelectedViewer.addSelectionChangedListener(event -> updateSelectedButtons());
	}

	public void createButtons(Composite parent) {
		Composite buttonComposite= new Composite(parent, SWT.NONE);
		buttonComposite.setLayout(new GridLayout());
		buttonComposite.setLayoutData(new GridData());
		buttonComposite.setFont(parent.getFont());

		fAddButton= SWTFactory.createPushButton(buttonComposite, BreakpointGroupMessages.GroupBreakpointsByDialog_2, null);
		fAddButton.addSelectionListener(fSelectionListener);

		fRemoveButton= SWTFactory.createPushButton(buttonComposite, BreakpointGroupMessages.GroupBreakpointsByDialog_4, null);
		fRemoveButton.addSelectionListener(fSelectionListener);

		fMoveUpButton= SWTFactory.createPushButton(buttonComposite, BreakpointGroupMessages.GroupBreakpointsByDialog_5, null);
		fMoveUpButton.addSelectionListener(fSelectionListener);

		fMoveDownButton= SWTFactory.createPushButton(buttonComposite, BreakpointGroupMessages.GroupBreakpointsByDialog_6, null);
		fMoveDownButton.addSelectionListener(fSelectionListener);

	}

	/**
	 * Returns the organizers chosen by the user. The order
	 * of the list is the order that the organizers should be displayed
	 * in the breakpoints view.
	 * @return the breakpoint organizers chosen by the user
	 */
	public IBreakpointOrganizer[] getOrganizers() {
		return fResult.toArray(new IBreakpointOrganizer[fResult.size()]);
	}

	/**
	 * When the user presses OK, convert the tree selection into a list.
	 */
	@Override
	protected void okPressed() {
		Object[] factories= fSelectedOrganizersProvider.getElements(null);
		while (factories.length > 0) {
			Object factory= factories[0];
			fResult.add(factory);
			factories= fSelectedOrganizersProvider.getChildren(factory);
		}
		super.okPressed();
	}

	/**
	 * Moves the selected item from the list of "available" factories
	 * to the tree of "selected" factories.
	 */
	public void handleAddPressed() {
		IStructuredSelection selection = fAvailableViewer.getStructuredSelection();
		if (selection.size() < 1) {
			return;
		}
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			fSelectedOrganizersProvider.addSelected((IBreakpointOrganizer) iter.next());
		}
		updateViewers();
	}

	/**
	 * Moves the selected item from the tree of "selected" factories
	 * to the list of "available" factories.
	 */
	public void handleRemovePressed() {
		IStructuredSelection selection = fSelectedViewer.getStructuredSelection();
		if (selection.size() < 1) {
			return;
		}
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			fAvailableOrganizersProvider.addAvailable((IBreakpointOrganizer) iter.next());
		}
		updateViewers();
	}

	/**
	 * Moves each selected item up in the tree of selected containers
	 */
	public void handleMoveUpPressed() {
		IStructuredSelection selection = fSelectedViewer.getStructuredSelection();
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			fSelectedOrganizersProvider.moveUp(iter.next());
		}
		updateViewers();
	}

	/**
	 * Moves each selected item down in the tree of selected containers
	 */
	public void handleMoveDownPressed() {
		IStructuredSelection selection = fSelectedViewer.getStructuredSelection();
		Object[] elements= selection.toArray();
		for (int i= elements.length - 1; i >= 0; i--) {
			fSelectedOrganizersProvider.moveDown(elements[i]);
		}
		updateViewers();
	}

	/**
	 * Fully refreshes and updates all viewers and buttons.
	 */
	public void updateViewers() {
		fAvailableViewer.refresh();
		fSelectedViewer.refresh();
		fSelectedViewer.expandAll();
		updateAddButton();
		updateSelectedButtons();
	}

	/**
	 * Updates all buttons associated with the tree of selected containers.
	 */
	public void updateSelectedButtons() {
		updateRemoveButton();
		updateMoveUpButton();
		updateMoveDownButton();
	}

	public void updateAddButton() {
		IStructuredSelection selection = fAvailableViewer.getStructuredSelection();
		fAddButton.setEnabled(selection.size() > 0);
	}

	public void updateRemoveButton() {
		IStructuredSelection selection = fSelectedViewer.getStructuredSelection();
		fRemoveButton.setEnabled(selection.size() > 0);
	}

	public void updateMoveUpButton() {
		boolean enabled= true;
		IStructuredSelection selection = fSelectedViewer.getStructuredSelection();
		if (selection.size() == 0) {
			enabled= false;
		} else {
			Object firstSelected= selection.getFirstElement();
			Object parent= fSelectedOrganizersProvider.getParent(firstSelected);
			if (!(parent instanceof IBreakpointOrganizer)) {
				enabled= false;
			}
		}
		fMoveUpButton.setEnabled(enabled);
	}

	public void updateMoveDownButton() {
		boolean enabled= true;
		IStructuredSelection selection = fSelectedViewer.getStructuredSelection();
		if (selection.size() == 0) {
			enabled= false;
		} else {
			Object lastSelected= selection.toList().get(selection.size() - 1);
			Object[] children= fSelectedOrganizersProvider.getChildren(lastSelected);
			if (children.length < 1) {
				enabled= false;
			}
		}
		fMoveDownButton.setEnabled(enabled);
	}

	/**
	 * Content provider that provides the list of breakpoint organaisers
	 * that are available but not currently selected.
	 */
	private class AvailableOrganizersProvider implements IStructuredContentProvider {
		protected List<IBreakpointOrganizer> availableOrganziers = new ArrayList<>();

		public void addAvailable(IBreakpointOrganizer organizer) {
			availableOrganziers.add(organizer);
			fSelectedOrganizersProvider.selectedOrganizers.remove(organizer);
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return availableOrganziers.toArray();
		}

		@Override
		public void dispose() {
		}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	/**
	 * Content provider that returns the selected breakpoint organizers
	 * as a tree. This tree shows the list of organzizers as they will
	 * appear in the breakpoints view.
	 */
	private class SelectedOrganizerProvider implements ITreeContentProvider {
		protected List<Object> selectedOrganizers = new ArrayList<>();

		public void addSelected(IBreakpointOrganizer organizer) {
			selectedOrganizers.add(organizer);
			fAvailableOrganizersProvider.availableOrganziers.remove(organizer);
		}

		public void moveUp(Object object) {
			int index = selectedOrganizers.indexOf(object);
			if (index > 0) {
				selectedOrganizers.remove(object);
				selectedOrganizers.add(index - 1, object);
			}
		}

		public void moveDown(Object object) {
			int index = selectedOrganizers.indexOf(object);
			if (index < selectedOrganizers.size() - 1) {
				selectedOrganizers.remove(object);
				selectedOrganizers.add(index + 1, object);
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			// A factory's "child" is the next factory in the list
			int index = selectedOrganizers.indexOf(parentElement);
			if (index < selectedOrganizers.size() - 1) {
				return new Object[] { selectedOrganizers.get(index + 1) };
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			// A factory's "parent" is the factory before it
			int index = selectedOrganizers.indexOf(element);
			if (index <= 0 || selectedOrganizers.size() <= 1) {
				return null;
			}
			return selectedOrganizers.get(index - 1);
		}

		@Override
		public boolean hasChildren(Object element) {
			// A factory has "children" if there are more
			// factories after it.
			int index = selectedOrganizers.indexOf(element);
			return index != -1 && index < selectedOrganizers.size() - 1;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (selectedOrganizers.size() > 0) {
				return new Object[] { selectedOrganizers.get(0) };
			}
			return new Object[0];
		}
	}

	/**
	 * Label provider which provides text and images for breakpoint container factories
	 */
	private class BreakpointOrganzierLabelProvider extends LabelProvider {
		private HashMap<ImageDescriptor, Image> fImageCache = new HashMap<>();

		@Override
		public String getText(Object element) {
			if (element instanceof IBreakpointOrganizer) {
				return ((IBreakpointOrganizer) element).getLabel();
			}
			return super.getText(element);
		}
		@Override
		public Image getImage(Object element) {
			if (element instanceof IBreakpointOrganizer) {
				ImageDescriptor imageDescriptor = ((IBreakpointOrganizer) element).getImageDescriptor();
				if (imageDescriptor != null) {
					Image image = fImageCache.get(imageDescriptor);
					if (image == null) {
						image= imageDescriptor.createImage();
						if (image != null) {
							fImageCache.put(imageDescriptor, image);
						}
					}
					return image;
				}
			}
			return super.getImage(element);
		}
		@Override
		public void dispose() {
			for (Image image : fImageCache.values()) {
				image.dispose();
			}
			super.dispose();
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(BreakpointGroupMessages.GroupBreakpointsByDialog_7);
	}
}
