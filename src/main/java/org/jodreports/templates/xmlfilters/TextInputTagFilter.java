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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// http://www.gnu.org/copyleft/lesser.html
//
package org.jodreports.templates.xmlfilters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jodreports.templates.Configuration;
import org.jodreports.templates.DocumentTemplateException;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.Text;

/**
 * OpenDocument XML file filter that replaces text-input elements with
 * FreeMarker expressions.
 * <p>
 * For example:
 * <p>
 * <tt>&lt;text:text-input text-description="JOOScript"&gt;$user.name&lt;/text:text-input&gt;</tt>
 * becomes <tt>${user.name}</tt>.
 * <p>
 * <tt>&lt;text:text-input text-description="JOOScript"&gt;[#assign title='Mr.']&lt;/text:text-input&gt;</tt>
 * becomes <tt>[#assign title='Mr.']</tt>.
 */
public class TextInputTagFilter extends XmlEntryFilter
{

    private boolean processJooScriptOnly = true;

    public void applyConfigurations(Map<String, Object> configurations)
    {
        super.applyConfigurations(configurations);
        this.processJooScriptOnly = Configuration.getConfiguration(Configuration.SETTING_PROCESS_JOOSCRIPT_ONLY,
                configurations);
    }

    public void doFilter(Document document) throws DocumentTemplateException
    {
        Nodes textInputNodes = document.query("//text:text-input", XPATH_CONTEXT);
        
        
        for (int nodeIndex = 0; nodeIndex < textInputNodes.size(); nodeIndex++)
        {
            Element textInputElement = (Element) textInputNodes.get(nodeIndex);
            String expression = textInputElement.getAttributeValue("description", textInputElement.getNamespaceURI())
                    .trim();
            
            
            if (expression.equalsIgnoreCase("jooscript"))
            {
                String value = textInputElement.getValue();
                
                
                if (value.startsWith("${"))
                {
                    textInputElement.getParent().replaceChild(textInputElement, new Text(value));
                }
                else
                {
                    try
                    {
                        String script = addScriptDirectives(textInputElement);
                        textInputElement.getParent().replaceChild(textInputElement, newNode(script));
                    }
                    catch (IOException ioException)
                    {
                        textInputElement.detach();
                    }
                }
            }
            else if (!processJooScriptOnly)
            {
                if (expression.length() > 0 && !expression.startsWith("${"))
                {
                    expression = "${" + expression + "}";
                }
                textInputElement.getParent().replaceChild(textInputElement, new Text(expression));
            }
        }
    }
    
    
    
    //
    // Provide same functionality for TextInput as occurs on Script Elements.
    //
    private static class ScriptPart
    {

        private StringBuffer text = new StringBuffer();
        private String location;
        private Boolean isEndTag;

        public ScriptPart()
        {
            // no location
        }

        public ScriptPart(String location, Boolean isEndTag)
        {
            this.location = location;
            this.isEndTag = isEndTag;
        }

        public void appendText(String line)
        {
            text.append(line + "\n");
        }

        public String getText()
        {
            return text.toString().trim();
        }

        public String getLocation()
        {
            return location;
        }

        public boolean afterEndTag()
        {
            return (isEndTag != null && isEndTag == Boolean.TRUE);
        }

        public boolean isTagAttribute()
        {
            return (isEndTag != null && isEndTag == Boolean.FALSE);
        }
    }

    /**
     * @param scriptElement
     *
     * @return the text that should replace the input field
     *
     * @throws DocumentTemplateException
     */
    private static String addScriptDirectives(Element scriptElement) throws IOException, DocumentTemplateException
    {
        String scriptReplacement = "";
        List<ScriptPart> scriptParts = parseScriptParts(scriptElement.getValue());


        for (int index = 0; index < scriptParts.size(); index++)
        {
            ScriptPart scriptPart = (ScriptPart) scriptParts.get(index);
            
            
            if (scriptPart.getLocation() == null)
            {
                scriptReplacement = scriptPart.getText();
            }
            else
            {
                Element enclosingElement = findEnclosingElement(scriptElement, scriptPart.getLocation());


                if (scriptPart.isTagAttribute())
                {
                    String[] nameValue = scriptPart.getText().split("=", 2);
                    
                    
                    if (nameValue.length != 2)
                    {
                        throw new DocumentTemplateException("script error: # attribute name=value not found");
                    }

                    String attributeNamespace = enclosingElement.getNamespaceURI();
                    
                    
                    if (nameValue[0].contains(":"))
                    {
                        String prefix = nameValue[0].split(":")[0];
                        
                        
                        if (!prefix.equals(enclosingElement.getNamespacePrefix()))
                        {
                            attributeNamespace = XPATH_CONTEXT.lookup(prefix);
                            if (attributeNamespace == null)
                            {
                                throw new DocumentTemplateException("unsupported attribute namespace: " + prefix);
                            }
                        }
                    }

                    enclosingElement.addAttribute(new Attribute(nameValue[0], attributeNamespace, nameValue[1]));
                }
                else
                {
                    ParentNode parent = enclosingElement.getParent();
                    int parentIndex = parent.indexOf(enclosingElement);


                    if (scriptPart.afterEndTag())
                    {
                        parentIndex++;
                    }
                    parent.insertChild(newNode(scriptPart.getText()), parentIndex);
                }
            }
        }

        return scriptReplacement;
    }

    private static List<ScriptPart> parseScriptParts(String scriptText) throws IOException, DocumentTemplateException
    {
        List<ScriptPart> scriptParts = new ArrayList<ScriptPart>();
        BufferedReader stringReader = new BufferedReader(new StringReader(scriptText));
        ScriptPart scriptPart = new ScriptPart();


        scriptParts.add(scriptPart);
        for (String line; (line = stringReader.readLine()) != null;)
        {
            line = line.trim();
            if (line.startsWith("@"))
            {
                String location = line.substring(1);


                if (location.startsWith("/"))
                {
                    scriptPart = new ScriptPart(location.substring(1), Boolean.TRUE);
                }
                else if (location.startsWith("#"))
                {
                    scriptPart = new ScriptPart(location.substring(1), Boolean.FALSE);
                }
                else
                {
                    scriptPart = new ScriptPart(location, null);
                }
                scriptParts.add(scriptPart);
            }
            else
            {
                scriptPart.appendText(line.replaceFirst("^\\[#--", "[#comment]").replaceFirst("--\\]$", "[/#comment]"));
            }
        }
        return scriptParts;
    }

    private static Element findEnclosingElement(Element element, String enclosingTagName)
            throws DocumentTemplateException
    {
        Nodes ancestors = element.query("ancestor::" + enclosingTagName, XPATH_CONTEXT);
        
        
        if (ancestors.size() == 0)
        {
            ancestors = element.query("preceding::" + enclosingTagName, XPATH_CONTEXT);
            if (ancestors.size() == 0)
            {
                throw new DocumentTemplateException(
                        "script error: no such enclosing tag named '" + enclosingTagName + "'");
            }
        }
        return (Element) ancestors.get(ancestors.size() - 1);
    }
}
