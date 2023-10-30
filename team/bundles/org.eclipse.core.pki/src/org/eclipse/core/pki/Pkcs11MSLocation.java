package org.eclipse.core.pki;

import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Properties;



/*
 *  This class was a best guess to find out where the required java and pkcs11
 *  files were located.   A PKCS11_HOME variable overrides the implementation.
 */

public class Pkcs11MSLocation extends Pkcs11Location implements Pkcs11LocationIfc {
	private static final String vendorDirectory = System.getProperty("vendor.install.dir");

	private static final String dir64bit = "C:\\Progra~2\\"+vendorDirectory;
	private static final String dir32bit = "C:\\Windows\\SysWow64\\"+vendorDirectory;
	private static  Pkcs11LocationIfc location=null;
	public static Pkcs11MSLocation getInstance() {
		if ( location == null ) {
			synchronized(Pkcs11LocationImpl.class) {
				if ( location == null ) {
					location = new Pkcs11MSLocation();
					location.initialize();	
				}
			}
		}
		return (Pkcs11MSLocation) location; 
	}
	public void initialize() {
		windowsPath();
	}
	private void windowsPath() {
		StringBuilder pkcs11Path = new StringBuilder();
		Pkcs11FixConfigFile configFile = null;
		try {
			if (!(getUserSpecifiedDirectory().equals("none")) ) {
				pkcs11Path.append( getUserSpecifiedDirectory() );
				if ((isDirectory( pkcs11Path.toString() ))) { 
					setDirectory(pkcs11Path.toString());
				}
			} else {
				pkcs11Path = new StringBuilder();
				if (System.getenv("PKCS11_HOME") != null ) {
					pkcs11Path = new StringBuilder();
					pkcs11Path.append(System.getenv("PKCS11_HOME"));
					DebugLogger.printDebug("Pkcs11MSlocation --  Setting pkcs11 loaction from PKCS11_HOME:"+ pkcs11Path.toString() );
					if ( isDirectory( pkcs11Path.toString() )) {
						configFile = Pkcs11FixConfigFile.getCfgInstance(pkcs11Path.toString());
						setDirectory(configFile.getCfgFilePath());
					} else {
						setDirectory(null);
					}
				} 
				if (getDirectory() == null) {
					pkcs11Path = new StringBuilder();
					pkcs11Path.append(dir64bit);
					if (!(isDirectory( pkcs11Path.toString() ))) { 					
						pkcs11Path = new StringBuilder();
						pkcs11Path.append(dir32bit);
						if ((isDirectory(pkcs11Path.toString()))) { 
							DebugLogger.printDebug("Pkcs11MSlocation --   64bit  FOUND LOACTION:"+pkcs11Path.toString());
							configFile = Pkcs11FixConfigFile.getCfgInstance(pkcs11Path.toString());
							setDirectory(configFile.getCfgFilePath());
						}
					} else {
						DebugLogger.printDebug("Pkcs11MSlocation --   32bit  FOUND LOACTION:"+pkcs11Path.toString());
						configFile = Pkcs11FixConfigFile.getCfgInstance(pkcs11Path.toString());
						setDirectory(configFile.getCfgFilePath());
					}
				}
			}
			DebugLogger.printDebug("Pkcs11MSlocation --   LOCATION:"+ pkcs11Path.toString() );
			if ( getDirectory() != null ) {
				if (!( isPath( pkcs11Path ))) {
					setFound(false);
				} else {
					setFound(true);
					setPath( Paths.get( getDirectory()) );
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Pkcs11InstalledLocation --- MAIN");
		Pkcs11MSLocation testing = new  Pkcs11MSLocation();
		
	}
}
