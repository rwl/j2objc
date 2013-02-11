package com.google.devtools.j2objc.gen;

import java.util.List;
import java.util.Map;

import llvm.Builder;
import llvm.Module;
import llvm.Value;

import org.eclipse.jdt.core.dom.ASTNode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

public class ValueGenerator extends ErrorReportingASTVisitor {

    public static List<Value> generate(ASTNode node) {
        ValueGenerator generator = new ValueGenerator();
        generator.run(node);
        return generator.values;
    }

    /**
     * The LLVM construct that contains all of the functions and global variables in a piece of code.
     */
    private final Module mod = Module.CreateWithName("j2objc");

    /**
     * Helper object that makes it easy to generate LLVM instructions.
     */
    private final Builder builder = Builder.CreateBuilder();

    /**
     * Keeps track of which values are defined in the current scope and what their LLVM representation is.
     * Method parameters will be in this map when generating code for their method body.
     */
    private final Map<String, Value> namedValues = Maps.newHashMap();

    /**
     * Static Single Assignment (SSA) registers
     */
    private final List<Value> values = Lists.newArrayList();

    private void error(String msg) {

    }
}
