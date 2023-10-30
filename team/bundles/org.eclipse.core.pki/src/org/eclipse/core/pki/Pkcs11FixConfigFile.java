package org.eclipse.core.pki;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import secure.eclipse.authentication.provider.debug.DebugLogger;

public class Pkcs11FixConfigFile {
	private static final String programFiles="Program Files (x86)";
	private static final CharSequence slash=(CharSequence) "/";
	private static final CharSequence none=(CharSequence) "";
	private static final CharSequence quotes=(CharSequence) "\"";
	private static final CharSequence sslash=(CharSequence) "\\";
	private static final CharSequence bslash=(CharSequence) "\\\\";
	private static final String biT32="cspid.dll";
	private static final String biT64="cspidx64.dll";
	private String cfgFilePath=null;
	private static String cspidHome;
	private static Pkcs11FixConfigFile configFile=null;
	private static StringBuffer sb = new StringBuffer();
	public static Pkcs11FixConfigFile getCfgInstance(String fileLocation) {	
		if ( configFile == null ) {
			synchronized(Pkcs11FixConfigFile.class) {
				if ( configFile == null ) {
					configFile = new Pkcs11FixConfigFile();
					cspidHome = fileLocation;
					DebugLogger.printDebug("CSPidFixConfigFile --  incoming path:"+ fileLocation);
					sb.append(fileLocation);
					initialize();
				}
			}
		}
		return configFile; 
	}
	public static void initialize() {
		try {
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append("java_pkcs11.cfg");
			Path path = openFile( sb.toString());
			Files.setAttribute(path, "dos:readonly", false);
			List<String> list = readFile(path);
			List<String> edit = editFile(list);
			Path outputPath = setOutputDirectory();
			saveFile(edit, outputPath);
			//listEditedFile(edit);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static Path setOutputDirectory() {
		Path path = null;
		StringBuilder sb = new StringBuilder();
		try {
			String appData = System.getenv("AppData");
			appData = Paths.get(appData).toAbsolutePath().toString().replace(sslash, bslash);
			if ( appData != null ) {
				sb.append(appData);
			} else {
				sb.append(System.getProperty("user.dir"));
			}
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append("cspid");
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append("java_pkcs11.cfg");
			
			path = Paths.get(sb.toString());
			//setCfgFilePath( sb.toString());
			DebugLogger.printDebug("CSPidFixCOnfigFile ----  cspidHome:"+cspidHome );
			setCfgFilePath( CreateCFGfile.initialize( cspidHome ));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return path;
	}
	public static Path openFile(String s) {
		return  FileSystems.getDefault().getPath(s);
	}
	public static List<String> readFile(Path path) {
		List <String>list = null;
		try {
			list = Files.readAllLines(path, Charset.defaultCharset());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	public static List<String> editFile(List<String> list) {
		ArrayList<String> edit = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		
		for ( String s : list) {
			CharSequence ch = "/";
			
			if (( s.contains("library")) && ( System.getenv("CSPID_HOME") != null))  {
				sb.append("library=");
				sb.append(System.getenv("CSPID_HOME"));
				sb.append(ch);
				sb.append(biT64);
				s=sb.toString();
			}
			
			if (!( s.startsWith("#"))) {
				if (( s.contains(biT32)) && ( System.getProperty("os.arch").contains("64"))) {
					s = s.replaceAll(biT32, biT64);
				}
			}
			if ( s.contains(quotes)) {
				s=s.replace(quotes, none);
			}
			
			if ( s.contains(programFiles)) {
				if ( System.getenv("CSPID_HOME") != null) {
					s = System.getenv("CSPID_HOME");
				} else {
					s = s.replace(programFiles, "Progra~2");
				}
				edit.add(s);
			} else {
				if (!( s.trim().isEmpty() )) {
					edit.add(s);
				}
			}
		}
		return edit;
	}
	public static void saveFile( List<String> list , Path path) {
		try {
			Files.write(path, list, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getCfgFilePath() {
		return cfgFilePath;
	}
	public static void setCfgFilePath(String cfgFilePath) {
		configFile.cfgFilePath = cfgFilePath;
	}
	private static void listEditedFile( List <String> list ) {
		DebugLogger.printDebug("CSPidFixConfigFile ----  ARCH:"+ System.getProperty("os.arch"));
		for ( String s : list) {
			DebugLogger.printDebug("CSPidFixConfigFile ----  edited line:"+ s);
		}
	}
	
}
