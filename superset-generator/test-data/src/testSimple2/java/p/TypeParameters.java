package p;

import java.util.List;

public class TypeParameters {
    /* Unbounded -> Invariant = Unbounded */
    public TypeParameters(List<String> s) {
    }

    /* Invariant -> Super = Super */
    public TypeParameters(List<? super String> s, int p0) {}

    /* Invariant -> Extends = Extends */
    public TypeParameters(List<? extends String> s, int p0, int p1) {}

    /* Extends -> Super = Unbounded */
    public TypeParameters(List<? super String> s, int p0, int p1, int p3) {}
}
