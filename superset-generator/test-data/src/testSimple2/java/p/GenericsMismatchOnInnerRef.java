package p;

import p.data.GenericI;

public class GenericsMismatchOnInnerRef implements GenericI<GenericsMismatchOnInnerRef.MyInnerV2> {

    public static class MyInnerV2 {}
}
