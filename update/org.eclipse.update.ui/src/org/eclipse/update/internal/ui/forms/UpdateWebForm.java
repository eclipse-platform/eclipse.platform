package org.eclipse.update.internal.ui.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.pages.IUpdateFormPage;
import org.eclipse.update.ui.forms.internal.WebForm;

public class UpdateWebForm extends WebForm implements IUpdateForm, IUpdateModelChangedListener {
	private IUpdateFormPage page;
	private Control focusControl;

	public UpdateWebForm(IUpdateFormPage page) {
		this.page = page;
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.addUpdateModelChangedListener(this);
	}
	
	public void dispose() {
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		model.removeUpdateModelChangedListener(this);
		super.dispose();
	}

	public void objectsAdded(Object parent, Object [] children) {
	}
	public void objectsRemoved(Object parent, Object [] children) {
	}
	public void objectChanged(Object object, String property) {
	}

	public IUpdateFormPage getPage() {
		return page;
	}

	public void initialize(Object model) {
		super.initialize(model);
		if (isWhiteBackground()) {
			setHeadingImage(UpdateUIPluginImages.get(UpdateUIPluginImages.IMG_FORM_BANNER));
			setHeadingUnderlineImage(
				UpdateUIPluginImages.get(UpdateUIPluginImages.IMG_FORM_UNDERLINE));
		}
		refreshSize();
	}
	private boolean isWhiteBackground() {
		Color color = getFactory().getBackgroundColor();
		return (
			color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255);
	}
	protected void refreshSize() {
		((Composite) getControl()).layout();
		updateSize();
	}
	protected void setFocusControl(Control control) {
		focusControl = control;
	}
	public void setFocus() {
		if (focusControl!=null) focusControl.setFocus();
	}
}