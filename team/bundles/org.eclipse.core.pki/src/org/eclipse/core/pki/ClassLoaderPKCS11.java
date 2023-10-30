package org.eclipse.core.pki;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;


public class ClassLoaderPKCS11 extends Pkcs11Location {
	Pkcs11LocationIfc locationService=getPkcs11LocationInstance();
	public ClassLoaderPKCS11() {}
	
	public Object loadClass() {
		Class<?> pkcs11Class = null;
		Object sunPkcs11 = null;
		String cfgFile=null;
		String extJDKDirectory=System.getProperty("java.ext.dirs");
		String jarDirectory=null;

			try {
				try {
					jarDirectory=jarLocation.getJarDirectory().toString();
				} catch (Exception e1) {
					System.out.println("There was a problem initializing the PKCS11 library.  You may be using a JRE, please try a JDK. ");					
					DebugLogger.printDebug(Arrays.toString(e1.getStackTrace()));
				}

				/*
				 *   Get the filename/directory where the java_cfg.xml file is located.
				 */
				cfgFile = getDirectory();
				
				DebugLogger.printDebug("CLASSLOADERPKCS11 --------------Loading up the OBJECT FILE JAR:"+jarDirectory);
				ClassLoader pkcs11 = new SpecialClassLoader(System.getProperty(jarDirectory));
				pkcs11Class = pkcs11.loadClass("sun.security.pkcs11.SunPKCS11");
					
				if ( ( pkcs11Class != null )) {
					/*
					 *  The instance of pkcs11 needs to have the location of the pkcs11 configuration file.
					 *  On unix its usually in /opt/pkcs11.  However, its not always found so easily on
					 *  a windows or windos derivative eu2 setup.  The file is called
					 */
					try {
						DebugLogger.printDebug("CLASSLOADERPKCS11 --------------CFG FILE:"+cfgFile);
						
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
				DebugLogger.printDebug("CLASSLOADERPKCS11 -----------------------loadSunPKCS11Class    FAILED");
				//e.printStackTrace();
				
			} 
		
			 
		 return sunPkcs11;
	}
}
