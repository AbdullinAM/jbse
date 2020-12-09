package jbse.apps.run;

public class Statistics {
    private final long totalPaths;
    private final long safePaths;
    private final long unsafePaths;
    private final long outOfScopePaths;
    private final long violatingPaths;
    private final long unmanageablePaths;

    public Statistics() {
        this(0L, 0L, 0L, 0L, 0L, 0L);
    }

    public Statistics(long totalPaths, long safePaths, long unsafePaths, long outOfScopePaths, long violatingPaths, long unmanageablePaths) {
        this.totalPaths = totalPaths;
        this.safePaths = safePaths;
        this.unsafePaths = unsafePaths;
        this.outOfScopePaths = outOfScopePaths;
        this.violatingPaths = violatingPaths;
        this.unmanageablePaths = unmanageablePaths;
    }

    @Override
    public String toString() {
        return "Analyzed paths: " + totalPaths +
                ", Safe: " + safePaths +
                ", Unsafe: " + unsafePaths +
                ", Out of scope: " + outOfScopePaths +
                ", Violating assumptions: " + violatingPaths +
                ", Unmanageable: " + unmanageablePaths +
                ".";
    }

    public Statistics plus(Statistics other) {
        return new Statistics(
                totalPaths + other.totalPaths,
                safePaths + other.safePaths,
                unsafePaths + other.unsafePaths,
                outOfScopePaths + other.outOfScopePaths,
                violatingPaths + other.violatingPaths,
                unmanageablePaths + other.unmanageablePaths
                );
    }
}
