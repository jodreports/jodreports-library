//
// JOOReports - The Open Source Java/OpenOffice Report Engine
// Copyright (C) 2004-2006 - Mirko Nasato <mirko@artofsolving.com>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// http://www.gnu.org/copyleft/lesser.html
//
package org.jodreports.templates;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jodreports.opendocument.OpenDocumentArchive;
import org.jodreports.opendocument.OpenDocumentIO;
import org.jodreports.templates.xmlfilters.DynamicImageFilter;
import org.jodreports.templates.xmlfilters.ScriptTagFilter;
import org.jodreports.templates.xmlfilters.TextInputTagFilter;
import org.jodreports.templates.xmlfilters.XmlEntryFilter;
import freemarker.template.Configuration;

public abstract class AbstractDocumentTemplate implements DocumentTemplate {

	private static final Configuration DEFAULT_FREEMARKER_CONFIGURATION = new Configuration();
	private static final ContentWrapper DEFAULT_CONTENT_WRAPPER = new ContentWrapper() {

		public String wrapContent(String content) {
			return "[#ftl]\n"
				+ "[#escape any as any?xml?replace(\"\\n\",\"<text:line-break />\")]\n"
				+ content
				+ "[/#escape]";
		}

	};

	private static final XmlEntryFilter[] DEFAULT_XML_ENTRY_FILTERS = new XmlEntryFilter[] {
		new TextInputTagFilter(),
		new ScriptTagFilter(),
		new DynamicImageFilter()
	};
	
	private final Configuration freemarkerConfiguration;
	private ContentWrapper contentWrapper = DEFAULT_CONTENT_WRAPPER;
	private XmlEntryFilter[] xmlEntryFilters = DEFAULT_XML_ENTRY_FILTERS;
	private String[] xmlEntries = new String[] {
		OpenDocumentArchive.ENTRY_CONTENT,
		OpenDocumentArchive.ENTRY_STYLES
	};
	private OpenDocumentArchive preProcessedTemplate;
	
	private Map openDocumentSettings = new HashMap();
	private Map configurations = new HashMap();
	
	public AbstractDocumentTemplate() {
		this(DEFAULT_FREEMARKER_CONFIGURATION);
	}

	public AbstractDocumentTemplate(Configuration freemarkerConfiguration) {
		this.freemarkerConfiguration = freemarkerConfiguration;
	}

	public void setXmlEntries(String[] xmlEntries) {
		this.xmlEntries = xmlEntries;
		Arrays.sort(this.xmlEntries);
	}

	public void setXmlEntryFilters(XmlEntryFilter[] xmlEntryFilters) {
		this.xmlEntryFilters = xmlEntryFilters;
	}
	
	public void setContentWrapper(ContentWrapper contentWrapper) {
		this.contentWrapper = contentWrapper;
	}

    protected abstract OpenDocumentArchive getOpenDocumentArchive();

	public void setOpenDocumentSettings(Map openDocumentSettings) {
		this.openDocumentSettings = openDocumentSettings;
	}

    public void createDocument(Object model, OutputStream output) throws IOException, DocumentTemplateException {
    	if (preProcessedTemplate == null) {
    		preProcess();
    	}
    	OpenDocumentArchive outputArchive = preProcessedTemplate.createCopy();
    	TemplateAndModelMerger templateAndModelMerger = new TemplateAndModelMerger(freemarkerConfiguration, xmlEntries, 
    			openDocumentSettings, configurations);
    	templateAndModelMerger.process(outputArchive, model);
    	
    	OpenDocumentIO.writeZip(outputArchive, output);
    }

    private void preProcess() throws IOException, DocumentTemplateException {
    	preProcessedTemplate = getOpenDocumentArchive();
    	TemplatePreProcessor templatePreProcessor = new TemplatePreProcessor(xmlEntries, xmlEntryFilters, contentWrapper, configurations);
    	templatePreProcessor.process(preProcessedTemplate);
    }

	public Map getConfigurations() {
		return configurations;
	}

}
