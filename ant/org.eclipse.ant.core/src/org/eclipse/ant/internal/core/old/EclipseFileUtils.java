package org.eclipse.ant.internal.core.old;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.util.FileUtils;
import org.eclipse.ant.core.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
/**
 * Subclasses FileUtiles from Ant to extend its
 * funtionalities.
 */
public class EclipseFileUtils extends FileUtils {

	/** Indicates the default string encoding on this platform */
	private static String defaultEncoding = new java.io.InputStreamReader(new java.io.ByteArrayInputStream(new byte[0])).getEncoding();

	/** instance of this library */
	private static final String LIBRARY_NAME = "core_2_0_5";
	private static boolean hasNatives = false;
	
	static {
		try {
			System.loadLibrary(LIBRARY_NAME);
			hasNatives = true;
		} catch (UnsatisfiedLinkError e) {
			logMissingNativeLibrary(e);
		}
	}
	
public void copyFile(File sourceFile, File destFile, FilterSetCollection filters, boolean overwrite, boolean preserveLastModified) throws IOException {
	super.copyFile(sourceFile, destFile, filters, overwrite, preserveLastModified);
	if (hasNatives)
		internalCopyAttributes(toPlatformBytes(sourceFile.getAbsolutePath()), toPlatformBytes(destFile.getAbsolutePath()), preserveLastModified);
}
/**
 * Copies file attributes from source to destination. The copyLastModified attribute
 * indicates whether the lastModified attribute should be copied.
 */
protected static boolean copyAttributes(String source, String destination, boolean copyLastModified) {
	if (hasNatives)
		return internalCopyAttributes(toPlatformBytes(source), toPlatformBytes(destination), false);
	return false; // not supported
}
/**
 * Copies file attributes from source to destination. The copyLastModified attribute
 * indicates whether the lastModified attribute should be copied.
 */
private static final native boolean internalCopyAttributes(byte[] source, byte[] destination, boolean copyLastModified);

private static void logMissingNativeLibrary(UnsatisfiedLinkError e) {
	String libName = System.mapLibraryName(LIBRARY_NAME);
	String message = Policy.bind("info.couldNotLoadLibrary", libName);
	IStatus status = new Status(IStatus.INFO, AntPlugin.PI_ANT, IStatus.INFO, message, e);
	AntPlugin.getPlugin().getLog().log(status);
}
/**
 * Calling String.getBytes() creates a new encoding object and other garbage.
 * This can be avoided by calling String.getBytes(String encoding) instead.
 */
protected static byte[] toPlatformBytes(String target) {
	if (defaultEncoding == null)
		return target.getBytes();
	// try to use the default encoding
	try {
		return target.getBytes(defaultEncoding);
	} catch (UnsupportedEncodingException e) {
		// null the default encoding so we don't try it again
		defaultEncoding = null;
		return target.getBytes();
	}
}
}