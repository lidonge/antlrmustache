package free.servpp.mustache.model;

/**
 * @author lidong@date 2024-07-27@version 1.0
 */
public class Partial implements IStatement{
    private String partialName;

    public String getPartialName() {
        return partialName;
    }

    public void setPartialName(String partialName) {
        this.partialName = partialName;
    }
}
