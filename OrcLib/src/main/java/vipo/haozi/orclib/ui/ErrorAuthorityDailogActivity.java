package vipo.haozi.orclib.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import vipo.haozi.orclib.BuildConfig;
import vipo.haozi.orclib.R;

/**
 * Created by Android Studio.
 * User:  jf.yin
 * Date: 2016/8/5
 * Time: 11:54
 */
public class ErrorAuthorityDailogActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_errorauthority_dialog);
        findViewById(R.id.button_setting).setOnClickListener(this);
    }

    @Override
    public void finish() {}

    public void realFinish(){
        super.finish();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_setting){
            String phoneType = Build.BRAND;
            String manufacturer = Build.MANUFACTURER;
            if(phoneType.startsWith("Xiaomi") || manufacturer.equalsIgnoreCase("Xiaomi")){
                gotoMiuiPermission();
            }else if(phoneType.startsWith("Meizu") || manufacturer.equalsIgnoreCase("Meizu")){
                gotoMeizuPermission();
            }else if(phoneType.startsWith("Huawei") || manufacturer.equalsIgnoreCase("HUAWEI")){
                gotoHuaweiPermission();
            //}else if(phoneType.startsWith("OPPO")){//暂时不知道权限接口
            //}else if(phoneType.startsWith("VIVO")){//暂时不知道权限接口
            }else{
                gotoSettingPage();
            }
            realFinish();
        }
    }

    /**
     * 跳转到miui的权限管理页面
     */
    private void gotoMiuiPermission() {
        Intent i = new Intent("miui.intent.action.APP_PERM_EDITOR");
        ComponentName componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
        i.setComponent(componentName);
        i.putExtra("extra_pkgname", getPackageName());
        try {
            startActivity(i);
        } catch (Exception e) {
            e.printStackTrace();
            gotoSettingPage();
        }
    }

    /**
     * 跳转到魅族的权限管理系统
     */
    private void gotoMeizuPermission() {
        Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            gotoSettingPage();
        }
    }

    /**
     * 华为的权限管理页面
     */
    private void gotoHuaweiPermission() {
        try {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName comp = new ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity");//华为权限管理
            intent.setComponent(comp);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            gotoSettingPage();
        }

    }

    private void gotoSettingPage(){
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 9) {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
        }
        startActivity(intent);
    }
}
