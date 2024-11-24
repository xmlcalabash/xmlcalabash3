package com.xmlcalabash.util;

import net.sf.saxon.s9api.Location;
import net.sf.saxon.s9api.XdmNode;

import java.net.URI;

public class BuilderLocation implements Location {
    String publicId = null;
    String systemId = null;
    int lineNumber = -1;
    int columnNumber = -1;

    public BuilderLocation(String systemId) {
        this.systemId = systemId;
    }

    public BuilderLocation(String systemId, int line, int col) {
        this.systemId = systemId;
        lineNumber = line;
        columnNumber = col;
    }

    public BuilderLocation(XdmNode node) {
        if (node.getBaseURI() != null) {
            systemId = node.getBaseURI().toString();
        }
        lineNumber = node.getLineNumber();
        columnNumber = node.getColumnNumber();
    }

    public BuilderLocation(XdmNode node, URI overrideBaseURI) {
        systemId = overrideBaseURI.toString();
        lineNumber = node.getLineNumber();
        columnNumber = node.getColumnNumber();
    }

    @Override
    public String getSystemId() {
        return systemId;
    }

    @Override
    public String getPublicId() {
        return publicId;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int getColumnNumber() {
        return columnNumber;
    }

    @Override
    public Location saveLocation() {
        return new BuilderLocation(systemId, lineNumber, columnNumber);
    }
}
