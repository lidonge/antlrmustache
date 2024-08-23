package free.mustache;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author lidong@date 2023-11-22@version 1.0
 */
public abstract class AntlrCompiler {
    private URI antlrUri;

    public AntlrCompiler(URI antlrUri) {
        this.antlrUri = antlrUri;
    }

    public AntlrCompiler(File antlrFile) {
        this(antlrFile.toURI());
    }

    public abstract ParseTreeListener compile() throws IOException;

    protected abstract Parser getParser() throws IOException;

    protected void parseFile(Parser parser, IAntlrParserExecutor executor, ParseTreeListener listener) {
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.removeErrorListeners();
        parser.addErrorListener(new GeneratorErrorListener(antlrUri));

        ParseTree tree = null;
        try {
            tree = executor.execute(); // STAGE 1
//            System.out.println(tree.toStringTree());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ParseTreeWalker walker = new ParseTreeWalker();


        walker.walk(listener, tree);
    }

    protected CharStream getCharStream() throws IOException {
        String str = getString();
        CharStream stream = new ANTLRInputStream(str);
        return stream;
    }

    protected String getString() throws IOException {
        InputStream reader = getInputStreamFromURI(antlrUri);
        byte[] bytes = reader.readAllBytes();
        reader.close();
        String str = new String(bytes);
        return str;
    }

    public static InputStream getInputStreamFromURI(URI uri) throws IOException {
        switch (uri.getScheme()) {
            case "file":
                // 处理文件 URI
                return getInputStreamFromFile(uri);
            case "http":
            case "https":
                // 处理 HTTP/HTTPS URI
                return getInputStreamFromHttp(uri);
            case "jar":
                // 处理 JAR 文件中的资源 URI
                return getInputStreamFromJar(uri);
            default:
                throw new IllegalArgumentException("Unsupported URI scheme: " + uri.getScheme());
        }
    }

    private static InputStream getInputStreamFromFile(URI uri) throws IOException {
        File file = new File(uri);
        return Files.newInputStream(Paths.get(file.toURI()));
    }

    private static InputStream getInputStreamFromHttp(URI uri) throws IOException {
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getInputStream();
    }

    private static InputStream getInputStreamFromJar(URI uri) throws IOException {
        InputStream inputStream = uri.toURL().openStream();
        if (inputStream == null) {
            throw new IOException("Resource not found: " + uri);
        }
        return inputStream;
    }
}
