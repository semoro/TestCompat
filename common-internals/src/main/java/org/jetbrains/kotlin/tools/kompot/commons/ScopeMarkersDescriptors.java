package org.jetbrains.kotlin.tools.kompot.commons;

public class ScopeMarkersDescriptors {


    // language=JAVA prefix="class A {" suffix=" a = null; }"
    public static final String scopeMarkersContainerFqName = "org.jetbrains.kotlin.tools.kompot.api.intrinsics.ScopeMarkers";


    // language=JAVA prefix="class A { void a() { ScopeMarkers." suffix="(); } }"
    public static final String enterVersionScopeDesc = "enterVersionScope";

    // language=JAVA prefix="class A { void a() { ScopeMarkers." suffix="(); } }"
    public static final String leaveVersionScopeDesc = "leaveVersionScope";
}
