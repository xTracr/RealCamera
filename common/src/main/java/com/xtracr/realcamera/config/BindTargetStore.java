package com.xtracr.realcamera.config;

import com.google.gson.Gson;
import com.xtracr.realcamera.RealCamera;
import com.xtracr.realcamera.config.binding.OffsetConfig;
import com.xtracr.realcamera.config.binding.RenderConfig;
import com.xtracr.realcamera.config.binding.TargetConfig;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class BindTargetStore {
    private static final String EXTENSION = ".json";
    private final Gson gson;
    private final Path directory;
    private final Map<Identifier, BindTarget> targets = new LinkedHashMap<>();
    /** Content last written for each target, so that {@link #save()} only touches files that changed. */
    private final Map<Identifier, String> persisted = new HashMap<>();

    public BindTargetStore(Gson gson, Path directory) {
        this.gson = gson;
        this.directory = directory;
    }

    public void load() {
        targets.clear();
        persisted.clear();
        scanDirectory();
    }

    public void ensureDefaults() {
        if (!targets.isEmpty()) return;
        BindTarget.defaults().forEach(this::put);
        save();
    }

    private void scanDirectory() {
        if (!Files.isDirectory(directory)) return;
        List<Path> files;
        try (var paths = Files.walk(directory)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(EXTENSION))
                    .toList();
        } catch (IOException exception) {
            RealCamera.LOGGER.warn("Failed to list binding targets in {}", directory, exception);
            return;
        }
        for (Path path : files) loadFile(path);
    }

    /**
     * Writes the targets that changed since the last save and removes files of deleted targets.
     *
     * @return whether every file operation succeeded
     */
    public boolean save() {
        boolean success = true;
        for (BindTarget target : targets.values()) {
            String content;
            try {
                content = gson.toJson(TargetFile.of(target));
            } catch (RuntimeException exception) {
                RealCamera.LOGGER.warn("Failed to serialize binding target {}", target.id(), exception);
                success = false;
                continue;
            }
            if (content.equals(persisted.get(target.id()))) continue;
            Path path = pathOf(target.id());
            if (path == null) {
                RealCamera.LOGGER.warn("Skipping binding target with an unsupported identifier: {}", target.id());
                success = false;
                continue;
            }
            if (write(path, content)) persisted.put(target.id(), content);
            else success = false;
        }
        for (Identifier id : List.copyOf(persisted.keySet())) {
            if (targets.containsKey(id)) continue;
            Path path = pathOf(id);
            if (path != null) delete(path);
            persisted.remove(id);
        }
        return success;
    }

    public @Nullable BindTarget find(Identifier id) {
        return targets.get(id);
    }

    public BindTarget get(Identifier id) {
        return targets.computeIfAbsent(id, BindTargetStore::emptyTarget);
    }

    public OffsetConfig offsets(Identifier id) {
        return get(id).offsets();
    }

    public RenderConfig renderConfig(Identifier id) {
        return get(id).renderConfig();
    }

    /**
     * All targets, ordered by priority (highest first). The identifier breaks ties so that the order does not
     * depend on the order in which the files were read.
     */
    public List<BindTarget> all() {
        List<BindTarget> sorted = new ArrayList<>(targets.values());
        sorted.sort(Comparator.comparingInt(BindTarget::priority).reversed().thenComparing(target -> target.id().toString()));
        return sorted;
    }

    public void put(BindTarget target) {
        targets.put(target.id(), target);
    }

    public void putIfAbsent(BindTarget target) {
        BindTarget existing = targets.get(target.id());
        if (existing == null) {
            targets.put(target.id(), target);
        } else if (existing.targetConfig().isEmpty() && !target.targetConfig().isEmpty()) {
            targets.put(target.id(), new BindTarget(target.id(), target.targetConfig(), existing.renderConfig(), existing.offsets(), existing.priority()));
        }
    }

    public boolean remove(Identifier id) {
        return targets.remove(id) != null;
    }

    private void loadFile(Path path) {
        Identifier id = idOf(path);
        if (id == null) {
            RealCamera.LOGGER.warn("Skipping binding target file with an unsupported name: {}", path);
            return;
        }
        String content;
        try {
            content = Files.readString(path);
        } catch (IOException exception) {
            RealCamera.LOGGER.warn("Failed to read binding target file {}", path, exception);
            return;
        }
        TargetFile file;
        try {
            file = gson.fromJson(content, TargetFile.class);
        } catch (RuntimeException exception) {
            RealCamera.LOGGER.warn("Failed to parse binding target file {}", path, exception);
            return;
        }
        if (file == null) return;
        targets.put(id, file.toBindTarget(id));
        persisted.put(id, content);
    }

    private @Nullable Path pathOf(Identifier id) {
        // Identifiers may contain "." and ".." path segments, which must never escape the target directory.
        for (String segment : id.getPath().split("/")) {
            if (segment.equals(".") || segment.equals("..")) return null;
        }
        return directory.resolve(id.getNamespace()).resolve(id.getPath() + EXTENSION);
    }

    private @Nullable Identifier idOf(Path path) {
        Path relative = directory.relativize(path);
        int count = relative.getNameCount();
        if (count < 2) return null;
        String namespace = relative.getName(0).toString();
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < count; i++) {
            if (!builder.isEmpty()) builder.append('/');
            String name = relative.getName(i).toString();
            builder.append(i == count - 1 ? name.substring(0, name.length() - EXTENSION.length()) : name);
        }
        String targetPath = builder.toString();
        Identifier id = Identifier.tryParse(namespace + ":" + targetPath);
        if (id == null || !id.getNamespace().equals(namespace) || !id.getPath().equals(targetPath)) return null;
        return id;
    }

    private boolean write(Path path, String content) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temp, content);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            RealCamera.LOGGER.warn("Failed to write binding target file {}", path, exception);
            return false;
        }
    }

    private void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            RealCamera.LOGGER.warn("Failed to delete binding target file {}", path, exception);
        }
    }

    private static BindTarget emptyTarget(Identifier id) {
        return new BindTarget(id, TargetConfig.EMPTY, new RenderConfig(), new OffsetConfig(), BindTarget.defaultPriority(id));
    }

    /**
     * On-disk representation of a single target; the id is given by the file location.
     */
    private record TargetFile(TargetConfig target, OffsetConfig offsets, RenderConfig render, int priority) {
        private TargetFile {
            target = target == null ? TargetConfig.EMPTY : target;
            offsets = offsets == null ? new OffsetConfig() : offsets;
            render = render == null ? new RenderConfig() : render;
        }

        private static TargetFile of(BindTarget target) {
            return new TargetFile(target.targetConfig(), target.offsets(), target.renderConfig(), target.priority());
        }

        private BindTarget toBindTarget(Identifier id) {
            offsets.clamp();
            return new BindTarget(id, target, render, offsets, priority);
        }
    }
}
