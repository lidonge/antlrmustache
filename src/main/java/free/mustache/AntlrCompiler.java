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

/**
 * @author lidong@date 2023-11-22@version 1.0
 */
public abstract class AntlrCompiler {
    File antlrFile;
    public AntlrCompiler(File antlrFile) {
        this.antlrFile = antlrFile;
    }

    public abstract ParseTreeListener compile() throws IOException;

    protected abstract Parser getParser() throws IOException;

    protected void parseFile(Parser parser, IAntlrParserExecutor executor, ParseTreeListener listener) {
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.removeErrorListeners();
        parser.addErrorListener(new GeneratorErrorListener(antlrFile));

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
        String sql = getString();
        CharStream stream = new ANTLRInputStream(sql);
        return stream;
    }

    protected String getString() throws IOException {
        FileInputStream reader = new FileInputStream(antlrFile);
        byte[] bytes = reader.readAllBytes();
        reader.close();
        String sql = new String(bytes);
        return sql;
    }
}
