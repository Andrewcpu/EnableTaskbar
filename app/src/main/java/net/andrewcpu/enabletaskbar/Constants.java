package net.andrewcpu.enabletaskbar;

import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class Constants {

    public static int taskbarStashedWidth = 413;
    public static int taskbarStashedHeight = 75;
    public static final float CORNER_RADIUS_MULTIPLIER = 0.9f;
    public static final double TASKBAR_ICON_SIZE_MULTIPLIER = 1.25;
    public static final boolean DISABLE_DEFAULT_GESTURE_PILL = true;
    private static Object stashedTaskbarViewController = null;
    private static Object taskbarController = null;

    public static final XC_MethodHook DISABLE_GESTURE_PILL = new XC_MethodHook() {
        @SuppressLint("DefaultLocale")
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            View result = (View) param.thisObject;
            ViewGroup.LayoutParams resultLayoutParams = result.getLayoutParams();
            int originalWidth, originalHeight;
            try {
                originalWidth = (int) getAdditionalInstanceField(param.thisObject, "originalWidth");
                originalHeight = (int) getAdditionalInstanceField(param.thisObject, "originalHeight");
            } catch (Throwable ignored) {
                originalWidth = resultLayoutParams.width;
                originalHeight = resultLayoutParams.height;
                setAdditionalInstanceField(param.thisObject, "originalWidth", originalWidth);
                setAdditionalInstanceField(param.thisObject, "originalHeight", originalHeight);
            }
            if (originalWidth != -1) {
                XposedBridge.log("Default Gesture Pill Width: " + originalWidth);
                taskbarStashedWidth = originalWidth;
                taskbarStashedHeight = originalHeight;
                if(stashedTaskbarViewController != null){
                    XposedBridge.log("Attempting to reinitialize the stashed taskbar controller");
                    XposedHelpers.callMethod(stashedTaskbarViewController, "init", XposedHelpers.getObjectField(stashedTaskbarViewController, "mControllers"));
                }
                XposedBridge.log(String.format("Updating taskbar sizes to (%d, %d)", originalWidth, originalHeight));

            }
            resultLayoutParams.width = Math.round(originalWidth * 0.0f); // Disable default gesture pill by setting size to 0
        }
    };

    public static final XC_MethodHook CORNER_RADIUS_HOOK = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            param.setResult(Math.round((int)(param.getResult()) * (CORNER_RADIUS_MULTIPLIER))); // Corner radius of the taskbar background.. 1.0f is too big for Pixel 7 Pro.. Unsure of how to get this dynamically.
        }
    };

    public static final XC_MethodHook HANDLE_STASHED_INIT = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            stashedTaskbarViewController = param.thisObject;
            XposedBridge.log("Initializing taskbar handle with width of " + taskbarStashedWidth);
            setIntField(param.thisObject, "mStashedHandleWidth", taskbarStashedWidth);
            if(taskbarController != null) {
                setIntField(taskbarController, "mStashedHeight", taskbarStashedHeight);
            }
        }
    };

    public static final XC_MethodHook DEVICE_PROFILE_UPDATE_SIZE = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            setObjectField(param.thisObject, "taskbarSize", (int)(Math.round(getIntField(param.thisObject, "iconSizePx") * TASKBAR_ICON_SIZE_MULTIPLIER)));
            setBooleanField(param.thisObject, "isTaskbarPresent", true);
            setBooleanField(param.thisObject, "isTaskbarPresentInApps", true);
        }
    };

    public static final XC_MethodHook GET_DEVICE_PROFILE_PADDING = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            int iconSizePx = (int)(Math.round(getIntField(param.thisObject, "iconSizePx") * TASKBAR_ICON_SIZE_MULTIPLIER));
            Rect cellLayout = (Rect) XposedHelpers.getObjectField(param.thisObject, "cellLayoutPaddingPx");
            Rect mInsets = (Rect) XposedHelpers.getObjectField(param.thisObject, "mInsets");
            float ICON_OVERLAP_FACTOR = 1.0f + (0.25F / 2F);
            float diffOverlapFactor = iconSizePx * (ICON_OVERLAP_FACTOR - 1) / 2;

            int topPadding = Math.max((int)(mInsets.top + cellLayout.top - diffOverlapFactor), 0);
            int bottomPadding = Math.max((int)(mInsets.bottom + cellLayout.bottom - diffOverlapFactor), 0);

            Rect rect = new Rect(8, topPadding + (int)(taskbarStashedHeight * 1.5), 8, bottomPadding); // I have no idea why 1.5 works. Need to look into this.
            param.setResult(rect);                                                                                   // When this value was 1.0, the animation from in application w/ taskbar open to home dock taskbar would jump after it was complete. Upping it to 1.5 fixed it, probably DPI related.
        }
    };

    public static final XC_MethodHook UPDATE_STASHED_HEIGHT = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            taskbarController = param.thisObject;
            setIntField(param.thisObject, "mStashedHeight", taskbarStashedHeight);
        }
    };

    public static final XC_MethodHook GET_STASHED_HEIGHT = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            taskbarController = param.thisObject;
            param.setResult(taskbarStashedHeight);
        }
    };

    public static final XC_MethodHook DISABLE_LAUNCHER_SCALING = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            XposedHelpers.setBooleanField(param.thisObject, "isScalable", false);
            XposedBridge.log("Disabling scalability of element.");
        }
    };
}
