package geyerk.sensorlab.suselogger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import timber.log.Timber;

public class QRScanner extends Activity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZXingScannerView(this);
        mScannerView.setAutoFocus(true);
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        Intent returnIntent = new Intent();
        final String result = rawResult.getText();
        returnIntent.putExtra("result", result);
        if(assessIfKsonFormat(result)){
            setResult(0, returnIntent);
        } else{
            setResult(1, returnIntent);
        }
        finish();
    }

    boolean assessIfKsonFormat(String data) {
        String[] dataRows = data.split("\n");
        if(dataRows.length == 5){
            if(dataRows[0].charAt(0) != '{'){
                Timber.i("not json as first character is not {");
                return false;
            }
            if(dataRows[1].charAt(0) != '{' || dataRows[1].charAt(1) != 'C'){
                Timber.i("not json as first character is not { and second is not C");
                return false;
            }
            if(dataRows[2].charAt(0) != '{' || dataRows[2].charAt(1) != 'U'){
                Timber.i("not json as first character is not { and second is not U");
                return false;
            }
            if(dataRows[3].charAt(0) != '{' || dataRows[3].charAt(1) != 'P'){
                Timber.i("not json as first character is not { and second is not P");
                return false;
            }
            Timber.i("last character assessment");
            return dataRows[4].charAt(0) == '}';
        }
        Timber.i("not json as instructions are not 5 elements in length");
        return false;
    }
}
