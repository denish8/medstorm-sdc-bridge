package test.org.somda.common;

public class CIDetector {

    public static boolean isRunningInCi() {
        return System.getenv().containsKey("CI") || System.getenv().containsKey("GITLAB_CI");
    }

}
