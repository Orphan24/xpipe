package io.xpipe.core.process;

import io.xpipe.core.store.FileNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface OsType {

    Windows WINDOWS = new Windows();
    Linux LINUX = new Linux();
    MacOs MACOS = new MacOs();
    Bsd BSD = new Bsd();
    Solaris SOLARIS = new Solaris();

    static Local getLocal() {
        String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((osName.contains("mac")) || (osName.contains("darwin"))) {
            return MACOS;
        } else if (osName.contains("win")) {
            return WINDOWS;
        } else {
            return LINUX;
        }
    }

    String makeFileSystemCompatible(String name);

    List<String> determineInterestingPaths(ShellControl pc) throws Exception;

    String getUserHomeDirectory(ShellControl pc) throws Exception;

    String getFileSystemSeparator();

    String getName();

    sealed interface Local extends OsType permits OsType.Windows, OsType.Linux, OsType.MacOs {

        String getId();

        default Any toAny() {
            return (Any) this;
        }
    }

    sealed interface Any extends OsType
            permits OsType.Windows, OsType.Linux, OsType.MacOs, OsType.Solaris, OsType.Bsd {}

    final class Windows implements OsType, Local, Any {

        @Override
        public String makeFileSystemCompatible(String name) {
            return name.replaceAll("[<>:\"/\\\\|?*]", "_").replaceAll("\\p{C}", "");
        }

        @Override
        public List<String> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = getUserHomeDirectory(pc);
            return List.of(
                    home,
                    FileNames.join(home, "Documents"),
                    FileNames.join(home, "Downloads"),
                    FileNames.join(home, "Desktop"));
        }

        @Override
        public String getUserHomeDirectory(ShellControl pc) throws Exception {
            return pc.executeSimpleStringCommand(
                    pc.getShellDialect().getPrintEnvironmentVariableCommand("USERPROFILE"));
        }

        @Override
        public String getFileSystemSeparator() {
            return "\\";
        }

        @Override
        public String getName() {
            return "Windows";
        }

        @Override
        public String getId() {
            return "windows";
        }
    }

    class Unix implements OsType {

        @Override
        public String makeFileSystemCompatible(String name) {
            // Technically the backslash is supported, but it causes all kinds of troubles, so we also exclude it
            return name.replaceAll("/\\\\", "_").replaceAll("\0", "");
        }

        @Override
        public List<String> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = getUserHomeDirectory(pc);
            var list = new ArrayList<>(List.of(
                    home,
                    FileNames.join(home, "Downloads"),
                    FileNames.join(home, "Documents"),
                    "/etc",
                    pc.getSystemTemporaryDirectory().toString(),
                    "/var"));
            var parentHome = FileNames.getParent(home);
            if (parentHome != null && !parentHome.equals("/")) {
                list.add(3, parentHome);
            }
            return list;
        }

        @Override
        public String getUserHomeDirectory(ShellControl pc) throws Exception {
            var r = pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("HOME"));
            if (r.isBlank()) {
                var user = pc.view().user();
                var eval = pc.command("eval echo ~" + user).readStdoutIfPossible();
                if (eval.isPresent() && !eval.get().isBlank()) {
                    return eval.get();
                }

                if (user.equals("root")) {
                    return "/root";
                } else {
                    return "/home/" + user;
                }
            } else {
                return r;
            }
        }

        @Override
        public String getFileSystemSeparator() {
            return "/";
        }

        @Override
        public String getName() {
            return "Linux";
        }
    }

    final class Linux extends Unix implements OsType, Local, Any {

        @Override
        public String getId() {
            return "linux";
        }
    }

    final class Solaris extends Unix implements Any {}

    final class Bsd extends Unix implements Any {}

    final class MacOs implements OsType, Local, Any {

        @Override
        public String getId() {
            return "macos";
        }

        @Override
        public String makeFileSystemCompatible(String name) {
            // Technically the backslash is supported, but it causes all kinds of troubles, so we also exclude it
            return name.replaceAll("[\\\\/:]", "_").replaceAll("\0", "");
        }

        @Override
        public List<String> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = getUserHomeDirectory(pc);
            return List.of(
                    home,
                    FileNames.join(home, "Downloads"),
                    FileNames.join(home, "Documents"),
                    FileNames.join(home, "Desktop"),
                    "/Applications",
                    "/Library",
                    "/System",
                    "/etc");
        }

        @Override
        public String getUserHomeDirectory(ShellControl pc) throws Exception {
            return pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("HOME"));
        }

        @Override
        public String getFileSystemSeparator() {
            return "/";
        }

        @Override
        public String getName() {
            return "Mac";
        }
    }
}
