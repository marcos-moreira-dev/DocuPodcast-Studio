package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

final class WindowsNativeWindowHandleResolver {
    private WindowsNativeWindowHandleResolver() {
    }

    static OptionalLong resolveHandle(Node node) {
        Optional<HWND> hwnd = resolveHwnd(node);
        if (hwnd.isEmpty()) {
            return OptionalLong.empty();
        }
        long value = Pointer.nativeValue(hwnd.get().getPointer());
        return value == 0L ? OptionalLong.empty() : OptionalLong.of(value);
    }

    static Optional<HWND> resolveHwnd(Node node) {
        if (node == null || node.getScene() == null || node.getScene().getWindow() == null) {
            return Optional.empty();
        }
        Window window = node.getScene().getWindow();
        long currentPid = ProcessHandle.current().pid();
        String title = window instanceof Stage stage ? stage.getTitle() : "";
        WindowBounds windowBounds = new WindowBounds(
                window.getX(),
                window.getY(),
                window.getWidth(),
                window.getHeight());
        List<HWND> candidates = new ArrayList<>();
        User32WindowLookupApi.INSTANCE.EnumWindows((hWnd, data) -> {
            if (!User32WindowLookupApi.INSTANCE.IsWindowVisible(hWnd)) {
                return true;
            }
            IntByReference pid = new IntByReference();
            User32WindowLookupApi.INSTANCE.GetWindowThreadProcessId(hWnd, pid);
            if (pid.getValue() != (int) currentPid) {
                return true;
            }
            if (!title.isBlank() && windowTitle(hWnd).contains(title)) {
                candidates.add(hWnd);
                return false;
            }
            if (roughlySameBounds(hWnd, windowBounds)) {
                candidates.add(hWnd);
            }
            return true;
        }, null);
        return candidates.stream().findFirst();
    }

    private static String windowTitle(HWND hWnd) {
        char[] buffer = new char[512];
        int length = User32WindowLookupApi.INSTANCE.GetWindowTextW(hWnd, buffer, buffer.length);
        return length <= 0 ? "" : Native.toString(buffer);
    }

    private static boolean roughlySameBounds(HWND hWnd, WindowBounds bounds) {
        RECT rect = new RECT();
        if (!User32WindowLookupApi.INSTANCE.GetWindowRect(hWnd, rect)) {
            return false;
        }
        double width = rect.right - rect.left;
        double height = rect.bottom - rect.top;
        return Math.abs(rect.left - bounds.x()) < 24
                && Math.abs(rect.top - bounds.y()) < 48
                && Math.abs(width - bounds.width()) < 64
                && Math.abs(height - bounds.height()) < 64;
    }

    private interface User32WindowLookupApi extends StdCallLibrary {
        User32WindowLookupApi INSTANCE = Native.load("user32", User32WindowLookupApi.class,
                W32APIOptions.DEFAULT_OPTIONS);

        boolean EnumWindows(com.sun.jna.platform.win32.WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);

        int GetWindowThreadProcessId(HWND hWnd, IntByReference processId);

        boolean IsWindowVisible(HWND hWnd);

        int GetWindowTextW(HWND hWnd, char[] lpString, int nMaxCount);

        boolean GetWindowRect(HWND hWnd, RECT rect);
    }

    private record WindowBounds(double x, double y, double width, double height) {
    }
}
