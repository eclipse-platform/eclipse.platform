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

import java.util.List;

import org.eclipse.help.internal.base.*;

/**
 * This is a standalone help system. It takes care of launching the eclipse with
 * its help system implementation, and controling it. This class can be used
 * instantiated and used in a Java program, or can be launched from command line
 * to execute single help action.
 *
 * Usage as a Java component:
 * <ul>
 * <li>create an instantance of this class and then hold onto this instance for
 * the duration of your application</li>
 * <li>call start()</li>
 * <li>call displayHelp(...) or displayContext(..) any number of times</li>
 * <li>at the end, call shutdown().</li>
 * </ul>
 */
public class StandaloneHelp extends EclipseController {
	// ID of the application to run
	private static final String HELP_APPLICATION_ID = HelpBasePlugin.PLUGIN_ID
			+ ".helpApplication"; //$NON-NLS-1$

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
	public StandaloneHelp(String[] args) {
		super(HELP_APPLICATION_ID, args);
	}

	/**
	 * @see org.eclipse.help.standalone.Infocenter#main(String[])
	 */
	public static void main(String[] args) {
		try {
			StandaloneHelp help = new StandaloneHelp(args);

			List<String> helpCommand = Options.getHelpCommand();

			if (help.executeCommand(helpCommand)) {
				return;
			}
			printMainUsage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see org.eclipse.help.standalone.Help#displayContext(java.lang.String,int,int)
	 */
	public void displayContext(String contextId, int x, int y) {
	}

	/**
	 * @see org.eclipse.help.standalone.Help#displayContextInfopop(java.lang.String,int,int)
	 */
	public void displayContextInfopop(String contextId, int x, int y) {
	}

	/**
	 * @see org.eclipse.help.standalone.Help#displayHelp()
	 */
	public void displayHelp() throws Exception {
		sendHelpCommand("displayHelp", new String[0]); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.help.standalone.Help#displayHelp(java.lang.String)
	 */
	public void displayHelp(String href) throws Exception {
		sendHelpCommand("displayHelp", new String[]{"href=" + href}); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @see org.eclipse.help.standalone.Help#displayHelp()
	 */
	public void displayHelpWindow() throws Exception {
		sendHelpCommand("displayHelpWindow", new String[0]); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.help.standalone.Help#displayHelp(java.lang.String)
	 */
	public void displayHelpWindow(String href) throws Exception {
		sendHelpCommand("displayHelpWindow", new String[]{"href=" + href}); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return true if commands contained a known command and it was executed
	 */
	private boolean executeCommand(List<String> helpCommands) throws Exception {

		if (helpCommands.size() <= 0) {
			return false;
		}
		String command = helpCommands.get(0);

		if ("start".equalsIgnoreCase(command)) { //$NON-NLS-1$
			start();
			return true;
		} else if ("shutdown".equalsIgnoreCase(command)) { //$NON-NLS-1$
			shutdown();
			return true;
		} else if ("displayHelp".equalsIgnoreCase(command)) { //$NON-NLS-1$
			if (helpCommands.size() >= 2) {
				displayHelp(helpCommands.get(1));
			} else {
				displayHelp();
			}
			return true;
		} else if ("displayHelpWindow".equalsIgnoreCase(command)) { //$NON-NLS-1$
			if (helpCommands.size() >= 2) {
				displayHelpWindow(helpCommands.get(1));
			} else {
				displayHelpWindow();
			}
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
		} else if ("displayContext".equalsIgnoreCase(command)) { //$NON-NLS-1$
			if (helpCommands.size() >= 4) {
				displayContext(helpCommands.get(1), Integer
						.parseInt(helpCommands.get(2)), Integer
						.parseInt(helpCommands.get(3)));

				return true;
			}
		} else if ("displayContextInfopop".equalsIgnoreCase(command)) { //$NON-NLS-1$
			if (helpCommands.size() >= 4) {
				displayContextInfopop(helpCommands.get(1), Integer
						.parseInt(helpCommands.get(2)), Integer
						.parseInt(helpCommands.get(3)));
				return true;
			}
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
				.println("-command start | shutdown | (displayHelp [href]) [-eclipsehome eclipseInstallPath] [-host helpServerHost] [-port helpServerPort] [platform options] [-vmargs [Java VM arguments]]"); //$NON-NLS-1$
		System.out.println();
		System.out.println("where:"); //$NON-NLS-1$
		System.out.println(" href is the URL of the help resource to display,"); //$NON-NLS-1$
		System.out
				.println(" eclipseInstallPath specifies Eclipse installation directory; this directory is a parent to \"plugins\" directory and eclipse executable;  the option must be provided, when current directory from which information center is launched, is not the same as Eclipse installation directory,"); //$NON-NLS-1$
		System.out
				.println(" helpServerHost specifies host name of the interface that help server will use,"); //$NON-NLS-1$
		System.out
				.println(" helpServerPort specifies port number that help server will use,"); //$NON-NLS-1$
		System.out
				.println(" platform options are other options that are supported by Eclipse Executable."); //$NON-NLS-1$
	}
}
