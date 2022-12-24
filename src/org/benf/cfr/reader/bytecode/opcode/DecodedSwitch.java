package org.benf.cfr.reader.bytecode.opcode;

import it.unimi.dsi.fastutil.objects.ObjectList;

public interface DecodedSwitch {

    ObjectList<DecodedSwitchEntry> getJumpTargets();
}
