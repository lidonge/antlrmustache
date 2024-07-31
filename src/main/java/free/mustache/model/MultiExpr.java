package free.mustache.model;

/**
 * @author lidong@date 2024-07-27@version 1.0
 */
public class MultiExpr implements IStatement{
    private String multiExpr;

    public String getMultiExpr() {
        return multiExpr;
    }

    public MultiExpr setMultiExpr(String multiExpr) {
        this.multiExpr = multiExpr;
        return this;
    }
}
