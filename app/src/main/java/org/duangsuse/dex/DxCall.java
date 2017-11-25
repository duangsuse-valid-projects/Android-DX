package org.duangsuse.dex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
//optional (for embedded DX)
import com.android.dx.command.Main;

import dalvik.system.DexClassLoader;

class DxCall {
    private static final String dexFileName = "dx.dex";
    private static final String targetClass = "com.android.dx.command.Main";
    private final String mDexPath;
    private final String mDexCachePath;
    private final MainActivity mActivity;
    private Class<?> Main;

    DxCall(MainActivity activity) {
        mActivity = activity;
        mDexPath = activity.getFilesDir().getPath() + "/" + dexFileName;
        mDexCachePath = activity.getCacheDir().getPath();
    }

    void init() {
        //remove if DX is loaded from Dex
        Main.version();
        File dexFile = new File(mDexPath);
        if (dexFile.isFile())
            loadDex();
        else {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mDexPath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                is = mActivity.getAssets().open(dexFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int i = 0;
            try {
                assert is != null;
                assert fos != null;
                while ((i = is.read()) != -1) {
                    fos.write(i);
                }
                is.close();
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadDex() {
        try {
            Main = Class.forName(targetClass);
        } catch (ClassNotFoundException e) {
            try {
                DexClassLoader loader = new DexClassLoader(mDexPath, mDexCachePath, null, MainActivity.class.getClassLoader());
                Main = loader.loadClass(targetClass);
            } catch (Exception ee) {
                e.printStackTrace();
            }
        }
    }

    void exec(final String[] args) {
        try {
            Runnable dx = () -> {
                Class[] argTypes = new Class[]{String[].class};
                Method main = null;
                try {
                    main = Main.getDeclaredMethod("main", argTypes);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                StringBuilder args_str = new StringBuilder();
                for (String p : args)
                    args_str.append(p).append(" ");
                assert main != null;
                System.err.format("invoking %s.%s : %s%n", Main.getName(), main.getName(), args_str.toString());
                try {
                    main.invoke(null, (Object) args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            new Thread(dx).start();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
