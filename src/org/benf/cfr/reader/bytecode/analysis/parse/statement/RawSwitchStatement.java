package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.Statement;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.opcode.DecodedSwitch;
import org.benf.cfr.reader.bytecode.opcode.DecodedSwitchEntry;
import org.benf.cfr.reader.util.output.Dumper;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * A 'raw' switch contains the original bytecode information about the switch.  We split this up into
 * SwitchStatements and CaseStatements.  Case statements are really no more than glorified comments,
 * as they perform no function other than to serve as labels.  However, we can embed useful information in
 * them.
 */
public class RawSwitchStatement extends AbstractStatement {
    private Expression switchOn;
    private final DecodedSwitch switchData;

    public RawSwitchStatement(BytecodeLoc loc, Expression switchOn, DecodedSwitch switchData) {
        super(loc);
        this.switchOn = switchOn;
        this.switchData = switchData;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, switchOn);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        dumper.print("switch (").dump(switchOn).print(") {").newln();
        ObjectList<DecodedSwitchEntry> targets = switchData.getJumpTargets();
        int targetIdx = 1;
        for (DecodedSwitchEntry decodedSwitchEntry : targets) {
            String tgtLbl = getTargetStatement(targetIdx++).getContainer().getLabel();
            dumper.print(" case " + decodedSwitchEntry.getValue() + ": goto " + tgtLbl + ";").newln();
        }
        dumper.print(" default: goto " + getTargetStatement(0).getContainer().getLabel() + ";").newln();
        dumper.print("}").newln();
        return dumper;
    }

    @Override
    public Statement deepClone(CloneHelper cloneHelper) {
        // we should really never get here!
        return new RawSwitchStatement(getLoc(), cloneHelper.replaceOrClone(switchOn), switchData);
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        switchOn = switchOn.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        switchOn = expressionRewriter.rewriteExpression(switchOn, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        switchOn.collectUsedLValues(lValueUsageCollector);
    }

    public DecodedSwitch getSwitchData() {
        return switchData;
    }

    public Expression getSwitchOn() {
        return switchOn;
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        throw new RuntimeException("Can't convert a raw switch statement to a structured statement");
    }

    public SwitchStatement getSwitchStatement(BlockIdentifier blockIdentifier) {
        return new SwitchStatement(getLoc(), switchOn, blockIdentifier);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        RawSwitchStatement other = (RawSwitchStatement) o;
        return constraint.equivalent(switchOn, other.switchOn);
    }

    @Override
    public boolean fallsToNext() {
        return false;
    }
}
