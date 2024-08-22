package free.mustache.handler;

import free.mustache.IEvaluatorEnvironment;
import free.mustache.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Handles the rendering of Mustache templates.
 * This class processes the template blocks and evaluates expressions to generate the final output.
 *
 * @author lidong
 * @version 1.0
 * @date 2024-07-27
 */
public class MustacheWriter {
    // Indicates if indentations should be added when dealing with recursive blocks
    private boolean indentIfRecursive = true;
    private int indent = 0;

    // Stack to keep track of section indices during rendering
    private Stack<Integer> sectionIndexs = new Stack<>();

    // Handler for processing partial files
    private IPartialFileHandler partialFileHandler;

    // Map to manage recursive executors based on section names
    private Map<String, Stack<IRecursiveExecutor>> recursiveExecutors = new HashMap<>();

    // Evaluator to handle expressions in the template
    private ExprEvaluator exprEvaluator = new ExprEvaluator();

    // Getter for the expression evaluator
    public ExprEvaluator getExprEvaluator() {
        return exprEvaluator;
    }

    // Getter for the partial file handler
    public IPartialFileHandler getPartialFileHandler() {
        return partialFileHandler;
    }

    // Setter for the partial file handler
    public MustacheWriter setPartialFileHandler(IPartialFileHandler partialFileHandler) {
        this.partialFileHandler = partialFileHandler;
        return this;
    }

    // Checks if indentation is enabled for recursive blocks
    public boolean isIndentIfRecursive() {
        return indentIfRecursive;
    }

    // Enables or disables indentation for recursive blocks
    public MustacheWriter setIndentIfRecursive(boolean indentIfRecursive) {
        this.indentIfRecursive = indentIfRecursive;
        return this;
    }

    /**
     * Renders the template by processing each block in it.
     *
     * @param sb          The StringBuffer to store the rendered output
     * @param parents     List of parent objects used in template evaluation
     * @param currentObj  The current object in context
     * @param template    The Mustache template to be rendered
     * @param sectionType The type of section being processed (Normal, First, Last, etc.)
     */
    public void write(StringBuffer sb, List<Object> parents, Object currentObj, Template template, BaseSection.SectionType sectionType) {
        for (IMustacheBlock block : template.getBlocks()) {
            dealBlock(sb, block, parents, currentObj, sectionType);
        }
    }

