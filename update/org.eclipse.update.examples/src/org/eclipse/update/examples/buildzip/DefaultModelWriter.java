package org.eclipse.update.examples.buildzip;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */ 
 
import java.io.OutputStream;
import java.io.PrintWriter;

import org.eclipse.update.core.model.*;

/**
 * Write standard feature manifest (feature.xml) from model.
 * </p>
 * @since 2.0
 */

public class DefaultModelWriter {
	
	private FeatureModel feature;
	
	/**
	 * @since 2.0
	 */
	public DefaultModelWriter(FeatureModel feature) {
		this.feature = feature;
	}
	
	/**
	 * @since 2.0
	 */
	public void writeFeatureManifest(OutputStream os) {
		PrintWriter w = new PrintWriter(os);
		writeFeature(w,0,feature);
		w.flush();
	}
	
	private void writeFeature(PrintWriter w, int level, FeatureModel feature) {
		if (feature == null) return;
		
		String in = getIndent(level);
		w.println(in+"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		
		w.println(in+"<feature");
		if (feature.getFeatureIdentifier()!=null)
			w.println(in+"   "+"id=\""+feature.getFeatureIdentifier()+"\"");
		if (feature.getFeatureVersion()!=null)
			w.println(in+"   "+"version=\""+feature.getFeatureVersion()+"\"");
		if (feature.getLabelNonLocalized()!=null)
			w.println(in+"   "+"label=\""+feature.getLabelNonLocalized()+"\"");
		if (feature.getProviderNonLocalized()!=null)
			w.println(in+"   "+"provider-name=\""+feature.getProviderNonLocalized()+"\"");
		if (feature.getImageURLString()!=null)
			w.println(in+"   "+"image=\""+feature.getImageURLString()+"\"");
		if (feature.getOS()!=null)
			w.println(in+"   "+"os=\""+feature.getOS()+"\"");
		if (feature.getWS()!=null)
			w.println(in+"   "+"ws=\""+feature.getWS()+"\"");
		if (feature.getNL()!=null)
			w.println(in+"   "+"nl=\""+feature.getNL()+"\"");
		if (feature.getApplication()!=null)
			w.println(in+"   "+"application=\""+feature.getApplication()+"\"");
		w.println(in+"   "+">");
		
		writeDescription(w, level+1, feature.getDescriptionModel());
		writeCopyright(w, level+1, feature.getCopyrightModel());
		writeLicense(w, level+1, feature.getLicenseModel());
		writeURLs(w, level+1, feature);
		writePrereqs(w, level+1, feature);
		writePluginEntries(w, level+1, feature);
		writeNonPluginEntries(w, level+1, feature);
         
        w.println("");
        w.println(in+"</feature>");
	}
	
	private void writeDescription(PrintWriter w, int level, URLEntryModel ue) {
		writeDescriptionEntry(w,level, ue, "description");
	}
	
	private void writeCopyright(PrintWriter w, int level, URLEntryModel ue) {
		writeDescriptionEntry(w,level, ue, "copyright");
	}
	
	private void writeLicense(PrintWriter w, int level, URLEntryModel ue) {
		writeDescriptionEntry(w,level, ue, "license");
	}
	
	private void writeDescriptionEntry(PrintWriter w, int level, URLEntryModel ue, String tag) {
		if (ue == null) return;
		String url = ue.getURLString();
		String txt = ue.getAnnotationNonLocalized();
		if (url==null && txt==null) return;
		
		String in = getIndent(level);
		w.println("");
		if (url==null)
			w.println(in+"<"+tag+">");
		else {
			w.print(in+"<"+tag+" url=\""+ue.getURLString()+"\"");
			if (txt==null)
				w.println("/>");
			else
				w.println(">");
		}
		if (txt!=null) {
			w.println(in + txt);
			w.println(in+"</"+tag+">");
		}
	}
	
	private void writeURLs(PrintWriter w, int level, FeatureModel feature) {	
		URLEntryModel update = feature.getUpdateSiteEntryModel();
		URLEntryModel[] discovery = feature.getDiscoverySiteEntryModels();		
		if (update == null && (discovery == null || discovery.length==0)) return;
		
		String in = getIndent(level);
		String in2 = getIndent(level+1);		
		w.println("");
		w.println(in+"<url>");
		
		if (update != null) {
			w.println(in2+"<update");
			if (update.getURLString()!=null)
				w.println(in2+"   "+"url=\""+update.getURLString()+"\"");
			if (update.getAnnotationNonLocalized()!=null)
				w.println(in2+"   "+"label=\""+update.getAnnotationNonLocalized()+"\"");
			w.println(in2+"   "+"/>");
		}
		
		for (int i=0; discovery!=null && i<discovery.length; i++) {
			w.println(in2+"<discovery");
			if (discovery[i].getURLString()!=null)	
				w.println(in2+"   "+"url=\""+discovery[i].getURLString()+"\"");
			if (discovery[i].getAnnotationNonLocalized()!=null)
				w.println(in2+"   "+"label=\""+discovery[i].getAnnotationNonLocalized()+"\"");
			w.println(in2+"   "+"/>");
		}
		
		w.println(in+"</url>");
	}
	
	private void writePrereqs(PrintWriter w, int level, FeatureModel feature) {	
		ImportModel[] imp = feature.getImportModels();	
		if (imp == null || imp.length == 0) return;
		
		String in = getIndent(level);
		String in2 = getIndent(level+1);
		w.println("");
		w.println(in+"<requires>");
		
		for (int i=0; imp!=null && i<imp.length; i++) {
			w.println(in2+"<import");
			if (imp[i].getIdentifier()!=null)
				w.println(in2+"   "+"plugin=\""+imp[i].getIdentifier()+"\"");
			if (imp[i].getVersion()!=null)
				w.println(in2+"   "+"version=\""+imp[i].getVersion()+"\"");
			if (imp[i].getMatchingRuleName()!=null)
				w.println(in2+"   "+"match=\""+imp[i].getMatchingRuleName()+"\"");
			w.println(in2+"   "+"/>");
		}
		
		w.println(in+"</requires>");
	}
	
	private void writePluginEntries(PrintWriter w, int level, FeatureModel feature) {
		PluginEntryModel[] plugin = feature.getPluginEntryModels();
		if (plugin == null || plugin.length == 0) return;
		
		String in = getIndent(level);
		w.println("");
		
		for (int i=0; i<plugin.length; i++) {
			w.println(in+"<plugin");
			if (plugin[i].getPluginIdentifier()!=null)
				w.println(in+"   "+"id=\""+plugin[i].getPluginIdentifier()+"\"");
			if (plugin[i].getPluginVersion()!=null)
				w.println(in+"   "+"version=\""+plugin[i].getPluginVersion()+"\"");
			if (plugin[i].isFragment())
				w.println(in+"   "+"fragment=\""+plugin[i].isFragment()+"\"");
			if (plugin[i].getOS()!=null)
				w.println(in+"   "+"os=\""+plugin[i].getOS()+"\"");
			if (plugin[i].getWS()!=null)
				w.println(in+"   "+"ws=\""+plugin[i].getWS()+"\"");
			if (plugin[i].getNL()!=null)
				w.println(in+"   "+"nl=\""+plugin[i].getNL()+"\"");
			if (plugin[i].getDownloadSize()!=ContentEntryModel.UNKNOWN_SIZE)
				w.println(in+"   "+"download-size=\""+plugin[i].getDownloadSize()+"\"");
			if (plugin[i].getInstallSize()!=ContentEntryModel.UNKNOWN_SIZE)
				w.println(in+"   "+"install-size=\""+plugin[i].getInstallSize()+"\"");
			w.println(in+"   "+"/>");
		}
	}
	
	private void writeNonPluginEntries(PrintWriter w, int level, FeatureModel feature) {
		NonPluginEntryModel[] data = feature.getNonPluginEntryModels();
		if (data == null || data.length == 0) return;
		
		String in = getIndent(level);
		w.println("");
		
		for (int i=0; i<data.length; i++) {
			w.println(in+"<data");
			if (data[i].getIdentifier()!=null)
				w.println(in+"   "+"id=\""+data[i].getIdentifier()+"\"");
			if (data[i].getOS()!=null)
				w.println(in+"   "+"os=\""+data[i].getOS()+"\"");
			if (data[i].getWS()!=null)
				w.println(in+"   "+"ws=\""+data[i].getWS()+"\"");
			if (data[i].getNL()!=null)
				w.println(in+"   "+"nl=\""+data[i].getNL()+"\"");
			if (data[i].getDownloadSize()!=ContentEntryModel.UNKNOWN_SIZE)
				w.println(in+"   "+"download-size=\""+data[i].getDownloadSize()+"\"");
			if (data[i].getInstallSize()!=ContentEntryModel.UNKNOWN_SIZE)
				w.println(in+"   "+"install-size=\""+data[i].getInstallSize()+"\"");
			w.println(in+"   "+"/>");
		}
	}
	
	
	private String getIndent(int level) {
		String indent = "";
		for (int i=0; i<level; i++) 
			indent += "   ";
		return indent;
	}
}
