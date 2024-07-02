/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.tests.filesystem.zip;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConcurrencyTest {

	@BeforeEach
	public void setup() throws Exception {
		ZipFileSystemTestSetup.defaultSetup();
	}

	@AfterEach
	public void teardown() throws Exception {
		ZipFileSystemTestSetup.teardown();
	}

	@Test
	public void testFetchInfoWithMultipleThreads() throws Exception {
		IFolder openedZipFile = ZipFileSystemTestSetup.firstProject
				.getFolder(ZipFileSystemTestSetup.ZIP_FILE_VIRTUAL_FOLDER_NAME);
		ZipFileSystemTestUtil.ensureExists(openedZipFile);
		URI zipFileURI = openedZipFile.getLocationURI();
		IFileStore zipFileStore = EFS.getStore(zipFileURI);

		int totalThreadCount = 10;
		CountDownLatch startLatch = new CountDownLatch(1); // Latch to start all threads simultaneously
		CountDownLatch doneLatch = new CountDownLatch(totalThreadCount); // Latch to wait for all threads to finish
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(totalThreadCount);

		// Shared exception reference to propagate exceptions from threads to the main
		// thread
		AtomicReference<Exception> exceptionReference = new AtomicReference<>();

		// Submit tasks to the executor
		for (int i = 0; i < totalThreadCount; i++) {
			executor.submit(() -> {
				try {
					startLatch.await(); // Wait for the signal to start
					// Perform various tasks on ZipFileStore
					for (int j = 0; j <= 10; j++) {
						zipFileStore.childInfos(0, ZipFileSystemTestUtil.getMonitor());
						assertTrue(zipFileStore.fetchInfo().exists(), "File system should exist");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					fail("Thread was interrupted");
					Thread.currentThread().interrupt();
				} catch (CoreException e) {
					// Propagate CoreException to the main thread
					exceptionReference.set(e);
				} finally {
					doneLatch.countDown();
				}
			});
		}

		// Start all threads
		startLatch.countDown();

		// Wait for all tasks to complete
		doneLatch.await();

		executor.shutdown();

		// Check if any exception was thrown by the threads
		if (exceptionReference.get() != null) {
			throw exceptionReference.get();
		}

		assertTrue(executor.getCompletedTaskCount() == totalThreadCount, "All tasks should complete successfully");
	}
}
