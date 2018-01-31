package io.fabric8.launcher.booster.catalog;

public class LauncherConfiguration {

    public interface PropertyName {
        String LAUNCHER_BOOSTER_CATALOG_REPOSITORY = "LAUNCHER_BOOSTER_CATALOG_REPOSITORY";
        String LAUNCHER_BOOSTER_CATALOG_REF = "LAUNCHER_BOOSTER_CATALOG_REF";
        String LAUNCHER_BOOSTER_CATALOG_IGNORE_LOCAL = "LAUNCHER_BOOSTER_CATALOG_IGNORE_LOCAL";
    }

    private static final String LAUNCHER_BOOSTER_CATALOG_REPOSITORY = getEnvVarOrSysProp(PropertyName.LAUNCHER_BOOSTER_CATALOG_REPOSITORY,
            "https://github.com/fabric8-launcher/launcher-booster-catalog.git");

    private static final String LAUNCHER_BOOSTER_CATALOG_REF = getEnvVarOrSysProp(PropertyName.LAUNCHER_BOOSTER_CATALOG_REF,
            "master");

    public static boolean ignoreLocalZip() {
        return Boolean.getBoolean(PropertyName.LAUNCHER_BOOSTER_CATALOG_IGNORE_LOCAL)
                || Boolean.parseBoolean(System.getenv(PropertyName.LAUNCHER_BOOSTER_CATALOG_IGNORE_LOCAL));
    }

    public static String boosterCatalogRepositoryURI() {
        return LAUNCHER_BOOSTER_CATALOG_REPOSITORY;
    }

    public static String boosterCatalogRepositoryRef() {
        return LAUNCHER_BOOSTER_CATALOG_REF;
    }

    private static String getEnvVarOrSysProp(String name, String defaultValue) {
        return System.getProperty(name, System.getenv().getOrDefault(name, defaultValue));
    }
}
