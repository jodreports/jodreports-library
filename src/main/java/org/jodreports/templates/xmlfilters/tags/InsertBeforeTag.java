package org.jodreports.templates.xmlfilters.tags;

import org.jodreports.templates.xmlfilters.AbstractInsertTag;
import nu.xom.Element;
import nu.xom.Node;

public class InsertBeforeTag extends AbstractInsertTag {

	public void process(Element scriptElement, Element tagElement) {
		Node node = newNode(tagElement.getValue());
		insertBefore(scriptElement, tagElement, node);
	}
}
