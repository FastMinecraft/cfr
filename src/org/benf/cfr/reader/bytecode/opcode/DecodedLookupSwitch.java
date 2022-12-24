package org.benf.cfr.reader.bytecode.opcode;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.bytestream.BaseByteData;
import org.benf.cfr.reader.util.bytestream.ByteData;

import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import java.util.TreeMap;

public class DecodedLookupSwitch implements DecodedSwitch {
    private static final int OFFSET_OF_DEFAULT = 0;
    private static final int OFFSET_OF_NUMPAIRS = 4;
    private static final int OFFSET_OF_PAIRS = 8;

    private final ObjectList<DecodedSwitchEntry> jumpTargets;

    /*
     * Note that offsetOfOriginalInstruction is data[-1]
     */
    public DecodedLookupSwitch(byte[] data, int offsetOfOriginalInstruction) {
        int curoffset = offsetOfOriginalInstruction + 1;
        int overflow = (curoffset % 4);
        int offset = overflow > 0 ? 4 - overflow : 0;

        ByteData bd = new BaseByteData(data);
        int defaultvalue = bd.getS4At(offset + OFFSET_OF_DEFAULT);
        int numpairs = bd.getS4At(offset + OFFSET_OF_NUMPAIRS);
        // Treemap so that targets are in bytecode order.
        Map<Integer, ObjectList<Integer>> uniqueTargets = MapFactory.newLazyMap(
            new TreeMap<>(),
            arg -> new ObjectArrayList<>()
        );
        uniqueTargets.get(defaultvalue).add(null);
        for (int x = 0; x < numpairs; ++x) {
            int value = bd.getS4At(offset + OFFSET_OF_PAIRS + (x * 8L));
            int target = bd.getS4At(offset + OFFSET_OF_PAIRS + (x * 8L) + 4);
            if (target != defaultvalue) {
                uniqueTargets.get(target).add(value);
            }
        }
        jumpTargets = new ObjectArrayList<>();
        for (Map.Entry<Integer, ObjectList<Integer>> entry : uniqueTargets.entrySet()) {
            jumpTargets.add(new DecodedSwitchEntry(entry.getValue(), entry.getKey()));
        }
    }

    @Override
    public ObjectList<DecodedSwitchEntry> getJumpTargets() {
        return jumpTargets;
    }
}
