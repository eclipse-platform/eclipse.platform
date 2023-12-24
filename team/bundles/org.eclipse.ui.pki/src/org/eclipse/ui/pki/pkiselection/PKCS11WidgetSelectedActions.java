/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.pki.pkiselection;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.pki.util.KeyStoreFormat;

public class PKCS11WidgetSelectedActions {
	
	public static void setKeyStoreFormat(){
		PKCSSelected.setPkcs11Selected(true);
		PKCSSelected.setPkcs12Selected(false);
		PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS11);
	}
	
	/**
	 * Causes the PKI Certificate to be visible or not.
	 * @param password The Text box to enter the path.
	 * @param passwordlabel The PKI Certificate label.
	 * @param visibility true to make all 3 fields visible, false to make it not visible.
	 */
	public static void userInterfaceDisplay(Text password, Label passwordlabel, boolean visibility){
		if(password != null) password.setVisible(visibility);
		passwordlabel.setVisible(visibility);
	}

}
