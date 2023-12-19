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
package org.eclipse.core.pki.auth;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public enum SecurityFileSnapshot {
	INSTANCE;
	Path pkiFile = null;
	Path userM2Home = null;
	public static final String USER_HOME = System.getProperty("user.home"); //$NON-NLS-1$
	public boolean image() {
		/*
		 * CHeck if .pki file is present.
		 */
		System.out.println("SecurityFileSnapshot -Searching HOME FILE:" + USER_HOME); //$NON-NLS-1$
		boolean isFound=false;
		try {

			if (System.getProperty("M2_HOME") != null) { //$NON-NLS-1$
				userM2Home = Paths.get(System.getProperty("M2_HOME")); //$NON-NLS-1$
			} else {
				// No M2_HOME is set so figure out where it is, check HOME first.
				userM2Home = Paths.get(USER_HOME, FileSystems.getDefault().getSeparator(), ".m2"); //$NON-NLS-1$
				System.out.println(
						"SecurityFileSnapshot -Searching NO M2_HOME set for FILE:" + userM2Home.toAbsolutePath()); //$NON-NLS-1$

			}

			pkiFile = Paths.get(userM2Home + "/.pki"); //$NON-NLS-1$

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		isSecurityFileRequired(""); //$NON-NLS-1$
		if (Files.exists(pkiFile)) {

			isFound=true;
		}
		return isFound;
	}
	public Properties load() {
		System.out.println("SecurityFileSnapshot -Searching HOME PATH:" + pkiFile.toString()); //$NON-NLS-1$
		System.out.println("SecurityFileSnapshot - loading properties from dot PKI file"); //$NON-NLS-1$
		Properties properties = new Properties();
		try {
			FileChannel fileChannel = FileChannel.open(pkiFile, StandardOpenOption.READ);
			FileLock lock = fileChannel.lock(0L, Long.MAX_VALUE,true);
			properties.load(Channels.newInputStream(fileChannel));
			for ( Entry<Object,Object>entry:properties.entrySet()) {
				entry.setValue(entry.getValue().toString().trim());
			}
			System.setProperties(properties);
			lock.release();
			System.out.println("SecurityFileSnapshot - loading properties COMPLETED"); //$NON-NLS-1$
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return properties;

	}

	//@SuppressWarnings("unused")
	private static void isSecurityFileRequired(String securityFileLocation) {
		Path dir = null;
		StringBuilder sb = new StringBuilder();

		try {
			sb.append(securityFileLocation);
			sb.append(FileSystems.getDefault().getSeparator());
			//sb.append("TESTDIR"); // testing
			//sb.append(FileSystems.getDefault().getSeparator());
			dir = Paths.get(sb.toString());
			Files.createDirectories(dir);

			// sb.append(".pki");//$NON-NLS-1$

			Path path = Paths.get(sb.toString());

			if (!(path.toFile().exists())) {
				Files.deleteIfExists(path);
				Files.createFile(path);
				Charset charset = Charset.forName("UTF-8");//$NON-NLS-1$
				ArrayList<String> a = fileContents();
				if ( FileSystems.getDefault().supportedFileAttributeViews().contains("posix") ) { //$NON-NLS-1$
					PosixFileAttributeView posixAttributes = Files.getFileAttributeView(path, PosixFileAttributeView.class);
					Set<PosixFilePermission> permissions = posixAttributes.readAttributes().permissions();
					permissions.remove(PosixFilePermission.GROUP_READ);
					posixAttributes.setPermissions(permissions);
					Files.write(path, a, charset, StandardOpenOption.TRUNCATE_EXISTING);
					//ls
					permissions.remove(PosixFilePermission.OWNER_WRITE);
					posixAttributes.setPermissions(permissions);
				} else {
					//Windoerz
					//DosFileAttributeView dosAttributes =  Files.getFileAttributeView(path, DosFileAttributeView.class);
					//DosFileAttributes standardPermissions = dosAttributes.readAttributes();
					Files.write(path, a, charset, StandardOpenOption.TRUNCATE_EXISTING);
					Files.setAttribute(path, "dos:hidden", Boolean.valueOf(true));//$NON-NLS-1$
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static ArrayList<String> fileContents() {

		ArrayList<String> a = new ArrayList<>();

		try {
			a.add("javax.net.ssl.trustStoreType="+ System.getProperty("javax.net.ssl.trustStoreType"));//$NON-NLS-1$ //$NON-NLS-2$
			a.add("javax.net.ssl.trustStorePassword="+ System.getProperty("javax.net.ssl.trustStorePassword"));//$NON-NLS-1$ //$NON-NLS-2$
			a.add("javax.net.ssl.trustStore="+ System.getProperty("javax.net.ssl.trustStore"));//$NON-NLS-1$ //$NON-NLS-2$
			a.add("");//$NON-NLS-1$


			if (System.getProperty("javax.net.ssl.keyStoreType") != null ) {//$NON-NLS-1$
				a.add("javax.net.ssl.keyStoreType="+ System.getProperty("javax.net.ssl.keyStoreType"));//$NON-NLS-1$ //$NON-NLS-2$
				a.add("javax.net.ssl.keyStore="+ System.getProperty("javax.net.ssl.keyStore")); //$NON-NLS-1$ //$NON-NLS-2$
				if (System.getProperty("javax.net.ssl.keyStoreType").equalsIgnoreCase("PKCS12")) { //$NON-NLS-1$ //$NON-NLS-2$
					//a.add("javax.net.ssl.keyStorePassword="+ System.getProperty("javax.net.ssl.keyStorePassword"));
				} else {
					a.add("javax.net.ssl.keyStorePassword=");//$NON-NLS-1$
					a.add("javax.net.ssl.keyStoreProvider="+ System.getProperty("javax.net.ssl.keyStoreProvider")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return a;
	}
}
