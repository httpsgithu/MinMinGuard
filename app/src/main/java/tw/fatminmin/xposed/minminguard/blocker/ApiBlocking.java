package tw.fatminmin.xposed.minminguard.blocker;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tw.fatminmin.xposed.minminguard.Main;

import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Created by fatminmin on 2015/10/27.
 */
public class ApiBlocking {
    public static void handle(final String packageName, final XC_LoadPackage.LoadPackageParam lpparam, final boolean removeAd) {

        Class<?> activity = XposedHelpers.findClass("android.app.Application", lpparam.classLoader);
        XposedBridge.hookAllMethods(activity, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context)(param.thisObject);
                for (Blocker blocker : Main.blockers) {
                    String name = blocker.getClass().getSimpleName();
                    Boolean result = blocker.handleLoadPackage(packageName, lpparam, removeAd);
                    if(result) {
                        Util.notifyAdNetwork(context, packageName, name);
                    }
                }
            }
        });
    }
    /*
        Helper function used for removing banners
     */
    public static boolean removeBanner(final String packageName, final String banner, final String bannerLoadFunc, final XC_LoadPackage.LoadPackageParam lpparam, final boolean removeAd) {
        try {
            Class<?> bannerClazz = findClass(banner, lpparam.classLoader);
            XposedBridge.hookAllMethods(bannerClazz, bannerLoadFunc, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                    String debugMsg = String.format("Detect %s %s in %s", banner, bannerLoadFunc, packageName);
                    Util.log(packageName, debugMsg);
                    if (removeAd) {
                        param.setResult(new Object());
                    }
                }
            });
        }
        catch(XposedHelpers.ClassNotFoundError e) {
            return false;
        }
        return true;
    }
}
