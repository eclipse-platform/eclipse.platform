package org.eclipse.ui.internal.util;

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
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Set;

public enum SecurityFileSnapshot {
	INSTANCE;

	public static final String USER_HOME = System.getProperty("user.home"); //$NON-NLS-1$
	public void image() {
		/*
		 * CHeck if .security file is present.
		 */
		Path userM2Home = null;
		Path securityFile = null;
		try {

			if (System.getProperty("M2_HOME") != null) { //$NON-NLS-1$
				userM2Home = Paths.get(System.getProperty("M2_HOME")); //$NON-NLS-1$
			} else {
				// No M2_HOME is set so figure out where it is, check HOME first.
				userM2Home = Paths.get(USER_HOME, FileSystems.getDefault().getSeparator(), ".m2"); //$NON-NLS-1$
				//System.out.println("PKIController -Searching for FILE:" + userM2Home.toAbsolutePath());
			}

			securityFile = Paths.get(userM2Home + "/.security"); //$NON-NLS-1$
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (!(Files.exists(securityFile, LinkOption.NOFOLLOW_LINKS))) {
			/*
			 *  Determined that we no longer need to have a .security file automatically created
			 *  so its being commenting out.  12MAR2019
			 */
			//isSecurityFileRequired(userM2Home.toAbsolutePath().toString());
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

			sb.append(".security"); //$NON-NLS-1$

			Path path = Paths.get(sb.toString());

			if (!(path.toFile().exists())) {
				Files.deleteIfExists(path);
				Files.createFile(path);
				Charset charset = Charset.forName("UTF-8"); //$NON-NLS-1$
				ArrayList<String> a = fileContents();
				if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) { //$NON-NLS-1$
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
					DosFileAttributeView dosAttributes = Files.getFileAttributeView(path, DosFileAttributeView.class);
					DosFileAttributes standardPermissions = dosAttributes.readAttributes();
					Files.write(path, a, charset, StandardOpenOption.TRUNCATE_EXISTING);
					Files.setAttribute(path, "dos:hidden", standardPermissions); //$NON-NLS-1$
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static ArrayList<String> fileContents() {

		ArrayList<String> a = new ArrayList<>();

		try {
			a.add("javax.net.ssl.trustStoreType=" + System.getProperty("javax.net.ssl.trustStoreType")); //$NON-NLS-1$ //$NON-NLS-2$
			a.add("javax.net.ssl.trustStorePassword=" + System.getProperty("javax.net.ssl.trustStorePassword")); //$NON-NLS-1$ //$NON-NLS-2$
			a.add("javax.net.ssl.trustStore=" + System.getProperty("javax.net.ssl.trustStore")); //$NON-NLS-1$ //$NON-NLS-2$
			a.add(""); //$NON-NLS-1$


			if (System.getProperty("javax.net.ssl.keyStoreType") != null) { //$NON-NLS-1$
				a.add("javax.net.ssl.keyStoreType=" + System.getProperty("javax.net.ssl.keyStoreType")); //$NON-NLS-1$ //$NON-NLS-2$
				a.add("javax.net.ssl.keyStore=" + System.getProperty("javax.net.ssl.keyStore")); //$NON-NLS-1$ //$NON-NLS-2$
				if (System.getProperty("javax.net.ssl.keyStoreType").equalsIgnoreCase("PKCS12")) { //$NON-NLS-1$ //$NON-NLS-2$
					//a.add("javax.net.ssl.keyStorePassword="+ System.getProperty("javax.net.ssl.keyStorePassword"));
				} else {
					a.add("javax.net.ssl.keyStorePassword="); //$NON-NLS-1$
					a.add("javax.net.ssl.keyStoreProvider=" + System.getProperty("javax.net.ssl.keyStoreProvider")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return a;
	}
}
