package org.eclipse.core.pki;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.ArrayList;


public class CreateCFGfile {
	
	
	private static final String charsetName = "UTF-8";
	private static final String line1="name = Vendor Open Source ";
	private static final String bit64DLL = "opensc-pkcs11.dll";
	private static final String line3="description = ISC_OpenSC";
	private static final String line4 = "slot = 1";
	private static final String line5 = "attributes = compatibility";
	public static String initialize( String Pkcs11cfgHome ) {
		StringBuffer sb = new StringBuffer();
		
		try {
			DebugLogger.printDebug("CreateCFGfile  ----   Pkcs11cfgHome:"+ Pkcs11cfgHome);
			Charset charset = Charset.forName(charsetName);
			sb.append(System.getProperty("user.home"));
			sb.append(FileSystems.getDefault().getSeparator());
			sb.append(".cspid");
			sb.append(FileSystems.getDefault().getSeparator());
			Path cspidDir = Paths.get (sb.toString() );
			Files.createDirectories( cspidDir );
			sb.append("java_pkcs11.cfg");
			DebugLogger.printDebug("CreateCFGfile  ----   NEW CFG FILE::"+ sb.toString() );
			Path path = Paths.get (sb.toString() );
			
			if (!(path.toFile().exists() )) {
				Files.deleteIfExists(path);
				Files.createFile(path);
				ArrayList<String> a = fileContents(Pkcs11cfgHome);
				Files.write( path, a, charset, StandardOpenOption.TRUNCATE_EXISTING );
			}
	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}
	private static ArrayList<String> fileContents(String lib) {
		ArrayList<String>a = new ArrayList<String>();
		
		a.add(line1);
		a.add(getLibraryLocation(lib));
		a.add(line3);
		a.add(line4);
		a.add(line5);
		
		return a;
	}
	private static String getLibraryLocation(String libraryLocation ) {
		StringBuffer sb = new StringBuffer();
		sb.append(libraryLocation);
		sb.append(File.separator);
		sb.append(File.separator);
		sb.append(bit64DLL);
		Object[] args = {sb.toString() };
		String lib = "library = {0}";
		MessageFormat mf = new MessageFormat(lib);
		String location = mf.format(args);
		return location;
	}
}
