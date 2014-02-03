/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Preference constants used for the Ant Editor
 */
public class AntEditorPreferenceConstants {

	private AntEditorPreferenceConstants() {
	}
	
	/**
	 * A named preference that controls if the Ant Editor code assist gets auto activated.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION= "content_assist_autoactivation"; //$NON-NLS-1$

	/**
	 * A name preference that holds the auto activation delay time in milli seconds.
	 * <p>
	 * Value is of type <code>Int</code>.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION_DELAY= "content_assist_autoactivation_delay"; //$NON-NLS-1$

	/**
	 * A named preference that controls if the Ant editor code assist inserts a
	 * proposal automatically if only one proposal is available.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 2.1
	 */
	public final static String CODEASSIST_AUTOINSERT= "content_assist_autoinsert"; //$NON-NLS-1$

	/**
	 * A named preference that holds the characters that auto activate code assist in an Ant buildfile.
	 * <p>
	 * Value is of type <code>Sring</code>. All characters that trigger auto code assist in an Ant buildfile.
	 * </p>
	 */
	public final static String CODEASSIST_AUTOACTIVATION_TRIGGERS= "content_assist_autoactivation_triggers_java"; //$NON-NLS-1$

	/**
	 * A named preference that controls if the Ant editor presents code assist proposals for
	 * user defined tasks
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 3.0
	 */
	public final static String CODEASSIST_USER_DEFINED_TASKS= "content_assist_userDefinedTasks"; //$NON-NLS-1$
	
	/**
	 * A named preference that specifies the tab size for the Ant formatter.
	 * <p>
	 * Value is of type <code>int</code>.
	 * </p>
	 */
	public static final String FORMATTER_TAB_SIZE= "formatter_tab_size"; //$NON-NLS-1$
	
	/**
	 * A named preference that specifies if the Ant formatter uses spaces for tabs.
	 * <p>
	 * Value is of type <code>boolean</code>. If <code>false</code> spaces instead of tabs are used
	 * when formatting. If <code>true</code> the formatter inserts a tab character for indenting.
	 * </p>
	 */
	public static final String FORMATTER_TAB_CHAR= "formatter_tab_char"; //$NON-NLS-1$
	
	/**
	 * A named preference that specifies if the Ant formatter aligns the final
	 * &quote&gt&quote in multi-line element tags
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true</code> the final
	 * &quote&gt&quote in multi-line element tags are aligned by the formatter.
	 * </p>
	 */
	public static final String FORMATTER_ALIGN= "formatter_align"; //$NON-NLS-1$
	
	/**
	 * A named preference that specifies the maximum line length for the Ant formatter.
	 * <p>
	 * Value is of type <code>int</code>.
	 * </p>
	 */
	public static final String FORMATTER_MAX_LINE_LENGTH= "formatter_max_line_length"; //$NON-NLS-1$
			
	/**
	 * A named preference that specifies if the Ant formatter should wrap elements that are longer than
	 * the maximum line length.
	 * <p>
	 * Value is of type <code>Boolean</code>. If <code>true</code> long elements are wrapped
	 * when formatting in the editor.
	 * </p>
	 */
	public static final String FORMATTER_WRAP_LONG= "formatter_wrap_long"; //$NON-NLS-1$
	
	/**
	 * Boolean preference identifier constant which specifies whether the Ant editor should
	 * format templates on insert.
	 */
	public static final String TEMPLATES_USE_CODEFORMATTER= "templates_use_codeformatter"; //$NON-NLS-1$

	/**
	 * Preference key suffix for bold text style preference keys.
	 * 
	 */
	public static final String EDITOR_BOLD_SUFFIX= "_bold"; //$NON-NLS-1$

	/**
	 * Preference key suffix for italic text style preference keys.
	 */
	public static final String EDITOR_ITALIC_SUFFIX= "_italic"; //$NON-NLS-1$
	
	/**
	 * Preference key prefix for all problem preference keys.
	 */
	public static final String PROBLEM= "problem"; //$NON-NLS-1$
	
	/**
	 * String preference identifier constant which specifies the severity level for problems
	 * related to classpath in the Ant editor
	 */
	public static final String PROBLEM_CLASSPATH= PROBLEM + "_classpath";  //$NON-NLS-1$
	
	/**
	 * String preference identifier constant which specifies the severity level for problems
	 * related to properties in the Ant editor
	 */
	public static final String PROBLEM_PROPERTIES= PROBLEM + "_properties";  //$NON-NLS-1$
	
	/**
	 * String preference identifier constant which specifies the severity level for problems
	 * related to imports in the Ant editor
	 */
	public static final String PROBLEM_IMPORTS= PROBLEM + "_imports";  //$NON-NLS-1$
	
	/**
	 * String preference identifier constant which specifies the severity level for problems
	 * related to tasks in the Ant editor
	 */
	public static final String PROBLEM_TASKS= PROBLEM + "_tasks";  //$NON-NLS-1$
    
