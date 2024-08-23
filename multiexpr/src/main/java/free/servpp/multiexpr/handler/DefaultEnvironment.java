package free.servpp.multiexpr.handler;


import free.servpp.multiexpr.IEvaluatorEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author lidong@date 2024-07-31@version 1.0
 */
public class DefaultEnvironment implements IEvaluatorEnvironment {

    private Map<String, Object> variables = new HashMap<>();
    private Map<String, Function<Object[], Object>> functions = new HashMap<>();

    public DefaultEnvironment() {
        addDefault();
    }

    @Override
    public void setVar(String name, Object value) {
        variables.put(name, value);
    }

    @Override
    public Object getVar(String name) {
        return variables.get(name);
    }

    @Override
    public void addFunction(String name, Function<Object[], Object> function) {
        functions.put(name,function);
    }

    @Override
    public Function<Object[], Object> getFunction(String name) {
        return functions.get(name);
    }
}
