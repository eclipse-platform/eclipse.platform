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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;


public class ClassLoaderPKCS11 extends Pkcs11Location {
	Pkcs11LocationIfc locationService=getPkcs11LocationInstance();
	public ClassLoaderPKCS11() {}

	public Object loadClass() {
		Class<?> pkcs11Class = null;
		Object sunPkcs11 = null;
		String cfgFile=null;
		// String extJDKDirectory = System.getProperty("java.ext.dirs"); //$NON-NLS-1$
		String jarDirectory=null;

			try {
				try {
					jarDirectory=jarLocation.getJarDirectory().toString();
				} catch (Exception e1) {
					System.out.println(
							"There was a problem initializing the PKCS11 library.  You may be using a JRE, please try a JDK. "); //$NON-NLS-1$
					System.out.println(Arrays.toString(e1.getStackTrace()));
				}

				/*
				 *   Get the filename/directory where the java_cfg.xml file is located.
				 */
				cfgFile = getDirectory();

				System.out.println("CLASSLOADERPKCS11 --------------Loading up the OBJECT FILE JAR:" + jarDirectory); //$NON-NLS-1$
				ClassLoader pkcs11 = new SpecialClassLoader(System.getProperty(jarDirectory));
				pkcs11Class = pkcs11.loadClass("sun.security.pkcs11.SunPKCS11"); //$NON-NLS-1$

				if ( ( pkcs11Class != null )) {
					/*
					 *  The instance of pkcs11 needs to have the location of the pkcs11 configuration file.
					 *  On unix its usually in /opt/pkcs11.  However, its not always found so easily on
					 *  a windows or windos derivative eu2 setup.  The file is called
					 */
					try {
						System.out.println("CLASSLOADERPKCS11 --------------CFG FILE:" + cfgFile); //$NON-NLS-1$

						sunPkcs11 = pkcs11Class.getConstructor(java.lang.String.class).newInstance(cfgFile);

					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				System.out.println("CLASSLOADERPKCS11 -----------------------loadSunPKCS11Class    FAILED"); //$NON-NLS-1$
				//e.printStackTrace();

			}


		 return sunPkcs11;
	}
}
