package p;


public class Annotated {

    @MyAnnotation("same")
    public void one() {}

    @MyAnnotation("v1")
    public void different() {}
}
