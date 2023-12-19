package org.eclipse.core.pki;

public class Pkcs11Location extends Pkcs11LocationImpl  {
	private static String userSpecifiedDirectory = "none"; //$NON-NLS-1$
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
}
