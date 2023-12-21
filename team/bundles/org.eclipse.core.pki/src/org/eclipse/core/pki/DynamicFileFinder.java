/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.pki;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;


public class DynamicFileFinder extends SimpleFileVisitor <Path>  {
	private boolean found=false;
	private String location= null;
	private ArrayList<Path> list = new ArrayList<>();
	private PathMatcher pathMatcher;
	private String pattern = "glob:[s][u][n]*[1][1].jar"; //$NON-NLS-1$
	public 	DynamicFileFinder(Path path) {
		pathMatcher = FileSystems.getDefault().getPathMatcher ( pattern );
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
		Path name = file.getFileName();
		if ( pathMatcher.matches(name)) {
			DebugLogger.printDebug("FILE:" + name.toString()); //$NON-NLS-1$
			DebugLogger.printDebug("path:" + file.toString()); //$NON-NLS-1$
			list.add( file );
			location = file.toString();
			found=true;
		}
		DebugLogger.printDebug("NOT A MATCH FILE:" + name.toString()); //$NON-NLS-1$
		return FileVisitResult.CONTINUE;
	}
	@Override
	public FileVisitResult visitFileFailed( Path file, IOException e) {
		return FileVisitResult.SKIP_SUBTREE;
	}
	@Override
	public FileVisitResult preVisitDirectory( Path dir,  BasicFileAttributes attr) {
		DebugLogger.printDebug("SKIPPING dir:" + dir.toString()); //$NON-NLS-1$
		Path name = dir.getFileName();
		if ( pathMatcher.matches(name)) {
			DebugLogger.printDebug("DIR FILE:" + name.toString()); //$NON-NLS-1$
			DebugLogger.printDebug("DIR path:" + dir.toString()); //$NON-NLS-1$
		}
		DebugLogger.printDebug("   DIR   NOT A MATCH FILE:" + name.toString()); //$NON-NLS-1$
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

	public ArrayList<Path> getList() {
		return list;
	}

	public void setList(ArrayList<Path> list) {
		this.list = list;
	}

}
