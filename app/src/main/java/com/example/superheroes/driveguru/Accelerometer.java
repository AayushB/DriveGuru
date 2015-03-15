package com.example.superheroes.driveguru;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.superheroes.driveguru.Processing.ProcessingManager;

import java.util.Arrays;
import java.util.List;

import io.relayr.RelayrSdk;
import io.relayr.ble.BleDevice;
import io.relayr.ble.BleDeviceMode;
import io.relayr.ble.BleDeviceType;
import io.relayr.ble.service.BaseService;
import io.relayr.ble.service.DirectConnectionService;
import io.relayr.model.AccelGyroscope;
import io.relayr.model.Reading;
import io.relayr.model.User;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

public class Accelerometer extends Activity {


    /**
     * Once the Activity has been started, the onCreate method will be called
     * @param savedInstanceState
     */

    private TextView mWelcomeDriverTextView;
    private TextView mWelcomeStrangerTextView;
    private TextView mTemperatureValueTextView;
    private TextView mTemperatureNameTextView;

    private Subscription mUserInfoSubscription = Subscriptions.empty();
    private Subscription mTemperatureDeviceSubscription = Subscriptions.empty();

    //private TextView mThermometerOutput;
    private TextView mThermometerError;

    private Subscription mScannerSubscription = Subscriptions.empty();
    private Subscription mDeviceSubscription = Subscriptions.empty();

    private boolean mStartedScanning;
    private BleDevice mDevice;

    private ProcessingManager mProcessingManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //we load the layout xml defined in app/src/main/res/layout
        View view = View.inflate(this, R.layout.activity_accelerometer_demo, null);

        mWelcomeDriverTextView = (TextView) view.findViewById(R.id.txt_welcome);
        mWelcomeStrangerTextView = (TextView) view.findViewById(R.id.txt_welcome2);
        mTemperatureValueTextView = (TextView) view.findViewById(R.id.txt_accelerometer_value);
        mTemperatureNameTextView = (TextView) view.findViewById(R.id.txt_accelerometer_name);

        mProcessingManager = new ProcessingManager();

        setContentView(view);