    /**
     * Processes an individual block in the template.
     * The block could be a text, variable, section, partial, etc.
     *
     * @param sb                 The StringBuffer to store the rendered output
     * @param block              The block to process
     * @param parents            List of parent objects used in template evaluation
     * @param currentObj         The current object in context
     * @param currentSectionType The type of the current section being processed
     */
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
            } else {
                // Handle first and last sections separately
                writeFirstLast(sb, block, parents, currentObj, currentSectionType, sectionType, sub);
            }
        } else if (block instanceof Partial) {
            Template sub = partialFileHandler.compilePartialTemplate(((Partial) block).getPartialName());
            write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
        } else if (block instanceof SectionIndex) {
            sb.append(sectionIndexs.peek());
        } else if (block instanceof Recursive) {
            popAndExecute(((Recursive) block).getRecursiveName());
        } else if (block instanceof MultiExpr) {
            // Evaluate and render a multi-expression block
            String formula = ((MultiExpr) block).getMultiExpr();
            exprEvaluator.setVar(IEvaluatorEnvironment.OBJ_CURRENT_OBJ, currentObj);
            exprEvaluator.setVar(IEvaluatorEnvironment.OBJ_PARENTS, parents);
            Object obj = exprEvaluator.evalFormula(formula);
            sb.append(obj);
        }
    }

    /**
     * Processes a normal section block in the template.
     *
     * @param sb         The StringBuffer to store the rendered output
     * @param block      The block to process
     * @param parents    List of parent objects used in template evaluation
     * @param currentObj The current object in context
     * @param sub        The sub-template associated with this section
     */
    private void writeNormalSection(StringBuffer sb, IMustacheBlock block, List<Object> parents, Object currentObj, Template sub) {
        String sectionName = ((BaseSection) block).getSectionName();
        boolean exprSection = ((BaseSection) block).isExprSection();
        Object sectionObj = exprSection ? exprEvaluator.getVar(sectionName)
                : ReflectTool.getQualifiedOrSimpleValue(parents, currentObj, sectionName);
        sectionObj = "null".equals(sectionObj) ? null : sectionObj;
        if (block instanceof InvertedSection && sectionObj == null) {
            write(sb, parents, currentObj, sub, BaseSection.SectionType.Normal);
        } else if (block instanceof Section && sectionObj != null) {
            Object[] array = null;
            array = getArray((Section) block, sectionObj, array);
            //should support length zero array, because dimString need
//            if(array != null && array.length == 0){
//                //skip
//            }else
            {
                parents.add(currentObj);
                if (array != null && array.length != 0) {
                    writeRecordArray(sb, block, parents, array, sectionName, sub);
                } else {
                    write(sb, parents, sectionObj, sub, BaseSection.SectionType.Normal);
                }
                parents.remove(currentObj);
            }
        }
    }

    /**
     * Processes an array of records within a section and renders them.
     *
     * @param sb          The StringBuffer to store the rendered output
     * @param block       The block to process
     * @param parents     List of parent objects used in template evaluation
     * @param array       The array of objects to be processed in the section
     * @param sectionName The name of the section being processed
     * @param sub         The sub-template associated with this section
     */
    private void writeRecordArray(StringBuffer sb, IMustacheBlock block, List<Object> parents, Object[] array, String sectionName, Template sub) {
        int iLast = array.length - 1;
        for (int i = 0; i < array.length; i++) {
            sectionIndexs.push(i);
            Object obj = array[i];

            writeRecordOfSection(sb, block, parents, i, iLast, obj, sectionName, sub);
        }
    }

    /**
     * Helper method to convert a section object into an array for processing.
     *
     * @param block      The section block being processed
     * @param sectionObj The object representing the section
     * @param array      The array to store the section objects
     * @return The array representation of the section object
     */
    private static Object[] getArray(Section block, Object sectionObj, Object[] array) {
        if (sectionObj instanceof List || sectionObj.getClass().isArray()) {
            if (sectionObj instanceof List) {
                array = ((List) sectionObj).toArray();
            } else {
                array = (Object[]) sectionObj;
            }
        } else if (sectionObj instanceof Map && block.isMapAsList()) {
            array = ((Map<?, ?>) sectionObj).values().toArray();
        }
        return array;
    }

    /**
     * Processes and renders an individual record within a section.
     *
     * @param sb          The StringBuffer to store the rendered output
     * @param block       The block to process
     * @param parents     List of parent objects used in template evaluation
     * @param i           The index of the current record in the array
     * @param iLast       The index of the last record in the array
     * @param obj         The current object being processed
     * @param sectionName The name of the section being processed
     * @param sub         The sub-template associated with this section
     */
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
        } else {
            popWithoutExecute(sectionName);
        }
        sectionIndexs.pop();
    }

    /**
     * Processes first and last sections within the template.
     *
     * @param sb                 The StringBuffer to store the rendered output
     * @param block              The block to process
     * @param parents            List of parent objects used in template evaluation
     * @param currentObj         The current object in context
     * @param currentSectionType The type of the current section being processed
     * @param sectionType        The type of the section being processed (First, Last, etc.)
     * @param sub                The sub-template associated with this section
     */
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

    /**
     * Appends text blocks to the output with optional indentation.
     *
     * @param sb    The StringBuffer to store the rendered output
     * @param block The text block to process
     */
    private void writeText(StringBuffer sb, Text block) {
        String text = block.getText();
        if (indentIfRecursive && indent > 0) {
            sb.append(text.replace("\n", "\n" + "\t".repeat(indent)));
        } else {
            sb.append(text);
        }
    }

    /**
     * Processes and renders a variable block by evaluating its value.
     *
     * @param sb         The StringBuffer to store the rendered output
     * @param block      The variable block to process
     * @param parents    List of parent objects used in template evaluation
     * @param currentObj The current object in context
     */
    private void writeVariable(StringBuffer sb, Variable block, List<Object> parents, Object currentObj) {
        String varName = block.getVarName();
        Object value = ReflectTool.getQualifiedOrSimpleValue(parents, currentObj, varName);
        sb.append(value.toString());
    }

    /**
     * Executes the most recent recursive executor for a section.
     *
     * @param sectionName The name of the section to execute
     */
    private void popAndExecute(String sectionName) {
        Stack<IRecursiveExecutor> executors = recursiveExecutors.get(sectionName);
        if (executors != null && !executors.isEmpty()) {
            IRecursiveExecutor executor = executors.pop();
            executor.execute();
        }
    }

    /**
     * Removes the most recent recursive executor without executing it.
     *
     * @param sectionName The name of the section to remove the executor from
     */
    private void popWithoutExecute(String sectionName) {
        Stack<IRecursiveExecutor> executors = recursiveExecutors.get(sectionName);
        if (executors != null && !executors.isEmpty()) {
            executors.pop();
        }
    }

    /**
     * Adds a recursive executor to the stack for a given section name.
     *
     * @param sectionName The name of the section to add the executor to
     * @param executor    The recursive executor to add
     */
    private void pushExecutor(String sectionName, IRecursiveExecutor executor) {
        Stack<IRecursiveExecutor> executors = recursiveExecutors.get(sectionName);
        if (executors == null) {
            executors = new Stack<>();
            recursiveExecutors.put(sectionName, executors);
        }
        executors.push(executor);
    }
}