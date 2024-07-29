package free.mustache;

import java.io.File;
import java.io.IOException;

/**
 * @author lidong@date 2024-07-24@version 1.0
 */
public class Main {
    static File mustacheFile = new File("/Users/lidong/gitspace/antlrmustache/src/main/resources/ddl.mustache");
    public static void main(String[] args) throws IOException {
        test1();
    }




    private static void test1() throws IOException {
        MustacheCompiler mustacheCompiler = new MustacheCompiler(mustacheFile);
        mustacheCompiler.compile();
    }
}
