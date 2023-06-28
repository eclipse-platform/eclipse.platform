/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
 *     Wind River - Anton Leherbauer - Fix selection provider (Bug 254442)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.variables.details;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPane2;
import org.eclipse.debug.ui.IDetailPane3;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Acts as a proxy between a view and a detail pane. Controls how information is displayed
 * in the details pane in a view.  Currently used by the variables, registers and expression
 * views as well as the inspect popup dialog.  The different types of detail panes use the
 * detailPaneFactories extension and must implement <code>IDetailPane</code>.  This class acts
 * as a proxy to the current detail pane, each time the detail pane type changes, this class
 * disposes of the old pane and creates the new one.  Uses a <code>DetailPaneManager</code> to
 * organize and find the panes to display.
 *
 * @see IDetailPane
 * @see DetailPaneManager
 * @since 3.3
 */
public class DetailPaneProxy implements ISaveablePart {

	/**
	 * The IDetailPane currently being used to display detailed information.
	 */
	private IDetailPane fCurrentPane;

	/**
	 * The UI control that the current detail pane is using to display details.
	 */
	private Control fCurrentControl;

	/**
	 * Detail pane container that the detail panes will be added to.
	 */
	private IDetailPaneContainer fParentContainer;

	/**
	 * Property listeners
	 */
	private ListenerList<IPropertyListener> fListeners = new ListenerList<>();

	/**
	 * Constructor that sets up the detail pane for a view.  Note that no default pane
	 * is created, so a control will not be created until <code>display</code> is called.
	 *
	 * @param parent the detail pane container that is holding this detail pane
	 */
	public DetailPaneProxy(IDetailPaneContainer parent) {
		fParentContainer = parent;
	}

	/**
	 * Displays the given selection in the preferred detail pane for that type of selection.
	 * Informs the parent container if the type of detail pane changes.
	 * If a null or empty selection is passed and a current pane exists, that view will be cleared.
	 * If a null or empty selection is passed and no pane exists, the default view is created.
	 *
	 * @param selection The selection to display detailed information for
	 */
	public void display(IStructuredSelection selection){

		IDetailPane3 saveable = getSaveable();
		boolean clean = false;
		if (saveable != null && saveable.isDirty() && saveable.isSaveOnCloseNeeded()) {
			// save the contents before changing
			saveable.doSave(null);
		}

		if ((selection == null || selection.isEmpty()) && fCurrentPane != null){
			fCurrentPane.display(selection);
			if (clean) {
				fireDirty();
			}
			return;
		}

		String preferredPaneID = DetailPaneManager.getDefault().getPreferredPaneFromSelection(selection);
		if (preferredPaneID == null) {
			preferredPaneID = MessageDetailPane.ID;
			selection = new StructuredSelection(DetailMessages.DetailPaneProxy_1);
		}

		// Don't change anything if the preferred pane is the current pane
		if (fCurrentPane != null && preferredPaneID.equals(fCurrentPane.getID())){
			fCurrentPane.display(selection);
			if (clean) {
				fireDirty();
			}
			return;
		}

		setupPane(preferredPaneID, selection);

		// Inform the container that a new detail pane is being used
		fParentContainer.paneChanged(preferredPaneID);
		if (clean) {
			fireDirty();
		}

	}

	/**
	 * Fires dirty property change.
	 */
	private void fireDirty() {
		for (IPropertyListener iPropertyListener : fListeners) {
			iPropertyListener.propertyChanged(this, PROP_DIRTY);
		}
	}

	/**
	 * Tells the current detail pane (if one exists) that it is gaining focus.
	 *
	 * @return true if the current pane successfully set focus to a control, false otherwise
	 */
	public boolean setFocus(){
		if (fCurrentPane != null){
			return fCurrentPane.setFocus();
		}
		return false;
	}

	/**
	 * Disposes of the current pane.
	 */
	public void dispose(){
		if (fCurrentPane != null)	{
			fCurrentPane.dispose();
			fCurrentPane = null;
		}
		if (fCurrentControl != null && !fCurrentControl.isDisposed()) {
			fCurrentControl.dispose();
			fCurrentControl = null;
		}
	}

	/**
	 * Checks if the current pane supports the <code>IAdaptable</code> framework
	 * and if so, calls its <code>getAdapter()</code> method.
	 * @param required the class to get the adapter for
	 * @return the {@link IAdaptable} for the given class or <code>null</code>
	 */
	public Object getAdapter(Class<?> required) {
		if (fCurrentPane != null && fCurrentPane instanceof IAdaptable){
			return ((IAdaptable)fCurrentPane).getAdapter(required);
		}
		else{
			return null;
		}
	}

	public Control getCurrentControl(){
		return fCurrentControl;
	}

	public String getCurrentPaneID(){
		if (fCurrentPane != null){
			return fCurrentPane.getID();
		}
		return null;
	}

