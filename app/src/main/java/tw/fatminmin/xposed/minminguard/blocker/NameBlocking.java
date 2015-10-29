package tw.fatminmin.xposed.minminguard.blocker;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tw.fatminmin.xposed.minminguard.Main;

/**
 * Created by fatminmin on 2015/10/27.
 */
public class NameBlocking {
    private static boolean matchBannerName(String clazzName, String banner, String bannerPrefix) {
        if(banner != null && banner.equals(clazzName)) {
            return true;
        }
        if(bannerPrefix != null && clazzName.startsWith(bannerPrefix)) {
            return true;
        }
        return false;
    }

    // return adnetwork name
    private static Boolean isAdView(Context context, String pkgName, String clazzName) {

        // android widgets
        if(clazzName.startsWith("android")) {
            return false;
        }

        // corner case
        if(clazzName.startsWith("com.google.ads")) {
            return true;
        }
        for (Blocker blocker : Main.blockers) {
            String name = blocker.getClass().getSimpleName();
            String banner = blocker.getBanner();
            String bannerPrefix = blocker.getBannerPrefix();

            // prefix is used to detect adview obfuscate by proguard
            if(matchBannerName(clazzName, banner, bannerPrefix))
            {
                Util.notifyAdNetwork(context, pkgName, name);
                return true;
            }
        }
        return null;

    }

    private static Boolean isAdView(Context context, String pkgName, View view)
    {
        Class clazz = view.getClass();
        // find also parent classes
        int level = 1;
        for (int i = 0; i < level && clazz != null; i++) {
            String clazzName = clazz.getName();

            if (isAdView(context, pkgName, clazzName)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }

//        if (view instanceof ViewGroup) {
//            ViewGroup vg = (ViewGroup) view;
//            for(int i = 0; i < vg.getChildCount(); i++) {
//                clearAdViewInLayout(pkgName, vg.getChildAt(i));
//            }
//        }

        return null;
    }

    public static void clearAdViewInLayout(final String packageName, final View view) {

        if(isAdView(view.getContext(), packageName, view)) {
            Main.removeAdView(view, packageName, true);
            Util.log(packageName, "clearAdViewInLayout: " + view.getClass().getName());
        }

        if(view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for(int i = 0; i < vg.getChildCount(); i++) {
                clearAdViewInLayout(packageName, vg.getChildAt(i));
            }
        }
    }

    public static void nameBasedBlocking(final String packageName, final XC_LoadPackage.LoadPackageParam lpparam) {

        Class<?> viewGroup = XposedHelpers.findClass("android.view.ViewGroup", lpparam.classLoader);
        XposedBridge.hookAllMethods(viewGroup, "addView", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View v = (View) param.args[0];

                if (isAdView(v.getContext(), packageName, v)) {
                    Main.removeAdView(v, packageName, true);
                    Util.log(packageName, "Name based blocking: " + v.getClass().getName());
                }

            }
        });
        Class<?> activity = XposedHelpers.findClass("android.app.Activity", lpparam.classLoader);
        XposedBridge.hookAllMethods(activity, "setContentView", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity ac = (Activity)(param.thisObject);
                ViewGroup root = (ViewGroup) ac.getWindow().getDecorView().findViewById(android.R.id.content);
                NameBlocking.clearAdViewInLayout(packageName, root);
            }
        });
    }
}