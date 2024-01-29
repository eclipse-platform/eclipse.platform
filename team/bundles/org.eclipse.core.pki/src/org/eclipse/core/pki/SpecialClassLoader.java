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
import java.nio.file.Files;
import java.nio.file.Paths;

public class SpecialClassLoader extends ClassLoader {

	private String findThisClassAt = null;

	public SpecialClassLoader(String classlocation){
		findThisClassAt = classlocation;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException{
		try{
			byte[] classBytes = null;
			classBytes = loadClassbytes(name);
			Class<?> cl = defineClass(name, classBytes, 0, classBytes.length);
			if (cl == null) throw new ClassNotFoundException(name);
			return cl;
		} catch (IOException e){
			throw new ClassNotFoundException(name);
		}
	}

	private byte[] loadClassbytes(String name) throws IOException {

		return Files.readAllBytes(Paths.get(findThisClassAt));
	}

	public String getFindThisClassAtLocation() {
		return findThisClassAt;
	}

}
