/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.pki;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Provider;
import java.security.Security;
import java.util.Optional;
import java.util.Properties;


public enum PKCS11Provider {
	CONFIGURATION;

	String cfgDirectory = null;
	Optional<String> providerContainer = null;
	Provider prototype = null;
	String securityProvider = null;
	protected byte[] salt = new byte[16];
	protected String PKI_PROVIDER = "SunPKCS11"; //$NON-NLS-1$
	Properties properties = new Properties();
	public void setUp(String pin) {

		readProperties();

		Optional<String> configurationDirectory = Optional
				.ofNullable(properties.getProperty("javax.net.ssl.cfgFileLocation")); //$NON-NLS-1$

		if (configurationDirectory.isEmpty()) {
			cfgDirectory = new String("/etc/opensc"); //$NON-NLS-1$
		} else {
			cfgDirectory = configurationDirectory.get().toString();
		}

		if (Files.exists(Paths.get(cfgDirectory))) {

			providerContainer = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStoreProvider")); //$NON-NLS-1$

			if (providerContainer.isEmpty()) {
				securityProvider = PKI_PROVIDER;
			} else {
				securityProvider = providerContainer.get().toString();
			}
			prototype = Security.getProvider(securityProvider);
			if (prototype == null) {
				System.out.println(" PKCS11Provider PROVIDER NOT FOUND"); //$NON-NLS-1$
			}

			try {
				Provider provider = prototype.configure(cfgDirectory);

				Security.addProvider(provider);
				System.out.println("PKCS11Provider  PROVIDER ADDED"); //$NON-NLS-1$
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void readProperties() {

		final String USER_HOME = System.getProperty("user.home"); //$NON-NLS-1$
		Path userM2Home = Paths.get(USER_HOME + FileSystems.getDefault().getSeparator() + ".m2"); //$NON-NLS-1$
		Path filePath = Paths.get(userM2Home.toString() + FileSystems.getDefault().getSeparator() + ".pki"); //$NON-NLS-1$
		try {
			final FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ);
			final FileLock lock = channel.lock(0L, Long.MAX_VALUE, true);
			properties.load(Channels.newInputStream(channel));
			lock.release();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
