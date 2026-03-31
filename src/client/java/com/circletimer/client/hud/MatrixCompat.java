package com.circletimer.client.hud;

import net.minecraft.client.gui.DrawContext;

import java.lang.reflect.Method;

final class MatrixCompat {
    private MatrixCompat() {
    }

    static Object getStack(DrawContext context) {
        Object stack = invokeNoArg(context, "getMatrices");
        if (stack != null) {
            return stack;
        }
        stack = invokeNoArg(context, "method_51448");
        if (stack != null) {
            return stack;
        }

        for (Method method : context.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            Class<?> type = method.getReturnType();
            String name = type.getSimpleName();
            if (!name.contains("Matrix")) {
                continue;
            }
            try {
                return method.invoke(context);
            } catch (ReflectiveOperationException ignored) {
                // Keep scanning.
            }
        }
        return null;
    }

    static void push(Object stack) {
        if (stack == null) {
            return;
        }
        if (invokeNoArg(stack, "pushMatrix") != null) {
            return;
        }
        invokeNoArg(stack, "push");
    }

    static void pop(Object stack) {
        if (stack == null) {
            return;
        }
        if (invokeNoArg(stack, "popMatrix") != null) {
            return;
        }
        invokeNoArg(stack, "pop");
    }

    static void scale(Object stack, float x, float y) {
        if (stack == null) {
            return;
        }
        if (invokeScale(stack, x, y)) {
            return;
        }
        invokeScale(stack, x, y, 1.0f);
    }

    private static boolean invokeScale(Object target, float... values) {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals("scale")) {
                continue;
            }
            if (method.getParameterCount() != values.length) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            boolean compatible = true;
            for (Class<?> type : types) {
                if (type != float.class && type != Float.class) {
                    compatible = false;
                    break;
                }
            }
            if (!compatible) {
                continue;
            }
            try {
                Object[] args = new Object[values.length];
                for (int i = 0; i < values.length; i++) {
                    args[i] = values[i];
                }
                method.invoke(target, args);
                return true;
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }
        return false;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
