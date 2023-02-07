/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.ui.internal.cheatsheets.composite.explorer;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskStateUtilities;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ICompositeCheatsheetTags;
import org.eclipse.ui.internal.cheatsheets.composite.views.TaskEditorManager;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;
import org.osgi.framework.Bundle;

public class TreeLabelProvider extends LabelProvider {

	private static int BLOCKED = -1;
	private Image defaultImage = null; // Image for tasks with null kind

	/*
	 * A set of related images
	 */
	private static class ImageSet {
		// Use a map rather than array so the nuber of icons is not hard coded
		Map<String, Image> images = new HashMap<>();

		public void put(int index, Image image) {
			images.put(Integer.toString(index), image);
		}

		public Image getImage(int index) {
			return images.get(Integer.toString(index));
		}

		void dispose() {
			for (Image nextImage : images.values()) {
				nextImage.dispose();
			}
		}
	}

	private Map<String, ImageSet> imageMap = null; // each entry is an ImageSet

	public TreeLabelProvider() {
		imageMap = new HashMap<>();
	}

	@Override
	public String getText(Object obj) {
		String result;
		if (obj instanceof ICompositeCheatSheetTask) {
			result =  ((ICompositeCheatSheetTask) obj).getName();
		} else {
			result =  obj.toString();
		}
		if (result == null) {
			result = ""; //$NON-NLS-1$
		}
		return result;
	}

	@Override
	public Image getImage(Object obj) {
		if (obj instanceof ICompositeCheatSheetTask) {
			ICompositeCheatSheetTask task = (ICompositeCheatSheetTask) obj;
			return lookupImage(task.getKind(), task.getState(), TaskStateUtilities.isBlocked(task));
		}
		return super.getImage(obj);
	}

	public Image lookupImage(String kind, int state, boolean isBlocked) {
		ImageSet images = imageMap.get(kind);
		if (images == null) {
			images = createImages(kind);
			imageMap.put(kind, images);
		}
		if (isBlocked) {
			return images.getImage(BLOCKED);
		}
		return images.getImage(state);
	}

	/**
	 * Create a set of images for a task which may be [redefined.
	 * @param kind
	 * @return
	 */
	private ImageSet createImages(String kind) {
		ImageSet images = new ImageSet();
		ImageDescriptor desc = getPredefinedImageDescriptor(kind, true);
		if (desc == null) {
			desc = TaskEditorManager.getInstance().getImageDescriptor(kind);
		}
		if (desc != null) {
			Image baseImage = desc.createImage();
			images.put(ICompositeCheatSheetTask.NOT_STARTED, baseImage);

			createImageWithOverlay(ICompositeCheatSheetTask.IN_PROGRESS,
					"$nl$/icons/ovr16/task_in_progress.png", //$NON-NLS-1$
					images,
					desc);
			createImageWithOverlay(ICompositeCheatSheetTask.SKIPPED,
					"$nl$/icons/ovr16/task_skipped.png", //$NON-NLS-1$
					images,
					desc);
			createDisabledImage(kind, BLOCKED,
					images,
					baseImage);
			createImageWithOverlay(ICompositeCheatSheetTask.COMPLETED,
					"$nl$/icons/ovr16/task_complete.png", //$NON-NLS-1$
					images,
					desc);

		}
		return images;
	}

	private ImageDescriptor getPredefinedImageDescriptor(String kind, boolean isEnabled) {
		String filename;
		if (ICompositeCheatsheetTags.CHEATSHEET_TASK_KIND.equals(kind)) {
			filename = "cheatsheet_task.png"; //$NON-NLS-1$
		} else if (ITaskGroup.SET.equals(kind)) {
			filename = "task_set.png"; //$NON-NLS-1$
		} else if (ITaskGroup.CHOICE.equals(kind)) {
			filename = "task_choice.png"; //$NON-NLS-1$
		} else if (ITaskGroup.SEQUENCE.equals(kind)) {
			filename = "task_sequence.png"; //$NON-NLS-1$
		} else {
			return null;
		}
		String iconPath =  "$nl$/icons/"; //$NON-NLS-1$
		if (isEnabled) {
			iconPath += CheatSheetPlugin.T_OBJ;
		} else {
			iconPath += CheatSheetPlugin.T_DLCL;
		}
		iconPath += filename;
		return createImageDescriptor(iconPath);
	}

	private void createImageWithOverlay(int state, String imagePath, ImageSet images, ImageDescriptor baseDescriptor) {
		ImageDescriptor descriptor = createImageDescriptor(imagePath);
		OverlayIcon icon = new OverlayIcon(baseDescriptor, new ImageDescriptor[][] {
				{}, { descriptor } });
		images.put(state, icon.createImage());
	}

	private void createDisabledImage(String kind, int state, ImageSet images, Image baseImage) {
		// The four images for task_set, task_sequence, task_choice and cheatsheet_task can be found
		// in icons/dlcl16.
		// TODO extend the extension point to allow disabled images to be specified.
		//if

		ImageDescriptor desc = getPredefinedImageDescriptor(kind, false);
		Image disabledImage;
		if (desc != null) {
			disabledImage = desc.createImage();
		} else {
			disabledImage = createGrayedImage(baseImage);
		}
		images.put(state, disabledImage);
	}

	private Image createGrayedImage(Image image) {
		return new Image(image.getDevice(), image, SWT.IMAGE_DISABLE);
	}

	private ImageDescriptor createImageDescriptor(String relativePath) {
		Bundle bundle = CheatSheetPlugin.getPlugin().getBundle();
		URL url = FileLocator.find(bundle, new Path(relativePath),null);
		if (url == null) return null;
		try {
			url = FileLocator.resolve(url);
			return ImageDescriptor.createFromURL(url);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public void dispose() {
		if (imageMap != null) {
			for (ImageSet nextImages : imageMap.values()) {
				nextImages.dispose();
			}
			imageMap = null;
		}
		if (defaultImage != null) {
			defaultImage.dispose();
			defaultImage = null;
		}
	}

}
