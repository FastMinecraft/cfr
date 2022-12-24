package org.benf.cfr.reader.entities.attributes;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * @param segments If the value of the type_path_kind item is 0, 1, or 2, then the value of the type_argument_index item is 0.
 *                 <p>
 *                 If the value of the type_path_kind item is 3, then the value of the type_argument_index item specifies which
 *                 type argument of a parameterized type is annotated, where 0 indicates the first type argument of a parameterized type.
 */
public record TypePath(ObjectList<TypePathPart> segments) {

}
