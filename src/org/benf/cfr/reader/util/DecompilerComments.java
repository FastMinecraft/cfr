package org.benf.cfr.reader.util;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.Collection;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class DecompilerComments implements Dumpable {
    private final ObjectSet<DecompilerComment> comments = new ObjectLinkedOpenHashSet<>();

    public DecompilerComments() {
    }

    public void addComment(String comment) {
        DecompilerComment decompilerComment = new DecompilerComment(comment);
        comments.add(decompilerComment);
    }

    public void addComment(DecompilerComment comment) {
        comments.add(comment);
    }

    public void addComments(Collection<DecompilerComment> comments) {
        this.comments.addAll(comments);
    }

    @Override
    public Dumper dump(Dumper d) {
        if (comments.isEmpty()) return d;
        d.beginBlockComment(false);
        for (DecompilerComment comment : comments) {
            d.dump(comment);
        }
        d.endBlockComment();
        return d;
    }

    public boolean contains(DecompilerComment comment) {
        return comments.contains(comment);
    }

    public Collection<DecompilerComment> getCommentCollection() {
        return comments;
    }

}
