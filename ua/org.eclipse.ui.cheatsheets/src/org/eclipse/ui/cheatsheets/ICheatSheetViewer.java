/*******************************************************************************
 *  Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.ui.cheatsheets;

import java.net.URL;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A cheat sheet viewer.
 * <p>
 * Clients call {@link CheatSheetViewerFactory#createCheatSheetView()} to create
 * a cheat sheet viewer instance, and then call the viewer's
 * <code>createPartControl</code> method to have it create the viewer's control
 * under the specified SWT composite. The viewer's control can then be retrieved
 * using <code>getControl</code> to arrange layout. The <code>setInput</code>
 * methods are used to set (or clear) the cheat sheet shown in the viewer,
 * and can be called either before or after the viewer's controls have been
 * created and laid out.
 * </p>
 * <p>
 * The execution states of open cheat sheets are maintained and persisted
 * globally using the cheat sheet id as the key.
 * </p>
 *
 * @see CheatSheetViewerFactory
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ICheatSheetViewer {

	/**
	 * Creates the SWT controls for this cheat sheet viewer.
	 * <p>
	 * When the parent Composite is disposed, this will automatically
	 * dispose the controls added by this viewer (and release any other
	 * viewer-specific state).
	 * </p>
	 *
	 * @param parent the parent control
	 */
	public void createPartControl(Composite parent);

	/**
	 * Returns the primary control associated with this viewer.
	 *
	 * @return the SWT control which displays this viewer's
	 * content, or <code>null</code> if this viewer's controls
	 * have not yet been created.
	 */
	public Control getControl();

	/**
	 * Returns the id of the cheat sheet showing in this view.
	 *
	 * @return id the cheat sheet id, or <code>null</code> if the
	 * view is not showing a cheat sheet
	 */
	public String getCheatSheetID();

	/**
	 * Asks this cheat sheet viewer to take focus.
	 */
	public void setFocus();

	/**
	 * Sets the cheat sheet viewer to show the cheat sheet with
	 * the given id. The cheat sheet content file is located via the
	 * <code>org.eclipse.ui.cheatsheets.cheatSheetContent</code>
	 * extension point. The viewer shows an error message if there
	 * is no cheat sheet with the given id.
	 * <p>
	 * The execution states of open cheat sheets are maintained
	 * and persisted globally using the cheat sheet id as the key.
	 * </p>
	 *
	 * @param id the cheat sheet id, or <code>null</code> to show
	 * no cheat sheet in this viewer
	 */
	public void setInput(String id);

	/**
	 * Sets the cheat sheet viewer to show the cheat sheet with the
	 * given cheat sheet content file. The viewer shows an error
	 * message if the cheat sheet content file cannot be opened or
	 * parsed.
	 * <p>
	 * The execution states of open cheat sheets are maintained
	 * and persisted globally using the cheat sheet id as the key.
	 * This means that each cheat sheet must have a distinct id,
	 * including ones opened from URLs.
	 * </p>
	 * <p>
	 * Use the other <code>setInput</code> method to clear
	 * the viewer; that is, call <code>setInput(null)</code>.
	 * </p>
	 *
	 * @param id the id to give this cheat sheet
	 * @param name the name to give this cheat sheet
	 * @param url URL of the cheat sheet content file
	 * @exception IllegalArgumentException if the parameters
	 * are <code>null</code>
	 */
	public void setInput(String id, String name, URL url);

	/**
	 * Sets the currently active cheat sheet to its initial state and
	 * initalizes the cheat sheet manager data.
	 * @param cheatSheetData A map whose keys and values are all of type
	 * <code>java.lang.String</code> or <code>null</code> to reset all data in
	 * the cheat sheet manager.
	 * @since 3.2
	 */
	public void reset(Map<String, String> cheatSheetData);
}
