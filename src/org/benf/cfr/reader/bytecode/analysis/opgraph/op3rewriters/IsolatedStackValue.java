package org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.AssignmentSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.ExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.Nop;
import org.benf.cfr.reader.util.collections.MapFactory;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

class IsolatedStackValue {
    static void nopIsolatedStackValues(ObjectList<Op03SimpleStatement> statements) {
        // A stack value is (EXCEPT IN THE CASE OF DUP) only consumed once.
        // We can nop both the assignment and the consumption if the consumption is
        // an expression statement.

        ObjectSet<StackSSALabel> blackList = new ObjectOpenHashSet<>();
        Map<StackSSALabel, Op03SimpleStatement> consumptions = MapFactory.newMap();
        Map<StackSSALabel, Op03SimpleStatement> assignments = MapFactory.newMap();

        for (Op03SimpleStatement statement : statements) {
            Statement stm = statement.getStatement();
            if (stm instanceof ExpressionStatement) {
                Expression expression = ((ExpressionStatement) stm).getExpression();
                if (expression instanceof StackValue sv) {
                    StackSSALabel stackValue = sv.getStackValue();
                    if (consumptions.put(stackValue, statement) != null|| stackValue.getStackEntry().getUsageCount() > 1) {
                        blackList.add(stackValue);
                    }
                }
            } else if (stm instanceof AssignmentSimple) {
                if (stm.getCreatedLValue() instanceof StackSSALabel stackValue) {
                    if (assignments.put(stackValue, statement) != null) {
                        blackList.add(stackValue);
                    }
                }
            }
        }

        for (Map.Entry<StackSSALabel, Op03SimpleStatement> entry : consumptions.entrySet()) {
            StackSSALabel label = entry.getKey();
            Op03SimpleStatement assign = assignments.get(label);
            if (blackList.contains(label) || assign == null) {
                continue;
            }
            entry.getValue().replaceStatement(new Nop());
            assign.replaceStatement(new ExpressionStatement(assign.getStatement().getRValue()));
        }

    }
}
