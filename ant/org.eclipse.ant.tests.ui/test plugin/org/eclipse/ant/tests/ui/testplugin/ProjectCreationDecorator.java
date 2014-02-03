/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.testplugin;



/**
 * Utility class
 */
public class ProjectCreationDecorator {
	
	private static boolean fgIsJ2SE15Compatible = false ;
	
	static {
		String version = System.getProperty("java.specification.version");
		if (version != null) {
			String[] nums = version.split("\\.");
			if (nums.length == 2) {
				try {
					int major = Integer.parseInt(nums[0]);
					int minor = Integer.parseInt(nums[1]);
					if (major >= 1) {
						if (minor >= 5) {
							fgIsJ2SE15Compatible = true;
						}
					}
				} catch (NumberFormatException e) {
				}
			}
		}
	};
	
	/**
	 * @return if the system property "java.specification.version" is 1.5 or greater
	 */
	public static boolean isJ2SE15Compatible() {
		return fgIsJ2SE15Compatible;
	}
}
