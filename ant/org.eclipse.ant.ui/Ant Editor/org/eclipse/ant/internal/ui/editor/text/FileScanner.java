/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
package org.eclipse.ant.internal.ui.editor.text;

import org.apache.tools.ant.DirectoryScanner;


public class FileScanner extends DirectoryScanner {
	
	public String[] getExcludesPatterns() {
		return excludes;
	}
	
	public String[] getIncludePatterns() {
		return includes;
	}
}
