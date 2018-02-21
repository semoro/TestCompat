package p;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WithNullability {
    /* @Nullable */
    public String wasDefault() {
        return null;
    }

    @NotNull
    public String madeDefault() {
        return "";
    }


    @Nullable
    public String becameNotNull() {
        return null;
    }

    @NotNull
    public String becameNullable() {
        return "";
    }

    public void parameterNullability(@NotNull String param) {
    }

    public void parameterNullability1(@NotNull String s) {
    }

    public void parameterNullability2(@NotNull String s) {
    }
}
