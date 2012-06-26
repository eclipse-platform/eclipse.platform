/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.standalone;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.internal.core.*;

/**
 * The application class used to launch standalone update commands.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class StandaloneUpdateApplication implements IPlatformRunnable {

	public final static Integer EXIT_ERROR = new Integer(1);
	private static boolean loggedException = false;

	/* (non-Javadoc)
	 * @see org.eclipse.core.boot.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(Object args) throws Exception {
		if (args == null)
			return EXIT_ERROR;
		if (args instanceof String[]) {
			String[] params = (String[]) args;
			CmdLineArgs cmdLineArgs = new CmdLineArgs(params);
			ScriptedCommand cmd = cmdLineArgs.getCommand();
			if (cmd == null) {
				System.out.println(NLS.bind(Messages.Standalone_cmdFailed, (new String[] { Platform.getLogFileLocation().toOSString() })));
				return EXIT_ERROR;
			}
			loggedException = false;
			boolean result = cmd.run();
			if (result) {
				if (loggedException) {
					System.out.println(NLS.bind(Messages.Standalone_cmdCompleteWithErrors, (new String[] { Platform.getLogFileLocation().toOSString() })));
				} else {
					System.out.println(Messages.Standalone_cmdOK); 
				}
				return IPlatformRunnable.EXIT_OK;
			} else {
				if (loggedException) {
					System.out.println(NLS.bind(Messages.Standalone_cmdFailed, (new String[] { Platform.getLogFileLocation().toOSString() })));
				} else {
					System.out.println(Messages.Standalone_cmdFailedNoLog);
				}
				return EXIT_ERROR;
			}
		}
		return EXIT_ERROR;
	}
	public static void exceptionLogged() {
		loggedException = true;
	}

}