        //we use the relayr SDK to see if the user is logged in by
        //caling the isUserLoggedIn function
        if (RelayrSdk.isUserLoggedIn()) {
            updateUiForALoggedInUser();
        } else {
            updateUiForANonLoggedInUser();
            //logIn();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!RelayrSdk.isBleSupported()) {
            Toast.makeText(this, getString(R.string.bt_not_supported), Toast.LENGTH_SHORT).show();
        } else if (!RelayrSdk.isBleAvailable()) {
            RelayrSdk.promptUserToActivateBluetooth(this);
        } else if (!mStartedScanning && mDevice == null) {
            mStartedScanning = true;
            discoverAccelerometer();
        }
    }

    @Override
    protected void onDestroy() {
        unSubscribeToUpdates();
        disconnectBluetooth();

        super.onDestroy();
    }


    public void discoverAccelerometer() {
        // Search for WunderBar temp/humidity sensors and take first that is direct connection mode
        mTemperatureValueTextView.setText("Discovering...");
        mScannerSubscription = RelayrSdk.getRelayrBleSdk()
                .scan(Arrays.asList(BleDeviceType.WunderbarGYRO))
                .filter(new Func1<List<BleDevice>, Boolean>() {
                    @Override
                    public Boolean call(List<BleDevice> bleDevices) {
                        for (BleDevice device : bleDevices) {
                            System.out.println(device);
                            System.out.println("OUR MODE IS " + device.getMode());
                            if (device.getMode() == BleDeviceMode.DIRECT_CONNECTION) {
                                // We can stop scanning, since we've found a sensor
                                RelayrSdk.getRelayrBleSdk().stop();

                                mTemperatureValueTextView.setText("FOUND");
                                return true;
                            }
                        }

                        mStartedScanning = false;
                        return false;
                    }
                })
                .map(new Func1<List<BleDevice>, BleDevice>() {
                    @Override
                    public BleDevice call(List<BleDevice> bleDevices) {
                        for (BleDevice device : bleDevices) {
                            if (device.getMode() == BleDeviceMode.DIRECT_CONNECTION) return device;
                        }

                        mStartedScanning = false;
                        return null; // will never happen since it's been filtered out before
                    }
                })
                .take(1)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<BleDevice>() {
                    @Override
                    public void onCompleted() {
                        mStartedScanning = false;

                    }

                    @Override
                    public void onError(Throwable e) {
                        mStartedScanning = false;
                        mTemperatureValueTextView.setText("Error");
                        mThermometerError.setText(R.string.sensor_discovery_error);
                    }

                    @Override
                    public void onNext(BleDevice device) {
                        mTemperatureValueTextView.setText("-.-");
                        subscribeForAccelerometerUpdates(device);
                    }
                });
    }







    /**
     * When Android is ready to draw any menus it initiates the
     * "prepareOptionsMenu" event, this method is caled to handle that
     * event.
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        //remove any previous items from the menu
        menu.clear();

        //Check to see if the user is logged in
        if (RelayrSdk.isUserLoggedIn()) {

            //if the user is logged in, we ask Android to draw the menu
            //we defined earlier in the thermometer_demo_logged_in.xml
            //file
            getMenuInflater().inflate(R.menu.thermometer_demo_logged_in, menu);
            updateUiForALoggedInUser();
        } else {

            //otherwise we return the
            //thermometer_demo_not_logged_in.xml file
            getMenuInflater().inflate(R.menu.thermometer_demo_not_logged_in, menu);
            updateUiForANonLoggedInUser();
        }

        //we must return this, so that any other classes interested in
        //the prepare menu event can do something.
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * When a menu item is selected, we see which item was called and
     * decide what to do according to the item.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //if the user slected login
        if (item.getItemId() == R.id.action_log_in) {

            //we call the login method on the relayr SDK
            Toast.makeText(this, "Attempting to Login", Toast.LENGTH_SHORT).show();
            RelayrSdk.logIn(this)
                    //.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<User>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            //showToast(R.string.unsuccessfully_logged_in);
                            updateUiForANonLoggedInUser();
                        }

                        @Override
                        public void onNext(User user) {
                            //showToast(R.string.successfully_logged_in);
                            invalidateOptionsMenu();
                            updateUiForALoggedInUser();
                        }
                    });
            return true;
        } else if (item.getItemId() == R.id.action_log_out) {

            //otherwise we call the logout method defined later in this
            //class
            logOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the user logs out
     */
    private void logOut() {

        //call the logOut method on the reayr SDK
        RelayrSdk.logOut();

        //call the invalidateOptionsMenu this is defined in the
        //Activity class and is used to reset the menu option
        invalidateOptionsMenu();

        //use the Toast library to display a message to the user
        Toast.makeText(this, R.string.successfully_logged_out, Toast.LENGTH_SHORT).show();
    }

    /**
     * When a user successfuly logs in, the SuccessUserLogin event is
     * fired, and is handled here:
     */

    public void onSuccessUserLogIn() {

        //use the Toast library to display a message to the user
        Toast.makeText(this, R.string.successfully_logged_in, Toast.LENGTH_SHORT).show();
        //call the invalidateOptionsMenu, which is defined in the
        //Activity class and is used to reset the menu option
        invalidateOptionsMenu();
    }

    /**
     * if there is a problem logging in a user, the ErrorLogin evet
     * is initiated and handled here.
     */

    public void onErrorLogin(Throwable e) {
        //use the Toast library to display a message to the user
        Toast.makeText(this, R.string.unsuccessfully_logged_in, Toast.LENGTH_SHORT).show();
    }

    private void updateUiForANonLoggedInUser() {
        mTemperatureValueTextView.setVisibility(View.GONE);
        mTemperatureNameTextView.setVisibility(View.GONE);
        mWelcomeDriverTextView.setVisibility(View.GONE);
        mWelcomeStrangerTextView.setVisibility(View.VISIBLE);
        mWelcomeStrangerTextView.setText(R.string.hello_stranger);
    }

    private void updateUiForALoggedInUser() {
        mTemperatureValueTextView.setVisibility(View.VISIBLE);
        mTemperatureNameTextView.setVisibility(View.VISIBLE);
        mWelcomeDriverTextView.setVisibility(View.VISIBLE);
        mWelcomeStrangerTextView.setVisibility(View.GONE);
        mWelcomeDriverTextView.setText(R.string.hello_driver);
        mTemperatureNameTextView.setText("Reading From Accelerometer");
        //loadUserInfo();
    }


    private void subscribeForAccelerometerUpdates(final BleDevice device) {
        mDevice = device;
        mDeviceSubscription = device.connect()
                .flatMap(new Func1<BaseService, Observable<Reading>>() {
                    @Override
                    public Observable<Reading> call(BaseService baseService) {
                        return ((DirectConnectionService) baseService).getReadings();
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        device.disconnect();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Reading>() {
                    @Override
                    public void onCompleted() {
                        mStartedScanning = false;
                    }

                    @Override
                    public void onError(Throwable e) {
                        mStartedScanning = false;
                        mTemperatureValueTextView.setText("Error");
                        //mThermometerError.setText(R.string.sensor_reading_error);
                    }

                    @Override
                    public void onNext(Reading reading) {
                        System.out.println(" ");
                        if(reading.meaning.equals("acceleration"))
                        {
                            System.out.println("check");

                            //parse

                            String value = reading.value.toString();
                            value = value.replace("{", "");
                            value = value.replace("}", "");
                            value = value.replace("\"", "");
                            value = value.replace("=", "");
                            value = value.replace("x", "");
                            value = value.replace("y", "");
                            value = value.replace("z", "");
                            String[] str_arr = value.split(",");


                            if(Double.valueOf(str_arr[0])>600)
                            {
                                str_arr[0]=Double.toString(655.3501-Double.valueOf(str_arr[0]));
                            }
                            if(Double.valueOf(str_arr[1])>600)
                            {
                                str_arr[1]=Double.toString(655.3501-Double.valueOf(str_arr[1]));
                            }

                            if(Double.valueOf(str_arr[2])>600)
                            {
                                str_arr[2]=Double.toString(655.3501-Double.valueOf(str_arr[2]));
                            }



                            mTemperatureValueTextView.setText("x " + str_arr[0]+"\n y " + str_arr[1]+ "\n z " + str_arr[2] );

                            AccelGyroscope sample = new AccelGyroscope();
                            sample.ts = System.currentTimeMillis();
                            sample.acceleration.x = Float.valueOf(str_arr[0]);
                            sample.acceleration.y = Float.valueOf(str_arr[0]);
                            sample.acceleration.z = Float.valueOf(str_arr[0]);

                            mProcessingManager.addData(sample);
                        }
                    }
                });
    }



    private void unSubscribeToUpdates() {
        mScannerSubscription.unsubscribe();
        mDeviceSubscription.unsubscribe();

        if (mDevice != null) mDevice.disconnect();
    }

    private void disconnectBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) mBluetoothAdapter.disable();
    }


}