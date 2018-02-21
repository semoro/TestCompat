package p;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WithNullability {
    @Nullable
    public String wasDefault() {
        return null;
    }

    /* @NotNull */
    public String madeDefault() {
        return "";
    }


    @NotNull /* @Nullable */
    public String becameNotNull() {
        return "";
    }

    @Nullable /* @NotNull */
    public String becameNullable() {
        return "";
    }
}
