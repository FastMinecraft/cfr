package org.benf.cfr.reader.bytecode.analysis.parse.utils.scope;

import it.unimi.dsi.fastutil.objects.*;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.SentinelLocalClassLValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredSwitch;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;

import java.util.*;

public abstract class AbstractLValueScopeDiscoverer implements LValueScopeDiscoverer {

    /*
     * We keep track of the first definition for a given variable.  If we exit the scope that the variable
     * is defined at (i.e. scope depth goes above) we have to remove all earliest definitions at that level.
     */
    final Map<NamedVariable, ScopeDefinition> earliestDefinition = new Object2ObjectLinkedOpenHashMap<>();
    final Map<Integer, Map<NamedVariable, Boolean>> earliestDefinitionsByLevel = MapFactory.newLazyMap(arg -> new Reference2ObjectOpenHashMap<>());
    int currentDepth = 0;

    final Stack<StatementContainer<StructuredStatement>> currentBlock = new Stack<>();

    final ObjectList<ScopeDefinition> discoveredCreations = new ObjectArrayList<>();
    final VariableFactory variableFactory;
    StatementContainer<StructuredStatement> currentMark = null;
    final Options options;
    private final MethodPrototype prototype;
    private final ScopeDiscoverInfoCache factCache = new ScopeDiscoverInfoCache();

    AbstractLValueScopeDiscoverer(Options options, MethodPrototype prototype, VariableFactory variableFactory) {
        this.options = options;
        this.prototype = prototype;
        final ObjectList<LocalVariable> parameters = prototype.getComputedParameters();
        this.variableFactory = variableFactory;
        for (LocalVariable parameter : parameters) {
            InferredJavaType inferredJavaType = parameter.getInferredJavaType();
            final ScopeDefinition prototypeScope = new ScopeDefinition(0, null, null, parameter, inferredJavaType, parameter.getName());
            earliestDefinition.put(parameter.getName(), prototypeScope);
        }
    }

    ScopeDiscoverInfoCache getFactCache() {
        return factCache;
    }

    @Override
    public void enterBlock(StructuredStatement structuredStatement) {
        StatementContainer<StructuredStatement> container = structuredStatement.getContainer();
        if (container == null) {
            return;
        }
        currentBlock.push(container);
        currentDepth++;
    }

    @Override
    public boolean ifCanDefine() {
        return false;
    }

    @Override
    public void processOp04Statement(Op04StructuredStatement statement) {
        statement.getStatement().traceLocalVariableScope(this);
    }

    @Override
    public void mark(StatementContainer<StructuredStatement> mark) {
        currentMark = mark;
    }

    @Override
    public void leaveBlock(StructuredStatement structuredStatement) {
        Op04StructuredStatement container = structuredStatement.getContainer();
        if (container == null) {
            return;
        }
        for (NamedVariable definedHere : earliestDefinitionsByLevel.get(currentDepth).keySet()) {
            earliestDefinition.remove(definedHere);
        }
        earliestDefinitionsByLevel.remove(currentDepth);
        StatementContainer<StructuredStatement> oldContainer = currentBlock.pop();
        if (container != oldContainer) {
            throw new IllegalStateException();
        }
        currentDepth--;
    }

    @Override
    public void collect(StackSSALabel lValue, StatementContainer<StructuredStatement> statementContainer, Expression value) {

    }

    @Override
    public void collectMultiUse(StackSSALabel lValue, StatementContainer<StructuredStatement> statementContainer, Expression value) {

    }

    @Override
    public void collectMutatedLValue(LValue lValue, StatementContainer<StructuredStatement> statementContainer, Expression value) {

    }

    private record ScopeKey(LValue lValue, JavaTypeInstance type) {
        //            this.type = type.getDeGenerifiedType();
        // Using the degenerified type causes us to 'correctly' combine a variable where it's been split into generic
        // and non-generic types, but I can't convince myself it doesn't have scope for illegal combining.

        @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                ScopeKey scopeKey = (ScopeKey) o;

