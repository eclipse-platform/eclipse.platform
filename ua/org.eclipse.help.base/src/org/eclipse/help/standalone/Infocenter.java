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
package org.eclipse.help.standalone;

import org.eclipse.help.internal.standalone.*;

/**
 * This is a standalone infocenter. It takes care of launching the Eclipse with
 * its help system implementation. This class can be instantiated and used in a
 * Java program, or can be launched from command line.
 *
 * Usage as a Java component:
 * <ul>
 * <li>create an instantance of this class</li>
 * <li>call start(), infocenter will run</li>
 * <li>when no longer needed call shutdown().</li>
 * </ul>
 */
public class Infocenter {
	private StandaloneInfocenter infocenter;
	/**
	 * Constructs Infocenter
	 *
	 * @param options
	 *            array of String options and their values
	 *            <p>
	 *            Option <code>-eclipseHome dir</code> specifies Eclipse
	 *            installation directory. This directory is a parent to
	 *            "plugins" directory and eclipse executable. The option must be
	 *            provided, when current directory from which infocenter is
	 *            launched, is not the same as Eclipse installation directory.
	 *            <p>
	 *            Option <code>-host helpServerHost</code> specifies host name
	 *            of the interface that help server will use. It overrides host
	 *            name specified in the application server plugin preferences.
	 *            <p>
	 *            Option <code>-port helpServerPort</code> specifies port
	 *            number that help server will use. It overrides port number
	 *            specified in the application server plugin preferences.
	 *            <p>
	 *            Option <code>-dir rtl</code> sets right-to-left rendering
	 *            direction of help UI in the browser.
	 *            <p>
	 *            Option <code>-noexec</code> indicates that Eclipse
	 *            executable should not be used.
	 *            <p>
	 *            Additionally, most options accepted by Eclipse execuable are
	 *            supported.
	 */
	public Infocenter(String[] options) {
		infocenter = new StandaloneInfocenter(options);
	}
	/**
	 * Starts the stand alone infocenter.
	 */
	public void start() throws Exception {
		infocenter.start();
	}
	/**
	 * Shuts-down the stand alone infocenter.
	 */
	public void shutdown() throws Exception {
		infocenter.shutdown();
	}

	/**
	 * Controls start up and shut down of infocenter from command line.
	 *
	 * @param args
	 *            array of String containing options.<br>
	 *            Options are:<br>
	 *            <code>-command start | shutdown [-eclipsehome eclipseInstallPath] [-host helpServerHost] [-port helpServerPort] [-dir rtl] [-noexec] [platform options] [-vmargs JavaVMarguments]</code>
	 *            <br>
	 *            where:
	 *            <ul>
	 *            <li><code>eclipseInstallPath</code> specifies Eclipse
	 *            installation directory; it must be provided, when current
	 *            directory is not the same as Eclipse installation directory,
	 *            </li>
	 *            <li><code>helpServerHost</code> specifies host name of the
	 *            interface that help server will use, it overrides host name
	 *            specified the application server plugin preferences</li>
	 *            <li><code>helpServerPort</code> specifies port number that
	 *            help server will use, it overrides port number specified the
	 *            application server plugin preferences.</li>
	 *            <li><code>-dir rtl</code> sets right-to-left rendering
	 *            direction of help UI in the browser.
	 *            <li><code>-noexec</code> option indicates that Eclipse
	 *            executable should not be used.</li>
	 *            <li><code>platform options</code> are other options that are
	 *            supported by Eclipse Executable.</li>
	 *            </ul>
	 */
	public static void main(String[] args) {
		StandaloneInfocenter.main(args);
	}
}
