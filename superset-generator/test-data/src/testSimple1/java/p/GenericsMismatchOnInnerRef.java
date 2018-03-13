package p;

import p.data.GenericI;

public class GenericsMismatchOnInnerRef implements GenericI<GenericsMismatchOnInnerRef.MyInnerV1> {

    public static class MyInnerV1 {}
}
