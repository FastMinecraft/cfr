package org.benf.cfr.reader.bytecode.analysis.types;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.collections.MapFactory;

import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Map;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * FIXME - this class has multiple ways of implementing the same thing - definitely feels redundant!
 */
public class GenericTypeBinder {
    private final Map<String, JavaTypeInstance> nameToBoundType;

    private GenericTypeBinder(Map<String, JavaTypeInstance> nameToBoundType) {
        this.nameToBoundType = nameToBoundType;
    }

    // TODO : This seems wrong.
    public static GenericTypeBinder createEmpty() {
        return new GenericTypeBinder(MapFactory.newMap());
    }

    @SafeVarargs
    public static GenericTypeBinder create(ObjectList<FormalTypeParameter> ... ftps) {
        Map<String, JavaTypeInstance> bounds = MapFactory.newMap();
        for (ObjectList<FormalTypeParameter> ftp : ftps) {
            if (ftp == null) continue;
            for (FormalTypeParameter f : ftp) {
                bounds.put(f.getName(), f.getBound());
            }
        }
        return new GenericTypeBinder(bounds);
    }

    static GenericTypeBinder bind(ObjectList<FormalTypeParameter> methodFormalTypeParameters,
                                  ClassSignature classSignature, ObjectList<JavaTypeInstance> args,
                                  JavaGenericRefTypeInstance boundInstance, ObjectList<JavaTypeInstance> boundArgs) {
        Map<String, JavaTypeInstance> nameToBoundType = MapFactory.newMap();

        if (boundInstance != null) {    // null for static.
            ObjectList<FormalTypeParameter> unboundParameters = classSignature.formalTypeParameters();
            ObjectList<JavaTypeInstance> boundParameters = boundInstance.getGenericTypes();

            if (unboundParameters == null || boundParameters.size() != unboundParameters.size()) {
                // I suspect this will happen all the time, but on the face of it I can't see why it should
                // be valid right now.

                // SCALA causes a lot of this.
                return null;
            }

            for (int x = 0; x < boundParameters.size(); ++x) {
                nameToBoundType.put(unboundParameters.get(x).getName(), boundParameters.get(x));
            }
        }

        ObjectList<FormalTypeParameter> classFormalTypeParamters = classSignature.formalTypeParameters();
        // TODO: Pretty sure this is a tautology given the calling pattern.

        GenericTypeBinder res = new GenericTypeBinder(nameToBoundType);

        if ((methodFormalTypeParameters != null && !methodFormalTypeParameters.isEmpty()) ||
                (classFormalTypeParamters != null && !classFormalTypeParamters.isEmpty())) {
            if (args.size() != boundArgs.size())
                throw new IllegalArgumentException(); // should be verified before we get here!


            for (int x = 0; x < args.size(); ++x) {
                JavaTypeInstance unbound = args.get(x);
                JavaTypeInstance bound = boundArgs.get(x);
                if (bound.getDeGenerifiedType() != unbound.getDeGenerifiedType()
                   && unbound instanceof JavaGenericRefTypeInstance && ((JavaGenericRefTypeInstance) unbound).hasUnbound()) {
                    BindingSuperContainer bindingSupers = bound.getBindingSupers();
                    if (bindingSupers != null) {
                        JavaGenericRefTypeInstance boundAppropriate = bindingSupers.getBoundSuperForBase(unbound.getDeGenerifiedType());
                        if (boundAppropriate != null && !boundAppropriate.hasUnbound()) {
                            bound = boundAppropriate;
                        }
                    }
                }
                if (unbound instanceof JavaArrayTypeInstance && bound instanceof JavaArrayTypeInstance) {
                    if (unbound.getNumArrayDimensions() == bound.getNumArrayDimensions()) {
                        unbound = unbound.getArrayStrippedType();
                        bound = bound.getArrayStrippedType();
                    }
                }
                if (unbound instanceof JavaGenericBaseInstance unboundGeneric) {
                    unboundGeneric.tryFindBinding(bound, res);
                }
            }
        }

        return res;
    }

