package org.eclipse.core.pki;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.util.Enumeration;

public class Pkcs11Location extends Pkcs11LocationImpl  {
	private static String userSpecifiedDirectory="none";
	private static boolean userDirFound=false;
	
	
	public static String getUserSpecifiedDirectory() {
		return userSpecifiedDirectory;
	}
	public static  void setUserSpecifiedDirectory(String userSpecifiedDirectory) {
		Pkcs11Location.userSpecifiedDirectory = userSpecifiedDirectory;
	}
	public String getJavaPkcs11Cfg() {
		return getPath().toString();
	}
	public static boolean isUserSpecifiedDirectoryFound() {
		StringBuilder sb = new StringBuilder();
		sb.append( getUserSpecifiedDirectory() );
		userDirFound = Pkcs11LocationImpl.isPath(sb);
		return userDirFound;
	}


	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		
		final String location="C:\\Program Files (x86)\\pkcs11";
		final String unixcsp="/home/user/pkcs11";
		System.out.println("Pkcs11Location --- MAIN");
		
		
		System.out.println("Pkcs11Location --- MAIN ARCH:"+System.getProperty("os.arch"));
		System.out.println("Pkcs11Location --- MAIN JVM Version:"+System.getProperty("java.version"));
		
//		try {
//			secure.eclipse.feature
//			KeyStore ks = KeyStore.getInstance("Windows-MY");
//			ks.load(null, null);
//			Provider pro = ks.getProvider();
//			System.out.println("Pkcs11Location --- MAIN  PROVIDER:"+ pro.getName() );
//			Enumeration em = ks.aliases();
//			while ( em.hasMoreElements() ) {
//				System.out.println("Pkcs11Location --- MAIN -- ALIAS:"+ em.nextElement() );
//			}
//			
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		Pkcs11Location testing =  new Pkcs11Location();
		//testing.setUserSpecifiedDirectory(unixcsp);
		testing.getPkcs11LocationInstance();
		if ( isFound() ) {
			System.out.println("Pkcs11Location --- CSPid CFG file FOUND:"+ testing.getDirectory() );
		} else {
			System.out.println("Pkcs11Location --- CSPid CFG file NOTFOUND:");
		}
		if ( testing.isPkcs11Found()) {
			System.out.println("Pkcs11Location --- PKCS11 location:"+ testing.jarLocation.getJarDirectory().toString() );
		} else {
			System.out.println("Pkcs11Location --- PKCS11 location NO JAR FOUND");
		}
		
		System.out.println("Pkcs11Location --- MAIN   USER DIR FOUND:"+testing.isUserSpecifiedDirectoryFound());
	}
}
