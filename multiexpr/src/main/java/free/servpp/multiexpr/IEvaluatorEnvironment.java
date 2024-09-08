package free.servpp.multiexpr;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    String OBJ_GET_CUR_OBJ = "obj_getCurObj";
    String OBJ_GET_PAR_OBJ = "obj_getParObj";
    String OBJ_NEW_LIST = "obj_newList";
    String OBJ_ADD_To_LIST = "obj_addToList";
    String OBJ_NEW_MAP = "obj_newMap";
    String OBJ_ADD_To_MAP = "obj_addToMap";

    default void addDefault(){
        addFunction(STRING_SUB, args -> ((String) args[0]).substring((int) args[1], (int) args[2]));
        addFunction(STRING_INDEX_OF, args -> ((String) args[0]).indexOf((String) args[1]));
        addFunction(OBJ_GET_CUR_OBJ, args -> {
            return getVar(OBJ_CURRENT_OBJ);
        });
        addFunction(OBJ_GET_PAR_OBJ, args -> {
            List<Object> parents = (List<Object>) getVar(OBJ_PARENTS);
            return parents.size() == 0 ? getVar(OBJ_CURRENT_OBJ) : parents.get(parents.size() -1);
        });
        addFunction(OBJ_GET_VAR, args -> {
            List<Object> parents = (List<Object>) getVar(OBJ_PARENTS);
            Object currentObj = getVar(OBJ_CURRENT_OBJ);
            ReflectTool.DEBUG = false;
            Object ret = ReflectTool.getQualifiedOrSimpleValue(parents, currentObj, (String) args[0]);
            ReflectTool.DEBUG = true;
            return ret;
        });
        addFunction(OBJ_GET_CUR_VAR, args -> {
            Object currentObj = getVar(OBJ_CURRENT_OBJ);
            return ReflectTool.getQualifiedOrSimpleValue(null, currentObj, (String) args[0]);
        });
        addFunction(OBJ_NEW_LIST, args -> new ArrayList<Object>());
        addFunction(OBJ_ADD_To_LIST, args -> {
            return ((List)args[0]).add(new Object(){
                Object value = args[1];
            });
        });
        addFunction(OBJ_NEW_MAP, args -> new HashMap<>());
        addFunction(OBJ_ADD_To_MAP, args -> {
            return ((Map)args[0]).put(args[1], new Object(){
                Object value = args[2];
            });
        });
    }

    void setVar(String name, Object value);

    Object getVar(String name);

    void addFunction(String name, Function<Object[], Object> function);

    Function<Object[], Object> getFunction(String name);
}
