package org.eclipse.update.internal.ui.forms;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.pages.UpdateFormPage;
import org.eclipse.update.ui.forms.internal.*;

public class RevertSection extends UpdateSection {
	// NL keys
	private static final String KEY_TITLE = "InstallConfigurationPage.RevertSection.title"; //$NON-NLS-1$
	private static final String KEY_DESC = "InstallConfigurationPage.RevertSection.desc"; //$NON-NLS-1$
	private static final String KEY_CURRENT_TEXT = "InstallConfigurationPage.RevertSection.currentText"; //$NON-NLS-1$
	private static final String KEY_REVERT_TEXT = "InstallConfigurationPage.RevertSection.revertText"; //$NON-NLS-1$
	private static final String KEY_REVERT_BUTTON = "InstallConfigurationPage.RevertSection.revertButton"; //$NON-NLS-1$
	private static final String KEY_RESTORE_TEXT = "InstallConfigurationPage.RevertSection.restoreText"; //$NON-NLS-1$
	private static final String KEY_RESTORE_BUTTON = "InstallConfigurationPage.RevertSection.restoreButton"; //$NON-NLS-1$
	private static final String KEY_DIALOG_TITLE = "InstallConfigurationPage.RevertSection.dialog.title"; //$NON-NLS-1$
	private static final String KEY_DIALOG_MESSAGE = "InstallConfigurationPage.RevertSection.dialog.message"; //$NON-NLS-1$

	private Composite container;
	private FormWidgetFactory factory;
	private IInstallConfiguration config;
	private Label currentTextLabel;
	private Label textLabel;
	private Button revertButton;

	public RevertSection(UpdateFormPage page) {
		super(page);
		setAddSeparator(false);
		setHeaderText(UpdateUIPlugin.getResourceString(KEY_TITLE));
		setDescription(UpdateUIPlugin.getResourceString(KEY_DESC));
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		HTMLTableLayout layout = new HTMLTableLayout();
		this.factory = factory;
		//header.setForeground(factory.getColor(factory.COLOR_COMPOSITE_SEPARATOR));
		updateHeaderColor();
		layout.leftMargin = layout.rightMargin = 0;
		layout.horizontalSpacing = 0;
		container = factory.createComposite(parent);
		container.setLayout(layout);
		layout.numColumns = 3;
		currentTextLabel = factory.createLabel(container, ""); //$NON-NLS-1$
		currentTextLabel.setFont(JFaceResources.getBannerFont());
		TableData td = new TableData();
		td.valign = TableData.MIDDLE;
		currentTextLabel.setLayoutData(td);
		textLabel = factory.createLabel(container, "", SWT.WRAP); //$NON-NLS-1$
		td = new TableData();
		td.valign = TableData.MIDDLE;
		textLabel.setLayoutData(td);
		revertButton = factory.createButton(container, "", SWT.PUSH); //$NON-NLS-1$
		revertButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				performRevert();
			}
		});
		td = new TableData();
		td.indent = 5;
		td.valign = TableData.MIDDLE;
		revertButton.setLayoutData(td);
		return container;
	}

	public void configurationChanged(IInstallConfiguration config) {
		this.config = config;
		boolean canRevert = false;
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration[] history =
				localSite.getConfigurationHistory();
			int length = history.length;
			canRevert = length > 1;
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
		}
		container.getParent().setVisible(canRevert);
		if (!canRevert)
			return;
		if (config.isCurrent()) {
			currentTextLabel.setText(
				UpdateUIPlugin.getResourceString(KEY_CURRENT_TEXT));
			textLabel.setText(
				UpdateUIPlugin.getResourceString(KEY_REVERT_TEXT));
			revertButton.setText(
				UpdateUIPlugin.getResourceString(KEY_REVERT_BUTTON));
		} else {
			currentTextLabel.setText(""); //$NON-NLS-1$
			textLabel.setText(
				UpdateUIPlugin.getResourceString(KEY_RESTORE_TEXT));
			revertButton.setText(
				UpdateUIPlugin.getResourceString(KEY_RESTORE_BUTTON));
		}
	}

	private void performRevert() {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration target = config;
			if (config.isCurrent()) {
				// take the previous one
				IInstallConfiguration[] history =
					localSite.getConfigurationHistory();
				target = history[history.length - 2];
			}
			performRevert(target);
		} catch (CoreException e) {
			UpdateUIPlugin.logException(e);
		}
	}

	public static void performRevert(final IInstallConfiguration target) {
		performRevert(target, true, true);
	}

	public static boolean performRevert(
		final IInstallConfiguration target,
		boolean confirm, 
		final boolean restart) {
		if (confirm) {
			// ask the user to confirm and bail if canceled
			String title = UpdateUIPlugin.getActivePage().getLabel();
			if (!MessageDialog.openConfirm(UpdateUIPlugin.getActiveWorkbenchShell(), title, UpdateUIPlugin.getResourceString("InstallConfigurationPage.RevertSection.confirm.message"))) //$NON-NLS-1$
				return false;
		}

		// make sure we can actually do the revert
		IStatus status = ActivityConstraints.validatePendingRevert(target);
		if (status != null) {
			ErrorDialog.openError(
				UpdateUIPlugin.getActiveWorkbenchShell(),
				null,
				null,
				status);
			return false;
		}

		// ok to perform the operation
		final boolean [] result = new boolean[1];
		result[0] = false;
		IRunnableWithProgress operation = new IRunnableWithProgress() {

			public void run(IProgressMonitor monitor) {
				boolean success = false;
				try {
					ILocalSite localSite = SiteManager.getLocalSite();
					localSite.revertTo(target, monitor, new UIProblemHandler());
					saveLocalSite();
					success = true;
				} catch (CoreException e) {
					UpdateUIPlugin.logException(e);
				} finally {
					monitor.done();
					result[0] = success;
					if (success && restart)
						UpdateUIPlugin.informRestartNeeded();
				}
			}
		};
		try {
			ProgressMonitorDialog dialog =
				new ProgressMonitorDialog(
					UpdateUIPlugin.getActiveWorkbenchShell().getShell());
			dialog.run(false, true, operation);
		} catch (InvocationTargetException e) {
			UpdateUIPlugin.logException(e);
		} catch (InterruptedException e) {
		}
		return result[0];
	}

	private static void saveLocalSite() throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		localSite.save();
	}
	public Control getFocusControl() {
		return revertButton;
	}
}