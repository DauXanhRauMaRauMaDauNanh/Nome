package org.bukkit.configuration.file;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;

public abstract class FileConfiguration extends MemoryConfiguration {
    public FileConfiguration() {
        super();
    }

    public FileConfiguration(Configuration defaults) {
        super(defaults);
    }

    public void save(File file) throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        String data = saveToString();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(data);
        }
    }

    public void save(String file) throws IOException {
        save(new File(file));
    }

    public abstract String saveToString();

    public void load(File file) throws IOException, InvalidConfigurationException {
        try (FileInputStream stream = new FileInputStream(file)) {
            load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    public void load(Reader reader) throws IOException, InvalidConfigurationException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader input = new BufferedReader(reader)) {
            String line;
            while ((line = input.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        loadFromString(builder.toString());
    }

    public void load(String file) throws IOException, InvalidConfigurationException {
        load(new File(file));
    }

    public abstract void loadFromString(String contents) throws InvalidConfigurationException;

    @Override
    public FileConfigurationOptions options() {
        if (options == null) {
            options = new FileConfigurationOptions(this);
        }
        return (FileConfigurationOptions) options;
    }
}
