package org.eclipse.pki.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Set;

public enum SecurityFileSnapshot {
	INSTANCE;
	public static final String USER_HOME = System.getProperty("user.home");
	public void image() {
		/*
		 * CHeck if .pki file is present.
		 */
		Path userM2Home = null;
		Path pkiFile = null;
		try {

			if (System.getProperty("M2_HOME") != null) {
				userM2Home = Paths.get(System.getProperty("M2_HOME"));
			} else {
				// No M2_HOME is set so figure out where it is, check HOME first.
				userM2Home = Paths.get(USER_HOME, FileSystems.getDefault().getSeparator(), ".m2");
				//System.out.println("PKIController -Searching for FILE:" + userM2Home.toAbsolutePath());
			}

			pkiFile = Paths.get(userM2Home + "/.pki");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (!(Files.exists(pkiFile, LinkOption.NOFOLLOW_LINKS))) {
			/*
			 *  Determined that we no longer need to have a .pki file automatically created
			 *  so its being commenting out.  12MAR2019
			 */
			//ispkiFileRequired(userM2Home.toAbsolutePath().toString());
		} 
	}

	@SuppressWarnings("unused")
	private static void isSecurityFileRequired(String securityFileLocation) {
		Path dir = null;
		StringBuilder sb = new StringBuilder();

		try {
			sb.append(securityFileLocation);
			sb.append(FileSystems.getDefault().getSeparator());
			//sb.append("TESTDIR"); // testing
			//sb.append(FileSystems.getDefault().getSeparator());
			dir = Paths.get(sb.toString());
			Files.createDirectories(dir);

			sb.append(".security");

			Path path = Paths.get(sb.toString());

			if (!(path.toFile().exists())) {
				Files.deleteIfExists(path);
				Files.createFile(path);
				Charset charset = Charset.forName("UTF-8");
				ArrayList<String> a = fileContents();	
				if ( FileSystems.getDefault().supportedFileAttributeViews().contains("posix") ) {
					PosixFileAttributeView posixAttributes = Files.getFileAttributeView(path, PosixFileAttributeView.class);
					Set<PosixFilePermission> permissions = posixAttributes.readAttributes().permissions();
					permissions.remove(PosixFilePermission.GROUP_READ);
					posixAttributes.setPermissions(permissions);
					Files.write(path, a, charset, StandardOpenOption.TRUNCATE_EXISTING);
					//ls
					permissions.remove(PosixFilePermission.OWNER_WRITE);
					posixAttributes.setPermissions(permissions);
				} else {
					//Windoerz
					//DosFileAttributeView dosAttributes =  Files.getFileAttributeView(path, DosFileAttributeView.class);
					//DosFileAttributes standardPermissions = dosAttributes.readAttributes();
					Files.write(path, a, charset, StandardOpenOption.TRUNCATE_EXISTING);
					Files.setAttribute(path, "dos:hidden", true);
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static ArrayList<String> fileContents() {
		
		ArrayList<String> a = new ArrayList<String>();
		
		try {
			a.add("javax.net.ssl.trustStoreType="+ System.getProperty("javax.net.ssl.trustStoreType"));
			a.add("javax.net.ssl.trustStorePassword="+ System.getProperty("javax.net.ssl.trustStorePassword"));
			a.add("javax.net.ssl.trustStore="+ System.getProperty("javax.net.ssl.trustStore"));
			a.add("");
			
			
			if (System.getProperty("javax.net.ssl.keyStoreType") != null ) {
				a.add("javax.net.ssl.keyStoreType="+ System.getProperty("javax.net.ssl.keyStoreType"));
				a.add("javax.net.ssl.keyStore="+ System.getProperty("javax.net.ssl.keyStore"));
				if (System.getProperty("javax.net.ssl.keyStoreType").equalsIgnoreCase("PKCS12")) {
					//a.add("javax.net.ssl.keyStorePassword="+ System.getProperty("javax.net.ssl.keyStorePassword"));
				} else {
					a.add("javax.net.ssl.keyStorePassword=");
					a.add("javax.net.ssl.keyStoreProvider="+ System.getProperty("javax.net.ssl.keyStoreProvider"));
				}
			}
			
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		return a;
	}
}