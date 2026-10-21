package com.termuxbackground;

/** Pure validation boundary: no frontend value can choose an executable or shell fragment. */
final class Operation {
    static final String EXECUTABLE = "/data/data/com.termux/files/usr/bin/termux-background";
    static final long TIMEOUT_MS = 60000;
    static String[] arguments(String operation, String opacity, boolean blur, String animation) {
        if (operation == null) throw new IllegalArgumentException("Unsupported operation");
        switch (operation) {
            case "APPLY":
                if (opacity == null || !opacity.matches("(?:0(?:\\.[0-9]{1,3})?|1(?:\\.0{1,3})?)"))
                    throw new IllegalArgumentException("Opacity must be 0 to 1 (up to 3 decimal places)");
                if (!"none".equals(animation) && !"scroll".equals(animation))
                    throw new IllegalArgumentException("Invalid animation");
                return new String[]{"set", "--image-stdin", "--opacity", opacity,
                        "--blur", Boolean.toString(blur), "--animation", animation};
            case "RESET": return new String[]{"reset"};
            case "STATUS": return new String[]{"status"};
            case "PROBE": return new String[]{"probe"};
            case "RELOAD": return new String[]{"reload"};
            default: throw new IllegalArgumentException("Unsupported operation");
        }
    }
    static String[] monetArguments(String operation, String opacity, boolean blur, String animation) {
        String[] validated = arguments(operation, opacity, blur, animation);
        if ("APPLY".equals(operation) && (blur || !"none".equals(animation)))
            throw new IllegalArgumentException("Monet does not support blur or scroll animation");
        String[] args = new String[validated.length + 2];
        args[0] = "--backend"; args[1] = "monet";
        System.arraycopy(validated, 0, args, 2, validated.length);
        return args;
    }
    static void validateValues(Object opacity, Object blur, Object animation) {
        if (!(opacity instanceof String) || !(blur instanceof Boolean) || !(animation instanceof String))
            throw new IllegalArgumentException("Invalid apply parameter types");
        arguments("APPLY", (String) opacity, (Boolean) blur, (String) animation);
    }
    static void validateImage(int width, int height, String mime) {
        if (width <= 0 || height <= 0 || (long) width * height > 40000000L
                || !("image/png".equals(mime) || "image/jpeg".equals(mime)))
            throw new IllegalArgumentException("Choose a valid PNG or JPEG of at most 40 megapixels");
    }
    static String resultCode(int internal, int exit) {
        if (internal == 150 || internal == 151) return "CLI_MISSING";
        if (internal != -1) return "EXECUTION_ERROR";
        switch (exit) {
            case 0: return "COMPLETED";
            case 2: return "CLI_VALIDATION_FAILED";
            case 3: return "RELOAD_UNAVAILABLE";
            case 4: return "IMAGE_TRANSFER_FAILED";
            case 5: return "CONFIG_WRITE_FAILED";
            case 8: return "DEPENDENCY_MISSING";
            case 7: return "CLEANUP_FAILED";
            case 6: return "RELOAD_FAILED";
            case 127: return "CLI_MISSING";
            default: return "UNEXPECTED_FAILURE";
        }
    }
    static boolean protocolSupported(String stdout) {
        return stdout != null && stdout.startsWith("TBG_PROTOCOL=1\n");
    }
    static boolean timedOut(long elapsed) { return elapsed >= TIMEOUT_MS; }
}
