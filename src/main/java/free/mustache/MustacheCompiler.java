package free.mustache;


import free.mustache.antlr.MustacheLexer;
import free.mustache.antlr.MustacheParser;
import free.mustache.handler.MustacheListenerImpl;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author lidong@date 2023-11-22@version 1.0
 */
public class MustacheCompiler extends AntlrCompiler {
    public MustacheCompiler(File cobolFile) {
        super(cobolFile);
    }

    public MustacheCompiler(URL url) throws URISyntaxException {
        super(url.toURI());
    }

    @Override
    protected Parser getParser() throws IOException {
        CharStream stream = getCharStream();
        MustacheLexer lexer1 = new MustacheLexer(stream);
        CommonTokenStream tokens1 = new CommonTokenStream(lexer1);
        MustacheParser parser1 = new MustacheParser(tokens1);
        return parser1;
    }
    @Override
    public MustacheListenerImpl compile() throws IOException {
        MustacheParser mustacheParser = (MustacheParser) getParser();
        MustacheListenerImpl listener = new MustacheListenerImpl();

        parseFile(mustacheParser, ()->{return mustacheParser.template();},listener);
        return listener;
    }
}
