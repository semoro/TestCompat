package api;

import org.jetbrains.kotlin.tools.kompot.api.annotations.*;

public class SomeApi {

    @ExistsIn(version = "173")
    public void foo(){}

    @ExistsIn(version = "174")
    public void foo(int i){}


}