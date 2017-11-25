package org.duangsuse.dex;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    final int ACTIVITY_CHOOSE_FILE = 1;

    ConsoleOutputCapturer cout_cap;
    TextView background_text;
    DxCall mDxCall;

    private static View createStatusView(Activity activity, int color) {
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = activity.getResources().getDimensionPixelSize(resourceId);

        View statusView = new View(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                statusBarHeight);
        statusView.setLayoutParams(params);
        statusView.setBackgroundColor(color);
        return statusView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        cout_cap = new ConsoleOutputCapturer();
        cout_cap.start(background_text);
        mDxCall = new DxCall(this);
        String[] args = {"--help"};
        mDxCall.init();
        mDxCall.exec(args);
        background_text = findViewById(R.id.tv);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        View statusView = createStatusView(this, getTitleColor());
        ViewGroup decorView = (ViewGroup) this.getWindow().getDecorView();
        decorView.addView(statusView);
        FrameLayout rootView = this.findViewById(android.R.id.content);
        ViewGroup rooView = (ViewGroup) rootView.getChildAt(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            rooView.setFitsSystemWindows(true);
        }
        rooView.setClipToPadding(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setIcon(new ColorDrawable(Color.argb(0, 0, 0, 0)));
            getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Compile");
        menu.add("Run Command");
        menu.add("Fresh output");
        menu.add("Exit");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        char c0 = item.getTitle().charAt(0);
        switch (c0) {
            case 'C':
                Intent chooseFile;
                Intent intent;
                chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("file/*");
                intent = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
                break;
            case 'R':
                final EditText input = new EditText(this);
                input.setHint("Command to pass");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    new AlertDialog.Builder(this)
                            .setView(input)
                            .setOnDismissListener(i -> {
                                String[] args = input.getText().toString().split(" ");
                                mDxCall.exec(args);
                            }).show();
                }
                break;
            case 'F':
                updateText();
                break;
            case 'E':
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_CHOOSE_FILE: {
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    String filePath = "(ERROR)";
                    if (uri != null) {
                        filePath = uri.getPath();
                    }
                    Toast.makeText(this, filePath, Toast.LENGTH_LONG).show();
                    String[] args = {"--verbose", "--dex", "--output=" + filePath + ".dex", filePath};
                    mDxCall.exec(args);
                }
            }
        }
    }

    void updateText() {
        background_text.setText(cout_cap.stop());
        cout_cap.start(background_text);
    }
}


class ConsoleOutputCapturer {
    private ByteArrayOutputStream mByteArrayOutputStream;
    private PrintStream previous;
    private boolean capturing;
    private TextView mTextView;

    public void start(TextView tv) {
        if (capturing) {
            return;
        }
        this.mTextView = tv;
        capturing = true;
        previous = System.err;
        mByteArrayOutputStream = new ByteArrayOutputStream();

        OutputStream outputStreamCombiner =
                new OutputStreamCombiner(Arrays.asList(previous, mByteArrayOutputStream));
        PrintStream custom = new PrintStream(outputStreamCombiner);

        System.setErr(custom);
        System.setOut(custom);
    }

    String stop() {
        if (!capturing) {
            return "";
        }

        System.setOut(previous);
        System.setErr(previous);

        String capturedValue = mByteArrayOutputStream.toString();

        mByteArrayOutputStream = null;
        previous = null;
        capturing = false;

        return capturedValue;
    }

    private class OutputStreamCombiner extends OutputStream {
        private List<OutputStream> outputStreams;

        OutputStreamCombiner(List<OutputStream> outputStreams) {
            this.outputStreams = outputStreams;
        }

        public void write(int b) throws IOException {
            for (OutputStream os : outputStreams) {
                //0x0A: ASCII Line Feed
                if (b == 0x0A)
                    mTextView.setText(mByteArrayOutputStream.toString());
                os.write(b);
            }
        }

        public void flush() throws IOException {
            for (OutputStream os : outputStreams) {
                mTextView.setText(mByteArrayOutputStream.toString());
                os.flush();
            }
        }

        public void close() throws IOException {
            for (OutputStream os : outputStreams) {
                mTextView.setText(mByteArrayOutputStream.toString());
                os.close();
            }
        }
    }
}
