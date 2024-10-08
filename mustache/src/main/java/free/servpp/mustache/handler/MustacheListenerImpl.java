package free.servpp.mustache.handler;

import free.servpp.mustache.antlr.*;
import free.servpp.mustache.model.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Stack;

/**
 * @author lidong@date 2024-07-27@version 1.0
 */
public class MustacheListenerImpl extends MustacheBaseListener {
    private Template template = new Template();

    public Template getTemplate() {
        return template;
    }

    Stack<Object> stacks = new Stack<>();
    @Override
    public void enterTemplate(MustacheParser.TemplateContext ctx) {
        stacks.push(template);
    }

    @Override
    public void exitTemplate(MustacheParser.TemplateContext ctx) {
        stacks.pop();
    }

    @Override
    public void enterVariable(MustacheParser.VariableContext ctx) {
        Template tpl = (Template) stacks.peek();
        Variable variable = new Variable();
        variable.setVarName(skipWS((MustacheParser.QualifiedNameContext) ctx.getChild(1)));
        tpl.addBlock(variable);
    }

    private String skipWS(MustacheParser.QualifiedNameContext ctx){
        String ret = "";
        for(int i = 0;i< ctx.getChildCount();i++){

            ParseTree child = ctx.getChild(i);
            if(child instanceof TerminalNode){
                int tokenType = ((TerminalNode) child).getSymbol().getType();
                if(tokenType == MustacheLexer.WS){
                    continue;
                }
            }
            String text = child.getText();
            ret += text;
        }
        return ret;
    }
    @Override
    public void exitVariable(MustacheParser.VariableContext ctx) {
    }

    @Override
    public void enterText(MustacheParser.TextContext ctx) {
        Template tpl = (Template) stacks.peek();
        tpl.addBlock(new Text().setText(ctx.getText()));
    }

    @Override
    public void exitText(MustacheParser.TextContext ctx) {

    }

    @Override
    public void enterComment(MustacheParser.CommentContext ctx) {
        Template tpl = (Template) stacks.peek();
        tpl.addBlock(new Comment().setComment(ctx.getText()));
    }

    @Override
    public void exitComment(MustacheParser.CommentContext ctx) {

    }

    @Override
    public void enterInvertedSection(MustacheParser.InvertedSectionContext ctx) {
        Template tpl = (Template) stacks.peek();
        InvertedSection invertedSection = new InvertedSection();
        _addSection(tpl,invertedSection);
    }

    @Override
    public void exitInvertedSection(MustacheParser.InvertedSectionContext ctx) {
        Template sub = (Template) stacks.pop();
        InvertedSection invertedSection = (InvertedSection) stacks.pop();
    }

    @Override
    public void enterSection(MustacheParser.SectionContext ctx) {
        Template tpl = (Template) stacks.peek();
        Section section = new Section();
        _addSection(tpl, section);
    }

    private void _addSection(Template tpl, BaseSection section) {
        tpl.addBlock(section);
        stacks.push(section);
        Template sub = new Template();
        section.setSubTemplate(sub);
        stacks.push(sub);
    }

    @Override
    public void exitSection(MustacheParser.SectionContext ctx) {
        Template sub = (Template) stacks.pop();
        Section section = (Section) stacks.pop();
    }

    @Override
    public void enterSectionBeg(MustacheParser.SectionBegContext ctx) {
        BaseSection section = getBaseSection();
        if(section instanceof Section){
            if(ctx.getChild(1).getText().equals("@")){
                ((Section) section).setMapAsList(true);
            }
        }

    }

    private BaseSection getBaseSection() {
        Template sub = (Template) stacks.pop();
        BaseSection section = (BaseSection) stacks.peek();
        stacks.push(sub);
        return section;
    }

    @Override
    public void exitSectionBeg(MustacheParser.SectionBegContext ctx) {

    }

    @Override
    public void enterSectionVar(MustacheParser.SectionVarContext ctx) {
        BaseSection section = getBaseSection();
        String name = section.getSectionName();
        String newName = null;
        ParseTree child = ctx.getChild(0);
        if(child.getText().trim().equals("$")) {
            section.setExprSection(true);
            child = ctx.getChild(1);
        }
        if(child instanceof MustacheParser.QualifiedNameContext) {
            newName = skipWS((MustacheParser.QualifiedNameContext) child);
        }else
            newName = child.getText();
        if(name != null && !name.equals(newName))
            throw new RuntimeException("Section "+name + "end incorrect at " + ctx.getStart().getLine());
        if(name == null)
            section.setSectionName(newName);
    }

    @Override
    public void exitSectionVar(MustacheParser.SectionVarContext ctx) {

    }

    @Override
    public void enterSectionContent(MustacheParser.SectionContentContext ctx) {

    }

    @Override
    public void exitSectionContent(MustacheParser.SectionContentContext ctx) {
    }

    @Override
    public void enterSectionEnd(MustacheParser.SectionEndContext ctx) {
        BaseSection section = getBaseSection();
        if(section instanceof Section){
            if(ctx.getChild(1).getText().equals("%")){
                ((Section) section).setRecursive(true);
            }
        }
    }

    @Override
    public void exitSectionEnd(MustacheParser.SectionEndContext ctx) {

    }

    @Override
    public void enterPartial(MustacheParser.PartialContext ctx) {
        Template tpl = (Template) stacks.peek();
        Partial variable = new Partial();
        variable.setPartialName(skipWS((MustacheParser.QualifiedNameContext) ctx.getChild(2)));
        tpl.addBlock(variable);
    }

    @Override
    public void exitPartial(MustacheParser.PartialContext ctx) {
    }

    @Override
    public void enterSectionIndex(MustacheParser.SectionIndexContext ctx) {
        Template tpl = (Template) stacks.peek();
        SectionIndex index = new SectionIndex();
        tpl.addBlock(index);
    }

    @Override
    public void exitSectionIndex(MustacheParser.SectionIndexContext ctx) {

    }

    @Override
    public void enterSectionRecursive(MustacheParser.SectionRecursiveContext ctx) {
        Template tpl = (Template) stacks.peek();
        Recursive variable = new Recursive();
        variable.setRecursiveName(skipWS((MustacheParser.QualifiedNameContext) ctx.getChild(2)));
        tpl.addBlock(variable);
    }

    @Override
    public void exitSectionRecursive(MustacheParser.SectionRecursiveContext ctx) {

    }


    @Override
    public void enterMultiexpr(MustacheParser.MultiexprContext ctx) {
        Template tpl = (Template) stacks.peek();
        MultiExpr expr = new MultiExpr();
        expr.setMultiExpr(ctx.getText());
        tpl.addBlock(expr);
    }

    @Override
    public void exitMultiexpr(MustacheParser.MultiexprContext ctx) {

    }
}