    /**
     * String preference identifier constant which specifies the severity level for problems
     * related to security exceptions in the Ant editor
     */
    public static final String PROBLEM_SECURITY= PROBLEM + "_security";  //$NON-NLS-1$
	
	/**
	 * The constants allowable for the severity levels for problem preferences
	 */
	public static final String BUILDFILE_ERROR = "error"; //$NON-NLS-1$
	public static final String BUILDFILE_WARNING = "warning"; //$NON-NLS-1$
	public static final String BUILDFILE_IGNORE = "ignore"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls whether problem reporting is enabled in the Ant editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @since 3.1
	 */
	public static String BUILDFILE_IGNORE_ALL = "ignoreAll"; //$NON-NLS-1$
	
	/**
	 * A named preference that lists the names of buildfiles to not do problem reporting for
	 * <p>
	 * Value is of type <code>String</code>.
	 * </p>
	 * 
	 * @since 3.1
	 */
	public static String BUILDFILE_NAMES_TO_IGNORE = "ignoreNames"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls whether folding is enabled in the Ant editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @since 3.1
	 */
	public static final String EDITOR_FOLDING_ENABLED= "editor_folding_enabled"; //$NON-NLS-1$
	
	/**
	 * A named preference that stores the value for comment folding.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @since 3.1
	 */
	public static final String EDITOR_FOLDING_COMMENTS= "editor_folding_comments"; //$NON-NLS-1$
	
	/**
	 * A named preference that stores the value for target folding.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @since 3.1
	 */
	public static final String EDITOR_FOLDING_TARGETS= "editor_folding_targets"; //$NON-NLS-1$
	
	/**
	 * A named preference that stores the value for DTD folding.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @since 3.1
	 */
	public static final String EDITOR_FOLDING_DTD= "editor_folding_dtd"; //$NON-NLS-1$
	
	/**
	 * A named preference that stores the value for defining elements folding.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * 
	 * @since 3.1
	 */
	public static final String EDITOR_FOLDING_DEFINING= "editor_folding_defining"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls if segmented view (show selected element only) is turned on or off.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 3.1
	 */
	public static final String EDITOR_SHOW_SEGMENTS= "editor_show_segments"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls if occurrences are marked in the editor
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 * @since 3.1
	 */
	public static final String EDITOR_MARK_OCCURRENCES= "editor_mark_occurrences"; //$NON-NLS-1$
    
    /**
     * A named preference that controls if background reconciling is triggered by the user
     * typing in the editor
     * <p>
     * Value is of type <code>Boolean</code>.
     * </p>
     * @since 3.1
     */
    public static final String EDITOR_RECONCILE= "editor_reconcile"; //$NON-NLS-1$
	
	/**
	 * A named preference that controls whether occurrences are sticky in the editor.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 *
	 * @since 3.1
	 */
	public static final String EDITOR_STICKY_OCCURRENCES= "stickyOccurrences"; //$NON-NLS-1$


	public static void initializeDefaultValues(IPreferenceStore store) {
		
		store.setDefault(CODEASSIST_AUTOINSERT, true);
		store.setDefault(CODEASSIST_USER_DEFINED_TASKS, true);
		store.setDefault(CODEASSIST_AUTOACTIVATION, true);
		store.setDefault(CODEASSIST_AUTOACTIVATION_DELAY, 500);
		store.setDefault(CODEASSIST_AUTOACTIVATION_TRIGGERS, "<${"); //$NON-NLS-1$
		
		store.setDefault(FORMATTER_TAB_CHAR, true);
		store.setDefault(FORMATTER_TAB_SIZE, 4);
		store.setDefault(FORMATTER_ALIGN, false);
		store.setDefault(FORMATTER_MAX_LINE_LENGTH, 80);
		store.setDefault(FORMATTER_WRAP_LONG, false);
		
		store.setDefault(TEMPLATES_USE_CODEFORMATTER, true);
		
        store.setDefault(PROBLEM_SECURITY, BUILDFILE_WARNING);
		store.setDefault(PROBLEM_CLASSPATH, BUILDFILE_WARNING);
		store.setDefault(PROBLEM_PROPERTIES, BUILDFILE_IGNORE);
		store.setDefault(PROBLEM_IMPORTS, BUILDFILE_WARNING);
		store.setDefault(PROBLEM_TASKS, BUILDFILE_IGNORE);
		
		store.setDefault(EDITOR_FOLDING_ENABLED, true);
		
		store.setDefault(EDITOR_SHOW_SEGMENTS, false);
		
		store.setDefault(EDITOR_MARK_OCCURRENCES, false);
		store.setDefault(EDITOR_STICKY_OCCURRENCES, true);
		
		store.setDefault(BUILDFILE_IGNORE_ALL, false);
        
        store.setDefault(EDITOR_RECONCILE, true);
	}
}
