/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
 *     Paul Pazderski  - Bug 548356: Removed error prone handling of input content from input partition.
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * A region in an IOConsole's document.
 *
 * @since 3.1
 */
public class IOConsolePartition implements ITypedRegion {
	/** Type for input partitions. */
	public static final String OUTPUT_PARTITION_TYPE = ConsolePlugin.getUniqueIdentifier() + ".io_console_output_partition_type"; //$NON-NLS-1$
	/** Type for output partitions. */
	public static final String INPUT_PARTITION_TYPE = ConsolePlugin.getUniqueIdentifier() + ".io_console_input_partition_type"; //$NON-NLS-1$

	private int offset;
	private int length;
	private String type;

	/**
	 * Output partitions are all read only. Input partitions are read only once they
	 * have been appended to the console's input stream.
	 */
	private boolean readOnly;

	/**
	 * Only one of inputStream or outputStream will be <code>null</code> depending
	 * on the partitions type.
	 */
	private IOConsoleOutputStream outputStream;
	private IOConsoleInputStream inputStream;

	/**
	 * Partition of console output.
	 *
	 * @param offset       offset where this partition starts
	 * @param outputStream source stream for this partition
	 */
	public IOConsolePartition(int offset, IOConsoleOutputStream outputStream) {
		this.outputStream = outputStream;
		this.offset = offset;
		this.type = OUTPUT_PARTITION_TYPE;
		this.readOnly = true;
	}

	/**
	 * Partition of console input.
	 *
	 * @param offset      offset where this partition starts
	 * @param inputStream source stream for this partition
	 */
	public IOConsolePartition(int offset, IOConsoleInputStream inputStream) {
		this.inputStream = inputStream;
		this.offset = offset;
		this.type = INPUT_PARTITION_TYPE;
		this.readOnly = false;
	}

	/**
	 * Partition of console output.
	 *
	 * @param outputStream source stream for this partition
	 * @param length       length of the partition
	 * @deprecated use {@link #IOConsolePartition(int, IOConsoleOutputStream)} and
	 *             {@link #setLength(int)} instead
	 */
	@Deprecated
	public IOConsolePartition(IOConsoleOutputStream outputStream, int length) {
		this(0, outputStream);
		setLength(length);
	}

	/**
	 * Partition of console input.
	 *
	 * @param inputStream source stream for this partition
	 * @param text        text of the input partition
	 * @deprecated use {@link #IOConsolePartition(int, IOConsoleInputStream)} and
	 *             {@link #setLength(int)} instead. Also note: input partitions do
	 *             not explicitly store the partitioned input anymore. Instead
	 *             request the input from the partitioned document using input
	 *             partition's offset and length.
	 */
	@Deprecated
	public IOConsolePartition(IOConsoleInputStream inputStream, String text) {
		this(0, inputStream);
		setLength(text == null ? 0 : text.length());
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public int getLength() {
		return length;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	/**
	 * Sets this partitions offset in the document.
	 *
	 * @param offset This partitions offset in the document.
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	/**
	 * Sets this partition's length.
	 *
	 * @param length new length of partition
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * Returns a StyleRange object which may be used for setting the style of this
	 * partition in a viewer.
	 *
	 * @param rangeOffset offset for the style range
	 * @param rangeLength length of style range
	 * @return style range for this partition
	 */
	public StyleRange getStyleRange(int rangeOffset, int rangeLength) {
		return new StyleRange(rangeOffset, rangeLength, getColor(), null, getFontStyle());
	}

	/**
	 * Returns the font of the input stream if the type of the partition is
	 * <code>INPUT_PARTITION_TYPE</code>, otherwise it returns the output stream
	 * font
	 *
	 * @return the font of one of the backing streams
	 */
	private int getFontStyle() {
		if (type.equals(INPUT_PARTITION_TYPE)) {
			return inputStream != null ? inputStream.getFontStyle() : 0;
		}
		return outputStream.getFontStyle();
	}

	/**
	 * Returns the colour of the input stream if the type of the partition is
	 * <code>INPUT_PARTITION_TYPE</code>, otherwise it returns the output stream
	 * colour
	 *
	 * @return the colour of one of the backing streams
	 */
	public Color getColor() {
		if (type.equals(INPUT_PARTITION_TYPE)) {
			return inputStream != null ? inputStream.getColor() : null;
		}
		return outputStream.getColor();
	}

	/**
	 * Returns if this partition is read-only.
	 *
	 * @see org.eclipse.ui.console.IConsoleDocumentPartitioner#isReadOnly(int)
	 * @return if this partition is read-only
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Sets the read-only state of this partition to <code>true</code>.
	 *
	 * @see org.eclipse.ui.console.IConsoleDocumentPartitioner#isReadOnly(int)
	 */
	public void setReadOnly() {
		readOnly = true;
	}

	/**
	 * Returns the underlying output stream.
	 * <p>
	 * Always <code>null</code> for input partitions.
	 * </p>
	 *
	 * @return the underlying output stream
	 * @deprecated use {@link #getOutputStream()} instead
	 */
	@Deprecated
	IOConsoleOutputStream getStream() {
		return outputStream;
	}

	/**
	 * Returns the underlying output stream.
	 * <p>
	 * Always <code>null</code> for input partitions.
	 * </p>
	 *
	 * @return the underlying output stream
	 */
	IOConsoleOutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * Returns the underlying input stream.
	 * <p>
	 * Always <code>null</code> for output partitions.
	 * </p>
	 *
	 * @return the underlying input stream
	 */
	IOConsoleInputStream getInputStream() {
		return inputStream;
	}

	/**
	 * Test if this partition belongs to the given input stream.
	 *
	 * @param in the input stream to test or <code>null</code>
	 * @return <code>true</code> if this partition belongs to input stream
	 */
	boolean belongsTo(IOConsoleInputStream in) {
		return inputStream == in;
	}

	/**
	 * Test if this partition belongs to the given output stream.
	 *
	 * @param out the output stream to test or <code>null</code>
	 * @return <code>true</code> if this partition belongs to output stream
	 */
	boolean belongsTo(IOConsoleOutputStream out) {
		return outputStream == out;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(40);
		sb.append(INPUT_PARTITION_TYPE.equals(type) ? "[Input" : "[Output"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!readOnly) {
			sb.append("+"); //$NON-NLS-1$
		}
		sb.append("]"); //$NON-NLS-1$
		sb.append(" Offset: "); //$NON-NLS-1$
		sb.append(offset);
		sb.append(" Length: "); //$NON-NLS-1$
		sb.append(length);
		return sb.toString();
	}
}
