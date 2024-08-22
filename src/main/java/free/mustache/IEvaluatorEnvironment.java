package free.mustache;

import free.mustache.handler.ReflectTool;

import java.util.List;
import java.util.function.Function;

/**
 * @author lidong@date 2024-07-31@version 1.0
 */
public interface IEvaluatorEnvironment {
    String STRING_SUB = "str_sub";
    String STRING_INDEX_OF = "str_indexOf";
    String OBJ_GET_VAR = "obj_getVar";
    String OBJ_GET_CUR_VAR = "obj_getCurVar";
    String OBJ_PARENTS = "obj_parents";
    String OBJ_CURRENT_OBJ = "obj_currentObj";

    default void addDefault(){
        addFunction(STRING_SUB, args -> ((String) args[0]).substring((int) args[1], (int) args[2]));
        addFunction(STRING_INDEX_OF, args -> ((String) args[0]).indexOf((String) args[1]));
        addFunction(OBJ_GET_VAR, args -> {
            List<Object> parents = (List<Object>) getVar(OBJ_PARENTS);
            Object currentObj = getVar(OBJ_CURRENT_OBJ);
            return ReflectTool.getQualifiedOrSimpleValue(parents, currentObj, (String) args[0]);
        });
        addFunction(OBJ_GET_CUR_VAR, args -> {
            Object currentObj = getVar(OBJ_CURRENT_OBJ);
            return ReflectTool.getQualifiedOrSimpleValue(null, currentObj, (String) args[0]);
        });
    }

    void setVar(String name, Object value);

    Object getVar(String name);

    void addFunction(String name, Function<Object[], Object> function);

    Function<Object[], Object> getFunction(String name);
}
