package org.eclipse.update.internal.ui.properties;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.swt.events.*;
import java.net.*;
import org.eclipse.update.internal.ui.UpdateUIPlugin;

/**
 * @see PropertyPage
 */
public class NamedObjectPropertyPage
	extends PropertyPage
	implements IWorkbenchPropertyPage {
	private static final String KEY_NAME = "NamedObjectPropertyPage.name";
	private static final String KEY_EXISTS = "NamedObjectPropertyPage.exists";
	private Text objectName;
	private boolean changed;

	/**
	 * The constructor.
	 */
	public NamedObjectPropertyPage() {
	}

	/**
	 * Insert the method's description here.
	 * @see PropertyPage#createContents
	 */
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);

		Label label = new Label(container, SWT.NULL);
		label.setText(UpdateUIPlugin.getResourceString(KEY_NAME));
		objectName = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		objectName.setLayoutData(gd);
		initializeFields();
		return container;
	}

	public boolean performOk() {
		if (changed) {
			NamedModelObject object = (NamedModelObject) getElement();
			object.setName(objectName.getText());
		}
		return true;
	}

	private void initializeFields() {
		NamedModelObject object = (NamedModelObject) getElement();
		objectName.setText(object.getName());

		objectName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkFields();
			}
		});
	}
	private void checkFields() {
		boolean valid = true;
		String errorMessage = null;
		String newName = objectName.getText();
		if (newName.length() == 0)
			valid = false;
		else {
			valid = !exists(newName);
			if (!valid)
				errorMessage =
					UpdateUIPlugin.getFormattedMessage(KEY_EXISTS, newName);
		}
		setValid(valid);
		setErrorMessage(errorMessage);
		changed = true;
	}
	private boolean exists(String name) {
		NamedModelObject object = (NamedModelObject) getElement();
		NamedModelObject parent = (NamedModelObject) object.getParent(null);
		Object[] candidates = null;
		if (parent == null) {
			// root level
			UpdateModel model = object.getModel();
			candidates = model.getBookmarks();
		} else {
			candidates = parent.getChildren(parent);
		}
		for (int i = 0; i < candidates.length; i++) {
			NamedModelObject candidate = (NamedModelObject) candidates[i];
			if (candidate.equals(object))
				continue;
			if (candidate.getName().equals(name))
				return true;
		}
		return false;
	}
}