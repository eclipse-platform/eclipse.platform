/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Paul Pazderski  - Contributions for:
 *                          Bug 547064: use binary search for getPartition
 *                          Bug 548356: fixed user input handling
 *                          Bug 550618: getStyleRanges produced invalid overlapping styles
 *                          Bug 550621: Implementation of IConsoleDocumentPartitionerExtension
 *                          Bug 76936:  Support interpretation of \b and \r in console output
 *                          Bug 365770: Race condition in console clearing
 *                          Bug 553282: Support interpretation of \f and \v in console output
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitionerExtension;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.MultiStringMatcher;
import org.eclipse.jface.text.MultiStringMatcher.Match;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TypedRegion;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.eclipse.ui.console.IConsoleDocumentPartitionerExtension;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Partitions an IOConsole's document
 *
 * @since 3.1
 */
public class IOConsolePartitioner
		implements IConsoleDocumentPartitioner, IConsoleDocumentPartitionerExtension, IDocumentPartitionerExtension {
	/**
	 * Enumeration used to distinct sources of document updates. (especially to
	 * distinct updates triggered by this partitioner from other document changes)
	 */
	private enum DocUpdateType {
		/**
		 * Default if reason for document change is not known. Document change is
		 * interpreted as user input.
		 */
		INPUT,
		/**
		 * Document update was triggered from this partitioner by appending content
		 * received from output streams.
		 */
		OUTPUT,
		/** Document update was triggered from this partitioner's {@link TrimJob}. */
		TRIM,
	}

	/**
	 * If true validate partitioning after changes and do other additional
	 * assertions. Useful for developing/debugging.
	 */
	private static final boolean ASSERT = false;

	/**
	 * Comparator to sort or search {@link IRegion}s by {@link IRegion#getOffset()}.
	 */
	private static final Comparator<IRegion> CMP_REGION_BY_OFFSET = Comparator.comparing(IRegion::getOffset);

	/**
	 * Pattern used to find supported ASCII control characters <b>except</b>
	 * carriage return.
	 */
	private static final String CONTROL_CHARACTERS_PATTERN_STR = "(?:\b+|\u0000+|\u000b+|\f+)"; //$NON-NLS-1$
	/**
	 * Pattern used to find supported ASCII control characters <b>including</b>
	 * carriage return.
	 */
	private static final String CONTROL_CHARACTERS_WITH_CR_PATTERN_STR = "(?:\b+|\u0000+|\u000b+|\f+|\r+(?!\n))"; //$NON-NLS-1$

	/** The connected {@link IDocument} this partitioner manages. */
	private IDocument document;
	/**
	 * List of all partitions. Must always be sorted ascending by
	 * {@link IRegion#getOffset()} and not contain <code>null</code> or 0-length
	 * elements. (see also {@link #checkPartitions()})
	 */
	private final ArrayList<IOConsolePartition> partitions = new ArrayList<>();
	/**
	 * max ~ 16MB when debugged application outputs faster then we can handle. Seems
	 * to be a could compromise between memory and speed we can handle. Eclipse IDE
	 * may use that memory size ~3 times during processing (buffer+document+parser)
	 */
	private static final int MAX_BUFFER_BYTES = 16_000_000;
	/**
	 * Queue of buffers that have not yet been appended to the document. Every
	 * buffer will hold up to 8192 byte - but typically a single line output if
	 * handled fast. The debugged application will block if the limit is exceeded.
	 */
	private final BlockingQueue<PendingPartition> pendingPartitions = new LinkedBlockingQueue<>(
			MAX_BUFFER_BYTES / 8192);
	/** Job that appends pending partitions to the document. */
	private final QueueProcessingJob queueJob = new QueueProcessingJob();
	/** Job that trims console content if it exceeds {@link #highWaterMark}. */
	private final TrimJob trimJob = new TrimJob();
	/**
	 * Reason for document update. Set before changing document inside this
	 * partitioner to prevent that change is interpreted as user input.
	 * <p>
	 * Automatically reset to {@link DocUpdateType#INPUT} after every document
	 * change.
	 * </p>
	 */
	private DocUpdateType updateType = DocUpdateType.INPUT;
	/**
	 * A list of partitions containing input from the console, that have not been
	 * appended to the input stream yet. No guarantees on element order.
	 */
	private ArrayList<IOConsolePartition> inputPartitions;
	/**
	 * A matcher to search for legal line delimiters in new input. Never
	 * <code>null</code> but match nothing if no document connected.
	 */
	private MultiStringMatcher legalLineDelimiterMatcher;
	/**
	 * The high mark for console content trimming. If console content exceeds this
	 * length trimming is scheduled. Trimming is disabled if value is negative.
	 */
	private int highWaterMark = -1;
	/**
	 * The low mark for console content trimming. If trim is performed approximate
	 * this many characters are remain in console.
	 */
	private int lowWaterMark = -1;

	/** The partitioned {@link IOConsole}. */
	private IOConsole console;

	/** Set after console signaled that all streams are closed. */
	private volatile boolean streamsClosed;
	/**
	 * Active pattern to search for supported control characters. If
	 * <code>null</code> control characters are treated as any other characters.
	 */
	private Pattern controlCharacterPattern = null;
	/**
	 * Whether <code>\r</code> is interpreted as control characters
	 * (<code>true</code>) or not in console output. If <code>false</code> they are
	 * probably handled as newline.
	 */
	private boolean carriageReturnAsControlCharacter = true;
	/**
	 * Offset where next output is written to console.
	 */
	private int outputOffset = 0;

	/**
	 * Create new partitioner for an {@link IOConsole}.
	 * <p>
	 * The partitioner must be explicit {@link #connect(IDocument) connected} with
	 * the consoles {@link IDocument}.
	 * </p>
	 *
	 * @param console the partitioned console. Not <code>null</code>.
	 */
	public IOConsolePartitioner(IOConsole console) {
		this.console = Objects.requireNonNull(console);
		queueJob.setRule(console.getSchedulingRule());
		trimJob.setRule(console.getSchedulingRule());
	}

	/**
	 * Get partitioned document or <code>null</code> if none connected.
	 *
	 * @return partitioned document
	 */
	public IDocument getDocument() {
		return document;
	}

	@Override
	public void connect(IDocument doc) {
		if (doc == document) {
			return;
		}
		disconnect();
		if (doc != null) {
			synchronized (partitions) {
				inputPartitions = new ArrayList<>();
				document = doc;
				legalLineDelimiterMatcher = MultiStringMatcher.create(document.getLegalLineDelimiters());
			}
		}
	}

	@Override
	public void disconnect() {
		pendingPartitions.clear();
		synchronized (partitions) {
			trimJob.cancel();
			queueJob.cancel();
			legalLineDelimiterMatcher = null;
			document = null;
			inputPartitions = null;
			partitions.clear();
		}
	}

	/**
	 * Get high water mark.
	 *
	 * @return the trim if exceeded mark
	 * @see IOConsole#getHighWaterMark()
	 */
	public int getHighWaterMark() {
		return highWaterMark;
	}

	/**
	 * Get low water mark.
	 *
	 * @return the trim to this length mark
	 * @see IOConsole#getLowWaterMark()
	 */
	public int getLowWaterMark() {
		return lowWaterMark;
	}

	/**
	 * Set low and high water marks.
	 *
	 * @param low  the trim to this length mark
	 * @param high the trim if exceeded mark
	 * @see IOConsole#setWaterMarks(int, int)
	 */
	public void setWaterMarks(int low, int high) {
		lowWaterMark = low;
		highWaterMark = high;
		ConsolePlugin.getStandardDisplay().asyncExec(this::checkBufferSize);
	}

	/**
	 * Notification from the console that all of its streams have been closed.
	 */
	public void streamsClosed() {
		if (streamsClosed) {
			String msg = "Streams are already closed.";//$NON-NLS-1$
			log(IStatus.ERROR, msg, new IllegalStateException(msg));
			return;
		}
		streamsClosed = true;
		checkFinished();
	}

	/**
	 * Check if partitioner is finished and does not expect any new data appended to
	 * document.
	 */
	private void checkFinished() {
		if (streamsClosed) {
			// do not expect new data since all streams are closed
			// check if pending data is queued
			final boolean morePending = !pendingPartitions.isEmpty();
			if (morePending) {
				queueJob.schedule();
			} else {
				console.partitionerFinished();
			}
		}
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
	}

	@Override
	public boolean documentChanged(DocumentEvent event) {
		return documentChanged2(event) != null;
	}

	@Override
	public String[] getLegalContentTypes() {
		return new String[] { IOConsolePartition.OUTPUT_PARTITION_TYPE, IOConsolePartition.INPUT_PARTITION_TYPE };
	}

	@Override
	public String getContentType(int offset) {
		return getPartition(offset).getType();
	}

	@Override
	public ITypedRegion[] computePartitioning(int offset, int length) {
		return computeIOPartitioning(offset, length);
	}

	/**
	 * Same as {@link #computePartitioning(int, int)} but with more specific return
	 * type.
	 *
	 * @param offset the offset of the range of interest
	 * @param length the length of the range of interest
	 * @return the partitioning of the requested range (never <code>null</code>)
	 */
	private IOConsolePartition[] computeIOPartitioning(int offset, int length) {
		return computePartitioning(offset, length, true, true);
	}

	/**
	 * Get partitioning for a given range with possibility to filter partitions by
	 * their read-only property.
	 *
	 * @param offset          the offset of the range of interest
	 * @param length          the length of the range of interest
	 * @param includeWritable if false writable partitions are skipped
	 * @param includeReadOnly if false read-only partitions are skipped
	 * @return the partitioning of the requested range (never <code>null</code>)
	 */
	private IOConsolePartition[] computePartitioning(int offset, int length, boolean includeWritable,
			boolean includeReadOnly) {
		final List<IOConsolePartition> result = new ArrayList<>();
		synchronized (partitions) {
			int index = findPartitionCandidate(offset);
			if (index < 0) { // requested range starts before any known partition offset
				index = 0; // so we start collecting at first known partition
			}

			final int end = offset + length;
			for (; index < partitions.size(); index++) {
				final IOConsolePartition partition = partitions.get(index);
				if (partition.getOffset() >= end) {
					break;
				}
				if ((includeWritable && !partition.isReadOnly()) || (includeReadOnly && partition.isReadOnly())) {
					result.add(partition);
				}
			}
		}
		return result.toArray(new IOConsolePartition[0]);
	}

	@Override
	public ITypedRegion getPartition(int offset) {
		final ITypedRegion partition = getIOPartition(offset);
		return partition != null ? partition : new TypedRegion(offset, 0, IOConsolePartition.INPUT_PARTITION_TYPE);
	}

	/**
	 * Like {@link #getPartition(int)} but returns <code>null</code> for
	 * unpartitioned or invalid offsets.
	 *
	 * @param offset the offset for which to determine the partition
	 * @return the partition containing this offset or <code>null</code> if offset
	 *         is not partitioned
	 */
	private IOConsolePartition getIOPartition(int offset) {
		synchronized (partitions) {
			final int index = findPartitionCandidate(offset);
			if (index >= 0) {
				final IOConsolePartition partition = partitions.get(index);
				if (partition.getOffset() + partition.getLength() > offset) {
					return partition;
				}
			}
			return null;
		}
	}

	/**
	 * Search {@link #partitions} for the partition which is most likely containing
	 * the requested offset.
	 * <p>
	 * This (index + 1) can be used to insert a new partition with this offset. The
	 * resulting {@link #partitions} list is guaranteed to still be sorted. (as long
	 * as you do proper synchronization and consider concurrency problems)
	 * </p>
	 *
	 * @param offset the offset for which to determine the partition candidate
	 * @return index of partition element with partition offset closest to requested
	 *         offset or <code>-1</code> if requested offset is lower than offset of
	 *         any known partition
	 */
	private int findPartitionCandidate(int offset) {
		final Region target = new Region(offset, 0);
		final int index = Collections.binarySearch(partitions, target, CMP_REGION_BY_OFFSET);
		if (index >= 0) {
			// found partition whose offset equals the requested offset
			return index;
		}
		// no exact offset match. Adjust index to point at partition which is closest to
		// requested offset but whose offset is still lower than requested offset.
		// Results in -1 if all known offsets are greater.
		return (-index) - 2;
	}

	/**
	 * Enforces the buffer size.
	 * <p>
	 * When the document length exceeds the high water mark, the beginning of the
	 * document is trimmed until the document length is approximately the low water
	 * mark.
	 * </p>
	 */
	private void checkBufferSize() {
		if (document != null && highWaterMark > 0) {
			int length = document.getLength();
			if (length > highWaterMark) {
				// do trim synchronous to prevent drawing trimmed text - we are already in UI and have the lock
				trim(length - lowWaterMark, true);
			}
		}
	}

	/**
	 * Clears the console content.
	 */
	public void clearBuffer() {
		pendingPartitions.clear();
		synchronized (partitions) {
			if (document != null) {
				trimJob.setTrimOffset(document.getLength());
				trimJob.schedule();
			}
		}
	}

	@Override
	public IRegion documentChanged2(DocumentEvent event) {
		try {
			if (document != event.getDocument()) {
				log(IStatus.WARNING, "IOConsolePartitioner is connected to wrong document."); //$NON-NLS-1$
				return null;
			}
			if (document.getLength() == 0) { // document cleared
				synchronized (partitions) {
					partitions.clear();
					inputPartitions.clear();
					outputOffset = 0;
				}
				return new Region(0, 0);
			}

			synchronized (partitions) {
				switch (updateType) {
				case INPUT:
					if (event.getOffset() <= outputOffset) { // move output offset if necessary
						outputOffset -= Math.min(event.getLength(), outputOffset - event.getOffset());
						if (event.getText() != null) {
							outputOffset += event.getText().length();
						}
					}
					return applyUserInput(event);

				// update and trim jobs are triggered by this partitioner and all partitioning
				// changes are applied separately
				case OUTPUT:
					return null; // changedRegion was never assigned
				case TRIM:
					return null; // trim does not change partition types

				default:
					log(IStatus.ERROR, "Invalid enum value " + updateType); //$NON-NLS-1$
					return null;
				}
			}
		} finally {
			// always reset type since all change events not triggered by this partitioner
			// are interpreted as user input
			updateType = DocUpdateType.INPUT;
		}
	}

	/**
	 * Update partitioning due to document change. All document change events not
	 * triggered by this partitioner are considered user input and therefore
	 * partitioned as input.
	 * <p>
	 * This method does not care if the document event removed or replaced parts of
	 * read-only partitions. It assumes manipulating read-only partitions is valid
	 * or is blocked before this method is used.
	 * </p>
	 *
	 * @param event the event describing the document change
	 * @return the region of the document in which the partition type changed or
	 *         <code>null</code>
	 */
	// Required for a false 'resource not closed' warning on inputStream.
	// This input stream must not be closed by this method.
	@SuppressWarnings("resource")
	private IRegion applyUserInput(DocumentEvent event) {
		final int eventTextLength = event.getText() != null ? event.getText().length() : 0;
		final int offset = event.getOffset();
		final int amountDeleted = event.getLength();
		final IOConsoleInputStream inputStream = console.getInputStream(); // do not close this stream

		if (amountDeleted == 0 && eventTextLength == 0) {
			// event did not changed document
			return null;
		}

		final int eventPartitionIndex = findPartitionCandidate(offset);
		int lastPartitionWithValidOffset = eventPartitionIndex;

		if (amountDeleted > 0 && eventPartitionIndex >= 0) {
			// adjust length of all partitions affected by replace/remove event
			int toDelete = amountDeleted;
			for (int i = eventPartitionIndex; i < partitions.size() && toDelete > 0; i++) {
				final IOConsolePartition partition = partitions.get(i);
				final int removeLength = Math.min(partition.getLength(), toDelete);
				partition.setLength(partition.getLength() - removeLength);
				toDelete -= removeLength;
			}
			if (ASSERT) {
				Assert.isTrue(toDelete == 0, "Tried to delete outside partitioned range."); //$NON-NLS-1$
			}
			lastPartitionWithValidOffset--; // update one more since first affected partition may be empty now
		}

		if (eventTextLength > 0) {
			// find best partition for event text
			int inputPartitionIndex = eventPartitionIndex;
			IOConsolePartition inputPartition = getPartitionByIndex(inputPartitionIndex);
			if (inputPartition != null && inputPartition.isReadOnly() && offset == inputPartition.getOffset()) {
				// if we could not reuse partition at event offset we may append the partition
				// right before our event offset (e.g. if input is at end of document)
				inputPartitionIndex--;
				lastPartitionWithValidOffset--;
				inputPartition = getPartitionByIndex(inputPartitionIndex);
			}

			// process event text in parts split on line delimiters
			int textOffset = 0;
			while (textOffset < eventTextLength) {
				final Match nextNewline = legalLineDelimiterMatcher.indexOf(event.getText(), textOffset);
				final int newTextOffset = nextNewline != null ? nextNewline.getOffset() + nextNewline.getText().length()
						: eventTextLength;
				final int inputLength = newTextOffset - textOffset;

				if (inputPartition == null || inputPartition.isReadOnly()) {
					final int inputOffset = offset + textOffset;
					if (inputPartition != null
							&& inputOffset < inputPartition.getOffset() + inputPartition.getLength()) {
						// input is inside an existing read-only partition
						splitPartition(inputOffset);
					}
					inputPartition = new IOConsolePartition(inputOffset, inputStream);
					inputPartitionIndex++;
					partitions.add(inputPartitionIndex, inputPartition);
					inputPartitions.add(inputPartition);
					lastPartitionWithValidOffset++; // new input partitions get build with correct offsets
				}

				inputPartition.setLength(inputPartition.getLength() + inputLength);

				if (nextNewline != null) {
					inputPartitions.sort(CMP_REGION_BY_OFFSET);
					final StringBuilder inputLine = new StringBuilder();
					for (IOConsolePartition p : inputPartitions) {
						try {
							final String fragment = document.get(p.getOffset(), p.getLength());
							inputLine.append(fragment);
						} catch (BadLocationException e) {
							log(e);
						}
						p.setReadOnly();
					}
					inputPartitions.clear();
					if (ASSERT) {
						Assert.isTrue(inputLine.length() > 0);
					}
					if (inputStream != null) {
						inputStream.appendData(inputLine.toString());
					}
				}
				Assert.isTrue(newTextOffset > textOffset); // can prevent infinity loop
				textOffset = newTextOffset;
			}
		}

		// repair partition offsets
		int newOffset = 0;
		if (lastPartitionWithValidOffset >= 0) {
			// reduce number of partition to update by skipping still valid entries
			final IOConsolePartition partition = partitions.get(lastPartitionWithValidOffset);
			newOffset = partition.getOffset() + partition.getLength();
		}
		final Iterator<IOConsolePartition> it = partitions.listIterator(lastPartitionWithValidOffset + 1);
		while (it.hasNext()) {
			final IOConsolePartition partition = it.next();
			if (partition.getLength() <= 0) {
				if (ASSERT) {
					Assert.isTrue(partition.getLength() == 0);
				}
				it.remove();
				if (isInputPartition(partition)) {
					final boolean removed = inputPartitions.remove(partition);
					if (ASSERT) {
						Assert.isTrue(removed);
					}
				}
			} else {
				partition.setOffset(newOffset);
				newOffset += partition.getLength();
			}
		}

		if (ASSERT) {
			checkPartitions();
		}
		return new Region(0, document.getLength());
	}

	/**
	 * Split an existing partition at offset. The offset must not be the first or
	 * last offset of the existing partition because this leads to empty partitions
	 * not bearable by this partitioner.
	 * <p>
	 * New partition is added to {@link #partitions} (always) and
	 * {@link #inputPartitions} (if applicable).
	 * </p>
	 *
	 * @param offset the offset where the existing partition will end after split
	 *               and a new partition will start
	 * @return the newly created partition (i.e. the right side of the split)
	 */
	@SuppressWarnings("resource") // suppress wrong 'not closed' warnings
	private IOConsolePartition splitPartition(int offset) {
		final int partitionIndex = findPartitionCandidate(offset);
		final IOConsolePartition existingPartition = partitions.get(partitionIndex);
		final IOConsolePartition newPartition;
		if (isInputPartition(existingPartition)) {
			newPartition = new IOConsolePartition(offset, existingPartition.getInputStream());
			if (existingPartition.isReadOnly()) {
				newPartition.setReadOnly();
			}
			if (inputPartitions.contains(existingPartition)) {
				inputPartitions.add(newPartition);
			}
		} else {
			newPartition = new IOConsolePartition(offset, existingPartition.getOutputStream());
		}
		newPartition.setLength((existingPartition.getOffset() + existingPartition.getLength()) - offset);
		existingPartition.setLength(offset - existingPartition.getOffset());
		partitions.add(partitionIndex + 1, newPartition);
		return newPartition;
	}

	/**
	 * A stream has been appended, add to pendingPartions list and schedule
	 * updateJob. updateJob is scheduled with a slight delay, this allows the
	 * console to run the job less frequently and update the document with a greater
	 * amount of data each time the job is run.
	 *
	 * @param stream The stream that was written to.
	 * @param s      The string that should be appended to the document.
	 * @throws IOException if partitioner is not connected to a document
	 */
	public void streamAppended(IOConsoleOutputStream stream, String s) throws IOException {
		if (document == null) {
			throw new IOException("Document is closed"); //$NON-NLS-1$
		}
		if (s == null) {
			return;
		}
		PendingPartition partition = new PendingPartition(stream, s);
		while (!offer(partition)) {
			helpProgress();
		}
		queueJob.schedule();
	}

	private void helpProgress() {
		if (Display.getCurrent() != null) {
			// make sure pendingPartitions can take
			queueJob.processPendingPartitions();
		} else {
			Thread.yield(); // give UI thread chance to proceed
		}
	}

	private boolean offer(PendingPartition p) {
		try {
			return pendingPartitions.offer(p, 10, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}

	/**
	 * Holds data until updateJob can be run and the document can be updated.
	 */
	private class PendingPartition {
		private final CharSequence text;
		private final IOConsoleOutputStream stream;

		PendingPartition(IOConsoleOutputStream stream, CharSequence text) {
			this.stream = stream;
			this.text = text;
		}

		@Override
		public String toString() {
			return text.toString();
		}
	}

	/**
	 * Updates the document and partitioning structure. Will append everything
	 * received from output streams that is available before finishing.
	 */
	private class QueueProcessingJob extends UIJob {
		/** The partition which contains the current output offset. */
		private IOConsolePartition atOutputPartition = null;
		/** The index of atOutputPartition in the partitions list. */
		private int atOutputPartitionIndex = -1;
		/** The pending number of characters to replace in document. */
		private int replaceLength;
		/** The offset in document where to apply the next replace. */
		private int nextWriteOffset;

		QueueProcessingJob() {
			super("IOConsole Updater"); //$NON-NLS-1$
			setSystem(true);
			setPriority(Job.INTERACTIVE);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			processPendingPartitions();
			if (ASSERT) {
				checkPartitions();
			}
			return Status.OK_STATUS;
		}

		/*
		 * Job will process as much as it can each time it's run, but it gets scheduled
		 * everytime a PendingPartition is added to the list, meaning that this job
		 * could get scheduled unnecessarily in cases of heavy output. Note however,
		 * that schedule() will only reschedule a running/scheduled Job once even if
		 * it's called many times.
		 */
		@Override
		public boolean shouldRun() {
			return !pendingPartitions.isEmpty();
		}

		/**
		 * Process {@link #pendingPartitions}, append their content to document and
		 * update partitioning.
		 */
		private void processPendingPartitions() {
			final List<PendingPartition> pendingCopy = new ArrayList<>();
			// draining the whole buffer here is important - this way we get as much data as
			// available and may skip to draw text that exceeds the Console buffer size
			// anyway (see checkBufferSize()).
			pendingPartitions.drainTo(pendingCopy);
			int sizeHint = 0;
			if (pendingCopy.isEmpty()) {
				return;
			}
			IOConsoleOutputStream stream = pendingCopy.get(0).stream;
			for (PendingPartition p : pendingCopy) {
				if (p.stream != stream) {
					break;
				}
				sizeHint += p.text.length();
			}
			synchronized (partitions) {
				if (document != null) {
					applyStreamOutput(pendingCopy, sizeHint);
				}
				checkFinished();
				checkBufferSize(); // needs partitions synchronized
			}
		}

		/**
		 * Apply content collected in pending partitions to document and update
		 * partitioning structure.
		 * <p>
		 * This method is also responsible to interpret control characters if enabled
		 * (see {@link #isHandleControlCharacters()}).
		 * </p>
		 *
		 * @param pendingCopy the pending partitions to process
		 * @param sizeHint    a hint for expected content length to initialize buffer
		 *                    size. Does not have to be exact as long as it is not
		 *                    negative.
		 */
		private void applyStreamOutput(List<PendingPartition> pendingCopy, int sizeHint) {
			// local reference to get consistent parsing without blocking pattern changes
			final Pattern controlPattern = controlCharacterPattern;
			// Variables to collect required data to reduce number of document updates. The
			// partitioning must be updated in smaller iterations as the actual document
			// content. E.g. pending partitions are distinct on source output stream
			// resulting in multiple partitions but if all the content is appended to the
			// document there is only one update required to add the actual content.
			nextWriteOffset = outputOffset;
			StringBuilder content = new StringBuilder(sizeHint);
			replaceLength = 0;
			atOutputPartition = null;
			atOutputPartitionIndex = -1;

			for (PendingPartition pending : pendingCopy) {
				// create matcher to find control characters in pending content (if enabled)
				CharSequence text = pending.text;
				IOConsoleOutputStream stream = pending.stream;
				final Matcher controlCharacterMatcher = controlPattern != null ? controlPattern.matcher(text)
						: null;

				for (int textOffset = 0; textOffset < text.length();) {
					// Process pending content in chunks.
					// Processing is primary split on control characters since there interpretation
					// is easier if all content changes before are already applied.
					// Additional processing splits may result while overwriting existing output and
					// overwrite overlaps partitions.
					final boolean foundControlCharacter;
					final int partEnd;
					if (controlCharacterMatcher != null && controlCharacterMatcher.find()) {
						if (ASSERT) {
							// check used pattern. Assert it matches only sequences of same characters.
							final String match = controlCharacterMatcher.group();
							Assert.isTrue(match.length() > 0);
							final char matchedChar = match.charAt(0);
							for (char c : match.toCharArray()) {
								Assert.isTrue(c == matchedChar);
							}
						}
						partEnd = controlCharacterMatcher.start();
						foundControlCharacter = true;
					} else {
						partEnd = text.length();
						foundControlCharacter = false;
					}

					partititonContent(stream, text, textOffset, partEnd, content);
					textOffset = partEnd;

					// finished processing of regular content before control characters
					// now interpret control characters if any
					if (controlCharacterMatcher != null && foundControlCharacter) {
						// at first update console document since it is easier to interpret control
						// characters on an up-to-date document and partitioning
						applyOutputToDocument(content.toString(), nextWriteOffset, replaceLength);
						content.setLength(0);
						replaceLength = 0;
						nextWriteOffset = outputOffset;

						final String controlCharacterMatch = controlCharacterMatcher.group();
						final char controlCharacter = controlCharacterMatch.charAt(0);
						final int outputLineStartOffset = findOutputLineStartOffset(outputOffset);
						switch (controlCharacter) {
						case '\b':
							// move virtual output cursor one step back for each \b
							// but stop at current line start and skip any input partitions
							int backStepCount = controlCharacterMatch.length();
							if (partitions.isEmpty()) {
								outputOffset = 0;
								break;
							}
							if (atOutputPartition == null) {
								atOutputPartitionIndex = partitions.size() - 1;
								atOutputPartition = getPartitionByIndex(atOutputPartitionIndex);
							}
							while (backStepCount > 0 && outputOffset > outputLineStartOffset) {
								if (atOutputPartition != null && isInputPartition(atOutputPartition)) {
									do {
										outputOffset = atOutputPartition.getOffset() - 1;
										atOutputPartitionIndex--;
										atOutputPartition = getPartitionByIndex(atOutputPartitionIndex);
									} while (atOutputPartition != null && isInputPartition(atOutputPartition));
									backStepCount--;
								}
								if (atOutputPartition == null) {
									outputOffset = 0;
									break;
								}
								final int backSteps = Math.min(outputOffset - atOutputPartition.getOffset(),
										backStepCount);
								outputOffset -= backSteps;
								backStepCount -= backSteps;
								atOutputPartitionIndex--;
								atOutputPartition = getPartitionByIndex(atOutputPartitionIndex);
							}
							outputOffset = Math.max(outputOffset, outputLineStartOffset);
							nextWriteOffset = outputOffset;
							break;

						case '\r':
							// move virtual output cursor to start of output line
							outputOffset = outputLineStartOffset;
							atOutputPartitionIndex = -1;
							atOutputPartition = null;
							nextWriteOffset = outputOffset;
							break;

						case '\f':
						case '\u000b': // \v
							// Vertical tab does not override existing content. It will introduce a newline
							// (at the end of current line even if output offset is inside the line) and
							// indent the new line dependent on current output offset.
							int indention = outputOffset - outputLineStartOffset;
							final int vtabCount = controlCharacterMatch.length();
							final StringBuilder vtab = new StringBuilder(indention + vtabCount);
							for (int i = 0; i < vtabCount; i++) {
								vtab.append(System.lineSeparator());
							}
							for (int i = 0; i < indention; i++) {
								vtab.append(' ');
							}
							outputOffset = document.getLength();
							nextWriteOffset = outputOffset;
							partititonContent(stream, vtab, 0, vtab.length(), content);
							break;

						case 0:
							// Do nothing for null bytes. The use of this is that a null byte which reach
							// the IOConsoleViewer will truncate the line on some platforms and will disturb
							// copying text on most platforms.
							// This case should simply filter out any null bytes.
							break;

						default:
							// should never happen as long as the used regex pattern is valid
							log(IStatus.ERROR, "No implementation to handle control character 0x" //$NON-NLS-1$
									+ Integer.toHexString(controlCharacter));
							break;
						}
						textOffset = controlCharacterMatcher.end();
					}
				}
			}
			applyOutputToDocument(content.toString(), nextWriteOffset, replaceLength);
		}

		/**
		 * If {@link IOConsolePartitioner#outputOffset} is at end of current content it
		 * will simply append the new partition or extend the last existing if
		 * applicable.
		 * <p>
		 * If output offset is within existing content the method will overwrite
		 * existing content and handle all required replacements and adjustments of
		 * existing partitions.
		 * </p>
		 *
		 * @param stream    the stream the to be partitioned content belongs to aka the
		 *                  stream which appended the content
		 * @param text      the text to partition. Depending on given offsets only a
		 *                  part of text is partitioned.
		 * @param offset    the start offset (inclusive) within text to partition
		 * @param endOffset the end offset (exclusive) within text to partition
		 */
		private void partititonContent(IOConsoleOutputStream stream, CharSequence text, int offset, int endOffset,
				StringBuilder content) {
			int textOffset = offset;
			while (textOffset < endOffset) {
				// Process content part. This part never contains control characters.
				// Processing may require multiple iterations if we overwrite existing content
				// which consists of distinct partitions.

				if (outputOffset >= document.getLength()) {
					// content is appended to document end (the easy case)
					if (atOutputPartition == null) {
						// get the last existing partition to try to expand it
						atOutputPartitionIndex = partitions.size() - 1;
						atOutputPartition = getPartitionByIndex(atOutputPartitionIndex);
						if (ASSERT) {
							Assert.isTrue(atOutputPartitionIndex == findPartitionCandidate(outputOffset - 1));
						}
					}
					if (atOutputPartition == null || !atOutputPartition.belongsTo(stream)) {
						// no partitions yet or last partition is incompatible to reuse -> add new one
						atOutputPartition = new IOConsolePartition(outputOffset, stream);
						partitions.add(atOutputPartition);
						atOutputPartitionIndex = partitions.size() - 1;
					}
					final int appendedLength = endOffset - textOffset;
					content.append(text, textOffset, endOffset);
					atOutputPartition.setLength(atOutputPartition.getLength() + appendedLength);
					outputOffset += appendedLength;
					textOffset = endOffset;
				} else {
					// content overwrites existing console content (the tricky case)
					if (atOutputPartition == null) {
						// find partition where output will overwrite or create one if unpartitioned
						atOutputPartitionIndex = findPartitionCandidate(outputOffset);
						atOutputPartition = getPartitionByIndex(atOutputPartitionIndex);
						if (atOutputPartition == null) {
							atOutputPartition = new IOConsolePartition(outputOffset, stream);
							atOutputPartitionIndex++;
							partitions.add(atOutputPartitionIndex, atOutputPartition);
						}
					}

					// we do not overwrite input partitions at the moment so they need to be skipped
					if (isInputPartition(atOutputPartition)) {
						outputOffset = atOutputPartition.getOffset() + atOutputPartition.getLength();
						atOutputPartitionIndex++;
						atOutputPartition = getPartitionByIndex(atOutputPartitionIndex);

						// apply document changes collected until now
						applyOutputToDocument(content.toString(), nextWriteOffset, replaceLength);
						content.setLength(0);
						replaceLength = 0;
						nextWriteOffset = outputOffset;
						continue; // to check if next selected partition is also input or appending now
					}

					// limit chunks to overwrite only one existing partition at a time
					final int chunkLength = Math.min(endOffset - textOffset,
							atOutputPartition.getLength() - (outputOffset - atOutputPartition.getOffset()));
					Assert.isTrue(chunkLength > 0); // do not remove since it can prevent an infinity loop

					if (!atOutputPartition.belongsTo(stream)) {
						// new output is from other stream then overwritten output

						// Note: this implementation ignores the possibility to reuse the partition
						// where the overwrite chunk ends and expand it towards replace begin since this
						// makes things code much more complex. In some cases this may leads to
						// consecutive partitions which could be merged to one partition. Merging is not
						// implemented at the moment.

						// in this part outputPartition is used to partition the new content
						// and atOutputPartition points to the partition whose content is overwritten
						// i.e. the new partition grows and the old one must shrink
						IOConsolePartition outputPartition = null;
						if (atOutputPartition.getOffset() == outputOffset) {
							// try to expand the partition before our output offset
							outputPartition = getPartitionByIndex(atOutputPartitionIndex - 1);
						} else {
							// overwrite starts inside existing incompatible partition
							atOutputPartition = splitPartition(outputOffset);
							atOutputPartitionIndex++;
						}
						if (outputPartition == null || !outputPartition.belongsTo(stream)) {
							outputPartition = new IOConsolePartition(outputOffset, stream);
							partitions.add(atOutputPartitionIndex, outputPartition);
							atOutputPartitionIndex++;
						}

						// update partitioning of the overwritten chunk
						outputPartition.setLength(outputPartition.getLength() + chunkLength);
						atOutputPartition.setOffset(atOutputPartition.getOffset() + chunkLength);
						atOutputPartition.setLength(atOutputPartition.getLength() - chunkLength);

						if (atOutputPartition.getLength() == 0) {
							// overwritten partition is now empty and must be be removed
							partitions.remove(atOutputPartitionIndex);
							atOutputPartition = getPartitionByIndex(atOutputPartitionIndex);
						}
					}
					content.append(text, textOffset, textOffset + chunkLength);
					replaceLength += chunkLength;
					textOffset += chunkLength;
					outputOffset += chunkLength;
					if (atOutputPartition != null
							&& outputOffset == atOutputPartition.getOffset() + atOutputPartition.getLength()) {
						atOutputPartitionIndex++;
						atOutputPartition = getPartitionByIndex(atOutputPartitionIndex);
					}
				}
			}
		}

		/**
		 * Find offset of line start from given output offset. This method ignores line
		 * breaks partitioned as input. I.e. it looks at the document as if it only
		 * consist of the output parts.
		 *
		 * @param outOffset offset where output should be written
		 * @return the start offset of line where output should be written
		 */
		private int findOutputLineStartOffset(int outOffset) {
			int outputLineStartOffset = 0;
			try {
				for (int lineIndex = document.getLineOfOffset(outOffset); lineIndex >= 0; lineIndex--) {
					outputLineStartOffset = document.getLineOffset(lineIndex);
					final IOConsolePartition lineBreakPartition = getIOPartition(outputLineStartOffset - 1);
					if (lineBreakPartition == null || !isInputPartition(lineBreakPartition)) {
						break;
					}
				}
			} catch (BadLocationException e) {
				log(e);
				outputLineStartOffset = 0;
			}
			if (ASSERT) {
				Assert.isTrue(outputLineStartOffset <= outOffset);
			}
			return outputLineStartOffset;
		}

		/**
		 * Apply content from output streams to document. It expects the partitioning
		 * has or will update partitioning to reflect the change since it prevents this
		 * partitioner's {@link #documentChanged2(DocumentEvent)} method from changing
		 * partitioning.
		 *
		 * @param text   collected content from output streams; not <code>null</code>
		 * @param offset offset in document where content is inserted
		 * @param length length of overwritten old output
		 */
		private void applyOutputToDocument(String text, int offset, int length) {
			if (text.length() > 0 || length > 0) {
				if (ASSERT) {
					Assert.isTrue(length <= text.length());
				}
				try {
					updateType = DocUpdateType.OUTPUT;
					document.replace(offset, length, text);
				} catch (BadLocationException e) {
					log(e);
				}
			}
		}
	}

	/**
	 * Job to trim the console document, runs in the UI thread.
	 */
	private class TrimJob extends WorkbenchJob {

		/** Trims output up to given offset. */
		private int truncateOffset;

		/**
		 * If <code>true</code> trim only to start of line containing the
		 * {@link #truncateOffset}.
		 */
		private boolean truncateToOffsetLineStart;

		/**
		 * Creates a new job to trim the buffer.
		 */
		TrimJob() {
			super("Trim Job"); //$NON-NLS-1$
			setSystem(true);
		}

		/**
		 * Sets the trim offset.
		 *
		 * @param offset trims console content up to this offset
		 */
		public void setTrimOffset(int offset) {
			// XXX an arbitrary offset would cause follow up errors if we trim a delim
			// exactly between \r and \n
			truncateOffset = offset;
			truncateToOffsetLineStart = false;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			synchronized (partitions) {
				trim(truncateOffset, truncateToOffsetLineStart);
			}
			return Status.OK_STATUS;
		}

	}

	private void trim(int truncateOffset, boolean truncateToOffsetLineStart) {
		if (document != null) {
			{
				try {
					int length = document.getLength();
					int cutOffset = truncateOffset;
					if (truncateToOffsetLineStart) {
						int cutoffLine = document.getLineOfOffset(truncateOffset);
						cutOffset = document.getLineOffset(cutoffLine);
					}
					if (cutOffset >= length) {
						updateType = DocUpdateType.TRIM;
						document.set(""); //$NON-NLS-1$
					} else {
						// set the new length of the first partition
						IOConsolePartition partition = getIOPartition(cutOffset);
						partition.setLength(partition.getOffset() + partition.getLength() - cutOffset);

						updateType = DocUpdateType.TRIM;
						document.replace(0, cutOffset, ""); //$NON-NLS-1$

						// remove partitions and reset Partition offsets
						int index = partitions.indexOf(partition);
						for (int i = 0; i < index; i++) {
							partitions.remove(0);
						}

						int offset = 0;
						for (IOConsolePartition p : partitions) {
							p.setOffset(offset);
							offset += p.getLength();
						}

						// fix output offset
						int removedLength = cutOffset;
						outputOffset = Math.max(outputOffset - removedLength, 0);
					}
					if (ASSERT) {
						checkPartitions();
					}
				} catch (BadLocationException e) {
					log(e);
				}
			}
		}
	}

	@Override
	public boolean isReadOnly(int offset) {
		final IOConsolePartition partition = getIOPartition(offset);
		return partition != null ? partition.isReadOnly() : true;
	}

	@Override
	public StyleRange[] getStyleRanges(int offset, int length) {
		final IOConsolePartition[] computedPartitions = computeIOPartitioning(offset, length);
		final StyleRange[] styles = new StyleRange[computedPartitions.length];
		for (int i = 0; i < computedPartitions.length; i++) {
			int rangeStart = computedPartitions[i].getOffset();
			int rangeLength = computedPartitions[i].getLength();

			// snap partitions to requested range
			final int underflow = offset - rangeStart;
			if (underflow > 0) {
				rangeStart += underflow;
				rangeLength -= underflow;
			}
			final int overflow = (rangeStart + rangeLength) - (offset + length);
			if (overflow > 0) {
				rangeLength -= overflow;
			}

			styles[i] = computedPartitions[i].getStyleRange(rangeStart, rangeLength);
		}
		return styles;
	}

	@Override
	public ITypedRegion[] computeReadOnlyPartitions() {
		if (document == null) {
			return new IOConsolePartition[0];
		}
		return computeReadOnlyPartitions(0, document.getLength());
	}

	@Override
	public ITypedRegion[] computeReadOnlyPartitions(int offset, int length) {
		return computePartitioning(offset, length, false, true);
	}

	@Override
	public ITypedRegion[] computeWritablePartitions() {
		if (document == null) {
			return new IOConsolePartition[0];
		}
		return computeWritablePartitions(0, document.getLength());
	}

	@Override
	public ITypedRegion[] computeWritablePartitions(int offset, int length) {
		return computePartitioning(offset, length, true, false);
	}

	@Override
	public boolean isReadOnly(int offset, int length) {
		final ITypedRegion[] readOnlyRegions = computeReadOnlyPartitions(offset, length);
		int o = offset;
		int end = offset + length;
		for (ITypedRegion readOnlyRegion : readOnlyRegions) {
			if (o < readOnlyRegion.getOffset()) {
				return false;
			}
			o += readOnlyRegion.getLength();
			if (o >= end) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsReadOnly(int offset, int length) {
		return computeReadOnlyPartitions(offset, length).length > 0;
	}

	@Override
	public int getPreviousOffsetByState(int offset, boolean searchWritable) {
		synchronized (partitions) {
			int partitionIndex = findPartitionCandidate(offset);
			for (; partitionIndex >= 0; partitionIndex--) {
				final IOConsolePartition partition = partitions.get(partitionIndex);
				if (partition.isReadOnly() != searchWritable) {
					return Math.min(partition.getOffset() + partition.getLength() - 1, offset);
				}
			}
		}
		return -1;
	}

	@Override
	public int getNextOffsetByState(int offset, boolean searchWritable) {
		synchronized (partitions) {
			int partitionIndex = findPartitionCandidate(offset);
			if (partitionIndex >= 0) {
				for (; partitionIndex < partitions.size(); partitionIndex++) {
					final IOConsolePartition partition = partitions.get(partitionIndex);
					if (partition.isReadOnly() != searchWritable) {
						return Math.max(partition.getOffset(), offset);
					}
				}
			}
		}
		return document != null ? document.getLength() : 0;
	}

	/**
	 * Check if console currently interprets ASCII control characters.
	 *
	 * @return <code>true</code> if console interprets ASCII control characters
	 * @since 3.9
	 */
	public boolean isHandleControlCharacters() {
		return controlCharacterPattern != null;
	}

	/**
	 * Enable or disable interpretation of ASCII control characters like backspace
	 * (<code>\b</code>).
	 *
	 * @param handleControlCharacters interpret control characters if
	 *                                <code>true</code>
	 * @since 3.9
	 */
	public void setHandleControlCharacters(boolean handleControlCharacters) {
		if (handleControlCharacters) {
			controlCharacterPattern = Pattern
					.compile(carriageReturnAsControlCharacter ? CONTROL_CHARACTERS_WITH_CR_PATTERN_STR
							: CONTROL_CHARACTERS_PATTERN_STR);
		} else {
			controlCharacterPattern = null;
		}
	}

	/**
	 * Check if carriage returns (<code>\r</code>) are interpreted as control
	 * characters. They are also not interpreted if general control character
	 * handling is disabled.
	 *
	 * @return if <code>true</code> carriage returns are interpreted as control
	 *         characters.
	 * @see #isHandleControlCharacters()
	 * @since 3.9
	 */
	public boolean isCarriageReturnAsControlCharacter() {
		return carriageReturnAsControlCharacter;
	}

	/**
	 * If control characters are interpreted by this console carriage returns
	 * (<code>\r</code>) are either ignored (<code>false</code>) and usually handled
	 * as line break by connected console document or if <code>true</code>
	 * interpreted with there control character meaning.
	 * <p>
	 * Note: this option has no effect if control character interpretation is
	 * disabled in general.
	 * </p>
	 *
	 * @param carriageReturnAsControlCharacter set <code>false</code> to exclude
	 *                                         carriage return from control
	 *                                         character interpretation
	 * @see #setHandleControlCharacters(boolean)
	 * @since 3.9
	 */
	public void setCarriageReturnAsControlCharacter(boolean carriageReturnAsControlCharacter) {
		this.carriageReturnAsControlCharacter = carriageReturnAsControlCharacter;
		// reset to update control character pattern
		setHandleControlCharacters(isHandleControlCharacters());
	}

	/**
	 * Get a partition by its index. Safe from out of bounds exceptions.
	 *
	 * @param index index of requested partition
	 * @return the requested partition or <code>null</code> if index is invalid
	 */
	private IOConsolePartition getPartitionByIndex(int index) {
		return (index >= 0 && index < partitions.size()) ? partitions.get(index) : null;
	}

	/**
	 * Check if given partition is from type input partition.
	 *
	 * @param partition partition to check (not <code>null</code>)
	 * @return true if partition is an input partition
	 */
	private static boolean isInputPartition(IOConsolePartition partition) {
		return IOConsolePartition.INPUT_PARTITION_TYPE.equals(partition.getType());
	}

	private static void log(Throwable t) {
		ConsolePlugin.log(t);
	}

	private static void log(int status, String msg) {
		ConsolePlugin.log(new Status(status, ConsolePlugin.getUniqueIdentifier(), msg));
	}

	private static void log(int status, String msg, Throwable t) {
		ConsolePlugin.log(new Status(status, ConsolePlugin.getUniqueIdentifier(), msg, t));
	}

	/**
	 * For debug purpose. Check if whole document is partitioned, partitions are
	 * ordered by offset, every partition has length greater 0 and all writable
	 * input partitions are listed in {@link #inputPartitions}.
	 */
	private void checkPartitions() {
		if (document == null) {
			return;
		}
		synchronized (partitions) {
			final List<IOConsolePartition> knownInputPartitions = new ArrayList<>(inputPartitions);
			int offset = 0;
			for (IOConsolePartition partition : partitions) {
				Assert.isTrue(offset == partition.getOffset());
				Assert.isTrue(partition.getLength() > 0);
				offset += partition.getLength();

				if (isInputPartition(partition) && !partition.isReadOnly()) {
					Assert.isTrue(knownInputPartitions.remove(partition));
				}
			}
			Assert.isTrue(offset == document.getLength());
			Assert.isTrue(knownInputPartitions.isEmpty());
		}
	}
}
