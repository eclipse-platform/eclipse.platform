package org.eclipse.update.tests.api;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.core.FeatureReference;
import org.eclipse.update.internal.core.FeatureExecutableContentProvider;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.tests.UpdateManagerTestCase;



public class TestFeatureContentProvider_FileFilterAPI extends UpdateManagerTestCase {
	
	private DefaultFeature remoteFeature=null;
	
	/**
	 * Test the testFeatures()
	 */
	public TestFeatureContentProvider_FileFilterAPI(String arg0) {
		super(arg0);
	}
	
	public void testFileFilter(){
		
		String p1 = "/p1/p2/a.*";
		String p2 = "p1/p2/b.*";
		String p3 = "/p1/p2/a.txt";
		String p4 = "/p1/p2/*.txt";
		
		String name1 = "/p1/p2/a.txt";
		String name2 = "/p1/p2/b.txt";
		String name3 = "/p3/p2/a.txt";
		String name4 = "p1/p2/a.txt";
		String name5 = "/p1/p2/b.txt";
		String name6 = "/p1/p2/c.txt";
		String name7 = "/p1/p2/p3/a.txt";
		
		
		FeatureContentProvider cp = new FeatureExecutableContentProvider(null);
		
		FeatureContentProvider.FileFilter filter1 = cp.new FileFilter(p1);
		FeatureContentProvider.FileFilter filter2 = cp.new FileFilter(p2);
		FeatureContentProvider.FileFilter filter3 = cp.new FileFilter(p3);
		FeatureContentProvider.FileFilter filter4 = cp.new FileFilter(p4);

		assertTrue("1.1",filter1.accept(name1));		
		assertTrue("1.2",!filter1.accept(name2));		
		assertTrue("1.3",!filter1.accept(name3));	
		assertTrue("1.4",!filter1.accept(name4));		
		assertTrue("1.5",!filter1.accept(name5));		
		assertTrue("1.6",!filter1.accept(name6));		
		assertTrue("1.7",!filter1.accept(name7));
	
		assertTrue("2.1",!filter2.accept(name1));		
		assertTrue("2.2",!filter2.accept(name2));		
		assertTrue("2.3",!filter2.accept(name3));	
		assertTrue("2.4",!filter2.accept(name4));		
		assertTrue("2.5",!filter2.accept(name5));		
		assertTrue("2.6",!filter2.accept(name6));		
		assertTrue("2.7",!filter2.accept(name7));

		assertTrue("3.1",filter3.accept(name1));		
		assertTrue("3.2",!filter3.accept(name2));		
		assertTrue("3.3",!filter3.accept(name3));	
		assertTrue("3.4",!filter3.accept(name4));		
		assertTrue("3.5",!filter3.accept(name5));		
		assertTrue("3.6",!filter3.accept(name6));		
		assertTrue("3.7",!filter3.accept(name7));
		
		assertTrue("4.1",filter4.accept(name1));		
		assertTrue("4.2",filter4.accept(name2));		
		assertTrue("4.3",!filter4.accept(name3));	
		assertTrue("4.4",!filter4.accept(name4));		
		assertTrue("4.5",filter4.accept(name5));		
		assertTrue("4.6",filter4.accept(name6));		
		assertTrue("4.7",!filter4.accept(name7));								
					
	}
}

