package org.eclipse.update.internal.ui.search;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.core.Import;
import org.eclipse.update.internal.ui.UpdateUIPlugin;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class NewPluginEntryDialog extends Dialog {
	private Text idText;
	private Text versionText;
	private Import iimport;
	
	public NewPluginEntryDialog(Shell shell) {
		super(shell);
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		Label label = new Label(container, SWT.NULL);
		label.setText(UpdateUIPlugin.getResourceString("NewPluginEntryDialog.id")); //$NON-NLS-1$
		
		idText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		idText.setLayoutData(gd);
		idText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		label = new Label(container, SWT.NULL);
		label.setText(UpdateUIPlugin.getResourceString("NewPluginEntryDialog.version")); //$NON-NLS-1$
		
		versionText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		versionText.setLayoutData(gd);
		WorkbenchHelp.setHelp(container, "org.eclipse.update.ui.NewPluginEntryDialog");
		return container;
	}
	
	public void create() {
		super.create();
		dialogChanged();
	}
	
	private void dialogChanged() {
		getButton(IDialogConstants.OK_ID).setEnabled(idText.getText().length()>0);
	}
	
	protected void okPressed() {
		iimport = new Import();
		iimport.setIdentifier(idText.getText());
		iimport.setVersion(versionText.getText());
		super.okPressed();
	}
	public Import getImport() {
		return iimport;
	}
}

