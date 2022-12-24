package org.benf.cfr.reader.util;

import it.unimi.dsi.fastutil.objects.ObjectList;

public interface DecompilerCommentSource {
    ObjectList<DecompilerComment> getComments();
}
