/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;


/**
 * Default implementation, assigns random colors to revisions based on committer id.
 * 
 * @since 3.2
 */
final class CommitterColors {
	
	private static CommitterColors fInstance;
	
	// Fixed committer color RGBs provided by the UI Designer
	private static final RGB[] COMMITTER_RGBs= new RGB[] {
		new RGB(131, 150, 98), new RGB(221, 205, 93), new RGB(199, 134, 57), new RGB(133, 166, 214), new RGB(197, 123, 127),
		new RGB(139, 136, 140), new RGB(48, 135, 144), new RGB(190, 93, 66), new RGB(143, 163, 54), new RGB(180, 148, 74),
		new RGB(101, 101, 217), new RGB(72, 153, 119), new RGB(23, 101, 160), new RGB(132, 164, 118),
		
		new RGB(255, 230, 59), new RGB(136, 176, 70), new RGB(255, 138, 1), new RGB(123, 187, 95), new RGB(233, 88, 98),
		new RGB(93, 158, 254), new RGB(175, 215, 0), new RGB(140, 134, 142), new RGB(232, 168, 21), new RGB(0, 172, 191),
		new RGB(251, 58, 4), new RGB(63, 64, 255), new RGB(27, 194, 130), new RGB(0, 104, 183)
	};  
	

	/**
	 * Returns the committer color singleton.
	 * 
	 * @return the committer color singleton
	 */
	public static CommitterColors getDefault() {
		if (fInstance == null)
			fInstance= new CommitterColors();
		return fInstance;
	}

	/** The color map. */
	private Map fColors= new HashMap();

	/** The number of colors that have been issued. */
	private int fCount= 0;

	private CommitterColors() {
	}

	/**
	 * Returns a unique color description for each string passed in. Colors for new committers are
	 * allocated to be as different as possible from the existing colors.
	 * 
	 * @param committer the committers unique name
	 * @return the corresponding color
	 */
	public RGB getCommitterRGB(String committer) {
		RGB rgb= (RGB) fColors.get(committer);
		if (rgb == null) {
			rgb= COMMITTER_RGBs[fCount++ % COMMITTER_RGBs.length];
			fColors.put(committer, rgb);
		}
		return rgb;
	}

}
