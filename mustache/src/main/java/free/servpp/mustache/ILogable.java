package free.servpp.mustache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lidong@date 2023-10-25@version 1.0
 */
public interface ILogable {
    default Logger getLogger(){
        return this.getLogger(this.getClass());
    }
    default Logger getLogger(Class cls){
        return LoggerFactory.getLogger(cls);
    }
}
