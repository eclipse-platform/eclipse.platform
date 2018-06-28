/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Atsuhiko Yamanaka, JCraft,Inc. - copying this class from o.e.team.cvs.ui plug-in.
 *******************************************************************************/
package org.eclipse.jsch.internal.ui.authenticator;

import org.eclipse.jsch.internal.ui.JSchUIPlugin;

/**
 * Here's how to reference the help context in code:
 *
 * WorkbenchHelp.setHelp(actionOrControl, IHelpContextIds.NAME_DEFIED_BELOW);
 * @since 1.1
 */
public interface IHelpContextIds{

  public static final String PREFIX=JSchUIPlugin.ID+"."; //$NON-NLS-1$

  public static final String KEYBOARD_INTERACTIVE_DIALOG=PREFIX
  +"keyboard_interactive_dialog_context"; //$NON-NLS-1$
  public static final String USER_VALIDATION_DIALOG=PREFIX
  +"user_validation_dialog_context"; //$NON-NLS-1$
}
