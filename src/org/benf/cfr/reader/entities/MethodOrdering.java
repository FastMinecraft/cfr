package org.benf.cfr.reader.entities;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.attributes.AttributeLineNumberTable;

import java.util.ArrayList;
import java.util.Collections;
import it.unimi.dsi.fastutil.objects.ObjectList;

/*
 * Sort methods by line number, if there's an attribute table, if not, retain the order
 */
public class MethodOrdering  {

    private record OrderData(Method method, boolean hasLineNumber, int origIdx) implements Comparable<OrderData> {

        @Override
            public int compareTo(OrderData o) {
                if (hasLineNumber != o.hasLineNumber) {
                    return hasLineNumber ? -1 : 1;
                }
                return origIdx - o.origIdx;
            }
        }

    public static ObjectList<Method> sort(ObjectList<Method> methods) {
        ObjectList<OrderData> od = new ObjectArrayList<>();
        boolean hasLineNumbers = false;
        for (int x=0,len=methods.size();x<len;++x) {
            Method method = methods.get(x);
            boolean hasLineNumber = false;
            int idx = x - 100000; // Just to force methods WITHOUT data to the start in order.
            AttributeCode codeAttribute = method.getCodeAttribute();
            if (codeAttribute != null) {
                AttributeLineNumberTable lineNumberTable = codeAttribute.getLineNumberTable();
                if (lineNumberTable != null && lineNumberTable.hasEntries()) {
                    hasLineNumber = true;
                    hasLineNumbers = true;
                    idx = lineNumberTable.getStartLine();
                }
            }
            od.add(new OrderData(method, hasLineNumber, idx));
        }
        if (!hasLineNumbers) return methods;
        Collections.sort(od);
        ObjectList<Method> res = new ObjectArrayList<>(methods.size());
        for (OrderData o : od) {
            res.add(o.method);
        }
        return res;
    }

 }
