package org.eclipse.core.pki;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
/*
 *   CSPidLocationimpl -  A best attempt at finding a usable cspid configuration file.
 *   In version 4 of cspid the cfg file call java_pkcs11.cfg contains a reference to the
 *   location of the dll files used to provide cspid manager and pkcs11 interoperability.
 * 	 There is a bug in the version 4.x cspid cfg file and its loading by the sunpkcs11.jar file
 *   where by the file contents cannot be correctly parsed due to spaces and parenthesis inside
 *   the path names.   
 *   WORK AROUND:   copy the java_pkcs11.cfg file into the users appdata dir
 *   location. and during copy edit data as follows;, i.e.  "Program Files (x86) to "Progra~2"  ..  
 * 
 */

import secure.eclipse.authentication.provider.debug.DebugLogger;


public class Pkcs11LocationImpl extends Pkcs11LibraryFinder implements Pkcs11LocationIfc {
	private static String directory=null;
	private static Path path =null;
	private static Path jarDir = null;
	protected static boolean found = false;
	protected static boolean pkcs11Found = false;
	private static Pkcs11LocationIfc location = null;
	static Pkcs11LibraryFinder jarLocation = null;
	public static Pkcs11LocationImpl getPkcs11LocationInstance() {
		
		if ( location == null ) {
			synchronized(Pkcs11LocationImpl.class) {
				if ( location == null ) {
					jarLocation = findSunPkcs11JarInstance();
					if (jarLocation.isPkcs11() ) {
						pkcs11Found=true;
						DebugLogger.printDebug("CSPidLocationimp jarDIR:"+ jarLocation.getJarDirectory().toString());
						setJarDir( Paths.get(jarLocation.getJarDirectory().toString()) );
					}
					if ( isUnix() ) {	
						location = Pkcs11UnixLocation.getInstance();
					} else { 
						location = Pkcs11MSLocation.getInstance();
					}
				}
			}
		}
		return (Pkcs11LocationImpl) location; 
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CSPidLocationimpl  OVERRIDE INITIALIZE");
		
	}
	private static boolean isUnix() {
		for ( Path path : FileSystems.getDefault().getRootDirectories()) {
			if (path.startsWith(FileSystems.getDefault().getSeparator())) {
				return true;
			}
		}
		return false;
	}
	
	protected static boolean isPath( StringBuilder cspidPath ) {
		
		try {
			if (!(cspidPath.toString().endsWith("java_pkcs11.cfg"))) {
				cspidPath.append(FileSystems.getDefault().getSeparator());
				cspidPath.append("java_pkcs11.cfg");
			}
			if (isDirectory(cspidPath.toString())) {
				setPath(Paths.get(cspidPath.toString()));
				setFound(true);
				return true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	protected static boolean isDirectory(String incoming) {
		try {
			Path path = Paths.get( incoming );
			if (Files.exists(path)) {
				return true;
			} 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	public static String getDirectory() {
		return directory;
	}
	public static void setDirectory(String directory) {
		Pkcs11LocationImpl.directory = directory;
	}
	public static Path getPath() {
		return path;
	}
	public static void setPath(Path path) {
		Pkcs11LocationImpl.path = path;
	}
	
	public static Path getJarDir() {
		return jarDir;
	}

	public static void setJarDir(Path jarDir) {
		Pkcs11LocationImpl.jarDir = jarDir;
	}

	public static void setFound(boolean found) {
		Pkcs11LocationImpl.found = found;
	}
	public static boolean isFound() {
		return found;
	}
	public static boolean isPkcs11Found() {
		return pkcs11Found;
	}
}