	/**
	 * Finds or creates an initialized detail pane with the given ID.  Asks the detail
	 * pane to create the control and display the selection.
	 *
	 * @param paneID the ID of the pane to display in
	 * @param selection the selection to display
	 */
	private void setupPane(String paneID, IStructuredSelection selection) {
		try {
			fParentContainer.getParentComposite().setRedraw(false);
			if (fCurrentPane != null) {
				fCurrentPane.dispose();
			}
			if (fCurrentControl != null && !fCurrentControl.isDisposed()) {
				fCurrentControl.dispose();
			}
			fCurrentPane = null;
			fCurrentPane = DetailPaneManager.getDefault().getDetailPaneFromID(paneID);
			if (fCurrentPane != null){
				final IWorkbenchPartSite workbenchPartSite = fParentContainer.getWorkbenchPartSite();
				fCurrentPane.init(workbenchPartSite);
				IDetailPane3 saveable = getSaveable();
				if (saveable != null) {
					for (IPropertyListener iPropertyListener : fListeners) {
						saveable.addPropertyListener(iPropertyListener);
					}
				}
				fCurrentControl = fCurrentPane.createControl(fParentContainer.getParentComposite());
				if (fCurrentControl != null){
					fParentContainer.getParentComposite().layout(true);
					fCurrentPane.display(selection);
					if (fParentContainer instanceof IDetailPaneContainer2) {
						fCurrentControl.addFocusListener(new FocusAdapter() {
							@Override
							public void focusGained(FocusEvent e) {
								updateSelectionProvider(true);
							}
							@Override
							public void focusLost(FocusEvent e) {
								updateSelectionProvider(false);
							}
						});
					}
				} else{
					createErrorLabel(DetailMessages.DetailPaneProxy_0);
					DebugUIPlugin.log(new CoreException(new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), MessageFormat.format(DetailMessages.DetailPaneProxy_2, new Object[] { fCurrentPane.getID() }))));
				}
			} else {
				createErrorLabel(DetailMessages.DetailPaneProxy_0);
				DebugUIPlugin.log(new CoreException(new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), MessageFormat.format(DetailMessages.DetailPaneProxy_3, new Object[] { paneID }))));
			}
		} finally {
			fParentContainer.getParentComposite().setRedraw(true);
		}
	}

	/**
	 * Update the selection provider of the current detail pane in the container.
	 *
	 * @param hasFocus  whether the detail pane control has the focus
	 */
	protected void updateSelectionProvider(boolean hasFocus) {
		if (fParentContainer instanceof IDetailPaneContainer2) {
			final IDetailPaneContainer2 container2 = (IDetailPaneContainer2) fParentContainer;
			if (fCurrentPane instanceof IDetailPane2) {
				final ISelectionProvider provider= hasFocus ? ((IDetailPane2) fCurrentPane).getSelectionProvider() : null;
				container2.setSelectionProvider(provider);
			} else {
				// Workaround for legacy detail pane implementations (bug 254442)
				// Forward the site's selection provider to container
				IWorkbenchPartSite site = container2.getWorkbenchPartSite();
				if (site != null) {
					container2.setSelectionProvider(site.getSelectionProvider());
				}
			}
		}
	}

	/**
	 * Creates a label in the detail pane area with the given message.
	 *
	 * @param message The message to display
	 */
	private void createErrorLabel(String message){
		if (fCurrentPane != null) {
			fCurrentPane.dispose();
		}
		if (fCurrentControl != null && !fCurrentControl.isDisposed()) {
			fCurrentControl.dispose();
		}
		fCurrentControl = SWTFactory.createComposite(fParentContainer.getParentComposite(), 1, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createLabel((Composite) fCurrentControl, message, 1);
		fParentContainer.getParentComposite().layout();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		ISaveablePart saveable = getSaveable();
		if (saveable != null) {
			saveable.doSave(monitor);
		}
	}

	@Override
	public void doSaveAs() {
		ISaveablePart saveable = getSaveable();
		if (saveable != null) {
			saveable.doSaveAs();
		}
	}

	@Override
	public boolean isDirty() {
		ISaveablePart saveable = getSaveable();
		if (saveable != null) {
			return saveable.isDirty();
		}
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		ISaveablePart saveable = getSaveable();
		if (saveable != null) {
			return saveable.isSaveAsAllowed();
		}
		return false;
	}

	@Override
	public boolean isSaveOnCloseNeeded() {
		ISaveablePart saveable = getSaveable();
		if (saveable != null) {
			return saveable.isSaveOnCloseNeeded();
		}
		return false;
	}

	/**
	 * Returns the active saveable part or <code>null</code> if none.
	 *
	 * @return saveable part or <code>null</code>
	 */
	IDetailPane3 getSaveable() {
		if (fCurrentPane instanceof IDetailPane3) {
			return (IDetailPane3) fCurrentPane;
		}
		return null;
	}

	public void addProperyListener(IPropertyListener listener) {
		fListeners.add(listener);
		IDetailPane3 saveable = getSaveable();
		if (saveable != null) {
			saveable.addPropertyListener(listener);
		}
	}

	public void removePropertyListener(IPropertyListener listener) {
		fListeners.remove(listener);
		IDetailPane3 saveable = getSaveable();
		if (saveable != null) {
			saveable.removePropertyListener(listener);
		}
	}
}
