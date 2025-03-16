package io.xpipe.core.util;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ModuleHelper {

    public static boolean isImage() {
        return ModuleHelper.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getProtocol()
                .equals("jrt");
    }

    @SneakyThrows
    public static Module getEveryoneModule() {
        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Field[] fields = (Field[]) getDeclaredFields0.invoke(Module.class, false);
        Field modifiers = null;
        for (Field each : fields) {
            if ("EVERYONE_MODULE".equals(each.getName())) {
                modifiers = each;
                break;
            }
        }
        modifiers.setAccessible(true);
        return (Module) modifiers.get(null);
    }

    @SneakyThrows
    public static void exportAndOpen(String pkg, Module mod) {
        if (mod.isExported(pkg) && mod.isOpen(pkg)) {
            return;
        }

        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredMethods0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Method[] fields = (Method[]) getDeclaredFields0.invoke(Module.class, false);
        Method modifiers = null;
        for (Method each : fields) {
            if ("implAddExportsOrOpens".equals(each.getName())) {
                modifiers = each;
                break;
            }
        }
        modifiers.setAccessible(true);

        var e = getEveryoneModule();
        modifiers.invoke(mod, pkg, e, false, true);
        modifiers.invoke(mod, pkg, e, true, true);
    }
}
