package org.eclipse.core.pki;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;


public class Pkcs11LibraryFinder {
	private static String javaHome=null;

	/*
	 *  32BIT jdk C:\\Progra~2\\Java
	 *  64BIT jdk C:\\Progra~1\\Java
	 */
	private static final String[] cdir = { "C:\\Progra~1\\Java", "C:\\Progra~2\\Java" }; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String usrLocalDir = "/usr/local"; //$NON-NLS-1$
	private String pattern = "glob:[s][u][n]*[1][1].jar"; //$NON-NLS-1$
	private static String jvmRuntime = null;
	private static final CharSequence arch32 = "x86"; //$NON-NLS-1$
	private static final CharSequence arch64 = "64"; //$NON-NLS-1$
	private boolean foundJar=false;
	DynamicFileFinder finder = null;
	private Path jarDirectory=null;


	private static Pkcs11LibraryFinder instance=null;

	//
	// Find the PKCS11 jar in the JDK. This has to be done to be compatible with Java
	// versions after 9 sinced they handle PKCS11 differently
	//
	public static Pkcs11LibraryFinder findSunPkcs11JarInstance() {
		DebugLogger.printDebug("Pkcs11LibraryFinder ------------findSunPkcs11JarInstance"); //$NON-NLS-1$
		if ( instance == null ) {
			synchronized(Pkcs11LocationImpl.class) {
				if ( instance == null ) {
					javaHome = System.getenv("JAVA_HOME"); //$NON-NLS-1$
					if (( javaHome == null ) || ( javaHome.isEmpty()) ){
						javaHome = System.getProperty("java.home"); //$NON-NLS-1$
					}
					jvmRuntime = System.getProperty("java.version"); //$NON-NLS-1$
					instance = new Pkcs11LibraryFinder();
					instance.initialize();
				}
			}
		}
		return instance;
	}


	//
	// find the PKCS11 package, it could be in different places if it's on windows or Linux
	//
	private void initialize() {
		DebugLogger.printDebug("Pkcs11LibraryFinder ------------initialize"); //$NON-NLS-1$
		try {
			Path path = null;
			StringBuffer sb = new StringBuffer();

			//
			// Linux
			//
			if ( isUnix() ) {
				sb = unixLocations();
				path = Paths.get(sb.toString());
				if ( Files.isSymbolicLink(path)) {
					sb = new StringBuffer();
					sb.append(path.toRealPath().toString());
					sb.append(FileSystems.getDefault().getSeparator());
					path = Paths.get(sb.toString());
					if ( Files.isSymbolicLink(path)) {
						sb = new StringBuffer();
						sb.append(path.toRealPath());
					}

				//
				//  find the location of the PKCS11 library
				//
				} else if ( findLocation(sb) ) {
						foundJar=true;
					}

			//
		    // Windows
		    //
			} else {
				try {
					if ( !( isJavaModulesBased() )) {
						//  This must be a java version that REQUIRES the SunPKCS11.jar file.
						windowsLocations();
					}
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private boolean findLocation(StringBuffer sb) {
		boolean found = false;
		ArrayList<Path> dirlist = new ArrayList<>();
		Path path = null;

		path = Paths.get(sb.toString());
		DebugLogger.printDebug("Pkcs11LibraryFinder  incoming PATH:" + path.toString()); //$NON-NLS-1$
		/*
		 * FIRST  FIND THE JDK, then find jar:
		 */

		//
		// add JAVA_HOME to the list of directories to search
		//
		dirlist.add(path);

		//
		// This next part is more for Oracle/Sun JDKs We are going to backup
		// in case we are using a jre to see if a JDK is there also.
		//
		if (path.endsWith("jre")) { //$NON-NLS-1$
			int index = sb.lastIndexOf("jre"); //$NON-NLS-1$

        	if (index > 0 )
        	{
        		Path path2 = Paths.get(sb.substring(0, index));
        		dirlist.add(path2);
				DebugLogger.printDebug("JRE adding " + path2.toString()); //$NON-NLS-1$
        	}
		}

        //
		// Get list of paths from searcher matching. Then loop through them
        // looking for the java lib for PKCS11
        //
		if (!( dirlist.isEmpty())  ) {
			for ( Path p : dirlist)  {
				if (search(p, pattern, Integer.valueOf(4))) {
					if (finder.getLocation().contains(jvmRuntime)) {
						setJarDirectory(Paths.get(finder.getLocation()));
						DebugLogger.printDebug("Pkcs11LibraryFinder  FOUND JAR in JVMRuntime"); //$NON-NLS-1$
						return true;
					} else {
						found = true;
						setJarDirectory(Paths.get(finder.getLocation()));
					}
				}
			}
		}

		return found;
	}



	private StringBuffer windowsLocations() {
		StringBuffer sb = new StringBuffer();
		ArrayList <String>list = new ArrayList<>();

		String jvmBitness = System.getProperty("os.arch"); //$NON-NLS-1$

		if ( jvmBitness != null ) {
			if ( jvmBitness.contains(arch64)) {
				list.add(cdir[0]);
			} else if ( jvmBitness.contains(arch32) ) {
			/*
			 *  A 32bit sunpkcs11.jar WILL NOT work with a 64BIT JVM.
			 */
				list.add(cdir[1]);
			}
		}


		if ((javaHome != null)) {
			if ( Files.exists(Paths.get(javaHome))) {
				list.add(javaHome);
			}
		}

		for ( String dir : list ) {
			sb = new StringBuffer();
			sb.append(dir);
			DebugLogger.printDebug(" Pkcs11LibraryFinder   Searching:" + sb.toString()); //$NON-NLS-1$
			if ( findLocation(sb) ) {
				foundJar=true;
			}
		}

		if ( !(foundJar) ) {
			DebugLogger
					.printDebug(" Pkcs11LibraryFinder --  Could not locate a sunpkcs11.jar in JDK or JRE, Trying P12"); //$NON-NLS-1$
		}

		return sb;
	}

	//
	// get the location of the Java, lets hope it's a JDK
	//
	private StringBuffer unixLocations() {
		StringBuffer sb = new StringBuffer();
		javaHome = System.getProperty("java.home"); //$NON-NLS-1$
		if ( javaHome != null) {
			sb.append(javaHome);
		} else {
			sb.append(usrLocalDir);
		}

		return sb;
	}

	//
	// Determine if we are on Linux
	//
	private static boolean isUnix() {
		for ( Path path : FileSystems.getDefault().getRootDirectories()) {
			if (path.startsWith(FileSystems.getDefault().getSeparator())) {
				return true;
			}
		}
		return false;
	}

	//
	// searches a path for the PKCS11 jar
	//
	public boolean search(Path path, String pattern, Integer depth) {
		boolean found = false;
		try {
			EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
			finder = new DynamicFileFinder(path);
			finder.setPattern(pattern);
			Files.walkFileTree(path, opts, depth.intValue(), finder);
			if ( finder.isFound()) {
				found = true;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return found;
	}


	public boolean isPkcs11() {
		return foundJar;
	}


	public Path getJarDirectory() {
		return this.jarDirectory;
	}


	public void setJarDirectory(Path directory) {
		this.jarDirectory = directory;
	}


	public boolean isJavaModulesBased() {
		try {
			Class.forName("java.lang.Module"); //$NON-NLS-1$
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
