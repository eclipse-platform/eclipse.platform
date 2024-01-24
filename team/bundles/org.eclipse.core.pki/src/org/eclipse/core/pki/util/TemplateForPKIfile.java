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
package org.eclipse.core.pki.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public enum TemplateForPKIfile {
	CREATION;

	public final String hashTag = "############################################################"; //$NON-NLS-1$
	public final String shortHashTag = "################"; //$NON-NLS-1$
	public final String USER_HOME = System.getProperty("user.home"); //$NON-NLS-1$
	Path userM2Home = null;

	public void setup() {
		userM2Home = Paths.get(USER_HOME + FileSystems.getDefault().getSeparator() + ".m2" + //$NON-NLS-1$
				FileSystems.getDefault().getSeparator() + ".pki"); //$NON-NLS-1$
		if (!(Files.exists(userM2Home))) {
			System.out.println("TemplateForPKIfile .pki file not  found"); //$NON-NLS-1$
			userM2Home = Paths.get(USER_HOME + FileSystems.getDefault().getSeparator() + ".m2" + //$NON-NLS-1$
					FileSystems.getDefault().getSeparator() + "pki.template"); //$NON-NLS-1$
			if (!(Files.exists(userM2Home))) {
				System.out.println("TemplateForPKIfile pki.template file not  found"); //$NON-NLS-1$
				createTemplate(userM2Home);
			}
		} else {
			System.out.println("TemplateForPKIfile .pki file found"); //$NON-NLS-1$
		}
	}

	public void createTemplate(Path path) {
		String editTag = "Edit this File, Save as .pki"; //$NON-NLS-1$
		try {
			Files.createFile(path);
			Files.write(path, (hashTag + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
			Files.write(path, (hashTag + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
			Files.write(path, shortHashTag.getBytes(), StandardOpenOption.APPEND);
			Files.write(path, editTag.getBytes(), StandardOpenOption.APPEND);
			Files.write(path, (shortHashTag + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
			Files.write(path, (hashTag + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
			Files.write(path, (hashTag + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
			Files.write(path, ((buildBuffer()) + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
			System.out.println("TemplateForPKIfile .pki.template file created"); //$NON-NLS-1$

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String buildBuffer() {
		StringBuilder b = new StringBuilder();
		b.append("javax.net.ssl.keyStore="); //$NON-NLS-1$
		b.append("[Fully quallified name of your Keystore File]"); //$NON-NLS-1$
		b.append(System.lineSeparator());
		b.append("javax.net.ssl.keyStorePassword="); //$NON-NLS-1$
		b.append("[Eclipse will encrypt this entry]"); //$NON-NLS-1$
		b.append(System.lineSeparator());
		b.append("javax.net.ssl.keyStoreType="); //$NON-NLS-1$
		b.append("[types allowed; PCKS11, PKCS12]"); //$NON-NLS-1$
		b.append(System.lineSeparator());
		b.append("javax.net.ssl.keyStoreProvider="); //$NON-NLS-1$
		b.append("[SunPKCS11, PKCS12]"); //$NON-NLS-1$
		b.append(System.lineSeparator());
		b.append("javax.net.ssl.trustStore="); //$NON-NLS-1$
		b.append("[Fully quallified name of your Truststore File]"); //$NON-NLS-1$
		b.append(System.lineSeparator());
		b.append("javax.net.ssl.trustStorePassword="); //$NON-NLS-1$
		b.append(System.lineSeparator());
		b.append("javax.net.ssl.trustStoreType="); //$NON-NLS-1$
		b.append(System.lineSeparator());
		b.append(hashTag);
		b.append(System.lineSeparator());
		return b.toString();
	}

	public static void main(String[] args) {
		TemplateForPKIfile.CREATION.setup();
	}
}
