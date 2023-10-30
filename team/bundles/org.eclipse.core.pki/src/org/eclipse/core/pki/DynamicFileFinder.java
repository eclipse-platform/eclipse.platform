package org.eclipse.core.pki;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import secure.eclipse.authentication.provider.debug.DebugLogger;

public class DynamicFileFinder extends SimpleFileVisitor <Path>  {
	private boolean found=false;
	private String location= null;
	private ArrayList list = new ArrayList();
	private PathMatcher pathMatcher;
	private String pattern = "glob:[s][u][n]*[1][1].jar" ;
	public 	DynamicFileFinder(Path path) {
		pathMatcher = FileSystems.getDefault().getPathMatcher ( pattern );
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {	
		Path name = file.getFileName();
		if ( pathMatcher.matches(name)) {
			DebugLogger.printDebug("FILE:"+ name.toString());
			DebugLogger.printDebug("path:"+ file.toString());
			list.add( file );
			location = file.toString();
			found=true;
		}
		DebugLogger.printDebug("NOT A MATCH FILE:"+ name.toString());
		return FileVisitResult.CONTINUE;
	}
	@Override
	public FileVisitResult visitFileFailed( Path file, IOException e) {
		return FileVisitResult.SKIP_SUBTREE;
	}
	@Override
	public FileVisitResult preVisitDirectory( Path dir,  BasicFileAttributes attr) {
		DebugLogger.printDebug("SKIPPING dir:"+ dir.toString());
		Path name = dir.getFileName();
		if ( pathMatcher.matches(name)) {
			DebugLogger.printDebug("DIR FILE:"+ name.toString());
			DebugLogger.printDebug("DIR path:"+ dir.toString());
		}
		DebugLogger.printDebug("   DIR   NOT A MATCH FILE:"+ name.toString());
		return FileVisitResult.CONTINUE;
	}
	public boolean isFound() {
		return found;
	}
	public String getLocation() {
		return location;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
		pathMatcher = FileSystems.getDefault().getPathMatcher ( this.pattern );
	}

	public ArrayList getList() {
		return list;
	}

	public void setList(ArrayList list) {
		this.list = list;
	}
	
}
