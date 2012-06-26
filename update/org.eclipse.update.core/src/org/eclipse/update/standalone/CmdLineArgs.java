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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.update.core.Utilities;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.mirror.MirrorCommand;

/**
 * This class parses the command line arguments for update standalone commands
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
public class CmdLineArgs {
	private HashMap options = new HashMap();
	public CmdLineArgs(String[] args) {
//		// default command
//		options.put("-command", "install");

		for (int i = 0; i < args.length - 1; i++) {
			if ("-command".equals(args[i])) { //$NON-NLS-1$
				if (isValidCommand(args[i + 1])) {
					options.put("-command", args[i + 1]); //$NON-NLS-1$
					i++;
				} else {
					StandaloneUpdateApplication.exceptionLogged();
					UpdateCore.log(
						Utilities.newCoreException(
							Messages.Standalone_invalidCmd + args[i + 1], 
							null));
					return;
				}
			}

			if (isValidParam(args[i])) {
				options.put(args[i], args[i + 1]);
				i++;

			}
			// -to should specify a directory
			// if -to specifies file URL, change it to a directory
			String to = (String) options.get("-to"); //$NON-NLS-1$
			if (to != null && to.startsWith("file:")) { //$NON-NLS-1$
				try {
					URL url = new URL(to);
					options.put("-to", url.getFile()); //$NON-NLS-1$
				} catch (MalformedURLException mue) {
				}
			}
		}
	}

	private boolean isValidParam(String param) {
		return param.equals("-command") //$NON-NLS-1$
			|| param.equals("-version") //$NON-NLS-1$
			|| param.equals("-to") //$NON-NLS-1$
			|| param.equals("-from") //$NON-NLS-1$
			|| param.equals("-featureId") //$NON-NLS-1$
			|| param.equals("-verifyOnly") //$NON-NLS-1$
			|| param.equals("-mirrorURL") //$NON-NLS-1$
		    || param.equals("-ignoreMissingPlugins"); //$NON-NLS-1$
	}

	private boolean isValidCommand(String cmd) {
		if (cmd == null)
			return false;
		else
			return cmd.equals("install") //$NON-NLS-1$
			|| cmd.equals("enable") //$NON-NLS-1$
			|| cmd.equals("disable") //$NON-NLS-1$
			|| cmd.equals("search") //$NON-NLS-1$
			|| cmd.equals("update") //$NON-NLS-1$
			|| cmd.equals("mirror") //$NON-NLS-1$
			|| cmd.equals("uninstall") //$NON-NLS-1$
			|| cmd.equals("listFeatures") //$NON-NLS-1$
			|| cmd.equals("addSite") //$NON-NLS-1$
			|| cmd.equals("removeSite"); //$NON-NLS-1$
	}

	public ScriptedCommand getCommand() {
		try {
			String cmd = (String) options.get("-command"); //$NON-NLS-1$
			if (cmd == null)
				return null;
			if (cmd.equals("install")) //$NON-NLS-1$
				return new InstallCommand(
					(String) options.get("-featureId"), //$NON-NLS-1$
					(String) options.get("-version"), //$NON-NLS-1$
					(String) options.get("-from"), //$NON-NLS-1$
					(String) options.get("-to"), //$NON-NLS-1$
					(String) options.get("-verifyOnly")); //$NON-NLS-1$
			else if (cmd.equals("enable")) //$NON-NLS-1$
				return new EnableCommand(
					(String) options.get("-featureId"), //$NON-NLS-1$
					(String) options.get("-version"), //$NON-NLS-1$
					(String) options.get("-to"), //$NON-NLS-1$
					(String) options.get("-verifyOnly")); //$NON-NLS-1$
			else if (cmd.equals("disable")) //$NON-NLS-1$
				return new DisableCommand(
					(String) options.get("-featureId"), //$NON-NLS-1$
					(String) options.get("-version"), //$NON-NLS-1$
					(String) options.get("-to"), //$NON-NLS-1$
					(String) options.get("-verifyOnly")); //$NON-NLS-1$
			else if (cmd.equals("search")) //$NON-NLS-1$
				return new SearchCommand((String) options.get("-from")); //$NON-NLS-1$
			else if (cmd.equals("update")) //$NON-NLS-1$
				return new UpdateCommand(
					(String) options.get("-featureId"), //$NON-NLS-1$
					(String) options.get("-version"), //$NON-NLS-1$
					(String) options.get("-verifyOnly")); //$NON-NLS-1$
			else if (cmd.equals("mirror")) //$NON-NLS-1$
				return new MirrorCommand(
					(String) options.get("-featureId"), //$NON-NLS-1$
					(String) options.get("-version"), //$NON-NLS-1$
					(String) options.get("-from"), //$NON-NLS-1$
					(String) options.get("-to"), //$NON-NLS-1$
					(String) options.get("-mirrorURL"), //$NON-NLS-1$
					(String) options.get("-ignoreMissingPlugins")); //$NON-NLS-1$
			else if (cmd.equals("uninstall")) //$NON-NLS-1$
				return new UninstallCommand(
					(String) options.get("-featureId"), //$NON-NLS-1$
					(String) options.get("-version"), //$NON-NLS-1$
					(String) options.get("-to"), //$NON-NLS-1$
					(String) options.get("-verifyOnly")); //$NON-NLS-1$
			else if (cmd.equals("listFeatures")) //$NON-NLS-1$
				return new ListFeaturesCommand((String) options.get("-from")); //$NON-NLS-1$
			else if (cmd.equals("addSite")) //$NON-NLS-1$
				return new AddSiteCommand((String) options.get("-from")); //$NON-NLS-1$
			else if (cmd.equals("removeSite")) //$NON-NLS-1$
				return new RemoveSiteCommand((String) options.get("-to")); //$NON-NLS-1$
			else
				return null;
		} catch (Exception e) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(e);
			return null;
		}
	}

}
