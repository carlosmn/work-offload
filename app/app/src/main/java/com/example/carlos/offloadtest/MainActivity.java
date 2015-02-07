package com.example.carlos.offloadtest;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.junit.internal.Classes;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;


public class MainActivity extends ActionBarActivity {

    private class RunBenchmarkTask extends AsyncTask<Class, Void, Result> {
        long memoryAtStart;

        RunBenchmarkTask(long memoryAtStart) {
            super();
            this.memoryAtStart = memoryAtStart;
        }

        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected Result doInBackground(Class... classes) {
            JUnitCore junit = new JUnitCore();
            return junit.run(classes);
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(Result res) {
            TextView text = (TextView) findViewById(R.id.textView3);
            Runtime rt = Runtime.getRuntime();
            long memoryNow = rt.totalMemory() - rt.freeMemory();
            double memoryUsage = (memoryNow - this.memoryAtStart) / (1024.0 * 1024);
            text.setText(String.format("Ran %dms, %d tests %d fail, used %fMB",
                    res.getRunTime(), res.getRunCount(), res.getFailureCount(), memoryUsage));
            Button button = (Button) findViewById(R.id.button);
            button.setEnabled(true);
        }
    }

    void setMiltipliedValue(int value) {
        TextView view = (TextView) findViewById(R.id.textView2);
        view.setText(String.format("x5 nodes = %d", value * 5));
        State.nodeCreationRounds = value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NumberPicker picker = (NumberPicker) findViewById(R.id.numberSelection);
        picker.setMaxValue(100);
        picker.setMinValue(1);
        picker.setValue(5);

        this.setMiltipliedValue(5);

        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setMiltipliedValue(newVal);
            }
        });

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                Runtime rt = Runtime.getRuntime();
                rt.gc();
                long memoryNow = rt.totalMemory() - rt.freeMemory();
                new RunBenchmarkTask(memoryNow).execute(LargeBenchmark.class);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
