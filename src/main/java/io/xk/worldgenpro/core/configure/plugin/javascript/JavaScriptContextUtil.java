package io.xk.worldgenpro.core.configure.plugin.javascript;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.io.FileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.*;

public class JavaScriptContextUtil {

    public static void debugVFS(VFSNode root) {
        debugVFS(root, "");
    }

    private static void debugVFS(VFSNode node, String indent) {
        for (Map.Entry<String, VFSNode> entry : node.children.entrySet()) {
            String name = entry.getKey();
            VFSNode child = entry.getValue();
            if (child.content == null) {
                // 目录
                System.out.println(indent + "[DIR] " + name);
                debugVFS(child, indent + "    ");
            } else {
                // 文件
                System.out.println(indent + "[FILE] " + name + " -> content length: " + child.content.length());
            }
        }
    }

    // 使用 Map 模拟嵌套目录结构
    private static class VFSNode {
        Map<String, VFSNode> children = new HashMap<>();
        String content = null; // null表示目录，非null表示文件内容
    }

    public static Context getNewContext(ObjectArrayList<JavaScriptPluginLoader.JavaScriptFile> allJSFile, String namespace) {
        VFSNode root = new VFSNode();

        // 构建虚拟文件系统树
        for (JavaScriptPluginLoader.JavaScriptFile file : allJSFile) {
            String[] parts = file.filePath.split("/");
            VFSNode current = root;
            for (int i = 0; i < parts.length; i++) {
                current = current.children.computeIfAbsent(parts[i], k -> new VFSNode());
            }
            current.content = file.source;
        }

        // 创建 Context 并提供 FileSystem
        return Context.newBuilder("js")
                .fileSystem(new FileSystem() {

                    private VFSNode resolve(Path path) {
                        String[] parts = path.toString().split("\\\\");
                        VFSNode current = root;
                        for (String part : parts) {
                            if (part.isEmpty()) continue;
                            current = current.children.get(part);
                            if (current == null) return null;
                        }
                        return current;
                    }

                    @Override
                    public Path parsePath(URI uri) {
                        return Paths.get(uri.getPath());
                    }

                    @Override
                    public Path parsePath(String path) {
                        return Paths.get(path);
                    }

                    @Override
                    public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
                        if (resolve(path) == null) throw new NoSuchFileException(path.toString());
                    }

                    @Override
                    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
                        throw new UnsupportedOperationException("VFS is read-only");
                    }

                    @Override
                    public void delete(Path path) throws IOException {
                        throw new UnsupportedOperationException("VFS is read-only");
                    }

                    @Override
                    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
                        VFSNode node = resolve(path);
                        if (node == null || node.content == null) throw new NoSuchFileException(path.toString());
                        byte[] bytes = node.content.getBytes();
                        return new SeekableByteChannel() {
                            private int pos = 0;
                            @Override public int read(ByteBuffer dst) {
                                if (pos >= bytes.length) return -1;
                                int n = Math.min(dst.remaining(), bytes.length - pos);
                                dst.put(bytes, pos, n);
                                pos += n;
                                return n;
                            }
                            @Override public int write(ByteBuffer src) { throw new UnsupportedOperationException(); }
                            @Override public long position() { return pos; }
                            @Override public SeekableByteChannel position(long newPosition) { pos = (int)newPosition; return this; }
                            @Override public long size() { return bytes.length; }
                            @Override public SeekableByteChannel truncate(long size) { throw new UnsupportedOperationException(); }
                            @Override public boolean isOpen() { return true; }
                            @Override public void close() {}
                        };
                    }

                    @Override
                    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
                        VFSNode node = resolve(dir);
                        if (node == null) throw new NotDirectoryException(dir.toString());
                        List<Path> paths = new ArrayList<>();
                        for (String name : node.children.keySet()) paths.add(Paths.get(name));
                        return new DirectoryStream<Path>() {
                            @Override public Iterator<Path> iterator() { return paths.iterator(); }
                            @Override public void close() {}
                        };
                    }

                    @Override
                    public Path toAbsolutePath(Path path) { return path; }

                    @Override
                    public Path toRealPath(Path path, LinkOption... linkOptions) { return path; }

                    @Override
                    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) { return Map.of(); }

                })
                .allowIO(true)
                .allowExperimentalOptions(true)
                .allowAllAccess(true)
                .build();
    }
}