    public static GenericTypeBinder buildIdentityBindings(JavaGenericRefTypeInstance unbound) {
        ObjectList<JavaTypeInstance> typeParameters = unbound.getGenericTypes();

        Map<String, JavaTypeInstance> unboundNames = MapFactory.newMap();
        //noinspection ForLoopReplaceableByForEach
        for (int x = 0, len = typeParameters.size(); x < len; ++x) {
            JavaTypeInstance unboundParam = typeParameters.get(x);
            if (!(unboundParam instanceof JavaGenericPlaceholderTypeInstance)) {
                throw new ConfusedCFRException("Unbound parameter expected to be placeholder!");
            }
            unboundNames.put(unboundParam.getRawName(), unboundParam);
        }
        return new GenericTypeBinder(unboundNames);
    }

    /*
     * Extra faffing if we don't know that the two classes are the same.
     */
    public static GenericTypeBinder extractBaseBindings(JavaGenericBaseInstance unbound, JavaTypeInstance maybeBound) {
        if (!(unbound instanceof JavaGenericRefTypeInstance unboundGeneric)) return extractBindings(unbound, maybeBound);
        if (!(maybeBound instanceof JavaGenericRefTypeInstance maybeBoundGeneric)) return extractBindings(unbound, maybeBound);

        BindingSuperContainer maybeBindingContainer = maybeBound.getBindingSupers();
        JavaTypeInstance boundAssignable = maybeBindingContainer.getBoundAssignable(maybeBoundGeneric, unboundGeneric);

        return extractBindings(unboundGeneric, boundAssignable);
    }


    public static GenericTypeBinder extractBindings(JavaGenericBaseInstance unbound, JavaTypeInstance maybeBound) {
        Map<String, JavaTypeInstance> boundNames = MapFactory.newMap();
        doBind(boundNames, unbound, maybeBound);
        return new GenericTypeBinder(boundNames);
    }

    private static void doBind(Map<String, JavaTypeInstance> boundNames,
                               JavaGenericBaseInstance unbound, JavaTypeInstance maybeBound) {

        if (unbound.getClass() == JavaGenericPlaceholderTypeInstance.class) {
            JavaGenericPlaceholderTypeInstance placeholder = (JavaGenericPlaceholderTypeInstance) unbound;
            boundNames.put(placeholder.getRawName(), maybeBound);
            return;
        }

        ObjectList<JavaTypeInstance> typeParameters = unbound.getGenericTypes();


        if (!(maybeBound instanceof JavaGenericBaseInstance bound)) {
            return;
        }

        ObjectList<JavaTypeInstance> boundTypeParameters = bound.getGenericTypes();
        if (typeParameters.size() != boundTypeParameters.size()) {
            return;
//            throw new IllegalStateException("Generic info mismatch");
        }

        for (int x = 0, len = typeParameters.size(); x < len; ++x) {
            JavaTypeInstance unboundParam = typeParameters.get(x);
            JavaTypeInstance boundParam = boundTypeParameters.get(x);
            if (!(unboundParam instanceof JavaGenericBaseInstance)) {
                continue;
            }
            doBind(boundNames, (JavaGenericBaseInstance) unboundParam, boundParam);
        }
    }

    public void removeBinding(JavaGenericPlaceholderTypeInstance type) {
        String name = type.getRawName();
        nameToBoundType.remove(name);
    }

    JavaTypeInstance getBindingFor(FormalTypeParameter formalTypeParameter) {
        return nameToBoundType.get(formalTypeParameter.getName());
    }

    public JavaTypeInstance getBindingFor(JavaTypeInstance maybeUnbound) {
        if (maybeUnbound instanceof JavaGenericPlaceholderTypeInstance placeholder) {
            String name = placeholder.getRawName();
            JavaTypeInstance bound = nameToBoundType.get(name);
            if (bound != null) {
                return bound;
            }
        } else if (maybeUnbound instanceof JavaGenericBaseInstance) {
            return ((JavaGenericBaseInstance) maybeUnbound).getBoundInstance(this);
        } else if (maybeUnbound instanceof JavaArrayTypeInstance ja) {
            JavaTypeInstance jaStripped = ja.getArrayStrippedType();
            JavaTypeInstance bindingFor = getBindingFor(jaStripped);
            if (!jaStripped.equals(bindingFor)) {
                return new JavaArrayTypeInstance(ja.getNumArrayDimensions(), bindingFor);
            }
        }
        return maybeUnbound;
    }


