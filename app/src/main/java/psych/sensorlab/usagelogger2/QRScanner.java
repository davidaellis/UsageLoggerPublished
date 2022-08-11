package psych.sensorlab.usagelogger2;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import timber.log.Timber;

public class QRScanner extends AppCompatActivity {
    /* access modifiers changed from: private */
    public CodeScanner mCodeScanner;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.scan_qr);
        final Activity activity = getActivity(this);
        CodeScannerView codeScannerView = findViewById(R.id.scanner_view);
        this.mCodeScanner = new CodeScanner(this, codeScannerView);
        this.mCodeScanner.setDecodeCallback(result -> activity.runOnUiThread(() -> {
            QRScanner.this.handleResult(result.getText());
            Timber.i("run message qr: %s", result.getText());
        }));
        codeScannerView.setOnClickListener(view -> QRScanner.this.mCodeScanner.startPreview());
    }

    public Activity getActivity(Context context) {
        if (!(context instanceof ContextWrapper)) {
            return null;
        }
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return getActivity(((ContextWrapper) context).getBaseContext());
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        this.mCodeScanner.startPreview();
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        this.mCodeScanner.releaseResources();
        super.onPause();
    }

    /* access modifiers changed from: package-private */
    public boolean assessIfKsonFormat(String str) {
        String[] split = str.split("\n");
        if (split.length != 5) {
            Timber.i("not json as instructions are not 5 elements in length");
            return false;
        } else if (split[0].charAt(0) != '{') {
            Timber.i("not json as first character is not {");
            return false;
        } else if (split[1].charAt(0) != '{' || split[1].charAt(1) != 'C') {
            Timber.i("not json as first character is not { and second is not C");
            return false;
        } else if (split[2].charAt(0) != '{' || split[2].charAt(1) != 'U') {
            Timber.i("not json as first character is not { and second is not U");
            return false;
        } else if (split[3].charAt(0) == '{' && split[3].charAt(1) == 'P') {
            Timber.i("last character assessment");
            return split[4].charAt(0) == '}';
        } else {
            Timber.i("not json as first character is not { and second is not P");
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void handleResult(String str) {
        Intent intent = new Intent();
        intent.putExtra("result", str);
        if (assessIfKsonFormat(str)) {
            setResult(0, intent);
        } else {
            setResult(1, intent);
        }
        finish();
    }
}
