package mvdicarlo.crabmanmode;

public enum SortOption {
    NEW_TO_OLD("New to Old"),
    OLD_TO_NEW("Old to New"),
    ALPHABETICAL_ASC("Alphabetical A-Z"),
    ALPHABETICAL_DESC("Alphabetical Z-A");

    SortOption(String displayName) {
        this.displayName = displayName;
    }

    private final String displayName;

    public String getDisplayName() {
        return displayName;
    }
}