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

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;


/**
 * An item in a breadcrumb viewer.
 * <p>
 * The item shows a label and an image. It also has the ability to expand, that is to open a drop
 * down menu.
 * </p>
 * <p>
 * The drop down allows to select any child of the items input element. The item shows the label and
 * icon of its data element, if any.
 * </p>
 *
 * @since 3.5
 */
class BreadcrumbItem extends Item {

	private TreePath fPath;

	private final BreadcrumbViewer fParent;
	private Composite fContainer;

	private BreadcrumbItemDropDown fExpandBlock;
	private BreadcrumbItemDetails fDetailsBlock;

	private boolean fIsLast;

	/**
	 * A new breadcrumb item which is shown inside the given viewer.
	 *
	 * @param viewer the items viewer
	 * @param parent the container containing the item
	 */
	public BreadcrumbItem(BreadcrumbViewer viewer, Composite parent) {
		super(parent, SWT.NONE);

		fParent= viewer;

		fContainer= new Composite(parent, SWT.NONE);
		fContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		GridLayout layout= new GridLayout(2, false);
		layout.marginBottom= 1;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.horizontalSpacing= 0;
		fContainer.setLayout(layout);

		fExpandBlock= new BreadcrumbItemDropDown(this, fContainer);
		fDetailsBlock= new BreadcrumbItemDetails(this, fContainer);
		fContainer.setData("org.eclipse.e4.ui.css.id", "DebugBreadcrumbItemComposite"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns this items viewer.
	 *
	 * @return the viewer showing this item
	 */
	public BreadcrumbViewer getViewer() {
		return fParent;
	}

	/*
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
		fContainer.dispose();
		super.dispose();
	}

	public TreePath getPath() {
		return fPath;
	}

	public void setPath(TreePath path) {
		fPath = path;
	}

	/**
	 * Should this item show a text label.
	 *
	 * @param enabled true if it should
	 */
	void setShowText(boolean enabled) {
		fDetailsBlock.setTextVisible(enabled);
	}

	/**
	 * Does this item show a text label?
	 *
	 * @return true if it does.
	 */
	boolean isShowText() {
		return fDetailsBlock.isTextVisible();
	}

	/**
	 * Returns the width of this item.
	 *
	 * @return the width of this item
	 */
	int getWidth() {
		return fDetailsBlock.getWidth() + fExpandBlock.getWidth() + 2;
	}

	/**
	 * Sets whether this item has to be marked as
	 * selected or not.
	 *
	 * @param selected true if marked as selected
	 */
	void setSelected(boolean selected) {
		fDetailsBlock.setSelected(selected);
	}

	/**
	 * Sets whether this item has the keyboard focus.
	 *
	 * @param state <code>true</code> if it has focus, <code>false</code> otherwise
	 */
	void setFocus(boolean state) {
		fDetailsBlock.setFocus(state);
	}

	/**
	 * Returns whether this item has the keyboard focus.
	 *
	 * @return <code>true</code> if this item has the keyboard focus
	 */
	boolean hasFocus() {
		return fDetailsBlock.hasFocus();
	}

	/**
	 * Set whether this is the last item in the breadcrumb item chain or not.
	 *
	 * @param isLast <code>true</code> if this is the last item, <code>false</code> otherwise
	 */
	void setIsLastItem(boolean isLast) {
		fIsLast= isLast;

		GridData data= (GridData) fContainer.getLayoutData();
		data.grabExcessHorizontalSpace= isLast;
	}

	/**
	 * Expand this item, shows the drop down menu.
	 */
	void openDropDownMenu() {
		fExpandBlock.showMenu();
	}

	/**
	 * @return true if this item is expanded
	 */
	boolean isMenuShown() {
		return fExpandBlock.isMenuShown();
	}

	/**
	 * Returns the drop down shell.
	 *
	 * @return the shell of the drop down if shown, <code>null</code> otherwise
	 */
	Shell getDropDownShell() {
		return fExpandBlock.getDropDownShell();
	}

	/**
	 * Returns the bounds of this item.
	 *
	 * @return the bounds of this item
	 */
	public Rectangle getBounds() {
		return fContainer.getBounds();
	}

	/**
	 * Set the tool tip of the item to the given text.
	 *
	 * @param text the tool tip for the item
	 */
	public void setToolTip(String text) {
		fDetailsBlock.setToolTip(text);
	}

	/*
	 * @see org.eclipse.swt.widgets.Item#setText(java.lang.String)
	 */
	@Override
	public void setText(String string) {
		super.setText(string);
		fDetailsBlock.setText(string);

		//more or less space might be required for the label
		if (fIsLast) {
			fContainer.layout(true, true);
		}
	}

	/*
	 * @see org.eclipse.swt.widgets.Item#setImage(org.eclipse.swt.graphics.Image)
	 */
	@Override
	public void setImage(Image image) {
		super.setImage(image);
		fDetailsBlock.setImage(image);
	}
}
