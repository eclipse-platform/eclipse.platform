package org.eclipse.update.internal.ui.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.pages.UpdateFormPage;
import org.eclipse.update.internal.ui.parts.SWTUtil;
import org.eclipse.update.ui.forms.internal.*;

public class InstallConfigurationForm extends PropertyWebForm {
	private static final String KEY_CREATED_ON =
		"InstallConfigurationPage.createdOn";
	private static final String KEY_CURRENT_CONFIG =
		"InstallConfigurationPage.currentConfig";
	private static final String KEY_YES = "InstallConfigurationPage.yes";
	private static final String KEY_NO = "InstallConfigurationPage.no";

	private IInstallConfiguration currentConfiguration;
	private Label dateLabel;
	private Label currentLabel;
	private ActivitySection activitySection;
	private RevertSection revertSection;
	private IUpdateModelChangedListener modelListener;

	public InstallConfigurationForm(UpdateFormPage page) {
		super(page);
	}

	public void dispose() {
		super.dispose();
	}

	public void initialize(Object modelObject) {
		setHeadingText("");
		super.initialize(modelObject);
		UpdateModel model = UpdateUIPlugin.getDefault().getUpdateModel();
		modelListener = new IUpdateModelChangedListener() {
			public void objectsAdded(Object parent, Object[] children) {
			}
			public void objectsRemoved(Object parent, Object[] children) {
			}
			public void objectChanged(Object obj, String property) {
				if (obj instanceof PreservedConfiguration)
					obj = ((PreservedConfiguration) obj).getConfiguration();
				if (obj.equals(currentConfiguration)) {
					SWTUtil.getStandardDisplay().asyncExec(new Runnable() {
						public void run() {
					inputChanged(currentConfiguration);
						}
					});
				}
			}
		};
		model.addUpdateModelChangedListener(modelListener);
	}

	protected void createContents(Composite parent) {
		HTMLTableLayout layout = new HTMLTableLayout();
		parent.setLayout(layout);
		layout.leftMargin = layout.rightMargin = 10;
		layout.topMargin = 10;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.numColumns = 1;

		FormWidgetFactory factory = getFactory();

		dateLabel =
			createProperty(parent, UpdateUIPlugin.getResourceString(KEY_CREATED_ON));
		currentLabel =
			createProperty(parent, UpdateUIPlugin.getResourceString(KEY_CURRENT_CONFIG));
		factory.createLabel(parent, null);

		activitySection = new ActivitySection((UpdateFormPage) getPage());
		Control control = activitySection.createControl(parent, factory);
		TableData td = new TableData();
		//td.align = TableData.FILL;
		//td.grabHorizontal = true;
		td.valign = TableData.TOP;
		//td.colspan = 2;
		control.setLayoutData(td);

		revertSection = new RevertSection((UpdateFormPage) getPage());
		control = revertSection.createControl(parent, factory);
		td = new TableData();
		td.align = TableData.FILL;
		td.grabHorizontal = true;
		td.valign = TableData.TOP;
		control.setLayoutData(td);
		setFocusControl(revertSection.getFocusControl());

		registerSection(activitySection);
		registerSection(revertSection);
		WorkbenchHelp.setHelp(parent, "org.eclipse.update.ui.InstallConfigurationForm");
	}

	protected Object createPropertyLayoutData() {
		TableData td = new TableData();
		//td.indent = 10;
		return td;
	}

	public void expandTo(Object obj) {
		if (obj instanceof PreservedConfiguration) {
			obj = ((PreservedConfiguration) obj).getConfiguration();
		}
		if (obj instanceof IInstallConfiguration) {
			inputChanged((IInstallConfiguration) obj);
		}
	}

	private void inputChanged(IInstallConfiguration configuration) {
		setHeadingText(configuration.getLabel());
		Date date = configuration.getCreationDate();
		dateLabel.setText(Utilities.format(date));
		String isCurrent =
			configuration.isCurrent()
				? UpdateUIPlugin.getResourceString(KEY_YES)
				: UpdateUIPlugin.getResourceString(KEY_NO);
		currentLabel.setText(isCurrent);

		activitySection.configurationChanged(configuration);
		revertSection.configurationChanged(configuration);
		// reflow
		dateLabel.getParent().layout(true);
		((Composite) getControl()).layout(true);
		getControl().redraw();
		updateSize();
		currentConfiguration = configuration;
	}
	
	public void objectChanged(Object object, String property) {
		if (object.equals(currentConfiguration)) {
			expandTo(object);
		}
	}

}