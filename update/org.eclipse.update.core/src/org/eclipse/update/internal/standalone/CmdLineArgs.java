/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.standalone;
import java.net.*;
import java.util.*;

import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.mirror.*;

public class CmdLineArgs {
	private HashMap options = new HashMap();
	public CmdLineArgs(String[] args) {
		// default command
		options.put("-command", "install");

		for (int i = 0; i < args.length - 1; i++) {
			if ("-command".equals(args[i])) {
				if (isValidCommand(args[i + 1])) {
					options.put("-command", args[i + 1]);
					i++;
				} else {
					StandaloneUpdateApplication.exceptionLogged();
					UpdateCore.log(
						Utilities.newCoreException(
							"Invalid command:" + args[i + 1],
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
			String to = (String) options.get("-to");
			if (to != null && to.startsWith("file:")) {
				try {
					URL url = new URL(to);
					options.put("-to", url.getFile());
				} catch (MalformedURLException mue) {
				}
			}
		}
	}

	private boolean isValidParam(String param) {
		return param.equals("-command")
			|| param.equals("-version")
			|| param.equals("-to")
			|| param.equals("-from")
			|| param.equals("-featureId")
			|| param.equals("-verifyOnly")
			|| param.equals("-mirrorURL");
	}

	private boolean isValidCommand(String cmd) {
		return cmd.equals("install")
			|| cmd.equals("enable")
			|| cmd.equals("disable")
			|| cmd.equals("search")
			|| cmd.equals("update")
			|| cmd.equals("mirror")
			|| cmd.equals("uninstall")
			|| cmd.equals("configuredFeatures")
			|| cmd.equals("addSite");
	}

	public ScriptedCommand getCommand() {
		try {
			String cmd = (String) options.get("-command");
			if (cmd.equals("install"))
				return new InstallCommand(
					(String) options.get("-featureId"),
					(String) options.get("-version"),
					(String) options.get("-from"),
					(String) options.get("-to"),
					(String) options.get("-verifyOnly"));
			else if (cmd.equals("enable"))
				return new EnableCommand(
					(String) options.get("-featureId"),
					(String) options.get("-version"),
					(String) options.get("-to"),
					(String) options.get("-verifyOnly"));
			else if (cmd.equals("disable"))
				return new DisableCommand(
					(String) options.get("-featureId"),
					(String) options.get("-version"),
					(String) options.get("-to"),
					(String) options.get("-verifyOnly"));
			else if (cmd.equals("search"))
				return new SearchCommand((String) options.get("-from"));
			else if (cmd.equals("update"))
				return new UpdateCommand(
					(String) options.get("-featureId"),
					(String) options.get("-verifyOnly"));
			else if (cmd.equals("mirror"))
				return new MirrorCommand(
					(String) options.get("-featureId"),
					(String) options.get("-version"),
					(String) options.get("-from"),
					(String) options.get("-to"),
					(String) options.get("-mirrorURL"));
			else if (cmd.equals("uninstall"))
				return new UninstallCommand(
					(String) options.get("-featureId"),
					(String) options.get("-version"),
					(String) options.get("-to"),
					(String) options.get("-verifyOnly"));
			else if (cmd.equals("configuredFeatures"))
				return new ListConfigFeaturesCommand((String) options.get("-from"));
			else if (cmd.equals("addSite"))
				return new AddSiteCommand((String) options.get("-from"));
			else
				return null;
		} catch (Exception e) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(e);
			return null;
		}
	}

}
