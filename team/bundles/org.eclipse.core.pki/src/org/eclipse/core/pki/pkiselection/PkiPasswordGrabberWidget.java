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
package org.eclipse.core.pki.pkiselection;

import java.util.Optional;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

import org.eclipse.core.pki.auth.PublishPasswordUpdate;
import org.eclipse.core.pki.util.KeyStoreFormat;
import org.eclipse.core.pki.util.KeyStoreManager;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public enum PkiPasswordGrabberWidget {
	INSTANCE;
	JFrame frame = null;
	Icon icon = null;

	JPasswordField pword = null;

	public String getInput() {

		//PublishPasswordUpdateImpl publisher = PublishPasswordUpdateImpl.getInstance();

		Optional keystoreContainer = null;
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Enter Password:");
		JLabel blankie = new JLabel("\n", SwingConstants.CENTER);
		pword = new JPasswordField(17);
		String pw=null;
		panel.add(label);
		panel.add(blankie);
		panel.add(pword);
		try {
			
			icon = new ImageIcon(getClass().getResource("/icons/icons8-password-48.png"));
		} catch (Exception iconErr) {
			//iconErr.printStackTrace();
		}

		panel.requestFocus();
		char[] password = null;
		while (true) {
			String[] options = new String[] {"cancel", "submit"};

			//showOptionDialog(Component parentComponent,
			//		Object message, String title, int optionType,
			//		int messageType, Icon icon, Object[] options,
			//		Object initialValue)

			int option = JOptionPane.showOptionDialog(null, panel, "Eclipse PKI Password/PiN Entry",
	                JOptionPane.INFORMATION_MESSAGE, JOptionPane.PLAIN_MESSAGE,
					icon, options, options[1]);

			//System.out.println("Your ENTRY OPTION is: " + option);

			if (option == 0) {
				//System.out.println("PkiPasswordGrabberWidget CANCEL value NO_OPTION ");
				JOptionPane.showMessageDialog(null,"CANCELED",null,
                        JOptionPane.ERROR_MESSAGE);

				break;
			} else if(option == 1) {
				password = pword.getPassword();
				pw=new String(password);
				//System.out.println("Your password is: " + new String(password));

				System.setProperty("javax.net.ssl.keyStorePassword", pw); //$NON-NLS-1$

				keystoreContainer = Optional
						.ofNullable(KeyStoreManager.INSTANCE.getKeyStore(System.getProperty("javax.net.ssl.keyStore"), //$NON-NLS-1$
								System.getProperty("javax.net.ssl.keyStorePassword"), //$NON-NLS-1$
								KeyStoreFormat.valueOf(System.getProperty("javax.net.ssl.keyStoreType")))); //$NON-NLS-1$
				if ((keystoreContainer.isEmpty()) || (!(KeyStoreManager.INSTANCE.isKeyStoreInitialized()))) {
					JOptionPane.showMessageDialog(null,"Incorrect Password",null,
	                        JOptionPane.ERROR_MESSAGE);
					System.clearProperty("javax.net.ssl.keyStorePassword"); //$NON-NLS-1$
					pword.setText("");
				} else {
					//System.out.println("Your password is GOOD");
					PublishPasswordUpdate.INSTANCE.publishMessage(pw);
					break;
				}
			} else {
				System.out.println("wtf");
				break;
			}

		}
		return pw;
	}
}
