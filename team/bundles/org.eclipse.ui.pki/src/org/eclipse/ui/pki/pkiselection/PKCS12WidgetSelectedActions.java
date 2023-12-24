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

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.pki.util.KeyStoreFormat;

public class PKCS12WidgetSelectedActions {
	
	public static void setKeyStoreFormat(){
		PKCSSelected.setPkcs12Selected(true);
		PKCSSelected.setPkcs11Selected(false);
		PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS12);
	}

	public static void userInterfaceDisplay(Text certpathtext, Label pkiLabel,
		Label passwordlabel, Text passwordtext, Button browseButton, boolean visiblity) {
		certpathtext.setVisible(visiblity);
		pkiLabel.setVisible(visiblity);
		browseButton.setVisible(visiblity);
		passwordlabel.setVisible(visiblity);
		passwordtext.setVisible(visiblity);		
	}
}
