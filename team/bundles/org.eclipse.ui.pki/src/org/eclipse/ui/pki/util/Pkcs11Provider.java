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
package org.eclipse.ui.pki.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.util.Properties;

import org.eclipse.ui.pki.preferences.AuthenticationPreferences;
import org.eclipse.ui.pki.AuthenticationPlugin;

public class Pkcs11Provider {
	
	private final static String JAVA_EXT_DIR = "java.ext.dirs";
	private static Class<?> pkcs11Class = null;
	private static String configurePath = null;
	
	
	public Pkcs11Provider(){}
	public static void listProviders() {
		for ( Provider provider : Security.getProviders() ) {
	    	//System.out.println("BEFORE ADDING ANY Provider NAME:"+ provider.getName() );
	    }
	}
	@Deprecated
	public static boolean addProvider() {
		 boolean isProvider = false;
		 try {
			/*
			 * TODO:  Make the single sign on work here.  
			 */
			 Provider provider = Security.getProvider(System.getProperty("javax.net.ssl.keyStoreProvider"));
			 //listProviders();
			 if ( provider != null ) {
				//System.out.println("Pkcs11Provider --------- pkcs11 IS INSTALLED  and MANAGER IS configured........................");
				
				AuthenticationPlugin.getDefault().getPreferenceStore().setValue(AuthenticationPreferences.PKI_SELECTION_TYPE, "pkcs11"); 
			 } 
			 ClassLoader pkcs11 = new SpecialClassLoader(System.getProperty(JAVA_EXT_DIR).split(":")[0]);
			 //Class<?> pkcs11Class = null;
			 pkcs11Class = pkcs11.loadClass("sun.security.pkcs11.SunPKCS11");
			 //Provider pkcs11Provider = new SunPKCS11(this.getOsInstalledPath().toString());
			 StringBuilder installedPkcs11 = new StringBuilder();
			 try {
				 /*
				  * NOTE:
				  * The configure path for pkcs11 may be altered, assuming its installed somewhere,
				  * This is done in the eclipse preference section. Otherwise,  the INSTALLATION
				  * is done via admin and is OS specific and not a user configured property.
				  */
				if ( getConfigurePath() != null) {
					installedPkcs11 = new StringBuilder();
					installedPkcs11.append(getConfigurePath());
				} else {
					installedPkcs11 = getOsInstalledPath();
				}
				
				if (!( installedPkcs11.toString().isEmpty())) {
					String cfg = configPath( installedPkcs11.toString() );
					Provider pkcs11Provider = (Provider)(pkcs11Class.getConstructor(java.lang.String.class).newInstance(cfg));
					Security.addProvider(pkcs11Provider);
					isProvider=true;
					AuthenticationPlugin.getDefault().getPreferenceStore().setValue(AuthenticationPreferences.PKI_SELECTION_TYPE, "pkcs11");
					//System.out.println("Pkcs11Provider ----  LOADED the provider from:"+cfg);
					setConfigurePath(cfg);
				}
				
			 } catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isProvider;
	}
	
	public void setSecurityProvider(String filePath) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, KeyStoreException{	
		String configName = filePath;	
		ClassLoader pkcs11 = new SpecialClassLoader(System.getProperty(JAVA_EXT_DIR).split(":")[0]);
		Class<?> pkcs11Class = null;
		Provider pkcs11Provider = null;
		pkcs11Class = pkcs11.loadClass("sun.security.pkcs11.SunPKCS11");		
		pkcs11Provider = (Provider)(pkcs11Class.getConstructor(java.lang.String.class).newInstance(configName));
		
		if(pkcs11Provider == null){
			throw new NullPointerException("PKCS11 PROVIDER IS NULL.");
		}
		
		Security.addProvider(pkcs11Provider);
	}

	public static String configPath(String specifiedLocation) {	
		StringBuilder location = new StringBuilder();
		if (( specifiedLocation != null) && (!(specifiedLocation.equalsIgnoreCase("NONE"))) && (!(specifiedLocation.equalsIgnoreCase("pkcs11")))) {
			location.append(specifiedLocation);
		} else {
			location = getOsInstalledPath();
		}
		if ((location.length() != 0) && (!(location.toString().endsWith("java_pkcs11.cfg")))){
			location.append(File.separator);
			location.append("java_pkcs11.cfg");
		}
		
		if(!getPkcsCfg(location.toString())){
			location = new StringBuilder();
		}
		
		return location.toString();		
	}
	@Deprecated
	public static StringBuilder getOsInstalledPath() {
		
		//String path=null;
		StringBuilder pkcs11Path = new StringBuilder();
		Properties p = System.getProperties();
		
		try {
			if (p.getProperty("os.name").equals("Linux")) {
				/*
				 *   The OS is Linux so process according to LINUX rules.
				 */
				pkcs11Path.append("/opt/pkcs11");
				if ( !(getPkcsCfg(pkcs11Path.toString() ))) {
					pkcs11Path = new StringBuilder();
					pkcs11Path.append(p.getProperty("user.home"));
					pkcs11Path.append(File.separator);
					pkcs11Path.append("pkcs11");
					
					if (!(getPkcsCfg(pkcs11Path.toString()))) { 
						pkcs11Path = new StringBuilder();
					}
				}
			} 
			if (p.getProperty("os.name").contains("Windows")) {
				pkcs11Path.append("C:");
				pkcs11Path.append(File.separator);
				pkcs11Path.append("Windows");
				pkcs11Path.append(File.separator);
				pkcs11Path.append("SysWOW64");
				pkcs11Path.append(File.separator);
				pkcs11Path.append("pkcs11");
				
				if (!(getPkcsCfg( pkcs11Path.toString() ))) { 					
					pkcs11Path = new StringBuilder();
					pkcs11Path.append("C:");
					pkcs11Path.append(File.separator);
					pkcs11Path.append("Progra~2");
					pkcs11Path.append(File.separator);
					pkcs11Path.append("pkcs11");
					if (!(getPkcsCfg(pkcs11Path.toString()))) { 
						pkcs11Path = new StringBuilder();
					}
				}
				
			} if (pkcs11Path.length() == 0 ) {
				//To Do
				//System.out.println("Pkcs11Provider ----   NO PKCS11 IS INSTALLED");
				throw new KeyStoreException("You have specified an invalid location, Is pkcs11 installed?");
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return pkcs11Path;
	}
	
	private static boolean getPkcsCfg(String incoming) {
		File file = new File(incoming);
		return file.exists();
	}
	@Deprecated
	public static String getConfigurePath() {
		return configurePath;
	}
	@Deprecated
	public static void setConfigurePath(String configurePath) {
		//System.out.println("Pkcs11Provider:    setConfigurePath" );
		Pkcs11Provider.configurePath = configurePath;
	}
	public Class getProviderClass() {
		return pkcs11Class;
		
	}
}
