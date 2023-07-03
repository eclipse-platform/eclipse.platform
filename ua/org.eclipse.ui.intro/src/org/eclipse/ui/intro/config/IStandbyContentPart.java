/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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
package org.eclipse.ui.intro.config;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.intro.IIntroPart;

/**
 * An Intro standby part. It is a UI component that represents some standby
 * content. Standby parts can be contributed to the Eclipse intro using the
 * following extension point:
 * <pre>
 *         &lt;extension point=&quot;org.eclipse.ui.intro.configExtension&quot;&gt;
 *        	&lt;standbyPart
 *       		pluginId=&quot;com.x.y.somePluginId&quot;
 *       		class=&quot;com.x.y.someClass&quot;
 *       		id=&quot;com.x.y.someContentPartId&quot;&gt;
 *       	&lt;/standbyPart&gt;
 *         &lt;/extension&gt;
 * </pre>
 *
 * Standby content parts have a life cycle that starts with a call to init,
 * shortly after part construction, followed by a call to createPartControl.
 * During these two calls, the part is responsible for creating its content and
 * using the memento to try to recreate its previous state. SetInput is the last
 * method called when trying to create a standby part.
 *
 * @since 3.0
 */
public interface IStandbyContentPart {

	/**
	 * Creates the SWT controls for this standby part.
	 * <p>
	 * Clients should not call this method. The intro framework calls this
	 * method when it needs to.
	 *
	 * @param parent
	 *            the parent control
	 * @param toolkit
	 *            the form toolkit being used by the IIntroPart implementation
	 */
	public void createPartControl(Composite parent, FormToolkit toolkit);

	/**
	 * Returns the primary control associated with this standby part. The
	 * control is typically set during the createPartControl() call when this
	 * part is being created.
	 *
	 * @return the SWT control which displays this standby part's content, or
	 *         <code>null</code> if this standby part's controls have not yet
	 *         been created.
	 */
	public Control getControl();

	/**
	 * Initializes this intro standby content part with the given intro site. A
	 * memento is passed to the part which contains a snapshot of the part state
	 * from a previous session. Where possible, the part should try to recreate
	 * that state.
	 * <p>
	 * This method is automatically called by the workbench shortly after part
	 * construction. It marks the start of this parts' lifecycle. Clients must
	 * not call this method.
	 * </p>
	 *
	 * @param introPart
	 *            the intro part hosting this stanndby content part.
	 * @param memento
	 *            this part state or <code>null</code> if there is no previous
	 *            saved state
	 * @exception PartInitException
	 *                if this part was not initialized successfully.
	 */
	public void init(IIntroPart introPart, IMemento memento)
			throws PartInitException;

	/**
	 * Sets the input to show in this standby part. Note that input can be null,
	 * such as when the part if created through an Intro URL that does not have
	 * an input specified, or when this standby part is being recreated from a
	 * previous workbench session. In this case, the standby part is responsible
	 * for handling a null input, and recreating itself from a cached IMemento.
	 *
	 * @param input
	 *            the input object to be used by this standby part.
	 */
	public void setInput(Object input);

	/**
	 * Asks this standby part to take focus.
	 * <p>
	 * Clients should not call this method (the intro framework calls this
	 * method at appropriate times).
	 * </p>
	 */
	public void setFocus();

	/**
	 * Disposes of this standby part.
	 * <p>
	 * Clients should not call this method. The intro framework calls this
	 * method when the Customizable IntroPart is closed.
	 * </p>
	 */
	public void dispose();

	/**
	 * Saves the object state within a memento.
	 * <p>
	 * This method is automatically called by the workbench at appropriate
	 * times. Clients must not call this method directly.
	 * </p>
	 *
	 * @param memento
	 *            a memento to receive the object state
	 */
	public void saveState(IMemento memento);

}
