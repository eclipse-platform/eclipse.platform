/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;

/**
 * A debug model presentation may implement this interface to override standard
 * annotations used to display instruction pointers for stack frames.
 * <p>
 * A client has several options when overriding default instruction pointer
 * annotations, and the debug platform uses the following prioritized order when
 * computing an annotation for a stack frame.
 * </p>
 * <ol>
 * <li>Specify the annotation object to use. This is done by returning a
 * non-<code>null</code> value from
 * <code>getInstructionPointerAnnotation(..)</code>.</li>
 * <li>Specify an <code>annotationType</code> extension to use. This is done by
 * returning a non-<code>null</code> value from
 * <code>getInstructionPointerAnnotationType(..)</code>. When specified, the
 * annotation type controls the image displayed via its associated
 * <code>markerAnnotationSpecification</code>.</li>
 * <li>Specify the image to use. This is done by returning a
 * non-<code>null</code> value from
 * <code>getInstructionPointerImage(..)</code>.</li>
 * </ol>
 * <p>
 * Additionally, when specifying an annotation type or image the text for the
 * instruction pointer may be specified by returning a non-<code>null</code>
 * value from <code>getInstructionPointerText(..)</code>.
 * </p>
 * <p>
 * These methods are called when the debugger has opened an editor to display
 * source for the given stack frame. The image will be positioned based on stack
 * frame line number and character ranges.
 * </p>
 * <p>
 * By default, the debug platform uses different annotations for top stack
 * frames and non-top stack frames in a thread. The default platform annotations
 * are contributed as <code>annotationType</code> extensions with the
 * identifiers
 * <code>IDebugUIConstants.ANNOTATION_INSTRUCTION_POINTER_CURRENT</code> and
 * <code>IDebugUIConstants.ANNOTAION_INSTRUCTION_POINTER_SECONDARY</code>.
 * </p>
 * <p>
 * Clients implementing a debug model presentation may also implement this
 * interface.
 * </p>
 *
 * @since 3.2
 */
public interface IInstructionPointerPresentation extends IDebugModelPresentation {
	/**
	 * Returns an annotation used for the specified stack frame in the specified
	 * editor, or <code>null</code> if a default annotation should be used.
	 *
	 * @param editorPart the editor the debugger has opened
	 * @param frame the stack frame for which the debugger is displaying
	 *  source
	 *  @return annotation or <code>null</code>
	 */
	Annotation getInstructionPointerAnnotation(IEditorPart editorPart, IStackFrame frame);

	/**
	 * Returns an identifier of a <code>org.eclipse.ui.editors.annotationTypes</code> extension used for
	 * the specified stack frame in the specified editor, or <code>null</code> if a default annotation
	 * should be used.
	 *
	 * @param editorPart the editor the debugger has opened
	 * @param frame the stack frame for which the debugger is displaying
	 *  source
	 *  @return annotation type identifier or <code>null</code>
	 */
	String getInstructionPointerAnnotationType(IEditorPart editorPart, IStackFrame frame);

	/**
	 * Returns the instruction pointer image used for the specified stack frame in the specified
	 * editor, or <code>null</code> if a default image should be used.
	 * <p>
	 * By default, the debug platform uses different images for top stack
	 * frames and non-top stack frames in a thread.
	 * </p>
	 * @param editorPart the editor the debugger has opened
	 * @param frame the stack frame for which the debugger is displaying
	 *  source
	 *  @return image or <code>null</code>
	 */
	Image getInstructionPointerImage(IEditorPart editorPart, IStackFrame frame);

	/**
	 * Returns the text to associate with the instruction pointer annotation used for the
	 * specified stack frame in the specified editor, or <code>null</code> if a default
	 * message should be used.
	 * <p>
	 * By default, the debug platform uses different images for top stack
	 * frames and non-top stack frames in a thread.
	 * </p>
	 * @param editorPart the editor the debugger has opened
	 * @param frame the stack frame for which the debugger is displaying
	 *  source
	 *  @return message or <code>null</code>
	 */
	String getInstructionPointerText(IEditorPart editorPart, IStackFrame frame);
}
