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
package org.eclipse.core.pki;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class Pkcs11FixConfigFile {
	private static final String programFiles = "Program Files (x86)"; //$NON-NLS-1$
	private static final CharSequence none = ""; //$NON-NLS-1$
	private static final CharSequence quotes = "\""; //$NON-NLS-1$
	private static final CharSequence sslash = "\\"; //$NON-NLS-1$
	private static final CharSequence bslash = "\\\\"; //$NON-NLS-1$
	private static final String biT32 = "cspid.dll"; //$NON-NLS-1$
	private static final String biT64 = "cspidx64.dll"; //$NON-NLS-1$
	private String cfgFilePath=null;
	private static String cspidHome;
	private static Pkcs11FixConfigFile configFile=null;
	private static StringBuffer sb = new StringBuffer();
	public static Pkcs11FixConfigFile getCfgInstance(String fileLocation) {
		if ( configFile == null ) {
			synchronized(Pkcs11FixConfigFile.class) {
				if ( configFile == null ) {
					configFile = new Pkcs11FixConfigFile();
					cspidHome = fileLocation;
					DebugLogger.printDebug("Pkcs11FixConfigFile --  incoming path:" + fileLocation); //$NON-NLS-1$
					sb.append(fileLocation);
					initialize();
				}
			}
		}
		return configFile;
	}
	public static void initialize() {
		try {
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append("java_pkcs11.cfg"); //$NON-NLS-1$
			Path path = openFile( sb.toString());
			Files.setAttribute(path, "dos:readonly", Boolean.valueOf(false)); //$NON-NLS-1$
			List<String> list = readFile(path);
			List<String> edit = editFile(list);
			Path outputPath = setOutputDirectory();
			saveFile(edit, outputPath);
			//listEditedFile(edit);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static Path setOutputDirectory() {
		Path path = null;
		StringBuilder sb = new StringBuilder();
		try {
			String appData = System.getenv("AppData"); //$NON-NLS-1$
			appData = Paths.get(appData).toAbsolutePath().toString().replace(sslash, bslash);
			if ( appData != null ) {
				sb.append(appData);
			} else {
				sb.append(System.getProperty("user.dir")); //$NON-NLS-1$
			}
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append("cspid"); //$NON-NLS-1$
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append("java_pkcs11.cfg"); //$NON-NLS-1$

			path = Paths.get(sb.toString());
			//setCfgFilePath( sb.toString());
			DebugLogger.printDebug("Pkcs11FixCOnfigFile ----  cspidHome:" + cspidHome); //$NON-NLS-1$
			setCfgFilePath( CreateCFGfile.initialize( cspidHome ));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return path;
	}
	public static Path openFile(String s) {
		return  FileSystems.getDefault().getPath(s);
	}
	public static List<String> readFile(Path path) {
		List <String>list = null;
		try {
			list = Files.readAllLines(path, Charset.defaultCharset());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	public static List<String> editFile(List<String> list) {
		ArrayList<String> edit = new ArrayList<>();
		StringBuffer sb = new StringBuffer();

		for ( String s : list) {
			CharSequence ch = "/"; //$NON-NLS-1$

			if ((s.contains("library")) && (System.getenv("PKCS11_HOME") != null)) { //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("library="); //$NON-NLS-1$
				sb.append(System.getenv("PKCS11_HOME")); //$NON-NLS-1$
				sb.append(ch);
				sb.append(biT64);
				s=sb.toString();
			}

			if (!(s.startsWith("#"))) { //$NON-NLS-1$
				if ((s.contains(biT32)) && (System.getProperty("os.arch").contains("64"))) { //$NON-NLS-1$ //$NON-NLS-2$
					s = s.replaceAll(biT32, biT64);
				}
			}
			if ( s.contains(quotes)) {
				s=s.replace(quotes, none);
			}

			if ( s.contains(programFiles)) {
				if (System.getenv("PKCS11_HOME") != null) { //$NON-NLS-1$
					s = System.getenv("PKCS11_HOME"); //$NON-NLS-1$
				} else {
					s = s.replace(programFiles, "Progra~2"); //$NON-NLS-1$
				}
				edit.add(s);
			} else {
				if (!( s.trim().isEmpty() )) {
					edit.add(s);
				}
			}
		}
		return edit;
	}
	public static void saveFile( List<String> list , Path path) {
		try {
			Files.write(path, list, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getCfgFilePath() {
		return cfgFilePath;
	}
	public static void setCfgFilePath(String cfgFilePath) {
		configFile.cfgFilePath = cfgFilePath;
	}
	/*
	 * private static void listEditedFile( List <String> list ) {
	 * DebugLogger.printDebug("PKCS11FixConfigFile ----  ARCH:" +
	 * System.getProperty("os.arch")); //$NON-NLS-1$ //$NON-NLS-2$ for ( String s :
	 * list) { DebugLogger.printDebug("PKCS11FixConfigFile ----  edited line:" + s);
	 * //$NON-NLS-1$ } }
	 */
}
