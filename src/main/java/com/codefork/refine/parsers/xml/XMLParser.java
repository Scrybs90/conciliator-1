package com.codefork.refine.parsers.xml;

import com.codefork.refine.parsers.ParseState;
import com.codefork.refine.resources.Result;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * SAX parser handler. We use SAX b/c it's faster than loading a whole DOM.
 */
public abstract class XMLParser<R extends ParseState> extends DefaultHandler {

    protected Map<String, StartElementHandler<R>> startElementHandlers = new HashMap<>();
    protected Map<String, EndElementHandler<R>> endElementHandlers = new HashMap<>();

    public final Stack<String> path = new Stack<>();

    protected R parseState;

    public XMLParser() {
        parseState = createParseState();
    }

    /**
     * XMLParser subclasses should override this if they want to create their
     * own ParseState subclasses.
     * @return
     */
    public abstract R createParseState();

    public List<Result> getResults() {
        return parseState.results;
    }

    public R getParseState() {
        return parseState;
    }

    public String getPath() {
        StringBuilder buf = new StringBuilder();
        String delim = "";
        for(String part : path) {
            buf.append(delim);
            buf.append(part);
            delim = "/";
        }
        return buf.toString();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        // strip ns prefix
        String name = qName;
        if(name.contains(":")) {
            name = qName.split("\\:")[1];
        }
        path.push(name);

        String path = getPath();
        //System.out.println(path);

        StartElementHandler<R> handler = startElementHandlers.get(path);
        if(handler != null) {
            handler.handle(parseState, uri, localName, qName, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        String path = getPath();
        //System.out.println(path);

        EndElementHandler<R> handler = endElementHandlers.get(path);
        if(handler != null) {
            handler.handle(parseState, uri, localName, qName);
        }

        this.path.pop();
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if(parseState.captureChars) {
            parseState.buf.append(ch, start, length);
        }
    }

}