    private static boolean isBetterBinding(JavaTypeInstance isBetter, JavaTypeInstance than) {
        if (than == null) return true;
        return !(isBetter instanceof JavaGenericPlaceholderTypeInstance);
    }

    public void suggestOnlyNullBinding(JavaGenericPlaceholderTypeInstance type) {
        String name = type.getRawName();
        if (nameToBoundType.containsKey(name)) return;
        nameToBoundType.put(name, TypeConstants.OBJECT);
    }

    public void suggestBindingFor(String name, JavaTypeInstance binding) {
        JavaTypeInstance alreadyBound = nameToBoundType.get(name);
        if (isBetterBinding(binding, alreadyBound)) {
            nameToBoundType.put(name, binding);
        }
    }

    public GenericTypeBinder mergeWith(GenericTypeBinder other, boolean mergeToCommonClass) {
        ObjectSet<String> keys = new ObjectOpenHashSet<>(nameToBoundType.keySet());
        keys.addAll(other.nameToBoundType.keySet());
        Map<String, JavaTypeInstance> res = MapFactory.newMap();
        for (String key : keys) {
            JavaTypeInstance t1 = nameToBoundType.get(key);
            JavaTypeInstance t2 = other.nameToBoundType.get(key);
            if (t1 == null) {
                res.put(key, t2);
                continue;
            }
            if (t2 == null) {
                res.put(key, t1);
                continue;
            }
            /*
             * Ok. Try to merge. Find highest common base class.
             * If completely incompatible, return null.
             */
            if (mergeToCommonClass) {
                if (t1.implicitlyCastsTo(t2, other)) {
                    res.put(key, t2);
                    continue;
                }
                if (t2.implicitlyCastsTo(t1, other)) {
                    res.put(key, t1);
                    continue;
                }
                /*
                 * Nope.  Ok, find a common BASE of t1 and t2.
                 */
                InferredJavaType clash = InferredJavaType.mkClash(t1, t2);
                clash.collapseTypeClash();
                res.put(key, clash.getJavaTypeInstance());
                continue;
            }
            return null;
        }
        return new GenericTypeBinder(res);
    }


    /*
     * Given that we have a type on the left we know (eg a return type), and a type on the right
     * which we're unsure of, can we use information about how the type on the left would be defined
     * if it was the type on the right?
     *
     * eg
     *
     * Pair<? extends Foo, Interface> function() {
     *    return new FlippedPair(new Impl(), FooBar);
     * }
     *
     * We can improve the declaration to
     *
     * new FlippedPair<Bar, Interface>(new Impl(), FooBar);
     */
    public GenericTypeBinder createAssignmentRhsBindings(GenericTypeBinder rhsBinder) {
        // First - check compatibility.
        if (!nameToBoundType.keySet().equals(rhsBinder.nameToBoundType.keySet())) return null;

        Map<String, JavaTypeInstance> resultMap = MapFactory.newMap();
        for (Map.Entry<String, JavaTypeInstance> entry : nameToBoundType.entrySet()) {
            String key = entry.getKey();
            JavaTypeInstance lhstype = entry.getValue();
            JavaTypeInstance rhstype = rhsBinder.nameToBoundType.get(key);
            JavaTypeInstance lhsStripped = lhstype.getDeGenerifiedType();
            JavaTypeInstance rhsStripped = rhstype.getDeGenerifiedType();
            if (!lhsStripped.equals(rhsStripped) && !(rhstype instanceof JavaGenericPlaceholderTypeInstance)) {
                BindingSuperContainer rhsBoundSupers = rhstype.getBindingSupers();
                if (rhsBoundSupers == null || !rhsBoundSupers.containsBase(lhstype.getDeGenerifiedType())) return null;
            }
            // Ok, rhstype extends/= lhstype.
            JavaTypeInstance bestGuess;
            if (lhstype instanceof JavaWildcardTypeInstance) {
                bestGuess = rhstype;
            } else {
                bestGuess = lhstype;
            }
            if (bestGuess instanceof JavaGenericPlaceholderTypeInstance) return null;
            resultMap.put(key, bestGuess);
        }
        return new GenericTypeBinder(resultMap);
    }
}
