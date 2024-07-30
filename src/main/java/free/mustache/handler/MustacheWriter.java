package free.mustache.handler;

import free.mustache.model.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author lidong@date 2024-07-27@version 1.0
 */
public class MustacheWriter {
    private boolean indentIfRecursive = true;
    private int indent = 0;

    private Stack<Integer> sectionIndexs = new Stack<>();
    private IPartialFileHandler partialFileHandler;

    private Map<String, Stack<IRecursiveExecutor>> recursiveExecutors = new HashMap<>();

    public IPartialFileHandler getPartialFileHandler() {
        return partialFileHandler;
    }

    public MustacheWriter setPartialFileHandler(IPartialFileHandler partialFileHandler) {
        this.partialFileHandler = partialFileHandler;
        return this;
    }

    public boolean isIndentIfRecursive() {
        return indentIfRecursive;
    }

    public MustacheWriter setIndentIfRecursive(boolean indentIfRecursive) {
        this.indentIfRecursive = indentIfRecursive;
        return this;
    }

    public void write(StringBuffer sb, List<Object> parents, Object currentObj, Template template, BaseSection.SectionType sectionType) {
        for (IMustacheBlock block : template.getBlocks()) {
            dealBlock(sb, block, parents, currentObj, sectionType);
        }
    }

    private void dealBlock(StringBuffer sb, IMustacheBlock block, List<Object> parents, Object currentObj, BaseSection.SectionType currentSectionType) {
        if (block instanceof Text) {
            String text = ((Text) block).getText();
            if (indentIfRecursive && indent > 0) {
                sb.append(text.replace("\n", "\n" + "\t".repeat(indent)));
            } else {
                sb.append(text);
            }
        } else if (block instanceof Variable) {
            String varName = ((Variable) block).getVarName();
            Object value = null;
            value = getQualifiedOrSimpleValue(parents, currentObj, varName);
            sb.append(value.toString());
        } else if (block instanceof BaseSection) {
            BaseSection.SectionType sectionType = ((BaseSection) block).getSectionType();
            Template sub = ((BaseSection) block).getSubTemplate();
            if (sectionType == BaseSection.SectionType.Normal) {
                String sectionName = ((BaseSection) block).getSectionName();
                Object sectionObj = getQualifiedOrSimpleValue(parents, currentObj, sectionName);
                if (block instanceof InvertedSection && sectionObj == null) {
                    write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
                } else if (block instanceof Section && sectionObj != null) {
                    parents.add(currentObj);
                    if (sectionObj instanceof List || sectionObj.getClass().isArray()) {
                        Object[] array = null;
                        if (sectionObj instanceof List) {
                            array = ((List) sectionObj).toArray();
                        } else {
                            array = (Object[]) sectionObj;
                        }
                        int iLast = array.length - 1;
                        for (int i = 0; i < array.length; i++) {
                            sectionIndexs.push(i);
                            Object obj = array[i];

                            BaseSection.SectionType firstLast = i == 0 ? BaseSection.SectionType.First :
                                    (i == iLast ? BaseSection.SectionType.Last : BaseSection.SectionType.Normal);
                            if (firstLast == BaseSection.SectionType.First && i == iLast)
                                firstLast = BaseSection.SectionType.FirstAndLast;
                            IRecursiveExecutor executor = new IRecursiveExecutor() {
                                @Override
                                public void execute() {
                                    Object subSectionObj = getQualifiedValue(obj, sectionName);
                                    if (subSectionObj != null) {
                                        indent++;
                                        dealBlock(sb, block, parents, obj, BaseSection.SectionType.Normal);
                                        indent--;
                                    }
                                }
                            };
                            pushExecutor(sectionName, executor);
                            write(sb, parents, obj, sub, firstLast);

                            if (((Section) block).isRecursive()) {
                                popAndExecute(sectionName);
                            }else
                                popWithoutExecute(sectionName);
                            sectionIndexs.pop();
                        }
                    } else {
                        write(sb, parents, sectionObj, sub, BaseSection.SectionType.Normal);
                    }
                    parents.remove(currentObj);
                }
            } else {//first and last
                boolean bInverted = block instanceof InvertedSection;
                boolean bSecTypeIsLast = sectionType == BaseSection.SectionType.Last;
                boolean bCurTypeIsLast = currentSectionType == BaseSection.SectionType.Last ||
                        currentSectionType == BaseSection.SectionType.FirstAndLast;
                boolean bSecTypeIsFirst = sectionType == BaseSection.SectionType.First;
                boolean bCurTypeIsFirst = currentSectionType == BaseSection.SectionType.First ||
                        currentSectionType == BaseSection.SectionType.FirstAndLast;
                if (!bInverted) {
                    if (bSecTypeIsLast && bCurTypeIsLast) {
                        write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
                    } else {
                        if (bSecTypeIsFirst && bCurTypeIsFirst) {
                            write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
                        }
                    }
                } else {
                    if (bSecTypeIsLast && !bCurTypeIsLast) {
                        write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
                    } else {
                        if (bSecTypeIsFirst && !bCurTypeIsFirst) {
                            write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
                        }
                    }
                }
            }
        } else if (block instanceof Partial) {
            Template sub = partialFileHandler.compilePartialTemplate(((Partial) block).getPartialName());
            write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
        } else if (block instanceof SectionIndex) {
            sb.append(sectionIndexs.peek());
        } else if (block instanceof Recursive) {
            popAndExecute(((Recursive) block).getRecursiveName());
        }
    }

