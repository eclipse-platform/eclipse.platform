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
package org.eclipse.ant.internal.launching.debug.model;


public class DebugMessageIds {
	
	public final static String MESSAGE_DELIMITER= ","; //$NON-NLS-1$
	
	public final static String BUILD_STARTED=   "build_started"; //$NON-NLS-1$
	public final static String TARGET_STARTED= "target_started"; //$NON-NLS-1$
	public final static String TARGET_FINISHED= "target_finished"; //$NON-NLS-1$
	public final static String TASK_STARTED= "task_started"; //$NON-NLS-1$
	public final static String TASK_FINISHED= "task_finished"; //$NON-NLS-1$
	
	public final static String STEP= "step"; //$NON-NLS-1$
	public final static String STEP_OVER= "step_over"; //$NON-NLS-1$
	public final static String STEP_INTO= "step_into"; //$NON-NLS-1$
	
	public final static String TERMINATE= "terminate"; //$NON-NLS-1$
	public final static String TERMINATED= "terminated"; //$NON-NLS-1$
	public final static String SUSPEND= "suspend"; //$NON-NLS-1$
	public final static String SUSPENDED= "suspended"; //$NON-NLS-1$
	public final static String RESUME= "resume"; //$NON-NLS-1$
	
	public final static String STACK= "stack"; //$NON-NLS-1$
	
	public final static String ADD_BREAKPOINT= "add"; //$NON-NLS-1$
	public final static String REMOVE_BREAKPOINT= "remove"; //$NON-NLS-1$
	
	public final static String CLIENT_REQUEST= "client"; //$NON-NLS-1$
	public final static String BREAKPOINT= "breakpoint"; //$NON-NLS-1$
	
	public final static String PROPERTIES= "prop"; //$NON-NLS-1$
	public final static String PROPERTY_VALUE= "value"; //$NON-NLS-1$
	public final static int PROPERTY_USER= 0;
	public final static int PROPERTY_SYSTEM= 1;
	public final static int PROPERTY_RUNTIME= 2;
}
