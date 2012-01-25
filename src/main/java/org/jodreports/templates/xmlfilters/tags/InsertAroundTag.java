package org.jodreports.templates.xmlfilters.tags;

import org.jodreports.templates.xmlfilters.AbstractInsertTag;
import nu.xom.Element;
import nu.xom.Node;

public class InsertAroundTag extends AbstractInsertTag {

	public void process(Element scriptElement, Element tagElement) {
		Node beforeNode = newNode(tagElement.getChild(0).getValue());
		Node afterNode = newNode(tagElement.getChild(2).getValue());
		insertBefore(scriptElement, tagElement, (Node) beforeNode.copy());
		insertAfter(scriptElement, tagElement, (Node) afterNode.copy());
	}
}