    private void popAndExecute(String sectionName) {
        Stack<IRecursiveExecutor> executors = recursiveExecutors.get(sectionName);
        if (executors != null && executors.size() != 0) {
            IRecursiveExecutor executor = executors.pop();
            executor.execute();
        }
    }

    private void popWithoutExecute(String sectionName) {
        Stack<IRecursiveExecutor> executors = recursiveExecutors.get(sectionName);
        if (executors != null && executors.size() != 0) {
            executors.pop();
        }
    }

    private void pushExecutor(String sectionName, IRecursiveExecutor executor) {
        Stack<IRecursiveExecutor> executors = recursiveExecutors.get(sectionName);
        if (executors == null) {
            executors = new Stack<>();
            recursiveExecutors.put(sectionName, executors);
        }
        executors.push(executor);
    }

    private Object getQualifiedOrSimpleValue(List<Object> parents, Object currentObj, String varName) {
        return getVarValue(parents, currentObj, varName);
    }

    private Object getVarValue(List<Object> parents, Object currentObj, String varName) {
        Object ret = null;
        int count = parents.size() - 1;
        do {
            ret = getVarValue(currentObj, varName);
            if (count < 0)
                break;
            currentObj = parents.get(count);
            count--;
        } while (ret == null);
        return ret;
    }

    private Object getVarValue(Object currentObj, String varName) {
        Object ret = null;
        if (varName.indexOf('.') != -1) {
            ret = getQualifiedValue(currentObj, varName);
        } else if (currentObj instanceof Map) {
            ret = ((Map<?, ?>) currentObj).get(varName);
        } else {
            try {
                Field field = currentObj.getClass().getDeclaredField(varName);
                field.setAccessible(true);
                ret = field.get(currentObj);
            } catch (NoSuchFieldException e) {
                ret = invockNamedMethod(currentObj, varName, ret);
            } catch (IllegalAccessException e) {
            }
        }
        return ret;
    }

    private static Object invockNamedMethod(Object root, String varName, Object ret) {
        String vname = capitalizeFirstLetter(varName);
        String getName = "get" + vname;
        String isName = "is" + vname;
        Method method = null;
        try {
            method = root.getClass().getMethod(getName, null);
        } catch (NoSuchMethodException ex) {
            try {
                method = root.getClass().getMethod(isName, null);
            } catch (NoSuchMethodException exc) {
            }
        }
        if (method != null) {
            try {
                ret = method.invoke(root, null);
            } catch (IllegalAccessException ex) {
            } catch (InvocationTargetException ex) {
            }
        }
        return ret;
    }

    private Object getQualifiedValue(Object obj, String qualifiedName) {
        String[] fieldNames = qualifiedName.split("\\.");
        Object currentObject = obj;

        for (String fieldName : fieldNames) {
            currentObject = getVarValue(currentObject, fieldName);

            if (currentObject == null) {
                return null;
            }
        }

        return currentObject;
    }

    public static String capitalizeFirstLetter(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
