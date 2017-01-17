package vipo.haozi.orctester;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import vipo.haozi.orclib.ui.PhoneScanBaseActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onTestClick(View view){
        Intent intent = new Intent(this,PhoneScanBaseActivity.class);
        startActivity(intent);
    }
}
