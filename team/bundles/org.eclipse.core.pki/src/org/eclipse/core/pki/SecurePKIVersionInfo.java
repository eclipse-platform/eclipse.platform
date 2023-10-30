package org.eclipse.core.pki;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


public enum SecurePKIVersionInfo {
	INSTANCE;
	public static String getVersion() {
		String version = null;
		Path path = null;
		//System.out.println("[GeT Version of this Build]");

		try {

			/*
			 * See if this is a valid path check to see if its eclipse testing
			 */
			path = Paths.get("../securePKI/target//SecurePKI.jar");

			if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
				//System.out.println(" GOOD PATH");
			} else {
				
				try {
					path = Paths.get(AuthenticationBase.class.getProtectionDomain().getCodeSource().getLocation().toURI());
					if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
						DebugLogger.printDebug("SigintPKIVersionInfo -- PATH:"+path.toAbsolutePath());
					} else {
						path = null;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					version = null;
				}
			}

			if (path != null) {
				try {
					//System.out.println("PATH:" + path.toAbsolutePath());
					Manifest manifest = new JarFile(path.toAbsolutePath().toString()).getManifest();
					Attributes attributes = manifest.getMainAttributes();
					version = attributes.getValue("Build-Label");
					DebugLogger.printDebug( " VALUE:"+ version );
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					version = null;
				}
			}

			if (version == null) {
				version = "isEmbeded?";
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return version;
	}
}
