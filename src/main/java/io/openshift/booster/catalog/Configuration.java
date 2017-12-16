package io.openshift.booster.catalog;

public class Configuration {

    private static final String LAUNCHER_GIT_HOST = getEnvVarOrSysProp("LAUNCHER_GIT_HOST",
            "https://github.com/");

    private static final String LAUNCHER_BOOSTER_CATALOG_REPOSITORY = getEnvVarOrSysProp("LAUNCHER_BOOSTER_CATALOG_REPOSITORY",
            "https://github.com/fabric8-launcher/launcher-booster-catalog.git");

    private static final String LAUNCHER_CATALOG_BRANCH = getEnvVarOrSysProp("LAUNCHER_CATALOG_BRANCH",
            "openshift-online-free");

    public static boolean ignoreLocalZip() {
        return Boolean.getBoolean("BOOSTER_CATALOG_IGNORE_LOCAL")
                || Boolean.parseBoolean(System.getenv("BOOSTER_CATALOG_IGNORE_LOCAL"));
    }

    public static String catalogRepositoryURI() {
        return LAUNCHER_BOOSTER_CATALOG_REPOSITORY;
    }

    public static String launcherGitHost() {
        return LAUNCHER_GIT_HOST;
    }

    public static String catalogRepositoryBranch() {
        return LAUNCHER_CATALOG_BRANCH;
    }

    private static String getEnvVarOrSysProp(String name, String defaultValue) {
        return System.getProperty(name, System.getenv().getOrDefault(name, defaultValue));
    }
}