                if (!lValue.equals(scopeKey.lValue)) return false;
            return type.equals(scopeKey.type);
        }

    }

    public void markDiscoveredCreations() {
        /*
         * Eliminate enclosing scopes where they were falsely detected, and
         * where scopes for the same variable exist, lift to the lowest common denominator.
         */
        Map<ScopeKey, ObjectList<ScopeDefinition>> definitionsByType = Functional.groupToMapBy(discoveredCreations,
                                                                                               new Object2ObjectLinkedOpenHashMap<ScopeKey, ObjectList<ScopeDefinition>>(),
            ScopeDefinition::getScopeKey
        );

        creation : for (Map.Entry<ScopeKey, ObjectList<ScopeDefinition>> entry : definitionsByType.entrySet()) {
            ScopeKey scopeKey = entry.getKey();
            ObjectList<ScopeDefinition> definitions = entry.getValue();
            // find the longest common nested scope - null wins automatically!

            ObjectList<StatementContainer<StructuredStatement>> commonScope = null;
            ScopeDefinition bestDefn = null;
            LValue scopedEntity = scopeKey.lValue();
            for (int x=definitions.size()-1;x>=0;--x) {
                ScopeDefinition definition = definitions.get(x);
                StructuredStatement statement = definition.getStatementContainer().getStatement();

                if (statement.alwaysDefines(scopedEntity)) {
                    statement.markCreator(scopedEntity, null);
                    continue;
                }

                ObjectList<StatementContainer<StructuredStatement>> scopeList = definition.getNestedScope();
                if (scopeList.isEmpty()) scopeList = null;

                if (scopeList == null) {
                    commonScope = null;
                    bestDefn = definition;
                    break;
                }

                if (commonScope == null) {
                    commonScope = scopeList;
                    bestDefn = definition;
                    continue;
                }
                // Otherwise, take the common prefix.
                commonScope = getCommonPrefix(commonScope, scopeList);
                if (commonScope.size() == scopeList.size()) {
                    bestDefn = definition;
                } else {
                    bestDefn = null;
                }
            }
            // But - we can only accept the first definition as a 'location'.
            // This is because we might be combining two declarations, in which case we
            // will NOT want to use the later one!
            if (bestDefn != definitions.get(0)) bestDefn = null;
            StatementContainer<StructuredStatement> creationContainer = null;
            if (scopedEntity instanceof SentinelLocalClassLValue) {
                ObjectList<StatementContainer<StructuredStatement>> scope = null;
                if (bestDefn != null) {
                    scope = bestDefn.getNestedScope();
                } else if (commonScope != null) {
                    scope = commonScope;
                }

                if (scope != null) {
                    for (int i = scope.size() - 1; i >= 0; --i) {
                        StatementContainer<StructuredStatement> thisItem = scope.get(i);
                        if (thisItem.getStatement() instanceof Block block) {
                            block.setIndenting(true);
                            creationContainer = thisItem;
                            break;
                        }
                    }
                }
            } else {
                if (bestDefn != null) {
                    creationContainer = bestDefn.getStatementContainer();
                } else if (commonScope != null && !commonScope.isEmpty()) {
                    // This is a bit ugly.  If we have a switchStatement at -2,
                    // then we know that the statement at -1 is invalid.
                    if (commonScope.size() > 2) {
                        StatementContainer<StructuredStatement> testSwitch = commonScope.get(commonScope.size() - 2);
                        if (testSwitch.getStatement() instanceof StructuredSwitch) {
                            // We can't define at this level.  We could either define 1 level higher, or (this is fun)
                            // we can define inside each of the first eligible blocks of definitions!
                            if (defineInsideSwitchContent(scopedEntity, definitions, commonScope)) {
                                //noinspection UnnecessaryLabelOnContinueStatement
                                continue creation;
                            }
                            commonScope = commonScope.subList(0, commonScope.size()-2);
                        }
                    }

                    creationContainer = commonScope.get(commonScope.size() - 1);
                }
            }

            StatementContainer<StructuredStatement> hint = bestDefn == null ? null : bestDefn.localHint;
            if (creationContainer == null) {
                continue;
            }
            // If we have no hint but a creation container, where in the scope is it?
            // Is it at the top of the scope?
            // If so, is this a constructor? Because if so we have to make sure we skip any
            // this / super calls.
            if (hint == null && commonScope != null && commonScope.size() == 1) {
                if (MiscConstants.INIT_METHOD.equals(prototype.getName())) {
                    hint = getNonInit(creationContainer);
                }
            }
            creationContainer.getStatement().markCreator(scopedEntity, hint);
        }
    }

    private StatementContainer<StructuredStatement> getNonInit(StatementContainer<StructuredStatement> creationContainer) {
        StructuredStatement stm = creationContainer.getStatement();
        if (!(stm instanceof Block)) {
            return null;
        }
        List<Op04StructuredStatement> content = ((Block) stm).getBlockStatements();
        int x;
        int len = content.size()-1;
        for (x = 0;x < len;++x) {
            StructuredStatement item = content.get(x).getStatement();
            if (item instanceof StructuredComment) continue;
            if (item instanceof StructuredExpressionStatement) {
                Expression e = (((StructuredExpressionStatement) item).getExpression());
                if (e instanceof MemberFunctionInvokation && ((MemberFunctionInvokation) e).isInitMethod()) break;
                if (e instanceof SuperFunctionInvokation) break;
            }
            return null;
        }
        return content.get(x+1);
    }

    private boolean defineInsideSwitchContent(LValue scopedEntity, ObjectList<ScopeDefinition> definitions, ObjectList<StatementContainer<StructuredStatement>> commonScope) {
        int commonScopeSize = commonScope.size();
        Set<StatementContainer<StructuredStatement>> usedPoints = new ReferenceOpenHashSet<>();
        ObjectList<ScopeDefinition> foundPoints = new ObjectArrayList<>();
        for (ScopeDefinition def : definitions) {
            if (def.nestedScope.size() <= commonScopeSize) return false;
            StatementContainer<StructuredStatement> innerDef = def.nestedScope.get(commonScopeSize);
            if (!innerDef.getStatement().canDefine(scopedEntity, factCache)) return false;
            if (usedPoints.add(innerDef)) {
                foundPoints.add(def);
            }
        }
        for (ScopeDefinition def : foundPoints) {
            StatementContainer<StructuredStatement> stm = def.nestedScope.get(commonScopeSize);
            if (def.nestedScope.size() == commonScopeSize+1) {
                if (def.exactStatement != null) {
                    stm = def.exactStatement;
                }
            }
            stm.getStatement().markCreator(scopedEntity, stm);
        }
        return true;
    }

    private static <T> ObjectList<T> getCommonPrefix(ObjectList<T> a, ObjectList<T> b) {
        ObjectList<T> la, lb;
        if (a.size() < b.size()) {
            la = a;
            lb = b;
        } else {
            la = b;
            lb = a;
        }
        // la is shortest or equal.
        int maxRes = Math.min(la.size(), lb.size());
        int sameLen = 0;
        for (int x = 0; x < maxRes; ++x, ++sameLen) {
            if (!la.get(x).equals(lb.get(x))) break;
        }
        if (sameLen == la.size()) return la;
        return la.subList(0, sameLen);
    }

    private JavaTypeInstance getUnclashedType(InferredJavaType inferredJavaType) {
        if (inferredJavaType.isClash()) {
            inferredJavaType.collapseTypeClash();
        }
        return inferredJavaType.getJavaTypeInstance();
    }

    class ScopeDefinition {
        private final int depth;
        private boolean immediate;
        // Keeping this nested scope is woefully inefficient.... fixme.
        private final ObjectList<StatementContainer<StructuredStatement>> nestedScope;
        private final StatementContainer<StructuredStatement> exactStatement;
        private final StatementContainer<StructuredStatement> localHint;
        private final LValue lValue;
        private final JavaTypeInstance lValueType;
        private final NamedVariable name;
        private final ScopeKey scopeKey;

        ScopeDefinition(int depth, Stack<StatementContainer<StructuredStatement>> nestedScope, StatementContainer<StructuredStatement> exactStatement,
                        LValue lValue, InferredJavaType inferredJavaType, NamedVariable name) {
            this(depth, nestedScope, exactStatement, lValue, getUnclashedType(inferredJavaType), name, null, true);
        }

        StatementContainer<StructuredStatement> getExactStatement() {
            return exactStatement;
        }

        ScopeDefinition(int depth, Stack<StatementContainer<StructuredStatement>> nestedScope, StatementContainer<StructuredStatement> exactStatement,
                        LValue lValue, JavaTypeInstance type, NamedVariable name, StatementContainer<StructuredStatement> hint, boolean immediate) {
            this.depth = depth;
            this.immediate = immediate;
            Pair< ObjectList<StatementContainer<StructuredStatement>>, StatementContainer<StructuredStatement>> adjustedScope = getBestScopeFor(lValue, nestedScope, exactStatement);
            this.nestedScope = adjustedScope.getFirst();
            this.exactStatement = adjustedScope.getSecond();
            this.lValue = lValue;
            this.lValueType = type;
            this.name = name;
            this.localHint = hint;
            this.scopeKey = new ScopeKey(lValue, type);
        }

        // nestedScope == null ? null : ListFactory.newList(nestedScope);
        private Pair< ObjectList<StatementContainer<StructuredStatement>>, StatementContainer<StructuredStatement>> getBestScopeFor(
                                     LValue lValue,
                                     Collection<StatementContainer<StructuredStatement>> nestedScope,
                                     StatementContainer<StructuredStatement> exactStatement) {
            if (nestedScope == null) return Pair.make(null, exactStatement);
            ObjectList<StatementContainer<StructuredStatement>> scope = new ObjectArrayList<>(nestedScope);
            if (exactStatement != null && exactStatement.getStatement().alwaysDefines(lValue)) return Pair.make(scope, exactStatement);
            if (scope.isEmpty()) return Pair.make(scope, exactStatement);
            for (int x=scope.size()-1;x>=0;--x) {
                StatementContainer<StructuredStatement> scopeTest = scope.get(x);
                if (scopeTest.getStatement().canDefine(lValue, factCache)) break;
                scope.remove(x);
            }
            if (scope.size() == nestedScope.size()) return Pair.make(scope, exactStatement);
            if (scope.isEmpty()) return Pair.make(null, exactStatement);
            exactStatement = scope.get(scope.size()-1);

            return Pair.make(scope, exactStatement);
        }

        public JavaTypeInstance getJavaTypeInstance() {
            return lValueType;
        }

        public StatementContainer<StructuredStatement> getStatementContainer() {
            return exactStatement;
        }

        public LValue getlValue() {
            return lValue;
        }

        int getDepth() {
            return depth;
        }

        public NamedVariable getName() {
            return name;
        }

        ScopeKey getScopeKey() {
            return scopeKey;
        }

        ObjectList<StatementContainer<StructuredStatement>> getNestedScope() {
            return nestedScope;
        }

        @Override
        public String toString() {
            return name + " : " + lValueType.getRawName();
        }

        boolean isImmediate() {
            return immediate;
        }

        void setImmediate() {
            immediate = true;
        }
    }

}
