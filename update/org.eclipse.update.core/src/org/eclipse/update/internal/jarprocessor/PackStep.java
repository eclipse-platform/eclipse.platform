/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.jarprocessor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PackStep extends CommandStep {

	protected static String packCommand = null;
	private static Boolean canPack = null;

	private Set exclusions = Collections.EMPTY_SET;

	public static boolean canPack() {
		if (canPack != null)
			return canPack.booleanValue();

		String[] locations = Utils.getPack200Commands("pack200"); //$NON-NLS-1$
		if (locations == null) {
			canPack = Boolean.FALSE;
			packCommand = null;
			return false;
		}

		int result;
		for (int i = 0; i < locations.length; i++) {
			if (locations[i] == null)
				continue;
			result = execute(new String[] {locations[i], "-V"}); //$NON-NLS-1$
			if (result == 0) {
				packCommand = locations[i];
				canPack = Boolean.TRUE;
				return true;
			}
		}

		canPack = Boolean.FALSE;
		return false;
	}

	public PackStep(Properties options) {
		super(options, null, null, false);
		exclusions = Utils.getPackExclusions(options);
	}

	public PackStep(Properties options, boolean verbose) {
		super(options, null, null, verbose);
		exclusions = Utils.getPackExclusions(options);
	}

	public String recursionEffect(String entryName) {
		if (canPack() && entryName.endsWith(".jar") && !exclusions.contains(entryName)) { //$NON-NLS-1$
			return entryName + Utils.PACKED_SUFFIX;
		}
		return null;
	}

	public File preProcess(File input, File workingDirectory) {
		return null;
	}

	public File postProcess(File input, File workingDirectory) {
		if (canPack() && packCommand != null) {
			Properties inf = Utils.getEclipseInf(input);
			if (inf != null && inf.containsKey(Utils.MARK_EXCLUDE_PACK) && Boolean.valueOf(inf.getProperty(Utils.MARK_EXCLUDE_PACK)).booleanValue()) {
				if(verbose)
					System.out.println("Excluding " + input.getName() + " from " + getStepName()); //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}
			File outputFile = new File(workingDirectory, input.getName() + Utils.PACKED_SUFFIX);
			try {
				String[] cmd = getCommand(input, outputFile, inf);
				int result = execute(cmd, verbose);
				if (result != 0 && verbose)
					System.out.println("Error: " + result + " was returned from command: " + Utils.concat(cmd)); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IOException e) {
				if (verbose)
					e.printStackTrace();
				return null;
			}
			return outputFile;
		}
		return null;
	}

	protected String[] getCommand(File input, File outputFile, Properties inf) throws IOException {
		String[] cmd = null;
		String arguments = null;
		if (inf != null && inf.containsKey(Utils.PACK_ARGS)) {
			arguments = inf.getProperty(Utils.PACK_ARGS);
		} else {
			arguments = getOptions().getProperty(input.getName() + ".pack.args"); //$NON-NLS-1$
		}
		if (arguments != null && arguments.length() > 0) {
			String[] args = Utils.toStringArray(arguments, ","); //$NON-NLS-1$
			cmd = new String[3 + args.length];
			cmd[0] = packCommand;
			System.arraycopy(args, 0, cmd, 1, args.length);
			cmd[cmd.length - 2] = outputFile.getCanonicalPath();
			cmd[cmd.length - 1] = input.getCanonicalPath();
		} else {
			cmd = new String[] {packCommand, outputFile.getCanonicalPath(), input.getCanonicalPath()};
		}
		return cmd;
	}

	public String getStepName() {
		return "Pack"; //$NON-NLS-1$
	}
	
	public void adjustInf(File input, Properties inf) {
		if (input == null || inf == null)
			return;

		if (inf.containsKey(Utils.MARK_EXCLUDE_PACK) && Boolean.valueOf(inf.getProperty(Utils.MARK_EXCLUDE_PACK)).booleanValue()) {
			return;
		}

		inf.put(Utils.MARK_PROPERTY, "true"); //$NON-NLS-1$
		String arguments = inf.getProperty(Utils.PACK_ARGS);
		if (arguments == null) {
			arguments = getOptions().getProperty(input.getName() + ".pack.args"); //$NON-NLS-1$
			if (arguments != null)
				inf.put(Utils.PACK_ARGS, arguments);
		}
	}
}
