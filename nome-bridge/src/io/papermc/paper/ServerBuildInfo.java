package io.papermc.paper;

import java.time.Instant;
import java.util.Optional;

public interface ServerBuildInfo {

    static ServerBuildInfo buildInfo() {
        return Holder.INSTANCE;
    }

    String brandId();
    String brandName();
    String minecraftVersionId();
    String minecraftVersionName();
    Optional<String> gitCommit();
    Optional<Integer> buildNumber();
    Optional<Instant> buildTime();

    class Holder {
        static final ServerBuildInfo INSTANCE = new ServerBuildInfo() {
            @Override public String brandId() { return "nome"; }
            @Override public String brandName() { return "Nome"; }
            @Override public String minecraftVersionId() { return "26.3"; }
            @Override public String minecraftVersionName() { return "26.3"; }
            @Override public Optional<String> gitCommit() { return Optional.of("git-Paper-26.3-NATIVE"); }
            @Override public Optional<Integer> buildNumber() { return Optional.of(169); }
            @Override public Optional<Instant> buildTime() { return Optional.of(Instant.now()); }
        };
    }
}
