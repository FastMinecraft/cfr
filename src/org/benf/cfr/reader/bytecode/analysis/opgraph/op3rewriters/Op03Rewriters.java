package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import org.benf.cfr.reader.bytecode.AnonymousClassUsage;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.StackVarToLocalRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.getopt.Options;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class Op03Rewriters {
    public static void rewriteWith(ObjectList<Op03SimpleStatement> in, ExpressionRewriter expressionRewriter) {
        for (Op03SimpleStatement op03SimpleStatement : in) {
            op03SimpleStatement.rewrite(expressionRewriter);
        }
    }

    public static void simplifyConditionals(ObjectList<Op03SimpleStatement> op03SimpleParseNodes, boolean aggressive, Method method) {
        ConditionalSimplifier.simplifyConditionals(op03SimpleParseNodes, aggressive, method);
    }

    public static void rewriteNegativeJumps(ObjectList<Op03SimpleStatement> statements, boolean requireChainedConditional) {
        NegativeJumps.rewriteNegativeJumps(statements, requireChainedConditional);
    }

    public static void replaceReturningIfs(ObjectList<Op03SimpleStatement> op03SimpleParseNodes, boolean aggressive) {
        ReturnRewriter.replaceReturningIfs(op03SimpleParseNodes, aggressive);
    }

    public static void propagateToReturn2(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        ReturnRewriter.propagateToReturn2(op03SimpleParseNodes);
    }

    public static void collapseAssignmentsIntoConditionals(ObjectList<Op03SimpleStatement> op03SimpleParseNodes, Options options, ClassFileVersion classFileVersion) {
        ConditionalCondenser.collapseAssignmentsIntoConditionals(op03SimpleParseNodes, options, classFileVersion);
    }

    public static void extendTryBlocks(DCCommonState dcCommonState, ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        TryRewriter.extendTryBlocks(dcCommonState, op03SimpleParseNodes);
    }

    public static void combineTryCatchEnds(ObjectList<Op03SimpleStatement> in) {
        TryRewriter.combineTryCatchEnds(in);
    }

    public static void extractExceptionJumps(ObjectList<Op03SimpleStatement> in) {
        TryRewriter.extractExceptionJumps(in);
    }

    public static void rewriteTryBackJumps(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        TryRewriter.rewriteTryBackJumps(op03SimpleParseNodes);
    }

    public static void rejoinBlocks(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        JoinBlocks.rejoinBlocks(op03SimpleParseNodes);
    }

    public static boolean condenseConditionals(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        return CondenseConditionals.condenseConditionals(op03SimpleParseNodes);
    }

    public static boolean condenseConditionals2(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        return CondenseConditionals.condenseConditionals2(op03SimpleParseNodes);
    }

    public static boolean normalizeDupAssigns(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        return DupAssigns.normalizeDupAssigns(op03SimpleParseNodes);
    }


    public static void optimiseForTypes(ObjectList<Op03SimpleStatement> statements) {
        ObjectList<Op03SimpleStatement> conditionals = Functional.filter(statements, new TypeFilter<>(IfStatement.class));
        for (Op03SimpleStatement conditional : conditionals) {
            IfStatement ifStatement = (IfStatement) (conditional.getStatement());
            ifStatement.optimiseForTypes();
        }
    }

    public static void rewriteDoWhileTruePredAsWhile(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        WhileRewriter.rewriteDoWhileTruePredAsWhile(op03SimpleParseNodes);
    }

    public static void rewriteWhilesAsFors(Options options, ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        WhileRewriter.rewriteWhilesAsFors(options, op03SimpleParseNodes);
    }

    public static void removeSynchronizedCatchBlocks(Options options, ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        SynchronizedRewriter.removeSynchronizedCatchBlocks(options, op03SimpleParseNodes);
    }

    public static void rewriteBreakStatements(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        BreakRewriter.rewriteBreakStatements(op03SimpleParseNodes);
    }

    public static void classifyGotos(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        ClassifyGotos.classifyGotos(op03SimpleParseNodes);
    }

    public static void classifyAnonymousBlockGotos(ObjectList<Op03SimpleStatement> op03SimpleParseNodes, boolean aggressive) {
        ClassifyGotos.classifyAnonymousBlockGotos(op03SimpleParseNodes, aggressive);
    }

    public static void labelAnonymousBlocks(ObjectList<Op03SimpleStatement> op03SimpleParseNodes, BlockIdentifierFactory blockIdentifierFactory) {
        AnonymousBlocks.labelAnonymousBlocks(op03SimpleParseNodes, blockIdentifierFactory);
    }

    public static void removePointlessJumps(ObjectList<Op03SimpleStatement> statements) {
        PointlessJumps.removePointlessJumps(statements);
    }

    public static void eclipseLoopPass(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        EclipseLoops.eclipseLoopPass(op03SimpleParseNodes);
    }

    public static ObjectList<Op03SimpleStatement> removeUselessNops(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        return UselessNops.removeUselessNops(op03SimpleParseNodes);
    }

    public static void extractAssertionJumps(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        AssertionJumps.extractAssertionJumps(op03SimpleParseNodes);
    }

    public static void replaceStackVarsWithLocals(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        rewriteWith(op03SimpleParseNodes, new StackVarToLocalRewriter());
    }

    public static void narrowAssignmentTypes(Method method, ObjectList<Op03SimpleStatement> statements) {
        NarrowingTypeRewriter.rewrite(method, statements);
    }

    public static ObjectList<Op03SimpleStatement> eliminateCatchTemporaries(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        return ExceptionRewriters.eliminateCatchTemporaries(op03SimpleParseNodes);
    }

    public static void identifyCatchBlocks(ObjectList<Op03SimpleStatement> op03SimpleParseNodes, BlockIdentifierFactory blockIdentifierFactory) {
        ExceptionRewriters.identifyCatchBlocks(op03SimpleParseNodes, blockIdentifierFactory);
    }

    public static void combineTryCatchBlocks(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        ExceptionRewriters.combineTryCatchBlocks(op03SimpleParseNodes);
    }

    public static ObjectList<Op03SimpleStatement> removeRedundantTries(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        return RedundantTries.removeRedundantTries(op03SimpleParseNodes);
    }

    public static void commentMonitors(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        MonitorRewriter.commentMonitors(op03SimpleParseNodes);
    }

    public static void condenseLValueChain1(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        LValueCondense.condenseLValueChain1(op03SimpleParseNodes);
    }

    public static void condenseLValueChain2(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        LValueCondense.condenseLValueChain2(op03SimpleParseNodes);
    }

    public static void pushPreChangeBack(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        PrePostchangeAssignmentRewriter.pushPreChangeBack(op03SimpleParseNodes);
    }

    public static void replacePrePostChangeAssignments(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        PrePostchangeAssignmentRewriter.replacePrePostChangeAssignments(op03SimpleParseNodes);
    }

    public static ObjectList<Op03SimpleStatement> pushThroughGoto(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        return PushThroughGoto.pushThroughGoto(op03SimpleParseNodes);
    }

    public static void extractExceptionMiddle(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        ExceptionRewriters.extractExceptionMiddle(op03SimpleParseNodes);
    }

    public static void removePointlessExpressionStatements(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        PointlessExpressions.removePointlessExpressionStatements(op03SimpleParseNodes);
    }

    public static void condenseConstruction(DCCommonState dcCommonState, Method method, ObjectList<Op03SimpleStatement> op03SimpleParseNodes, AnonymousClassUsage anonymousClassUsage) {
        CondenseConstruction.condenseConstruction(dcCommonState, method, op03SimpleParseNodes, anonymousClassUsage);
    }

    public static void nopIsolatedStackValues(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        IsolatedStackValue.nopIsolatedStackValues(op03SimpleParseNodes);
    }

    public static void rewriteBadCompares(VariableFactory vf, ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        new BadCompareRewriter(vf).rewrite(op03SimpleParseNodes);
    }

    /*
     * Neither of these (cloneCodeFromLoop/moveJumpsIntoDo) are 'nice' transforms - they mess with the original code.
     */
    public static void cloneCodeFromLoop(ObjectList<Op03SimpleStatement> op03SimpleParseNodes, Options options, DecompilerComments comments) {
        new JumpsIntoLoopCloneRewriter(options).rewrite(op03SimpleParseNodes, comments);
    }

    public static void moveJumpsIntoDo(VariableFactory vf, ObjectList<Op03SimpleStatement> op03SimpleParseNodes, Options options, DecompilerComments comments) {
        new JumpsIntoDoRewriter(vf).rewrite(op03SimpleParseNodes, comments);
    }

    public static ObjectList<Op03SimpleStatement> removeDeadConditionals(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        return DeadConditionalRemover.INSTANCE.rewrite(op03SimpleParseNodes);
    }

    public static void condenseStaticInstances(ObjectList<Op03SimpleStatement> op03SimpleParseNodes) {
        StaticInstanceCondenser.INSTANCE.rewrite(op03SimpleParseNodes);
    }

    public static void relinkInstanceConstants(JavaRefTypeInstance thisType, ObjectList<Op03SimpleStatement> op03SimpleParseNodes, DCCommonState dcCommonState) {
        InstanceConstants.INSTANCE.rewrite(thisType, op03SimpleParseNodes, dcCommonState);
    }
}