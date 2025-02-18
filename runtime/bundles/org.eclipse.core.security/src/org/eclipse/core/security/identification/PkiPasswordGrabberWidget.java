/*******************************************************************************
 * Copyright (c) 2025 Eclipse Platform, Security Group and others.
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
package org.eclipse.core.security.identification;

import java.util.Optional;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

import org.eclipse.core.security.managers.KeyStoreManager;
import org.eclipse.core.security.util.KeyStoreFormat;


public class PkiPasswordGrabberWidget implements Runnable {
	JFrame frame = null;
	Icon icon = null;
	private static PkiPasswordGrabberWidget INSTANCE;
	JPasswordField pword = null;
	private PkiPasswordGrabberWidget() {}
	public static PkiPasswordGrabberWidget getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new PkiPasswordGrabberWidget();
		}
		return INSTANCE;
	}
	@Override
	public void run() {
		try {
			String pw = getInput();
			System.setProperty("javax.net.ssl.keyStorePassword", pw);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public String getInput() {

		Optional keystoreContainer = null;
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Enter Password:");//$NON-NLS-1$
		JLabel blankie = new JLabel("\n", SwingConstants.CENTER);//$NON-NLS-1$
		pword = new JPasswordField(17);
		String pw=null;
		panel.add(label);
		panel.add(blankie);
		panel.add(pword);
		try {
			
			icon = new ImageIcon(getClass().getResource("/icons/icons8-password-48.png"));//$NON-NLS-1$
		} catch (Exception iconErr) {
		}

		panel.requestFocus();
		char[] password = null;
		while (true) {
			String[] options = new String[] {"cancel", "submit"};//$NON-NLS-1$

			//showOptionDialog(Component parentComponent,
			//		Object message, String title, int optionType,
			//		int messageType, Icon icon, Object[] options,
			//		Object initialValue)

			int option = JOptionPane.showOptionDialog(null, panel, "Eclipse PKI Password/PiN Entry",
	                JOptionPane.INFORMATION_MESSAGE, JOptionPane.PLAIN_MESSAGE,
					icon, options, options[1]);//$NON-NLS-1$

			if (option == 0) {
				JOptionPane.showMessageDialog(null,"CANCELED",null,
                        JOptionPane.ERROR_MESSAGE);//$NON-NLS-1$
				break;
			} else if(option == 1) {
				password = pword.getPassword();
				pw=new String(password);
				System.setProperty("javax.net.ssl.keyStorePassword", pw); //$NON-NLS-1$

				keystoreContainer = Optional
						.ofNullable(KeyStoreManager.getInstance().getKeyStore(System.getProperty("javax.net.ssl.keyStore"), //$NON-NLS-1$
								System.getProperty("javax.net.ssl.keyStorePassword"), //$NON-NLS-1$
								KeyStoreFormat.valueOf(System.getProperty("javax.net.ssl.keyStoreType")))); //$NON-NLS-1$
				if ((keystoreContainer.isEmpty()) || (!(KeyStoreManager.getInstance().isKeyStoreInitialized()))) {
					JOptionPane.showMessageDialog(null,"Incorrect Password",null,
	                        JOptionPane.ERROR_MESSAGE);//$NON-NLS-1$
					System.clearProperty("javax.net.ssl.keyStorePassword"); //$NON-NLS-1$
					pword.setText("");//$NON-NLS-1$
				} else {
					PublishPasswordUpdate.getInstance().publishMessage(pw);
					break;
				}
			} else {
				break;
			}
		}
		return pw;
	}
}
