package free.mustache;


import free.mustache.handler.IPartialFileHandler;
import free.mustache.handler.MustacheListenerImpl;
import free.mustache.handler.MustacheWriter;
import free.mustache.model.BaseSection;
import free.mustache.model.Template;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lidong@date 2023-11-18@version 1.0
 */
public class TestRecursion {
    static File mustacheFile = new File("/Users/lidong/gitspace/antlrmustache/src/main/resources/TestTree.mustache");
    static class Component{
        int id;
        List<Component> children = new ArrayList<>();

        public Component(int id) {
            this.id = id;
        }

        public Component add(Component child){
            children.add(child);
            return this;
        }

        @Override
        public String toString() {
            return "Component{" +
                    "id=" + id +
                    ", children=" + children +
                    '}';
        }
    }
    public static void main(String[] args) throws IOException {
        MustacheCompiler mustacheCompiler = new MustacheCompiler(mustacheFile);
        MustacheListenerImpl impl = mustacheCompiler.compile();
        Component root = new Component(1);
        root.add(new Component(2).add(new Component(3).add(new Component(6))).add(new Component(5)));
        root.add(new Component(4));
        System.out.println(root);
        MustacheWriter writer = new MustacheWriter();
        writer.setPartialFileHandler(new IPartialFileHandler() {
            @Override
            public Template compilePartialTemplate(String partialName) {
                URL url = TestRecursion.class.getResource("/"+partialName +".mustache");
                try {
                    MustacheCompiler mustacheCompiler = new MustacheCompiler(url);
                    try {
                        return mustacheCompiler.compile().getTemplate();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        StringBuffer sb = new StringBuffer();
        writer.write(sb, new ArrayList<>(),root, impl.getTemplate(), BaseSection.SectionType.Normal);
        System.out.println(sb);
    }
}
