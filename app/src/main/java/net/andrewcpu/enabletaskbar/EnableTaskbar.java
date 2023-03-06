package net.andrewcpu.enabletaskbar;

import static net.andrewcpu.enabletaskbar.Constants.GET_STASHED_HEIGHT;
import static net.andrewcpu.enabletaskbar.Constants.TASKBAR_ICON_SIZE_MULTIPLIER;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class EnableTaskbar implements IXposedHookLoadPackage {

    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher";

    //Disable Gesture Handle
    private static final String DEFAULT_GESTURE_BAR_HANDLE = "com.android.systemui.navigationbar.gestural.NavigationHandle";


    //Taskbar Modifications
    private static final String INVARIANT_GRIDOPTION = "com.android.launcher3.InvariantDeviceProfile$GridOption";
    private static final String DEVICE_PROFILE = "com.android.launcher3.DeviceProfile";
    private static final String TASKBAR_STASH_CONTROLLER = "com.android.launcher3.taskbar.TaskbarStashController";
    private static final String STASHED_HANDLE_VIEW_CONTROLLER = "com.android.launcher3.taskbar.StashedHandleViewController";
    private static final String TASKBAR_ACTIVITY_CONTEXT = "com.android.launcher3.taskbar.TaskbarActivityContext";

    private void initSystemUI(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Exception {
        Class<?> NavigationHandleClass = findClass(DEFAULT_GESTURE_BAR_HANDLE, loadPackageParam.classLoader);
        if(Constants.DISABLE_DEFAULT_GESTURE_PILL){
            hookAllMethods(NavigationHandleClass, "setVertical", Constants.DISABLE_GESTURE_PILL);
        }
    }

    private void initPixelLauncher(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Class<?> gridOption = findClass(INVARIANT_GRIDOPTION, loadPackageParam.classLoader);
            Class<?> deviceProfile = findClass(DEVICE_PROFILE, loadPackageParam.classLoader);
            Class<?> stashController = findClass(TASKBAR_STASH_CONTROLLER, loadPackageParam.classLoader);
            Class<?> stashedHandle = findClass(STASHED_HANDLE_VIEW_CONTROLLER, loadPackageParam.classLoader);
            Class<?> taskbarActivityContext = findClass(TASKBAR_ACTIVITY_CONTEXT, loadPackageParam.classLoader);

            // Set taskbar corner radius
            hookAllMethods(taskbarActivityContext, "getLeftCornerRadius", Constants.CORNER_RADIUS_HOOK);
            hookAllMethods(taskbarActivityContext, "getRightCornerRadius", Constants.CORNER_RADIUS_HOOK);

            // Set stashed taskbar dimensions
            hookAllMethods(stashedHandle, "init", Constants.HANDLE_STASHED_INIT);

            // Set taskbar dimensions within DeviceProfile
            hookAllConstructors(deviceProfile, Constants.DEVICE_PROFILE_UPDATE_SIZE);

            // Set the taskbar "hotseat" (Pixel Launcher integration of bottom row) padding. This functionality is partially extracted from DeviceProfile#getHotseatLayoutPadding.
            hookAllMethods(deviceProfile, "getHotseatLayoutPadding", Constants.GET_DEVICE_PROFILE_PADDING);
            // Update the stored stashed height of the taskbar in the stashed controller
            hookAllConstructors(stashController, Constants.UPDATE_STASHED_HEIGHT);

            // Override the stash controllers getStashedHeight function to return out forced stashed height
            hookAllMethods(stashController, "getStashedHeight", Constants.GET_STASHED_HEIGHT);

            // Disables pixel launcher grid scaling
            hookAllConstructors(gridOption, Constants.DISABLE_LAUNCHER_SCALING);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try{
            if (lpparam.packageName.equals(SYSTEM_UI_PACKAGE)) {
                initSystemUI(lpparam);
            }
            else if (lpparam.packageName.equals(LAUNCHER_PACKAGE)) {
                initPixelLauncher(lpparam);
            }
        }catch (Exception ex) {
            XposedBridge.log(ex);
            throw ex;
        }
    }
}
