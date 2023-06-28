/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 *     Pawel Piech (Wind River) - adapted breadcrumb for use in Debug view (Bug 252677)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.breadcrumb;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;


/**
 * Breadcrumb base class.  It creates the breadcrumb viewer and manages
 * its activation.
 * <p>
 * Clients must implement the abstract methods.
 * </p>
 *
 * @since 3.5
 */
public abstract class AbstractBreadcrumb {

	private BreadcrumbViewer fBreadcrumbViewer;

	private boolean fHasFocus;

	private Composite fComposite;

	private Listener fDisplayFocusListener;
	private Listener fDisplayKeyListener;

	public AbstractBreadcrumb() {
	}

	/**
	 * The active element of the editor.
	 *
	 * @return the active element of the editor, or <b>null</b> if none
	 */
	protected abstract Object getCurrentInput();

	/**
	 * Create and configure the viewer used to display the parent chain.
	 *
	 * @param parent the parent composite
	 * @return the viewer
	 */
	protected abstract BreadcrumbViewer createViewer(Composite parent);

	/**
	 * Open the element in a new editor if possible.
	 *
	 * @param selection element the element to open
	 * @return true if the element could be opened
	 */
	protected abstract boolean open(ISelection selection);

	/**
	 * The breadcrumb has been activated. Implementors must retarget the editor actions to the
	 * breadcrumb aware actions.
	 */
	protected abstract void activateBreadcrumb();

	/**
	 * The breadcrumb has been deactivated. Implementors must retarget the breadcrumb actions to the
	 * editor actions.
	 */
	protected abstract void deactivateBreadcrumb();

	/**
	 * Returns the selection provider for this breadcrumb.
	 *
	 * @return the selection provider for this breadcrumb
	 */
	public ISelectionProvider getSelectionProvider() {
		return fBreadcrumbViewer;
	}

	/**
	 * Set the input of the breadcrumb to the given element
	 *
	 * @param element the input element can be <code>null</code>
	 */
	public void setInput(Object element) {
		if (element == null || fBreadcrumbViewer == null || fBreadcrumbViewer.getControl().isDisposed())
			return;

		Object input= fBreadcrumbViewer.getInput();
		if (input == element || element.equals(input)) {
			refresh();
			return;
		}

		fBreadcrumbViewer.setInput(element);
	}

	protected void refresh() {
		if (!fBreadcrumbViewer.getControl().isDisposed()) {
			fBreadcrumbViewer.refresh();
		}
	}

	/**
	 * Activates the breadcrumb. This sets the keyboard focus
	 * inside this breadcrumb and retargets the editor
	 * actions.
	 */
	public void activate() {
		if (fBreadcrumbViewer.getSelection().isEmpty())
			fBreadcrumbViewer.setSelection(new StructuredSelection(fBreadcrumbViewer.getInput()));
		fBreadcrumbViewer.setFocus();
	}

	/**
	 * A breadcrumb is active if it either has the focus or another workbench part has the focus and
	 * the breadcrumb had the focus before the other workbench part was made active.
	 *
	 * @return <code>true</code> if this breadcrumb is active
	 */
	public boolean isActive() {
		return true;
	}

	/**
	 * Create breadcrumb content.
	 *
	 * @param parent the parent of the content
	 * @return the control containing the created content
	 */
	public Control createContent(Composite parent) {
		Assert.isTrue(fComposite == null, "Content must only be created once."); //$NON-NLS-1$

		boolean rtl= (parent.getShell().getStyle() & SWT.RIGHT_TO_LEFT) != 0;
		//boolean rtl = true;

		fComposite= new Composite(parent, rtl ? SWT.RIGHT_TO_LEFT : SWT.NONE);
		GridData data= new GridData(SWT.FILL, SWT.TOP, true, false);
		fComposite.setLayoutData(data);
		GridLayout gridLayout= new GridLayout(1, false);
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight= 0;
		gridLayout.verticalSpacing= 0;
		gridLayout.horizontalSpacing= 0;
		fComposite.setLayout(gridLayout);

		fDisplayFocusListener= event -> {
			if (fComposite.isDisposed()) return;

			if (isBreadcrumbEvent(event)) {
				if (fHasFocus)
					return;

				focusGained();
			} else {
				if (!fHasFocus)
					return;

				focusLost();
			}
		};
		Display.getCurrent().addFilter(SWT.FocusIn, fDisplayFocusListener);

		fBreadcrumbViewer= createViewer(fComposite);

		fBreadcrumbViewer.addDoubleClickListener(event -> {
			Object element= ((IStructuredSelection) event.getSelection()).getFirstElement();
			if (element == null)
				return;

			BreadcrumbItem item= (BreadcrumbItem) fBreadcrumbViewer.doFindItem(element);
			if (item == null)
				return;
			item.openDropDownMenu();
		});

		fBreadcrumbViewer.addOpenListener(event -> doOpen(event.getSelection()));

		return fComposite;
	}

	/**
	 * Dispose all resources hold by this breadcrumb.
	 */
	public void dispose() {
		if (fDisplayFocusListener != null) {
			Display.getDefault().removeFilter(SWT.FocusIn, fDisplayFocusListener);
		}
		deinstallDisplayListeners();
	}

	/**
	 * Either reveal the selection in the editor or open the selection in a new editor. If both fail
	 * open the child pop up of the selected element.
	 *
	 * @param selection the selection to open
	 */
	private void doOpen(ISelection selection) {
		if (open(selection)) {
			fBreadcrumbViewer.setInput(getCurrentInput());
		}
	}

	/**
	 * Focus has been transfered into the breadcrumb.
	 */
	private void focusGained() {
		if (fHasFocus)
			focusLost();

		fHasFocus= true;

		installDisplayListeners();

		activateBreadcrumb();
	}

	/**
	 * Focus has been revoked from the breadcrumb.
	 */
	private void focusLost() {
		fHasFocus= false;

		deinstallDisplayListeners();

		deactivateBreadcrumb();
	}

	/**
	 * Installs all display listeners.
	 */
	private void installDisplayListeners() {
		//Sanity check
		deinstallDisplayListeners();

		fDisplayKeyListener= event -> {
			if (event.keyCode != SWT.ESC)
				return;

			if (!isBreadcrumbEvent(event))
				return;
		};
		Display.getDefault().addFilter(SWT.KeyDown, fDisplayKeyListener);
	}

	/**
	 * Removes all previously installed display listeners.
	 */
	private void deinstallDisplayListeners() {
		if (fDisplayKeyListener != null) {
			Display.getDefault().removeFilter(SWT.KeyDown, fDisplayKeyListener);
			fDisplayKeyListener= null;
		}
	}

	/**
	 * Tells whether the given event was issued inside the breadcrumb viewer's control.
	 *
	 * @param event the event to inspect
	 * @return <code>true</code> if event was generated by a breadcrumb child
	 */
	private boolean isBreadcrumbEvent(Event event) {
		if (fBreadcrumbViewer == null)
			return false;

		Widget item= event.widget;
		if (!(item instanceof Control))
			return false;

		Shell dropDownShell= fBreadcrumbViewer.getDropDownShell();
		if (dropDownShell != null && isChild((Control) item, dropDownShell))
			return true;

		return isChild((Control) item, fBreadcrumbViewer.getControl());
	}

	private boolean isChild(Control child, Control parent) {
		if (child == null)
			return false;

		if (child == parent)
			return true;

		return isChild(child.getParent(), parent);
	}
}
