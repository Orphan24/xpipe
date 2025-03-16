package io.xpipe.app.prefs;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;

import java.util.Arrays;
import java.util.Locale;

public class ExternalApplicationHelper {

    public static String replaceFileArgument(String format, String variable, String file) {
        // Support for legacy variables that were not upper case
        variable = variable.toUpperCase(Locale.ROOT);
        format = format.replace("$" + variable.toLowerCase(Locale.ROOT), "$" + variable.toUpperCase(Locale.ROOT));

        var fileString = file.contains(" ") ? "\"" + file + "\"" : file;
        // Check if the variable is already quoted
        return format.replace("\"$" + variable + "\"", fileString).replace("$" + variable, fileString);
    }

    public static void startAsync(String raw) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            if (ShellDialects.isPowershell(sc)) {
                // Do the best effort here
                // This does not respect quoting rules, but otherwise powershell wouldn't work at all
                var split = raw.split("\\s+");
                var splitBuilder = CommandBuilder.of().addAll(Arrays.asList(split));
                startAsync(splitBuilder);
            } else {
                startAsync(CommandBuilder.ofString(raw));
            }
        }
    }

    public static void startAsync(CommandBuilder b) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var cmd = sc.getShellDialect().launchAsnyc(b);
            TrackEvent.withDebug("Executing local application")
                    .tag("command", b.buildFull(sc))
                    .tag("adjusted", cmd.buildFull(sc))
                    .handle();
            try (var c = sc.command(cmd).start()) {
                c.discardOrThrow();
            }
        }
    }
}
