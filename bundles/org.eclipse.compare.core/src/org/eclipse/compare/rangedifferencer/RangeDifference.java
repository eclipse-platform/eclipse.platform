/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.rangedifferencer;

/**
 * Description of a change between two or three ranges of comparable entities.
 * <p>
 * <code>RangeDifference</code> objects are the elements of a compare result returned from
 * the <code>RangeDifferencer</code> <code>find* </code> methods.
 * Clients use these objects as they are returned from the differencer.
 * This class is not intended to be instantiated outside of the Compare framework.
 * <p>
 * Note: A range in the <code>RangeDifference</code> object is given as a start index
 * and length in terms of comparable entities. However, these entity indices and counts
 * are not necessarily character positions. For example, if an entity represents a line
 * in a document, the start index would be a line number and the count would be in lines.
 * </p>
 *
 * @see RangeDifferencer
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class RangeDifference {

	/** Two-way change constant indicating no change. */
	public final static int NOCHANGE = 0;

	/**
	 * Two-way change constant indicating two-way change (same as
	 * <code>RIGHT</code>)
	 */
	public final static int CHANGE = 2;

	/** Three-way change constant indicating a change in both right and left. */
	public final static int CONFLICT = 1;

	/** Three-way change constant indicating a change in right. */
	public final static int RIGHT = 2;

	/** Three-way change constant indicating a change in left. */
	public final static int LEFT = 3;

	/**
	 * Three-way change constant indicating the same change in both right and
	 * left, that is only the ancestor is different.
	 */
	public final static int ANCESTOR = 4;

	/** Constant indicating an unknown change kind. */
	public final static int ERROR = 5;

	/**
	 * the kind of change: NOCHANGE, CHANGE, LEFT, RIGHT, ANCESTOR, CONFLICT,
	 * ERROR
	 * 
	 * @since org.eclipse.compare.core 3.5
	 */
	protected int kind;

	/**
	 * @since org.eclipse.compare.core 3.5
	 */
	protected int leftStart;

	/**
	 * @since org.eclipse.compare.core 3.5
	 */
	protected int leftLength;

	/**
	 * @since org.eclipse.compare.core 3.5
	 */
	protected int rightStart;

	/**
	 * @since org.eclipse.compare.core 3.5
	 */
	protected int rightLength;

	/**
	 * @since org.eclipse.compare.core 3.5
	 */
	protected int ancestorStart;

	/**
	 * @since org.eclipse.compare.core 3.5
	 */
	protected int ancestorLength;
	
	/**
	 * Creates a new range difference with the given change kind.
	 * 
	 * @param changeKind
	 *            the kind of change
	 * @since org.eclipse.compare.core 3.5
	 */
	protected RangeDifference(int changeKind) {
		this.kind = changeKind;
	}

	/**
	 * Creates a new <code>RangeDifference</code> with the given change kind and
	 * left and right ranges.
	 * 
	 * @param kind
	 *            the kind of change
	 * @param rightStart
	 *            start index of entity on right side
	 * @param rightLength
	 *            number of entities on right side
	 * @param leftStart
	 *            start index of entity on left side
	 * @param leftLength
	 *            number of entities on left side
	 * @since org.eclipse.compare.core 3.5
	 */
	protected RangeDifference(int kind, int rightStart, int rightLength, int leftStart, int leftLength) {
		this.kind= kind;
		this.rightStart= rightStart;
		this.rightLength= rightLength;
		this.leftStart= leftStart;
		this.leftLength= leftLength;
	}

	/**
	 * Creates a new <code>RangeDifference</code> with the given change kind and
	 * left, right, and ancestor ranges.
	 * 
	 * @param kind
	 *            the kind of change
	 * @param rightStart
	 *            start index of entity on right side
	 * @param rightLength
	 *            number of entities on right side
	 * @param leftStart
	 *            start index of entity on left side
	 * @param leftLength
	 *            number of entities on left side
	 * @param ancestorStart
	 *            start index of entity on ancestor side
	 * @param ancestorLength
	 *            number of entities on ancestor side
	 * @since org.eclipse.compare.core 3.5
	 */
	protected RangeDifference(int kind, int rightStart, int rightLength, int leftStart, int leftLength,
									int ancestorStart, int ancestorLength) {
		this(kind, rightStart, rightLength, leftStart, leftLength);
		this.ancestorStart= ancestorStart;
		this.ancestorLength= ancestorLength;
	}

	/**
	 * Returns the kind of difference.
	 *
	 * @return the kind of difference, one of
	 * <code>NOCHANGE</code>, <code>CHANGE</code>, <code>LEFT</code>, <code>RIGHT</code>,
	 * <code>ANCESTOR</code>, <code>CONFLICT</code>, <code>ERROR</code>
	 */
	public int kind() {
		return this.kind;
	}

	/**
	 * Returns the start index of the entity range on the ancestor side.
	 *
	 * @return the start index of the entity range on the ancestor side
	 */
	public int ancestorStart() {
		return this.ancestorStart;
	}

	/**
	 * Returns the number of entities on the ancestor side.
	 *
	 * @return the number of entities on the ancestor side
	 */
	public int ancestorLength() {
		return this.ancestorLength;
	}

	/**
	 * Returns the end index of the entity range on the ancestor side.
	 *
	 * @return the end index of the entity range on the ancestor side
	 */
	public int ancestorEnd() {
		return this.ancestorStart + this.ancestorLength;
	}

	/**
	 * Returns the start index of the entity range on the right side.
	 *
	 * @return the start index of the entity range on the right side
	 */
	public int rightStart() {
		return this.rightStart;
	}

	/**
	 * Returns the number of entities on the right side.
	 *
	 * @return the number of entities on the right side
	 */
	public int rightLength() {
		return this.rightLength;
	}

	/**
	 * Returns the end index of the entity range on the right side.
	 *
	 * @return the end index of the entity range on the right side
	 */
	public int rightEnd() {
		return this.rightStart + this.rightLength;
	}

	/**
	 * Returns the start index of the entity range on the left side.
	 *
	 * @return the start index of the entity range on the left side
	 */
	public int leftStart() {
		return this.leftStart;
	}

	/**
	 * Returns the number of entities on the left side.
	 *
	 * @return the number of entities on the left side
	 */
	public int leftLength() {
		return this.leftLength;
	}

	/**
	 * Returns the end index of the entity range on the left side.
	 *
	 * @return the end index of the entity range on the left side
	 */
	public int leftEnd() {
		return this.leftStart + this.leftLength;
	}

	/**
	 * Returns the maximum number of entities in the left, right, and ancestor sides of this range.
	 *
	 * @return the maximum number of entities in the left, right, and ancestor sides of this range
	 */
	public int maxLength() {
		return Math.max(this.rightLength, Math.max(this.leftLength, this.ancestorLength));
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof RangeDifference) {
			RangeDifference other = (RangeDifference) obj;
			return this.kind == other.kind
				&& this.leftStart == other.leftStart
				&& this.leftLength == other.leftLength
				&& this.rightStart == other.rightStart
				&& this.rightLength == other.rightLength
				&& this.ancestorStart == other.ancestorStart
				&& this.ancestorLength == other.ancestorLength;
		}
		return super.equals(obj);
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.kind;
		result = prime * result + this.leftStart;
		result = prime * result + this.leftLength;
		result = prime * result + this.rightStart;
		result = prime * result + this.rightLength;
		result = prime * result + this.ancestorStart;
		result = prime * result + this.ancestorLength;
		return result;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer("RangeDifference {"); //$NON-NLS-1$
		switch (this.kind) {
		case NOCHANGE:
			buf.append("NOCHANGE"); //$NON-NLS-1$
			break;
		case CHANGE:
			buf.append("CHANGE/RIGHT"); //$NON-NLS-1$
			break;
		case CONFLICT:
			buf.append("CONFLICT"); //$NON-NLS-1$
			break;
		case LEFT:
			buf.append("LEFT"); //$NON-NLS-1$
			break;
		case ERROR:
			buf.append("ERROR"); //$NON-NLS-1$
			break;
		case ANCESTOR:
			buf.append("ANCESTOR"); //$NON-NLS-1$
			break;
		default:
			break;
		}

		buf.append(", "); //$NON-NLS-1$

		buf.append("Left: " + toRangeString(this.leftStart, this.leftLength) + " Right: " + toRangeString(this.rightStart, this.rightLength)); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.ancestorLength > 0 || this.ancestorStart > 0)
			buf.append(" Ancestor: " + toRangeString(this.ancestorStart, this.ancestorLength)); //$NON-NLS-1$

		buf.append("}"); //$NON-NLS-1$
		return buf.toString();
	}

	private String toRangeString(int start, int length) {
		return "(" + start + ", " + length + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}

