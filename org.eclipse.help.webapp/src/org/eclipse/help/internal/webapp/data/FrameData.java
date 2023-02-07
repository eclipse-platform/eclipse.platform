/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/

package org.eclipse.help.internal.webapp.data;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.webapp.AbstractFrame;

public class FrameData extends RequestData {

	private static final String FRAME_EXTENSION_POINT = "org.eclipse.help.webapp.frame"; //$NON-NLS-1$
	private List<AbstractFrame> allFrames;

	public FrameData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);
	}

	public AbstractFrame[] getFrames(int location) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor(FRAME_EXTENSION_POINT);
		if (allFrames == null) {
			allFrames = new ArrayList<>();
			for (IConfigurationElement element : elements) {
				Object obj = null;
				try {
					obj = element.createExecutableExtension("class"); //$NON-NLS-1$
				} catch (CoreException e) {


					Platform.getLog(getClass()).error("Create extension failed:[" //$NON-NLS-1$
							+ FRAME_EXTENSION_POINT + "].", e); //$NON-NLS-1$
				}
				if (obj instanceof AbstractFrame) {
					allFrames.add((AbstractFrame) obj);
				}
			}
			allFrames.sort(null);
		}

		List<AbstractFrame> frameList = new ArrayList<>();
		for (AbstractFrame frame : allFrames) {
			if (frame.isVisible() && frame.getLocation() == location) {
				frameList.add(frame);
			}
		}
		AbstractFrame[] frames = frameList.toArray(new AbstractFrame[frameList.size()]);
		return frames;
	}

	public String getUrl(AbstractFrame frame) {
		return request.getContextPath() + frame.getURL();
	}

	public String getContentAreaFrameSizes() {
		String size = "24,*"; //$NON-NLS-1$
		AbstractFrame[] frames = getFrames(AbstractFrame.BELOW_CONTENT);
		for (AbstractFrame frame : frames) {
			size += ',';
			size += frame.getSize();
		}
		return size;
	}

	/**
	 * Get the additional frame added to the Main Help Toolbar
	 * Considering of the layout and space of Main Help Toolbar, only one extra frame is supported
	 *
	 * @return AbstractFrame or null if not found
	 */
	public AbstractFrame getHelpToolbarFrame() {
		AbstractFrame[] frames = getFrames(AbstractFrame.HELP_TOOLBAR);
		if(frames.length > 0) {
			if(frames.length > 1){
				Platform.getLog(getClass()).warn("Only one extra frame is supported to be added to Help Toolbar. The first reterived element will be used.");  //$NON-NLS-1$
			}
			return frames[0];
		}else {
			return null;
		}
	}

	/**
	 * Get layout(frame sizes) of Main Help Toolbar
	 */
	public String getHelpToolbarFrameSizes() {
		String size = "*"; //$NON-NLS-1$
		AbstractFrame frame = getHelpToolbarFrame();
		if(null != frame) {
			boolean isRTL = UrlUtil.isRTL(request, response);
			if(isRTL) {
				size = frame.getSize() + ", " + size; //$NON-NLS-1$
			}else {
				size =  size + ", " + frame.getSize(); //$NON-NLS-1$
			}
		}
		return size;
	}

}
