/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.help.internal.standalone;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.util.List;

import org.eclipse.help.internal.base.*;

/**
 * This program is used to start or stop Eclipse Infocenter application.
 */
public class StandaloneInfocenter extends EclipseController {
	// ID of the application to run
	private static final String INFOCENTER_APPLICATION_ID = HelpBasePlugin.PLUGIN_ID
			+ ".infocenterApplication"; //$NON-NLS-1$

	/**
	 * Constructs help system
	 *
	 * @param args
	 *            array of String options and their values Option
	 *            <code>-eclipseHome dir</code> specifies Eclipse installation
	 *            directory. It must be provided, when current directory is not
	 *            the same as Eclipse installation directory. Additionally, most
	 *            options accepted by Eclipse execuable are supported.
	 */
	public StandaloneInfocenter(String[] args) {
		super(INFOCENTER_APPLICATION_ID, args);
	}

	/**
	 * @see org.eclipse.help.standalone.Infocenter#main(String[])
	 */
	public static void main(String[] args) {
		try {
			StandaloneInfocenter infocenter = new StandaloneInfocenter(args);

			List<String> helpCommand = Options.getHelpCommand();

			final String adminId = Options.getAdminId();
			final String adminPassword = Options.getAdminPassword();
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(adminId, adminPassword.toCharArray());
				}
			});
			HttpURLConnection.setFollowRedirects(true);

			if (infocenter.executeCommand(helpCommand)) {
				return;
			}
			printMainUsage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return true if commands contained a known command and it was executed
	 */
	private boolean executeCommand(List<String> helpCommand) throws Exception {
		if (helpCommand.size() <= 0) {
			return false;
		}
		String command = helpCommand.get(0);
		if ("start".equalsIgnoreCase(command)) { //$NON-NLS-1$
			start();
			return true;
		} else if ("shutdown".equalsIgnoreCase(command)) { //$NON-NLS-1$
			shutdown();
			return true;
		} else if (CMD_INSTALL.equalsIgnoreCase(command)
				|| CMD_ENABLE.equalsIgnoreCase(command)
				|| CMD_DISABLE.equalsIgnoreCase(command)
				|| CMD_UNINSTALL.equalsIgnoreCase(command)
				|| CMD_UPDATE.equalsIgnoreCase(command)
				|| CMD_SEARCH.equalsIgnoreCase(command)
				|| CMD_LIST.equalsIgnoreCase(command)
				|| CMD_ADDSITE.equalsIgnoreCase(command)
				|| CMD_REMOVESITE.equalsIgnoreCase(command)
				|| CMD_APPLY.equalsIgnoreCase(command)) {
			return executeUpdateCommand(command);
		}
		return false;
	}

	/**
	 * Prints usage of this class as a program.
	 */
	private static void printMainUsage() {
		System.out.println("Parameters syntax:"); //$NON-NLS-1$
		System.out.println();
		System.out
				.println("-command start | shutdown | [update command [update parameters]] [-eclipsehome eclipseInstallPath] [-host helpServerHost] [-port helpServerPort] [-adminId administratorUserId] [-adminPassword administratorPassword] [-trustStoreLocation trustStoreLocation] [-trustStorePassword trustStorePassword][-noexec] [platform options] [-vmargs [Java VM arguments]]"); //$NON-NLS-1$
		System.out.println();
		System.out.println("where:"); //$NON-NLS-1$
		System.out
				.println(" eclipseInstallPath specifies Eclipse installation directory; this directory is a parent to \"plugins\" directory and eclipse executable;  the option must be provided, when current directory from which information center is launched, is not the same as Eclipse installation directory,"); //$NON-NLS-1$
		System.out
				.println(" helpServerHost specifies host name of the interface that help server will use,"); //$NON-NLS-1$
		System.out
				.println(" helpServerPort specifies port number that help server will use,"); //$NON-NLS-1$
		System.out
				.println(" administratorUserId specifies the administrator user id to use when secure access is enabled"); //$NON-NLS-1$
		System.out
				.println(" administratorPassword specifies the administrator password to use when secure access is enabled"); //$NON-NLS-1$
		System.out
				.println(" trustStoreLocation specifies the location of the truststore file to use when secure access is enabled"); //$NON-NLS-1$
		System.out
				.println(" trustStorePassword specifies the password of the truststore file when secure access is enabled"); //$NON-NLS-1$
		System.out
				.println(" noexec option indicates that Eclipse executable should not be used, "); //$NON-NLS-1$
		System.out
				.println(" platform options are other options that are supported by Eclipse Executable,"); //$NON-NLS-1$
		System.out
				.println(" update command is one of install, update, enable, disable, uninstall, search, listFeatures, addSite, removeSite, or apply,"); //$NON-NLS-1$
		System.out
				.println(" update parameters are -featureId, -version, -from, -to, -verifyOnly as required by update commands used."); //$NON-NLS-1$
	}

}
