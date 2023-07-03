/*******************************************************************************
 *  Copyright (c) 2002, 2019 IBM Corporation and others.
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
 *     Christoph Läubrich - Bug 552773 - Simplify logging in platform code base
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class for cheat sheets.
 */

public class CheatSheetPlugin extends AbstractUIPlugin {

	public final static String PLUGIN_ID = "org.eclipse.help.base"; //$NON-NLS-1$

	//The shared instance of this plugin.
	static CheatSheetPlugin plugin;

	//Resource bundle.
	//private boolean resourceBundleInitialized = false;
	//private ResourceBundle resourceBundle;
	private CheatSheetHistory history = null;
	private DocumentBuilder documentBuilder = null;

	private static final String HISTORY_FILENAME = "history.xml"; //$NON-NLS-1$
	private static final String MEMENTO_TAG_CHEATSHEET = "cheatsheet"; //$NON-NLS-1$
	private static final String MEMENTO_TAG_VERSION = "version"; //$NON-NLS-1$
	private static final String VERSION_STRING[] = { "0.0", "3.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String MEMENTO_TAG_CHEATSHEET_HISTORY = "cheatsheetHistory"; //$NON-NLS-1$

	public static final IPath ICONS_PATH = IPath.fromOSString("$nl$/icons/"); //$NON-NLS-1$
	public static final String T_OBJ = "obj16/"; //$NON-NLS-1$
	public static final String T_ELCL = "elcl16/"; //$NON-NLS-1$
	public static final String T_DLCL = "dlcl16/"; //$NON-NLS-1$
	public static final String T_VIEW = "view16/"; //$NON-NLS-1$

	/**
	 * The constructor.
	 */
	public CheatSheetPlugin() {
		super();
	}

	/**
	 * Returns the shared instance.
	 */
	public static CheatSheetPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Returns the image in Cheat Sheet's image registry with the given key,
	 * or <code>null</code> if none.
	 * Convenience method equivalent to
	 * <pre>
	 * CheatSheetPlugin.getImageRegistry().get(key)
	 * </pre>
	 *
	 * @param key the key
	 * @return the image, or <code>null</code> if none
	 */
	public Image getImage(String key) {
		Image image = getImageRegistry().get(key);
		return image;
	}

	/**
	 * Returns the CheatSheetHistory
	 */
	public CheatSheetHistory getCheatSheetHistory() {
		if (history == null) {
			history = new CheatSheetHistory(CheatSheetRegistryReader.getInstance());
			restoreCheatSheetHistory();
		}
		return history;
	}

	/**
	 * Get a file from the state folder.
	 */
	private File getCheatSheetStateFile(String filename) {
		IPath path = CheatSheetPlugin.getPlugin().getStateLocation();
		path = path.append(filename);
		return path.toFile();
	}

	/**
	 * Returns the DocumentBuilder to be used by the cheat sheets.
	 */
	public DocumentBuilder getDocumentBuilder() {
		if(documentBuilder == null) {
			try {
				documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				documentBuilder.setEntityResolver(new LocalEntityResolver());
			} catch (Exception e) {
				CheatSheetPlugin.getPlugin().getLog().error(Messages.ERROR_CREATING_DOCUMENT_BUILDER, e);
			}
		}
		return documentBuilder;
	}

	/**
	 * Logs an Error message with an exception.
	 */
	public static synchronized void logError(String message, Throwable ex) {
		if (message == null)
			message = ""; //$NON-NLS-1$
		CheatSheetPlugin.getPlugin().getLog().error(message, ex);
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		IPath path = ICONS_PATH.append(T_OBJ).append("cheatsheet_obj.png");//$NON-NLS-1$
		ImageDescriptor imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_OBJ, imageDescriptor);

		path = ICONS_PATH.append(T_OBJ).append("skip_status.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_ITEM_SKIP, imageDescriptor);

		path = ICONS_PATH.append(T_OBJ).append("complete_status.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_ITEM_COMPLETE, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("linkto_help.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_ITEM_HELP, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("start_cheatsheet.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_START, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("restart_cheatsheet.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_RESTART, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("start_task.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_START, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("skip_task.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_SKIP, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("complete_task.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_COMPLETE, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("restart_task.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_RESTART, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("return_to_start.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_RETURN, imageDescriptor);

		path = ICONS_PATH.append(T_OBJ).append("error.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.ERROR, imageDescriptor);

		// Images used by composites

		path = ICONS_PATH.append(T_OBJ).append("composite_obj.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.COMPOSITE_OBJ, imageDescriptor);

		path = ICONS_PATH.append(T_OBJ).append("information.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.INFORMATION, imageDescriptor);

		path = ICONS_PATH.append(T_OBJ).append("warning.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.WARNING, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("start_ccs_task.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.COMPOSITE_TASK_START, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("skip_ccs_task.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.COMPOSITE_TASK_SKIP, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("review_ccs_task.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.COMPOSITE_TASK_REVIEW, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("goto_ccs_task.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.COMPOSITE_GOTO_TASK, imageDescriptor);

		path = ICONS_PATH.append(T_ELCL).append("restart_all.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.COMPOSITE_RESTART_ALL, imageDescriptor);

		path = ICONS_PATH.append(T_VIEW).append("cheatsheet_view.png");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(getPlugin().getBundle(), path);
		reg.put(ICheatSheetResource.CHEATSHEET_VIEW, imageDescriptor);
	}

	/**
	 * Restores the state of the previously saved cheatsheet history
	 */
	private void restoreCheatSheetHistory() {
		SafeRunner.run(new SafeRunnable() {
			@Override
			public void run() {
				IMemento memento;
				memento = readMemento(HISTORY_FILENAME);
				if (memento != null) {
					IMemento childMem = memento.getChild(MEMENTO_TAG_CHEATSHEET_HISTORY);
					if (childMem != null) {
						history.restoreState(childMem);
					}
				}
			}
			@Override
			public void handleException(Throwable e) {
				String message = Messages.ERROR_READING_STATE_FILE;
				CheatSheetPlugin.getPlugin().getLog().error(message, e);
			}
		});
	}

	/**
	 * Read a memento from the state directory for the cheatsheets plugin
	 * @param filename A simple filename
	 * @return A memento read from the state directory or null if the memento could not be read
	 */
	public XMLMemento readMemento(String filename) {
		XMLMemento memento;
		// Read the cheatsheet state file.
		final File stateFile = getCheatSheetStateFile(filename);
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(stateFile), StandardCharsets.UTF_8)) {
			memento = XMLMemento.createReadRoot(reader);
		} catch (FileNotFoundException e) {
			memento = null;
			// Do nothing, the file will not exist the first time the workbench in used.
		} catch (Exception e) {
			String message = Messages.ERROR_READING_STATE_FILE;
			CheatSheetPlugin.getPlugin().getLog().error(message, e);
			memento = null;
		}
		return memento;
	}

	/**
	 * Saves the current cheatsheet history so it can be restored later on
	 */
	private void saveCheatSheetHistory() {
		SafeRunner.run(new SafeRunnable() {
			@Override
			public void run() {
				XMLMemento memento = XMLMemento.createWriteRoot(MEMENTO_TAG_CHEATSHEET);

				// Save the version number.
				memento.putString(MEMENTO_TAG_VERSION, VERSION_STRING[1]);

				// Save perspective history.
				getCheatSheetHistory().saveState(memento.createChild(MEMENTO_TAG_CHEATSHEET_HISTORY));

				IStatus status = saveMemento(memento, HISTORY_FILENAME);
				if (!status.isOK()) {
					CheatSheetPlugin.getPlugin().getLog().log(status);
				}
			}
			@Override
			public void handleException(Throwable e) {
				String message = Messages.ERROR_WRITING_STATE_FILE;
				CheatSheetPlugin.getPlugin().getLog().error(message, e);
			}
		});
	}

	/**
	 * Save the memento to a file in this plugins state area
	 * @param memento The memento to save
	 * @param filename A simple filename
	 * @return OK_Status if the memento was saved without error, otherwise an error
	 * status
	 */
	public IStatus saveMemento(XMLMemento memento, String filename) {
		// Save the IMemento to a file.
		File stateFile = getCheatSheetStateFile(filename);
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(stateFile),
				StandardCharsets.UTF_8)) {
			memento.save(writer);
			return Status.OK_STATUS;
		} catch (IOException e) {
			stateFile.delete();
			String message = Messages.ERROR_WRITING_STATE_FILE;
			IStatus status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, message, e);
			return status;
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		plugin = this;

		// allow the MRU history to be lazily initialized by getCheatSheetHistory
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);

		// save the MRU history if necessary
		// if we never restored history, let existing memento stand
		if (history != null) {
			saveCheatSheetHistory();
		}

		CheatSheetRegistryReader.getInstance().stop();
	}

	/*
	 * Since 3.1.1. Load from icon paths with $NL$
	 */
	public static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path) {
		URL url= FileLocator.find(bundle, path, null);
		if (url != null) {
			return ImageDescriptor.createFromURL(url);
		}
		return null;
	}

}
