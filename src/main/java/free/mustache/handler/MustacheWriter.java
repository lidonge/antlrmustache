package free.mustache.handler;

import free.mustache.IEvaluatorEnvironment;
import free.mustache.model.*;


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
    private ExprEvaluator exprEvaluator = new ExprEvaluator();

    public ExprEvaluator getExprEvaluator() {
        return exprEvaluator;
    }

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
            writeText(sb, (Text) block);
        } else if (block instanceof Variable) {
            writeVariable(sb, (Variable) block, parents, currentObj);
        } else if (block instanceof BaseSection) {
            BaseSection.SectionType sectionType = ((BaseSection) block).getSectionType();
            Template sub = ((BaseSection) block).getSubTemplate();
            if (sectionType == BaseSection.SectionType.Normal) {
                writeNormalSection(sb, block, parents, currentObj, sub);
            } else {//first and last
                writeFirstLast(sb, block, parents, currentObj, currentSectionType, sectionType, sub);
            }
        } else if (block instanceof Partial) {
            Template sub = partialFileHandler.compilePartialTemplate(((Partial) block).getPartialName());
            write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
        } else if (block instanceof SectionIndex) {
            sb.append(sectionIndexs.peek());
        } else if (block instanceof Recursive) {
            popAndExecute(((Recursive) block).getRecursiveName());
        }else if(block instanceof MultiExpr){
            String formula = ((MultiExpr) block).getMultiExpr();
            exprEvaluator.setVar(IEvaluatorEnvironment.OBJ_CURRENT_OBJ,currentObj);
            exprEvaluator.setVar(IEvaluatorEnvironment.OBJ_PARENTS,parents);
            Object obj = exprEvaluator.evalFormula(formula);
            sb.append(obj);
        }
    }

    private void writeNormalSection(StringBuffer sb, IMustacheBlock block, List<Object> parents, Object currentObj, Template sub) {
        String sectionName = ((BaseSection) block).getSectionName();
        boolean exprSection = ((BaseSection) block).isExprSection() ? true : false;
        Object sectionObj = exprSection ? exprEvaluator.getVar(sectionName)
                : ReflectTool.getQualifiedOrSimpleValue(parents, currentObj, sectionName);
        sectionObj = "null".equals(sectionObj) ? null : sectionObj;
        if (block instanceof InvertedSection && sectionObj == null) {
            write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
        } else if (block instanceof Section && sectionObj != null) {
            parents.add(currentObj);
            Object[] array = null;
            array = getArray((Section) block, sectionObj, array);
            if(array != null && array.length != 0){
                writeRecordArray(sb, block, parents, array, sectionName, sub);
            } else {
                write(sb, parents, sectionObj, sub, BaseSection.SectionType.Normal);
            }
            parents.remove(currentObj);
        }
    }

    private void writeRecordArray(StringBuffer sb, IMustacheBlock block, List<Object> parents, Object[] array, String sectionName, Template sub) {
        int iLast = array.length - 1;
        for (int i = 0; i < array.length; i++) {
            sectionIndexs.push(i);
            Object obj = array[i];

            writeRecordOfSection(sb, block, parents, i, iLast, obj, sectionName, sub);
        }
    }

    private static Object[] getArray(Section block, Object sectionObj, Object[] array) {
        if (sectionObj instanceof List || sectionObj.getClass().isArray()) {
            if (sectionObj instanceof List) {
                array = ((List) sectionObj).toArray();
            } else {
                array = (Object[]) sectionObj;
            }
        }else if(sectionObj instanceof Map && block.isMapAsList()){
            array = ((Map<?, ?>) sectionObj).values().toArray();
        }
        return array;
    }

    private void writeRecordOfSection(StringBuffer sb, IMustacheBlock block, List<Object> parents, int i, int iLast, Object obj, String sectionName, Template sub) {
        BaseSection.SectionType firstLast = i == 0 ? BaseSection.SectionType.First :
                (i == iLast ? BaseSection.SectionType.Last : BaseSection.SectionType.Normal);
        if (firstLast == BaseSection.SectionType.First && i == iLast)
            firstLast = BaseSection.SectionType.FirstAndLast;
        IRecursiveExecutor executor = new IRecursiveExecutor() {
            @Override
            public void execute() {
                Object subSectionObj = ReflectTool.getQualifiedValue(obj, sectionName);
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

    private void writeFirstLast(StringBuffer sb, IMustacheBlock block, List<Object> parents, Object currentObj, BaseSection.SectionType currentSectionType, BaseSection.SectionType sectionType, Template sub) {
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

    private void writeText(StringBuffer sb, Text block) {
        String text = block.getText();
        if (indentIfRecursive && indent > 0) {
            sb.append(text.replace("\n", "\n" + "\t".repeat(indent)));
        } else {
            sb.append(text);
        }
    }

    private void writeVariable(StringBuffer sb, Variable block, List<Object> parents, Object currentObj) {
        String varName = block.getVarName();
        Object value = null;
        value = ReflectTool.getQualifiedOrSimpleValue(parents, currentObj, varName);
        sb.append(value.toString());
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

}
