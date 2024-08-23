package free.servpp.mustache.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lidong@date 2024-07-27@version 1.0
 */
public class Template {
    private List<IMustacheBlock> blocks = new ArrayList<>();

    public void addBlock(IMustacheBlock block){
        blocks.add(block);
    }

    public List<IMustacheBlock> getBlocks() {
        return blocks;
    }
}
